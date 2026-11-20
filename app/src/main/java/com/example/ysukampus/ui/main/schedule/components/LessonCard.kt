package com.example.ysukampus.ui.main.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.schedule.AudienceScope
import com.example.ysukampus.ui.main.schedule.Lesson
import com.example.ysukampus.ui.main.schedule.LessonAudience
import com.example.ysukampus.ui.main.schedule.LessonAudienceMember
import com.example.ysukampus.ui.main.schedule.LessonKind
import com.example.ysukampus.ui.main.schedule.ScheduleColors
import com.example.ysukampus.ui.main.schedule.ScheduleEntity
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType
import com.example.ysukampus.ui.main.schedule.ScheduleSlot
import com.example.ysukampus.ui.main.schedule.color
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/** Сколько групп потока показываем до сворачивания в «и ещё N». */
private const val STREAM_LIMIT = 3

/**
 * Карточка одной пары.
 *
 * Внутри слота может быть несколько занятий сразу (подгруппы, разные потоки) —
 * тогда аудитория показывается у каждого занятия, а не общей строкой сверху.
 *
 * Преподаватель и группы, у которых есть `oid`, кликабельны и открывают чужое
 * расписание. Компонент при этом остаётся чисто презентационным: он принимает
 * данные и отдаёт наружу событие [onEntityClick], ничего не зная про сеть
 * и ViewModel. Благодаря этому он полностью покрывается @Preview.
 *
 * @param currentOid чья сущность открыта сейчас — её саму кликабельной не делаем.
 */
