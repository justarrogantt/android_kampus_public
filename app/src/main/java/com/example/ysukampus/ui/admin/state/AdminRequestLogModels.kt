package com.example.ysukampus.ui.admin.state

data class AdminRequestLogListItem(
    val id: Int,
    val fullUrl: String,
    val method: String?,
    val controllerPath: String?,
    val controllerMethod: String?,
    val responseStatus: Int,
    val calledAt: String
)

data class AdminRequestLogDetails(
    val id: Int,
    val fullUrl: String,
    val method: String,
    val controllerPath: String?,
    val controllerMethod: String?,
    val requestBody: String,
    val requestHeaders: String,
    val userId: Long?,
    val ipAddress: String?,
    val userAgent: String?,
    val responseStatus: Int,
    val responseBody: String,
    val responseHeaders: String,
    val calledAt: String
)

data class AdminRequestLogsPage(
    val items: List<AdminRequestLogListItem>,
    val page: Int,
    val totalPages: Int,
    val totalCount: Int,
    val perPage: Int
)

data class AdminRequestLogsFilter(
    val search: String = "",
    val userId: String = "",
    val responseStatus: String = "",
    val ipAddress: String = "",
    val controllerPath: String = "",
    val method: String? = null,
    val sortBy: String = "called_at",
    val sortOrder: String = "desc",
    val statusGroup: String? = null,
    val perPage: Int = 10
)
