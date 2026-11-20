package com.example.ysukampus.data.notifications

import android.util.Log
import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.local.db.loadCached
import com.example.ysukampus.data.network.dto.NotificationDto
import com.example.ysukampus.data.network.dto.NotificationsPageDto
import com.example.ysukampus.data.network.dto.ReadNotificationsRequestDto
import com.example.ysukampus.data.network.dto.TelegramNotificationsRequestDto
import com.example.ysukampus.data.network.resolveApiFileUrl
import java.io.IOException
import retrofit2.HttpException

/**
 * Уведомления форума.
 *
 * Пагинация — курсорная, ровно как у ленты (`ForumRepository.getFeed`):
 * `null` в `cursor` означает первую страницу, а `nextCursor = null` в ответе —
 * что список кончился. Тексты ошибок взяты из вебки
 * (`notifications.api.ts`), чтобы формулировки совпадали.
 */
class NotificationRepository(
    private val authorizedService: AuthorizedService,
    /**
     * Дисковый кэш первой страницы. `null` — работаем только по сети: так
     * собираются @Preview и тесты, которым Room не нужен.
     */
    private val cache: NotificationCache? = null
) {
    /** @param cursor `null` — первая страница; иначе `next_cursor` предыдущей. */
    suspend fun getNotifications(limit: Int, cursor: String? = null): Result<NotificationsPage> {
        val fetch: suspend () -> Result<NotificationsPageDto> = {
            runApiCatching(LOAD_ERROR_MESSAGE) {
                val page = authorizedService.getNotifications(limit = limit, cursor = cursor)
                Log.d(
                    TAG,
                    "GET notifications(limit=$limit, cursor=$cursor) -> items=${page.items.size} " +
                        "unread=${page.unreadCount} moderation=${page.moderationCount} " +
                        "next=${page.nextCursor}"
                )
                page
            }
        }

        // Страницы за курсором мимо кэша: см. комментарий в NotificationCache.
        if (cursor != null) return fetch().map { it.toPage() }

        return loadCached(
            readCache = { maxAge -> cache?.readFirstPage(limit, maxAge) },
            writeCache = { dto -> cache?.writeFirstPage(limit, dto) },
            freshMaxAge = FIRST_PAGE_MAX_AGE_MS,
            fetch = fetch,
            toDomain = NotificationsPageDto::toPage
        )
    }

    /** Первая страница с диска, независимо от возраста и не трогая сеть. */
    suspend fun peekCachedFirstPage(limit: Int): NotificationsPage? =
        cache?.readFirstPage(limit, maxAge = null)?.toPage()

    /** @return число непрочитанных после отметки — считает его сервер, не мы. */
    suspend fun markRead(notificationId: Int): Result<Int> = runApiCatching(READ_ERROR_MESSAGE) {
        authorizedService.readNotifications(
            ReadNotificationsRequestDto(notificationId = notificationId)
        ).unreadCount
    }.onSuccess { cache?.evictAll() }

    suspend fun markAllRead(): Result<Int> = runApiCatching(READ_ERROR_MESSAGE) {
        authorizedService.readNotifications(ReadNotificationsRequestDto(all = true)).unreadCount
    }.onSuccess { cache?.evictAll() }

    /** Тумблер дублирования уведомлений в Telegram-бота. */
    suspend fun setTelegramNotifications(enabled: Boolean): Result<Boolean> =
        runApiCatching(TELEGRAM_ERROR_MESSAGE) {
            authorizedService.updateTelegramNotifications(
                TelegramNotificationsRequestDto(enabled = enabled)
            ).enabled
        }

    private inline fun <T> runApiCatching(
        defaultMessage: String,
        block: () -> T
    ): Result<T> = try {
        Result.success(block())
    } catch (error: IOException) {
        Result.failure(NotificationException(NETWORK_ERROR_MESSAGE, error))
    } catch (error: HttpException) {
        Result.failure(NotificationException(defaultMessage, error))
    }

    private companion object {
        const val TAG = "NotificationRepo"

        /**
         * Короткий TTL, как у ленты: кэш здесь не ради экономии запросов, а
         * ради мгновенного открытия шторки и работы без сети.
         */
        const val FIRST_PAGE_MAX_AGE_MS = 5L * 60 * 1000
        const val LOAD_ERROR_MESSAGE = "Не удалось загрузить уведомления"
        const val READ_ERROR_MESSAGE = "Не удалось обновить уведомления"
        const val TELEGRAM_ERROR_MESSAGE = "Не удалось изменить настройку уведомлений"
        const val NETWORK_ERROR_MESSAGE =
            "Не удалось связаться с сервером. Проверьте соединение с интернетом"
    }
}

