package com.example.ysukampus.ui.admin.data

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
import kotlinx.coroutines.flow.StateFlow

interface AdminRepository {
    suspend fun getModerationContent(): List<AdminModerationItem>
    suspend fun getModerationReports(): List<AdminModerationReport>
    /** Вердикт модератора: `active` — одобрить, `hidden` — скрыть. */
    suspend fun updateModerationStatus(
        targetType: AdminModerationTargetType,
        targetId: String,
        status: String
    )

    /** Закрывает жалобу после вердикта по её контенту. */
    suspend fun resolveModerationReport(reportId: String)

    /** Лента истории модерации; нужно право `get-moderation-history`. */
    suspend fun getModerationHistory(): List<AdminModerationEvent>

    /** Возвращает скрытый контент в ленту; нужно право `restore-hidden-content`. */
    suspend fun restoreModerationContent(targetType: String, targetId: String)

    /** Слаги прав текущего пользователя; `null` — права неизвестны. */
    fun currentUserPermissions(): Set<String>?

    /**
     * Когда пользователь последний раз открывал подвкладку модерации. По этой
     * отметке подсвечиваются карточки, появившиеся с прошлого захода.
     */
    fun moderationTabSeenAt(tab: AdminModerationTab): Long

    fun markModerationTabSeen(tab: AdminModerationTab)

    fun observeModeratorWsEvents(): Flow<ModeratorWsEvent>
    fun observeModeratorWsConnectionState(): StateFlow<ModeratorWsConnectionState>
    fun startModeratorSocket()
    fun stopModeratorSocket()

    /** Список дневных отчётов; нужно право `view-daily-report`. */
    suspend fun getDailyReports(): List<AdminReportListItem>

    /** Содержимое отчёта за дату `YYYY-MM-DD`. */
    suspend fun getDailyReport(date: String): AdminDailyReport

    /** Живые метрики за сегодня: считаются на бэке на лету, файла ещё нет. */
    suspend fun getTodayReportMetrics(): Map<AdminReportMetric, Int>

    /** Динамика метрики по дням в диапазоне `YYYY-MM-DD`. */
    suspend fun getReportSeries(
        metric: AdminReportMetric,
        from: String,
        to: String
    ): AdminReportSeries?

    /**
     * Ставит генерацию отчёта в очередь; нужно право `generate-daily-report`.
     * Готовый файл бэк защищает 409 — это не ошибка, а повод спросить про
     * пересборку, поэтому ответ приходит значением, а не исключением.
     */
    suspend fun generateDailyReport(date: String, force: Boolean): AdminGenerateReportResult

    suspend fun getUsers(): List<AdminUser>
    suspend fun getRoles(): List<AdminRole>
    suspend fun getPermissions(): List<AdminPermission>

    suspend fun createRole(input: AdminRoleInput): AdminRole
    suspend fun updateRole(roleId: Long, input: AdminRoleInput): AdminRole
    suspend fun softDeleteRole(roleId: Long): AdminRole
    suspend fun hardDeleteRole(roleId: Long)
    suspend fun restoreRole(roleId: Long): AdminRole

    suspend fun createPermission(input: AdminPermissionInput): AdminPermission
    suspend fun updatePermission(permissionId: Long, input: AdminPermissionInput): AdminPermission
    suspend fun softDeletePermission(permissionId: Long): AdminPermission
    suspend fun hardDeletePermission(permissionId: Long)
    suspend fun restorePermission(permissionId: Long): AdminPermission

    suspend fun assignRoleToUser(userId: Long, roleId: Long): AdminUser
    suspend fun softRemoveRoleFromUser(userId: Long, roleId: Long): AdminUser
    suspend fun hardRemoveRoleFromUser(userId: Long, roleId: Long): AdminUser

    suspend fun getRolePermissions(roleId: Long): List<AdminPermission>
    suspend fun assignPermissionToRole(roleId: Long, permissionId: Long): List<AdminPermission>
    suspend fun softRemovePermissionFromRole(roleId: Long, permissionId: Long): List<AdminPermission>
    suspend fun hardRemovePermissionFromRole(roleId: Long, permissionId: Long): List<AdminPermission>

    suspend fun getChangedUsers(): List<AdminChangedUser>
    suspend fun getChangedRoles(): List<AdminChangedRole>
    suspend fun getChangedPermissions(): List<AdminChangedPermission>

    suspend fun getUserStory(userId: Long): List<AdminChangeLogRecord>
    suspend fun getRoleStory(roleId: Long): List<AdminChangeLogRecord>
    suspend fun getPermissionStory(permissionId: Long): List<AdminChangeLogRecord>

    suspend fun restoreChangeLog(logId: Long): AdminRestoreChangeLogResult

    suspend fun getRequestLogs(page: Int, filter: AdminRequestLogsFilter): AdminRequestLogsPage
    suspend fun getRequestLog(logId: Long): AdminRequestLogDetails
    suspend fun deleteRequestLog(logId: Long)
}
