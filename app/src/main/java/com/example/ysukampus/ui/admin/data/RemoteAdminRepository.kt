package com.example.ysukampus.ui.admin.data

import android.content.SharedPreferences
import android.util.Log
import com.example.ysukampus.data.network.admin.AdminApi
import com.example.ysukampus.data.network.admin.toAdminModerationEvents
import com.example.ysukampus.data.network.admin.toAdminModerationItems
import com.example.ysukampus.data.network.admin.toAdminModerationReports
import com.example.ysukampus.data.network.admin.dto.AssignPermissionToRoleRequestDto
import com.example.ysukampus.data.network.admin.dto.AssignRoleToUserRequestDto
import com.example.ysukampus.data.network.admin.toAdminPermission
import com.example.ysukampus.data.network.admin.toAdminRequestLogDetails
import com.example.ysukampus.data.network.admin.toAdminRequestLogsPage
import com.example.ysukampus.data.network.admin.toAdminRole
import com.example.ysukampus.data.network.admin.toAdminUser
import com.example.ysukampus.data.network.admin.toAdminChangeLogRecord
import com.example.ysukampus.data.network.admin.toAdminChangedPermission
import com.example.ysukampus.data.network.admin.toAdminChangedRole
import com.example.ysukampus.data.network.admin.toAdminChangedUser
import com.example.ysukampus.data.network.admin.toAdminDailyReport
import com.example.ysukampus.data.network.admin.toAdminReportListItem
import com.example.ysukampus.data.network.admin.toAdminReportMetrics
import com.example.ysukampus.data.network.admin.toAdminReportSeries
import com.example.ysukampus.data.network.admin.toAdminRestoreChangeLogResult
import com.example.ysukampus.data.network.admin.toCreatePermissionRequestDto
import com.example.ysukampus.data.network.admin.toCreateRoleRequestDto
import com.example.ysukampus.data.network.admin.toUpdatePermissionRequestDto
import com.example.ysukampus.data.network.admin.toUpdateRoleRequestDto
import com.example.ysukampus.data.network.admin.toRequestLogQueryDto
import com.example.ysukampus.data.network.error.toReadableMessage
import com.example.ysukampus.data.session.CurrentUserStore
import com.example.ysukampus.data.network.ws.ModeratorWebSocketClient
import com.example.ysukampus.data.network.ws.ModeratorWsConnectionState
import com.example.ysukampus.data.network.ws.ModeratorWsEvent
import com.example.ysukampus.ui.admin.AdminPermission
import com.example.ysukampus.ui.admin.AdminRole
import com.example.ysukampus.ui.admin.AdminUser
import com.example.ysukampus.ui.admin.state.AdminChangeLogRecord
import com.example.ysukampus.ui.admin.state.AdminChangedPermission
import com.example.ysukampus.ui.admin.state.AdminChangedRole
import com.example.ysukampus.ui.admin.state.AdminChangedUser
import com.example.ysukampus.ui.admin.state.AdminDailyReport
import com.example.ysukampus.ui.admin.state.AdminGenerateReportResult
import com.example.ysukampus.ui.admin.state.AdminReportListItem
import com.example.ysukampus.ui.admin.state.AdminReportMetric
import com.example.ysukampus.ui.admin.state.AdminReportSeries
import com.example.ysukampus.ui.admin.state.AdminModerationEvent
import com.example.ysukampus.ui.admin.state.AdminModerationItem
import com.example.ysukampus.ui.admin.state.AdminModerationReport
import com.example.ysukampus.ui.admin.state.AdminModerationTab
import com.example.ysukampus.ui.admin.state.AdminModerationTargetType
import com.example.ysukampus.ui.admin.state.AdminPermissionInput
import com.example.ysukampus.ui.admin.state.AdminRequestLogDetails
import com.example.ysukampus.ui.admin.state.AdminRequestLogsFilter
import com.example.ysukampus.ui.admin.state.AdminRequestLogsPage
import com.example.ysukampus.ui.admin.state.AdminRestoreChangeLogResult
import com.example.ysukampus.ui.admin.state.AdminRoleInput
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

