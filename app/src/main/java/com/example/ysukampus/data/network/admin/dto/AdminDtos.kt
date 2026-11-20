package com.example.ysukampus.data.network.admin.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AdminUserDto(
    // id пользователя — это telegram_id: он давно не влезает в Int
    // (`users.id` на бэке — обычный питоновский int без верхней границы).
    val id: Long,
    val username: String? = null,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("forum_name")
    val forumName: String? = null,
    @SerialName("forum_color")
    val forumColor: String? = null,
    val role: String? = null,
    @SerialName("entity_id")
    val entityId: Int? = null,
    @SerialName("entity_oid")
    val entityOid: String? = null,
    @SerialName("entity_name")
    val entityName: String? = null,
    val faculty: String? = null,
    val roles: List<UserRoleLinkDto> = emptyList()
)

@Serializable
data class UserRoleLinkDto(
    val id: Int,
    @SerialName("user_id")
    val userId: Long,
    @SerialName("role_id")
    val roleId: Int,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null,
    // TODO: confirm every user-role link includes the nested role object.
    val role: AdminRoleDto
)

@Serializable
data class AdminRoleDto(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null
)

@Serializable
data class AdminPermissionDto(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null
)

@Serializable
data class CreateRoleRequestDto(
    val name: String,
    val slug: String,
    val description: String? = null
)

@Serializable
data class UpdateRoleRequestDto(
    val name: String,
    val slug: String,
    val description: String? = null
)

@Serializable
data class CreatePermissionRequestDto(
    val name: String,
    val slug: String,
    val description: String? = null
)

@Serializable
data class UpdatePermissionRequestDto(
    val name: String,
    val slug: String,
    val description: String? = null
)

@Serializable
data class AssignRoleToUserRequestDto(
    @SerialName("role_id")
    val roleId: Int
)

@Serializable
data class AssignPermissionToRoleRequestDto(
    @SerialName("permission_id")
    val permissionId: Int
)

@Serializable
data class SuccessMessageDto(
    val message: String
)

@Serializable
data class ChangedFieldDto(
    @SerialName("old")
    val oldValue: JsonElement? = null,
    @SerialName("new")
    val newValue: JsonElement? = null
)

@Serializable
data class ActorDto(
    val id: Long,
    val username: String? = null,
    @SerialName("first_name")
    val firstName: String
)

@Serializable
data class ChangeLogDto(
    val id: Int,
    @SerialName("entity_type")
    val entityType: String,
    // Для записей о пользователях здесь лежит telegram_id.
    @SerialName("entity_id")
    val entityId: Long,
    @SerialName("operation_type")
    val operationType: String,
    @SerialName("can_restore")
    val canRestore: Boolean,
    @SerialName("changed_fields")
    val changedFields: Map<String, ChangedFieldDto> = emptyMap(),
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("created_by")
    val createdBy: Long,
    val actor: ActorDto? = null
)

@Serializable
data class ChangeLogListDto(
    val items: List<ChangeLogDto> = emptyList(),
    val total: Int
)

@Serializable
data class ChangedUserDto(
    val id: Long,
    val username: String? = null,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_changed_at")
    val lastChangedAt: String
)

@Serializable
data class ChangedRoleDto(
    val id: Int,
    val name: String,
    val slug: String,
    @SerialName("last_changed_at")
    val lastChangedAt: String,
    @SerialName("is_deleted")
    val isDeleted: Boolean
)

@Serializable
data class ChangedPermissionDto(
    val id: Int,
    val name: String,
    val slug: String,
    @SerialName("last_changed_at")
    val lastChangedAt: String,
    @SerialName("is_deleted")
    val isDeleted: Boolean
)

@Serializable
data class ChangedUsersListDto(
    val items: List<ChangedUserDto> = emptyList(),
    val total: Int
)

@Serializable
data class ChangedRolesListDto(
    val items: List<ChangedRoleDto> = emptyList(),
    val total: Int
)

@Serializable
data class ChangedPermissionsListDto(
    val items: List<ChangedPermissionDto> = emptyList(),
    val total: Int
)

@Serializable
data class RestoreChangeLogResponseDto(
    @SerialName("entity_type")
    val entityType: String,
    @SerialName("entity_id")
    val entityId: Long,
    val entity: JsonElement? = null
)

@Serializable
data class RequestLogQueryDto(
    val page: Int,
    @SerialName("per_page")
    val perPage: Int,
    val filters: Map<String, JsonElement> = emptyMap(),
    val sort: Map<String, String> = mapOf("called_at" to "desc"),
    @SerialName("status_group")
    val statusGroup: String? = null,
    val search: String? = null
)

@Serializable
data class RequestLogShortDto(
    val id: Int,
    @SerialName("full_url")
    val fullUrl: String,
    val method: String? = null,
    @SerialName("controller_path")
    val controllerPath: String? = null,
    @SerialName("controller_method")
    val controllerMethod: String? = null,
    @SerialName("response_status")
    val responseStatus: Int,
    @SerialName("called_at")
    val calledAt: String
)

@Serializable
data class RequestLogFullDto(
    val id: Int,
    @SerialName("full_url")
    val fullUrl: String,
    val method: String? = null,
    @SerialName("controller_path")
    val controllerPath: String? = null,
    @SerialName("controller_method")
    val controllerMethod: String? = null,
    @SerialName("request_body")
    val requestBody: JsonElement? = null,
    @SerialName("request_headers")
    val requestHeaders: JsonElement? = null,
    @SerialName("user_id")
    val userId: Long? = null,
    @SerialName("ip_address")
    val ipAddress: String? = null,
    @SerialName("user_agent")
    val userAgent: String? = null,
    @SerialName("response_status")
    val responseStatus: Int,
    @SerialName("response_body")
    val responseBody: JsonElement? = null,
    @SerialName("response_headers")
    val responseHeaders: JsonElement? = null,
    @SerialName("called_at")
    val calledAt: String
)

@Serializable
data class RequestLogListDto(
    val items: List<RequestLogShortDto> = emptyList(),
    val page: Int,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("per_page")
    val perPage: Int
)
