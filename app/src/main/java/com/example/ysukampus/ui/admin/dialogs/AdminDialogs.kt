package com.example.ysukampus.ui.admin.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.admin.AdminActionKind
import com.example.ysukampus.ui.admin.AdminPermission
import com.example.ysukampus.ui.admin.AdminRole
import com.example.ysukampus.ui.admin.AdminSelectOption
import com.example.ysukampus.ui.admin.AdminStatus
import com.example.ysukampus.ui.admin.AdminUser
import com.example.ysukampus.ui.admin.MockPermissions
import com.example.ysukampus.ui.admin.MockRoles
import com.example.ysukampus.ui.admin.MockUsers
import com.example.ysukampus.ui.admin.components.AdminActionButton
import com.example.ysukampus.ui.admin.components.AdminCodeBlock
import com.example.ysukampus.ui.admin.components.AdminDialogFrame
import com.example.ysukampus.ui.admin.components.AdminEmptyStateCard
import com.example.ysukampus.ui.admin.components.AdminPrimaryButton
import com.example.ysukampus.ui.admin.components.AdminSearchField
import com.example.ysukampus.ui.admin.components.AdminSectionLabel
import com.example.ysukampus.ui.admin.components.AdminSelect
import com.example.ysukampus.ui.admin.components.AdminSheetBlock
import com.example.ysukampus.ui.admin.components.AdminStatusBadge
import com.example.ysukampus.ui.admin.components.AdminTextArea
import com.example.ysukampus.ui.admin.components.AdminTextField
import com.example.ysukampus.ui.admin.components.PermissionListItem
import com.example.ysukampus.ui.admin.components.RoleListItem
import com.example.ysukampus.ui.admin.state.AdminPermissionInput
import com.example.ysukampus.ui.admin.state.AdminChangeLogRecord
import com.example.ysukampus.ui.admin.state.AdminChangeOperationType
import com.example.ysukampus.ui.admin.state.AdminHistoryEntityType
import com.example.ysukampus.ui.admin.state.AdminRequestLogDetails
import com.example.ysukampus.ui.admin.state.AdminRoleInput
import com.example.ysukampus.ui.admin.state.AdminRequestLogsFilter
import com.example.ysukampus.ui.theme.YsuAdminColors
import com.example.ysukampus.ui.theme.YsuAdminDimens
import com.example.ysukampus.ui.theme.YsuAdminTextStyles

