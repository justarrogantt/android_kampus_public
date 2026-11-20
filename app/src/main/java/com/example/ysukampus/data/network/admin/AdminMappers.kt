package com.example.ysukampus.data.network.admin

import com.example.ysukampus.data.network.admin.dto.AdminPermissionDto
import com.example.ysukampus.data.network.admin.dto.AdminRoleDto
import com.example.ysukampus.data.network.admin.dto.AdminUserDto
import com.example.ysukampus.data.network.admin.dto.ActorDto
import com.example.ysukampus.data.network.admin.dto.ChangeLogDto
import com.example.ysukampus.data.network.admin.dto.ChangedFieldDto
import com.example.ysukampus.data.network.admin.dto.ChangedPermissionDto
import com.example.ysukampus.data.network.admin.dto.ChangedRoleDto
import com.example.ysukampus.data.network.admin.dto.ChangedUserDto
import com.example.ysukampus.data.network.admin.dto.CreatePermissionRequestDto
import com.example.ysukampus.data.network.admin.dto.CreateRoleRequestDto
import com.example.ysukampus.data.network.admin.dto.RestoreChangeLogResponseDto
import com.example.ysukampus.data.network.admin.dto.RequestLogFullDto
import com.example.ysukampus.data.network.admin.dto.RequestLogListDto
import com.example.ysukampus.data.network.admin.dto.RequestLogQueryDto
import com.example.ysukampus.data.network.admin.dto.RequestLogShortDto
import com.example.ysukampus.data.network.admin.dto.UpdatePermissionRequestDto
import com.example.ysukampus.data.network.admin.dto.UpdateRoleRequestDto
import com.example.ysukampus.ui.admin.AdminPermission
import com.example.ysukampus.ui.admin.AdminRole
import com.example.ysukampus.ui.admin.AdminUser
import com.example.ysukampus.ui.admin.state.AdminChangeLogActor
import com.example.ysukampus.ui.admin.state.AdminChangeLogRecord
import com.example.ysukampus.ui.admin.state.AdminChangeOperationType
import com.example.ysukampus.ui.admin.state.AdminChangedField
import com.example.ysukampus.ui.admin.state.AdminChangedPermission
import com.example.ysukampus.ui.admin.state.AdminChangedRole
import com.example.ysukampus.ui.admin.state.AdminChangedUser
import com.example.ysukampus.ui.admin.state.AdminHistoryEntityType
import com.example.ysukampus.ui.admin.state.AdminPermissionInput
import com.example.ysukampus.ui.admin.state.AdminRequestLogDetails
import com.example.ysukampus.ui.admin.state.AdminRequestLogListItem
import com.example.ysukampus.ui.admin.state.AdminRequestLogsFilter
import com.example.ysukampus.ui.admin.state.AdminRequestLogsPage
import com.example.ysukampus.ui.admin.state.AdminRestoreChangeLogResult
import com.example.ysukampus.ui.admin.state.AdminRoleInput
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

fun AdminUserDto.toAdminUser(): AdminUser =
    AdminUser(
        id = id,
        name = firstName,
        username = username,
        roles = roles
            .filter { it.deletedAt == null }
            .map { it.role.toAdminRole() }
    )

fun AdminRoleDto.toAdminRole(): AdminRole =
    AdminRole(
        id = id,
        name = name,
        slug = slug,
        description = description,
        isDeleted = deletedAt != null
    )

fun AdminPermissionDto.toAdminPermission(): AdminPermission =
    AdminPermission(
        id = id,
        name = name,
        slug = slug,
        description = description,
        isDeleted = deletedAt != null
    )

fun AdminRoleInput.toCreateRoleRequestDto(): CreateRoleRequestDto =
    CreateRoleRequestDto(
        name = name,
        slug = slug,
        description = description.takeIf { it.isNotBlank() }
    )

fun AdminRoleInput.toUpdateRoleRequestDto(): UpdateRoleRequestDto =
    UpdateRoleRequestDto(
        name = name,
        slug = slug,
        description = description.takeIf { it.isNotBlank() }
    )

fun AdminPermissionInput.toCreatePermissionRequestDto(): CreatePermissionRequestDto =
    CreatePermissionRequestDto(
        name = name,
        slug = slug,
        description = description.takeIf { it.isNotBlank() }
    )

fun AdminPermissionInput.toUpdatePermissionRequestDto(): UpdatePermissionRequestDto =
    UpdatePermissionRequestDto(
        name = name,
        slug = slug,
        description = description.takeIf { it.isNotBlank() }
    )

fun ChangedUserDto.toAdminChangedUser(): AdminChangedUser =
    AdminChangedUser(
        id = id,
        username = username,
        firstName = firstName,
        lastChangedAt = lastChangedAt
    )

fun ChangedRoleDto.toAdminChangedRole(): AdminChangedRole =
    AdminChangedRole(
        id = id,
        name = name,
        slug = slug,
        lastChangedAt = lastChangedAt,
        isDeleted = isDeleted
    )

fun ChangedPermissionDto.toAdminChangedPermission(): AdminChangedPermission =
    AdminChangedPermission(
        id = id,
        name = name,
        slug = slug,
        lastChangedAt = lastChangedAt,
        isDeleted = isDeleted
    )