/** Страница уведомлений и живые счётчики к ней. */
data class NotificationsPage(
    val items: List<Notification>,
    val nextCursor: String?,
    /** Непрочитанные по всему аккаунту, а не только среди [items]. */
    val unreadCount: Int,
    /** Размер очереди модерации; 0 у тех, кто её не разбирает. */
    val moderationCount: Int
)

/** Карточка уведомления — одна и та же для списка и для события сокета. */
data class Notification(
    val id: Int,
    val kind: NotificationKind,
    val actor: NotificationActor,
    val target: NotificationTarget,
    val createdAtIso: String,
    /** `null` — не прочитано; карточка подсвечивается в списке. */
    val readAtIso: String?
) {
    val isRead: Boolean get() = readAtIso != null
}

/** Ответили на пост или на комментарий — от этого зависит текст карточки. */
enum class NotificationKind {
    /** Комментарий к вашему посту. */
    Comment,

    /** Ответ на ваш комментарий. */
    Reply;

    companion object {
        fun fromApi(value: String): NotificationKind =
            if (value == "reply") Reply else Comment
    }
}

data class NotificationActor(
    val profileId: String,
    val displayName: String,
    val displayColor: String,
    /** Готовый абсолютный URL аватара либо `null` — тогда рисуем заглушку. */
    val avatarImageUrl: String?
)

/**
 * Цель уведомления.
 *
 * Недоступную цель бэк не выкидывает из списка, а помечает: карточка остаётся,
 * но вместо превью показывает причину и перестаёт открывать тред.
 */
data class NotificationTarget(
    val postId: String,
    val commentId: String,
    val preview: String,
    val imagePreviewUrl: String?,
    val isAvailable: Boolean,
    val unavailableReason: NotificationUnavailableReason?
)

enum class NotificationUnavailableReason {
    CommentDeleted,
    PostDeleted;

    companion object {
        fun fromApi(value: String?): NotificationUnavailableReason? = when (value) {
            "comment_deleted" -> CommentDeleted
            "post_deleted" -> PostDeleted
            else -> null
        }
    }
}

/** Ошибка с готовым для показа текстом — как [com.example.ysukampus.data.forum.ForumException]. */
class NotificationException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

/** Страница целиком — тот же путь разбора для сети и для кэша. */
internal fun NotificationsPageDto.toPage(): NotificationsPage = NotificationsPage(
    items = items.map { it.toDomain() },
    nextCursor = nextCursor,
    unreadCount = unreadCount,
    moderationCount = moderationCount
)

internal fun NotificationDto.toDomain(): Notification = Notification(
    id = id,
    kind = NotificationKind.fromApi(kind),
    actor = NotificationActor(
        profileId = actor.profileId,
        displayName = actor.displayName,
        displayColor = actor.displayColor,
        avatarImageUrl = resolveApiFileUrl(actor.avatarUrl)
    ),
    target = NotificationTarget(
        postId = target.postId,
        commentId = target.commentId,
        preview = target.preview,
        imagePreviewUrl = resolveApiFileUrl(target.imagePreviewUrl),
        isAvailable = target.isAvailable,
        unavailableReason = NotificationUnavailableReason.fromApi(target.unavailableReason)
    ),
    createdAtIso = createdAt,
    readAtIso = readAt
)
