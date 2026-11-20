package com.example.ysukampus.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ysukampus.ui.admin.components.AdminFeedbackCard
import com.example.ysukampus.ui.admin.components.AdminHeader
import com.example.ysukampus.ui.admin.components.AdminLoadingStateCard
import com.example.ysukampus.ui.admin.components.AdminEmptyStateCard
import com.example.ysukampus.ui.admin.components.AdminTabHeader
import com.example.ysukampus.ui.admin.dialogs.AddPermissionDialog
import com.example.ysukampus.ui.admin.dialogs.AddRoleDialog
import com.example.ysukampus.ui.admin.dialogs.HistoryRecordDialog
import com.example.ysukampus.ui.admin.dialogs.ModerationHistoryEventDialog
import com.example.ysukampus.ui.admin.dialogs.ModerationItemDialog
import com.example.ysukampus.ui.admin.dialogs.ModerationReportDialog
import com.example.ysukampus.ui.admin.dialogs.HistoryRecordsDialog
import com.example.ysukampus.ui.admin.dialogs.PermissionSettingsDialog
import com.example.ysukampus.ui.admin.dialogs.RequestLogsFilterDialog
import com.example.ysukampus.ui.admin.dialogs.RequestLogDetailsDialog
import com.example.ysukampus.ui.admin.dialogs.RoleSettingsDialog
import com.example.ysukampus.ui.admin.dialogs.UserSettingsDialog
import com.example.ysukampus.ui.admin.screens.ModerationTabContent
import com.example.ysukampus.ui.admin.data.AdminDataSourceMode
import com.example.ysukampus.ui.admin.data.AdminRepositoryProvider
import com.example.ysukampus.ui.admin.dialogs.ConfirmDeleteRequestLogDialog
import com.example.ysukampus.ui.admin.screens.HistoryTabContent
import com.example.ysukampus.ui.admin.screens.LoggingTabContent
import com.example.ysukampus.ui.admin.dialogs.ReportDetailsDialog
import com.example.ysukampus.ui.admin.screens.AccessTabContent
import com.example.ysukampus.ui.admin.screens.PermissionsTabContent
import com.example.ysukampus.ui.admin.screens.StatsTabContent
import com.example.ysukampus.ui.admin.state.AdminAccessSection
import com.example.ysukampus.ui.admin.screens.RolesTabContent
import com.example.ysukampus.ui.admin.screens.UsersTabContent
import com.example.ysukampus.ui.admin.state.AdminEvent
import com.example.ysukampus.ui.admin.state.AdminUiState
import com.example.ysukampus.ui.theme.YsuAdminColors
import com.example.ysukampus.ui.theme.YsuAdminDimens

@Composable
fun AdminPanelScreen(
    onOpenMain: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        // Fake mode remains available in AdminRepositoryProvider for local UI development.
        AdminRepositoryProvider.create(context = context, mode = AdminDataSourceMode.Remote)
    }
    val factory = remember(repository) {
        AdminViewModelFactory(repository)
    }
    val viewModel: AdminViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    AdminPanelContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenMain = onOpenMain,
        modifier = modifier
    )
}

