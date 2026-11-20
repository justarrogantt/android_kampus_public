package com.example.ysukampus.data.forum

import android.content.Context
import android.net.Uri
import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.local.db.loadCached
import com.example.ysukampus.data.media.ImageUploadException
import com.example.ysukampus.data.media.withImageUploadPart
import com.example.ysukampus.data.network.dto.CreateForumCommentRequestDto
import com.example.ysukampus.data.network.dto.CreateForumPostRequestDto
import com.example.ysukampus.data.network.dto.ForumActivityCommentDto
import com.example.ysukampus.data.network.dto.ForumActivityContextDto
import com.example.ysukampus.data.network.dto.ForumAttachmentDto
import com.example.ysukampus.data.network.dto.ForumCommentDto
import com.example.ysukampus.data.network.dto.ForumCommentsPageDto
import com.example.ysukampus.data.network.dto.ForumActivityCommentsPageDto
import com.example.ysukampus.data.network.dto.ForumPostDto
import com.example.ysukampus.data.network.dto.ForumPostsPageDto
import com.example.ysukampus.data.network.dto.ForumUserProfileDto
import com.example.ysukampus.data.network.resolveApiFileUrl
import com.example.ysukampus.ui.main.forum.ForumActivityComment
import com.example.ysukampus.ui.main.forum.ForumActivityContext
import com.example.ysukampus.ui.main.forum.ForumComment
import com.example.ysukampus.ui.main.forum.ForumPost
import com.example.ysukampus.ui.main.forum.ForumPostStatus
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/**
 * Доступ к форуму.
 *
 * Покрывает ленту, отдельный пост, его комментарии и действия над тем и другим:
 * главная берёт первые посты через [getPosts], вкладка «Форум» листает ленту
 * курсором через [getFeed], экран треда — комментарии через [getComments].
 * Тексты ошибок перенесены из вебки, чтобы формулировки совпадали.
 */
