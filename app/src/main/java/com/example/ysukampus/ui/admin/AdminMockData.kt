package com.example.ysukampus.ui.admin

val MockRoles = listOf(
    AdminRole(1, "Admin", "admin"),
    AdminRole(2, "User", "user"),
    AdminRole(3, "Guest", "guest"),
    AdminRole(4, "Moderator", "moderator")
)

val MockUsers = listOf(
    AdminUser(1, "Егор", "demo_user", MockRoles.take(3))
)

val MockPermissions = listOf(
    AdminPermission(1, "get-list-user", "get-list-user"),
    AdminPermission(2, "read-user", "read-user"),
    AdminPermission(3, "create-user", "create-user"),
    AdminPermission(4, "update-user", "update-user"),
    AdminPermission(5, "delete-user", "delete-user"),
    AdminPermission(6, "restore-user", "restore-user"),
    AdminPermission(7, "get-list-role", "get-list-role"),
    AdminPermission(8, "read-role", "read-role"),
    AdminPermission(9, "create-role", "create-role"),
    AdminPermission(10, "create-permission", "create-permission"),
    AdminPermission(11, "delete-log", "delete-log")
)