@Composable
fun AdminPanelContent(
    uiState: AdminUiState,
    onEvent: (AdminEvent) -> Unit,
    onOpenMain: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(uiState.selectedTab, uiState.moderationRefreshTick) {
        if (uiState.selectedTab == AdminTab.Moderation && uiState.moderationRefreshTick > 0) {
            scrollState.animateScrollTo(0)
        }
    }
    val accessTotal = when (uiState.selectedAccessSection) {
        AdminAccessSection.Roles -> uiState.roles.size
        AdminAccessSection.Permissions -> uiState.permissions.size
        AdminAccessSection.History ->
            uiState.changedUsers.size + uiState.changedRoles.size + uiState.changedPermissions.size
    }
    val totals = mapOf(
        // Вебка считает «Всего» для модерации как очередь + жалобы, независимо
        // от выбранной подвкладки.
        AdminTab.Moderation to (uiState.moderationItems.size + uiState.moderationReports.size),
        AdminTab.Stats to uiState.reports.size,
        AdminTab.Users to uiState.users.size,
        // У «Доступов» общий заголовок, но счётчик — по открытому разделу.
        AdminTab.Access to accessTotal,
        AdminTab.Logging to uiState.requestLogsTotalCount
    )
    val showInitialLoading = when (uiState.selectedTab) {
        AdminTab.Moderation -> uiState.isModerationLoading &&
            uiState.moderationItems.isEmpty() &&
            uiState.moderationReports.isEmpty() &&
            uiState.moderationHistoryEvents.isEmpty()
        AdminTab.Stats -> uiState.isReportsLoading && uiState.reports.isEmpty()
        else -> uiState.isLoading &&
            uiState.users.isEmpty() &&
            uiState.roles.isEmpty() &&
            uiState.permissions.isEmpty()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (uiState.activeDialog != null) 7.dp else 0.dp)
                .background(YsuAdminColors.Background)
                .statusBarsPadding()
                .padding(horizontal = YsuAdminDimens.ScreenPadding)
        ) {
            AdminHeader(
                modifier = Modifier.padding(top = YsuAdminDimens.HeaderTopPadding),
                onOpenMain = onOpenMain
            )
            Spacer(Modifier.height(YsuAdminDimens.HeaderBottomSpacing))
            AdminTabHeader(
                currentTab = uiState.selectedTab,
                tabs = uiState.availableTabs,
                totals = totals,
                onTabSelected = { onEvent(AdminEvent.TabSelected(it)) },
                onAddClick = when {
                    uiState.selectedTab != AdminTab.Access -> null
                    uiState.selectedAccessSection == AdminAccessSection.Roles ->
                        ({ onEvent(AdminEvent.CreateRoleClicked) })
                    uiState.selectedAccessSection == AdminAccessSection.Permissions ->
                        ({ onEvent(AdminEvent.CreatePermissionClicked) })
                    else -> null
                },
                onSecondaryActionClick = if (uiState.selectedTab == AdminTab.Logging) {
                    { onEvent(AdminEvent.RequestLogsFilterClicked) }
                } else {
                    null
                },
                secondaryActionIconText = "⌯",
                isRefreshing = uiState.isRefreshing,
                onRefreshClick = {
                    if (uiState.selectedTab == AdminTab.Logging) {
                        onEvent(AdminEvent.RequestLogsRefreshClicked)
                    } else {
                        onEvent(AdminEvent.RefreshClicked)
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            uiState.errorMessage?.let { errorMessage ->
                AdminFeedbackCard(
                    title = "Ошибка",
                    message = errorMessage,
                    isError = true,
                    onDismiss = { onEvent(AdminEvent.ErrorConsumed) }
                )
                Spacer(Modifier.height(8.dp))
            }
            uiState.messageText?.let { messageText ->
                AdminFeedbackCard(
                    title = "Готово",
                    message = messageText,
                    isError = false,
                    onDismiss = { onEvent(AdminEvent.MessageConsumed) }
                )
                Spacer(Modifier.height(8.dp))
            }
            Column(Modifier.weight(1f).verticalScroll(scrollState)) {
                if (showInitialLoading) {
                    AdminLoadingStateCard()
                } else {
                    when (uiState.selectedTab) {
                        AdminTab.Moderation -> ModerationTabContent(
                            selectedTab = uiState.selectedModerationTab,
                            moderationItems = uiState.moderationItems,
                            moderationReports = uiState.moderationReports,
                            moderationHistoryEvents = uiState.moderationHistoryEvents,
                            searchQuery = uiState.searchQuery,
                            isLoading = uiState.isModerationLoading,
                            hasFreshContent = uiState.hasFreshContent,
                            freshEventsCount = uiState.freshEventsCount,
                            moderationQueueCount = uiState.moderationQueueCount,
                            canViewHistory = uiState.canViewModerationHistory,
                            seenAtMillis = uiState.moderationSeenAtMillis,
                            onSearchQueryChange = { onEvent(AdminEvent.SearchChanged(it)) },
                            onModerationTabSelected = { onEvent(AdminEvent.ModerationTabSelected(it)) },
                            onFreshContentClick = { onEvent(AdminEvent.FreshModerationContentClicked) },
                            onModerationItemClick = { onEvent(AdminEvent.ModerationItemSelected(it)) },
                            onModerationReportClick = { onEvent(AdminEvent.ModerationReportSelected(it)) },
                            onModerationEventClick = { onEvent(AdminEvent.ModerationHistoryEventSelected(it)) }
                        )
                        AdminTab.Users -> UsersTabContent(
                            users = uiState.users,
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { onEvent(AdminEvent.SearchChanged(it)) },
                            onUserClick = { onEvent(AdminEvent.UserSelected(it)) }
                        )
                        AdminTab.Stats -> StatsTabContent(
                            selectedView = uiState.selectedStatsView,
                            todayMetrics = uiState.todayReportMetrics,
                            reports = uiState.reports,
                            chartMetrics = uiState.chartMetrics,
                            chartPeriod = uiState.chartPeriod,
                            chartSeries = uiState.chartSeries,
                            isReportsLoading = uiState.isReportsLoading,
                            isTodayLoading = uiState.isTodayReportLoading,
                            isChartLoading = uiState.isChartLoading,
                            canGenerate = uiState.canGenerateReports,
                            isGeneratePanelOpen = uiState.isGeneratePanelOpen,
                            generateDate = uiState.generateReportDate,
                            generateNeedsForce = uiState.generateNeedsForce,
                            isGenerating = uiState.isGeneratingReport,
                            onViewSelected = { onEvent(AdminEvent.StatsViewSelected(it)) },
                            onReportClick = { onEvent(AdminEvent.ReportSelected(it)) },
                            onMetricToggle = { onEvent(AdminEvent.ChartMetricToggled(it)) },
                            onPeriodSelected = { onEvent(AdminEvent.ChartPeriodSelected(it)) },
                            onGeneratePanelOpen = { onEvent(AdminEvent.GenerateReportPanelOpened) },
                            onGeneratePanelClose = { onEvent(AdminEvent.GenerateReportPanelClosed) },
                            onGenerateDateChange = { onEvent(AdminEvent.GenerateReportDateChanged(it)) },
                            onGenerateSubmit = { force -> onEvent(AdminEvent.GenerateReportSubmitted(force)) }
                        )
                        AdminTab.Access -> AccessTabContent(
                            selectedSection = uiState.selectedAccessSection,
                            availableSections = uiState.availableAccessSections,
                            roles = uiState.roles,
                            permissions = uiState.permissions,
                            changedUsers = uiState.changedUsers,
                            changedRoles = uiState.changedRoles,
                            changedPermissions = uiState.changedPermissions,
                            searchQuery = uiState.searchQuery,
                            isHistoryLoading = uiState.isHistoryLoading,
                            onSectionSelected = { onEvent(AdminEvent.AccessSectionSelected(it)) },
                            onSearchQueryChange = { onEvent(AdminEvent.SearchChanged(it)) },
                            onRoleClick = { onEvent(AdminEvent.RoleSelected(it)) },
                            onPermissionClick = { onEvent(AdminEvent.PermissionSelected(it)) },
                            onHistoryEntityClick = { entityType, entityId ->
                                onEvent(AdminEvent.HistoryEntitySelected(entityType, entityId))
                            }
                        )
                        AdminTab.Logging -> LoggingTabContent(
                            logs = uiState.requestLogs,
                            searchQuery = uiState.searchQuery,
                            isLoading = uiState.isRequestLogsLoading,
                            currentPage = uiState.requestLogsPage,
                            totalPages = uiState.requestLogsTotalPages,
                            onSearchQueryChange = { onEvent(AdminEvent.SearchChanged(it)) },
                            onLogClick = { logId -> onEvent(AdminEvent.RequestLogSelected(logId)) },
                            onPreviousPage = {
                                if (uiState.requestLogsPage > 1) {
                                    onEvent(AdminEvent.RequestLogsPageChanged(uiState.requestLogsPage - 1))
                                }
                            },
                            onNextPage = {
                                if (uiState.requestLogsPage < uiState.requestLogsTotalPages) {
                                    onEvent(AdminEvent.RequestLogsPageChanged(uiState.requestLogsPage + 1))
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        when (val dialog = uiState.activeDialog) {
            is AdminDialogState.ModerationItem -> {
                val item = uiState.selectedModerationItem
                    ?: uiState.moderationItems.firstOrNull {
                        it.targetType == dialog.targetType && it.id == dialog.itemId
                    }
                if (item != null) {
                    ModerationItemDialog(
                        item = item,
                        isSaving = uiState.isSaving,
                        onVerdict = { status ->
                            onEvent(
                                AdminEvent.ModerationVerdictClicked(
                                    targetType = item.targetType,
                                    targetId = item.id,
                                    status = status
                                )
                            )
                        },
                        onClose = { onEvent(AdminEvent.DialogClosed) }
                    )
                } else {
                    LaunchedEffect(dialog) {
                        onEvent(AdminEvent.DialogClosed)
                    }
                }
            }
            is AdminDialogState.ModerationReport -> {
                val report = uiState.selectedModerationReport
                    ?: uiState.moderationReports.firstOrNull { it.id == dialog.reportId }
                if (report != null) {
                    ModerationReportDialog(
                        report = report,
                        isSaving = uiState.isSaving,
                        onVerdict = { status ->
                            onEvent(
                                AdminEvent.ModerationVerdictClicked(
                                    targetType = report.targetType,
                                    targetId = report.targetId,
                                    status = status,
                                    reportId = report.id
                                )
                            )
                        },
                        onClose = { onEvent(AdminEvent.DialogClosed) }
                    )
                } else {
                    LaunchedEffect(dialog) {
                        onEvent(AdminEvent.DialogClosed)
                    }
                }
            }
            is AdminDialogState.ModerationHistoryEvent -> {
                val event = uiState.selectedModerationEvent
                    ?: uiState.moderationHistoryEvents.firstOrNull { it.id == dialog.eventId }
                if (event != null) {
                    ModerationHistoryEventDialog(
                        event = event,
                        canRestore = uiState.canRestoreModerationContent,
                        isRestoring = uiState.isSaving,
                        onRestore = {
                            onEvent(
                                AdminEvent.RestoreModerationContentClicked(
                                    targetType = event.target.type,
                                    targetId = event.target.id
                                )
                            )
                        },
                        onClose = { onEvent(AdminEvent.DialogClosed) }
                    )
                } else {
                    LaunchedEffect(dialog) {
                        onEvent(AdminEvent.DialogClosed)
                    }
                }
            }
            is AdminDialogState.Report -> ReportDetailsDialog(
                date = dialog.date,
                report = uiState.selectedReport,
                isLoading = uiState.isReportDetailsLoading,
                onClose = { onEvent(AdminEvent.DialogClosed) }
            )
            is AdminDialogState.UserSettings -> {
                val user = uiState.selectedUser ?: uiState.users.firstOrNull { it.id == dialog.userId }
                if (user != null) {
                    UserSettingsDialog(
                        user = user,
                        roles = uiState.roles,
                        onAssignRole = { userId, roleId ->
                            onEvent(AdminEvent.AssignRoleToUserClicked(userId, roleId))
                        },
                        onSoftRemoveRole = { userId, roleId ->
                            onEvent(AdminEvent.SoftRemoveRoleFromUserClicked(userId, roleId))
                        },
                        onHardRemoveRole = { userId, roleId ->
                            onEvent(AdminEvent.HardRemoveRoleFromUserClicked(userId, roleId))
                        },
                        isSaving = uiState.isSaving,
                        isDeleting = uiState.isDeleting,
                        onClose = { onEvent(AdminEvent.DialogClosed) }
                    )
                } else {
                    LaunchedEffect(dialog) {
                        onEvent(AdminEvent.DialogClosed)
                    }
                }
            }
            is AdminDialogState.RoleSettings -> {
                val role = uiState.selectedRole ?: uiState.roles.firstOrNull { it.id == dialog.roleId }
                if (role != null) {
                    RoleSettingsDialog(
                        role = role,
                        permissions = uiState.permissions,
                        assignedPermissions = uiState.selectedRolePermissions,
                        onClose = { onEvent(AdminEvent.DialogClosed) },
                        onSave = { roleId, input ->
                            onEvent(AdminEvent.SaveRoleClicked(roleId = roleId, input = input))
                        },
                        onAssignPermission = { roleId, permissionId ->
                            onEvent(AdminEvent.AssignPermissionToRoleClicked(roleId, permissionId))
                        },
                        onSoftRemovePermission = { roleId, permissionId ->
                            onEvent(AdminEvent.SoftRemovePermissionFromRoleClicked(roleId, permissionId))
                        },
                        onHardRemovePermission = { roleId, permissionId ->
                            onEvent(AdminEvent.HardRemovePermissionFromRoleClicked(roleId, permissionId))
                        },
                        onSoftDelete = { roleId -> onEvent(AdminEvent.SoftDeleteRoleClicked(roleId)) },
                        onHardDelete = { roleId -> onEvent(AdminEvent.HardDeleteRoleClicked(roleId)) },
                        onRestore = { roleId -> onEvent(AdminEvent.RestoreRoleClicked(roleId)) },
                        isSaving = uiState.isSaving,
                        isDeleting = uiState.isDeleting
                    )
                } else {
                    LaunchedEffect(dialog) {
                        onEvent(AdminEvent.DialogClosed)
                    }
                }
            }
            AdminDialogState.AddRole -> AddRoleDialog(
                permissions = uiState.permissions,
                onClose = { onEvent(AdminEvent.DialogClosed) },
                onSave = { input, permissionIds ->
                    onEvent(
                        AdminEvent.SaveRoleClicked(
                            roleId = null,
                            input = input,
                            permissionIds = permissionIds
                        )
                    )
                },
                isSaving = uiState.isSaving
            )
            is AdminDialogState.PermissionSettings -> {
                val permission = uiState.selectedPermission
                    ?: uiState.permissions.firstOrNull { it.id == dialog.permissionId }
                if (permission != null) {
                    PermissionSettingsDialog(
                        permission = permission,
                        onClose = { onEvent(AdminEvent.DialogClosed) },
                        onSave = { permissionId, input ->
                            onEvent(AdminEvent.SavePermissionClicked(permissionId = permissionId, input = input))
                        },
                        onSoftDelete = { permissionId ->
                            onEvent(AdminEvent.SoftDeletePermissionClicked(permissionId))
                        },
                        onHardDelete = { permissionId ->
                            onEvent(AdminEvent.HardDeletePermissionClicked(permissionId))
                        },
                        onRestore = { permissionId ->
                            onEvent(AdminEvent.RestorePermissionClicked(permissionId))
                        },
                        isSaving = uiState.isSaving,
                        isDeleting = uiState.isDeleting
                    )
                } else {
                    LaunchedEffect(dialog) {
                        onEvent(AdminEvent.DialogClosed)
                    }
                }
            }
            AdminDialogState.AddPermission -> AddPermissionDialog(
                onClose = { onEvent(AdminEvent.DialogClosed) },
                onSave = { input ->
                    onEvent(AdminEvent.SavePermissionClicked(permissionId = null, input = input))
                },
                isSaving = uiState.isSaving
            )
            is AdminDialogState.HistoryRecords -> HistoryRecordsDialog(
                records = uiState.selectedHistoryRecords,
                isLoading = uiState.isHistoryRecordsLoading,
                onRecordClick = { recordId -> onEvent(AdminEvent.HistoryRecordSelected(recordId)) },
                onClose = { onEvent(AdminEvent.DialogClosed) }
            )
            is AdminDialogState.HistoryRecord -> {
                val record = uiState.selectedHistoryRecord
                    ?: uiState.selectedHistoryRecords.firstOrNull { it.id == dialog.logId }
                if (record != null) {
                    HistoryRecordDialog(
                        record = record,
                        isRestoring = uiState.isRestoringHistory,
                        onBack = { onEvent(AdminEvent.HistoryBackToRecordsClicked) },
                        onRestore = { logId -> onEvent(AdminEvent.RestoreHistoryRecordClicked(logId)) },
                        onClose = { onEvent(AdminEvent.DialogClosed) }
                    )
                } else {
                    LaunchedEffect(dialog) {
                        onEvent(AdminEvent.HistoryBackToRecordsClicked)
                    }
                }
            }
            AdminDialogState.RequestLogsFilter -> RequestLogsFilterDialog(
                filter = uiState.requestLogsFilter,
                onApply = { onEvent(AdminEvent.RequestLogsFilterApplied(it)) },
                onReset = { onEvent(AdminEvent.RequestLogsFilterReset) },
                onClose = { onEvent(AdminEvent.DialogClosed) }
            )
            is AdminDialogState.RequestLogDetails -> RequestLogDetailsDialog(
                log = uiState.selectedRequestLog,
                isLoading = uiState.isRequestLogDetailsLoading,
                isDeleting = uiState.isDeletingRequestLog,
                onDelete = { onEvent(AdminEvent.RequestLogDeleteConfirmationRequested(dialog.logId)) },
                onClose = { onEvent(AdminEvent.DialogClosed) }
            )
            is AdminDialogState.ConfirmDeleteRequestLog -> ConfirmDeleteRequestLogDialog(
                logId = dialog.logId,
                isDeleting = uiState.isDeletingRequestLog,
                onConfirm = { id -> onEvent(AdminEvent.DeleteRequestLogClicked(id.toLong()))},
                onCancel = { onEvent(AdminEvent.DialogClosed)}
            )
            null -> Unit
        }
    }
}
