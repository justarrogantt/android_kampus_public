package com.example.ysukampus.ui.admin.state

import com.example.ysukampus.ui.admin.AdminPermission
import com.example.ysukampus.ui.admin.AdminRole
import com.example.ysukampus.ui.admin.AdminTab
import com.example.ysukampus.ui.admin.AdminUser

sealed interface AdminEvent {
    data class TabSelected(val tab: AdminTab) : AdminEvent
    data class ModerationTabSelected(val tab: AdminModerationTab) : AdminEvent
    data class AccessSectionSelected(val section: AdminAccessSection) : AdminEvent
    data class StatsViewSelected(val view: AdminStatsView) : AdminEvent
    /** Переключение метрики графика; больше [ADMIN_CHART_MAX_METRICS] не выбрать. */
    data class ChartMetricToggled(val metric: AdminReportMetric) : AdminEvent
    data class ChartPeriodSelected(val period: AdminReportPeriod) : AdminEvent
    data class ReportSelected(val date: String) : AdminEvent
    data object GenerateReportPanelOpened : AdminEvent
    data object GenerateReportPanelClosed : AdminEvent
    data class GenerateReportDateChanged(val date: String) : AdminEvent
    data class GenerateReportSubmitted(val force: Boolean) : AdminEvent
    data class SearchChanged(val query: String) : AdminEvent
    data object FreshModerationContentClicked : AdminEvent
    data class ModerationItemSelected(val item: AdminModerationItem) : AdminEvent
    data class ModerationReportSelected(val report: AdminModerationReport) : AdminEvent
    data class ModerationHistoryEventSelected(val event: AdminModerationEvent) : AdminEvent
    data class RestoreModerationContentClicked(
        val targetType: AdminModerationEventTargetType,
        val targetId: String
    ) : AdminEvent
    /**
     * Вердикт модератора из окна деталей: `status` — `active` или `hidden`,
     * `reportId` заполнен, только когда окно открыто из вкладки репортов.
     */
    data class ModerationVerdictClicked(
        val targetType: AdminModerationTargetType,
        val targetId: String,
        val status: String,
        val reportId: String? = null
    ) : AdminEvent
    data class UserSelected(val user: AdminUser) : AdminEvent
    data class RoleSelected(val role: AdminRole) : AdminEvent
    data class PermissionSelected(val permission: AdminPermission) : AdminEvent
    data object DialogClosed : AdminEvent
    data object CreateRoleClicked : AdminEvent
    data object CreatePermissionClicked : AdminEvent
    data object RefreshClicked : AdminEvent
    data class SaveRoleClicked(
        val roleId: Long?,
        val input: AdminRoleInput,
        val permissionIds: List<Long> = emptyList()
    ) : AdminEvent
    data class SavePermissionClicked(
        val permissionId: Long?,
        val input: AdminPermissionInput
    ) : AdminEvent
    data class AssignRoleToUserClicked(val userId: Long, val roleId: Long) : AdminEvent
    data class SoftRemoveRoleFromUserClicked(val userId: Long, val roleId: Long) : AdminEvent
    data class HardRemoveRoleFromUserClicked(val userId: Long, val roleId: Long) : AdminEvent
    data class AssignPermissionToRoleClicked(val roleId: Long, val permissionId: Long) : AdminEvent
    data class SoftRemovePermissionFromRoleClicked(val roleId: Long, val permissionId: Long) : AdminEvent
    data class HardRemovePermissionFromRoleClicked(val roleId: Long, val permissionId: Long) : AdminEvent
    data class HistoryEntitySelected(
        val entityType: AdminHistoryEntityType,
        val entityId: Long
    ) : AdminEvent
    data class HistoryRecordSelected(val recordId: Int) : AdminEvent
    data object HistoryBackToRecordsClicked : AdminEvent
    data class RestoreHistoryRecordClicked(val logId: Long) : AdminEvent
    data object HistoryRefreshClicked : AdminEvent
    data object RequestLogsRefreshClicked : AdminEvent
    data class RequestLogsPageChanged(val page: Int) : AdminEvent
    data object RequestLogsFilterClicked : AdminEvent
    data class RequestLogsFilterApplied(val filter: AdminRequestLogsFilter) : AdminEvent
    data object RequestLogsFilterReset : AdminEvent
    data class RequestLogSelected(val logId: Int) : AdminEvent
    data class DeleteRequestLogClicked(val logId: Long) : AdminEvent
    data class RequestLogDeleteConfirmationRequested(val logId: Int) : AdminEvent
    data class SoftDeleteRoleClicked(val roleId: Long) : AdminEvent
    data class HardDeleteRoleClicked(val roleId: Long) : AdminEvent
    data class RestoreRoleClicked(val roleId: Long) : AdminEvent
    data class SoftDeletePermissionClicked(val permissionId: Long) : AdminEvent
    data class HardDeletePermissionClicked(val permissionId: Long) : AdminEvent
    data class RestorePermissionClicked(val permissionId: Long) : AdminEvent
    data object ErrorConsumed : AdminEvent
    data object MessageConsumed : AdminEvent
}
