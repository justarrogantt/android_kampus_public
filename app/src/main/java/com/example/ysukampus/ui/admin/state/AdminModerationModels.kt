package com.example.ysukampus.ui.admin.state

enum class AdminModerationTab(val title: String) {
    Moderation("Модерация"),
    Reports("Репорты"),
    History("История")
}

/** Статусы контента, которые принимает `PATCH forum/moderation/.../status`. */
const val MODERATION_STATUS_ACTIVE = "active"
const val MODERATION_STATUS_HIDDEN = "hidden"

enum class AdminModerationTargetType {
    Post,
    Comment
}

data class AdminModerationItem(
    val id: String,
    val targetType: AdminModerationTargetType,
    val postId: String? = null,
    val parentId: String? = null,
    val authorId: Long? = null,
    val authorName: String,
    val authorColor: String? = null,
    val content: String,
    val status: String,
    val createdAt: String,
    val zone: String? = null,
    val aiScore: Double? = null,
    val aiReason: String? = null,
    /** Сколько жалоб пришло на этот контент; в очереди обычно 0. */
    val reportsCount: Int = 0,
    val imageUrl: String? = null,
    val imagePreviewUrl: String? = null
)

data class AdminModerationReport(
    val id: String,
    val targetType: AdminModerationTargetType,
    val targetId: String,
    val authorName: String,
    val authorColor: String? = null,
    val content: String,
    /** Статус самой жалобы (`report_status`). */
    val status: String? = null,
    /** Статус контента, на который пожаловались (`content_status`). */
    val contentStatus: String? = null,
    val createdAt: String,
    val reportReason: String? = null,
    val zone: String? = null,
    val aiScore: Double? = null,
    val aiReason: String? = null,
    val reportsCount: Int = 0,
    val imageUrl: String? = null,
    val imagePreviewUrl: String? = null
)
