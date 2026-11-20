package com.example.ysukampus.ui.admin.state

import com.example.ysukampus.data.network.ws.ModeratorWsConnectionState
import com.example.ysukampus.ui.admin.AdminDialogState
import com.example.ysukampus.ui.admin.AdminPermission
import com.example.ysukampus.ui.admin.AdminRole
import com.example.ysukampus.ui.admin.AdminTab
import com.example.ysukampus.ui.admin.AdminUser

data class AdminUiState(
    val selectedTab: AdminTab = AdminTab.Users,
    /** Верхние вкладки, открытые правами пользователя; порядок — как в вебке. */
    val availableTabs: List<AdminTab> = AdminTab.entries,
    val selectedAccessSection: AdminAccessSection = AdminAccessSection.Roles,
    val availableAccessSections: List<AdminAccessSection> = AdminAccessSection.entries,
    val selectedStatsView: AdminStatsView = AdminStatsView.Today,
    val selectedModerationTab: AdminModerationTab = AdminModerationTab.Moderation,
    val searchQuery: String = "",
    val moderationItems: List<AdminModerationItem> = emptyList(),
    val moderationReports: List<AdminModerationReport> = emptyList(),
    val moderationHistoryEvents: List<AdminModerationEvent> = emptyList(),
    val pendingModerationItems: List<AdminModerationItem> = emptyList(),
    val users: List<AdminUser> = emptyList(),
    val roles: List<AdminRole> = emptyList(),
    val permissions: List<AdminPermission> = emptyList(),
    val changedUsers: List<AdminChangedUser> = emptyList(),
    val changedRoles: List<AdminChangedRole> = emptyList(),
    val changedPermissions: List<AdminChangedPermission> = emptyList(),
    val selectedRolePermissions: List<AdminPermission> = emptyList(),
    val selectedModerationItem: AdminModerationItem? = null,
    val selectedModerationReport: AdminModerationReport? = null,
    val selectedModerationEvent: AdminModerationEvent? = null,
    /** Право `get-moderation-history`: без него подвкладки «История» просто нет. */
    val canViewModerationHistory: Boolean = true,
    /** Право `restore-hidden-content`: им открывается кнопка возврата контента. */
    val canRestoreModerationContent: Boolean = true,
    /** Прошлый заход на текущую подвкладку: всё, что новее, помечается новым. */
    val moderationSeenAtMillis: Long = 0L,
    val selectedUser: AdminUser? = null,
    val selectedRole: AdminRole? = null,
    val selectedPermission: AdminPermission? = null,
    val selectedHistoryEntityType: AdminHistoryEntityType? = null,
    val selectedHistoryEntityId: Long? = null,
    val selectedHistoryRecords: List<AdminChangeLogRecord> = emptyList(),
    val selectedHistoryRecord: AdminChangeLogRecord? = null,
    /** Живые метрики за сегодня; `null` — ещё не загружены. */
    val todayReportMetrics: Map<AdminReportMetric, Int>? = null,
    val reports: List<AdminReportListItem> = emptyList(),
    val selectedReport: AdminDailyReport? = null,
    val chartMetrics: List<AdminReportMetric> = listOf(AdminReportMetric.UsersActive),
    val chartPeriod: AdminReportPeriod = AdminReportPeriod.Month,
    val chartSeries: List<AdminReportSeries> = emptyList(),
    /** Право `view-daily-report`; без него вкладки «Статистика» просто нет. */
    val canViewReports: Boolean = true,
    /** Право `generate-daily-report`: им открывается постановка в очередь. */
    val canGenerateReports: Boolean = true,
    /** Открыта ли панель постановки отчёта в очередь. */
    val isGeneratePanelOpen: Boolean = false,
    /** Дата отчёта в панели генерации, `YYYY-MM-DD`; по умолчанию вчера. */
    val generateReportDate: String = "",
    /** Бэк ответил 409: файл за дату есть, спрашиваем про пересборку. */
    val generateNeedsForce: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val isReportsLoading: Boolean = false,
    val isTodayReportLoading: Boolean = false,
    val isChartLoading: Boolean = false,
    val isReportDetailsLoading: Boolean = false,
    val requestLogs: List<AdminRequestLogListItem> = emptyList(),
    val requestLogsPage: Int = 1,
    val requestLogsTotalPages: Int = 1,
    val requestLogsTotalCount: Int = 0,
    val requestLogsPerPage: Int = 10,
    val requestLogsFilter: AdminRequestLogsFilter = AdminRequestLogsFilter(),
    val selectedRequestLog: AdminRequestLogDetails? = null,
    val activeDialog: AdminDialogState? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isModerationLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isHistoryLoading: Boolean = false,
    val isHistoryRecordsLoading: Boolean = false,
    val isRestoringHistory: Boolean = false,
    val isRequestLogsLoading: Boolean = false,
    val isRequestLogDetailsLoading: Boolean = false,
    val isDeletingRequestLog: Boolean = false,
    val moderatorWsConnectionState: ModeratorWsConnectionState = ModeratorWsConnectionState.Disconnected,
    val hasFreshContent: Boolean = false,
    /** Живой размер очереди модерации из события `moderation_count`; `null` — пока не приходил. */
    val moderationQueueCount: Int? = null,
    val freshEventsCount: Int = 0,
    val moderationRefreshTick: Int = 0,
    val errorMessage: String? = null,
    val messageText: String? = null
)
