package com.example.ysukampus.ui.admin.data

import com.example.ysukampus.data.network.ws.ModeratorWsConnectionState
import com.example.ysukampus.data.network.ws.ModeratorWsEvent
import com.example.ysukampus.ui.admin.AdminPermission
import com.example.ysukampus.ui.admin.AdminRole
import com.example.ysukampus.ui.admin.AdminUser
import com.example.ysukampus.ui.admin.MockPermissions
import com.example.ysukampus.ui.admin.MockRoles
import com.example.ysukampus.ui.admin.MockUsers
import com.example.ysukampus.ui.admin.state.AdminChangeLogActor
import com.example.ysukampus.ui.admin.state.AdminChangeLogRecord
import com.example.ysukampus.ui.admin.state.AdminChangeOperationType
import com.example.ysukampus.ui.admin.state.AdminChangedField
import com.example.ysukampus.ui.admin.state.AdminChangedPermission
import com.example.ysukampus.ui.admin.state.AdminChangedRole
import com.example.ysukampus.ui.admin.state.AdminChangedUser
import com.example.ysukampus.ui.admin.state.AdminDailyReport
import com.example.ysukampus.ui.admin.state.AdminGenerateReportResult
import com.example.ysukampus.ui.admin.state.AdminReportJobStatus
import com.example.ysukampus.ui.admin.state.AdminReportListItem
import com.example.ysukampus.ui.admin.state.AdminReportMetric
import com.example.ysukampus.ui.admin.state.AdminReportSeries
import com.example.ysukampus.ui.admin.state.AdminReportSeriesPoint
import com.example.ysukampus.ui.admin.state.AdminHistoryEntityType
import com.example.ysukampus.ui.admin.state.AdminModerationEvent
import com.example.ysukampus.ui.admin.state.AdminModerationEventActor
import com.example.ysukampus.ui.admin.state.AdminModerationEventDetails
import com.example.ysukampus.ui.admin.state.AdminModerationEventTarget
import com.example.ysukampus.ui.admin.state.AdminModerationEventTargetType
import com.example.ysukampus.ui.admin.state.AdminModerationItem
import com.example.ysukampus.ui.admin.state.AdminModerationReport
import com.example.ysukampus.ui.admin.state.AdminModerationTab
import com.example.ysukampus.ui.admin.state.AdminModerationTargetType
import com.example.ysukampus.ui.admin.state.AdminPermissionInput
import com.example.ysukampus.ui.admin.state.AdminRequestLogDetails
import com.example.ysukampus.ui.admin.state.AdminRequestLogListItem
import com.example.ysukampus.ui.admin.state.AdminRequestLogsFilter
import com.example.ysukampus.ui.admin.state.AdminRequestLogsPage
import com.example.ysukampus.ui.admin.state.AdminRestoreChangeLogResult
import com.example.ysukampus.ui.admin.state.AdminRoleInput
import com.example.ysukampus.ui.admin.state.MODERATION_STATUS_ACTIVE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.ceil

