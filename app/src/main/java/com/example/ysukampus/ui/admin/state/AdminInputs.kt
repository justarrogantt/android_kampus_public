package com.example.ysukampus.ui.admin.state

data class AdminRoleInput(
    val name: String,
    val slug: String,
    val description: String
)

data class AdminPermissionInput(
    val name: String,
    val slug: String,
    val description: String
)
