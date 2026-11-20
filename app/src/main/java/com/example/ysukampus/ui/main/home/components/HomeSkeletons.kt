package com.example.ysukampus.ui.main.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Скелетоны главной. Повторяют форму реальных блоков, чтобы после загрузки
 * ничего не прыгало — тот же приём, что и в вебке.
 */

/**
 * Прямоугольник-заглушка с бликом, пробегающим слева направо.
 *
 * Раньше блок просто пульсировал целиком: все скелетоны экрана мигали в такт и
 * читались как ошибка отрисовки. Бегущий блик — тот же приём, что и CSS-шиммер
 * в вебке: он задаёт направление и выглядит как загрузка, а не как сбой.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Линейно и с паузой в конце цикла: блик должен идти ровно, но не
            // мельтешить без остановки.
            animation = tween(durationMillis = SHIMMER_CYCLE_MS, easing = LinearEasing)
        ),
        label = "skeleton-shimmer"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(HomeColors.SkeletonBase)
            .drawWithCache {
                // Блик шире самого блока: у узких плашек (точка, строка в
                // 16.dp) иначе не остаётся места для градиента.
                val sweep = size.width.coerceAtLeast(size.height) * SHIMMER_SWEEP_FRACTION
                val travel = size.width + sweep * 2f
                onDrawBehind {
                    val center = -sweep + travel * progress
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                HomeColors.SkeletonHighlight.copy(alpha = 0f),
                                HomeColors.SkeletonHighlight,
                                HomeColors.SkeletonHighlight.copy(alpha = 0f)
                            ),
                            startX = center - sweep,
                            endX = center + sweep
                        )
                    )
                }
            }
    )
}

private const val SHIMMER_CYCLE_MS = 1300
private const val SHIMMER_SWEEP_FRACTION = 0.45f

/** Строка погоды: иконка и текст. */
@Composable
fun WeatherSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SkeletonBlock(modifier = Modifier.size(16.dp), shape = CircleShape)
        SkeletonBlock(modifier = Modifier.width(120.dp).height(16.dp))
    }
}

/** Полоска дней недели: шесть мини-карточек. */
@Composable
fun WeekStripSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(WEEK_DAYS) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(DAY_CARD_HEIGHT)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HomeColors.SkeletonBase)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SkeletonBlock(modifier = Modifier.width(18.dp).height(7.dp))
                SkeletonBlock(modifier = Modifier.width(16.dp).height(14.dp))
            }
        }
    }
}

/** Карточка ближайшей пары. */
@Composable
fun NextLessonSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(NEXT_CARD_HEIGHT)
            .clip(RoundedCornerShape(22.dp))
            .background(HomeColors.PrimarySoft)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBlock(modifier = Modifier.width(96.dp).height(22.dp), shape = RoundedCornerShape(20.dp))
            SkeletonBlock(modifier = Modifier.width(56.dp).height(22.dp), shape = RoundedCornerShape(12.dp))
        }
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.75f).height(16.dp))
        SkeletonBlock(modifier = Modifier.width(120.dp).height(14.dp))
    }
}

/** Лента постов: по умолчанию две карточки, форум просит больше — на всю высоту. */
@Composable
fun BuzzListSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 2
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(HomeColors.Surface)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkeletonBlock(modifier = Modifier.size(36.dp), shape = CircleShape)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBlock(modifier = Modifier.width(140.dp).height(12.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBlock(modifier = Modifier.width(56.dp).height(28.dp), shape = RoundedCornerShape(999.dp))
                        SkeletonBlock(modifier = Modifier.width(56.dp).height(28.dp), shape = RoundedCornerShape(999.dp))
                    }
                }
            }
        }
    }
}

/** Столько дней в полоске: учебная неделя Пн–Сб, как её отдаёт бэк. */
private const val WEEK_DAYS = 6
internal val DAY_CARD_HEIGHT: Dp = 56.dp
internal val NEXT_CARD_HEIGHT: Dp = 124.dp

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HomeSkeletonsPreview() {
    YsukampusTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            WeatherSkeleton()
            WeekStripSkeleton()
            NextLessonSkeleton()
            BuzzListSkeleton()
        }
    }
}
