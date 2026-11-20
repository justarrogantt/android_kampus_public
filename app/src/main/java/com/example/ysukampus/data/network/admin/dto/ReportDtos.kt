package com.example.ysukampus.data.network.admin.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Девять метрик дневного отчёта — те же поля, что лежат в файле на бэке. */
@Serializable
data class DailyReportMetricsDto(
    @SerialName("total_users")
    val totalUsers: Int = 0,
    @SerialName("new_users")
    val newUsers: Int = 0,
    @SerialName("users_active")
    val usersActive: Int = 0,
    @SerialName("schedule_views")
    val scheduleViews: Int = 0,
    @SerialName("schedule_api_errors")
    val scheduleApiErrors: Int = 0,
    @SerialName("posts_created")
    val postsCreated: Int = 0,
    @SerialName("comments_created")
    val commentsCreated: Int = 0,
    @SerialName("likes_created")
    val likesCreated: Int = 0,
    @SerialName("forum_active_users")
    val forumActiveUsers: Int = 0
)

@Serializable
data class DailyReportDto(
    val date: String,
    /** Со смещением (`+05:00`), в отличие от naive-UTC остальных дат бэка. */
    @SerialName("generated_at")
    val generatedAt: String,
    val metrics: DailyReportMetricsDto
)

@Serializable
data class ReportListItemDto(
    val date: String,
    /** `queued` | `running` | `done` | `failed`; `null` — задачи в БД уже нет. */
    val status: String? = null,
    @SerialName("has_file")
    val hasFile: Boolean = false,
    @SerialName("size_bytes")
    val sizeBytes: Long? = null
)

@Serializable
data class ReportListDto(
    val items: List<ReportListItemDto> = emptyList()
)

@Serializable
data class ReportJobDto(
    val id: Int,
    @SerialName("report_date")
    val reportDate: String,
    val status: String,
    val error: String? = null,
    @SerialName("finished_at")
    val finishedAt: String? = null
)

@Serializable
data class ReportSeriesPointDto(
    val date: String,
    val value: Int
)

@Serializable
data class ReportSeriesDto(
    val metric: String,
    val points: List<ReportSeriesPointDto> = emptyList()
)
