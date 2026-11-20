package com.example.ysukampus.ui.admin.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.admin.AdminActionKind
import com.example.ysukampus.ui.admin.AdminSelectOption
import com.example.ysukampus.ui.admin.AdminStatus
import com.example.ysukampus.ui.admin.components.AdminActionButton
import com.example.ysukampus.ui.admin.components.AdminChartLegend
import com.example.ysukampus.ui.admin.components.AdminChartPoint
import com.example.ysukampus.ui.admin.components.AdminChartSeries
import com.example.ysukampus.ui.admin.components.AdminChartSeriesColors
import com.example.ysukampus.ui.admin.components.AdminBarChart
import com.example.ysukampus.ui.admin.components.AdminEmptyStateCard
import com.example.ysukampus.ui.admin.components.AdminEntityCard
import com.example.ysukampus.ui.admin.components.AdminLineChart
import com.example.ysukampus.ui.admin.components.AdminLoadingStateCard
import com.example.ysukampus.ui.admin.components.AdminSectionLabel
import com.example.ysukampus.ui.admin.components.AdminSegmentedTabs
import com.example.ysukampus.ui.admin.components.AdminSelect
import com.example.ysukampus.ui.admin.components.AdminSheetBlock
import com.example.ysukampus.ui.admin.state.ADMIN_CHART_MAX_METRICS
import com.example.ysukampus.ui.admin.state.ADMIN_REPORT_METRIC_GROUPS
import com.example.ysukampus.ui.admin.state.AdminReportJobStatus
import com.example.ysukampus.ui.admin.state.AdminReportListItem
import com.example.ysukampus.ui.admin.state.AdminReportMetric
import com.example.ysukampus.ui.admin.state.AdminReportPeriod
import com.example.ysukampus.ui.admin.state.AdminReportSeries
import com.example.ysukampus.ui.admin.state.AdminReportSeriesPoint
import com.example.ysukampus.ui.admin.state.AdminStatsView
import com.example.ysukampus.ui.theme.YsuAdminColors
import com.example.ysukampus.ui.theme.YsuAdminTextStyles
import java.time.LocalDate

/**
 * «Статистика» — порт вкладки `StatsTab` вебки: три вида («Сегодня», «Отчёты»,
 * «График»), список сохранённых дневных отчётов и постановка новой генерации
 * в очередь под правом `generate-daily-report`.
 */
@Composable
fun StatsTabContent(
    selectedView: AdminStatsView,
    todayMetrics: Map<AdminReportMetric, Int>?,
    reports: List<AdminReportListItem>,
    chartMetrics: List<AdminReportMetric>,
    chartPeriod: AdminReportPeriod,
    chartSeries: List<AdminReportSeries>,
    isReportsLoading: Boolean = false,
    isTodayLoading: Boolean = false,
    isChartLoading: Boolean = false,
    canGenerate: Boolean = false,
    isGeneratePanelOpen: Boolean = false,
    generateDate: String = "",
    generateNeedsForce: Boolean = false,
    isGenerating: Boolean = false,
    onViewSelected: (AdminStatsView) -> Unit = {},
    onReportClick: (String) -> Unit = {},
    onMetricToggle: (AdminReportMetric) -> Unit = {},
    onPeriodSelected: (AdminReportPeriod) -> Unit = {},
    onGeneratePanelOpen: () -> Unit = {},
    onGeneratePanelClose: () -> Unit = {},
    onGenerateDateChange: (String) -> Unit = {},
    onGenerateSubmit: (Boolean) -> Unit = {}
) {
    val views = AdminStatsView.entries
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminSegmentedTabs(
            titles = views.map { it.title },
            selectedIndex = views.indexOf(selectedView),
            onSelected = { index -> onViewSelected(views[index]) }
        )
        when (selectedView) {
            AdminStatsView.Today -> TodayMetricsPanel(
                metrics = todayMetrics,
                isLoading = isTodayLoading
            )
            AdminStatsView.Reports -> ReportsPanel(
                reports = reports,
                isLoading = isReportsLoading,
                canGenerate = canGenerate,
                isGeneratePanelOpen = isGeneratePanelOpen,
                generateDate = generateDate,
                generateNeedsForce = generateNeedsForce,
                isGenerating = isGenerating,
                onReportClick = onReportClick,
                onGeneratePanelOpen = onGeneratePanelOpen,
                onGeneratePanelClose = onGeneratePanelClose,
                onGenerateDateChange = onGenerateDateChange,
                onGenerateSubmit = onGenerateSubmit
            )
            AdminStatsView.Chart -> ChartPanel(
                chartMetrics = chartMetrics,
                chartPeriod = chartPeriod,
                chartSeries = chartSeries,
                isLoading = isChartLoading,
                onMetricToggle = onMetricToggle,
                onPeriodSelected = onPeriodSelected
            )
        }
    }
}

