package com.example.ysukampus.ui.main.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.lerp as lerpDp
import kotlin.math.absoluteValue
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.KampusMotion
import com.example.ysukampus.ui.main.MainTab
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Положение пейджера в долях вкладки, пока страницы листают пальцем, и
     * `null`, когда вкладку переключили тапом. Лямбда, а не значение: под
     * жестом оно меняется каждый кадр, и читать его должны только таблетки,
     * а не вся оболочка.
     */
    swipePosition: () -> Float? = { null }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(KampusColors.FooterBackground)
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MainTab.entries.forEachIndexed { index, tab ->
            MainBottomBarItem(
                tab = tab,
                selected = tab == selectedTab,
                // Доля «выбранности»: 1 — страница ровно на этой вкладке, 0 —
                // отъехала на соседнюю и дальше. На середине жеста обе
                // соседние таблетки взяты наполовину, и синий читается как
                // перелившийся из одной в другую.
                swipeSelection = {
                    swipePosition()?.let { position ->
                        (1f - (position - index).absoluteValue).coerceIn(0f, 1f)
                    }
                },
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MainBottomBarItem(
    tab: MainTab,
    selected: Boolean,
    swipeSelection: () -> Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Под жестом таблетка живёт не «выбрана / не выбрана», а долей: цвет и
    // ширина берутся прямо из положения пейджера.
    val swipeFraction = swipeSelection()
    val underSwipe = swipeFraction != null
    val selection = swipeFraction ?: if (selected) 1f else 0f
    // Спек подменяется, а не значение: под жестом анимация схлопнута в snap,
    // поэтому та же самая анимация всё время держит актуальное значение — и
    // когда палец отпущен, пружина продолжает с того места, где остановился
    // жест, без скачка.
    val backgroundColor by animateColorAsState(
        targetValue = KampusColors.PrimaryBlue.copy(alpha = selection),
        animationSpec = if (underSwipe) {
            snap()
        } else {
            tween(durationMillis = KampusMotion.ColorMs, easing = KampusMotion.Standard)
        },
        label = "mainBottomBarBackground"
    )
    // Пилюля раздаётся пружиной, а не линейным tween: лёгкий перелёт по ширине
    // и есть то самое ощущение живого переключателя.
    val itemWidth by animateDpAsState(
        targetValue = lerpDp(45.dp, 65.dp, selection),
        animationSpec = if (underSwipe) snap() else KampusMotion.lively(),
        label = "mainBottomBarWidth"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) KampusMotion.ButtonPressScale else 1f,
        animationSpec = KampusMotion.PressSpring,
        label = "mainBottomBarPress"
    )

    Box(
        modifier = modifier.height(45.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .width(itemWidth)
                .height(45.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(backgroundColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (underSwipe) {
                // Под жестом иконка не пересобирается: подмена «вглубь и
                // обратно» на середине свайпа мигала бы поперёк непрерывного
                // движения. Тон просто доходит до белого вместе с заливкой.
                Icon(
                    painter = painterResource(tab.iconRes()),
                    contentDescription = tab.contentDescription(),
                    tint = lerpColor(KampusColors.MutedBlue, Color.White, selection),
                    modifier = Modifier.size(24.dp)
                )
                return@Box
            }
            // Смена состояния вкладки перерисовывает иконку: старая уходит
            // вглубь, новая выходит из глубины — переход читается как одно
            // движение, а не как мигание.
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (scaleIn(KampusMotion.enter(220), initialScale = 0.7f) +
                        fadeIn(KampusMotion.enter(220)))
                        .togetherWith(
                            scaleOut(KampusMotion.exit(120), targetScale = 0.7f) +
                                fadeOut(KampusMotion.exit(120))
                        )
                },
                label = "mainBottomBarIconContent"
            ) { isSelected ->
                Icon(
                    painter = painterResource(tab.iconRes()),
                    contentDescription = tab.contentDescription(),
                    tint = if (isSelected) Color.White else KampusColors.MutedBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@DrawableRes
private fun MainTab.iconRes(): Int =
    when (this) {
        MainTab.Home -> R.drawable.ic_main_home
        MainTab.Schedule -> R.drawable.ic_main_calendar
        MainTab.Forum -> R.drawable.ic_main_chat
        MainTab.Profile -> R.drawable.ic_main_profile
    }

private fun MainTab.contentDescription(): String =
    when (this) {
        MainTab.Home -> "Открыть главную"
        MainTab.Schedule -> "Открыть расписание"
        MainTab.Forum -> "Открыть «Между парами»"
        MainTab.Profile -> "Открыть профиль"
    }

/** Середина свайпа: синий взят наполовину обеими соседними таблетками. */
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MainBottomBarSwipePreview() {
    YsukampusTheme(dynamicColor = false) {
        MainBottomBar(
            selectedTab = MainTab.Home,
            onTabSelected = {},
            swipePosition = { 0.5f }
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MainBottomBarPreview() {
    YsukampusTheme(dynamicColor = false) {
        MainBottomBar(
            selectedTab = MainTab.Profile,
            onTabSelected = {}
        )
    }
}
