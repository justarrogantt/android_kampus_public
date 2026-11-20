package com.example.ysukampus.ui.main.schedule.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.BlurShellWhileVisible
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.KampusSheet
import com.example.ysukampus.ui.main.schedule.ScheduleEntity
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/** Диаметр кружка режима в переключателе внутри поля поиска. */
private val MODE_THUMB_SIZE = 26.dp

/**
 * Выбор группы или преподавателя.
 *
 * В вебке это самописный оверлей; здесь — [ModalBottomSheet] из Material 3.
 * Нативный компонент бесплатно даёт свайп вниз, системную кнопку «назад»
 * и правильное поведение при появлении клавиатуры.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPickerSheet(
    members: List<ScheduleEntity>,
    mode: ScheduleEntityType,
    query: String,
    isLoading: Boolean,
    selectedOid: String?,
    onQueryChange: (String) -> Unit,
    onModeChange: (ScheduleEntityType) -> Unit,
    onSelect: (ScheduleEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BlurShellWhileVisible(sheetState)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KampusColors.Surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(KampusSheet.HeightFraction)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(
                    if (mode == ScheduleEntityType.Group) {
                        R.string.schedule_picker_group_title
                    } else {
                        R.string.schedule_picker_lecturer_title
                    }
                ),
                style = ScheduleTypography.PickerTitle,
                color = KampusColors.TextMain,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            GroupPickerContent(
                members = members,
                mode = mode,
                query = query,
                isLoading = isLoading,
                selectedOid = selectedOid,
                onQueryChange = onQueryChange,
                onModeChange = onModeChange,
                onSelect = onSelect
            )
        }
    }
}

/**
 * Начинка пикера без обёртки-шторки: поиск, переключатель режима и список.
 *
 * Вынесена отдельно, потому что редактирование профиля показывает тот же
 * список внутри своей шторки — вложенный [ModalBottomSheet] там был бы лишним.
 */
@Composable
fun GroupPickerContent(
    members: List<ScheduleEntity>,
    mode: ScheduleEntityType,
    query: String,
    isLoading: Boolean,
    selectedOid: String?,
    onQueryChange: (String) -> Unit,
    onModeChange: (ScheduleEntityType) -> Unit,
    onSelect: (ScheduleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SearchField(
            query = query,
            mode = mode,
            onQueryChange = onQueryChange,
            onModeChange = onModeChange
        )

        Spacer(modifier = Modifier.height(6.dp))

        when {
            isLoading -> PickerSkeleton()

            members.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedule_picker_empty),
                    style = ScheduleTypography.Empty,
                    color = KampusColors.TextMain
                )
                Text(
                    text = stringResource(
                        if (mode == ScheduleEntityType.Group) {
                            R.string.schedule_picker_empty_try_lecturers
                        } else {
                            R.string.schedule_picker_empty_try_groups
                        }
                    ),
                    style = ScheduleTypography.Empty,
                    color = KampusColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // key по oid: при фильтрации Compose переиспользует строки,
                // а не пересоздаёт весь список заново.
                items(members, key = { it.oid }) { member ->
                    MemberRow(
                        member = member,
                        isSelected = member.oid == selectedOid,
                        onClick = { onSelect(member) }
                    )
                }
            }
        }
    }
}

/**
 * Поле поиска с переключателем режима внутри.
 *
 * `BasicTextField`, а не `OutlinedTextField`: у материального поля свои
 * отступы и место под лейбл, из-за которых круглый переключатель справа
 * не вставал вплотную к краю, как в вебке.
 */
@Composable
private fun SearchField(
    query: String,
    mode: ScheduleEntityType,
    onQueryChange: (String) -> Unit,
    onModeChange: (ScheduleEntityType) -> Unit
) {
    val selectionColors = TextSelectionColors(
        handleColor = KampusColors.PrimaryBlue,
        backgroundColor = KampusColors.PrimaryBlue.copy(alpha = 0.2f)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(KampusColors.Surface)
            .border(1.dp, KampusColors.Border, RoundedCornerShape(999.dp))
            .padding(start = 14.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = KampusColors.TextMuted,
            modifier = Modifier.size(18.dp)
        )

        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = ScheduleTypography.PickerItem.copy(color = KampusColors.TextMain),
                cursorBrush = SolidColor(KampusColors.PrimaryBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(
                                if (mode == ScheduleEntityType.Group) {
                                    R.string.schedule_picker_group_hint
                                } else {
                                    R.string.schedule_picker_lecturer_hint
                                }
                            ),
                            style = ScheduleTypography.PickerItem,
                            color = KampusColors.TextMuted
                        )
                    }
                    innerTextField()
                }
            )
        }

        ModeToggle(mode = mode, onModeChange = onModeChange)
    }
}