/** Живой слепок за сегодня: те же три группы метрик, что и в открытом отчёте. */
@Composable
private fun TodayMetricsPanel(metrics: Map<AdminReportMetric, Int>?, isLoading: Boolean) {
    when {
        metrics == null && isLoading -> AdminLoadingStateCard("Загрузка метрик...")
        metrics == null -> AdminEmptyStateCard(
            title = "Нет данных",
            subtitle = "Метрики за сегодня появятся после загрузки"
        )
        else -> AdminSheetBlock {
            ADMIN_REPORT_METRIC_GROUPS.forEachIndexed { index, group ->
                if (index > 0) Spacer(Modifier.height(4.dp))
                AdminSectionLabel(group.title)
                group.metrics.forEach { metric ->
                    MetricRow(label = metric.label, value = metrics[metric]?.toString() ?: "—")
                }
            }
        }
    }
}

@Composable
internal fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = YsuAdminTextStyles.CardSubtitle,
            color = YsuAdminColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(value, style = YsuAdminTextStyles.CardTitle, color = YsuAdminColors.Text)
    }
}

@Composable
private fun ReportsPanel(
    reports: List<AdminReportListItem>,
    isLoading: Boolean,
    canGenerate: Boolean,
    isGeneratePanelOpen: Boolean,
    generateDate: String,
    generateNeedsForce: Boolean,
    isGenerating: Boolean,
    onReportClick: (String) -> Unit,
    onGeneratePanelOpen: () -> Unit,
    onGeneratePanelClose: () -> Unit,
    onGenerateDateChange: (String) -> Unit,
    onGenerateSubmit: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            isLoading && reports.isEmpty() -> AdminLoadingStateCard("Загрузка отчётов...")
            reports.isEmpty() -> AdminEmptyStateCard(
                title = "Отчётов пока нет",
                subtitle = "Первый отчёт появится после генерации"
            )
            else -> reports.forEach { report ->
                AdminEntityCard(
                    iconText = "▤",
                    title = formatReportDate(report.date),
                    subtitle = reportSubtitle(report),
                    statusText = report.status?.label ?: if (report.hasFile) "Готов" else "—",
                    status = reportBadgeStatus(report),
                    onClick = { onReportClick(report.date) }
                )
            }
        }
        if (canGenerate) {
            if (isGeneratePanelOpen) {
                GeneratePanel(
                    date = generateDate,
                    needsForce = generateNeedsForce,
                    isGenerating = isGenerating,
                    onDateChange = onGenerateDateChange,
                    onClose = onGeneratePanelClose,
                    onSubmit = onGenerateSubmit
                )
            } else {
                AdminActionButton(
                    text = "Сгенерировать отчёт",
                    kind = AdminActionKind.Primary,
                    onClick = onGeneratePanelOpen,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Панель постановки в очередь. Даты выбираются шагом в день: календарь ради
 * «вчера» (значение по умолчанию) на телефоне только мешает, а вперёд бэк всё
 * равно не пустит.
 */
@Composable
private fun GeneratePanel(
    date: String,
    needsForce: Boolean,
    isGenerating: Boolean,
    onDateChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: (Boolean) -> Unit
) {
    AdminSheetBlock {
        AdminSectionLabel("Дата отчёта")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DateStepButton(text = "‹", enabled = !isGenerating) {
                onDateChange(date.shiftDays(-1))
            }
            Text(
                text = formatReportDate(date),
                style = YsuAdminTextStyles.CardTitle,
                color = YsuAdminColors.Text
            )
            DateStepButton(
                text = "›",
                // Сегодняшний день ещё не закончился, будущее бэк отклоняет.
                enabled = !isGenerating && date.shiftDays(1) <= LocalDate.now().toString()
            ) {
                onDateChange(date.shiftDays(1))
            }
        }
        if (needsForce) {
            Text(
                text = "Отчёт за ${formatReportDate(date)} уже есть. Пересобрать с перезаписью?",
                style = YsuAdminTextStyles.CardSubtitle,
                color = YsuAdminColors.TextSecondary
            )
        }
        AdminActionButton(
            text = when {
                isGenerating -> "Постановка..."
                needsForce -> "Пересобрать"
                else -> "Поставить в очередь"
            },
            kind = if (needsForce) AdminActionKind.SoftWarning else AdminActionKind.Primary,
            onClick = { onSubmit(needsForce) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGenerating
        )
        AdminActionButton(
            text = "Отмена",
            kind = AdminActionKind.Danger,
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGenerating
        )
    }
}

@Composable
private fun DateStepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) YsuAdminColors.PrimaryBright else YsuAdminColors.BackgroundSecondary)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = YsuAdminTextStyles.ScreenTitle,
            color = if (enabled) YsuAdminColors.PrimaryDark else YsuAdminColors.TextMuted
        )
    }
}

