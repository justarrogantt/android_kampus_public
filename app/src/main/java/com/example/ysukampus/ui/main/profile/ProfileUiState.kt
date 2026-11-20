package com.example.ysukampus.ui.main.profile

import androidx.compose.ui.graphics.Color
import com.example.ysukampus.ui.main.schedule.ScheduleDirectory
import com.example.ysukampus.ui.main.schedule.ScheduleEntity
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType

data class ProfileUiState(
    val name: String = "Студент ЮГУ",
    val entityName: String = "Группа не указана",
    val forumColor: String? = null,
    val avatarUrl: String? = null,
    val avatarImageUrl: String? = null,
    val avatarMenuVisible: Boolean = false,
    val isAvatarUploading: Boolean = false,
    val isAvatarDeleting: Boolean = false,
    /**
     * Без сохранённого id пользователя ни перечитать, ни сохранить профиль
     * нельзя — кнопка редактирования в этом случае неактивна.
     */
    val canEdit: Boolean = false,
    /** `null` — шторка редактирования закрыта. */
    val edit: EditProfileState? = null,
    val message: String? = null
) {
    val isAvatarBusy: Boolean get() = isAvatarUploading || isAvatarDeleting
}

/** Страница внутри шторки: сама форма или выбор группы/преподавателя. */
enum class EditProfilePage { Form, Picker }

data class EditProfileState(
    /** Сохранённые значения — с ними сравнивается черновик и к ним откатывается ошибка. */
    val saved: ProfileDraft,
    val draft: ProfileDraft,
    val page: EditProfilePage = EditProfilePage.Form,
    val pickerMode: ScheduleEntityType = ScheduleEntityType.Group,
    val pickerQuery: String = "",
    val directory: ScheduleDirectory? = null,
    val isDirectoryLoading: Boolean = false,
    val isSaving: Boolean = false
) {
    val isForumNameValid: Boolean
        get() = draft.forumName.length in FORUM_NAME_MIN_LENGTH..FORUM_NAME_MAX_LENGTH

    val isChanged: Boolean get() = draft != saved

    val isSaveEnabled: Boolean
        get() = isForumNameValid && isChanged && !isSaving && draft.entityOid.isNotBlank()

    /** Отфильтрованный поиском список текущего режима пикера. */
    val pickerMembers: List<ScheduleEntity>
        get() {
            val members = when (pickerMode) {
                ScheduleEntityType.Group -> directory?.groups
                ScheduleEntityType.Lecturer -> directory?.lecturers
            }.orEmpty()
            val query = pickerQuery.trim().lowercase()
            if (query.isEmpty()) return members

            return members.filter { it.name.lowercase().contains(query) }
        }

    companion object {
        const val FORUM_NAME_MIN_LENGTH = 4
        const val FORUM_NAME_MAX_LENGTH = 16
    }
}

data class ProfileDraft(
    val forumName: String,
    val forumColor: String,
    val entityOid: String,
    val entityName: String,
    val entityType: ScheduleEntityType,
    val telegramNotificationsEnabled: Boolean
)

/**
 * Палитра форумных цветов.
 *
 * Список и порядок повторяют `FORUM_COLORS` бэка (`app/utils/forum_identity.py`):
 * значение вне этого набора сервер отбивает 422, поэтому цвет из ответа,
 * которого здесь нет, откатывается к первому.
 */
object ForumColors {
    val values = listOf(
        "#3b82f6",
        "#059669",
        "#8b5cf6",
        "#ea580c",
        "#ec4899",
        "#0891b2",
        "#d97706",
        "#e11d48",
        "#4f46e5",
        "#65a30d"
    )

    fun safe(color: String?): String {
        val normalized = color?.trim()?.lowercase()
        return if (normalized in values) normalized!! else values.first()
    }

    fun indexOf(color: String): Int = values.indexOf(safe(color)).coerceAtLeast(0)

    /** `#rrggbb` -> [Color]. Неразобранное значение деградирует в первый цвет палитры. */
    fun toComposeColor(color: String?): Color {
        val hex = safe(color).removePrefix("#")
        val rgb = hex.toLongOrNull(radix = 16) ?: return Color(0xFF3B82F6)
        return Color(0xFF000000L or rgb)
    }
}
