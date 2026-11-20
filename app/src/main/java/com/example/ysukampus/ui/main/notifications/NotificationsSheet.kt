package com.example.ysukampus.ui.main.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.data.notifications.Notification
import com.example.ysukampus.data.notifications.NotificationActor
import com.example.ysukampus.data.notifications.NotificationKind
import com.example.ysukampus.data.notifications.NotificationTarget
import com.example.ysukampus.ui.main.BlurShellWhileVisible
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.KampusSheet
import com.example.ysukampus.ui.main.forum.ForumErrorBlock
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.main.home.components.SkeletonBlock
import com.example.ysukampus.ui.main.notifications.components.NotificationCard
import com.example.ysukampus.ui.theme.NotificationsTypography
import com.example.ysukampus.ui.theme.YsukampusTheme
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Шторка уведомлений — перенос веб-компонента `NotificationsBottomSheet`.
 *
 * Состояние приходит снаружи: модель живёт на уровне всей оболочки, потому что
 * бейджи должны обновляться и при закрытой шторке. Тап по доступной карточке
 * закрывает шторку и просит оболочку открыть тред — сама она никуда не ходит.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    uiState: NotificationsUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNotificationClick: (Int) -> Unit,
    onReadAllClick: () -> Unit,
    onModerationClick: () -> Unit,
    onMessageShown: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BlurShellWhileVisible(sheetState)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KampusColors.AppBackground,
        // Затемнение делаем лёгким: за шторкой оболочка размывается
        // (см. MainScreen), и плотный скрим этот блюр просто съел бы.
        scrimColor = Color.Black.copy(alpha = 0.2f)
    ) {
        NotificationsContent(
            uiState = uiState,
            onRetry = onRetry,
            onLoadMore = onLoadMore,
            onNotificationClick = onNotificationClick,
            onReadAllClick = onReadAllClick,
            onModerationClick = onModerationClick,
            onMessageShown = onMessageShown
        )
    }
}

/** Отрисовка без ViewModel — содержимое шторки покрывается @Preview без сети. */
@Composable
fun NotificationsContent(
    uiState: NotificationsUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNotificationClick: (Int) -> Unit,
    onReadAllClick: () -> Unit,
    onModerationClick: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onMessageShown()
    }

    // Догрузка страницы — тот же приём, что в ленте форума: триггер стреляет,
    // когда до конца списка осталось меньше трёх элементов.
    LaunchedEffect(listState, uiState.items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val total = listState.layoutInfo.totalItemsCount
                if (lastVisible != null && total > 0 && lastVisible >= total - 3) {
                    onLoadMore()
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(KampusSheet.HeightFraction)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.notifications_title),
                style = NotificationsTypography.SheetTitle,
                color = KampusColors.TextMain,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            )

            // Очередь модерации — не уведомление, а живое состояние: она висит
            // над списком, не читается и уходит сама, когда очередь пуста.
            // Пока она на экране, пустого состояния нет — ей и так есть что
            // показать.
            if (!uiState.isLoading && uiState.error == null && uiState.moderationCount > 0) {
                ModerationRow(
                    count = uiState.moderationCount,
                    onClick = onModerationClick,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.error != null && uiState.items.isEmpty() -> ForumErrorBlock(
                        message = uiState.error,
                        onRetry = onRetry,
                        modifier = Modifier.padding(16.dp)
                    )

                    uiState.isLoading && uiState.items.isEmpty() ->
                        NotificationsSkeleton(modifier = Modifier.padding(horizontal = 10.dp))

                    uiState.isEmpty -> NotificationsEmpty()

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            end = 10.dp,
                            top = 6.dp,
                            // Место под плашку «Прочитать все», чтобы она не
                            // накрывала последнюю карточку.
                            bottom = 66.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.items, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = { onNotificationClick(notification.id) }
                            )
                        }

                        if (uiState.isLoadingMore) {
                            item(key = "notifications-loading-more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = HomeColors.Primary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Плашка появляется только пока есть что читать — как в вебке.
        AnimatedVisibility(
            visible = !uiState.isLoading && uiState.error == null && uiState.unreadCount > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(KampusColors.AppBackground),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onReadAllClick, enabled = !uiState.isReadingAll) {
                    Text(
                        text = stringResource(R.string.notifications_read_all),
                        style = NotificationsTypography.Action,
                        color = if (uiState.isReadingAll) HomeColors.TextMuted else HomeColors.Primary
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Строка очереди модерации — всегда первая и вне списка уведомлений. */
@Composable
private fun ModerationRow(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NotificationsColors.WarningSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.notifications_moderation_row),
            style = NotificationsTypography.Action,
            color = NotificationsColors.Warning,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .widthIn(min = 22.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(NotificationsColors.Warning)
                .padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = NotificationsTypography.Badge,
                color = KampusColors.Surface
            )
        }
    }
}

@Composable
private fun NotificationsEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_schedule_notifications),
            contentDescription = null,
            tint = HomeColors.TextMuted,
            modifier = Modifier.size(50.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.notifications_empty_title),
            style = NotificationsTypography.EmptyTitle,
            color = HomeColors.TextMuted
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = stringResource(R.string.notifications_empty_text),
            style = NotificationsTypography.EmptyText,
            color = HomeColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 220.dp)
        )
    }
}

/** Скелетон первой загрузки: четыре строки той же формы, что и карточка. */
@Composable
private fun NotificationsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                SkeletonBlock(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(999.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(12.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun NotificationsContentPreview() {
    YsukampusTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(KampusColors.AppBackground)) {
            NotificationsContent(
                uiState = NotificationsUiState(
                    isLoading = false,
                    unreadCount = 2,
                    moderationCount = 3,
                    items = listOf(
                        previewNotification(1, NotificationKind.Comment, null),
                        previewNotification(2, NotificationKind.Reply, null),
                        previewNotification(3, NotificationKind.Reply, "2026-09-03T10:00:00Z")
                    )
                ),
                onRetry = {},
                onLoadMore = {},
                onNotificationClick = {},
                onReadAllClick = {},
                onModerationClick = {},
                onMessageShown = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun NotificationsEmptyPreview() {
    YsukampusTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(KampusColors.AppBackground)) {
            NotificationsContent(
                uiState = NotificationsUiState(isLoading = false),
                onRetry = {},
                onLoadMore = {},
                onNotificationClick = {},
                onReadAllClick = {},
                onModerationClick = {},
                onMessageShown = {}
            )
        }
    }
}

private fun previewNotification(
    id: Int,
    kind: NotificationKind,
    readAtIso: String?
): Notification = Notification(
    id = id,
    kind = kind,
    actor = NotificationActor(
        profileId = "1a2b",
        displayName = "Onblessedboy",
        displayColor = "#3B82F6",
        avatarImageUrl = null
    ),
    target = NotificationTarget(
        postId = "post-1",
        commentId = "comment-$id",
        preview = "Да, пара будет, только в другой аудитории",
        imagePreviewUrl = null,
        isAvailable = true,
        unavailableReason = null
    ),
    createdAtIso = "2026-09-04T09:00:00Z",
    readAtIso = readAtIso
)