class ForumRepository(
    private val context: Context,
    private val authorizedService: AuthorizedService,
    /**
     * Дисковый кэш шторки «Профиль». `null` — работаем только по сети: так
     * собираются @Preview и тесты, которым Room не нужен.
     */
    private val cache: ForumCache? = null
) {
    /** @param limit сколько постов нужно ленте. Бэк ограничивает сверху сотней. */
    suspend fun getPosts(limit: Int): Result<List<ForumPost>> = loadCached(
        readCache = { maxAge -> cache?.readHomePosts(limit, maxAge) },
        writeCache = { dto -> cache?.writeHomePosts(limit, dto) },
        freshMaxAge = FEED_MAX_AGE_MS,
        fetch = {
            runApiCatching(FEED_ERROR_MESSAGE) { authorizedService.getForumPosts(limit = limit) }
        },
        toDomain = { page -> page.items.map { it.toDomain() } }
    )

    /**
     * Страница ленты для вкладки «Форум».
     *
     * @param cursor `null` — первая страница; иначе `next_cursor` предыдущей.
     */
    suspend fun getFeed(limit: Int, cursor: String? = null): Result<ForumFeedPage> {
        val fetch: suspend () -> Result<ForumPostsPageDto> = {
            runApiCatching(FEED_ERROR_MESSAGE) {
                authorizedService.getForumPosts(limit = limit, cursor = cursor)
            }
        }

        // Страницы за курсором мимо кэша: см. комментарий в ForumCache.
        if (cursor != null) return fetch().map { it.toFeedPage() }

        return loadCached(
            readCache = { maxAge -> cache?.readFeed(limit, maxAge) },
            writeCache = { dto -> cache?.writeFeed(limit, dto) },
            freshMaxAge = FEED_MAX_AGE_MS,
            fetch = fetch,
            toDomain = ForumPostsPageDto::toFeedPage
        )
    }

    /** Первая страница ленты с диска, независимо от возраста и не трогая сеть. */
    suspend fun peekCachedFeed(limit: Int): ForumFeedPage? =
        cache?.readFeed(limit, maxAge = null)?.toFeedPage()

    /** Один пост — тред открывается по нему, даже если лента ещё не загружена. */
    suspend fun getPost(postId: String): Result<ForumPost> = loadCached(
        readCache = { maxAge -> cache?.readPost(postId, maxAge) },
        writeCache = { dto -> cache?.writePost(postId, dto) },
        freshMaxAge = FEED_MAX_AGE_MS,
        fetch = { runApiCatching(FEED_ERROR_MESSAGE) { authorizedService.getForumPost(postId) } },
        toDomain = ForumPostDto::toDomain
    )

    /**
     * Страница комментариев поста — курсор устроен так же, как в [getFeed].
     *
     * @param cursor `null` — первая страница; иначе `next_cursor` предыдущей.
     */
    suspend fun getComments(
        postId: String,
        limit: Int,
        cursor: String? = null
    ): Result<ForumCommentsPage> {
        val fetch: suspend () -> Result<ForumCommentsPageDto> = {
            runApiCatching(COMMENTS_ERROR_MESSAGE) {
                authorizedService.getForumComments(postId = postId, limit = limit, cursor = cursor)
            }
        }

        // Страницы за курсором мимо кэша: см. комментарий в ForumCache.
        if (cursor != null) return fetch().map { it.toCommentsPage() }

        return loadCached(
            readCache = { maxAge -> cache?.readComments(postId, limit, maxAge) },
            writeCache = { dto -> cache?.writeComments(postId, limit, dto) },
            freshMaxAge = FEED_MAX_AGE_MS,
            fetch = fetch,
            toDomain = ForumCommentsPageDto::toCommentsPage
        )
    }

    /** Открытый ранее тред с диска, независимо от возраста и не трогая сеть. */
    suspend fun peekCachedPost(postId: String): ForumPost? =
        cache?.readPost(postId, maxAge = null)?.toDomain()

    suspend fun peekCachedComments(postId: String, limit: Int): ForumCommentsPage? =
        cache?.readComments(postId, limit, maxAge = null)?.toCommentsPage()

    /**
     * Публичный профиль форума: ник, цвет, аватар, дата регистрации и счётчики.
     * Своя карточка берётся той же ручкой, что и чужая.
     */
    suspend fun getUserProfile(profileId: String): Result<ForumUserProfile> = loadCached(
        readCache = { maxAge -> cache?.readUserProfile(profileId, maxAge) },
        writeCache = { dto -> cache?.writeUserProfile(profileId, dto) },
        freshMaxAge = PROFILE_MAX_AGE_MS,
        fetch = {
            runApiCatching(PROFILE_ERROR_MESSAGE) {
                authorizedService.getForumUserProfile(profileId)
            }
        },
        toDomain = ForumUserProfileDto::toDomain
    )

    /** Что лежит на диске от прошлого открытия шторки, не трогая сеть. */
    suspend fun peekCachedUserProfile(profileId: String): ForumUserProfile? =
        cache?.readUserProfile(profileId, maxAge = null)?.toDomain()

    suspend fun peekCachedMyPosts(): ForumFeedPage? =
        cache?.readMyPosts(maxAge = null)?.toFeedPage()

    suspend fun peekCachedMyComments(): ForumActivityCommentsPage? =
        cache?.readMyComments(maxAge = null)?.toActivityPage()

    /**
     * Своя активность для «Моих постов»: страница постов текущего пользователя.
     *
     * Ручка та же, что и у публичного профиля (`forum/user-posts`), только без
     * `profile_id` — бэк подставляет автора из токена.
     */
    suspend fun getMyPosts(limit: Int, cursor: String? = null): Result<ForumFeedPage> {
        val fetch: suspend () -> Result<ForumPostsPageDto> = {
            runApiCatching(FEED_ERROR_MESSAGE) {
                authorizedService.getMyForumPosts(limit = limit, cursor = cursor)
            }
        }

        // Страницы за курсором мимо кэша: см. комментарий в ForumCache.
        if (cursor != null) return fetch().map { it.toFeedPage() }

        return loadCached(
            readCache = { maxAge -> cache?.readMyPosts(maxAge) },
            writeCache = { dto -> cache?.writeMyPosts(dto) },
            freshMaxAge = ACTIVITY_MAX_AGE_MS,
            fetch = fetch,
            toDomain = ForumPostsPageDto::toFeedPage
        )
    }

    /** Страница своих комментариев с контекстом поста и ответа. */
    suspend fun getMyComments(
        limit: Int,
        cursor: String? = null
    ): Result<ForumActivityCommentsPage> {
        val fetch: suspend () -> Result<ForumActivityCommentsPageDto> = {
            runApiCatching(COMMENTS_ERROR_MESSAGE, ForumTarget.Comment) {
                authorizedService.getMyForumComments(limit = limit, cursor = cursor)
            }
        }

        if (cursor != null) return fetch().map { it.toActivityPage() }

        return loadCached(
            readCache = { maxAge -> cache?.readMyComments(maxAge) },
            writeCache = { dto -> cache?.writeMyComments(dto) },
            freshMaxAge = ACTIVITY_MAX_AGE_MS,
            fetch = fetch,
            toDomain = ForumActivityCommentsPageDto::toActivityPage
        )
    }

    /**
     * Страница постов произвольного пользователя — публичный профиль (чужой
     * или свой, открытый по явному `profileId`, а не через [getMyPosts]).
     */
    suspend fun getUserPosts(
        profileId: String,
        limit: Int,
        cursor: String? = null
    ): Result<ForumFeedPage> {
        val fetch: suspend () -> Result<ForumPostsPageDto> = {
            runApiCatching(FEED_ERROR_MESSAGE) {
                authorizedService.getForumUserPosts(profileId = profileId, limit = limit, cursor = cursor)
            }
        }

        if (cursor != null) return fetch().map { it.toFeedPage() }

        return loadCached(
            readCache = { maxAge -> cache?.readUserPosts(profileId, maxAge) },
            writeCache = { dto -> cache?.writeUserPosts(profileId, dto) },
            freshMaxAge = ACTIVITY_MAX_AGE_MS,
            fetch = fetch,
            toDomain = ForumPostsPageDto::toFeedPage
        )
    }

    /** Комментарии произвольного пользователя — аналог [getMyComments] по `profileId`. */
    suspend fun getUserComments(
        profileId: String,
        limit: Int,
        cursor: String? = null
    ): Result<ForumActivityCommentsPage> {
        val fetch: suspend () -> Result<ForumActivityCommentsPageDto> = {
            runApiCatching(COMMENTS_ERROR_MESSAGE, ForumTarget.Comment) {
                authorizedService.getForumUserComments(profileId = profileId, limit = limit, cursor = cursor)
            }
        }

        if (cursor != null) return fetch().map { it.toActivityPage() }

        return loadCached(
            readCache = { maxAge -> cache?.readUserComments(profileId, maxAge) },
            writeCache = { dto -> cache?.writeUserComments(profileId, dto) },
            freshMaxAge = ACTIVITY_MAX_AGE_MS,
            fetch = fetch,
            toDomain = ForumActivityCommentsPageDto::toActivityPage
        )
    }

    suspend fun peekCachedUserPosts(profileId: String): ForumFeedPage? =
        cache?.readUserPosts(profileId, maxAge = null)?.toFeedPage()

    suspend fun peekCachedUserComments(profileId: String): ForumActivityCommentsPage? =
        cache?.readUserComments(profileId, maxAge = null)?.toActivityPage()

    /**
     * Создаёт пост.
     *
     * @param imageId вложение из [uploadImage]. Пост без текста и без картинки
     * бэк отклоняет.
     * @return [ForumPublication.Published] с готовой карточкой либо
     * [ForumPublication.ModerationRequired], если контент ушёл на модерацию.
     */
    suspend fun createPost(
        content: String,
        imageId: String? = null
    ): Result<ForumPublication<ForumPost>> =
        runApiCatching(CREATE_ERROR_MESSAGE, ForumTarget.Post) {
            val result = authorizedService.createForumPost(
                CreateForumPostRequestDto(content = content, imageId = imageId)
            )
            result.post?.takeIf { result.result == RESULT_PUBLISHED }
                ?.let { ForumPublication.Published(it.toDomain()) }
                ?: ForumPublication.ModerationRequired
        }
            // Контент изменился — кэш форума больше не описывает
            // действительность, и ждать TTL незачем.
            .onSuccess { cache?.evictContent() }

    /**
     * Создаёт комментарий или ответ на комментарий.
     *
     * @param parentId `null` — комментарий первого уровня, иначе id комментария,
     * на который отвечают.
     * @param imageId вложение из [uploadImage]. Комментарий без текста и без
     * картинки бэк отклоняет.
     */
    suspend fun addComment(
        postId: String,
        content: String,
        parentId: String? = null,
        imageId: String? = null
    ): Result<ForumPublication<ForumComment>> =
        runApiCatching(CREATE_ERROR_MESSAGE, ForumTarget.Comment) {
            val result = authorizedService.createForumComment(
                postId = postId,
                body = CreateForumCommentRequestDto(
                    content = content,
                    parentId = parentId,
                    imageId = imageId
                )
            )
            result.comment?.takeIf { result.result == RESULT_PUBLISHED }
                ?.let { ForumPublication.Published(it.toDomain()) }
                ?: ForumPublication.ModerationRequired
        }
            // Контент изменился — кэш форума больше не описывает
            // действительность, и ждать TTL незачем.
            .onSuccess { cache?.evictContent() }

    /**
     * Загружает картинку для будущего поста или комментария.
     *
     * Файл уходит на сервер сразу при выборе, пока пользователь допечатывает
     * текст: при публикации остаётся передать только `id`. Чтение Uri, проверка
     * формата и размера — общие с фото профиля ([withImageUploadPart]).
     */
    suspend fun uploadImage(uri: Uri): Result<ForumAttachment> = runCatching {
        withImageUploadPart(
            context = context,
            uri = uri,
            fileNamePrefix = "forum_image",
            tooLargeMessage = IMAGE_TOO_LARGE_MESSAGE,
            invalidFormatMessage = IMAGE_INVALID_FORMAT_MESSAGE
        ) { part -> authorizedService.uploadForumMedia(part).toDomain() }
    }.recoverCatching { error ->
        // Загрузку отменяют, когда пользователь убрал картинку или выбрал новую:
        // отмена не ошибка и наверх как ошибка уходить не должна.
        if (error is CancellationException) throw error
        throw error.toUploadException()
    }

    private fun Throwable.toUploadException(): ForumException = when (this) {
        // Локальная проверка файла уже содержит готовый текст.
        is ImageUploadException -> ForumException(message, this)
        is IOException -> ForumException(NETWORK_ERROR_MESSAGE, this)
        is HttpException -> when (code()) {
            403 -> ForumException("Действие временно ограничено", this)
            413 -> ForumException(IMAGE_TOO_LARGE_MESSAGE, this)
            422 -> ForumException(IMAGE_INVALID_FORMAT_MESSAGE, this)
            else -> ForumException(IMAGE_UPLOAD_ERROR_MESSAGE, this)
        }

        else -> ForumException(IMAGE_UPLOAD_ERROR_MESSAGE, this)
    }

    /** @return новое состояние лайка и счётчик с сервера. */
    suspend fun toggleLike(postId: String): Result<ForumLike> = runApiCatching(LIKE_ERROR_MESSAGE) {
        val reaction = authorizedService.toggleForumPostLike(postId)
        ForumLike(liked = reaction.liked, count = reaction.count)
    }

    /** Лайк комментария — счётчик и состояние приходят с сервера, как у поста. */
    suspend fun toggleCommentLike(commentId: String): Result<ForumLike> =
        runApiCatching(LIKE_ERROR_MESSAGE, ForumTarget.Comment) {
            val reaction = authorizedService.toggleForumCommentLike(commentId)
            ForumLike(liked = reaction.liked, count = reaction.count)
        }

    suspend fun reportPost(postId: String): Result<Unit> = runApiCatching(REPORT_ERROR_MESSAGE) {
        // Тело ответа не нужно, но его надо закрыть, иначе соединение утечёт.
        authorizedService.reportForumPost(postId).close()
    }

    suspend fun reportComment(commentId: String): Result<Unit> =
        runApiCatching(REPORT_ERROR_MESSAGE, ForumTarget.Comment) {
            authorizedService.reportForumComment(commentId).close()
        }

    suspend fun deletePost(postId: String): Result<Unit> = runApiCatching(DELETE_ERROR_MESSAGE) {
        authorizedService.deleteForumPost(postId).close()
    }.onSuccess { cache?.evictContent() }

    suspend fun deleteComment(commentId: String): Result<Unit> =
        runApiCatching(DELETE_ERROR_MESSAGE, ForumTarget.Comment) {
            authorizedService.deleteForumComment(commentId).close()
        }.onSuccess { cache?.evictContent() }

    /**
     * @param target от чего зависят тексты 404/409/429 — пост это или комментарий.
     */
    private inline fun <T> runApiCatching(
        defaultMessage: String,
        target: ForumTarget = ForumTarget.Post,
        block: () -> T
    ): Result<T> = try {
        Result.success(block())
    } catch (error: IOException) {
        Result.failure(ForumException(NETWORK_ERROR_MESSAGE, error))
    } catch (error: HttpException) {
        Result.failure(ForumException(error.toForumMessage(defaultMessage, target), error))
    }

    private fun HttpException.toForumMessage(
        defaultMessage: String,
        target: ForumTarget
    ): String = when (code()) {
        403 -> "Действие временно ограничено"
        404 -> target.goneMessage
        409 -> target.alreadyReportedMessage
        429 -> target.dailyLimitMessage
        else -> defaultMessage
    }

    /** Цель действия: от неё зависят формулировки ошибок про пост и комментарий. */
    private enum class ForumTarget(
        val goneMessage: String,
        val alreadyReportedMessage: String,
        val dailyLimitMessage: String
    ) {
        Post(
            goneMessage = "Пост уже недоступен",
            alreadyReportedMessage = "Вы уже жаловались на этот пост",
            dailyLimitMessage = "Превышен дневной лимит постов."
        ),
        Comment(
            goneMessage = "Комментарий уже недоступен",
            alreadyReportedMessage = "Вы уже жаловались на этот комментарий",
            dailyLimitMessage = "Превышен дневной лимит комментариев."
        )
    }

    private companion object {
        /**
         * Сколько кэш шторки «Профиль» считается свежим. Активность меняется
         * только действиями самого пользователя, а их мы и так сбрасываем
         * через `ForumCache.evictContent`, поэтому час — не мало.
         */
        const val PROFILE_MAX_AGE_MS = 60L * 60 * 1000
        const val ACTIVITY_MAX_AGE_MS = 60L * 60 * 1000

        /**
         * Лента живая: её TTL короткий, и кэш здесь нужен не чтобы экономить
         * запросы, а чтобы вкладка открывалась мгновенно и работала без сети.
         */
        const val FEED_MAX_AGE_MS = 5L * 60 * 1000

        const val RESULT_PUBLISHED = "published"
        const val FEED_ERROR_MESSAGE = "Не удалось загрузить посты"
        const val PROFILE_ERROR_MESSAGE = "Не удалось загрузить профиль"
        const val COMMENTS_ERROR_MESSAGE = "Не удалось загрузить комментарии"
        const val CREATE_ERROR_MESSAGE = "Ошибка, попробуйте позже"
        const val LIKE_ERROR_MESSAGE = "Не удалось поставить лайк"
        const val REPORT_ERROR_MESSAGE = "Жалоба не отправлена. Попробуйте еще раз"
        const val DELETE_ERROR_MESSAGE = "Ошибка удаления. Попробуйте еще раз"
        const val NETWORK_ERROR_MESSAGE =
            "Не удалось связаться с сервером. Проверьте соединение с интернетом"
        const val IMAGE_UPLOAD_ERROR_MESSAGE = "Не удалось загрузить картинку"
        const val IMAGE_TOO_LARGE_MESSAGE = "Картинка слишком большая. Максимальный размер - 5 MB."
        const val IMAGE_INVALID_FORMAT_MESSAGE =
            "Неверный формат изображения. Поддерживаются JPG, PNG и WEBP."
    }
}

