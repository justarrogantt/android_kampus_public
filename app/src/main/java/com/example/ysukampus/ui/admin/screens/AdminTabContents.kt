package com.example.ysukampus.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.admin.AdminActionKind
import com.example.ysukampus.ui.admin.AdminPermission
import com.example.ysukampus.ui.admin.AdminRole
import com.example.ysukampus.ui.admin.AdminStatus
import com.example.ysukampus.ui.admin.AdminUser
import com.example.ysukampus.ui.admin.components.AdminActionButton
import com.example.ysukampus.ui.admin.components.AdminEmptyStateCard
import com.example.ysukampus.ui.admin.components.AdminEntityCard
import com.example.ysukampus.ui.admin.components.AdminFreshContentPill
import com.example.ysukampus.ui.admin.components.AdminHistoryEntityCard
import com.example.ysukampus.ui.admin.components.AdminLoadingStateCard
import com.example.ysukampus.ui.admin.components.AdminModerationCard
import com.example.ysukampus.ui.admin.components.AdminModerationQueueCounter
import com.example.ysukampus.ui.admin.components.AdminModerationSubTabs
import com.example.ysukampus.ui.admin.components.AdminRequestLogCard
import com.example.ysukampus.ui.admin.components.AdminSearchField
import com.example.ysukampus.ui.admin.components.AdminSegmentedTabs
import com.example.ysukampus.ui.admin.state.AdminAccessSection
import com.example.ysukampus.ui.admin.state.AdminChangedPermission
import com.example.ysukampus.ui.admin.state.AdminChangedRole
import com.example.ysukampus.ui.admin.state.AdminChangedUser
import com.example.ysukampus.ui.admin.state.AdminHistoryEntityType
import com.example.ysukampus.ui.admin.state.AdminModerationEvent
import com.example.ysukampus.ui.admin.state.AdminModerationEventTargetType
import com.example.ysukampus.ui.admin.state.AdminModerationItem
import com.example.ysukampus.ui.admin.state.AdminModerationReport
import com.example.ysukampus.ui.admin.state.AdminModerationTab
import com.example.ysukampus.ui.admin.state.AdminModerationTargetType
import com.example.ysukampus.ui.admin.state.AdminRequestLogListItem
import com.example.ysukampus.ui.theme.YsuAdminColors
import com.example.ysukampus.ui.theme.YsuAdminTextStyles
import java.time.Instant

