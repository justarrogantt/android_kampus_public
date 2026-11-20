package com.example.ysukampus.ui.admin.state

import com.example.ysukampus.ui.admin.AdminTab

/** Разделы вкладки «Доступы» — в вебке это были отдельные верхние вкладки. */
enum class AdminAccessSection(val title: String) {
    Roles("Роли"),
    Permissions("Права"),
    History("История")
}

/** Права, без которых верхняя вкладка не показывается (список из вебки). */
private val ADMIN_TAB_PERMISSIONS: Map<AdminTab, List<String>> = mapOf(
    AdminTab.Moderation to listOf("moderate-forum"),
    AdminTab.Stats to listOf(PERMISSION_VIEW_DAILY_REPORT),
    AdminTab.Users to listOf("get-list-user"),
    AdminTab.Logging to listOf("get-list-log")
)

/** Права разделов «Доступов»; у истории нужны все три сразу, как в вебке. */
private val ADMIN_ACCESS_SECTION_PERMISSIONS: Map<AdminAccessSection, List<String>> = mapOf(
    AdminAccessSection.Roles to listOf("get-list-role"),
    AdminAccessSection.Permissions to listOf("get-list-permission"),
    AdminAccessSection.History to listOf("get-story-user", "get-story-role", "get-story-permission")
)

/**
 * `permissions == null` — сессия старая и прав в кэше нет: тогда ничего не
 * прячем и, как раньше, полагаемся на 403 от бэкенда.
 */
fun availableAccessSections(permissions: Set<String>?): List<AdminAccessSection> =
    AdminAccessSection.entries.filter { section ->
        permissions == null ||
            ADMIN_ACCESS_SECTION_PERMISSIONS.getValue(section).all { it in permissions }
    }

fun availableAdminTabs(permissions: Set<String>?): List<AdminTab> =
    AdminTab.entries.filter { tab ->
        when (tab) {
            AdminTab.Access -> availableAccessSections(permissions).isNotEmpty()
            else -> permissions == null ||
                ADMIN_TAB_PERMISSIONS[tab].orEmpty().all { it in permissions }
        }
    }