class FakeAdminRepository : AdminRepository {
    private val moderatorEvents = MutableSharedFlow<ModeratorWsEvent>(extraBufferCapacity = 8)
    private val moderatorWsState = MutableStateFlow(ModeratorWsConnectionState.Disconnected)
    private val moderationContent = mutableListOf(
        AdminModerationItem(
            id = "p-201",
            targetType = AdminModerationTargetType.Post,
            authorId = 44,
            authorName = "rezak159",
            authorColor = "#3B82F6",
            content = "Продам монстер вейпер 80мг",
            status = "hidden",
            createdAt = "2026-06-16T22:52:00Z",
            zone = "red",
            aiScore = 0.91,
            aiReason = "Подозрение на запрещённую продажу"
        ),
        AdminModerationItem(
            id = "p-198",
            targetType = AdminModerationTargetType.Post,
            authorId = 12,
            authorName = "ИИ",
            content = "Продам монстер вейпер 80мг",
            status = "active",
            createdAt = "2026-06-16T22:21:00Z",
            zone = "yellow",
            aiScore = 0.73,
            aiReason = "Вердикт модели требует проверки"
        ),
        AdminModerationItem(
            id = "c-410",
            targetType = AdminModerationTargetType.Comment,
            postId = "p-198",
            authorId = 12,
            authorName = "ИИ",
            content = "Хо-ро-шо!",
            status = "active",
            createdAt = "2026-06-15T22:33:00Z",
            zone = "yellow",
            aiScore = 0.68,
            aiReason = "Саркастичный комментарий"
        ),
        AdminModerationItem(
            id = "p-190",
            targetType = AdminModerationTargetType.Post,
            authorId = 12,
            authorName = "ИИ",
            content = "Друзья, на данный момент мы находимся в бета-тесте. Если у вас возникли какие-то...",
            status = "review",
            createdAt = "2026-06-15T21:31:00Z",
            zone = "yellow",
            aiScore = 0.62,
            aiReason = "Нужна ручная модерация"
        ),
        AdminModerationItem(
            id = "p-175",
            targetType = AdminModerationTargetType.Post,
            authorId = 12,
            authorName = "ИИ",
            content = "Ставь лайк если проснулся",
            status = "active",
            createdAt = "2026-06-15T20:53:00Z",
            zone = "yellow",
            aiScore = 0.56,
            aiReason = "Пограничный engagement bait"
        ),
        AdminModerationItem(
            id = "c-388",
            targetType = AdminModerationTargetType.Comment,
            postId = "p-172",
            authorId = 44,
            authorName = "rezak159",
            authorColor = "#3B82F6",
            content = "когда выход на ipo?",
            status = "active",
            createdAt = "2026-06-15T12:42:00Z"
        )
    )
    private val moderationReports = mutableListOf<AdminModerationReport>()
    private val seenAt = mutableMapOf<AdminModerationTab, Long>()
    private val moderationHistory = mutableListOf(
        AdminModerationEvent(
            id = "ai_1",
            ts = "2026-06-16T22:52:10Z",
            action = "ai_verdict",
            actor = AdminModerationEventActor(kind = "ai", name = "ИИ"),
            target = AdminModerationEventTarget(
                type = AdminModerationEventTargetType.Post,
                id = "p-201",
                preview = "Продам монстер вейпер 80мг",
                content = "Продам монстер вейпер 80мг",
                author = "rezak159",
                authorColor = "#3B82F6",
                status = "review"
            ),
            details = AdminModerationEventDetails(
                zone = "red",
                score = 91,
                reason = "Подозрение на запрещённую продажу"
            )
        ),
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
            id = "rpt_3",
            ts = "2026-06-15T22:40:00Z",
            action = "resolve_report",
            actor = AdminModerationEventActor(kind = "moderator", name = "Администратор"),
            target = AdminModerationEventTarget(
                type = AdminModerationEventTargetType.Comment,
                id = "c-410",
                preview = "Хо-ро-шо!",
                content = "Хо-ро-шо!",
                author = "ИИ",
                status = "active"
            ),
            details = AdminModerationEventDetails(contentStatus = "active", reportId = "17")
        )
    )
    private val users = MockUsers.map { user ->
        user.copy(roles = emptyList())
    }.toMutableList()
    private val roles = MockRoles.map { it.copy() }.toMutableList()
    private val permissions = MockPermissions.map { it.copy() }.toMutableList()
    private val userRoleLinks = MockUsers.flatMap { user ->
        user.roles.map { role -> UserRoleLink(user.id.toLong(), role.id.toLong(), isSoftRemoved = false) }
    }.toMutableList()
    private val rolePermissionLinks = buildList {
        MockRoles.firstOrNull()?.let { role ->
            listOf(2, 8, 3, 10).forEach { permissionIndex ->
                MockPermissions.getOrNull(permissionIndex)?.let { permission ->
                    add(RolePermissionLink(role.id.toLong(), permission.id.toLong(), isSoftRemoved = false))
                }
            }
        }
    }.toMutableList()
    private val historyActor = AdminChangeLogActor(
        id = 1,
        username = "admin",
        firstName = "Администратор"
    )
    private val historyRecords = listOf(
        AdminChangeLogRecord(
            id = 1,
            entityType = AdminHistoryEntityType.Role,
            entityId = (MockRoles.firstOrNull()?.id ?: 1).toLong(),
            operationType = AdminChangeOperationType.Update,
            canRestore = true,
            changedFields = mapOf(
                "name" to AdminChangedField(oldValue = "Moderator", newValue = "Moderator Pro")
            ),
            createdAt = "2026-05-19T10:00:00Z",
            createdBy = historyActor.id,
            actor = historyActor
        ),
        AdminChangeLogRecord(
            id = 2,
            entityType = AdminHistoryEntityType.Permission,
            entityId = (MockPermissions.getOrNull(1)?.id ?: 2).toLong(),
            operationType = AdminChangeOperationType.Create,
            canRestore = false,
            changedFields = mapOf(
                "slug" to AdminChangedField(oldValue = "", newValue = "schedule.read")
            ),
            createdAt = "2026-05-19T10:15:00Z",
            createdBy = historyActor.id,
            actor = historyActor
        ),
        AdminChangeLogRecord(
            id = 3,
            entityType = AdminHistoryEntityType.Permission,
            entityId = (MockPermissions.getOrNull(2)?.id ?: 3).toLong(),
            operationType = AdminChangeOperationType.Delete,
            canRestore = true,
            changedFields = mapOf(
                "deleted_at" to AdminChangedField(oldValue = "", newValue = "2026-05-19T10:30:00Z")
            ),
            createdAt = "2026-05-19T10:30:00Z",
            createdBy = historyActor.id,
            actor = historyActor
        ),
        AdminChangeLogRecord(
            id = 4,
            entityType = AdminHistoryEntityType.User,
            entityId = MockUsers.firstOrNull()?.id ?: 1,
            operationType = AdminChangeOperationType.Update,
            canRestore = false,
            changedFields = mapOf(
                "first_name" to AdminChangedField(oldValue = "Егор", newValue = "Егор")
            ),
            createdAt = "2026-05-19T10:45:00Z",
            createdBy = historyActor.id,
            actor = historyActor
        )
    )
    private val requestLogDetails = mutableListOf(
        RequestLogEntry(
            id = 1,
            fullUrl = "/api/ref/log/request",
            method = "POST",
            controllerPath = "/api/ref/log/request",
            controllerMethod = "get_logs_route",
            requestBody = """{"page":1,"per_page":10}""",
            requestHeaders = """{"authorization":"Bearer ***"}""",
            userId = 1,
            ipAddress = "127.0.0.1",
            userAgent = "okhttp/4.12.0",
            responseStatus = 200,
            responseBody = """{"items":[],"total_count":0}""",
            responseHeaders = """{"content-type":"application/json"}""",
            calledAt = "2026-05-19T09:01:00Z"
        ),
        RequestLogEntry(
            id = 2,
            fullUrl = "/api/ref/changelog/changed/permission",
            method = "GET",
            controllerPath = "/api/ref/changelog/changed/permission",
            controllerMethod = "get_changed_permissions_route",
            requestBody = "{}",
            requestHeaders = """{"authorization":"Bearer ***"}""",
            userId = 1,
            ipAddress = "127.0.0.1",
            userAgent = "okhttp/4.12.0",
            responseStatus = 200,
            responseBody = """{"items":[{"id":1}]}""",
            responseHeaders = """{"content-type":"application/json"}""",
            calledAt = "2026-05-19T09:05:00Z"
        ),
        RequestLogEntry(3, "/api/ref/user/", "GET", "/api/ref/user/", "get_users_route", "{}", """{"authorization":"Bearer ***"}""", 1, "127.0.0.1", "okhttp/4.12.0", 401, """{"detail":"Unauthorized"}""", """{"content-type":"application/json"}""", "2026-05-19T09:09:00Z"),
        RequestLogEntry(4, "/api/ref/user/", "GET", "/api/ref/user/", "get_users_route", "{}", """{"authorization":"Bearer ***"}""", 2, "10.0.2.2", "okhttp/4.12.0", 200, """[{"id":1}]""", """{"content-type":"application/json"}""", "2026-05-19T09:10:00Z"),
        RequestLogEntry(5, "/api/ref/policy/role", "POST", "/api/ref/policy/role", "create_role_route", """{"name":"Manager"}""", """{"authorization":"Bearer ***"}""", 2, "10.0.2.2", "okhttp/4.12.0", 201, """{"id":12}""", """{"content-type":"application/json"}""", "2026-05-19T09:12:00Z"),
        RequestLogEntry(6, "/api/ref/policy/role/12", "DELETE", "/api/ref/policy/role/{id}", "hard_delete_role_route", "{}", """{"authorization":"Bearer ***"}""", 2, "10.0.2.2", "okhttp/4.12.0", 404, """{"detail":"Role not found"}""", """{"content-type":"application/json"}""", "2026-05-19T09:15:00Z"),
        RequestLogEntry(7, "/api/ref/policy/permission", "POST", "/api/ref/policy/permission", "create_permission_route", """{"slug":"perm.read"}""", """{"authorization":"Bearer ***"}""", 3, "192.168.0.22", "Mozilla/5.0", 400, """{"detail":"Validation error"}""", """{"content-type":"application/json"}""", "2026-05-19T09:18:00Z"),
        RequestLogEntry(8, "/api/ref/changelog/changed/permission", "GET", "/api/ref/changelog/changed/permission", "get_changed_permissions_route", "{}", """{"authorization":"Bearer ***"}""", 3, "192.168.0.22", "Mozilla/5.0", 500, """{"detail":"Internal error"}""", """{"content-type":"application/json"}""", "2026-05-19T09:20:00Z"),
        RequestLogEntry(9, "/api/ref/log/request/8", "DELETE", "/api/ref/log/request/{id}", "delete_log_route", "{}", """{"authorization":"Bearer ***"}""", 3, "192.168.0.22", "Mozilla/5.0", 200, """{"message":"deleted"}""", """{"content-type":"application/json"}""", "2026-05-19T09:22:00Z"),
        RequestLogEntry(10, "/api/ref/user/", "GET", "/api/ref/user/", "get_users_route", "{}", """{"authorization":"Bearer ***"}""", null, "172.16.1.10", "curl/8.5.0", 200, """[{"id":2}]""", """{"content-type":"application/json"}""", "2026-05-19T09:25:00Z"),
        RequestLogEntry(11, "/api/ref/log/request", "POST", "/api/ref/log/request", "get_logs_route", """{"page":2,"per_page":10}""", """{"authorization":"Bearer ***"}""", null, "172.16.1.10", "curl/8.5.0", 200, """{"items":[{"id":11}]}""", """{"content-type":"application/json"}""", "2026-05-19T09:30:00Z"),
        RequestLogEntry(12, "/api/ref/policy/permission/7", "DELETE", "/api/ref/policy/permission/{id}", "hard_delete_permission_route", "{}", """{"authorization":"Bearer ***"}""", 5, "203.0.113.7", "PostmanRuntime/7.42.0", 200, """{"message":"ok"}""", """{"content-type":"application/json"}""", "2026-05-19T09:35:00Z")
    )

    override suspend fun getModerationContent(): List<AdminModerationItem> =
        moderationContent.map { it.copy() }

    override suspend fun getModerationReports(): List<AdminModerationReport> =
        moderationReports.map { it.copy() }

    override suspend fun updateModerationStatus(
        targetType: AdminModerationTargetType,
        targetId: String,
        status: String
    ) {
        val index = moderationContent.indexOfFirst { it.targetType == targetType && it.id == targetId }
        if (index >= 0) {
            moderationContent[index] = moderationContent[index].copy(status = status)
        }
        moderationReports.replaceAll { report ->
            if (report.targetType == targetType && report.targetId == targetId) {
                report.copy(contentStatus = status)
            } else {
                report
            }
        }
    }

    override suspend fun resolveModerationReport(reportId: String) {
        moderationReports.removeAll { it.id == reportId }
    }

    override suspend fun getModerationHistory(): List<AdminModerationEvent> =
        moderationHistory.map { it.copy() }

    override suspend fun restoreModerationContent(targetType: String, targetId: String) {
        val type = AdminModerationEventTargetType.fromApi(targetType)
        val index = moderationContent.indexOfFirst {
            it.targetType.name.equals(targetType, ignoreCase = true) && it.id == targetId
        }
        if (index >= 0) {
            moderationContent[index] = moderationContent[index].copy(status = MODERATION_STATUS_ACTIVE)
        }
        moderationHistory.replaceAll { event ->
            if (event.target.type == type && event.target.id == targetId) {
                event.copy(target = event.target.copy(status = MODERATION_STATUS_ACTIVE))
            } else {
                event
            }
        }
    }

    /** Превью показывают всё: прав у фейкового пользователя столько, сколько нужно. */
    override fun currentUserPermissions(): Set<String>? = null

    override fun moderationTabSeenAt(tab: AdminModerationTab): Long = seenAt[tab] ?: 0L

    override fun markModerationTabSeen(tab: AdminModerationTab) {
        seenAt[tab] = System.currentTimeMillis()
    }

    override fun observeModeratorWsEvents(): Flow<ModeratorWsEvent> =
        moderatorEvents

    override fun observeModeratorWsConnectionState(): StateFlow<ModeratorWsConnectionState> =
        moderatorWsState

    override fun startModeratorSocket() {
        moderatorWsState.value = ModeratorWsConnectionState.Connected
        // На бэкенде счётчик приезжает при первом изменении очереди; в превью
        // ждать нечего, поэтому отдаём его сразу после подключения.
        moderatorEvents.tryEmit(ModeratorWsEvent.ModerationCount(count = moderationContent.size))
    }

    override fun stopModeratorSocket() {
        moderatorWsState.value = ModeratorWsConnectionState.Disconnected
    }

    private val dailyReports = mutableListOf(
        AdminReportListItem("2026-09-14", AdminReportJobStatus.Done, hasFile = true, sizeBytes = 2048),
        AdminReportListItem("2026-09-13", AdminReportJobStatus.Done, hasFile = true, sizeBytes = 1994),
        AdminReportListItem("2026-09-12", AdminReportJobStatus.Failed, hasFile = false, sizeBytes = null),
        AdminReportListItem("2026-09-11", null, hasFile = true, sizeBytes = 1870)
    )

    /** Ровные, но неодинаковые числа: на них видно и списки, и форму графиков. */
    private fun sampleMetrics(seed: Int): Map<AdminReportMetric, Int> =
        AdminReportMetric.entries.mapIndexed { index, metric ->
            metric to (40 + (seed * 7 + index * 13) % 60)
        }.toMap()

    override suspend fun getDailyReports(): List<AdminReportListItem> = dailyReports.toList()

    override suspend fun getDailyReport(date: String): AdminDailyReport = AdminDailyReport(
        date = date,
        generatedAt = "${date}T03:10:00+05:00",
        metrics = sampleMetrics(date.takeLast(2).toIntOrNull() ?: 1)
    )

    override suspend fun getTodayReportMetrics(): Map<AdminReportMetric, Int> = sampleMetrics(seed = 3)

    override suspend fun getReportSeries(
        metric: AdminReportMetric,
        from: String,
        to: String
    ): AdminReportSeries = AdminReportSeries(
        metric = metric,
        points = dailyReports.filter { it.hasFile }.map { report ->
            AdminReportSeriesPoint(
                date = report.date,
                value = sampleMetrics(report.date.takeLast(2).toIntOrNull() ?: 1).getValue(metric)
            )
        }.sortedBy { it.date }
    )

    override suspend fun generateDailyReport(
        date: String,
        force: Boolean
    ): AdminGenerateReportResult {
        val existing = dailyReports.firstOrNull { it.date == date }
        if (existing != null && existing.hasFile && !force) return AdminGenerateReportResult.Exists

        dailyReports.removeAll { it.date == date }
        dailyReports.add(
            0,
            AdminReportListItem(date, AdminReportJobStatus.Queued, hasFile = false, sizeBytes = null)
        )
        return AdminGenerateReportResult.Queued
    }

    override suspend fun getUsers(): List<AdminUser> =
        users.map { user -> user.withVisibleRoles() }

    override suspend fun getRoles(): List<AdminRole> =
        roles.map { it.copy() }

    override suspend fun getPermissions(): List<AdminPermission> =
        permissions.map { it.copy() }

    override suspend fun createRole(input: AdminRoleInput): AdminRole {
        requireUniqueRoleSlug(input.slug)
        val role = AdminRole(
            id = nextRoleId(),
            name = input.name,
            slug = input.slug,
            description = input.description.ifBlank { null }
        )
        roles.add(role)
        return role.copy()
    }

    override suspend fun updateRole(roleId: Long, input: AdminRoleInput): AdminRole {
        val index = roles.indexOfFirst { it.id.toLong() == roleId }
        if (index == -1) throw IllegalArgumentException("Роль не найдена")
        requireUniqueRoleSlug(input.slug, ignoredRoleId = roleId)
        val updated = roles[index].copy(
            name = input.name,
            slug = input.slug,
            description = input.description.ifBlank { null }
        )
        roles[index] = updated
        return updated.copy()
    }

    override suspend fun softDeleteRole(roleId: Long): AdminRole {
        val index = roles.indexOfFirst { it.id.toLong() == roleId }
        if (index == -1) throw IllegalArgumentException("Роль не найдена")
        val updated = roles[index].copy(isDeleted = true)
        roles[index] = updated
        return updated.copy()
    }

    override suspend fun hardDeleteRole(roleId: Long) {
        val removed = roles.removeAll { it.id.toLong() == roleId }
        if (!removed) throw IllegalArgumentException("Роль не найдена")
        userRoleLinks.removeAll { it.roleId == roleId }
        rolePermissionLinks.removeAll { it.roleId == roleId }
    }

    override suspend fun restoreRole(roleId: Long): AdminRole {
        val index = roles.indexOfFirst { it.id.toLong() == roleId }
        if (index == -1) throw IllegalArgumentException("Роль не найдена")
        val updated = roles[index].copy(isDeleted = false)
        roles[index] = updated
        return updated.copy()
    }

    override suspend fun createPermission(input: AdminPermissionInput): AdminPermission {
        requireUniquePermissionSlug(input.slug)
        val permission = AdminPermission(
            id = nextPermissionId(),
            name = input.name,
            slug = input.slug,
            description = input.description.ifBlank { null }
        )
        permissions.add(permission)
        return permission.copy()
    }

    override suspend fun updatePermission(permissionId: Long, input: AdminPermissionInput): AdminPermission {
        val index = permissions.indexOfFirst { it.id.toLong() == permissionId }
        if (index == -1) throw IllegalArgumentException("Разрешение не найдено")
        requireUniquePermissionSlug(input.slug, ignoredPermissionId = permissionId)
        val updated = permissions[index].copy(
            name = input.name,
            slug = input.slug,
            description = input.description.ifBlank { null }
        )
        permissions[index] = updated
        return updated.copy()
    }

    override suspend fun softDeletePermission(permissionId: Long): AdminPermission {
        val index = permissions.indexOfFirst { it.id.toLong() == permissionId }
        if (index == -1) throw IllegalArgumentException("Разрешение не найдено")
        val updated = permissions[index].copy(isDeleted = true)
        permissions[index] = updated
        return updated.copy()
    }

    override suspend fun hardDeletePermission(permissionId: Long) {
        val removed = permissions.removeAll { it.id.toLong() == permissionId }
        if (!removed) throw IllegalArgumentException("Разрешение не найдено")
        rolePermissionLinks.removeAll { it.permissionId == permissionId }
    }

    override suspend fun restorePermission(permissionId: Long): AdminPermission {
        val index = permissions.indexOfFirst { it.id.toLong() == permissionId }
        if (index == -1) throw IllegalArgumentException("Разрешение не найдено")
        val updated = permissions[index].copy(isDeleted = false)
        permissions[index] = updated
        return updated.copy()
    }

    override suspend fun assignRoleToUser(userId: Long, roleId: Long): AdminUser {
        findUser(userId)
        findRole(roleId)
        val existing = userRoleLinks.firstOrNull { it.userId == userId && it.roleId == roleId }
        when {
            existing == null -> userRoleLinks.add(UserRoleLink(userId, roleId, isSoftRemoved = false))
            existing.isSoftRemoved -> {
                userRoleLinks[userRoleLinks.indexOf(existing)] = existing.copy(isSoftRemoved = false)
            }
            else -> throw IllegalArgumentException("Роль уже назначена пользователю")
        }
        return findUser(userId).withVisibleRoles()
    }

    override suspend fun softRemoveRoleFromUser(userId: Long, roleId: Long): AdminUser {
        findUser(userId)
        findRole(roleId)
        val index = userRoleLinks.indexOfFirst {
            it.userId == userId && it.roleId == roleId && !it.isSoftRemoved
        }
        if (index == -1) throw IllegalArgumentException("Назначение роли пользователю не найдено")
        userRoleLinks[index] = userRoleLinks[index].copy(isSoftRemoved = true)
        return findUser(userId).withVisibleRoles()
    }

    override suspend fun hardRemoveRoleFromUser(userId: Long, roleId: Long): AdminUser {
        findUser(userId)
        findRole(roleId)
        val removed = userRoleLinks.removeAll { it.userId == userId && it.roleId == roleId }
        if (!removed) throw IllegalArgumentException("Назначение роли пользователю не найдено")
        return findUser(userId).withVisibleRoles()
    }

    override suspend fun getRolePermissions(roleId: Long): List<AdminPermission> {
        findRole(roleId)
        return visiblePermissionsForRole(roleId)
    }

    override suspend fun assignPermissionToRole(roleId: Long, permissionId: Long): List<AdminPermission> {
        findRole(roleId)
        findPermission(permissionId)
        val existing = rolePermissionLinks.firstOrNull {
            it.roleId == roleId && it.permissionId == permissionId
        }
        when {
            existing == null -> rolePermissionLinks.add(
                RolePermissionLink(roleId, permissionId, isSoftRemoved = false)
            )
            existing.isSoftRemoved -> {
                rolePermissionLinks[rolePermissionLinks.indexOf(existing)] = existing.copy(isSoftRemoved = false)
            }
            else -> throw IllegalArgumentException("Разрешение уже назначено роли")
        }
        return visiblePermissionsForRole(roleId)
    }

    override suspend fun softRemovePermissionFromRole(roleId: Long, permissionId: Long): List<AdminPermission> {
        findRole(roleId)
        findPermission(permissionId)
        val index = rolePermissionLinks.indexOfFirst {
            it.roleId == roleId && it.permissionId == permissionId && !it.isSoftRemoved
        }
        if (index == -1) throw IllegalArgumentException("Назначение разрешения роли не найдено")
        rolePermissionLinks[index] = rolePermissionLinks[index].copy(isSoftRemoved = true)
        return visiblePermissionsForRole(roleId)
    }

    override suspend fun hardRemovePermissionFromRole(roleId: Long, permissionId: Long): List<AdminPermission> {
        findRole(roleId)
        findPermission(permissionId)
        val removed = rolePermissionLinks.removeAll {
            it.roleId == roleId && it.permissionId == permissionId
        }
        if (!removed) throw IllegalArgumentException("Назначение разрешения роли не найдено")
        return visiblePermissionsForRole(roleId)
    }

    override suspend fun getChangedUsers(): List<AdminChangedUser> =
        historyRecords
            .filter { it.entityType == AdminHistoryEntityType.User }
            .mapNotNull { record ->
                users.firstOrNull { it.id == record.entityId }?.let { user ->
                    AdminChangedUser(
                        id = user.id,
                        username = user.username,
                        firstName = user.name,
                        lastChangedAt = record.createdAt
                    )
                }
            }

    override suspend fun getChangedRoles(): List<AdminChangedRole> =
        historyRecords
            .filter { it.entityType == AdminHistoryEntityType.Role }
            .mapNotNull { record ->
                roles.firstOrNull { it.id.toLong() == record.entityId }?.let { role ->
                    AdminChangedRole(
                        id = role.id,
                        name = role.name,
                        slug = role.slug,
                        lastChangedAt = record.createdAt,
                        isDeleted = role.isDeleted
                    )
                }
            }

    override suspend fun getChangedPermissions(): List<AdminChangedPermission> =
        historyRecords
            .filter { it.entityType == AdminHistoryEntityType.Permission }
            .mapNotNull { record ->
                permissions.firstOrNull { it.id.toLong() == record.entityId }?.let { permission ->
                    AdminChangedPermission(
                        id = permission.id,
                        name = permission.name,
                        slug = permission.slug,
                        lastChangedAt = record.createdAt,
                        isDeleted = permission.isDeleted
                    )
                }
            }

    override suspend fun getUserStory(userId: Long): List<AdminChangeLogRecord> {
        findUser(userId)
        return storyFor(AdminHistoryEntityType.User, userId)
    }

    override suspend fun getRoleStory(roleId: Long): List<AdminChangeLogRecord> {
        findRole(roleId)
        return storyFor(AdminHistoryEntityType.Role, roleId)
    }

    override suspend fun getPermissionStory(permissionId: Long): List<AdminChangeLogRecord> {
        findPermission(permissionId)
        return storyFor(AdminHistoryEntityType.Permission, permissionId)
    }

    override suspend fun restoreChangeLog(logId: Long): AdminRestoreChangeLogResult {
        val record = historyRecords.firstOrNull { it.id.toLong() == logId }
            ?: throw IllegalArgumentException("Запись истории не найдена")
        return AdminRestoreChangeLogResult(
            entityType = record.entityType,
            entityId = record.entityId
        )
    }

    override suspend fun getRequestLogs(page: Int, filter: AdminRequestLogsFilter): AdminRequestLogsPage {
        val perPage = filter.perPage.coerceAtLeast(1)
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedSearch = filter.search.trim().lowercase()
        val normalizedMethod = filter.method?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
        val normalizedControllerPath = filter.controllerPath.trim().lowercase()
        val normalizedIpAddress = filter.ipAddress.trim().lowercase()
        val userIdFilter = filter.userId.trim().toLongOrNull()
        val responseStatusFilter = filter.responseStatus.trim().toIntOrNull()
        val filtered = requestLogDetails
            .asSequence()
            .filter { entry ->
                userIdFilter == null || entry.userId == userIdFilter
            }
            .filter { entry ->
                responseStatusFilter == null || entry.responseStatus == responseStatusFilter
            }
            .filter { entry ->
                normalizedIpAddress.isBlank() || entry.ipAddress.orEmpty().lowercase().contains(normalizedIpAddress)
            }
            .filter { entry ->
                normalizedControllerPath.isBlank() || entry.controllerPath.orEmpty().lowercase().contains(normalizedControllerPath)
            }
            .filter { entry ->
                normalizedMethod == null || entry.method.orEmpty().uppercase() == normalizedMethod
            }
            .filter { entry ->
                when (filter.statusGroup) {
                    "2xx" -> entry.responseStatus in 200..299
                    "3xx" -> entry.responseStatus in 300..399
                    "4xx" -> entry.responseStatus in 400..499
                    "5xx" -> entry.responseStatus in 500..599
                    else -> true
                }
            }
            .filter { entry ->
                if (normalizedSearch.isBlank()) {
                    true
                } else {
                    val haystack = listOf(
                        entry.fullUrl,
                        entry.controllerPath.orEmpty(),
                        entry.controllerMethod.orEmpty(),
                        entry.responseStatus.toString()
                    ).joinToString(" ").lowercase()
                    haystack.contains(normalizedSearch)
                }
            }
            .toList()
            .sortedWith(requestLogComparator(filter.sortBy, filter.sortOrder))
        val totalCount = filtered.size
        val totalPages = maxOf(1, ceil(totalCount.toDouble() / perPage.toDouble()).toInt())
        val from = ((normalizedPage - 1) * perPage).coerceAtMost(totalCount)
        val to = (from + perPage).coerceAtMost(totalCount)
        val pageItems = filtered.subList(from, to).map { it.toListItem() }
        return AdminRequestLogsPage(
            items = pageItems,
            page = normalizedPage,
            totalPages = totalPages,
            totalCount = totalCount,
            perPage = perPage
        )
    }

    override suspend fun getRequestLog(logId: Long): AdminRequestLogDetails =
        requestLogDetails.firstOrNull { it.id.toLong() == logId }?.toDetails()
            ?: throw IllegalArgumentException("Лог запроса не найден")

    override suspend fun deleteRequestLog(logId: Long) {
        val removed = requestLogDetails.removeAll { it.id.toLong() == logId }
        if (!removed) throw IllegalArgumentException("Лог запроса не найден")
    }

    private fun nextRoleId(): Int =
        (roles.maxOfOrNull { it.id } ?: 0) + 1

    private fun nextPermissionId(): Int =
        (permissions.maxOfOrNull { it.id } ?: 0) + 1

    private fun requireUniqueRoleSlug(slug: String, ignoredRoleId: Long? = null) {
        if (roles.any { it.slug == slug && it.id.toLong() != ignoredRoleId }) {
            throw IllegalArgumentException("Роль с таким служебным именем уже существует")
        }
    }

    private fun requireUniquePermissionSlug(slug: String, ignoredPermissionId: Long? = null) {
        if (permissions.any { it.slug == slug && it.id.toLong() != ignoredPermissionId }) {
            throw IllegalArgumentException("Разрешение с таким служебным именем уже существует")
        }
    }

    private fun findUser(userId: Long): AdminUser =
        users.firstOrNull { it.id.toLong() == userId }
            ?: throw IllegalArgumentException("Пользователь не найден")

    private fun findRole(roleId: Long): AdminRole =
        roles.firstOrNull { it.id.toLong() == roleId }
            ?: throw IllegalArgumentException("Роль не найдена")

    private fun findPermission(permissionId: Long): AdminPermission =
        permissions.firstOrNull { it.id.toLong() == permissionId }
            ?: throw IllegalArgumentException("Разрешение не найдено")

    private fun AdminUser.withVisibleRoles(): AdminUser {
        val visibleRoles = userRoleLinks
            .filter { it.userId == id.toLong() && !it.isSoftRemoved }
            .mapNotNull { link -> roles.firstOrNull { it.id.toLong() == link.roleId }?.copy() }
        return copy(roles = visibleRoles)
    }

    private fun visiblePermissionsForRole(roleId: Long): List<AdminPermission> =
        rolePermissionLinks
            .filter { it.roleId == roleId && !it.isSoftRemoved }
            .mapNotNull { link -> permissions.firstOrNull { it.id.toLong() == link.permissionId }?.copy() }

    private fun storyFor(entityType: AdminHistoryEntityType, entityId: Long): List<AdminChangeLogRecord> =
        historyRecords
            .filter { it.entityType == entityType && it.entityId.toLong() == entityId }
            .map { it.copy(changedFields = it.changedFields.toMap(), actor = it.actor?.copy()) }

    private fun requestLogComparator(sortBy: String, sortOrder: String): Comparator<RequestLogEntry> {
        val ascending = sortOrder.equals("asc", ignoreCase = true)
        val comparator = when (sortBy.trim()) {
            "id" -> compareBy<RequestLogEntry> { it.id }
            "response_status" -> compareBy { it.responseStatus }
            "user_id" -> compareBy { it.userId ?: Long.MIN_VALUE }
            "ip_address" -> compareBy { it.ipAddress.orEmpty() }
            "controller_path" -> compareBy { it.controllerPath.orEmpty() }
            "full_url" -> compareBy { it.fullUrl }
            "called_at" -> compareBy { it.calledAt }
            else -> compareBy { it.calledAt }
        }
        return if (ascending) comparator else comparator.reversed()
    }

    private fun RequestLogEntry.toListItem(): AdminRequestLogListItem =
        AdminRequestLogListItem(
            id = id,
            fullUrl = fullUrl,
            method = method,
            controllerPath = controllerPath,
            controllerMethod = controllerMethod,
            responseStatus = responseStatus,
            calledAt = calledAt
        )

    private fun RequestLogEntry.toDetails(): AdminRequestLogDetails =
        AdminRequestLogDetails(
            id = id,
            fullUrl = fullUrl,
            method = method.orEmpty(),
            controllerPath = controllerPath,
            controllerMethod = controllerMethod,
            requestBody = requestBody,
            requestHeaders = requestHeaders,
            userId = userId,
            ipAddress = ipAddress,
            userAgent = userAgent,
            responseStatus = responseStatus,
            responseBody = responseBody,
            responseHeaders = responseHeaders,
            calledAt = calledAt
        )

    private data class UserRoleLink(
        val userId: Long,
        val roleId: Long,
        val isSoftRemoved: Boolean
    )

    private data class RolePermissionLink(
        val roleId: Long,
        val permissionId: Long,
        val isSoftRemoved: Boolean
    )

    private data class RequestLogEntry(
        val id: Int,
        val fullUrl: String,
        val method: String?,
        val controllerPath: String?,
        val controllerMethod: String?,
        val requestBody: String,
        val requestHeaders: String,
        val userId: Long?,
        val ipAddress: String?,
        val userAgent: String?,
        val responseStatus: Int,
        val responseBody: String,
        val responseHeaders: String,
        val calledAt: String
    )
}