@Composable
fun ModerationTabContent(
    selectedTab: AdminModerationTab,
    moderationItems: List<AdminModerationItem>,
    moderationReports: List<AdminModerationReport>,
    moderationHistoryEvents: List<AdminModerationEvent>,
    searchQuery: String = "",
    isLoading: Boolean = false,
    hasFreshContent: Boolean = false,
    freshEventsCount: Int = 0,
    moderationQueueCount: Int? = null,
    canViewHistory: Boolean = true,
    /** Всё, что новее этой отметки, подсвечивается как новое. */
    seenAtMillis: Long = 0L,
    onSearchQueryChange: (String) -> Unit = {},
    onModerationTabSelected: (AdminModerationTab) -> Unit = {},
    onFreshContentClick: () -> Unit = {},
    onModerationItemClick: (AdminModerationItem) -> Unit = {},
    onModerationReportClick: (AdminModerationReport) -> Unit = {},
    onModerationEventClick: (AdminModerationEvent) -> Unit = {}
) {
    val tabs = if (canViewHistory) {
        AdminModerationTab.entries
    } else {
        AdminModerationTab.entries.filterNot { it == AdminModerationTab.History }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminModerationSubTabs(
            currentTab = selectedTab,
            onTabSelected = onModerationTabSelected,
            tabs = tabs,
            dotTabs = buildSet {
                if (moderationItems.isNotEmpty()) add(AdminModerationTab.Moderation)
                if (moderationReports.isNotEmpty()) add(AdminModerationTab.Reports)
            }
        )
        if (moderationQueueCount != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AdminModerationQueueCounter(count = moderationQueueCount)
            }
        }
        AdminSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "Поиск по автору, типу и контенту"
        )
        if (selectedTab == AdminModerationTab.Moderation && hasFreshContent) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                AdminFreshContentPill(
                    count = freshEventsCount,
                    onClick = onFreshContentClick
                )
            }
        }
        when {
            isLoading -> AdminLoadingStateCard("Загрузка модерации...")
            selectedTab == AdminModerationTab.Reports -> {
                val filteredReports = moderationReports.filter { it.matchesSearch(searchQuery) }
                if (filteredReports.isEmpty()) {
                    AdminEmptyStateCard(
                        title = "Ничего не найдено",
                        subtitle = if (searchQuery.isBlank()) "Содержимое появится после загрузки" else "Попробуйте изменить поисковый запрос"
                    )
                } else {
                    filteredReports.forEach { report ->
                        AdminModerationCard(
                            iconTargetType = report.targetType,
                            title = report.authorName,
                            subtitle = report.targetType.searchLabel,
                            createdAt = formatModerationDateTime(report.createdAt),
                            badgeText = moderationStatusLabel(report.status.orEmpty()),
                            badgeStatus = moderationStatusBadge(report.status.orEmpty()),
                            isNew = isNewerThan(report.createdAt, seenAtMillis),
                            onClick = { onModerationReportClick(report) }
                        )
                    }
                }
            }
            selectedTab == AdminModerationTab.History -> {
                val filteredEvents = moderationHistoryEvents.filter { it.matchesSearch(searchQuery) }
                if (filteredEvents.isEmpty()) {
                    AdminEmptyStateCard(
                        title = "Ничего не найдено",
                        subtitle = if (searchQuery.isBlank()) "Содержимое появится после загрузки" else "Попробуйте изменить поисковый запрос"
                    )
                } else {
                    filteredEvents.forEach { event ->
                        AdminModerationCard(
                            iconTargetType = event.target.type.cardIconType,
                            title = event.actor.name,
                            subtitle = event.target.preview ?: event.target.type.label,
                            createdAt = formatModerationDateTime(event.ts),
                            badgeText = moderationActionLabel(event.action),
                            badgeStatus = moderationActionBadge(event),
                            isNew = isNewerThan(event.ts, seenAtMillis),
                            onClick = { onModerationEventClick(event) }
                        )
                    }
                }
            }
            else -> {
                val filteredItems = moderationItems.filter { it.matchesSearch(searchQuery) }
                if (hasFreshContent && filteredItems.isEmpty()) {
                    Spacer(Modifier.width(0.dp))
                } else if (filteredItems.isEmpty()) {
                    AdminEmptyStateCard(
                        title = "Ничего не найдено",
                        subtitle = if (searchQuery.isBlank()) "Содержимое появится после загрузки" else "Попробуйте изменить поисковый запрос"
                    )
                } else {
                    filteredItems.forEach { item ->
                        AdminModerationCard(
                            iconTargetType = item.targetType,
                            title = item.authorName,
                            subtitle = item.targetType.searchLabel,
                            createdAt = formatModerationDateTime(item.createdAt),
                            badgeText = moderationStatusLabel(item.status),
                            badgeStatus = moderationStatusBadge(item.status),
                            isNew = isNewerThan(item.createdAt, seenAtMillis),
                            onClick = { onModerationItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UsersTabContent(
    users: List<AdminUser>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onUserClick: (AdminUser) -> Unit
) {
    val filtered = users.filter { user ->
        searchQuery.isBlank() ||
            user.name.contains(searchQuery, ignoreCase = true) ||
            user.username.orEmpty().contains(searchQuery, ignoreCase = true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminSearchField(searchQuery, onSearchQueryChange, "Поиск по имени или тегу")
        if (filtered.isEmpty()) {
            AdminEmptyStateCard(
                title = if (searchQuery.isBlank()) "Пользователей пока нет" else "Пользователи не найдены",
                subtitle = if (searchQuery.isBlank()) {
                    "Список появится после загрузки данных"
                } else {
                    "Попробуйте изменить поисковый запрос"
                }
            )
        }
        filtered.forEach { user ->
            AdminEntityCard(
                iconText = "♙",
                title = user.name,
                subtitle = "@${user.username.orEmpty()}",
                chips = user.roles.map { it.name },
                onClick = { onUserClick(user) }
            )
        }
    }
}

@Composable
fun RolesTabContent(
    roles: List<AdminRole>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onRoleClick: (AdminRole) -> Unit
) {
    val filtered = roles.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.slug.contains(searchQuery, true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminSearchField(searchQuery, onSearchQueryChange, "Поиск по названию роли")
        if (filtered.isEmpty()) {
            AdminEmptyStateCard(
                title = if (searchQuery.isBlank()) "Ролей пока нет" else "Роли не найдены",
                subtitle = if (searchQuery.isBlank()) {
                    "Создайте роль или обновите список"
                } else {
                    "Попробуйте изменить поисковый запрос"
                }
            )
        }
        filtered.forEach { role ->
            AdminEntityCard(
                iconText = "◇",
                title = role.name,
                subtitle = role.slug,
                statusText = if (role.isDeleted) "удалена" else "активна",
                onClick = { onRoleClick(role) }
            )
        }
    }
}

@Composable
fun PermissionsTabContent(
    permissions: List<AdminPermission>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onPermissionClick: (AdminPermission) -> Unit
) {
    val filtered = permissions.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.slug.contains(searchQuery, true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminSearchField(searchQuery, onSearchQueryChange, "Поиск по названию права")
        if (filtered.isEmpty()) {
            AdminEmptyStateCard(
                title = if (searchQuery.isBlank()) "Прав пока нет" else "Права не найдены",
                subtitle = if (searchQuery.isBlank()) {
                    "Создайте право или обновите список"
                } else {
                    "Попробуйте изменить поисковый запрос"
                }
            )
        }
        filtered.forEach { permission ->
            AdminEntityCard(
                iconText = "♂",
                title = permission.name,
                subtitle = permission.slug,
                statusText = if (permission.isDeleted) "удалена" else "активна",
                onClick = { onPermissionClick(permission) }
            )
        }
    }
}

@Composable
fun HistoryTabContent(
    changedUsers: List<AdminChangedUser>,
    changedRoles: List<AdminChangedRole>,
    changedPermissions: List<AdminChangedPermission>,
    searchQuery: String = "",
    isLoading: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onHistoryEntityClick: (entityType: AdminHistoryEntityType, entityId: Long) -> Unit = { _, _ -> }
) {
    val items = buildHistoryItems(changedUsers, changedRoles, changedPermissions)
    val filtered = items.filter { item ->
        searchQuery.isBlank() ||
            item.title.contains(searchQuery, ignoreCase = true) ||
            item.subtitle.contains(searchQuery, ignoreCase = true) ||
            item.changedAt.contains(searchQuery, ignoreCase = true) ||
            item.badgeText.contains(searchQuery, ignoreCase = true) ||
            item.entityType.name.contains(searchQuery, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "Поиск по названию, типу или времени"
        )
        when {
            isLoading -> AdminLoadingStateCard("Загрузка истории...")
            items.isEmpty() -> AdminEmptyStateCard(
                title = "История изменений пуста",
                subtitle = "Измененные сущности появятся после загрузки данных"
            )
            filtered.isEmpty() -> AdminEmptyStateCard(
                title = "Ничего не найдено по запросу",
                subtitle = "Попробуйте изменить поисковый запрос"
            )
            else -> filtered.forEach { item ->
                AdminHistoryEntityCard(
                    iconText = item.iconText,
                    title = item.title,
                    subtitle = item.subtitle,
                    changedAt = item.changedAt,
                    badgeText = item.badgeText,
                    onClick = { onHistoryEntityClick(item.entityType, item.id) }
                )
            }
        }
    }
}

@Composable
fun LoggingTabContent(
    logs: List<AdminRequestLogListItem>,
    searchQuery: String,
    isLoading: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSearchQueryChange: (String) -> Unit,
    onLogClick: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val filtered = logs.filter { item ->
        searchQuery.isBlank() ||
            item.fullUrl.contains(searchQuery, ignoreCase = true) ||
            item.method.orEmpty().contains(searchQuery, ignoreCase = true) ||
            item.controllerPath.orEmpty().contains(searchQuery, ignoreCase = true) ||
            item.controllerMethod.orEmpty().contains(searchQuery, ignoreCase = true) ||
            item.responseStatus.toString().contains(searchQuery, ignoreCase = true) ||
            item.calledAt.contains(searchQuery, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "Поиск по URL, методу, статусу или времени"
        )
        when {
            isLoading -> AdminLoadingStateCard("Загрузка логов...")
            logs.isEmpty() -> AdminEmptyStateCard(
                title = "Логи пока не найдены",
                subtitle = "Записи API-запросов появятся после загрузки"
            )
            filtered.isEmpty() -> AdminEmptyStateCard(
                title = "Ничего не найдено по запросу",
                subtitle = "Попробуйте изменить поисковый запрос"
            )
            else -> filtered.forEach { log ->
                AdminRequestLogCard(
                    method = formatLogMethod(log.method),
                    path = resolveLogPath(log),
                    controllerMethod = log.controllerMethod.orEmpty().ifBlank { "Метод контроллера не указан" },
                    calledAt = formatLogDateTime(log.calledAt),
                    status = log.responseStatus,
                    onClick = { onLogClick(log.id) }
                )
            }
        }
        if (totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminActionButton(
                    text = "Назад",
                    kind = AdminActionKind.Primary,
                    enabled = currentPage > 1,
                    onClick = onPreviousPage,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Страница $currentPage из $totalPages",
                    style = YsuAdminTextStyles.CardSubtitle,
                    color = YsuAdminColors.TextSecondary
                )
                Spacer(Modifier.width(10.dp))
                AdminActionButton(
                    text = "Вперёд",
                    kind = AdminActionKind.Primary,
                    enabled = currentPage < totalPages,
                    onClick = onNextPage,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun buildHistoryItems(
    changedUsers: List<AdminChangedUser>,
    changedRoles: List<AdminChangedRole>,
    changedPermissions: List<AdminChangedPermission>
): List<AdminHistoryListItem> {
    val items = changedUsers.map { user ->
        AdminHistoryListItem(
            id = user.id,
            entityType = AdminHistoryEntityType.User,
            title = user.firstName,
            subtitle = user.username?.let { "@$it" }.orEmpty(),
            changedAtRaw = user.lastChangedAt,
            changedAt = formatHistoryDateTime(user.lastChangedAt),
            badgeText = "Пользователь",
            iconText = "♙"
        )
    } + changedRoles.map { role ->
        AdminHistoryListItem(
            id = role.id.toLong(),
            entityType = AdminHistoryEntityType.Role,
            title = role.name,
            subtitle = role.slug,
            changedAtRaw = role.lastChangedAt,
            changedAt = formatHistoryDateTime(role.lastChangedAt),
            badgeText = "Роль",
            iconText = "◇"
        )
    } + changedPermissions.map { permission ->
        AdminHistoryListItem(
            id = permission.id.toLong(),
            entityType = AdminHistoryEntityType.Permission,
            title = permission.name,
            subtitle = permission.slug,
            changedAtRaw = permission.lastChangedAt,
            changedAt = formatHistoryDateTime(permission.lastChangedAt),
            badgeText = "Разрешение",
            iconText = "♂"
        )
    }

    return if (items.all { it.sortKey != null }) {
        items.sortedByDescending { it.sortKey }
    } else {
        items
    }
}

private fun formatHistoryDateTime(value: String): String {
    val match = isoDateTimeRegex.find(value) ?: return value
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    val hour = match.groupValues[4]
    val minute = match.groupValues[5]
    return "$day.$month.$year, $hour:$minute"
}

private val isoDateTimeRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})""")
private val fullUrlPathRegex = Regex("""^(?:https?://[^/]+)?(/[^?#]*)""")

private data class AdminHistoryListItem(
    val id: Long,
    val entityType: AdminHistoryEntityType,
    val title: String,
    val subtitle: String,
    val changedAtRaw: String,
    val changedAt: String,
    val badgeText: String,
    val iconText: String
) {
    val sortKey: String? = isoDateTimeRegex.find(changedAtRaw)?.value
}

private fun formatLogMethod(method: String?): String = method?.uppercase().orEmpty().ifBlank { "GET" }

private fun resolveLogPath(item: AdminRequestLogListItem): String {
    val controllerPath = item.controllerPath.orEmpty().trim()
    if (controllerPath.isNotBlank()) return controllerPath
    val fullUrl = item.fullUrl.trim()
    val match = fullUrlPathRegex.find(fullUrl)?.groupValues?.getOrNull(1).orEmpty()
    return if (match.isNotBlank()) match else fullUrl
}

private fun formatLogDateTime(value: String): String {
    val match = isoDateTimeRegex.find(value) ?: return value
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    val hour = match.groupValues[4]
    val minute = match.groupValues[5]
    return "$day.$month.$year, $hour:$minute"
}

private fun AdminModerationItem.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    return listOf(
        authorName,
        content,
        status,
        createdAt,
        targetType.searchLabel,
        zone.orEmpty(),
        aiReason.orEmpty()
    ).any { it.contains(query, ignoreCase = true) }
}

private fun AdminModerationReport.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    return listOf(
        authorName,
        content,
        status.orEmpty(),
        createdAt,
        targetType.searchLabel,
        reportReason.orEmpty(),
        aiReason.orEmpty()
    ).any { it.contains(query, ignoreCase = true) }
}

private fun AdminModerationEvent.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    return listOf(
        actor.name,
        target.preview.orEmpty(),
        target.type.label,
        ts,
        moderationActionLabel(action)
    ).any { it.contains(query, ignoreCase = true) }
}

/** Подписи статусов контента и жалоб — те же, что в вебке. */
internal fun moderationStatusLabel(status: String): String = when (status.lowercase()) {
    "active" -> "Активен"
    "review" -> "На проверке"
    "pending" -> "Ожидает"
    "hidden" -> "Скрыт"
    "new" -> "Новый"
    "resolved" -> "Решен"
    "in_progress" -> "В работе"
    "rejected" -> "Отклонен"
    "reviewed" -> "Рассмотрен"
    else -> status
}

private fun moderationStatusBadge(status: String): AdminStatus = when (status.lowercase()) {
    "active" -> AdminStatus.Active
    "review" -> AdminStatus.SoftDeleted
    "pending" -> AdminStatus.HardDanger
    else -> AdminStatus.Primary
}

internal fun moderationActionLabel(action: String): String = when (action.lowercase()) {
    "ai_verdict" -> "Вердикт ИИ"
    "approve" -> "Одобрено"
    "hide" -> "Скрыто"
    "mute" -> "Мьют"
    "unmute" -> "Размьют"
    "resolve_report" -> "Жалоба"
    "delete_by_author" -> "Удалено автором"
    else -> action
}

private fun moderationActionBadge(event: AdminModerationEvent): AdminStatus =
    when (event.action.lowercase()) {
        "approve", "unmute" -> AdminStatus.Active
        "hide", "mute" -> AdminStatus.HardDanger
        "ai_verdict" -> when (event.details.zone?.lowercase()) {
            "green" -> AdminStatus.Active
            "yellow" -> AdminStatus.SoftDeleted
            "red" -> AdminStatus.HardDanger
            else -> AdminStatus.Primary
        }
        else -> AdminStatus.Primary
    }

private val AdminModerationEventTargetType.label: String
    get() = when (this) {
        AdminModerationEventTargetType.Post -> "Пост"
        AdminModerationEventTargetType.Comment -> "Комментарий"
        AdminModerationEventTargetType.User -> "Пользователь"
    }

/** У событий по пользователю своя иконка, поэтому тип цели тут может быть `null`. */
private val AdminModerationEventTargetType.cardIconType: AdminModerationTargetType?
    get() = when (this) {
        AdminModerationEventTargetType.Post -> AdminModerationTargetType.Post
        AdminModerationEventTargetType.Comment -> AdminModerationTargetType.Comment
        AdminModerationEventTargetType.User -> null
    }

/**
 * Время с бэкенда — naive UTC без таймзоны; без `Z` оно распарсится как
 * локальное, и «новыми» станут не те карточки (так же дописывает `Z` вебка).
 */
private fun isNewerThan(createdAt: String, seenAtMillis: Long): Boolean {
    if (seenAtMillis <= 0L) return false
    val normalized = if (createdAt.endsWith("Z", ignoreCase = true)) createdAt else "${createdAt}Z"
    val millis = runCatching { Instant.parse(normalized).toEpochMilli() }.getOrNull() ?: return false
    return millis > seenAtMillis
}

/** Общий формат даты для карточек модерации и окна «Детали модерации». */
internal fun formatModerationDateTime(value: String): String {
    val match = isoDateTimeRegex.find(value) ?: return value
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    val hour = match.groupValues[4]
    val minute = match.groupValues[5]
    return "$day.$month.$year, $hour:$minute"
}

private val AdminModerationTargetType.searchLabel: String
    get() = when (this) {
        AdminModerationTargetType.Post -> "Пост"
        AdminModerationTargetType.Comment -> "Комментарий"
    }

/**
 * «Доступы» — объединённая вкладка вебки (`AccessTab`): роли, права и история
 * изменений под одним заголовком, конкретный раздел выбирается сегментами.
 * Сами разделы не менялись — здесь только переключатель и проброс колбэков.
 */
@Composable
fun AccessTabContent(
    selectedSection: AdminAccessSection,
    availableSections: List<AdminAccessSection>,
    roles: List<AdminRole>,
    permissions: List<AdminPermission>,
    changedUsers: List<AdminChangedUser>,
    changedRoles: List<AdminChangedRole>,
    changedPermissions: List<AdminChangedPermission>,
    searchQuery: String = "",
    isHistoryLoading: Boolean = false,
    onSectionSelected: (AdminAccessSection) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onRoleClick: (AdminRole) -> Unit = {},
    onPermissionClick: (AdminPermission) -> Unit = {},
    onHistoryEntityClick: (AdminHistoryEntityType, Long) -> Unit = { _, _ -> }
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminSegmentedTabs(
            titles = availableSections.map { it.title },
            selectedIndex = availableSections.indexOf(selectedSection),
            onSelected = { index -> onSectionSelected(availableSections[index]) }
        )
        when (selectedSection) {
            AdminAccessSection.Roles -> RolesTabContent(
                roles = roles,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onRoleClick = onRoleClick
            )
            AdminAccessSection.Permissions -> PermissionsTabContent(
                permissions = permissions,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onPermissionClick = onPermissionClick
            )
            AdminAccessSection.History -> HistoryTabContent(
                changedUsers = changedUsers,
                changedRoles = changedRoles,
                changedPermissions = changedPermissions,
                searchQuery = searchQuery,
                isLoading = isHistoryLoading,
                onSearchQueryChange = onSearchQueryChange,
                onHistoryEntityClick = onHistoryEntityClick
            )
        }
    }
}
