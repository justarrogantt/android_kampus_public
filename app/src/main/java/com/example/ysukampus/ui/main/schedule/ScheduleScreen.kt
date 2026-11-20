package com.example.ysukampus.ui.main.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.schedule.components.EntityConfirmSheet
import com.example.ysukampus.ui.main.schedule.components.GroupPickerSheet
import com.example.ysukampus.ui.main.schedule.components.LessonCard
import com.example.ysukampus.ui.main.notifications.components.NotificationsBell
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme
import kotlinx.coroutines.flow.distinctUntilChanged

/** Дней в учебной неделе (Пн–Сб) — по ним считаем стык догруженной недели. */
private const val DAYS_IN_WEEK = 6

/** Радиус пилюль шапки и переключателя режимов (`border-radius: 999px` в вебке). */
private val PILL_RADIUS = 999.dp

/**
 * Точка входа вкладки: создаёт ViewModel и подписывается на её состояние.
 *
 * `collectAsStateWithLifecycle` вместо `collectAsState` — подписка ставится на
 * паузу, когда экран уходит в фон, и не жжёт ресурсы в неактивном приложении.
 */
@Composable
fun ScheduleScreen(
    focusDate: String? = null,
    onFocusDateHandled: () -> Unit = {},
    /** Непрочитанные уведомления — бейдж на колокольчике в шапке. */
    unreadNotificationsCount: Int = 0,
    /** Размер очереди модерации: бейдж без числа, как в вебке. */
    moderationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Клик по дню на главной: переключаем вкладку на неделю и скроллим к дате.
    LaunchedEffect(focusDate) {
        val date = focusDate ?: return@LaunchedEffect
        viewModel.onFocusDate(date)
        onFocusDateHandled()
    }

    ScheduleContent(
        uiState = uiState,
        onViewChange = viewModel::onViewChange,
        onOpenPicker = viewModel::onOpenPicker,
        onDismissPicker = viewModel::onDismissPicker,
        onPickerModeChange = viewModel::onPickerModeChange,
        onPickerQueryChange = viewModel::onPickerQueryChange,
        onEntitySelected = viewModel::onEntitySelected,
        onLessonEntityClick = viewModel::onLessonEntityClick,
        onPendingEntityConfirmed = viewModel::onPendingEntityConfirmed,
        onPendingEntityDismissed = viewModel::onPendingEntityDismissed,
        onReturnToOwnEntity = viewModel::onReturnToOwnEntity,
        onLoadNextWeek = viewModel::onLoadNextWeek,
        onRetry = viewModel::onRetry,
        onMessageShown = viewModel::onMessageShown,
        onFocusDateHandled = viewModel::onFocusDateHandled,
        unreadNotificationsCount = unreadNotificationsCount,
        moderationCount = moderationCount,
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )
}

/**
 * Вся отрисовка вынесена в composable без ViewModel — так экран целиком
 * покрывается @Preview и не требует сети.
 */