fun ChangeLogDto.toAdminChangeLogRecord(): AdminChangeLogRecord =
    AdminChangeLogRecord(
        id = id,
        entityType = entityType.toAdminHistoryEntityType(),
        entityId = entityId,
        operationType = operationType.toAdminChangeOperationType(),
        canRestore = canRestore,
        changedFields = changedFields.mapValues { (_, field) -> field.toAdminChangedField() },
        createdAt = createdAt,
        createdBy = createdBy,
        actor = actor?.toAdminChangeLogActor()
    )

fun ActorDto.toAdminChangeLogActor(): AdminChangeLogActor =
    AdminChangeLogActor(
        id = id,
        username = username,
        firstName = firstName
    )

fun RestoreChangeLogResponseDto.toAdminRestoreChangeLogResult(): AdminRestoreChangeLogResult =
    AdminRestoreChangeLogResult(
        entityType = entityType.toAdminHistoryEntityType(),
        entityId = entityId
    )

fun RequestLogShortDto.toAdminRequestLogListItem(): AdminRequestLogListItem =
    AdminRequestLogListItem(
        id = id,
        fullUrl = fullUrl,
        method = method,
        controllerPath = controllerPath,
        controllerMethod = controllerMethod,
        responseStatus = responseStatus,
        calledAt = calledAt
    )

fun RequestLogFullDto.toAdminRequestLogDetails(): AdminRequestLogDetails =
    AdminRequestLogDetails(
        id = id,
        fullUrl = fullUrl,
        method = method.orEmpty(),
        controllerPath = controllerPath,
        controllerMethod = controllerMethod,
        requestBody = requestBody.toDisplayJson(),
        requestHeaders = requestHeaders.toDisplayJson(),
        userId = userId,
        ipAddress = ipAddress,
        userAgent = userAgent,
        responseStatus = responseStatus,
        responseBody = responseBody.toDisplayJson(),
        responseHeaders = responseHeaders.toDisplayJson(),
        calledAt = calledAt
    )

fun RequestLogListDto.toAdminRequestLogsPage(): AdminRequestLogsPage =
    AdminRequestLogsPage(
        items = items.map { it.toAdminRequestLogListItem() },
        page = page,
        totalPages = totalPages,
        totalCount = totalCount,
        perPage = perPage
    )

fun AdminRequestLogsFilter.toRequestLogQueryDto(page: Int): RequestLogQueryDto {
    val normalizedMethod = method?.trim()?.takeIf { it.isNotEmpty() }
    val normalizedStatusGroup = statusGroup?.trim()?.takeIf { it.isNotEmpty() }
    val normalizedSearch = search.trim().takeIf { it.isNotEmpty() }
    val filters = buildMap<String, JsonElement> {
        userId.trim().toIntOrNull()?.let { put("user_id", JsonPrimitive(it)) }
        responseStatus.trim().toIntOrNull()?.let { put("response_status", JsonPrimitive(it)) }
        ipAddress.trim().takeIf { it.isNotEmpty() }?.let { put("ip_address", JsonPrimitive(it)) }
        controllerPath.trim().takeIf { it.isNotEmpty() }?.let { put("controller_path", JsonPrimitive(it)) }
        normalizedMethod?.let { put("method", JsonPrimitive(it)) }
    }
    val sortByKey = sortBy.trim().ifBlank { "called_at" }
    val sortOrderValue = sortOrder.trim().lowercase().ifBlank { "desc" }
    return RequestLogQueryDto(
        page = page,
        perPage = perPage,
        filters = filters,
        sort = mapOf(sortByKey to sortOrderValue),
        statusGroup = normalizedStatusGroup,
        search = normalizedSearch
    )
}

private fun ChangedFieldDto.toAdminChangedField(): AdminChangedField =
    AdminChangedField(
        oldValue = oldValue.toDisplayValue(),
        newValue = newValue.toDisplayValue()
    )

private fun String.toAdminChangeOperationType(): AdminChangeOperationType =
    when (lowercase()) {
        "create" -> AdminChangeOperationType.Create
        "update" -> AdminChangeOperationType.Update
        "delete" -> AdminChangeOperationType.Delete
        else -> AdminChangeOperationType.Unknown
    }

private fun String.toAdminHistoryEntityType(): AdminHistoryEntityType =
    when (lowercase()) {
        "user", "role_user" -> AdminHistoryEntityType.User
        "role", "permission_role" -> AdminHistoryEntityType.Role
        "permission" -> AdminHistoryEntityType.Permission
        else -> AdminHistoryEntityType.Unknown
    }

private fun JsonElement?.toDisplayValue(): String =
    when (this) {
        null, JsonNull -> ""
        is JsonPrimitive -> {
            if (isString) {
                content
            } else {
                toString()
            }
        }
        else -> Json.encodeToString(this)
    }

private fun JsonElement?.toDisplayJson(): String =
    when (this) {
        null, JsonNull -> "{}"
        else -> PrettyPrintJson.encodeToString(this)
    }

private val PrettyPrintJson = Json { prettyPrint = true }
