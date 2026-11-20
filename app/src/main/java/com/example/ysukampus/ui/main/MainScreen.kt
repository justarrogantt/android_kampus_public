package com.example.ysukampus.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ysukampus.ui.main.components.MainBottomBar
import com.example.ysukampus.ui.main.forum.ForumScreen
import com.example.ysukampus.ui.main.forum.ForumThreadHost
import com.example.ysukampus.ui.main.forum.ForumViewModel
import com.example.ysukampus.ui.main.forum.ForumViewModelFactory
import com.example.ysukampus.ui.main.home.HomeScreen
import com.example.ysukampus.ui.main.notifications.NotificationsSheet
import com.example.ysukampus.ui.main.notifications.NotificationsViewModel
import com.example.ysukampus.ui.main.notifications.NotificationsViewModelFactory
import com.example.ysukampus.ui.main.profile.ProfileActions
import com.example.ysukampus.ui.main.profile.ProfileScreen
import com.example.ysukampus.ui.main.profile.ProfileUiState
import com.example.ysukampus.ui.main.profile.activity.MyProfileSheet
import com.example.ysukampus.ui.main.schedule.ScheduleScreen
import com.example.ysukampus.ui.main.update.BetaUpdateBottomSheet
import com.example.ysukampus.ui.main.update.BetaUpdateState
import com.example.ysukampus.ui.theme.YsukampusTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onOpenAdmin: () -> Unit,
    betaUpdateState: BetaUpdateState = BetaUpdateState(),
    onDismissBetaUpdate: () -> Unit = {},
    onStartBetaUpdate: () -> Unit = {},
    profileUiState: ProfileUiState = ProfileUiState(),
    profileActions: ProfileActions = ProfileActions(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Уведомления живут на уровне оболочки, а не одной вкладки: бейджи должны
    // обновляться и при закрытой шторке, а персональный сокет — пока
    // пользователь вообще находится в авторизованной зоне.
    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = remember(context) { NotificationsViewModelFactory(context) }
    )
    val notificationsState by notificationsViewModel.uiState.collectAsStateWithLifecycle()

    // Тред живёт над вкладками, а не внутри «Форума», поэтому его ViewModel
    // берёт оболочка: открыть комментарии можно с главной, из шторки профиля и
    // по тапу в уведомлении, ни на какую вкладку при этом не переходя. Вкладка
    // «Форум» получает через `viewModel()` тот же экземпляр.
    val forumViewModel: ForumViewModel = viewModel(
        factory = remember(context) { ForumViewModelFactory(context) }
    )

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    // Вкладки листаются пальцем, поэтому под ними лежит пейджер. Выбранная
    // вкладка остаётся источником правды для таббара, а пейджер и она держат
    // друг друга в согласии: тап уводит пейджер, свайп — переставляет вкладку.
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { MainTab.entries.size }
    val scope = rememberCoroutineScope()

    // Сколько прокруток таббар запустил сам. Счётчик, а не флаг: новый тап
    // отменяет предыдущую прокрутку, и одиночный флаг успел бы сброситься уже
    // после того, как вторая корутина его подняла.
    var programmaticScrolls by remember { mutableIntStateOf(0) }

    // Прокрутку запускает само нажатие, а не `LaunchedEffect(selectedTab)`.
    // Через вкладку (1 → 3) эффект перезапускался от промежуточной страницы,
    // которую пейджер успевал объявить текущей по дороге, отменял свою же
    // анимацию и оставлял экран стоять между вкладками.
    val openTab: (MainTab) -> Unit = { tab ->
        selectedTab = tab
        scope.launch {
            programmaticScrolls++
            try {
                // Тап по таббару едет ровно так же, как жест, а не по пружине
                // пейджера по умолчанию.
                pagerState.animateScrollToPage(
                    page = tab.ordinal,
                    animationSpec = KampusMotion.enter(KampusMotion.ScreenSlideInMs)
                )
            } finally {
                programmaticScrolls--
            }
        }
    }

    // Положение пейджера в «долях вкладки» — но только пока его ведёт палец
    // (и пока доезжает бросок после отпускания). От тапа таббар анимируется
    // по-старому, пружиной от вкладки к вкладке; под жестом синий перетекает
    // из одной таблетки в другую вместе со страницей.
    val swipePosition: () -> Float? = {
        if (pagerState.isScrollInProgress && programmaticScrolls == 0) {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        } else {
            null
        }
    }

    // Свайп между вкладками выключен, пока сверху открыт тред: иначе жест по
    // нему листал бы вкладки под ним. Смотрим на один флаг, а не на всё
    // состояние форума, — иначе набор текста в комментарии перерисовывал бы
    // всю оболочку.
    val isThreadOpen by remember(forumViewModel) {
        forumViewModel.uiState.map { it.openedPostId != null }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    // `targetPage` — это страница, на которой жест или анимация закончатся:
    // пилюля в таббаре переезжает вместе с пальцем, но по дороге не отмечает
    // вкладки, через которые пейджер только пролетает.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage }.collect { page ->
            selectedTab = MainTab.entries[page]
        }
    }
    // День, выбранный на главной: расписание откроется на нём и сбросит запрос.
    var scheduleFocusDate by rememberSaveable { mutableStateOf<String?>(null) }
    // Шторка своего профиля форума: маршрутов в проекте нет, это состояние.
    var isMyProfileOpen by rememberSaveable { mutableStateOf(false) }
    // Шторка чужого профиля — тот же оверлей, но с явным profile_id автора,
    // по которому тапнули на карточке поста или комментария.
    var openedAuthorProfileId by rememberSaveable { mutableStateOf<String?>(null) }

    // Канал `/ws/user` включён ровно на время пребывания в оболочке: после
    // разлогина композиция уходит, и сокет не крутится с мёртвым токеном.
    DisposableEffect(Unit) {
        notificationsViewModel.onEnterAuthorizedZone()
        onDispose { notificationsViewModel.onLeaveAuthorizedZone() }
    }

    // Тап по уведомлению открывает тред поверх той вкладки, где находится
    // пользователь: «назад» из него возвращает туда же.
    LaunchedEffect(notificationsState.openTarget) {
        val target = notificationsState.openTarget ?: return@LaunchedEffect
        forumViewModel.onPostClick(target.postId, target.commentId)
        notificationsViewModel.onOpenTargetHandled()
    }

    // Оболочка размывается под любой открытой шторкой — каждая отмечается в
    // этом счётчике через `BlurShellWhileVisible`. `Modifier.blur` работает с
    // API 31; на более старых системах он молча ничего не делает, и остаётся
    // обычное затемнение шторки.
    //
    // Гаснуть блюр начинает, как только шторка поехала вниз (см.
    // `BlurShellWhileVisible`), и укладывается в 100 мс.
    // Изинг на угасании линейный: с дефолтным FastOutSlowIn почти весь блюр
    // сходил в первые десятки миллисекунд, а остаток времени тянулся незаметный
    // хвост — на глаз это снова выглядело как скачок.
    val sheetBlurState = remember { KampusSheetBlurState() }
    val isSheetOpen = sheetBlurState.isAnySheetOpen
    val shellBlur by animateDpAsState(
        targetValue = if (isSheetOpen) SHELL_BLUR_RADIUS else 0.dp,
        animationSpec = if (isSheetOpen) {
            tween(durationMillis = SHELL_BLUR_IN_MS)
        } else {
            tween(durationMillis = SHELL_BLUR_OUT_MS, easing = LinearEasing)
        },
        label = "shell-blur"
    )

    CompositionLocalProvider(LocalKampusSheetBlur provides sheetBlurState) {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(shellBlur),
                containerColor = KampusColors.AppBackground,
                bottomBar = {
                    // Под открытым тредом таббара нет вообще. Он оставался
                    // видимым и кликабельным: тап переключал вкладку под
                    // тредом, а сам тред продолжал висеть сверху — экран
                    // оказывался в состоянии, из которого выход только через
                    // «назад». Тред занимает весь экран, как в вебке.
                    if (!isThreadOpen) {
                        MainBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = openTab,
                            swipePosition = swipePosition
                        )
                    }
                }
            ) { padding ->
                // Клавиатура перекрывает таббар целиком, поэтому и место под
                // него резервировать незачем: иначе поле ввода комментария
                // висело бы над клавиатурой с пустой полосой в высоту таббара.
                val density = LocalDensity.current
                val layoutDirection = LocalLayoutDirection.current
                val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
                val contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = (padding.calculateBottomPadding() - imeBottom).coerceAtLeast(0.dp)
                )

                // Тред сам разбирается с инсетами снизу: поле ввода встаёт над
                // навигацией или над клавиатурой. Без обнуления отступа
                // системная навигация отсчиталась бы дважды.
                val threadPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding(),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                    bottom = 0.dp
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KampusColors.AppBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    ) {
                        // Вкладки листаются пальцем, поэтому и едут за ним —
                        // тем же движением, что и наезд треда. Растворение одной
                        // вкладки в другой (как в вебке) под жестом выглядело
                        // неправильно: палец тянет вбок, а на экране ничего не
                        // едет.
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            // Соседняя вкладка не должна оживать от касания раньше,
                            // чем жест на неё действительно уводит.
                            beyondViewportPageCount = 0,
                            userScrollEnabled = !isThreadOpen
                        ) { page ->
                            when (MainTab.entries[page]) {
                                MainTab.Home -> HomeScreen(
                                    onOpenForum = { openTab(MainTab.Forum) },
                                    onOpenPost = forumViewModel::onPostClick,
                                    onOpenSchedule = { openTab(MainTab.Schedule) },
                                    onOpenScheduleOnDate = { date ->
                                        scheduleFocusDate = date
                                        openTab(MainTab.Schedule)
                                    },
                                    unreadNotificationsCount = notificationsState.unreadCount,
                                    moderationCount = notificationsState.moderationCount,
                                    onOpenNotifications = notificationsViewModel::onOpenSheet,
                                    onAuthorClick = { profileId -> openedAuthorProfileId = profileId }
                                )
                                MainTab.Schedule -> ScheduleScreen(
                                    focusDate = scheduleFocusDate,
                                    onFocusDateHandled = { scheduleFocusDate = null },
                                    unreadNotificationsCount = notificationsState.unreadCount,
                                    moderationCount = notificationsState.moderationCount,
                                    onOpenNotifications = notificationsViewModel::onOpenSheet
                                )
                                MainTab.Forum -> ForumScreen(
                                    unreadNotificationsCount = notificationsState.unreadCount,
                                    moderationCount = notificationsState.moderationCount,
                                    onOpenNotifications = notificationsViewModel::onOpenSheet,
                                    onAuthorClick = { profileId -> openedAuthorProfileId = profileId }
                                )
                                MainTab.Profile -> ProfileScreen(
                                    onOpenAdmin = onOpenAdmin,
                                    isUpdateAvailable = betaUpdateState.isUpdateAvailable,
                                    isUpdateActionInProgress = betaUpdateState.isStartingUpdate,
                                    onUpdateClick = onStartBetaUpdate,
                                    onOpenMyProfile = { isMyProfileOpen = true },
                                    onOpenNotifications = notificationsViewModel::onOpenSheet,
                                    showNotificationsBadge = notificationsState.hasBadge,
                                    notificationsBadgeLabel = notificationsState.badgeLabel,
                                    uiState = profileUiState,
                                    actions = profileActions
                                )
                            }
                        }
                    }

                    // Тред — слой над вкладками: он появляется одинаково, из
                    // ленты форума или с главной, и не трогает выбор вкладки.
                    Box(modifier = Modifier.fillMaxSize().padding(threadPadding)) {
                        ForumThreadHost(
                            viewModel = forumViewModel,
                            onAuthorClick = { profileId -> openedAuthorProfileId = profileId }
                        )
                    }
                }
            }

            if (isMyProfileOpen || openedAuthorProfileId != null) {
                MyProfileSheet(
                    profileId = openedAuthorProfileId,
                    onDismiss = {
                        isMyProfileOpen = false
                        openedAuthorProfileId = null
                    },
                    onOpenThread = { postId ->
                        // Тред открывается поверх профиля, шторку закрываем.
                        isMyProfileOpen = false
                        openedAuthorProfileId = null
                        forumViewModel.onPostClick(postId)
                    }
                )
            }

            if (notificationsState.isSheetOpen) {
                NotificationsSheet(
                    uiState = notificationsState,
                    onDismiss = notificationsViewModel::onCloseSheet,
                    onRetry = notificationsViewModel::onRetry,
                    onLoadMore = notificationsViewModel::onLoadMore,
                    onNotificationClick = notificationsViewModel::onNotificationClick,
                    onReadAllClick = notificationsViewModel::onReadAllClick,
                    onModerationClick = {
                        // Очередь модерации разбирается в админке — шторку закрываем.
                        notificationsViewModel.onCloseSheet()
                        onOpenAdmin()
                    },
                    onMessageShown = notificationsViewModel::onMessageShown
                )
            }

            BetaUpdateBottomSheet(
                isVisible = betaUpdateState.isBottomSheetVisible,
                displayVersion = betaUpdateState.displayVersion,
                buildVersion = betaUpdateState.buildVersion,
                releaseNotes = betaUpdateState.releaseNotes,
                isLoading = betaUpdateState.isStartingUpdate,
                onDismiss = onDismissBetaUpdate,
                onUpdateClick = onStartBetaUpdate,
                failure = betaUpdateState.failure
            )
        }
    }
}

/** Насколько размывается оболочка под открытой шторкой. */
private val SHELL_BLUR_RADIUS = 4.dp

private const val SHELL_BLUR_IN_MS = 180
private const val SHELL_BLUR_OUT_MS = 100

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MainScreenPreview() {
    YsukampusTheme(dynamicColor = false) {
        MainScreen(onOpenAdmin = {})
    }
}