@Composable
fun ScheduleContent(
    uiState: ScheduleUiState,
    onViewChange: (ScheduleView) -> Unit,
    onOpenPicker: () -> Unit,
    onDismissPicker: () -> Unit,
    onPickerModeChange: (ScheduleEntityType) -> Unit,
    onPickerQueryChange: (String) -> Unit,
    onEntitySelected: (ScheduleEntity) -> Unit,
    onLessonEntityClick: (ScheduleEntity) -> Unit,
    onPendingEntityConfirmed: () -> Unit,
    onPendingEntityDismissed: () -> Unit,
    onReturnToOwnEntity: () -> Unit,
    onLoadNextWeek: () -> Unit,
    onRetry: () -> Unit,
    onMessageShown: () -> Unit,
    onFocusDateHandled: () -> Unit = {},
    unreadNotificationsCount: Int = 0,
    moderationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Сообщение об ошибке показываем один раз и сразу гасим в состоянии,
    // иначе при следующей рекомпозиции снекбар всплыл бы повторно.
    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onMessageShown()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScheduleHeader(
                entityLabel = uiState.selectedEntityLabel,
                onOpenPicker = onOpenPicker,
                unreadNotificationsCount = unreadNotificationsCount,
                moderationCount = moderationCount,
                onOpenNotifications = onOpenNotifications
            )
            ScheduleTabs(
                selected = uiState.view,
                onSelect = onViewChange
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> ScheduleSkeleton()

                    uiState.hasError || !uiState.hasContent -> ScheduleError(onRetry = onRetry)

                    uiState.view == ScheduleView.Week -> WeekList(
                        week = requireNotNull(uiState.week),
                        selectedGroupName = uiState.selectedGroupName,
                        currentOid = uiState.selectedEntity?.oid,
                        isLoadingNextWeek = uiState.isLoadingNextWeek,
                        focusDate = uiState.focusDate,
                        onFocusDateHandled = onFocusDateHandled,
                        onEntityClick = onLessonEntityClick,
                        onLoadNextWeek = onLoadNextWeek
                    )

                    else -> DayList(
                        day = requireNotNull(uiState.activeDay).day,
                        selectedGroupName = uiState.selectedGroupName,
                        currentOid = uiState.selectedEntity?.oid,
                        onEntityClick = onLessonEntityClick
                    )
                }
            }
        }

        // Плашка возврата — как в вебе: снизу, у пальца и с подписью,
        // чтобы выход из чужого расписания не приходилось разгадывать.
        AnimatedVisibility(
            visible = uiState.isForeignEntity,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            ReturnToOwnButton(onClick = onReturnToOwnEntity)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    uiState.pendingEntity?.let { pending ->
        EntityConfirmSheet(
            entity = pending,
            onConfirm = onPendingEntityConfirmed,
            onDismiss = onPendingEntityDismissed
        )
    }

    if (uiState.isPickerVisible) {
        GroupPickerSheet(
            members = uiState.pickerMembers,
            mode = uiState.pickerMode,
            query = uiState.pickerQuery,
            isLoading = uiState.isDirectoryLoading,
            selectedOid = uiState.selectedEntity?.oid,
            onQueryChange = onPickerQueryChange,
            onModeChange = onPickerModeChange,
            onSelect = onEntitySelected,
            onDismiss = onDismissPicker
        )
    }
}

@Composable
private fun ScheduleHeader(
    entityLabel: String,
    onOpenPicker: () -> Unit,
    unreadNotificationsCount: Int,
    moderationCount: Int,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.schedule_title),
            style = ScheduleTypography.ScreenTitle,
            color = KampusColors.TextMain
        )

        Spacer(modifier = Modifier.weight(1f))

        // Тёмная пилюля, как в вебке (`.groupBtn`): инверсия фона, чтобы выбор
        // сущности читался как единственное активное действие в шапке. Ширину
        // ограничиваем сверху — подпись уже обрезана по символам, но у кириллицы
        // разная ширина букв, и без потолка пилюля вытеснила бы колокольчик.
        Row(
            modifier = Modifier
                .widthIn(max = 170.dp)
                .clip(RoundedCornerShape(PILL_RADIUS))
                .background(KampusColors.PillBackground)
                .clickable(onClick = onOpenPicker)
                .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = entityLabel,
                style = ScheduleTypography.EntityButton,
                color = KampusColors.OnPill,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = KampusColors.OnPill,
                modifier = Modifier.size(14.dp)
            )
        }

        NotificationsBell(
            unreadCount = unreadNotificationsCount,
            moderationCount = moderationCount,
            onClick = onOpenNotifications
        )
    }
}

/**
 * Переключатель режимов. Три отдельные пилюли, как в вебке (`.tab`/`.tabActive`):
 * активная заливается синим с белым текстом, остальные — белые с рамкой.
 */
@Composable
private fun ScheduleTabs(
    selected: ScheduleView,
    onSelect: (ScheduleView) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScheduleView.entries.forEach { view ->
            val isSelected = view == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(PILL_RADIUS))
                    .background(if (isSelected) KampusColors.PrimaryBlue else KampusColors.Surface)
                    .then(
                        if (isSelected) {
                            Modifier
                        } else {
                            Modifier.border(1.dp, KampusColors.Border, RoundedCornerShape(PILL_RADIUS))
                        }
                    )
                    .clickable { onSelect(view) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(view.titleRes()),
                    style = ScheduleTypography.Tab,
                    color = if (isSelected) KampusColors.Surface else ScheduleColors.TextAccent
                )
            }
        }
    }
}

