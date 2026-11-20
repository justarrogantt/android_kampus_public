package com.example.ysukampus.ui.admin.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ysukampus.ui.theme.YsuAdminColors
import com.example.ysukampus.ui.theme.YsuAdminTextStyles
import kotlin.math.roundToInt

/** Точка графика: день ряда и значение метрики; `null` — за этот день отчёта нет. */
data class AdminChartPoint(val date: String, val value: Int?)

/** Одна линия графика со своей подписью и цветом. */
data class AdminChartSeries(
    val label: String,
    val color: Color,
    val points: List<AdminChartPoint>
)

/** Цвета линий мультиграфика — те же четыре, что у вебки. */
val AdminChartSeriesColors = listOf(
    YsuAdminColors.Primary,
    YsuAdminColors.Success,
    YsuAdminColors.Warning,
    YsuAdminColors.Danger
)

private val ChartHeight = 190.dp
private val AxisLabelStyle @Composable get() = TextStyle(fontSize = 9.sp, color = YsuAdminColors.TextSecondary)

/**
 * Линейный график с заливкой. При нескольких сериях значения нормируются в
 * проценты от пика каждой метрики ([normalized]) — иначе одна большая метрика
 * прижмёт остальные к нулю, как и объясняет подпись под графиком в вебке.
 *
 * Тач заменяет наведение мышью: тап или проводка по графику выбирает ближайший
 * день и показывает его значения над полотном.
 */
@Composable
fun AdminLineChart(
    series: List<AdminChartSeries>,
    normalized: Boolean,
    modifier: Modifier = Modifier
) {
    AdminChartFrame(series = series, normalized = normalized, modifier = modifier) { geometry ->
        series.forEach { line ->
            val seriesMax = if (normalized) line.maxValue() else geometry.maxValue
            val offsets = geometry.offsets(line, seriesMax)
            // Заливка под единственной линией: у мультиграфика она превратилась
            // бы в кашу из перекрывающихся полупрозрачных пятен.
            if (series.size == 1) {
                drawAreaUnder(offsets, geometry, line.color)
            }
            drawSegments(offsets) { from, to ->
                drawLine(
                    color = line.color,
                    start = from,
                    end = to,
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }
    }
}

/** Столбчатый график одной метрики — «детальный» вид вебки под линией. */
@Composable
fun AdminBarChart(
    series: AdminChartSeries,
    modifier: Modifier = Modifier
) {
    AdminChartFrame(series = listOf(series), normalized = false, modifier = modifier) { geometry ->
        val count = series.points.size.coerceAtLeast(1)
        val step = geometry.plotSize.width / count
        val barWidth = (step * 0.6f).coerceAtMost(26.dp.toPx())
        series.points.forEachIndexed { index, point ->
            val value = point.value ?: return@forEachIndexed
            val ratio = if (geometry.maxValue <= 0) 0f else value / geometry.maxValue.toFloat()
            val height = geometry.plotSize.height * ratio
            val centerX = geometry.plotLeft + step * (index + 0.5f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(YsuAdminColors.Primary, YsuAdminColors.PrimaryLight)
                ),
                topLeft = Offset(centerX - barWidth / 2f, geometry.plotTop + geometry.plotSize.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

/** Легенда мультиграфика: цвет линии и подпись метрики. */
@Composable
fun AdminChartLegend(series: List<AdminChartSeries>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        series.forEach { line ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(line.color)
                )
                Text(line.label, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.TextSecondary)
            }
        }
    }
}

/**
 * Общая рамка графиков: сетка, оси, выбранный день и его значения. Сам рисунок
 * серий отдаётся вызывающему — различаются только линии и столбцы.
 */
@Composable
private fun AdminChartFrame(
    series: List<AdminChartSeries>,
    normalized: Boolean,
    modifier: Modifier = Modifier,
    drawSeries: DrawScope.(geometry: ChartGeometry) -> Unit
) {
    val dates = series.firstOrNull()?.points?.map { it.date }.orEmpty()
    var selectedIndex by remember(dates.size) { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = AxisLabelStyle
    val maxValue = series.maxOfOrNull { line -> line.maxValue() } ?: 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(YsuAdminColors.Surface)
            .border(1.dp, YsuAdminColors.Border, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        selectedIndex?.let { index ->
            ChartTooltip(series = series, index = index, dates = dates)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .pointerInput(dates.size) {
                    detectTapGestures { offset ->
                        selectedIndex = dates.indexAt(offset.x, size.width.toFloat())
                    }
                }
                .pointerInput(dates.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = { },
                        onHorizontalDrag = { change, _ ->
                            selectedIndex = dates.indexAt(change.position.x, size.width.toFloat())
                        }
                    )
                }
        ) {
            val geometry = ChartGeometry(
                canvasSize = size,
                maxValue = if (normalized) 100 else maxValue,
                pointCount = dates.size,
                axisWidth = 30.dp.toPx(),
                axisHeight = 14.dp.toPx()
            )
            drawGrid(geometry, textMeasurer, axisStyle, normalized)
            drawSeries(geometry)
            selectedIndex?.let { index -> drawSelection(geometry, index) }
            drawAxisDates(geometry, dates, textMeasurer, axisStyle)
        }
    }
}

@Composable
private fun ChartTooltip(series: List<AdminChartSeries>, index: Int, dates: List<String>) {
    val date = dates.getOrNull(index) ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(YsuAdminColors.BackgroundSecondary)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(formatChartDate(date), style = YsuAdminTextStyles.CardTitle, color = YsuAdminColors.Text)
        series.forEach { line ->
            val value = line.points.getOrNull(index)?.value ?: return@forEach
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(line.color)
                )
                Text(
                    text = "${line.label}: $value",
                    style = YsuAdminTextStyles.CardSubtitle,
                    color = YsuAdminColors.Text
                )
            }
        }
    }
}