/**
 * Переключатель «группы / преподаватели»: две иконки в ряд и синий кружок,
 * переезжающий под активную — тот же жест, что и в вебке (`.modeThumb`).
 */
@Composable
private fun ModeToggle(
    mode: ScheduleEntityType,
    onModeChange: (ScheduleEntityType) -> Unit
) {
    val isGroup = mode == ScheduleEntityType.Group
    val nextMode = if (isGroup) ScheduleEntityType.Lecturer else ScheduleEntityType.Group
    val thumbOffset by animateDpAsState(
        targetValue = if (isGroup) 0.dp else MODE_THUMB_SIZE,
        label = "picker-mode-thumb"
    )

    Box(
        modifier = Modifier.clickable(onClick = { onModeChange(nextMode) }),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(MODE_THUMB_SIZE)
                .clip(CircleShape)
                .background(KampusColors.PrimaryBlue)
        )
        Row {
            ModeIcon(
                iconRes = R.drawable.ic_entity_student,
                descriptionRes = R.string.schedule_picker_show_groups,
                isActive = isGroup
            )
            ModeIcon(
                iconRes = R.drawable.ic_entity_teacher,
                descriptionRes = R.string.schedule_picker_show_lecturers,
                isActive = !isGroup
            )
        }
    }
}

@Composable
private fun ModeIcon(iconRes: Int, descriptionRes: Int, isActive: Boolean) {
    Box(
        modifier = Modifier.size(MODE_THUMB_SIZE),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descriptionRes),
            tint = if (isActive) KampusColors.Surface else KampusColors.TextMuted,
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * Строка справочника: имя и разделитель снизу — карточек тут нет,
 * список должен читаться как один сплошной перечень.
 */
@Composable
private fun MemberRow(
    member: ScheduleEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = member.name,
                style = ScheduleTypography.PickerItem,
                color = if (isSelected) KampusColors.PrimaryBlue else KampusColors.TextMain,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(KampusColors.PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = KampusColors.Surface,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = KampusColors.Border)
    }
}

/** Заглушка на время загрузки справочника — строки той же высоты, что и в списке. */
@Composable
private fun PickerSkeleton() {
    Column {
        repeat(10) {
            Column {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp, vertical = 16.dp)
                        .size(width = 92.dp, height = 14.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(KampusColors.FooterBackground)
                )
                HorizontalDivider(thickness = 1.dp, color = KampusColors.Border)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 700)
@Composable
private fun GroupPickerSheetPreview() {
    YsukampusTheme(dynamicColor = false) {
        GroupPickerSheet(
            members = listOf(
                ScheduleEntity("1", "ПИ41б", ScheduleEntityType.Group),
                ScheduleEntity("2", "ИВТ41б", ScheduleEntityType.Group),
                ScheduleEntity("3", "ПМИ31б", ScheduleEntityType.Group)
            ),
            mode = ScheduleEntityType.Group,
            query = "",
            isLoading = false,
            selectedOid = "1",
            onQueryChange = {},
            onModeChange = {},
            onSelect = {},
            onDismiss = {}
        )
    }
}

/** Список преподавателей — как он выглядит после переключения режима. */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 700)
@Composable
private fun GroupPickerSheetLecturersPreview() {
    YsukampusTheme(dynamicColor = false) {
        GroupPickerSheet(
            members = listOf(
                ScheduleEntity("1", "Абдалкина Валерия Романовна", ScheduleEntityType.Lecturer),
                ScheduleEntity("2", "Абдуллаев Рафаэль Азерович", ScheduleEntityType.Lecturer),
                ScheduleEntity("3", "Абзалилов Артур Ильясович", ScheduleEntityType.Lecturer)
            ),
            mode = ScheduleEntityType.Lecturer,
            query = "",
            isLoading = false,
            selectedOid = null,
            onQueryChange = {},
            onModeChange = {},
            onSelect = {},
            onDismiss = {}
        )
    }
}
