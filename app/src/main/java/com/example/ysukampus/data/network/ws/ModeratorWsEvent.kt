package com.example.ysukampus.data.network.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface ModeratorWsEvent {
    @Serializable
    data class NewPost(
        val id: String,
        @SerialName("author_id")
        val authorId: Long? = null,
        @SerialName("display_name")
        val displayName: String,
        @SerialName("display_color")
        val displayColor: String? = null,
        val content: String,
        val status: String,
        @SerialName("created_at")
        val createdAt: String,
        val zone: String? = null,
        @SerialName("ai_score")
        val aiScore: Double? = null,
        @SerialName("ai_reason")
        val aiReason: String? = null
    ) : ModeratorWsEvent

    @Serializable
    data class NewComment(
        @SerialName("target_type")
        val targetType: String = "comment",
        val id: String,
        @SerialName("post_id")
        val postId: String,
        @SerialName("parent_id")
        val parentId: String? = null,
        @SerialName("author_id")
        val authorId: Long? = null,
        @SerialName("display_name")
        val displayName: String,
        @SerialName("display_color")
        val displayColor: String? = null,
        val content: String,
        val status: String,
        @SerialName("created_at")
        val createdAt: String,
        val zone: String? = null,
        @SerialName("ai_score")
        val aiScore: Double? = null,
        @SerialName("ai_reason")
        val aiReason: String? = null
    ) : ModeratorWsEvent

    /**
     * Живой размер очереди модерации. Бэкенд шлёт его при любом изменении очереди
     * (`forum_service.broadcast_moderation_count`): новый контент, решение модератора,
     * новая или закрытая жалоба.
     */
    @Serializable
    data class ModerationCount(
        val count: Int
    ) : ModeratorWsEvent
}

typealias ModeratorWsConnectionState = WsConnectionState
