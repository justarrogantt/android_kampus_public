package com.example.ysukampus.data.network.admin

import com.example.ysukampus.data.network.admin.dto.DailyReportDto
import com.example.ysukampus.data.network.admin.dto.DailyReportMetricsDto
import com.example.ysukampus.data.network.admin.dto.ReportListItemDto
import com.example.ysukampus.data.network.admin.dto.ReportSeriesDto
import com.example.ysukampus.ui.admin.state.AdminDailyReport
import com.example.ysukampus.ui.admin.state.AdminReportJobStatus
import com.example.ysukampus.ui.admin.state.AdminReportListItem
import com.example.ysukampus.ui.admin.state.AdminReportMetric
import com.example.ysukampus.ui.admin.state.AdminReportSeries
import com.example.ysukampus.ui.admin.state.AdminReportSeriesPoint

/** Метрики отчёта в виде карты — UI выводит их по порядку групп, а не по полям DTO. */
fun DailyReportMetricsDto.toAdminReportMetrics(): Map<AdminReportMetric, Int> = mapOf(
    AdminReportMetric.TotalUsers to totalUsers,
    AdminReportMetric.NewUsers to newUsers,
    AdminReportMetric.UsersActive to usersActive,
    AdminReportMetric.ScheduleViews to scheduleViews,
    AdminReportMetric.ScheduleApiErrors to scheduleApiErrors,
    AdminReportMetric.PostsCreated to postsCreated,
    AdminReportMetric.CommentsCreated to commentsCreated,
    AdminReportMetric.LikesCreated to likesCreated,
    AdminReportMetric.ForumActiveUsers to forumActiveUsers
)

fun DailyReportDto.toAdminDailyReport(): AdminDailyReport = AdminDailyReport(
    date = date,
    generatedAt = generatedAt,
    metrics = metrics.toAdminReportMetrics()
)

fun ReportListItemDto.toAdminReportListItem(): AdminReportListItem = AdminReportListItem(
    date = date,
    status = AdminReportJobStatus.fromApi(status),
    hasFile = hasFile,
    sizeBytes = sizeBytes
)

/** Серия неизвестной метрики до UI не доходит — рисовать её всё равно нечем. */
fun ReportSeriesDto.toAdminReportSeries(): AdminReportSeries? {
    val parsedMetric = AdminReportMetric.fromKey(metric) ?: return null
    return AdminReportSeries(
        metric = parsedMetric,
        points = points.map { AdminReportSeriesPoint(date = it.date, value = it.value) }
    )
}