/** Координаты полотна: место под подписи осей и масштаб значений. */
private class ChartGeometry(
    canvasSize: Size,
    val maxValue: Int,
    val pointCount: Int,
    val axisWidth: Float,
    axisHeight: Float
) {
    val plotLeft = axisWidth
    val plotTop = 0f
    val plotSize = Size(
        width = (canvasSize.width - axisWidth).coerceAtLeast(1f),
        height = (canvasSize.height - axisHeight).coerceAtLeast(1f)
    )

    fun xAt(index: Int): Float {
        if (pointCount <= 1) return plotLeft + plotSize.width / 2f
        return plotLeft + plotSize.width * index / (pointCount - 1).toFloat()
    }

    fun yAt(value: Float): Float {
        val ratio = if (maxValue <= 0) 0f else value / maxValue.toFloat()
        return plotTop + plotSize.height * (1f - ratio.coerceIn(0f, 1f))
    }

    /** Точки линии в координатах полотна; дни без отчёта остаются разрывом. */
    fun offsets(series: AdminChartSeries, seriesMax: Int): List<Offset?> =
        series.points.mapIndexed { index, point ->
            val value = point.value ?: return@mapIndexed null
            // При нормировке шкала общая (0–100 %), поэтому значение переводится
            // в проценты от пика своей метрики.
            val scaled = if (seriesMax == maxValue) {
                value.toFloat()
            } else {
                if (seriesMax <= 0) 0f else value * 100f / seriesMax
            }
            Offset(xAt(index), yAt(scaled))
        }
}

private fun AdminChartSeries.maxValue(): Int = points.mapNotNull { it.value }.maxOrNull() ?: 0

private fun List<String>.indexAt(x: Float, width: Float): Int? {
    if (isEmpty()) return null
    if (size == 1) return 0
    val ratio = (x / width).coerceIn(0f, 1f)
    return (ratio * (size - 1)).roundToInt().coerceIn(0, size - 1)
}

/** Соединяет соседние точки, пропуская дни без данных (`connectNulls={false}`). */
private inline fun DrawScope.drawSegments(
    offsets: List<Offset?>,
    segment: (from: Offset, to: Offset) -> Unit
) {
    for (index in 0 until offsets.size - 1) {
        val from = offsets[index] ?: continue
        val to = offsets[index + 1] ?: continue
        segment(from, to)
    }
}

private fun DrawScope.drawAreaUnder(
    offsets: List<Offset?>,
    geometry: ChartGeometry,
    color: Color
) {
    val bottom = geometry.plotTop + geometry.plotSize.height
    var index = 0
    while (index < offsets.size) {
        if (offsets[index] == null) {
            index++
            continue
        }
        val chunk = mutableListOf<Offset>()
        while (index < offsets.size) {
            val offset = offsets[index] ?: break
            chunk += offset
            index++
        }
        if (chunk.size < 2) continue
        val path = Path().apply {
            moveTo(chunk.first().x, bottom)
            chunk.forEach { lineTo(it.x, it.y) }
            lineTo(chunk.last().x, bottom)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f))
            )
        )
    }
}

private fun DrawScope.drawGrid(
    geometry: ChartGeometry,
    textMeasurer: TextMeasurer,
    axisStyle: TextStyle,
    normalized: Boolean
) {
    val steps = 2
    repeat(steps + 1) { step ->
        val ratio = step / steps.toFloat()
        val y = geometry.plotTop + geometry.plotSize.height * (1f - ratio)
        drawLine(
            color = YsuAdminColors.Border,
            start = Offset(geometry.plotLeft, y),
            end = Offset(geometry.plotLeft + geometry.plotSize.width, y),
            strokeWidth = 1f
        )
        val value = (geometry.maxValue * ratio).roundToInt()
        val label = if (normalized) "$value%" else value.toString()
        val measured = textMeasurer.measure(label, axisStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = (geometry.plotLeft - measured.size.width - 4f).coerceAtLeast(0f),
                y = y - measured.size.height / 2f
            )
        )
    }
}

/** Подписи оси X: первый, средний и последний день — больше не влезает. */
private fun DrawScope.drawAxisDates(
    geometry: ChartGeometry,
    dates: List<String>,
    textMeasurer: TextMeasurer,
    axisStyle: TextStyle
) {
    if (dates.isEmpty()) return
    val indexes = listOf(0, dates.size / 2, dates.size - 1).distinct()
    indexes.forEach { index ->
        val label = formatAxisDate(dates[index])
        val measured = textMeasurer.measure(label, axisStyle)
        val x = (geometry.xAt(index) - measured.size.width / 2f)
            .coerceIn(0f, size.width - measured.size.width)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(x, geometry.plotTop + geometry.plotSize.height + 2f)
        )
    }
}

private fun DrawScope.drawSelection(geometry: ChartGeometry, index: Int) {
    val x = geometry.xAt(index)
    drawLine(
        color = YsuAdminColors.BorderStrong,
        start = Offset(x, geometry.plotTop),
        end = Offset(x, geometry.plotTop + geometry.plotSize.height),
        strokeWidth = 1.5f
    )
}

/** «2026-06-15» → «15.06». */
private fun formatAxisDate(value: String): String {
    val parts = value.split("-")
    if (parts.size != 3) return value
    return "${parts[2]}.${parts[1]}"
}

/** «2026-06-15» → «15.06.2026». */
internal fun formatChartDate(value: String): String {
    val parts = value.split("-")
    if (parts.size != 3) return value
    return "${parts[2]}.${parts[1]}.${parts[0]}"
}