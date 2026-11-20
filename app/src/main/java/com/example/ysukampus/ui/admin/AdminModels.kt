package com.example.ysukampus.ui.admin

import com.example.ysukampus.ui.admin.state.AdminHistoryEntityType
import com.example.ysukampus.ui.admin.state.AdminModerationTargetType

data class AdminUser(
    /** telegram_id пользователя — за пределами диапазона Int. */
    val id: Long,
    val name: String,
    val username: String?,
    val roles: List<AdminRole>
)

data class AdminRole(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    val isDeleted: Boolean = false
)

data class AdminPermission(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    val isDeleted: Boolean = false
)

/**
 * Верхние вкладки — те же пять, что в вебке, и в том же порядке. Роли, права и
 * история живут внутри «Доступов» сегментами ([AdminAccessSection]), потому что
 * у них общий заголовок и общая природа.
 */
enum class AdminTab(val title: String) {
    Moderation("Модерация"),
    Stats("Статистика"),
    Users("Пользователи"),
    Access("Доступы"),
    Logging("Логи")
}

enum class AdminStatus { Active, SoftDeleted, HardDanger, Primary }

enum class AdminActionKind { Primary, SoftWarning, Danger, Restore }

data class AdminSelectOption(val id: String, val title: String)

sealed interface AdminDialogState {
    /** «Детали модерации» для карточки очереди или истории. */
    data class ModerationItem(
        val targetType: AdminModerationTargetType,
        val itemId: String
    ) : AdminDialogState

    /** «Детали модерации» для карточки жалобы. */
    data class ModerationReport(val reportId: String) : AdminDialogState

    /** «Событие модерации» для карточки истории. */
    data class ModerationHistoryEvent(val eventId: String) : AdminDialogState

    /** Сохранённый дневной отчёт за дату `YYYY-MM-DD`. */
    data class Report(val date: String) : AdminDialogState

    data class UserSettings(val userId: Long) : AdminDialogState
    data class RoleSettings(val roleId: Int) : AdminDialogState
    data object AddRole : AdminDialogState
    data class PermissionSettings(val permissionId: Int) : AdminDialogState
    data object AddPermission : AdminDialogState
    data class HistoryRecords(
        val entityType: AdminHistoryEntityType,
        val entityId: Long
    ) : AdminDialogState
    data class HistoryRecord(val logId: Int) : AdminDialogState
    data object RequestLogsFilter : AdminDialogState
    data class RequestLogDetails(val logId: Int) : AdminDialogState
    data class ConfirmDeleteRequestLog(val logId: Int) : AdminDialogState
}
