package com.example.ysukampus.ui.admin.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.admin.components.AdminLoadingStateCard
import com.example.ysukampus.ui.admin.components.AdminDialogFrame
import com.example.ysukampus.ui.admin.components.AdminSectionLabel
import com.example.ysukampus.ui.admin.components.AdminSheetBlock
import com.example.ysukampus.ui.admin.screens.MetricRow
import com.example.ysukampus.ui.admin.screens.formatReportDate
import com.example.ysukampus.ui.admin.state.ADMIN_REPORT_METRIC_GROUPS
import com.example.ysukampus.ui.admin.state.AdminDailyReport
import com.example.ysukampus.ui.admin.state.AdminReportMetric

/**
 * Сохранённый дневной отчёт: дата, время генерации и девять метрик по группам —
 * порт web-модалки `reportCard`. Кнопки «Скачать JSON» здесь нет: в приложении
 * те же цифры уже на экране, а файл некуда открывать.
 */
@Composable
fun ReportDetailsDialog(
    date: String,
    report: AdminDailyReport?,
    isLoading: Boolean,
    onClose: () -> Unit
) {
    AdminDialogFrame(title = "Отчёт", onClose = onClose) {
        if (report == null) {
            if (isLoading) {
                AdminLoadingStateCard("Загрузка отчёта...")
            }
            return@AdminDialogFrame
        }
        AdminSheetBlock {
            MetricRow("Дата отчёта", formatReportDate(report.date.ifBlank { date }))
            MetricRow("Сформирован", formatGeneratedAt(report.generatedAt))
        }
        Spacer(Modifier.height(12.dp))
        AdminSheetBlock {
            ADMIN_REPORT_METRIC_GROUPS.forEachIndexed { index, group ->
                if (index > 0) Spacer(Modifier.height(4.dp))
                AdminSectionLabel(group.title)
                group.metrics.forEach { metric ->
                    MetricRow(metric.label, report.metrics[metric]?.toString() ?: "—")
                }
            }
        }
    }
}

/**
 * `generated_at` приходит со смещением (`+05:00`), а не naive-UTC, как остальные
 * даты бэка, поэтому дописывать `Z` нельзя — берём дату и время как есть.
 */
private fun formatGeneratedAt(value: String): String {
    val match = Regex("""^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})""").find(value) ?: return value
    val (year, month, day, hour, minute) = match.destructured
    return "$day.$month.$year, $hour:$minute"
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F4FF)
@Composable
private fun ReportDetailsDialogPreview() {
    ReportDetailsDialog(
        date = "2026-09-14",
        report = AdminDailyReport(
            date = "2026-09-14",
            generatedAt = "2026-09-15T03:10:00+05:00",
            metrics = AdminReportMetric.entries.associateWith { 42 }
        ),
        isLoading = false,
        onClose = {}
    )
}
