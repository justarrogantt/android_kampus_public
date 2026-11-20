package com.example.ysukampus.ui.main.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.main.home.HomeDates
import com.example.ysukampus.ui.main.kampusPressable
import com.example.ysukampus.ui.main.schedule.Lesson
import com.example.ysukampus.ui.main.schedule.LessonKind
import com.example.ysukampus.ui.main.schedule.ScheduleDates
import com.example.ysukampus.ui.main.schedule.ScheduleSlot
import com.example.ysukampus.ui.main.schedule.color
import com.example.ysukampus.ui.theme.HomeTypography
import com.example.ysukampus.ui.theme.YsukampusTheme
import kotlinx.coroutines.delay

/** Карточка ближайшей пары с обратным отсчётом. Клик открывает расписание. */
@Composable
fun NextLessonCard(
    slot: ScheduleSlot,
    lesson: Lesson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeUntil = rememberTimeUntil(slot.startTime)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = NEXT_CARD_HEIGHT)
            .kampusPressable(onClick)
            .clip(CardShape)
            .background(HomeColors.PrimarySoft)
            .border(1.5.dp, HomeColors.BorderStrong, CardShape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(HomeColors.PrimarySoft)
                    .border(1.dp, HomeColors.Border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_clock),
                    contentDescription = null,
                    tint = HomeColors.TextAccent,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = stringResource(R.string.home_next_in, timeUntil),
                    style = HomeTypography.NextChip,
                    color = HomeColors.TextAccent,
                    maxLines = 1
                )
            }

            Text(
                text = slot.startTime,
                style = HomeTypography.NextTime,
                color = HomeColors.TextAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(HomeColors.PrimarySoft)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Text(
            text = lesson.discipline,
            style = HomeTypography.NextSubject,
            color = HomeColors.TextMain,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp)
        )

        Text(
            text = lesson.kindLabel,
            style = HomeTypography.NextKind,
            color = lesson.kindColor(),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Одна строка вместо переноса: FlowRow несовместим между версиями
            // compose-foundation, а преподаватель и аудитория и так сокращаются.
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (lesson.lecturer.isNotBlank()) {
                    NextLessonMeta(
                        iconRes = R.drawable.ic_schedule_teacher,
                        text = ScheduleDates.formatLecturerShortName(lesson.lecturer)
                    )
                }
                if (lesson.auditorium.isNotBlank()) {
                    NextLessonMeta(
                        iconRes = R.drawable.ic_home_pin,
                        text = lesson.auditorium
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.ic_forum_open),
                contentDescription = null,
                tint = HomeColors.TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Пар на сегодня больше нет — дата и подпись по центру карточки того же размера. */
@Composable
fun NextLessonEmptyCard(
    hasLessonsToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = NEXT_CARD_HEIGHT)
            .kampusPressable(onClick)
            .clip(CardShape)
            // Пустой день не «горит» синим: карточка белая, как остальные.
            .background(colorResource(R.color.kampus_white))
            .border(1.5.dp, HomeColors.BorderStrong, CardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = HomeDates.formatGreetingDate() + "\n" + stringResource(
                if (hasLessonsToday) R.string.home_no_more_lessons else R.string.home_no_lessons
            ),
            style = HomeTypography.NextEmpty,
            color = HomeColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NextLessonMeta(iconRes: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = HomeColors.TextAccent,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = HomeTypography.NextMeta,
            color = HomeColors.TextAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * «Через 28 мин» — пересчитывается раз в секунду, пока карточка на экране.
 * Секунды показываем только на последней минуте, как в вебке.
 */
@Composable
private fun rememberTimeUntil(startTime: String): String {
    val hoursMinutes = stringResource(R.string.home_hours_minutes)
    val minutesOnly = stringResource(R.string.home_minutes)
    val secondsOnly = stringResource(R.string.home_seconds)

    val timeUntil by produceState(initialValue = "", startTime, hoursMinutes) {
        while (true) {
            val totalSeconds = HomeDates.secondsUntil(startTime)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            value = when {
                hours > 0 -> String.format(hoursMinutes, hours, minutes)
                minutes > 0 -> String.format(minutesOnly, minutes)
                else -> String.format(secondsOnly, seconds)
            }
            delay(1_000)
        }
    }

    return timeUntil
}

/** Цвет подписи типа занятия. Неизвестный тип — акцентный, как `ncTypeMuted` в вебке. */
private fun Lesson.kindColor() =
    if (kind == LessonKind.Other) HomeColors.TextAccent else kind.color()

private val CardShape = RoundedCornerShape(22.dp)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NextLessonCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NextLessonCard(
                slot = ScheduleSlot(
                    number = "2",
                    startTime = "10:20",
                    endTime = "11:50",
                    lessons = emptyList()
                ),
                lesson = Lesson(
                    discipline = "Проектирование информационных систем",
                    kindLabel = "Лекция",
                    kind = LessonKind.Lecture,
                    lecturer = "Иванов Иван Иванович",
                    auditorium = "301",
                    audience = com.example.ysukampus.ui.main.schedule.LessonAudience(
                        label = "ПИ41б",
                        scope = com.example.ysukampus.ui.main.schedule.AudienceScope.Group
                    )
                ),
                onClick = {}
            )
            NextLessonEmptyCard(hasLessonsToday = true, onClick = {})
        }
    }
}