/** Страница ленты: посты и курсор следующей страницы (`null` — лента кончилась). */
data class ForumFeedPage(
    val posts: List<ForumPost>,
    val nextCursor: String?
)

/** Шапка профиля форума: ник, цвет, аватар, дата регистрации и счётчики активности. */
data class ForumUserProfile(
    val profileId: String,
    val displayName: String,
    val displayColor: String,
    /** Готовый абсолютный URL аватара либо `null` — тогда рисуем заглушку. */
    val avatarImageUrl: String?,
    val createdAtIso: String,
    val postsCount: Int,
    val commentsCount: Int
)

/** Страница своей активности: комментарии с контекстом и курсор следующей страницы. */
data class ForumActivityCommentsPage(
    val comments: List<ForumActivityComment>,
    val nextCursor: String?
)

/** Страница треда: комментарии и курсор следующей страницы. */
data class ForumCommentsPage(
    val comments: List<ForumComment>,
    val nextCursor: String?
)

/**
 * Итог публикации.
 *
 * Бэк не отдаёт карточку, если AI-модерация отправила контент в очередь —
 * в этом случае показывать в списке нечего.
 */
sealed interface ForumPublication<out T> {
    data class Published<T>(val content: T) : ForumPublication<T>
    data object ModerationRequired : ForumPublication<Nothing>
}

