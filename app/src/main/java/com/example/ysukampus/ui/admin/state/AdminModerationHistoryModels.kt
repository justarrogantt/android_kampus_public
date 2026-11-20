package com.example.ysukampus.ui.admin.state

/** Право на просмотр ленты истории модерации. */
const val PERMISSION_MODERATION_HISTORY = "get-moderation-history"

/** Право вернуть скрытый пост или комментарий в ленту. */
const val PERMISSION_RESTORE_CONTENT = "restore-hidden-content"

/**
 * Лента истории модерации (`GET forum/moderation/history`): вердикты ИИ и
 * решения модераторов в одном списке. В отличие от очереди это уже принятые
 * решения, поэтому редактировать тут нечего — можно только восстановить
 * скрытый контент.
 */
data class AdminModerationEvent(
    val id: String,
    /** ISO-время события. */
    val ts: String,
    /** `ai_verdict` | `approve` | `hide` | `mute` | `unmute` | `resolve_report` | `delete_by_author`. */
    val action: String,
    val actor: AdminModerationEventActor,
    val target: AdminModerationEventTarget,
    val details: AdminModerationEventDetails
)

/** Кто принял решение: `ai`, `moderator` или сам `author`. */
data class AdminModerationEventActor(
    val kind: String,
    val name: String
)

data class AdminModerationEventTarget(
    val type: AdminModerationEventTargetType,
    val id: String,
    /** Короткая подпись: начало текста поста/коммента или ник пользователя. */
    val preview: String? = null,
    val content: String? = null,
    val author: String? = null,
    val authorColor: String? = null,
    val status: String? = null,
    val deletedByAuthor: Boolean = false,
    val imageUrl: String? = null,
    val imagePreviewUrl: String? = null,
    /** Картинка была, но отдать её нельзя: автор удалил контент. */
    val imageHidden: Boolean = false
)

enum class AdminModerationEventTargetType {
    Post,
    Comment,
    User;

    /** Значение для путей `forum/moderation/content/{target_type}/{id}/...`. */
    val apiValue: String get() = name.lowercase()

    companion object {
        fun fromApi(raw: String?): AdminModerationEventTargetType = when (raw?.lowercase()) {
            "post" -> Post
            "comment" -> Comment
            else -> User
        }
    }
}

/** Разнородный `details` события, разобранный по полям, которые показывает UI. */
data class AdminModerationEventDetails(
    val fromStatus: String? = null,
    val toStatus: String? = null,
    val zone: String? = null,
    val score: Int? = null,
    val reason: String? = null,
    /** Вердикт ИИ выставлен фолбэком, потому что его API не ответил. */
    val apiError: Boolean = false,
    /** Срок мьюта в часах; `null` — навсегда. */
    val hours: Int? = null,
    val contentStatus: String? = null,
    val reportId: String? = null
)