@Composable
fun LessonCard(
    slot: ScheduleSlot,
    selectedGroupName: String,
    currentOid: String? = null,
    onEntityClick: (ScheduleEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMulti = slot.lessons.size > 1
    val firstKind = slot.lessons.firstOrNull()?.kind
    // Плашка номера пары красится, только если все занятия слота одного типа —
    // иначе один цвет соврал бы про содержимое.
    val isSameKind = firstKind != null && slot.lessons.all { it.kind == firstKind }
    val badgeColor = if (isSameKind) firstKind.color() else ScheduleColors.Muted

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KampusColors.Surface)
            .border(1.dp, KampusColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = slot.number,
                        style = ScheduleTypography.SlotNumber,
                        color = KampusColors.Surface
                    )
                }
                Text(
                    text = slotTimeRange(slot.startTime, slot.endTime),
                    style = ScheduleTypography.SlotTime,
                    color = KampusColors.TextMain,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            if (!isMulti) {
                slot.lessons.firstOrNull()?.auditorium?.takeIf { it.isNotBlank() }?.let { auditorium ->
                    Text(
                        text = auditorium,
                        style = ScheduleTypography.Auditorium,
                        color = KampusColors.TextMain,
                        maxLines = 1
                    )
                }
            }
        }

        slot.lessons.forEachIndexed { index, lesson ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    thickness = 1.dp,
                    color = KampusColors.Border
                )
            }
            LessonBlock(
                lesson = lesson,
                showAuditorium = isMulti,
                selectedGroupName = selectedGroupName,
                currentOid = currentOid,
                onEntityClick = onEntityClick,
                modifier = Modifier.padding(top = if (index == 0) 8.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun LessonBlock(
    lesson: Lesson,
    showAuditorium: Boolean,
    selectedGroupName: String,
    currentOid: String?,
    onEntityClick: (ScheduleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = lesson.discipline,
                style = ScheduleTypography.Discipline,
                color = KampusColors.TextMain,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (showAuditorium && lesson.auditorium.isNotBlank()) {
                Text(
                    text = lesson.auditorium,
                    style = ScheduleTypography.AuditoriumInline,
                    color = KampusColors.TextMain,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Text(
            text = lesson.kindLabel,
            style = ScheduleTypography.LessonKind,
            color = lesson.kind.color(),
            modifier = Modifier.padding(top = 3.dp, bottom = 6.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (lesson.lecturer.isNotBlank()) {
                MetaRow(iconRes = R.drawable.ic_schedule_teacher) {
                    EntityText(
                        label = lesson.lecturer,
                        // Имя в справочнике у преподавателя совпадает с подписью в карточке.
                        entity = lesson.lecturerOid
                            ?.takeIf { it != currentOid }
                            ?.let {
                                ScheduleEntity(it, lesson.lecturer, ScheduleEntityType.Lecturer)
                            },
                        onClick = onEntityClick
                    )
                }
            }
            AudienceMetaRow(
                audience = lesson.audience,
                selectedGroupName = selectedGroupName,
                currentOid = currentOid,
                onEntityClick = onEntityClick
            )
        }
    }
}

@Composable
private fun MetaRow(iconRes: Int, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = ScheduleColors.TextAccent,
            modifier = Modifier.size(12.dp)
        )
        Box(modifier = Modifier.padding(start = 4.dp)) {
            content()
        }
    }
}

/**
 * Строка аудитории.
 *
 * Когда бэк прислал `members`, группы рисуются поштучно, чтобы каждую можно
 * было тапнуть. Пустой список — ответ старого формата (или из его кэша):
 * тогда показываем готовую строку `audience.label` как раньше.
 */
@Composable
private fun AudienceMetaRow(
    audience: LessonAudience,
    selectedGroupName: String,
    currentOid: String?,
    onEntityClick: (ScheduleEntity) -> Unit
) {
    if (audience.members.isEmpty()) {
        val label = fallbackAudienceLabel(audience, selectedGroupName) ?: return
        MetaRow(iconRes = R.drawable.ic_schedule_audience) {
            Text(
                text = label,
                style = ScheduleTypography.Meta,
                color = ScheduleColors.TextAccent
            )
        }
        return
    }

    // Длинный поток сворачиваем до первой группы: бэк ставит первой выбранную.
    val isCollapsed = audience.scope == AudienceScope.Stream &&
        audience.members.size > STREAM_LIMIT
    val visible = if (isCollapsed) audience.members.take(1) else audience.members

    MetaRow(iconRes = R.drawable.ic_schedule_audience) {
        AudienceMembers(
            members = visible,
            collapsedRestCount = if (isCollapsed) audience.members.size - 1 else 0,
            currentOid = currentOid,
            onEntityClick = onEntityClick
        )
    }
}

/**
 * Группы поштучно. `FlowRow`, а не `Text`, потому что каждая группа —
 * самостоятельная область тапа; на узкой карточке список переносится по строкам.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudienceMembers(
    members: List<LessonAudienceMember>,
    collapsedRestCount: Int,
    currentOid: String?,
    onEntityClick: (ScheduleEntity) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        members.forEachIndexed { index, member ->
            EntityText(
                // Запятая живёт внутри своего же элемента, чтобы не переноситься
                // в начало следующей строки.
                label = if (index < members.lastIndex) "${member.label}," else member.label,
                entity = member.oid
                    ?.takeIf { it != currentOid }
                    ?.let { ScheduleEntity(it, member.name, ScheduleEntityType.Group) },
                onClick = onEntityClick
            )
        }
        if (collapsedRestCount > 0) {
            Text(
                text = stringResource(R.string.schedule_stream_rest, collapsedRestCount),
                style = ScheduleTypography.Meta,
                color = ScheduleColors.TextAccent
            )
        }
    }
}

/**
 * «10:00 – 11:40»: тире блеклее самого времени и прижато к нему узкими
 * пробелами, чтобы диапазон читался одним блоком.
 */
private fun slotTimeRange(startTime: String, endTime: String): AnnotatedString =
    buildAnnotatedString {
        append(startTime)
        withStyle(SpanStyle(color = KampusColors.MutedBlue)) { append("\u2009-\u2009") }
        append(endTime)
    }

/**
 * Подпись сущности. С [entity] — кликабельна, без него — обычный текст,
 * не реагирующий на тап.
 *
 * Кликабельность помечаем только пунктиром снизу, тем же цветом, что и обычная
 * подпись: сплошное синее подчёркивание читалось как гиперссылка и спорило
 * с остальной карточкой (в вебке это `.link` с `text-decoration-style: dashed`).
 */
@Composable
private fun EntityText(
    label: String,
    entity: ScheduleEntity?,
    onClick: (ScheduleEntity) -> Unit
) {
    if (entity == null) {
        Text(
            text = label,
            style = ScheduleTypography.Meta,
            color = ScheduleColors.TextAccent
        )
        return
    }

    // Compose умеет только сплошное подчёркивание, поэтому пунктир рисуем сами
    // по разметке текста — по одной линии на строку, если подпись перенеслась.
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = label,
        style = ScheduleTypography.Meta,
        color = ScheduleColors.TextAccent,
        onTextLayout = { layout = it },
        modifier = Modifier
            .clickable { onClick(entity) }
            .dashedUnderline { layout }
    )
}

/** Пунктир под каждой строкой текста: приглушённый, чтобы не спорить с буквами. */
private fun Modifier.dashedUnderline(layout: () -> TextLayoutResult?): Modifier = drawBehind {
    val result = layout() ?: return@drawBehind
    val color = ScheduleColors.TextAccent.copy(alpha = 0.45f)
    val effect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5f.dp.toPx()), 0f)

    for (line in 0 until result.lineCount) {
        val y = result.getLineBottom(line) - 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(result.getLineLeft(line), y),
            end = Offset(result.getLineRight(line), y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = effect
        )
    }
}