/** Загруженное вложение: id для публикации и готовые абсолютные ссылки. */
data class ForumAttachment(
    val id: String,
    val imageUrl: String,
    val imagePreviewUrl: String,
    val imageWidth: Int,
    val imageHeight: Int
)

/** Итог переключения лайка. */
data class ForumLike(
    val liked: Boolean,
    val count: Int
)

/** Ошибка с готовым для показа текстом. */
class ForumException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

internal fun ForumPostDto.toDomain(): ForumPost = ForumPost(
    id = id,
    profileId = profileId,
    displayName = displayName,
    displayColor = displayColor,
    avatarImageUrl = resolveApiFileUrl(avatarUrl),
    visibleRole = visibleRole?.takeIf { it.isNotBlank() },
    isMine = isMine,
    content = content,
    status = ForumPostStatus.fromApi(status),
    likesCount = likesCount,
    commentsCount = commentsCount,
    likedByMe = likedByMe,
    // Вебка показывает время публикации, а до неё — создания.
    createdAtIso = publishedAt ?: createdAt,
    imageUrl = resolveApiFileUrl(imageUrl),
    imagePreviewUrl = resolveApiFileUrl(imagePreviewUrl),
    imageWidth = imageWidth,
    imageHeight = imageHeight
)

internal fun ForumCommentDto.toDomain(): ForumComment = ForumComment(
    id = id,
    postId = postId,
    profileId = profileId,
    displayName = displayName,
    displayColor = displayColor,
    avatarImageUrl = resolveApiFileUrl(avatarUrl),
    visibleRole = visibleRole?.takeIf { it.isNotBlank() },
    isMine = isMine,
    isPostAuthor = isPostAuthor,
    content = content,
    parentId = parentId,
    status = ForumPostStatus.fromApi(status),
    likesCount = likesCount,
    repliesCount = repliesCount,
    likedByMe = likedByMe,
    createdAtIso = publishedAt ?: createdAt,
    imageUrl = resolveApiFileUrl(imageUrl),
    imagePreviewUrl = resolveApiFileUrl(imagePreviewUrl),
    imageWidth = imageWidth,
    imageHeight = imageHeight
)