class RemoteAdminRepository(
    private val api: AdminApi,
    private val moderatorWebSocketClient: ModeratorWebSocketClient,
    private val currentUserStore: CurrentUserStore,
    private val prefs: SharedPreferences
) : AdminRepository {
    override suspend fun getModerationContent(): List<AdminModerationItem> =
        request {
            try {
                api.getModerationContent().toAdminModerationItems()
            } catch (error: HttpException) {
                if (error.code() != HTTP_NOT_FOUND) {
                    throw error
                }
                Log.w(
                    TAG,
                    "GET moderation/content returned 404, falling back to moderation/posts + moderation/comments"
                )
                val posts = fetchOptionalModerationItems(
                    endpointName = "moderation/posts",
                    requestBlock = api::getModerationPosts
                )
                val comments = fetchOptionalModerationItems(
                    endpointName = "moderation/comments",
                    requestBlock = api::getModerationComments
                )
                (posts + comments).sortedByDescending { it.createdAt }
            }
        }

    override suspend fun getModerationReports(): List<AdminModerationReport> =
        request {
            try {
                api.getModerationReports().toAdminModerationReports()
            } catch (error: HttpException) {
                if (error.code() != HTTP_NOT_FOUND) {
                    throw error
                }
                Log.w(TAG, "GET moderation/reports returned 404, using empty moderation reports list")
                emptyList()
            }
        }

    override suspend fun updateModerationStatus(
        targetType: AdminModerationTargetType,
        targetId: String,
        status: String
    ) {
        val body = buildJsonObject { put("status", status) }
        request {
            when (targetType) {
                AdminModerationTargetType.Post -> api.updateModerationPostStatus(targetId, body)
                AdminModerationTargetType.Comment -> api.updateModerationCommentStatus(targetId, body)
            }
        }
    }

    override suspend fun resolveModerationReport(reportId: String) {
        request { api.resolveModerationReport(reportId, JsonObject(emptyMap())) }
    }

    override suspend fun getModerationHistory(): List<AdminModerationEvent> =
        request { api.getModerationHistory().toAdminModerationEvents() }

    override suspend fun restoreModerationContent(targetType: String, targetId: String) {
        request {
            api.restoreModerationContent(targetType, targetId, JsonObject(emptyMap()))
        }
    }

    override fun currentUserPermissions(): Set<String>? = currentUserStore.get()?.permissions

    override fun moderationTabSeenAt(tab: AdminModerationTab): Long =
        prefs.getLong(tab.seenAtKey, 0L)

    override fun markModerationTabSeen(tab: AdminModerationTab) {
        prefs.edit().putLong(tab.seenAtKey, System.currentTimeMillis()).apply()
    }

    /** Ключи те же, что у вебки в localStorage, — так их проще держать в голове. */
    private val AdminModerationTab.seenAtKey: String
        get() = when (this) {
            AdminModerationTab.Moderation -> "moderationSeenAt"
            AdminModerationTab.Reports -> "reportsSeenAt"
            AdminModerationTab.History -> "historySeenAt"
        }

    override fun observeModeratorWsEvents(): Flow<ModeratorWsEvent> =
        moderatorWebSocketClient.events

    override fun observeModeratorWsConnectionState(): StateFlow<ModeratorWsConnectionState> =
        moderatorWebSocketClient.connectionState

    override fun startModeratorSocket() {
        moderatorWebSocketClient.start()
    }

    override fun stopModeratorSocket() {
        moderatorWebSocketClient.stop()
    }

    override suspend fun getDailyReports(): List<AdminReportListItem> =
        request { api.getDailyReports().items.map { it.toAdminReportListItem() } }

    override suspend fun getDailyReport(date: String): AdminDailyReport =
        request { api.getDailyReport(date).toAdminDailyReport() }

    override suspend fun getTodayReportMetrics(): Map<AdminReportMetric, Int> =
        request { api.getTodayReportMetrics().toAdminReportMetrics() }

    override suspend fun getReportSeries(
        metric: AdminReportMetric,
        from: String,
        to: String
    ): AdminReportSeries? =
        request { api.getDailyReportSeries(metric.key, from, to).toAdminReportSeries() }

    override suspend fun generateDailyReport(
        date: String,
        force: Boolean
    ): AdminGenerateReportResult =
        try {
            api.generateDailyReport(date = date, force = force)
            AdminGenerateReportResult.Queued
        } catch (error: HttpException) {
            // 409 — файл за дату уже есть: спрашиваем про пересборку, а не падаем.
            if (error.code() == HTTP_CONFLICT) {
                AdminGenerateReportResult.Exists
            } else {
                throw IllegalStateException(error.toReadableMessage(), error)
            }
        } catch (error: Throwable) {
            throw IllegalStateException(error.toReadableMessage(), error)
        }

    override suspend fun getUsers(): List<AdminUser> =
        request { api.getUsers().map { it.toAdminUser() } }

    override suspend fun getRoles(): List<AdminRole> =
        request { api.getRoles().map { it.toAdminRole() } }

    override suspend fun getPermissions(): List<AdminPermission> =
        request { api.getPermissions().map { it.toAdminPermission() } }

    override suspend fun createRole(input: AdminRoleInput): AdminRole =
        request { api.createRole(input.toCreateRoleRequestDto()).toAdminRole() }

    override suspend fun updateRole(roleId: Long, input: AdminRoleInput): AdminRole =
        request { api.updateRole(roleId.toBackendId(), input.toUpdateRoleRequestDto()).toAdminRole() }

    override suspend fun softDeleteRole(roleId: Long): AdminRole =
        request { api.softDeleteRole(roleId.toBackendId()).toAdminRole() }

    override suspend fun hardDeleteRole(roleId: Long) {
        request { api.hardDeleteRole(roleId.toBackendId()) }
    }

    override suspend fun restoreRole(roleId: Long): AdminRole =
        request { api.restoreRole(roleId.toBackendId()).toAdminRole() }

    override suspend fun createPermission(input: AdminPermissionInput): AdminPermission =
        request { api.createPermission(input.toCreatePermissionRequestDto()).toAdminPermission() }

    override suspend fun updatePermission(permissionId: Long, input: AdminPermissionInput): AdminPermission =
        request {
            api.updatePermission(
                permissionId.toBackendId(),
                input.toUpdatePermissionRequestDto()
            ).toAdminPermission()
        }

    override suspend fun softDeletePermission(permissionId: Long): AdminPermission =
        request { api.softDeletePermission(permissionId.toBackendId()).toAdminPermission() }

    override suspend fun hardDeletePermission(permissionId: Long) {
        request { api.hardDeletePermission(permissionId.toBackendId()) }
    }

    override suspend fun restorePermission(permissionId: Long): AdminPermission =
        request { api.restorePermission(permissionId.toBackendId()).toAdminPermission() }

    override suspend fun assignRoleToUser(userId: Long, roleId: Long): AdminUser =
        request {
            api.assignRoleToUser(userId, AssignRoleToUserRequestDto(roleId.toBackendId()))
            loadUser(userId)
        }

    override suspend fun softRemoveRoleFromUser(userId: Long, roleId: Long): AdminUser =
        request {
            api.softRemoveRoleFromUser(userId, roleId.toBackendId())
            loadUser(userId)
        }

    override suspend fun hardRemoveRoleFromUser(userId: Long, roleId: Long): AdminUser =
        request {
            api.hardRemoveRoleFromUser(userId, roleId.toBackendId())
            loadUser(userId)
        }

    override suspend fun getRolePermissions(roleId: Long): List<AdminPermission> =
        request { loadRolePermissions(roleId) }

    override suspend fun assignPermissionToRole(roleId: Long, permissionId: Long): List<AdminPermission> =
        request {
            api.assignPermissionToRole(
                roleId.toBackendId(),
                AssignPermissionToRoleRequestDto(permissionId.toBackendId())
            )
            loadRolePermissions(roleId)
        }

    override suspend fun softRemovePermissionFromRole(roleId: Long, permissionId: Long): List<AdminPermission> =
        request {
            api.softRemovePermissionFromRole(roleId.toBackendId(), permissionId.toBackendId())
            loadRolePermissions(roleId)
        }

    override suspend fun hardRemovePermissionFromRole(roleId: Long, permissionId: Long): List<AdminPermission> =
        request {
            api.hardRemovePermissionFromRole(roleId.toBackendId(), permissionId.toBackendId())
            loadRolePermissions(roleId)
        }

    override suspend fun getChangedUsers(): List<AdminChangedUser> =
        request { api.getChangedUsers().items.map { it.toAdminChangedUser() } }

    override suspend fun getChangedRoles(): List<AdminChangedRole> =
        request { api.getChangedRoles().items.map { it.toAdminChangedRole() } }

    override suspend fun getChangedPermissions(): List<AdminChangedPermission> =
        request { api.getChangedPermissions().items.map { it.toAdminChangedPermission() } }

    override suspend fun getUserStory(userId: Long): List<AdminChangeLogRecord> =
        request { api.getUserStory(userId).items.map { it.toAdminChangeLogRecord() } }

    override suspend fun getRoleStory(roleId: Long): List<AdminChangeLogRecord> =
        request { api.getRoleStory(roleId.toBackendId()).items.map { it.toAdminChangeLogRecord() } }

    override suspend fun getPermissionStory(permissionId: Long): List<AdminChangeLogRecord> =
        request { api.getPermissionStory(permissionId.toBackendId()).items.map { it.toAdminChangeLogRecord() } }

    override suspend fun restoreChangeLog(logId: Long): AdminRestoreChangeLogResult =
        request { api.restoreChangeLog(logId.toBackendId()).toAdminRestoreChangeLogResult() }

    override suspend fun getRequestLogs(page: Int, filter: AdminRequestLogsFilter): AdminRequestLogsPage =
        request {
            api.getRequestLogs(filter.toRequestLogQueryDto(page)).toAdminRequestLogsPage()
        }

    override suspend fun getRequestLog(logId: Long): AdminRequestLogDetails =
        request { api.getRequestLog(logId.toBackendId()).toAdminRequestLogDetails() }

    override suspend fun deleteRequestLog(logId: Long) {
        request { api.deleteRequestLog(logId.toBackendId()) }
    }

    private suspend fun loadUser(userId: Long): AdminUser =
        api.getUsers()
            .firstOrNull { it.id == userId }
            ?.toAdminUser()
            ?: throw IllegalArgumentException("Пользователь не найден")

    private suspend fun loadRolePermissions(roleId: Long): List<AdminPermission> =
        api.getRolePermissions(roleId.toBackendId()).map { it.toAdminPermission() }

    private suspend fun <T> request(block: suspend () -> T): T =
        try {
            block()
        } catch (error: Throwable) {
            throw IllegalStateException(error.toReadableMessage(), error)
        }

    private suspend fun fetchOptionalModerationItems(
        endpointName: String,
        requestBlock: suspend () -> kotlinx.serialization.json.JsonElement
    ): List<AdminModerationItem> =
        try {
            requestBlock().toAdminModerationItems()
        } catch (error: HttpException) {
            if (error.code() != HTTP_NOT_FOUND) {
                throw error
            }
            Log.w(TAG, "GET $endpointName returned 404, using empty list for this moderation source")
            emptyList()
        }

    /**
     * Идентификаторы ролей, прав и логов на бэке — обычные serial-ключи, они
     * помещаются в Int. Идентификатор пользователя через это не проходит:
     * `users.id` — это telegram_id, и в ручки он уезжает как Long.
     */
    private fun Long.toBackendId(): Int =
        requireNotNull(takeIf { it >= Int.MIN_VALUE.toLong() && it <= Int.MAX_VALUE.toLong() }?.toInt()) {
            "Некорректный идентификатор"
        }

    private companion object {
        const val TAG = "RemoteAdminRepo"
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
    }
}