@Composable
private fun ChartPanel(
    chartMetrics: List<AdminReportMetric>,
    chartPeriod: AdminReportPeriod,
    chartSeries: List<AdminReportSeries>,
    isLoading: Boolean,
    onMetricToggle: (AdminReportMetric) -> Unit,
    onPeriodSelected: (AdminReportPeriod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricChips(selected = chartMetrics, onToggle = onMetricToggle)
        AdminSelect(
            value = AdminSelectOption(chartPeriod.name, chartPeriod.title),
            placeholder = "Период",
            options = AdminReportPeriod.entries.map { AdminSelectOption(it.name, it.title) },
            onValueChange = { option ->
                val period = AdminReportPeriod.entries.firstOrNull { it.name == option?.id }
                if (period != null) onPeriodSelected(period)
            }
        )
        val series = chartSeries.toChartSeries()
        when {
            isLoading -> AdminLoadingStateCard("Загрузка динамики...")
            series.none { line -> line.points.any { it.value != null } } -> AdminEmptyStateCard(
                title = "Нет данных за период",
                subtitle = "За выбранные дни отчётов ещё нет"
            )
            // Одна метрика — привычный «детальный» вид: линия плюс столбцы.
            series.size == 1 -> {
                AdminLineChart(series = series, normalized = false)
                AdminBarChart(series = series.first())
            }
            else -> {
                AdminChartLegend(series = series)
                AdminLineChart(series = series, normalized = true)
                Text(
                    text = "Линии нормированы — % от пика каждой метрики",
                    style = YsuAdminTextStyles.CardSubtitle,
                    color = YsuAdminColors.TextSecondary
                )
            }
        }
    }
}

/** Выбор метрик графика: до [ADMIN_CHART_MAX_METRICS] штук, как в вебке. */
@Composable
private fun MetricChips(
    selected: List<AdminReportMetric>,
    onToggle: (AdminReportMetric) -> Unit
) {
    val isFull = selected.size >= ADMIN_CHART_MAX_METRICS
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdminReportMetric.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { metric ->
                    val isSelected = metric in selected
                    MetricChip(
                        label = metric.label,
                        isSelected = isSelected,
                        // Пятая метрика не выбирается, но снять уже выбранную можно всегда.
                        enabled = isSelected || !isFull,
                        onClick = { onToggle(metric) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) YsuAdminColors.Primary else YsuAdminColors.Surface)
            .border(
                1.dp,
                if (isSelected) Color.Transparent else YsuAdminColors.Border,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = YsuAdminTextStyles.Tab,
            color = when {
                isSelected -> Color.White
                enabled -> YsuAdminColors.PrimaryDark
                else -> YsuAdminColors.TextMuted
            }
        )
    }
}

/**
 * Серии бэкенда → ряды графика. Дни без отчёта в ответе отсутствуют, поэтому
 * ряд разворачивается в непрерывный диапазон с разрывами (`null`) — иначе линия
 * соединит пропуски и соврёт.
 */
