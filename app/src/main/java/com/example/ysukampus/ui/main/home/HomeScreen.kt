package com.example.ysukampus.ui.main.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.KampusMotion
import com.example.ysukampus.ui.main.notifications.components.NotificationsBell
import com.example.ysukampus.ui.main.forum.ForumPost
import com.example.ysukampus.ui.main.forum.ForumPostStatus
import com.example.ysukampus.ui.main.forum.components.ThreadCard
import com.example.ysukampus.ui.main.forum.forumTimeAgo
import com.example.ysukampus.ui.main.home.components.BuzzListSkeleton
import com.example.ysukampus.ui.main.home.components.DayCard
import com.example.ysukampus.ui.main.home.components.NextLessonCard
import com.example.ysukampus.ui.main.home.components.NextLessonEmptyCard
import com.example.ysukampus.ui.main.home.components.NextLessonSkeleton
import com.example.ysukampus.ui.main.home.components.WeatherSkeleton
import com.example.ysukampus.ui.main.home.components.WeekStripSkeleton
import com.example.ysukampus.ui.theme.HomeTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Главная — перенос веб-страницы `pages/MainPage/Home`.
 *
 * Порядок блоков тот же: шапка с логотипом, приветствие с погодой, полоска дней
 * недели, карточка ближайшей пары и первые посты форума. Открытие треда пока
 * ведёт на вкладку «Форум» — самой ленты в приложении ещё нет.
 */
@Composable
fun HomeScreen(
    onOpenForum: () -> Unit,
    /** Тред конкретного поста: тап по карточке или по счётчику комментариев. */
    onOpenPost: (String) -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenScheduleOnDate: (String) -> Unit,
    /** Непрочитанные уведомления — бейдж на колокольчике в шапке. */
    unreadNotificationsCount: Int = 0,
    /** Размер очереди модерации: бейдж без числа, как в вебке. */
    moderationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    /** Тап по автору поста — открывает его публичный профиль. */
    onAuthorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = remember(context) { HomeViewModelFactory(context) }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Вкладка открыта заново или приложение вышло из фона — повторяем то, что
    // в прошлый раз не загрузилось. Уже загруженные блоки не перезапрашиваем.
    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenShown()
        onPauseOrDispose {}
    }

    HomeContent(
        uiState = uiState,
        onOpenForum = onOpenForum,
        onOpenPost = onOpenPost,
        onOpenSchedule = onOpenSchedule,
        onOpenScheduleOnDate = onOpenScheduleOnDate,
        onRetryWeek = viewModel::onRetryWeek,
        onRetryPosts = viewModel::onRetryPosts,
        onLikeClick = viewModel::onLikeClick,
        onReportClick = viewModel::onReportClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissConfirmation = viewModel::onDismissConfirmation,
        onConfirmReport = viewModel::onConfirmReport,
        onConfirmDelete = viewModel::onConfirmDelete,
        onMessageShown = viewModel::onMessageShown,
        unreadNotificationsCount = unreadNotificationsCount,
        moderationCount = moderationCount,
        onOpenNotifications = onOpenNotifications,
        onAuthorClick = onAuthorClick,
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onOpenForum: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenScheduleOnDate: (String) -> Unit,
    onRetryWeek: () -> Unit,
    onRetryPosts: () -> Unit,
    onLikeClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onDismissConfirmation: () -> Unit,
    onConfirmReport: () -> Unit,
    onConfirmDelete: () -> Unit,
    onMessageShown: () -> Unit,
    unreadNotificationsCount: Int = 0,
    moderationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onMessageShown()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader(
                unreadNotificationsCount = unreadNotificationsCount,
                moderationCount = moderationCount,
                onOpenNotifications = onOpenNotifications
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Greeting(name = uiState.greetingName, weather = uiState.weather)

                WeekSection(
                    uiState = uiState,
                    onDayClick = onOpenScheduleOnDate,
                    onRetry = onRetryWeek
                )

                NextLessonSection(
                    uiState = uiState,
                    onOpenSchedule = onOpenSchedule
                )

                Text(
                    text = stringResource(R.string.home_buzz_title),
                    style = HomeTypography.SectionLabel,
                    color = HomeColors.TextMain,
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                BuzzSection(
                    uiState = uiState,
                    onOpenPost = onOpenPost,
                    onRetry = onRetryPosts,
                    onLikeClick = onLikeClick,
                    onReportClick = onReportClick,
                    onDeleteClick = onDeleteClick,
                    onAuthorClick = onAuthorClick
                )

                if (!uiState.isPostsLoading && uiState.postsError == null) {
                    AllPostsButton(onClick = onOpenForum)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (uiState.reportPostId != null) {
        ConfirmationDialog(
            title = stringResource(R.string.forum_report_title),
            text = stringResource(R.string.forum_report_text),
            confirmLabel = stringResource(R.string.forum_report),
            onConfirm = onConfirmReport,
            onDismiss = onDismissConfirmation
        )
    }

    if (uiState.deletePostId != null) {
        ConfirmationDialog(
            title = stringResource(R.string.forum_delete_title),
            text = stringResource(R.string.forum_delete_text),
            confirmLabel = stringResource(R.string.forum_delete),
            onConfirm = onConfirmDelete,
            onDismiss = onDismissConfirmation
        )
    }
}

/**
 * Длительность подмены скелетона реальным блоком. Одна на все блоки главной:
 * они догружаются вразнобой, и разный темп проявления сразу заметен.
 */
private const val BLOCK_REVEAL_MS = 260

/** Шапка: логотип с выделенной «k», пилюля beta и колокольчик уведомлений. */
@Composable
private fun HomeHeader(
    unreadNotificationsCount: Int,
    moderationCount: Int,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KampusColors.AppBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.home_logo_prefix))
                withStyle(SpanStyle(color = HomeColors.Primary)) {
                    append(stringResource(R.string.home_logo_accent))
                }
                append(stringResource(R.string.home_logo_suffix))
            },
            style = HomeTypography.Logo,
            color = HomeColors.TextMain
        )

        Text(
            text = stringResource(R.string.home_beta),
            style = HomeTypography.BetaBadge,
            color = HomeColors.NeutralBadgeText,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(HomeColors.NeutralBadgeBackground)
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Колокольчик справа — как в вебке (`Home.tsx`: логотип слева, уведомления справа).
        NotificationsBell(
            unreadCount = unreadNotificationsCount,
            moderationCount = moderationCount,
            onClick = onOpenNotifications
        )
    }
}

