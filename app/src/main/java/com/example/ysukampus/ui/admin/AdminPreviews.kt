package com.example.ysukampus.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.admin.components.AdminEntityCard
import com.example.ysukampus.ui.admin.components.AdminHeader
import com.example.ysukampus.ui.admin.dialogs.AddPermissionDialog
import com.example.ysukampus.ui.admin.dialogs.AddRoleDialog
import com.example.ysukampus.ui.admin.dialogs.HistoryRecordDialog
import com.example.ysukampus.ui.admin.dialogs.HistoryRecordsDialog
import com.example.ysukampus.ui.admin.dialogs.PermissionSettingsDialog
import com.example.ysukampus.ui.admin.dialogs.RequestLogsFilterDialog
import com.example.ysukampus.ui.admin.dialogs.RequestLogDetailsDialog
import com.example.ysukampus.ui.admin.dialogs.RoleSettingsDialog
import com.example.ysukampus.ui.admin.dialogs.UserSettingsDialog
import com.example.ysukampus.ui.admin.screens.HistoryTabContent
import com.example.ysukampus.ui.admin.screens.LoggingTabContent
import com.example.ysukampus.ui.admin.state.AdminChangeLogActor
import com.example.ysukampus.ui.admin.state.AdminChangeLogRecord
import com.example.ysukampus.ui.admin.state.AdminChangeOperationType
import com.example.ysukampus.ui.admin.state.AdminChangedField
import com.example.ysukampus.ui.admin.state.AdminChangedPermission
import com.example.ysukampus.ui.admin.state.AdminChangedRole
import com.example.ysukampus.ui.admin.state.AdminChangedUser
import com.example.ysukampus.ui.admin.state.AdminHistoryEntityType
import com.example.ysukampus.ui.admin.state.AdminModerationEvent
import com.example.ysukampus.ui.admin.state.AdminModerationEventActor
import com.example.ysukampus.ui.admin.state.AdminModerationEventDetails
import com.example.ysukampus.ui.admin.state.AdminModerationEventTarget
import com.example.ysukampus.ui.admin.state.AdminModerationEventTargetType
import com.example.ysukampus.ui.admin.state.AdminModerationItem
import com.example.ysukampus.ui.admin.state.AdminModerationTab
import com.example.ysukampus.ui.admin.state.AdminModerationTargetType
import com.example.ysukampus.ui.admin.state.AdminRequestLogListItem
import com.example.ysukampus.ui.admin.state.AdminRequestLogDetails
import com.example.ysukampus.ui.admin.state.AdminRequestLogsFilter
import com.example.ysukampus.ui.admin.state.AdminAccessSection
import com.example.ysukampus.ui.admin.state.AdminUiState
import com.example.ysukampus.ui.theme.YsuAdminTheme

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun AdminPanelUsersPreview() {
    YsuAdminTheme { AdminPanelPreviewContent(AdminTab.Users) }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun AdminPanelRolesPreview() {
    YsuAdminTheme { AdminPanelPreviewContent(AdminTab.Access, AdminAccessSection.Roles) }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun AdminPanelPermissionsPreview() {
    YsuAdminTheme { AdminPanelPreviewContent(AdminTab.Access, AdminAccessSection.Permissions) }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun AdminPanelModerationPreview() {
    YsuAdminTheme {
        AdminPanelContent(
            uiState = previewAdminState(AdminTab.Moderation).copy(
                moderationItems = previewModerationItems,
                moderationQueueCount = previewModerationItems.size
            ),
            onEvent = {}
        )
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun AdminPanelModerationHistoryPreview() {
    YsuAdminTheme {
        AdminPanelContent(
            uiState = previewAdminState(AdminTab.Moderation).copy(
                selectedModerationTab = AdminModerationTab.History,
                moderationItems = previewModerationItems,
                moderationHistoryEvents = previewModerationEvents
            ),
            onEvent = {}
        )
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun AdminPanelHistoryPreview() {
    YsuAdminTheme {
        AdminPanelContent(
            uiState = previewAdminState(AdminTab.Access, AdminAccessSection.History).copy(
                changedRoles = previewChangedRoles,
                changedPermissions = previewChangedPermissions,
                changedUsers = previewChangedUsers
            ),
            onEvent = {}
        )
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun UserSettingsDialogPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame {
            UserSettingsDialog(onClose = {})
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun UserSettingsDialogSelectOpenPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame {
            UserSettingsDialog(onClose = {}, forceSelectExpanded = true)
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun UserSettingsDialogSelectedRolePreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame {
            UserSettingsDialog(onClose = {}, initialSelectedRoleId = 1)
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun RoleSettingsDialogPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Access, accessSection = AdminAccessSection.Roles) {
            RoleSettingsDialog(onClose = {})
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun AddRoleDialogPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Access, accessSection = AdminAccessSection.Roles) {
            AddRoleDialog(onClose = {})
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun AddPermissionDialogPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Access, accessSection = AdminAccessSection.Permissions) {
            AddPermissionDialog(onClose = {})
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun PermissionSettingsDialogPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Access, accessSection = AdminAccessSection.Permissions) {
            PermissionSettingsDialog(onClose = {})
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun HistoryRecordsDialogPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Access, accessSection = AdminAccessSection.History) {
            HistoryRecordsDialog(
                records = previewHistoryRecords,
                isLoading = false,
                onRecordClick = {},
                onClose = {}
            )
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun HistoryRecordDialogRestorablePreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Access, accessSection = AdminAccessSection.History) {
            HistoryRecordDialog(
                record = previewHistoryRecords.first(),
                isRestoring = false,
                onBack = {},
                onRestore = {},
                onClose = {}
            )
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun HistoryRecordDialogReadonlyPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Access, accessSection = AdminAccessSection.History) {
            HistoryRecordDialog(
                record = previewHistoryRecords.last(),
                isRestoring = false,
                onBack = {},
                onRestore = {},
                onClose = {}
            )
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun RequestLogsFilterDialogDefaultPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Logging) {
            RequestLogsFilterDialog(
                filter = AdminRequestLogsFilter(),
                onApply = {},
                onReset = {},
                onClose = {}
            )
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun RequestLogsFilterDialogFilledPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Logging) {
            RequestLogsFilterDialog(
                filter = AdminRequestLogsFilter(
                    search = "users",
                    userId = "12",
                    responseStatus = "404",
                    ipAddress = "127.0.0.1",
                    controllerPath = "/api/ref/user/",
                    method = "GET",
                    sortBy = "response_status",
                    sortOrder = "desc",
                    statusGroup = "4xx",
                    perPage = 20
                ),
                onApply = {},
                onReset = {},
                onClose = {}
            )
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun RequestLogDetailsDialogLoadingPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Logging) {
            RequestLogDetailsDialog(
                log = null,
                isLoading = true,
                isDeleting = false,
                onDelete = {},
                onClose = {}
            )
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun RequestLogDetailsDialogRequestPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Logging) {
            RequestLogDetailsDialog(
                log = previewRequestLogDetails,
                isLoading = false,
                isDeleting = false,
                onDelete = {},
                onClose = {}
            )
        }
    }
}

@Preview(widthDp = 382, heightDp = 825, showBackground = true)
@Composable
fun RequestLogDetailsDialogMissingPreview() {
    YsuAdminTheme {
        AdminDialogOverlayPreviewFrame(initialTab = AdminTab.Logging) {
            RequestLogDetailsDialog(
                log = null,
                isLoading = false,
                isDeleting = false,
                onDelete = {},
                onClose = {}
            )
        }
    }
}

@Composable
private fun AdminDialogOverlayPreviewFrame(
    initialTab: AdminTab = AdminTab.Users,
    accessSection: AdminAccessSection = AdminAccessSection.Roles,
    dialog: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        AdminPanelContent(
            uiState = previewAdminState(initialTab, accessSection),
            onEvent = {},
            modifier = Modifier
                .fillMaxSize()
                .blur(7.dp)
        )
        dialog()
    }
}

@Preview(showBackground = true)
@Composable
fun AdminCardComponentPreview() {
    YsuAdminTheme {
        AdminEntityCard("◇", "Admin", "admin", "активна", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun AdminHeaderComponentPreview() {
    YsuAdminTheme { AdminHeader() }
}

@Preview(showBackground = true)
@Composable
fun HistoryTabContentPreview() {
    YsuAdminTheme {
        HistoryTabContent(
            changedUsers = previewChangedUsers,
            changedRoles = previewChangedRoles,
            changedPermissions = previewChangedPermissions
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryTabEmptyPreview() {
    YsuAdminTheme {
        HistoryTabContent(
            changedUsers = emptyList(),
            changedRoles = emptyList(),
            changedPermissions = emptyList()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryTabSearchEmptyPreview() {
    YsuAdminTheme {
        HistoryTabContent(
            changedUsers = previewChangedUsers,
            changedRoles = previewChangedRoles,
            changedPermissions = previewChangedPermissions,
            searchQuery = "ничего"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoggingTabContentPreview() {
    YsuAdminTheme {
        LoggingTabContent(
            logs = previewRequestLogs,
            searchQuery = "",
            isLoading = false,
            currentPage = 1,
            totalPages = 3,
            onSearchQueryChange = {},
            onLogClick = {},
            onPreviousPage = {},
            onNextPage = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoggingTabLoadingPreview() {
    YsuAdminTheme {
        LoggingTabContent(
            logs = emptyList(),
            searchQuery = "",
            isLoading = true,
            currentPage = 1,
            totalPages = 1,
            onSearchQueryChange = {},
            onLogClick = {},
            onPreviousPage = {},
            onNextPage = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoggingTabEmptyPreview() {
    YsuAdminTheme {
        LoggingTabContent(
            logs = emptyList(),
            searchQuery = "",
            isLoading = false,
            currentPage = 1,
            totalPages = 1,
            onSearchQueryChange = {},
            onLogClick = {},
            onPreviousPage = {},
            onNextPage = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoggingTabSearchEmptyPreview() {
    YsuAdminTheme {
        LoggingTabContent(
            logs = previewRequestLogs,
            searchQuery = "zzz",
            isLoading = false,
            currentPage = 1,
            totalPages = 3,
            onSearchQueryChange = {},
            onLogClick = {},
            onPreviousPage = {},
            onNextPage = {}
        )
    }
}

@Composable
private fun AdminPanelPreviewContent(
    initialTab: AdminTab,
    accessSection: AdminAccessSection = AdminAccessSection.Roles
) {
    AdminPanelContent(
        uiState = previewAdminState(initialTab, accessSection),
        onEvent = {}
    )
}

private fun previewAdminState(
    initialTab: AdminTab,
    accessSection: AdminAccessSection = AdminAccessSection.Roles
): AdminUiState =
    AdminUiState(
        selectedTab = initialTab,
        selectedAccessSection = accessSection,
        users = MockUsers,
        roles = MockRoles,
        permissions = MockPermissions
    )

private val previewModerationItems = listOf(
    AdminModerationItem(
        id = "p-201",
        targetType = AdminModerationTargetType.Post,
        authorName = "rezak159",
        authorColor = "#3B82F6",
        content = "Продам монстер вейпер 80мг",
        status = "review",
        createdAt = "2026-06-16T22:52:00Z",
        zone = "red",
        aiScore = 91.0,
        aiReason = "Подозрение на запрещённую продажу"
    ),
    AdminModerationItem(
        id = "c-410",
        targetType = AdminModerationTargetType.Comment,
        postId = "p-198",
        authorName = "ИИ",
        content = "Хо-ро-шо!",
        status = "pending",
        createdAt = "2026-06-15T22:33:00Z"
    )
)

private val previewModerationEvents = listOf(
    AdminModerationEvent(
        id = "chg_2",
        ts = "2026-06-16T23:04:00Z",
        action = "hide",
        actor = AdminModerationEventActor(kind = "moderator", name = "Администратор"),
        target = AdminModerationEventTarget(
            type = AdminModerationEventTargetType.Post,
            id = "p-201",
            preview = "Продам монстер вейпер 80мг",
            content = "Продам монстер вейпер 80мг",
            author = "rezak159",
            authorColor = "#3B82F6",
            status = "hidden"
        ),
        details = AdminModerationEventDetails(fromStatus = "review", toStatus = "hidden")
    ),
    AdminModerationEvent(
        id = "ai_1",
        ts = "2026-06-16T22:52:10Z",
        action = "ai_verdict",
        actor = AdminModerationEventActor(kind = "ai", name = "ИИ"),
        target = AdminModerationEventTarget(
            type = AdminModerationEventTargetType.Post,
            id = "p-201",
            preview = "Продам монстер вейпер 80мг",
            author = "rezak159"
        ),
        details = AdminModerationEventDetails(zone = "red", score = 91, reason = "Запрещённая продажа")
    )
)

private val previewChangedUsers = listOf(
    AdminChangedUser(
        id = 1,
        username = "demo_user",
        firstName = "Егор",
        lastChangedAt = "2026-05-18T22:40:00Z"
    )
)

private val previewChangedRoles = listOf(
    AdminChangedRole(
        id = 1,
        name = "ert",
        slug = "ert",
        lastChangedAt = "2026-05-18T22:33:00Z",
        isDeleted = false
    )
)

private val previewChangedPermissions = listOf(
    AdminChangedPermission(
        id = 2,
        name = "update-user",
        slug = "update-user",
        lastChangedAt = "2026-05-18T22:33:00Z",
        isDeleted = false
    ),
    AdminChangedPermission(
        id = 3,
        name = "delete-user",
        slug = "delete-user",
        lastChangedAt = "2026-05-18T22:07:00Z",
        isDeleted = false
    ),
    AdminChangedPermission(
        id = 4,
        name = "read-role",
        slug = "read-role",
        lastChangedAt = "2026-05-18T19:51:00Z",
        isDeleted = false
    )
)

private val previewHistoryActor = AdminChangeLogActor(
    id = 10001,
    username = "demo_user",
    firstName = "Егор"
)

private val previewHistoryRecords = listOf(
    AdminChangeLogRecord(
        id = 7,
        entityType = AdminHistoryEntityType.Permission,
        entityId = 7,
        operationType = AdminChangeOperationType.Create,
        canRestore = true,
        changedFields = mapOf(
            "name" to AdminChangedField(oldValue = "", newValue = "ert"),
            "slug" to AdminChangedField(oldValue = "", newValue = "ert")
        ),
        createdAt = "2026-05-18T22:33:00Z",
        createdBy = previewHistoryActor.id,
        actor = previewHistoryActor
    ),
    AdminChangeLogRecord(
        id = 6,
        entityType = AdminHistoryEntityType.Permission,
        entityId = 6,
        operationType = AdminChangeOperationType.Create,
        canRestore = false,
        changedFields = mapOf(
            "slug" to AdminChangedField(oldValue = "", newValue = "delete-user")
        ),
        createdAt = "2026-05-18T22:07:00Z",
        createdBy = previewHistoryActor.id,
        actor = previewHistoryActor
    ),
    AdminChangeLogRecord(
        id = 5,
        entityType = AdminHistoryEntityType.Role,
        entityId = 1,
        operationType = AdminChangeOperationType.Update,
        canRestore = true,
        changedFields = mapOf(
            "name" to AdminChangedField(oldValue = "read", newValue = "read-role"),
            "slug" to AdminChangedField(oldValue = "read", newValue = "read-role")
        ),
        createdAt = "2026-05-18T19:51:00Z",
        createdBy = previewHistoryActor.id,
        actor = previewHistoryActor
    )
)

private val previewRequestLogs = listOf(
    AdminRequestLogListItem(
        id = 11,
        fullUrl = "http://127.0.0.1:8000/api/ref/log/request",
        method = "post",
        controllerPath = "/api/ref/log/request",
        controllerMethod = "get_logs_route",
        responseStatus = 200,
        calledAt = "2026-05-19T16:09:00Z"
    ),
    AdminRequestLogListItem(
        id = 12,
        fullUrl = "http://127.0.0.1:8000/api/ref/changelog/changed/permission",
        method = "get",
        controllerPath = "/api/ref/changelog/changed/permission",
        controllerMethod = "get_changed_permissions_route",
        responseStatus = 404,
        calledAt = "2026-05-19T16:08:00Z"
    ),
    AdminRequestLogListItem(
        id = 13,
        fullUrl = "http://127.0.0.1:8000/api/ref/user/",
        method = "get",
        controllerPath = "/api/ref/user/",
        controllerMethod = "get_users_route",
        responseStatus = 500,
        calledAt = "2026-05-19T16:07:00Z"
    )
)

private val previewRequestLogDetails = AdminRequestLogDetails(
    id = 11,
    fullUrl = "http://127.0.0.1:8000/api/ref/log/request?page=1",
    method = "POST",
    controllerPath = "/api/ref/log/request",
    controllerMethod = "get_logs_route",
    requestBody = """{"page":1,"per_page":10}""",
    requestHeaders = """{"authorization":"Bearer ***","x-device-id":"android-device"}""",
    userId = 12,
    ipAddress = "127.0.0.1",
    userAgent = "okhttp/4.12.0",
    responseStatus = 200,
    responseBody = """{"items":[],"total_count":0}""",
    responseHeaders = """{"content-type":"application/json"}""",
    calledAt = "2026-05-19T16:09:00Z"
)