@Composable
fun UserSettingsDialog(
    user: AdminUser = MockUsers.first(),
    roles: List<AdminRole> = MockRoles,
    onClose: () -> Unit,
    onAssignRole: (userId: Long, roleId: Long) -> Unit = { _, _ -> },
    onSoftRemoveRole: (userId: Long, roleId: Long) -> Unit = { _, _ -> },
    onHardRemoveRole: (userId: Long, roleId: Long) -> Unit = { _, _ -> },
    isSaving: Boolean = false,
    isDeleting: Boolean = false,
    initialSelectedRoleId: Int? = null,
    forceSelectExpanded: Boolean = false
) {
    var selectedRoleId by rememberSaveable { mutableStateOf(initialSelectedRoleId) }
    var newRole by remember { mutableStateOf<AdminSelectOption?>(null) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val assignableRoles = roles.filter { role ->
        !role.isDeleted && user.roles.none { userRole -> userRole.id == role.id }
    }
    val selectedCurrentRoleId = selectedRoleId?.takeIf { roleId ->
        user.roles.any { role -> role.id == roleId }
    }
    AdminDialogFrame(title = "Настройки пользователя", onClose = onClose) {
        AdminSheetBlock {
            AdminSectionLabel("ТЕКУЩИЕ РОЛИ")
            user.roles.forEach { role ->
                RoleListItem(role = role, selected = selectedRoleId == role.id, onClick = { selectedRoleId = if (selectedRoleId == role.id) null else role.id })
            }
            if (selectedCurrentRoleId != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminActionButton(
                        if (isDeleting) "Удаление..." else "Мягко снять",
                        AdminActionKind.SoftWarning,
                        onClick = {
                            if (!isDeleting) {
                                onSoftRemoveRole(user.id.toLong(), selectedCurrentRoleId.toLong())
                                selectedRoleId = null
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting
                    )
                    AdminActionButton(
                        if (isDeleting) "Удаление..." else "Снять совсем",
                        AdminActionKind.Danger,
                        onClick = {
                            if (!isDeleting) {
                                onHardRemoveRole(user.id.toLong(), selectedCurrentRoleId.toLong())
                                selectedRoleId = null
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            AdminSectionLabel("НАЗНАЧИТЬ НОВУЮ РОЛЬ")
            AdminSelect(
                value = newRole,
                placeholder = "Выбрать новую роль",
                options = assignableRoles.map { AdminSelectOption(it.id.toString(), it.name) },
                onValueChange = {
                    newRole = it
                    validationError = null
                },
                forceExpanded = forceSelectExpanded
            )
            ValidationText(validationError)
            AdminPrimaryButton(
                if (isSaving) "Сохранение..." else "Назначить роль",
                onClick = {
                    val roleId = newRole?.id?.toLongOrNull()
                    if (roleId == null) {
                        validationError = "Выберите роль для назначения"
                    } else if (!isSaving) {
                        validationError = null
                        onAssignRole(user.id.toLong(), roleId)
                        newRole = null
                    }
                },
                enabled = !isSaving
            )
        }
    }
}

@Composable
fun RoleSettingsDialog(
    role: AdminRole = MockRoles.first(),
    permissions: List<AdminPermission> = MockPermissions,
    assignedPermissions: List<AdminPermission> = MockPermissions.filterNot { it.isDeleted }.take(4),
    onClose: () -> Unit,
    onSave: (roleId: Long, input: AdminRoleInput) -> Unit = { _, _ -> },
    onAssignPermission: (roleId: Long, permissionId: Long) -> Unit = { _, _ -> },
    onSoftRemovePermission: (roleId: Long, permissionId: Long) -> Unit = { _, _ -> },
    onHardRemovePermission: (roleId: Long, permissionId: Long) -> Unit = { _, _ -> },
    onSoftDelete: (roleId: Long) -> Unit = {},
    onHardDelete: (roleId: Long) -> Unit = {},
    onRestore: (roleId: Long) -> Unit = {},
    isSaving: Boolean = false,
    isDeleting: Boolean = false
) {
    var name by rememberSaveable { mutableStateOf(role.name) }
    var slug by rememberSaveable { mutableStateOf(role.slug) }
    var description by rememberSaveable { mutableStateOf(role.description.orEmpty()) }
    var query by rememberSaveable { mutableStateOf("") }
    var addPermission by remember { mutableStateOf<AdminSelectOption?>(null) }
    var selectedPermissionId by rememberSaveable { mutableStateOf<Int?>(null) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var assignmentValidationError by rememberSaveable { mutableStateOf<String?>(null) }
    val assigned = assignedPermissions.filter { permission ->
        query.isBlank() || permission.name.contains(query, true) || permission.slug.contains(query, true)
    }
    val availablePermissions = permissions.filter { permission ->
        !permission.isDeleted && assignedPermissions.none { assignedPermission -> assignedPermission.id == permission.id }
    }
    val selectedAssignedPermissionId = selectedPermissionId?.takeIf { permissionId ->
        assignedPermissions.any { permission -> permission.id == permissionId }
    }
    AdminDialogFrame(title = "Настройки роли", onClose = onClose) {
        AdminSheetBlock {
            AdminSectionLabel("НАЗВАНИЕ")
            AdminTextField(name, { name = it })
            AdminSectionLabel("СЛУЖЕБНОЕ ИМЯ")
            AdminTextField(slug, { slug = it })
            AdminSectionLabel("ОПИСАНИЕ")
            AdminTextArea(description, { description = it })
            ValidationText(validationError)
            AdminPrimaryButton(
                if (isSaving) "Сохранение..." else "Сохранить изменения",
                onClick = {
                    val input = normalizedRoleInput(name, slug, description)
                    validationError = validateAdminInput(input.name, input.slug)
                    if (validationError == null && !isSaving) {
                        onSave(role.id.toLong(), input)
                    }
                },
                enabled = !isSaving
            )
        }
        Spacer(Modifier.height(12.dp))
        AdminSheetBlock {
            AdminSectionLabel("НАЗНАЧЕННЫЕ РАЗРЕШЕНИЯ")
            AdminSearchField(query, { query = it }, "Поиск разрешения")
            assigned.forEach { permission ->
                PermissionListItem(
                    permission,
                    selected = selectedPermissionId == permission.id,
                    onClick = {
                        selectedPermissionId = if (selectedPermissionId == permission.id) null else permission.id
                    }
                )
            }
            if (assigned.isEmpty()) {
                AdminEmptyStateCard(
                    title = "У роли нет разрешений",
                    subtitle = "Добавьте новое разрешение ниже"
                )
            }
            if (selectedAssignedPermissionId != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminActionButton(
                        if (isDeleting) "Удаление..." else "Мягко снять",
                        AdminActionKind.SoftWarning,
                        onClick = {
                            if (!isDeleting) {
                                onSoftRemovePermission(role.id.toLong(), selectedAssignedPermissionId.toLong())
                                selectedPermissionId = null
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting
                    )
                    AdminActionButton(
                        if (isDeleting) "Удаление..." else "Снять совсем",
                        AdminActionKind.Danger,
                        onClick = {
                            if (!isDeleting) {
                                onHardRemovePermission(role.id.toLong(), selectedAssignedPermissionId.toLong())
                                selectedPermissionId = null
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting
                    )
                }
            }
            AdminSectionLabel("ДОБАВИТЬ НОВОЕ РАЗРЕШЕНИЕ")
            AdminSelect(
                value = addPermission,
                placeholder = "Выбрать разрешение",
                options = availablePermissions.map { AdminSelectOption(it.id.toString(), it.name) },
                onValueChange = {
                    addPermission = it
                    assignmentValidationError = null
                }
            )
            ValidationText(assignmentValidationError)
            AdminPrimaryButton(
                if (isSaving) "Сохранение..." else "Добавить разрешение",
                onClick = {
                    val permissionId = addPermission?.id?.toLongOrNull()
                    if (permissionId == null) {
                        assignmentValidationError = "Выберите разрешение для добавления"
                    } else if (!isSaving) {
                        assignmentValidationError = null
                        onAssignPermission(role.id.toLong(), permissionId)
                        addPermission = null
                    }
                },
                enabled = !isSaving
            )
        }
        Spacer(Modifier.height(12.dp))
        AdminSheetBlock {
            AdminSectionLabel("ДЕЙСТВИЯ НАД РОЛЬЮ")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (role.isDeleted) {
                    AdminActionButton(
                        if (isDeleting) "Удаление..." else "Восстановить",
                        AdminActionKind.Restore,
                        onClick = { if (!isDeleting) onRestore(role.id.toLong()) },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting
                    )
                } else {
                    AdminActionButton(
                        if (isDeleting) "Удаление..." else "Мягко удалить",
                        AdminActionKind.SoftWarning,
                        onClick = { if (!isDeleting) onSoftDelete(role.id.toLong()) },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting
                    )
                }
                AdminActionButton(
                    if (isDeleting) "Удаление..." else "Удалить совсем",
                    AdminActionKind.Danger,
                    onClick = { if (!isDeleting) onHardDelete(role.id.toLong()) },
                    modifier = Modifier.weight(1f),
                    enabled = !isDeleting
                )
            }
        }
    }
}

@Composable
fun AddRoleDialog(
    onClose: () -> Unit,
    permissions: List<AdminPermission> = MockPermissions,
    onSave: (AdminRoleInput, List<Long>) -> Unit = { _, _ -> },
    isSaving: Boolean = false
) {
    var name by rememberSaveable { mutableStateOf("") }
    var slug by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var permission by remember { mutableStateOf<AdminSelectOption?>(null) }
    var selectedPermissionIds by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var selectedPermissionId by rememberSaveable { mutableStateOf<Int?>(null) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var assignmentValidationError by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPermissions = permissions.filter { permissionItem ->
        selectedPermissionIds.contains(permissionItem.id)
    }
    val availablePermissions = permissions.filter { permissionItem ->
        !permissionItem.isDeleted && !selectedPermissionIds.contains(permissionItem.id)
    }
    val selectedAssignedPermissionId = selectedPermissionId?.takeIf { permissionId ->
        selectedPermissionIds.contains(permissionId)
    }
    AdminDialogFrame(title = "Добавить роль", onClose = onClose) {
        AdminSheetBlock {
            AdminSectionLabel("НАЗВАНИЕ")
            AdminTextField(name, { name = it }, "Введите название")
            AdminSectionLabel("СЛУЖЕБНОЕ ИМЯ")
            AdminTextField(slug, { slug = it }, "Введите служебное имя")
            AdminSectionLabel("ОПИСАНИЕ")
            AdminTextArea(description, { description = it }, "Введите описание")
        }
        Spacer(Modifier.height(12.dp))
        AdminSheetBlock {
            AdminSectionLabel("НАЗНАЧЕННЫЕ РАЗРЕШЕНИЯ")
            if (selectedPermissions.isEmpty()) {
                AdminEmptyStateCard(
                    title = "У роли нет разрешений",
                    subtitle = "Добавьте новое разрешение ниже"
                )
            } else {
                selectedPermissions.forEach { selectedPermission ->
                    PermissionListItem(
                        selectedPermission,
                        selected = selectedPermissionId == selectedPermission.id,
                        onClick = {
                            selectedPermissionId = if (selectedPermissionId == selectedPermission.id) {
                                null
                            } else {
                                selectedPermission.id
                            }
                        }
                    )
                }
            }
            if (selectedAssignedPermissionId != null) {
                AdminActionButton(
                    "Снять совсем",
                    AdminActionKind.Danger,
                    onClick = {
                        selectedPermissionIds = selectedPermissionIds - selectedAssignedPermissionId
                        selectedPermissionId = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AdminSectionLabel("ДОБАВИТЬ НОВОЕ РАЗРЕШЕНИЕ")
            AdminSelect(
                value = permission,
                placeholder = "Выбрать разрешение",
                options = availablePermissions.map { AdminSelectOption(it.id.toString(), it.name) },
                onValueChange = {
                    permission = it
                    assignmentValidationError = null
                }
            )
            ValidationText(assignmentValidationError)
            AdminPrimaryButton(
                "Добавить разрешение",
                onClick = {
                    val permissionId = permission?.id?.toIntOrNull()
                    when {
                        permissionId == null -> {
                            assignmentValidationError = "Выберите разрешение для добавления"
                        }
                        selectedPermissionIds.contains(permissionId) -> {
                            assignmentValidationError = "Разрешение уже добавлено"
                        }
                        else -> {
                            assignmentValidationError = null
                            selectedPermissionIds = selectedPermissionIds + permissionId
                            permission = null
                        }
                    }
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        ValidationText(validationError)
        AdminPrimaryButton(
            if (isSaving) "Сохранение..." else "Добавить роль",
            onClick = {
                val input = normalizedRoleInput(name, slug, description)
                validationError = validateAdminInput(input.name, input.slug)
                if (validationError == null && !isSaving) {
                    onSave(input, selectedPermissionIds.map { it.toLong() })
                }
            },
            enabled = !isSaving
        )
    }
}

@Composable
fun AddPermissionDialog(
    onClose: () -> Unit,
    onSave: (AdminPermissionInput) -> Unit = {},
    isSaving: Boolean = false
) {
    PermissionFormDialog(
        title = "Добавить разрешение",
        button = "Добавить разрешение",
        permission = AdminPermission(0, "", ""),
        onClose = onClose,
        onSave = { _, input -> onSave(input) },
        isSaving = isSaving
    )
}

@Composable
fun PermissionSettingsDialog(
    permission: AdminPermission = MockPermissions.first(),
    onClose: () -> Unit,
    onSave: (permissionId: Long, input: AdminPermissionInput) -> Unit = { _, _ -> },
    onSoftDelete: (permissionId: Long) -> Unit = {},
    onHardDelete: (permissionId: Long) -> Unit = {},
    onRestore: (permissionId: Long) -> Unit = {},
    isSaving: Boolean = false,
    isDeleting: Boolean = false
) {
    PermissionFormDialog(
        title = "Настройки разрешения",
        button = "Сохранить изменения",
        permission = permission,
        onClose = onClose,
        onSave = onSave,
        showActions = true,
        onSoftDelete = onSoftDelete,
        onHardDelete = onHardDelete,
        onRestore = onRestore,
        isSaving = isSaving,
        isDeleting = isDeleting
    )
}

@Composable
fun HistoryRecordsDialog(
    records: List<AdminChangeLogRecord>,
    isLoading: Boolean,
    onRecordClick: (Int) -> Unit,
    onClose: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val sortedRecords = records.sortedWith(
        compareByDescending<AdminChangeLogRecord> { historyDateSortKey(it.createdAt) }
            .thenByDescending { it.id }
    )
    val filtered = sortedRecords.filter { record ->
        val operation = record.operationType.historyOperationLabel()
        val author = record.historyAuthor()
        val date = formatHistoryDateTime(record.createdAt)
        query.isBlank() ||
            record.id.toString().contains(query, ignoreCase = true) ||
            operation.contains(query, ignoreCase = true) ||
            author.contains(query, ignoreCase = true) ||
            date.contains(query, ignoreCase = true) ||
            record.changedFields.keys.any { it.contains(query, ignoreCase = true) }
    }
    AdminDialogFrame(title = "История изменений", onClose = onClose) {
        AdminSearchField(query, { query = it }, "Поиск по записи...")
        Spacer(Modifier.height(14.dp))
        AdminSheetBlock {
            AdminSectionLabel("ЗАПИСИ")
            when {
                isLoading -> AdminEmptyStateCard(
                    title = "Загрузка записей...",
                    subtitle = "История изменений появится после загрузки"
                )
                records.isEmpty() -> AdminEmptyStateCard(
                    title = "Записей истории пока нет",
                    subtitle = "Изменения по выбранной сущности появятся здесь"
                )
                filtered.isEmpty() -> AdminEmptyStateCard(
                    title = "Ничего не найдено по запросу",
                    subtitle = "Попробуйте изменить поисковый запрос"
                )
                else -> filtered.forEach { record ->
                    HistoryRecordListItem(record = record, onClick = { onRecordClick(record.id) })
                }
            }
        }
    }
}

@Composable
fun HistoryRecordDialog(
    record: AdminChangeLogRecord,
    isRestoring: Boolean,
    onBack: () -> Unit,
    onRestore: (Long) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66111827)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.90f).fillMaxHeight(0.84f),
            shape = RoundedCornerShape(YsuAdminDimens.DialogRadius),
            colors = CardDefaults.cardColors(containerColor = YsuAdminColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(YsuAdminDimens.DialogPadding)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    DialogIconButton("↩", onBack)
                    Text("Запись истории", style = YsuAdminTextStyles.DialogTitle, color = YsuAdminColors.Text)
                    DialogIconButton("×", onClose)
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminSheetBlock {
                        HistoryDetailField("Сущность", record.entityType.historyEntityLabel())
                        HistoryDetailField("ID записи", record.entityId.toString())
                        HistoryDetailField("Тип операции", record.operationType.historyOperationLabel())
                        HistoryDetailField("Кто изменил", record.historyAuthor(), "ID ${record.createdBy}")
                        HistoryDetailField("Дата изменения", formatHistoryDateTime(record.createdAt))
                    }
                    if (record.changedFields.isNotEmpty()) {
                        AdminSheetBlock {
                            AdminSectionLabel("ИЗМЕНЕННЫЕ ПОЛЯ")
                            record.changedFields.forEach { (fieldName, field) ->
                                AdminEmptyStateCard(
                                    title = fieldName,
                                    subtitle = "Было: ${field.oldValue.ifBlank { "—" }}\nСтало: ${field.newValue.ifBlank { "—" }}"
                                )
                            }
                        }
                    }
                    if (record.canRestore) {
                        AdminPrimaryButton(
                            text = if (isRestoring) "Восстановление..." else "Восстановить состояние",
                            onClick = { if (!isRestoring) onRestore(record.id.toLong()) },
                            enabled = !isRestoring
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestLogDetailsDialog(
    log: AdminRequestLogDetails?,
    isLoading: Boolean,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var activeTab by rememberSaveable { mutableStateOf(RequestLogDetailsTab.Request) }
    AdminDialogFrame(title = "Запись API-запроса", onClose = onClose) {
        when {
            isLoading && log == null -> {
                AdminEmptyStateCard(
                    title = "Загрузка записи...",
                    subtitle = "Детали API-запроса появятся после загрузки"
                )
            }

            log == null -> {
                AdminEmptyStateCard(
                    title = "Запись лога не найдена",
                    subtitle = "Попробуйте открыть другую запись"
                )
            }

            else -> {
                AdminSheetBlock {
                    RequestLogInfoField("URL", log.fullUrl)
                    RequestLogInfoField("Метод", log.method.uppercase())
                    RequestLogInfoField("Контроллер", log.controllerPath.orDash())
                    RequestLogInfoField("Метод контроллера", log.controllerMethod.orDash())
                    RequestLogInfoField("Пользователь", log.userId?.toString() ?: "—")
                    RequestLogInfoField("IP адрес", log.ipAddress.orDash())
                    RequestLogInfoField("User-Agent", log.userAgent.orDash())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Статус ответа", style = YsuAdminTextStyles.CardTitle, color = YsuAdminColors.Text)
                        AdminStatusBadge(
                            text = log.responseStatus.toString(),
                            status = log.responseStatus.toAdminStatus()
                        )
                    }
                    RequestLogInfoField("Дата вызова", formatRequestLogDateTime(log.calledAt))
                }
                Spacer(Modifier.height(12.dp))
                RequestLogDetailsTabs(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it }
                )
                Spacer(Modifier.height(10.dp))
                when (activeTab) {
                    RequestLogDetailsTab.Request -> {
                        AdminSheetBlock {
                            AdminSectionLabel("ЗАГОЛОВКИ ЗАПРОСА")
                            AdminCodeBlock(log.requestHeaders.asCodeBlockValue())
                            AdminSectionLabel("ТЕЛО ЗАПРОСА")
                            AdminCodeBlock(log.requestBody.asCodeBlockValue())
                        }
                    }

                    RequestLogDetailsTab.Response -> {
                        AdminSheetBlock {
                            AdminSectionLabel("ЗАГОЛОВКИ ОТВЕТА")
                            AdminCodeBlock(log.responseHeaders.asCodeBlockValue())
                            AdminSectionLabel("ТЕЛО ОТВЕТА")
                            AdminCodeBlock(log.responseBody.asCodeBlockValue())
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                AdminActionButton(
                    text = if (isDeleting) "Удаление..." else "Удалить запись",
                    kind = AdminActionKind.Danger,
                    onClick = { if (!isDeleting) onDelete() },
                    enabled = !isDeleting
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteRequestLogDialog(
    logId: Int,
    isDeleting: Boolean,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    AdminDialogFrame(title = "Подтверждение удаления", onClose = onCancel) {
        AdminSheetBlock {
            AdminEmptyStateCard(
                title = "Удалить запись лога?",
                subtitle = "Действие необратимо. ID записи: $logId"
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminActionButton(
                    text = "Отмена",
                    kind = AdminActionKind.SoftWarning,
                    onClick = onCancel,
                    enabled = !isDeleting,
                    modifier = Modifier.weight(1f)
                )
                AdminActionButton(
                    text = if (isDeleting) "Удаление..." else "Подтвердить удаление",
                    kind = AdminActionKind.Danger,
                    onClick = { if (!isDeleting) onConfirm(logId) },
                    enabled = !isDeleting,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
@Composable
fun RequestLogsFilterDialog(
    filter: AdminRequestLogsFilter,
    onApply: (AdminRequestLogsFilter) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf(filter.search) }
    var userId by rememberSaveable { mutableStateOf(filter.userId) }
    var responseStatus by rememberSaveable { mutableStateOf(filter.responseStatus) }
    var ipAddress by rememberSaveable { mutableStateOf(filter.ipAddress) }
    var controllerPath by rememberSaveable { mutableStateOf(filter.controllerPath) }
    var perPageText by rememberSaveable { mutableStateOf(filter.perPage.toString()) }
    var method by rememberSaveable { mutableStateOf(filter.method) }
    var sortBy by rememberSaveable { mutableStateOf(filter.sortBy) }
    var sortOrder by rememberSaveable { mutableStateOf(filter.sortOrder) }
    var statusGroup by rememberSaveable { mutableStateOf(filter.statusGroup) }
    var userIdError by rememberSaveable { mutableStateOf<String?>(null) }
    var responseStatusError by rememberSaveable { mutableStateOf<String?>(null) }
    var perPageError by rememberSaveable { mutableStateOf<String?>(null) }

    val methodOptions = listOf("GET", "POST", "PUT", "PATCH", "DELETE")
    val sortByOptions = listOf(
        AdminSelectOption("called_at", "Дата вызова"),
        AdminSelectOption("id", "ID записи"),
        AdminSelectOption("response_status", "Статус ответа"),
        AdminSelectOption("user_id", "ID пользователя"),
        AdminSelectOption("ip_address", "IP адрес"),
        AdminSelectOption("controller_path", "Путь контроллера")
    )
    val sortOrderOptions = listOf(
        AdminSelectOption("desc", "По убыванию"),
        AdminSelectOption("asc", "По возрастанию")
    )
    val statusGroupOptions = listOf("2xx", "3xx", "4xx", "5xx")

    AdminDialogFrame(title = "Фильтр", onClose = onClose) {
        AdminSheetBlock {
            AdminSectionLabel("ПОИСК")
            AdminTextField(search, { search = it }, "Введите поисковый запрос...")

            AdminSectionLabel("ID ПОЛЬЗОВАТЕЛЯ")
            AdminTextField(userId, { userId = it; userIdError = null }, "Введите ID пользователя...")
            ValidationText(userIdError)

            AdminSectionLabel("СТАТУС ОТВЕТА")
            AdminTextField(responseStatus, { responseStatus = it; responseStatusError = null }, "Введите статус ответа...")
            ValidationText(responseStatusError)

            AdminSectionLabel("IP АДРЕС")
            AdminTextField(ipAddress, { ipAddress = it }, "Введите IP адрес...")

            AdminSectionLabel("ПУТЬ КОНТРОЛЛЕРА")
            AdminTextField(controllerPath, { controllerPath = it }, "Введите путь контроллера...")

            AdminSectionLabel("КОЛИЧЕСТВО НА СТРАНИЦЕ")
            AdminTextField(perPageText, { perPageText = it; perPageError = null }, "Введите количество")
            ValidationText(perPageError)

            AdminSectionLabel("МЕТОД")
            AdminSelect(
                value = method?.let { AdminSelectOption(it, it) },
                placeholder = "Не выбрано",
                options = methodOptions.map { AdminSelectOption(it, it) },
                onValueChange = { method = it?.id }
            )

            AdminSectionLabel("СОРТИРОВАТЬ ПО")
            AdminSelect(
                value = sortByOptions.firstOrNull { it.id == sortBy },
                placeholder = "Дата вызова",
                options = sortByOptions,
                onValueChange = { sortBy = it?.id ?: "called_at" }
            )

            AdminSectionLabel("ПОРЯДОК")
            AdminSelect(
                value = sortOrderOptions.firstOrNull { it.id == sortOrder },
                placeholder = "По убыванию",
                options = sortOrderOptions,
                onValueChange = { sortOrder = it?.id ?: "desc" }
            )

            AdminSectionLabel("ГРУППА СТАТУСОВ")
            AdminSelect(
                value = statusGroup?.let { AdminSelectOption(it, it) },
                placeholder = "Не выбрано",
                options = statusGroupOptions.map { AdminSelectOption(it, it) },
                onValueChange = { statusGroup = it?.id }
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminActionButton(
                text = "Сбросить",
                kind = AdminActionKind.SoftWarning,
                onClick = onReset,
                modifier = Modifier.weight(1f)
            )
            AdminPrimaryButton(
                text = "Применить",
                onClick = {
                    val normalizedSearch = search.trim()
                    val normalizedUserId = userId.trim()
                    val normalizedResponseStatus = responseStatus.trim()
                    val normalizedIp = ipAddress.trim()
                    val normalizedControllerPath = controllerPath.trim()
                    val normalizedPerPageText = perPageText.trim()

                    val parsedUserId = normalizedUserId.toIntOrNull()
                    val parsedResponseStatus = normalizedResponseStatus.toIntOrNull()
                    val parsedPerPage = when {
                        normalizedPerPageText.isBlank() -> 10
                        else -> normalizedPerPageText.toIntOrNull()
                    }

                    userIdError = if (normalizedUserId.isNotBlank() && parsedUserId == null) {
                        "ID пользователя должен быть числом"
                    } else {
                        null
                    }
                    responseStatusError = if (normalizedResponseStatus.isNotBlank() && parsedResponseStatus == null) {
                        "Статус ответа должен быть числом"
                    } else {
                        null
                    }
                    perPageError = when {
                        parsedPerPage == null -> "Количество на странице должно быть числом"
                        parsedPerPage !in 1..100 -> "Количество на странице должно быть от 1 до 100"
                        else -> null
                    }

                    if (userIdError != null || responseStatusError != null || perPageError != null) {
                        return@AdminPrimaryButton
                    }

                    onApply(
                        AdminRequestLogsFilter(
                            search = normalizedSearch,
                            userId = normalizedUserId,
                            responseStatus = normalizedResponseStatus,
                            ipAddress = normalizedIp,
                            controllerPath = normalizedControllerPath,
                            method = method?.trim()?.ifBlank { null },
                            sortBy = sortBy.trim().ifBlank { "called_at" },
                            sortOrder = sortOrder.trim().ifBlank { "desc" },
                            statusGroup = statusGroup?.trim()?.ifBlank { null },
                            perPage = parsedPerPage ?: 10
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PermissionFormDialog(
    title: String,
    button: String,
    permission: AdminPermission,
    onClose: () -> Unit,
    onSave: (permissionId: Long, input: AdminPermissionInput) -> Unit,
    showActions: Boolean = false,
    onSoftDelete: (permissionId: Long) -> Unit = {},
    onHardDelete: (permissionId: Long) -> Unit = {},
    onRestore: (permissionId: Long) -> Unit = {},
    isSaving: Boolean = false,
    isDeleting: Boolean = false
) {
    var name by rememberSaveable { mutableStateOf(permission.name) }
    var slug by rememberSaveable { mutableStateOf(permission.slug) }
    var description by rememberSaveable { mutableStateOf(permission.description.orEmpty()) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    AdminDialogFrame(title = title, onClose = onClose) {
        AdminSheetBlock {
            AdminSectionLabel("НАЗВАНИЕ")
            AdminTextField(name, { name = it }, "Введите название")
            AdminSectionLabel("СЛУЖЕБНОЕ ИМЯ")
            AdminTextField(slug, { slug = it }, "Введите служебное имя")
            AdminSectionLabel("ОПИСАНИЕ")
            AdminTextArea(description, { description = it }, "Введите описание")
            ValidationText(validationError)
            AdminPrimaryButton(
                if (isSaving) "Сохранение..." else button,
                onClick = {
                    val input = normalizedPermissionInput(name, slug, description)
                    validationError = validateAdminInput(input.name, input.slug)
                    if (validationError == null && !isSaving) {
                        onSave(permission.id.toLong(), input)
                    }
                },
                enabled = !isSaving
            )
        }
        if (showActions) {
            Spacer(Modifier.height(12.dp))
            AdminSheetBlock {
                AdminSectionLabel("ДЕЙСТВИЯ НАД РАЗРЕШЕНИЕМ")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (permission.isDeleted) {
                        AdminActionButton(
                            if (isDeleting) "Удаление..." else "Восстановить",
                            AdminActionKind.Restore,
                            onClick = { if (!isDeleting) onRestore(permission.id.toLong()) },
                            modifier = Modifier.weight(1f),
                            enabled = !isDeleting
                        )
                    } else {
                        AdminActionButton(
                            if (isDeleting) "Удаление..." else "Мягко удалить",
                            AdminActionKind.SoftWarning,
                            onClick = { if (!isDeleting) onSoftDelete(permission.id.toLong()) },
                            modifier = Modifier.weight(1f),
                            enabled = !isDeleting
                        )
                    }
                    AdminActionButton(
                        if (isDeleting) "Удаление..." else "Удалить совсем",
                        AdminActionKind.Danger,
                        onClick = { if (!isDeleting) onHardDelete(permission.id.toLong()) },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordListItem(record: AdminChangeLogRecord, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(YsuAdminColors.Surface)
            .border(1.dp, YsuAdminColors.BorderStrong, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            "ID записи: ${record.id}",
            style = YsuAdminTextStyles.CardTitle,
            color = YsuAdminColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Тип: ${record.operationType.historyOperationLabel()}",
            style = YsuAdminTextStyles.CardSubtitle,
            color = YsuAdminColors.PrimaryDark
        )
        Text(
            "Изменений: ${record.changedFields.size}",
            style = YsuAdminTextStyles.CardSubtitle,
            color = YsuAdminColors.PrimaryDark
        )
        Text(
            "Автор: ${record.historyAuthor()}",
            style = YsuAdminTextStyles.CardSubtitle,
            color = YsuAdminColors.PrimaryDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Дата изменения: ${formatHistoryDateTime(record.createdAt)}",
            style = YsuAdminTextStyles.CardSubtitle,
            color = YsuAdminColors.PrimaryDark
        )
    }
}

@Composable
private fun HistoryDetailField(label: String, value: String, secondary: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = YsuAdminTextStyles.CardTitle, color = YsuAdminColors.Text)
        Text(value, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.Text)
        if (secondary != null) {
            Text(secondary, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.Text)
        }
    }
}

@Composable
private fun DialogIconButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(YsuAdminDimens.CloseButton)
            .clip(RoundedCornerShape(7.dp))
            .background(YsuAdminColors.PrimaryBright)
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text, color = YsuAdminColors.PrimaryDark, style = YsuAdminTextStyles.ScreenTitle)
    }
}

@Composable
private fun ValidationText(text: String?) {
    if (text != null) {
        Text(text = text, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.Danger)
    }
}

@Composable
private fun RequestLogDetailsTabs(
    activeTab: RequestLogDetailsTab,
    onTabSelected: (RequestLogDetailsTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(YsuAdminColors.PrimaryBright)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RequestLogDetailsTab.entries.forEach { tab ->
            val isActive = activeTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) YsuAdminColors.Primary else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    style = YsuAdminTextStyles.Tab,
                    color = if (isActive) Color.White else YsuAdminColors.PrimaryDark
                )
            }
        }
    }
}

@Composable
private fun RequestLogInfoField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = YsuAdminTextStyles.CardTitle, color = YsuAdminColors.Text)
        Text(value, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.Text)
    }
}

private fun validateAdminInput(name: String, slug: String): String? {
    return when {
        name.isBlank() -> "Название не должно быть пустым"
        slug.isBlank() -> "Служебное имя не должно быть пустым"
        !SlugPattern.matches(slug) -> "Служебное имя может содержать a-z, 0-9, _ и -"
        else -> null
    }
}

private fun normalizedRoleInput(
    name: String,
    slug: String,
    description: String
): AdminRoleInput =
    AdminRoleInput(
        name = name.trim(),
        slug = slug.trim(),
        description = description.trim()
    )

private fun normalizedPermissionInput(
    name: String,
    slug: String,
    description: String
): AdminPermissionInput =
    AdminPermissionInput(
        name = name.trim(),
        slug = slug.trim(),
        description = description.trim()
    )

private fun AdminChangeLogRecord.historyAuthor(): String =
    actor?.username
        ?: actor?.firstName
        ?: createdBy.toString()

private fun AdminChangeOperationType.historyOperationLabel(): String =
    when (this) {
        AdminChangeOperationType.Create -> "Создание"
        AdminChangeOperationType.Update -> "Изменение"
        AdminChangeOperationType.Delete -> "Удаление"
        AdminChangeOperationType.Unknown -> "Неизвестно"
    }

private fun AdminHistoryEntityType.historyEntityLabel(): String =
    when (this) {
        AdminHistoryEntityType.User -> "Пользователь"
        AdminHistoryEntityType.Role -> "Роль"
        AdminHistoryEntityType.Permission -> "Разрешение"
        AdminHistoryEntityType.Unknown -> "Неизвестно"
    }

private fun formatHistoryDateTime(value: String): String {
    val match = HistoryDateTimeRegex.find(value) ?: return value
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    val hour = match.groupValues[4]
    val minute = match.groupValues[5]
    return "$day.$month.$year, $hour:$minute"
}

private fun formatRequestLogDateTime(value: String): String {
    val match = HistoryDateTimeRegex.find(value) ?: return value
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    val hour = match.groupValues[4]
    val minute = match.groupValues[5]
    return "$day.$month.$year, $hour:$minute"
}

private fun historyDateSortKey(value: String): String =
    HistoryDateTimeRegex.find(value)?.let { match ->
        "${match.groupValues[1]}${match.groupValues[2]}${match.groupValues[3]}${match.groupValues[4]}${match.groupValues[5]}"
    } ?: DisplayDateTimeRegex.find(value)?.let { match ->
        "${match.groupValues[3]}${match.groupValues[2]}${match.groupValues[1]}${match.groupValues[4]}${match.groupValues[5]}"
    } ?: value

private val SlugPattern = Regex("^[a-z0-9_-]+$")
private val HistoryDateTimeRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})""")
private val DisplayDateTimeRegex = Regex("""^(\d{2})\.(\d{2})\.(\d{4}),\s*(\d{2}):(\d{2})""")

private enum class RequestLogDetailsTab(val title: String) {
    Request("Запрос"),
    Response("Ответ")
}

private fun Int.toAdminStatus(): AdminStatus =
    when (this) {
        in 200..299 -> AdminStatus.Active
        in 300..499 -> AdminStatus.SoftDeleted
        else -> AdminStatus.HardDanger
    }

private fun String?.orDash(): String =
    this?.takeIf { it.isNotBlank() } ?: "—"

private fun String.asCodeBlockValue(): String =
    if (isBlank()) "{}" else this
