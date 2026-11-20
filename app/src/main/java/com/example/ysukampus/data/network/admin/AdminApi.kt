package com.example.ysukampus.data.network.admin

import com.example.ysukampus.data.network.admin.dto.AdminPermissionDto
import com.example.ysukampus.data.network.admin.dto.AdminRoleDto
import com.example.ysukampus.data.network.admin.dto.AdminUserDto
import com.example.ysukampus.data.network.admin.dto.AssignPermissionToRoleRequestDto
import com.example.ysukampus.data.network.admin.dto.AssignRoleToUserRequestDto
import com.example.ysukampus.data.network.admin.dto.ChangeLogListDto
import com.example.ysukampus.data.network.admin.dto.ChangedPermissionsListDto
import com.example.ysukampus.data.network.admin.dto.ChangedRolesListDto
import com.example.ysukampus.data.network.admin.dto.ChangedUsersListDto
import com.example.ysukampus.data.network.admin.dto.CreatePermissionRequestDto
import com.example.ysukampus.data.network.admin.dto.CreateRoleRequestDto
import com.example.ysukampus.data.network.admin.dto.DailyReportDto
import com.example.ysukampus.data.network.admin.dto.DailyReportMetricsDto
import com.example.ysukampus.data.network.admin.dto.ReportJobDto
import com.example.ysukampus.data.network.admin.dto.ReportListDto
import com.example.ysukampus.data.network.admin.dto.ReportSeriesDto
import com.example.ysukampus.data.network.admin.dto.RestoreChangeLogResponseDto
import com.example.ysukampus.data.network.admin.dto.RequestLogFullDto
import com.example.ysukampus.data.network.admin.dto.RequestLogListDto
import com.example.ysukampus.data.network.admin.dto.RequestLogQueryDto
import com.example.ysukampus.data.network.admin.dto.SuccessMessageDto
import com.example.ysukampus.data.network.admin.dto.UpdatePermissionRequestDto
import com.example.ysukampus.data.network.admin.dto.UpdateRoleRequestDto
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @GET("forum/moderation/content")
    suspend fun getModerationContent(): JsonElement

    @GET("forum/moderation/reports")
    suspend fun getModerationReports(): JsonElement

    @GET("forum/moderation/posts")
    suspend fun getModerationPosts(): JsonElement

    @GET("forum/moderation/comments")
    suspend fun getModerationComments(): JsonElement

    @PATCH("forum/moderation/posts/{postId}/status")
    suspend fun updateModerationPostStatus(
        @Path("postId") postId: String,
        @Body body: JsonElement
    ): JsonElement

    @PATCH("forum/moderation/comments/{commentId}/status")
    suspend fun updateModerationCommentStatus(
        @Path("commentId") commentId: String,
        @Body body: JsonElement
    ): JsonElement

    @POST("forum/moderation/reports/{reportId}/resolve")
    suspend fun resolveModerationReport(
        @Path("reportId") reportId: String,
        @Body body: JsonElement
    ): JsonElement

    /** Вебка берёт одну большую страницу: `per_page=100`, без фильтров. */
    @GET("forum/moderation/history")
    suspend fun getModerationHistory(
        @Query("per_page") perPage: Int = 100
    ): JsonElement

    @POST("forum/moderation/content/{targetType}/{targetId}/restore")
    suspend fun restoreModerationContent(
        @Path("targetType") targetType: String,
        @Path("targetId") targetId: String,
        @Body body: JsonElement
    ): JsonElement

    @GET("admin/reports/daily")
    suspend fun getDailyReports(): ReportListDto

    /**
     * Живой слепок за сегодня: считается на лету, файла за эту дату ещё нет.
     * Путь обязан идти выше `daily/{date}`, иначе `today` уедет в разбор даты.
     */
    @GET("admin/reports/daily/today")
    suspend fun getTodayReportMetrics(): DailyReportMetricsDto

    @GET("admin/reports/daily/series")
    suspend fun getDailyReportSeries(
        @Query("metric") metric: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): ReportSeriesDto

    @GET("admin/reports/daily/{date}")
    suspend fun getDailyReport(
        @Path("date") date: String
    ): DailyReportDto

    /** Ставит отчёт в очередь; без `force` бэк отвечает 409 на готовый файл. */
    @POST("admin/reports/daily/generate")
    suspend fun generateDailyReport(
        @Query("date") date: String?,
        @Query("force") force: Boolean,
        @Body body: Map<String, String> = emptyMap()
    ): ReportJobDto

    @GET("ref/user/")
    suspend fun getUsers(): List<AdminUserDto>

    @GET("ref/policy/role")
    suspend fun getRoles(): List<AdminRoleDto>

    @POST("ref/policy/role")
    suspend fun createRole(
        @Body body: CreateRoleRequestDto
    ): AdminRoleDto

    @PUT("ref/policy/role/{roleId}")
    suspend fun updateRole(
        @Path("roleId") roleId: Int,
        @Body body: UpdateRoleRequestDto
    ): AdminRoleDto

    @DELETE("ref/policy/role/{roleId}/soft")
    suspend fun softDeleteRole(
        @Path("roleId") roleId: Int
    ): AdminRoleDto

    @DELETE("ref/policy/role/{roleId}")
    suspend fun hardDeleteRole(
        @Path("roleId") roleId: Int
    ): SuccessMessageDto

    @POST("ref/policy/role/{roleId}/restore")
    suspend fun restoreRole(
        @Path("roleId") roleId: Int,
        @Body body: Map<String, String> = emptyMap()
    ): AdminRoleDto

    @GET("ref/policy/permission")
    suspend fun getPermissions(): List<AdminPermissionDto>

    @POST("ref/policy/permission")
    suspend fun createPermission(
        @Body body: CreatePermissionRequestDto
    ): AdminPermissionDto

    @PUT("ref/policy/permission/{permissionId}")
    suspend fun updatePermission(
        @Path("permissionId") permissionId: Int,
        @Body body: UpdatePermissionRequestDto
    ): AdminPermissionDto

    @DELETE("ref/policy/permission/{permissionId}/soft")
    suspend fun softDeletePermission(
        @Path("permissionId") permissionId: Int
    ): AdminPermissionDto

    @DELETE("ref/policy/permission/{permissionId}")
    suspend fun hardDeletePermission(
        @Path("permissionId") permissionId: Int
    ): SuccessMessageDto

    @POST("ref/policy/permission/{permissionId}/restore")
    suspend fun restorePermission(
        @Path("permissionId") permissionId: Int,
        @Body body: Map<String, String> = emptyMap()
    ): AdminPermissionDto

    @POST("ref/user/{userId}/role")
    suspend fun assignRoleToUser(
        @Path("userId") userId: Long,
        @Body body: AssignRoleToUserRequestDto
    ): AdminRoleDto

    @DELETE("ref/user/{userId}/role/{roleId}/soft")
    suspend fun softRemoveRoleFromUser(
        @Path("userId") userId: Long,
        @Path("roleId") roleId: Int
    ): AdminRoleDto

    @DELETE("ref/user/{userId}/role/{roleId}")
    suspend fun hardRemoveRoleFromUser(
        @Path("userId") userId: Long,
        @Path("roleId") roleId: Int
    ): SuccessMessageDto

    @GET("ref/policy/role/{roleId}/permission")
    suspend fun getRolePermissions(
        @Path("roleId") roleId: Int
    ): List<AdminPermissionDto>

    @POST("ref/policy/role/{roleId}/permission")
    suspend fun assignPermissionToRole(
        @Path("roleId") roleId: Int,
        @Body body: AssignPermissionToRoleRequestDto
    ): AdminPermissionDto

    // TODO: confirm the backend soft-detach path. API_MAPPING_ADMIN.md only documents hard detach here.
    @DELETE("ref/policy/role/{roleId}/permission/{permissionId}/soft")
    suspend fun softRemovePermissionFromRole(
        @Path("roleId") roleId: Int,
        @Path("permissionId") permissionId: Int
    ): SuccessMessageDto

    @DELETE("ref/policy/role/{roleId}/permission/{permissionId}")
    suspend fun hardRemovePermissionFromRole(
        @Path("roleId") roleId: Int,
        @Path("permissionId") permissionId: Int
    ): SuccessMessageDto

    @GET("ref/changelog/changed/user")
    suspend fun getChangedUsers(): ChangedUsersListDto

    @GET("ref/changelog/changed/role")
    suspend fun getChangedRoles(): ChangedRolesListDto

    @GET("ref/changelog/changed/permission")
    suspend fun getChangedPermissions(): ChangedPermissionsListDto

    @GET("ref/user/{userId}/story")
    suspend fun getUserStory(
        @Path("userId") userId: Long
    ): ChangeLogListDto

    @GET("ref/policy/role/{roleId}/story")
    suspend fun getRoleStory(
        @Path("roleId") roleId: Int
    ): ChangeLogListDto

    @GET("ref/policy/permission/{permissionId}/story")
    suspend fun getPermissionStory(
        @Path("permissionId") permissionId: Int
    ): ChangeLogListDto

    @POST("ref/changelog/{logId}/restore")
    suspend fun restoreChangeLog(
        @Path("logId") logId: Int,
        @Body body: Map<String, String> = emptyMap()
    ): RestoreChangeLogResponseDto

    @POST("ref/log/request")
    suspend fun getRequestLogs(
        @Body body: RequestLogQueryDto
    ): RequestLogListDto

    @GET("ref/log/request/{logId}")
    suspend fun getRequestLog(
        @Path("logId") logId: Int
    ): RequestLogFullDto

    @DELETE("ref/log/request/{logId}")
    suspend fun deleteRequestLog(
        @Path("logId") logId: Int
    ): SuccessMessageDto
}