private fun ForumAttachmentDto.toDomain(): ForumAttachment = ForumAttachment(
    id = id,
    // Бэк отдаёт путь от корня (`/api/forum/media/...`) — дополняем его хостом.
    imageUrl = resolveApiFileUrl(imageUrl) ?: imageUrl,
    imagePreviewUrl = resolveApiFileUrl(imagePreviewUrl) ?: imagePreviewUrl,
    imageWidth = imageWidth,
    imageHeight = imageHeight
)

internal fun ForumActivityCommentDto.toDomain(): ForumActivityComment = ForumActivityComment(
    comment = ForumComment(
        id = id,
        postId = postId,
        profileId = profileId,
        displayName = displayName,
        displayColor = displayColor,
        avatarImageUrl = resolveApiFileUrl(avatarUrl),
        visibleRole = visibleRole?.takeIf { it.isNotBlank() },
        isMine = isMine,
        isPostAuthor = isPostAuthor,
        content = content,
        parentId = parentId,
        status = ForumPostStatus.fromApi(status),
        likesCount = likesCount,
        repliesCount = repliesCount,
        likedByMe = likedByMe,
        createdAtIso = publishedAt ?: createdAt,
        imageUrl = resolveApiFileUrl(imageUrl),
        imagePreviewUrl = resolveApiFileUrl(imagePreviewUrl),
        imageWidth = imageWidth,
        imageHeight = imageHeight
    ),
    postContext = postContext.toDomain(),
    replyContext = replyContext?.toDomain()
)

private fun ForumActivityContextDto.toDomain(): ForumActivityContext = ForumActivityContext(
    id = id,
    displayName = displayName,
    content = content
)

internal fun ForumCommentsPageDto.toCommentsPage(): ForumCommentsPage = ForumCommentsPage(
    comments = items.map { it.toDomain() },
    nextCursor = nextCursor
)

/** Страница ленты целиком — тот же путь разбора для сети и для кэша. */
internal fun ForumPostsPageDto.toFeedPage(): ForumFeedPage = ForumFeedPage(
    posts = items.map { it.toDomain() },
    nextCursor = nextCursor
)

internal fun ForumActivityCommentsPageDto.toActivityPage(): ForumActivityCommentsPage =
    ForumActivityCommentsPage(
        comments = items.map { it.toDomain() },
        nextCursor = nextCursor
    )

internal fun ForumUserProfileDto.toDomain(): ForumUserProfile = ForumUserProfile(
    profileId = profileId,
    displayName = displayName,
    displayColor = displayColor,
    avatarImageUrl = resolveApiFileUrl(avatarUrl),
    createdAtIso = createdAt,
    postsCount = postsCount,
    commentsCount = commentsCount
)