@Composable
private fun Greeting(name: String, weather: Weather?) {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)) {
        Text(
            text = if (name.isBlank()) {
                stringResource(R.string.home_greeting_no_name)
            } else {
                stringResource(R.string.home_greeting, name)
            },
            style = HomeTypography.GreetingName,
            color = HomeColors.TextMain
        )

        Text(
            text = HomeDates.formatGreetingDate(),
            style = HomeTypography.GreetingDate,
            color = HomeColors.TextAccent,
            modifier = Modifier.padding(top = 6.dp)
        )

        Box(modifier = Modifier.padding(top = 6.dp)) {
            Crossfade(
                targetState = weather,
                animationSpec = KampusMotion.enter(BLOCK_REVEAL_MS),
                label = "home-weather"
            ) { current ->
                if (current == null) WeatherSkeleton() else WeatherRow(weather = current)
            }
        }
    }
}

@Composable
private fun WeatherRow(weather: Weather) {
    val iconRes = weatherIconRes(weather.type, weather.isDay)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(R.string.home_weather_icon_description),
                tint = HomeColors.TextAccent,
                modifier = Modifier.size(16.dp)
            )
        }

        val temperature = if (weather.temperature > 0) {
            stringResource(R.string.home_temperature_positive, weather.temperature)
        } else {
            stringResource(R.string.home_temperature, weather.temperature)
        }

        Text(
            text = stringResource(R.string.home_weather_line, temperature, weather.condition),
            style = HomeTypography.Weather,
            color = HomeColors.TextAccent
        )
    }
}