private fun ScheduleView.titleRes(): Int = when (this) {
    ScheduleView.Today -> R.string.schedule_tab_today
    ScheduleView.Tomorrow -> R.string.schedule_tab_tomorrow
    ScheduleView.Week -> R.string.schedule_tab_week
}

@Composable
private fun DayList(
    day: ScheduleDay,
    selectedGroupName: String,
    currentOid: String?,
    onEntityClick: (ScheduleEntity) -> Unit
) {
    if (day.slots.isEmpty()) {
        EmptyDay(dateIso = day.date)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "day-label-${day.date}") {
            DayLabel(dateIso = day.date)
        }
        items(day.slots.size, key = { index -> "${day.date}-${day.slots[index].number}-$index" }) { index ->
            LessonCard(
                slot = day.slots[index],
                selectedGroupName = selectedGroupName,
                currentOid = currentOid,
                onEntityClick = onEntityClick
            )
        }
    }
}

@Composable
private fun WeekList(
    week: ScheduleWeekResult,
    selectedGroupName: String,
    currentOid: String?,
    isLoadingNextWeek: Boolean,
    focusDate: String?,
    onFocusDateHandled: () -> Unit,
    onEntityClick: (ScheduleEntity) -> Unit,
    onLoadNextWeek: () -> Unit
) {
    val listState = rememberLazyListState()

    // Скроллим к запрошенному с главной дню, а по умолчанию — к сегодняшнему.
    LaunchedEffect(week.days.firstOrNull()?.date, focusDate) {
        val targetDate = focusDate ?: ScheduleDates.todayIso()
        val index = week.days.indexOfFirst { it.date == targetDate }
        if (index > 0) {
            listState.scrollToItem(weekItemIndexOfDay(week.days, index))
        }
        if (focusDate != null && index >= 0) {
            onFocusDateHandled()
        }
    }

    // Догрузка следующей недели: срабатывает, когда до конца списка осталось
    // меньше трёх элементов. snapshotFlow + distinctUntilChanged, чтобы триггер
    // не стрелял на каждый пиксель скролла.
    LaunchedEffect(listState, week.days.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val total = listState.layoutInfo.totalItemsCount
                if (lastVisible != null && total > 0 && lastVisible >= total - 3) {
                    onLoadNextWeek()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        week.days.forEachIndexed { dayIndex, day ->
            // Первый день после полной недели — начало догруженной следующей.
            if (dayIndex > 0 && dayIndex % DAYS_IN_WEEK == 0) {
                val endIndex = minOf(dayIndex + DAYS_IN_WEEK - 1, week.days.lastIndex)
                item(key = "divider-${day.date}") {
                    WeekDivider(
                        fromIso = day.date,
                        toIso = week.days[endIndex].date
                    )
                }
            }

            item(key = "label-${day.date}") {
                DayLabel(dateIso = day.date)
            }

            if (day.slots.isEmpty()) {
                item(key = "empty-${day.date}") {
                    Text(
                        text = stringResource(R.string.schedule_no_lessons),
                        style = ScheduleTypography.DayEmpty,
                        color = KampusColors.TextMuted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                }
            } else {
                items(
                    count = day.slots.size,
                    key = { index -> "${day.date}-${day.slots[index].number}-$index" }
                ) { index ->
                    LessonCard(
                        slot = day.slots[index],
                        selectedGroupName = selectedGroupName,
                        currentOid = currentOid,
                        onEntityClick = onEntityClick
                    )
                }
            }
        }

        if (isLoadingNextWeek) {
            item(key = "next-week-loader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = KampusColors.PrimaryBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Индекс элемента списка, с которого начинается день [dayIndex].
 * У каждого дня есть заголовок плюс либо пары, либо строка «пар нет»,
 * а на стыке недель добавляется разделитель — всё это надо учесть.
 */
private fun weekItemIndexOfDay(days: List<ScheduleDay>, dayIndex: Int): Int {
    var index = 0
    for (i in 0 until dayIndex) {
        if (i > 0 && i % DAYS_IN_WEEK == 0) index++
        index++
        index += if (days[i].slots.isEmpty()) 1 else days[i].slots.size
    }
    if (dayIndex > 0 && dayIndex % DAYS_IN_WEEK == 0) index++
    return index
}

/** Пилюля «Моё расписание»: видна только пока открыто чужое расписание. */
@Composable
private fun ReturnToOwnButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(KampusColors.Surface)
            .border(1.dp, KampusColors.Border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = KampusColors.TextMain,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(R.string.schedule_return_to_own),
            style = ScheduleTypography.Tab,
            color = KampusColors.TextMain,
            maxLines = 1
        )
    }
}

@Composable
private fun DayLabel(dateIso: String) {
    Text(
        text = ScheduleDates.formatDayTitle(dateIso),
        style = ScheduleTypography.DayLabel,
        color = ScheduleColors.TextAccent,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
    )
}

@Composable
private fun WeekDivider(fromIso: String, toIso: String) {
    Text(
        text = stringResource(
            R.string.schedule_next_week,
            ScheduleDates.formatShortDate(fromIso),
            ScheduleDates.formatShortDate(toIso)
        ),
        style = ScheduleTypography.WeekDivider,
        color = KampusColors.OnPill,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(KampusColors.PillBackground)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    )
}

@Composable
private fun EmptyDay(dateIso: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = ScheduleDates.formatDayTitle(dateIso),
            style = ScheduleTypography.DayLabel,
            color = KampusColors.TextMain,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.schedule_no_lessons),
            style = ScheduleTypography.DayEmpty,
            color = KampusColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScheduleError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.schedule_load_error),
            style = ScheduleTypography.Empty,
            color = KampusColors.TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.schedule_retry),
            style = ScheduleTypography.Tab,
            color = KampusColors.PrimaryBlue,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

/** Скелетон загрузки — серые карточки вместо спиннера, как в вебке. */
@Composable
private fun ScheduleSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(KampusColors.FooterBackground)
        )
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KampusColors.FooterBackground)
            )
        }
    }
}

