package com.example.ysukampus.ui.admin.state

enum class AdminHistoryEntityType {
    User,
    Role,
    Permission,
    Unknown
}

enum class AdminChangeOperationType {
    Create,
    Update,
    Delete,
    Unknown
}

data class AdminChangedField(
    val oldValue: String,
    val newValue: String
)

data class AdminChangeLogActor(
    val id: Long,
    val username: String?,
    val firstName: String
)

data class AdminChangeLogRecord(
    val id: Int,
    val entityType: AdminHistoryEntityType,
    /** Для записей о пользователях это telegram_id. */
    val entityId: Long,
    val operationType: AdminChangeOperationType,
    val canRestore: Boolean,
    val changedFields: Map<String, AdminChangedField>,
    val createdAt: String,
    val createdBy: Long,
    val actor: AdminChangeLogActor?
)

data class AdminChangedUser(
    val id: Long,
    val username: String?,
    val firstName: String,
    val lastChangedAt: String
)

data class AdminChangedRole(
    val id: Int,
    val name: String,
    val slug: String,
    val lastChangedAt: String,
    val isDeleted: Boolean
)

data class AdminChangedPermission(
    val id: Int,
    val name: String,
    val slug: String,
    val lastChangedAt: String,
    val isDeleted: Boolean
)

data class AdminRestoreChangeLogResult(
    val entityType: AdminHistoryEntityType,
    val entityId: Long
)