/**
 * Фолбэк для ответов без `members`. Для потоковой пары список групп может быть
 * длинным — сворачиваем его до «своя группа и ещё N», чтобы карточка не разъезжалась.
 */
@Composable
private fun fallbackAudienceLabel(audience: LessonAudience, selectedGroupName: String): String? {
    val label = audience.label.trim()
    if (label.isEmpty()) return null
    if (audience.scope != AudienceScope.Stream) return label

    val parts = label.split(",", " ").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size <= STREAM_LIMIT) return label

    val name = selectedGroupName.trim().ifEmpty { parts.first() }
    return stringResource(R.string.schedule_stream_more, name, parts.size - 1)
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F7FF, widthDp = 360)
@Composable
private fun LessonCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LessonCard(
                slot = ScheduleSlot(
                    number = "2",
                    startTime = "10:10",
                    endTime = "11:40",
                    lessons = listOf(
                        Lesson(
                            discipline = "Разработка мобильных приложений",
                            kindLabel = "Лекция",
                            kind = LessonKind.Lecture,
                            lecturer = "Иванов И. И.",
                            lecturerOid = "l-1",
                            auditorium = "426",
                            audience = LessonAudience(
                                label = "ПИ41б, ИВТ41б",
                                scope = AudienceScope.Stream,
                                members = listOf(
                                    // Своя группа — без oid тапа не будет.
                                    LessonAudienceMember("ПИ41б", "ПИ41б", null),
                                    LessonAudienceMember("ИВТ41б", "ИВТ41б", "2")
                                )
                            )
                        )
                    )
                ),
                selectedGroupName = "ПИ41б",
                currentOid = "1"
            )
            LessonCard(
                slot = ScheduleSlot(
                    number = "3",
                    startTime = "12:10",
                    endTime = "13:40",
                    lessons = listOf(
                        Lesson(
                            discipline = "Базы данных",
                            kindLabel = "Лабораторная работа",
                            kind = LessonKind.Lab,
                            lecturer = "Петров П. П.",
                            auditorium = "312",
                            audience = LessonAudience("ПИ41б/1", AudienceScope.Subgroup)
                        ),
                        Lesson(
                            discipline = "Базы данных",
                            kindLabel = "Практика",
                            kind = LessonKind.Practice,
                            lecturer = "Сидоров С. С.",
                            auditorium = "314",
                            audience = LessonAudience("ПИ41б/2", AudienceScope.Subgroup)
                        )
                    )
                ),
                selectedGroupName = "ПИ41б"
            )
        }
    }
}