private val previewOwnGroup = ScheduleEntity("1", "ПИ41б", ScheduleEntityType.Group)
private val previewOtherGroup = ScheduleEntity("2", "ИВТ41б", ScheduleEntityType.Group)

private fun previewToday(entity: ScheduleEntity) = ScheduleDayResult(
    entity = entity,
    day = ScheduleDay(
        date = ScheduleDates.todayIso(),
        slots = listOf(
            ScheduleSlot(
                number = "1",
                startTime = "08:30",
                endTime = "10:00",
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
                                LessonAudienceMember("ПИ41б", "ПИ41б", "1"),
                                LessonAudienceMember("ИВТ41б", "ИВТ41б", "2")
                            )
                        )
                    )
                )
            )
        )
    )
)

@Composable
private fun ScheduleContentPreviewHost(uiState: ScheduleUiState) {
    YsukampusTheme(dynamicColor = false) {
        ScheduleContent(
            uiState = uiState,
            onViewChange = {},
            onOpenPicker = {},
            onDismissPicker = {},
            onPickerModeChange = {},
            onPickerQueryChange = {},
            onEntitySelected = {},
            onLessonEntityClick = {},
            onPendingEntityConfirmed = {},
            onPendingEntityDismissed = {},
            onReturnToOwnEntity = {},
            onLoadNextWeek = {},
            onRetry = {},
            onMessageShown = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F7FF, widthDp = 360, heightDp = 800)
@Composable
private fun ScheduleContentPreview() {
    ScheduleContentPreviewHost(
        ScheduleUiState(
            selectedEntity = previewOwnGroup,
            ownerEntity = previewOwnGroup,
            today = previewToday(previewOwnGroup)
        )
    )
}

/** Чужое расписание: снизу видна плашка возврата к своему. */
@Preview(showBackground = true, backgroundColor = 0xFFF4F7FF, widthDp = 360, heightDp = 800)
@Composable
private fun ScheduleContentForeignEntityPreview() {
    ScheduleContentPreviewHost(
        ScheduleUiState(
            selectedEntity = previewOtherGroup,
            ownerEntity = previewOwnGroup,
            today = previewToday(previewOtherGroup)
        )
    )
}
