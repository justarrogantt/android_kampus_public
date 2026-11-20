package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Уведомления форума (`app/schemas/notification_schemas.py`).
 *
 * Одна и та же карточка приходит и страницей из `GET notifications`, и
 * событием `notification_created` персонального сокета, поэтому
 * [NotificationDto] переиспользуется обоими каналами.
 */
@Serializable
data class NotificationDto(
    val id: Int,
    /** `comment` — ответ на пост, `reply` — ответ на комментарий. */
    val kind: String,
    val actor: NotificationActorDto,
    val target: NotificationTargetDto,
    @SerialName("created_at")
    val createdAt: String,
    /** `null` — уведомление ещё не прочитано. */
    @SerialName("read_at")
    val readAt: String? = null
)

/** Автор действия: по нему рисуются аватар, ник и цвет обводки. */
@Serializable
data class NotificationActorDto(
    @SerialName("profile_id")
    val profileId: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("display_color")
    val displayColor: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

/**
 * Комментарий, к которому ведёт уведомление.
 *
 * У удалённой цели бэк вычищает `preview` и превью картинки, оставляя только
 * причину: карточка остаётся в списке, но открыть её уже нельзя.
 */
@Serializable
data class NotificationTargetDto(
    @SerialName("post_id")
    val postId: String,
    @SerialName("comment_id")
    val commentId: String,
    val preview: String = "",
    @SerialName("image_preview_url")
    val imagePreviewUrl: String? = null,
    @SerialName("is_available")
    val isAvailable: Boolean,
    /** `comment_deleted` | `post_deleted` | `null`. */
    @SerialName("unavailable_reason")
    val unavailableReason: String? = null
)

/**
 * Страница уведомлений.
 *
 * `unread_count` — число непрочитанных на сервере целиком, а не среди
 * загруженных карточек. `moderation_count` приходит только тем, у кого есть
 * право `moderate-forum`, остальным всегда 0.
 */
@Serializable
data class NotificationsPageDto(
    val items: List<NotificationDto> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    @SerialName("unread_count")
    val unreadCount: Int = 0,
    @SerialName("moderation_count")
    val moderationCount: Int = 0
)

/**
 * Отметка прочитанного: либо одно уведомление, либо все сразу.
 * Бэк отбивает запрос, в котором заполнены оба поля или ни одного.
 */
@Serializable
data class ReadNotificationsRequestDto(
    @SerialName("notification_id")
    val notificationId: Int? = null,
    val all: Boolean? = null
)

/** Актуальное число непрочитанных после отметки. */
@Serializable
data class ReadNotificationsResultDto(
    @SerialName("unread_count")
    val unreadCount: Int
)

/** Тело `PATCH profile/telegram-notifications`. */
@Serializable
data class TelegramNotificationsRequestDto(
    val enabled: Boolean
)

/** Ответ `PATCH profile/telegram-notifications` — состояние тумблера. */
@Serializable
data class TelegramNotificationsResultDto(
    val enabled: Boolean
)
