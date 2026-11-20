package com.example.ysukampus.data.forum

import android.content.Context
import android.net.Uri
import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.media.ImageUploadException
import com.example.ysukampus.data.media.withImageUploadPart
import com.example.ysukampus.data.network.dto.CreateForumCommentRequestDto
import com.example.ysukampus.data.network.dto.CreateForumPostRequestDto
import com.example.ysukampus.data.network.dto.ForumActivityCommentDto
import com.example.ysukampus.data.network.dto.ForumActivityContextDto
import com.example.ysukampus.data.network.dto.ForumAttachmentDto
import com.example.ysukampus.data.network.dto.ForumCommentDto
import com.example.ysukampus.data.network.dto.ForumPostDto
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
    private val authorizedService: AuthorizedService
) {
    /** @param limit сколько постов нужно ленте. Бэк ограничивает сверху сотней. */
    suspend fun getPosts(limit: Int): Result<List<ForumPost>> = runApiCatching(FEED_ERROR_MESSAGE) {
        authorizedService.getForumPosts(limit = limit).items.map { it.toDomain() }
    }

    /**
     * Страница ленты для вкладки «Форум».
     *
     * @param cursor `null` — первая страница; иначе `next_cursor` предыдущей.
     */
    suspend fun getFeed(limit: Int, cursor: String? = null): Result<ForumFeedPage> =
        runApiCatching(FEED_ERROR_MESSAGE) {
            val page = authorizedService.getForumPosts(limit = limit, cursor = cursor)
            ForumFeedPage(
                posts = page.items.map { it.toDomain() },
                nextCursor = page.nextCursor
            )
        }

    /** Один пост — тред открывается по нему, даже если лента ещё не загружена. */
    suspend fun getPost(postId: String): Result<ForumPost> = runApiCatching(FEED_ERROR_MESSAGE) {
        authorizedService.getForumPost(postId).toDomain()
    }

    /**
     * Страница комментариев поста — курсор устроен так же, как в [getFeed].
     *
     * @param cursor `null` — первая страница; иначе `next_cursor` предыдущей.
     */
    suspend fun getComments(
        postId: String,
        limit: Int,
        cursor: String? = null
    ): Result<ForumCommentsPage> = runApiCatching(COMMENTS_ERROR_MESSAGE) {
        val page = authorizedService.getForumComments(
            postId = postId,
            limit = limit,
            cursor = cursor
        )
        ForumCommentsPage(
            comments = page.items.map { it.toDomain() },
            nextCursor = page.nextCursor
        )
    }

    /**
     * Публичный профиль форума: ник, цвет, аватар, дата регистрации и счётчики.
     * Своя карточка берётся той же ручкой, что и чужая.
     */
    suspend fun getUserProfile(profileId: String): Result<ForumUserProfile> =
        runApiCatching(PROFILE_ERROR_MESSAGE) {
            authorizedService.getForumUserProfile(profileId).toDomain()
        }

    /**
     * Своя активность для «Моих постов»: страница постов текущего пользователя.
     *
     * Ручка та же, что и у публичного профиля (`forum/user-posts`), только без
     * `profile_id` — бэк подставляет автора из токена.
     */
    suspend fun getMyPosts(limit: Int, cursor: String? = null): Result<ForumFeedPage> =
        runApiCatching(FEED_ERROR_MESSAGE) {
            val page = authorizedService.getMyForumPosts(limit = limit, cursor = cursor)
            ForumFeedPage(
                posts = page.items.map { it.toDomain() },
                nextCursor = page.nextCursor
            )
        }

    /** Страница своих комментариев с контекстом поста и ответа. */
    suspend fun getMyComments(
        limit: Int,
        cursor: String? = null
    ): Result<ForumActivityCommentsPage> = runApiCatching(COMMENTS_ERROR_MESSAGE, ForumTarget.Comment) {
        val page = authorizedService.getMyForumComments(limit = limit, cursor = cursor)
        ForumActivityCommentsPage(
            comments = page.items.map { it.toDomain() },
            nextCursor = page.nextCursor
        )
    }

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
    }

    suspend fun deleteComment(commentId: String): Result<Unit> =
        runApiCatching(DELETE_ERROR_MESSAGE, ForumTarget.Comment) {
            authorizedService.deleteForumComment(commentId).close()
        }

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

internal fun ForumUserProfileDto.toDomain(): ForumUserProfile = ForumUserProfile(
    profileId = profileId,
    displayName = displayName,
    displayColor = displayColor,
    avatarImageUrl = resolveApiFileUrl(avatarUrl),
    createdAtIso = createdAt,
    postsCount = postsCount,
    commentsCount = commentsCount
)