private fun List<AdminReportSeries>.toChartSeries(): List<AdminChartSeries> {
    val dates = flatMap { series -> series.points.map { it.date } }.distinct().sorted()
    val range = continuousDates(dates)
    return mapIndexed { index, series ->
        val valueByDate = series.points.associate { it.date to it.value }
        AdminChartSeries(
            label = series.metric.label,
            color = AdminChartSeriesColors[index % AdminChartSeriesColors.size],
            points = range.map { date -> AdminChartPoint(date, valueByDate[date]) }
        )
    }
}

private fun continuousDates(dates: List<String>): List<String> {
    val first = dates.firstOrNull()?.toLocalDateOrNull() ?: return emptyList()
    val last = dates.lastOrNull()?.toLocalDateOrNull() ?: return emptyList()
    val result = mutableListOf<String>()
    var current = first
    while (!current.isAfter(last)) {
        result += current.toString()
        current = current.plusDays(1)
    }
    return result
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.shiftDays(days: Long): String =
    toLocalDateOrNull()?.plusDays(days)?.toString() ?: this

private fun reportSubtitle(report: AdminReportListItem): String {
    val status = report.status?.label ?: if (report.hasFile) "Готов" else "—"
    val size = report.sizeBytes?.formatSize()
    return if (size != null) "$status · $size" else status
}

private fun Long.formatSize(): String =
    if (this < 1024) "$this Б" else "%.1f КБ".format(this / 1024.0)

private fun reportBadgeStatus(report: AdminReportListItem): AdminStatus = when {
    report.isActive -> AdminStatus.Primary
    !report.hasFile -> AdminStatus.HardDanger
    else -> AdminStatus.Active
}

/** «2026-06-15» → «15.06.2026». */
internal fun formatReportDate(value: String): String {
    val parts = value.split("-")
    if (parts.size != 3) return value
    return "${parts[2]}.${parts[1]}.${parts[0]}"
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F4FF, widthDp = 360)
@Composable
private fun StatsTabTodayPreview() {
    StatsTabContent(
        selectedView = AdminStatsView.Today,
        todayMetrics = AdminReportMetric.entries.associateWith { 42 },
        reports = emptyList(),
        chartMetrics = listOf(AdminReportMetric.UsersActive),
        chartPeriod = AdminReportPeriod.Month,
        chartSeries = emptyList()
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F4FF, widthDp = 360)
@Composable
private fun StatsTabReportsPreview() {
    StatsTabContent(
        selectedView = AdminStatsView.Reports,
        todayMetrics = null,
        reports = listOf(
            AdminReportListItem("2026-09-14", AdminReportJobStatus.Done, hasFile = true, sizeBytes = 2048),
            AdminReportListItem("2026-09-13", AdminReportJobStatus.Running, hasFile = false, sizeBytes = null),
            AdminReportListItem("2026-09-12", AdminReportJobStatus.Failed, hasFile = false, sizeBytes = null)
        ),
        chartMetrics = listOf(AdminReportMetric.UsersActive),
        chartPeriod = AdminReportPeriod.Month,
        chartSeries = emptyList(),
        canGenerate = true
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F4FF, widthDp = 360, heightDp = 900)
@Composable
private fun StatsTabChartPreview() {
    val dates = (1..14).map { day -> "2026-09-%02d".format(day) }
    StatsTabContent(
        selectedView = AdminStatsView.Chart,
        todayMetrics = null,
        reports = emptyList(),
        chartMetrics = listOf(AdminReportMetric.UsersActive, AdminReportMetric.PostsCreated),
        chartPeriod = AdminReportPeriod.Week,
        chartSeries = listOf(
            AdminReportSeries(
                metric = AdminReportMetric.UsersActive,
                points = dates.mapIndexed { index, date ->
                    AdminReportSeriesPoint(date, 40 + (index * 7) % 35)
                }
            ),
            AdminReportSeries(
                metric = AdminReportMetric.PostsCreated,
                points = dates.mapIndexed { index, date ->
                    AdminReportSeriesPoint(date, 4 + (index * 3) % 11)
                }
            )
        )
    )
}
