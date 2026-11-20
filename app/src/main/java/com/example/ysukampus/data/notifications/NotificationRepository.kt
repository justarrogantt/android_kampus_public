package com.example.ysukampus.data.notifications

import android.util.Log
import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.network.dto.NotificationDto
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
    private val authorizedService: AuthorizedService
) {
    /** @param cursor `null` — первая страница; иначе `next_cursor` предыдущей. */
    suspend fun getNotifications(limit: Int, cursor: String? = null): Result<NotificationsPage> =
        runApiCatching(LOAD_ERROR_MESSAGE) {
            val page = authorizedService.getNotifications(limit = limit, cursor = cursor)
            Log.d(
                TAG,
                "GET notifications(limit=$limit, cursor=$cursor) -> items=${page.items.size} " +
                    "unread=${page.unreadCount} moderation=${page.moderationCount} " +
                    "next=${page.nextCursor}"
            )
            NotificationsPage(
                items = page.items.map { it.toDomain() },
                nextCursor = page.nextCursor,
                unreadCount = page.unreadCount,
                moderationCount = page.moderationCount
            )
        }

    /** @return число непрочитанных после отметки — считает его сервер, не мы. */
    suspend fun markRead(notificationId: Int): Result<Int> = runApiCatching(READ_ERROR_MESSAGE) {
        authorizedService.readNotifications(
            ReadNotificationsRequestDto(notificationId = notificationId)
        ).unreadCount
    }

    suspend fun markAllRead(): Result<Int> = runApiCatching(READ_ERROR_MESSAGE) {
        authorizedService.readNotifications(ReadNotificationsRequestDto(all = true)).unreadCount
    }

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
