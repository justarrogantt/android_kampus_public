package com.example.ysukampus.ui.main.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.main.KampusMotion
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.main.kampusPressable
import com.example.ysukampus.ui.theme.HomeTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Мини-карточка дня недели: сокращение сверху, число по центру, точки-пары снизу.
 * Пустой день приглушён прозрачностью — как в вебке.
 */
@Composable
fun DayCard(
    name: String,
    dayOfMonth: Int,
    lessonsCount: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = DAY_CARD_HEIGHT)
            .alpha(if (lessonsCount == 0) 0.55f else 1f)
            .kampusPressable(onClick, pressedScale = KampusMotion.ButtonPressScale)
            .clip(shape)
            .background(if (isActive) HomeColors.Primary else HomeColors.Surface)
            .then(
                if (isActive) Modifier else Modifier.border(1.dp, HomeColors.Border, shape)
            )
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = name,
            style = HomeTypography.DayCardName,
            color = if (isActive) HomeColors.OnPrimary.copy(alpha = 0.7f) else HomeColors.TextMuted
        )
        Text(
            text = dayOfMonth.toString(),
            style = HomeTypography.DayCardNumber,
            color = if (isActive) HomeColors.OnPrimary else HomeColors.TextMain
        )
        // Точки-пары переносятся вручную: FlowRow — экспериментальный API, чья
        // сигнатура несовместима между версиями compose-foundation.
        Column(
            modifier = Modifier.padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dotColor = if (isActive) {
                HomeColors.OnPrimary.copy(alpha = 0.85f)
            } else {
                HomeColors.PrimaryLight.copy(alpha = 0.6f)
            }

            if (lessonsCount == 0) {
                Box(modifier = Modifier.height(5.dp))
            }

            (0 until lessonsCount).chunked(DOTS_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { _ ->
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

/** Сколько точек влезает в ширину карточки — дальше перенос на новую строку. */
private const val DOTS_PER_ROW = 6

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun DayCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            DayCard(name = "ПН", dayOfMonth = 5, lessonsCount = 3, isActive = true, onClick = {}, modifier = Modifier.weight(1f))
            DayCard(name = "ВТ", dayOfMonth = 6, lessonsCount = 2, isActive = false, onClick = {}, modifier = Modifier.weight(1f))
            DayCard(name = "СР", dayOfMonth = 7, lessonsCount = 0, isActive = false, onClick = {}, modifier = Modifier.weight(1f))
        }
    }
}