/** Полоска дней недели: клик по дню открывает расписание на этом дне. */
@Composable
private fun WeekSection(
    uiState: HomeUiState,
    onDayClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.padding(bottom = 14.dp)) {
        when {
            uiState.weekError != null && uiState.week == null -> HomeErrorBlock(
                message = uiState.weekError,
                onRetry = onRetry
            )

            // Скелетон и полоска дней одного размера, поэтому подмена
            // проявляется без прыжка вёрстки — растворяем одно в другом.
            else -> Crossfade(
                targetState = uiState.week != null,
                animationSpec = KampusMotion.enter(BLOCK_REVEAL_MS),
                label = "home-week"
            ) { loaded ->
                if (!loaded) {
                    WeekStripSkeleton()
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        uiState.days.forEach { day ->
                            DayCard(
                                name = HomeDates.formatWeekdayShort(day.date),
                                dayOfMonth = HomeDates.dayOfMonth(day.date),
                                lessonsCount = day.slots.size,
                                isActive = day.date == HomeDates.todayIso,
                                onClick = { onDayClick(day.date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextLessonSection(
    uiState: HomeUiState,
    onOpenSchedule: () -> Unit
) {
    val slot = uiState.nextSlot
    val lesson = uiState.nextLesson

    Box(modifier = Modifier.padding(bottom = 8.dp)) {
        when {
            // Ошибку расписания показывает уже полоска дней — здесь достаточно повтора.
            uiState.weekError != null && uiState.week == null -> Unit

            else -> Crossfade(
                targetState = uiState.week != null,
                animationSpec = KampusMotion.enter(BLOCK_REVEAL_MS),
                label = "home-next-lesson"
            ) { loaded ->
                when {
                    !loaded -> NextLessonSkeleton()

                    slot != null && lesson != null -> NextLessonCard(
                        slot = slot,
                        lesson = lesson,
                        onClick = onOpenSchedule
                    )

                    else -> NextLessonEmptyCard(
                        hasLessonsToday = uiState.hasLessonsToday,
                        onClick = onOpenSchedule
                    )
                }
            }
        }
    }
}

@Composable
private fun BuzzSection(
    uiState: HomeUiState,
    onOpenPost: (String) -> Unit,
    onRetry: () -> Unit,
    onLikeClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    Box(modifier = Modifier.padding(bottom = 14.dp)) {
        when {
            uiState.postsError != null && uiState.posts.isEmpty() -> HomeErrorBlock(
                message = uiState.postsError,
                onRetry = onRetry
            )

            else -> Crossfade(
                targetState = uiState.isPostsLoading && uiState.posts.isEmpty(),
                animationSpec = KampusMotion.enter(BLOCK_REVEAL_MS),
                label = "home-buzz"
            ) { loading ->
                when {
                    loading -> BuzzListSkeleton()

                    uiState.isPostsEmpty -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_buzz_empty),
                            style = HomeTypography.AllPosts,
                            color = HomeColors.TextMuted
                        )
                    }

                    else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.posts.forEach { post ->
                            ThreadCard(
                                post = post,
                                timeAgo = forumTimeAgo(post.createdAtIso),
                                // Тред живёт на вкладке «Форум»: и карточка, и
                                // счётчик комментариев открывают именно его.
                                onClick = { onOpenPost(post.id) },
                                onLikeClick = { onLikeClick(post.id) },
                                onReportClick = { onReportClick(post.id) },
                                onDeleteClick = { onDeleteClick(post.id) },
                                onAuthorClick = post.profileId?.let { id -> { onAuthorClick(id) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllPostsButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
    ) {
        Text(
            text = stringResource(R.string.home_all_posts),
            style = HomeTypography.AllPosts,
            color = HomeColors.TextAccent
        )
        Icon(
            painter = painterResource(R.drawable.ic_home_arrow),
            contentDescription = null,
            tint = HomeColors.TextAccent,
            modifier = Modifier.size(14.dp)
        )
    }
}

/** Ошибка блока с повтором — вместо бесконечного скелетона. */
@Composable
private fun HomeErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HomeColors.PrimarySoft)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = message,
            style = HomeTypography.NextMeta,
            color = HomeColors.TextAccent,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.schedule_retry),
                style = HomeTypography.AllPosts,
                color = HomeColors.Primary
            )
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = HomeTypography.NextSubject, color = HomeColors.TextMain) },
        text = { Text(text = text, style = HomeTypography.NextMeta, color = HomeColors.TextAccent) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, color = HomeColors.Danger, style = HomeTypography.AllPosts)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.forum_cancel),
                    color = HomeColors.TextAccent,
                    style = HomeTypography.AllPosts
                )
            }
        },
        containerColor = HomeColors.Surface
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeContentPreview() {
    YsukampusTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(KampusColors.AppBackground)) {
            HomeContent(
                uiState = HomeUiState(
                    greetingName = "Эмиль",
                    weather = Weather(
                        city = "Ханты-Мансийск",
                        temperature = 7,
                        condition = "Солнечно",
                        type = WeatherType.Clear,
                        isDay = true
                    ),
                    isWeekLoading = false,
                    isPostsLoading = false,
                    posts = listOf(
                        ForumPost(
                            id = "1",
                            profileId = null,
                            displayName = "Аноним",
                            displayColor = "#3b82f6",
                            avatarImageUrl = null,
                            visibleRole = "Студент",
                            isMine = false,
                            content = "Кто-нибудь знает, будет ли пара по матанализу в пятницу?",
                            status = ForumPostStatus.Active,
                            likesCount = 12,
                            commentsCount = 3,
                            likedByMe = false,
                            createdAtIso = "2026-08-28T09:00:00"
                        )
                    )
                ),
                onOpenForum = {},
                onOpenPost = {},
                onOpenSchedule = {},
                onOpenScheduleOnDate = {},
                onRetryWeek = {},
                onRetryPosts = {},
                onLikeClick = {},
                onReportClick = {},
                onDeleteClick = {},
                onDismissConfirmation = {},
                onConfirmReport = {},
                onConfirmDelete = {},
                onMessageShown = {}
            )
        }
    }
}
