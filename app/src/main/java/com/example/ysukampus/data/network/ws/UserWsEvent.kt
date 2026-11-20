package com.example.ysukampus.data.network.ws

import com.example.ysukampus.data.network.dto.NotificationDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** События персонального канала `/ws/user` (`app/routers/ws.py::user_ws`). */
sealed interface UserWsEvent {

    /** Новое уведомление: карточка приходит целиком, догружать её не нужно. */
    @Serializable
    data class NotificationCreated(
        val notification: NotificationDto
    ) : UserWsEvent

    /**
     * Цель уведомления удалена.
     *
     * `comment_id` пуст, когда удалён весь пост: тогда недоступными становятся
     * все уведомления этого поста, а не одного комментария.
     */
    @Serializable
    data class NotificationTargetUnavailable(
        @SerialName("post_id")
        val postId: String,
        @SerialName("comment_id")
        val commentId: String? = null
    ) : UserWsEvent
}
