package com.example.ysukampus.ui.main.forum

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.BlurShellWhileVisible
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.notifications.components.NotificationsBell
import com.example.ysukampus.ui.main.forum.components.ImageAttachButton
import com.example.ysukampus.ui.main.forum.components.ImageAttachPreview
import com.example.ysukampus.ui.main.forum.components.ThreadCard
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.main.home.components.BuzzListSkeleton
import com.example.ysukampus.ui.theme.ForumTypography
import com.example.ysukampus.ui.theme.HomeTypography
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Вкладка «Форум»: лента постов с курсорной пагинацией и открытый из неё тред.
 *
 * Карточку и действия над постом делит с главной — отличается только тем, что
 * лента бесконечная. Маршрутов здесь нет: тред рисуется вместо ленты, пока в
 * состоянии заполнен `openedPostId`.
 */
@Composable
fun ForumScreen(
    modifier: Modifier = Modifier,
    /** Непрочитанные уведомления — бейдж на колокольчике в шапке. */
    unreadNotificationsCount: Int = 0,
    /** Размер очереди модерации: бейдж без числа, как в вебке. */
    moderationCount: Int = 0,
    onOpenNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ForumViewModel = viewModel(
        factory = remember(context) { ForumViewModelFactory(context) }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
    }

    ForumContent(
        uiState = uiState,
        onRetry = viewModel::onRetry,
        onLoadMore = viewModel::onLoadMore,
        onLikeClick = viewModel::onLikeClick,
        onReportClick = viewModel::onReportClick,
        onDeleteClick = viewModel::onDeleteClick,
        onPostClick = viewModel::onPostClick,
        onCreatePostClick = viewModel::onCreatePostClick,
        onComposerDismiss = viewModel::onComposerDismiss,
        onComposerDraftChange = viewModel::onComposerDraftChange,
        onComposerImagePicked = viewModel::onComposerImagePicked,
        onComposerImageRemoved = viewModel::onComposerImageRemoved,
        onCreatePost = viewModel::onCreatePost,
        onImageClick = viewModel::onImageClick,
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
fun ForumContent(
    uiState: ForumUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onLikeClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onCreatePostClick: () -> Unit,
    onComposerDismiss: () -> Unit,
    onComposerDraftChange: (String) -> Unit,
    onComposerImagePicked: (Uri) -> Unit,
    onComposerImageRemoved: () -> Unit,
    onCreatePost: () -> Unit,
    onImageClick: (String) -> Unit,
    unreadNotificationsCount: Int = 0,
    moderationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ForumHeader(
                unreadNotificationsCount = unreadNotificationsCount,
                moderationCount = moderationCount,
                onOpenNotifications = onOpenNotifications
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.error != null -> ForumErrorBlock(
                        message = uiState.error,
                        onRetry = onRetry,
                        modifier = Modifier.padding(16.dp)
                    )

                    uiState.isLoading -> ForumFeedSkeleton()

                    uiState.isEmpty -> Text(
                        text = stringResource(R.string.forum_empty),
                        style = HomeTypography.AllPosts,
                        color = HomeColors.TextMuted,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    else -> ForumFeed(
                        uiState = uiState,
                        onLoadMore = onLoadMore,
                        onPostClick = onPostClick,
                        onLikeClick = onLikeClick,
                        onReportClick = onReportClick,
                        onDeleteClick = onDeleteClick,
                        onImageClick = onImageClick
                    )
                }
            }
        }

        // Кнопка нового поста живёт только на ленте: в треде снизу поле ввода.
        if (uiState.error == null) {
            NewPostPill(
                onClick = onCreatePostClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }
    }

    if (uiState.isComposerOpen) {
        ForumComposerSheet(
            draft = uiState.composerDraft,
            image = uiState.composerImage,
            canPublish = uiState.canPublish,
            isPublishing = uiState.isPublishing,
            onDraftChange = onComposerDraftChange,
            onImagePicked = onComposerImagePicked,
            onImageRemoved = onComposerImageRemoved,
            onPublish = onCreatePost,
            onDismiss = onComposerDismiss
        )
    }

}

/**
 * Пилюля «+ Новый пост» — как в вебке: градиент из `--primary` в
 * `--primary-light` под углом 135° вместо круглой кнопки-плюса.
 */
@Composable
private fun NewPostPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(listOf(HomeColors.Primary, HomeColors.PrimaryLight))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = HomeColors.OnPrimary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(R.string.forum_create_post),
            style = ForumTypography.NewPostPill,
            color = HomeColors.OnPrimary
        )
    }
}

/**
 * Форма нового поста.
 *
 * [ModalBottomSheet] бесплатно даёт свайп вниз и системный «назад», а
 * `imePadding` поднимает поле над клавиатурой. Счётчик показывает остаток
 * из лимита бэка, поэтому длиннее лимита в поле просто не набрать.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForumComposerSheet(
    draft: String,
    image: ForumImageDraft,
    canPublish: Boolean,
    isPublishing: Boolean,
    onDraftChange: (String) -> Unit,
    onImagePicked: (Uri) -> Unit,
    onImageRemoved: () -> Unit,
    onPublish: () -> Unit,
    onDismiss: () -> Unit
) {
    BlurShellWhileVisible()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onImagePicked) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KampusColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.forum_create_post),
                style = ScheduleTypography.ScreenTitle,
                color = KampusColors.TextMain
            )

            OutlinedTextField(
                value = draft,
                onValueChange = { text -> onDraftChange(text.take(FORUM_CONTENT_MAX_LENGTH)) },
                shape = RoundedCornerShape(18.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.forum_create_post_hint),
                        style = ForumTypography.Body,
                        color = HomeColors.TextMuted
                    )
                },
                textStyle = ForumTypography.Body,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HomeColors.Primary,
                    unfocusedBorderColor = HomeColors.Border,
                    focusedTextColor = HomeColors.TextMain,
                    unfocusedTextColor = HomeColors.TextMain,
                    cursorColor = HomeColors.Primary,
                    focusedContainerColor = KampusColors.Surface,
                    unfocusedContainerColor = KampusColors.Surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ImageAttachButton(
                    // Вложение всегда одно: пока картинка выбрана, кнопка выключена.
                    enabled = !image.hasImage && !isPublishing,
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                if (image.hasImage) {
                    ImageAttachPreview(image = image, onRemove = onImageRemoved)
                }

                Text(
                    text = stringResource(
                        R.string.forum_create_post_counter,
                        draft.length,
                        FORUM_CONTENT_MAX_LENGTH
                    ),
                    style = ForumTypography.MetaCounter,
                    color = HomeColors.TextMuted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onPublish,
                enabled = canPublish,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeColors.Primary,
                    contentColor = HomeColors.OnPrimary,
                    disabledContainerColor = HomeColors.PrimarySoft,
                    disabledContentColor = HomeColors.TextMuted
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        color = HomeColors.OnPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.forum_create_post_submit),
                        style = HomeTypography.AllPosts
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumFeed(
    uiState: ForumUiState,
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onImageClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    // Догрузка страницы: срабатывает, когда до конца списка осталось меньше
    // трёх карточек. snapshotFlow + distinctUntilChanged, чтобы триггер не
    // стрелял на каждый пиксель скролла — тот же приём, что в расписании.
    LaunchedEffect(listState, uiState.posts.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val total = listState.layoutInfo.totalItemsCount
                if (lastVisible != null && total > 0 && lastVisible >= total - 3) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(uiState.posts, key = { post -> post.id }) { post ->
            ThreadCard(
                post = post,
                timeAgo = forumTimeAgo(post.createdAtIso),
                onClick = { onPostClick(post.id) },
                onLikeClick = { onLikeClick(post.id) },
                onReportClick = { onReportClick(post.id) },
                onDeleteClick = { onDeleteClick(post.id) },
                onImageClick = { post.imageUrl?.let(onImageClick) },
                // Свой пост появляется сверху, удалённый уходит, остальные
                // разъезжаются: без этого лента дёргается при каждом действии.
                modifier = Modifier.animateItem()
            )
        }

        if (uiState.isLoadingMore) {
            item(key = "forum-loading-more") {
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

/** Шапка вкладки — тот же заголовок, что у расписания. */
/** Наезд треда на ленту: чуть длиннее смены вкладки — путь длиннее. */
private const val THREAD_ENTER_MS = 340

/** Возврат к ленте. Как и везде, выход короче входа. */
private const val THREAD_EXIT_MS = 220

@Composable
private fun ForumHeader(
    unreadNotificationsCount: Int,
    moderationCount: Int,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.forum_title),
            style = ScheduleTypography.ScreenTitle,
            color = KampusColors.TextMain,
            modifier = Modifier.weight(1f)
        )
        // Как в вебке (`Forum.tsx`): заголовок слева, колокольчик справа.
        NotificationsBell(
            unreadCount = unreadNotificationsCount,
            moderationCount = moderationCount,
            onClick = onOpenNotifications
        )
    }
}

/**
 * Скелетон ленты на всю доступную высоту: считаем, сколько карточек влезает,
 * и рисуем на одну больше, чтобы низ экрана не оставался пустым.
 */
@Composable
internal fun ForumFeedSkeleton(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val count = ((maxHeight - 32.dp) / SKELETON_CARD_HEIGHT).toInt() + 1
        BuzzListSkeleton(
            modifier = Modifier.padding(16.dp),
            count = count.coerceAtLeast(2)
        )
    }
}

/** Высота карточки скелетона вместе с отступом между карточками. */
private val SKELETON_CARD_HEIGHT = 130.dp

/** Ошибка первой загрузки с повтором — вместо бесконечного скелетона. */
@Composable
internal fun ForumErrorBlock(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
internal fun ForumConfirmationDialog(
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
private fun ForumContentPreview() {
    YsukampusTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(KampusColors.AppBackground)) {
            ForumContent(
                uiState = ForumUiState(
                    posts = listOf(
                        ForumPost(
                            id = "1",
                            profileId = "p1",
                            displayName = "Эмиль",
                            displayColor = "#3B82F6",
                            avatarImageUrl = null,
                            visibleRole = "Староста",
                            isMine = true,
                            content = "Кто-нибудь знает, будет ли пара по матанализу?",
                            status = ForumPostStatus.Active,
                            likesCount = 12,
                            commentsCount = 3,
                            likedByMe = true,
                            createdAtIso = "2026-09-02T09:00:00Z"
                        ),
                        ForumPost(
                            id = "2",
                            profileId = "p2",
                            displayName = "Аноним",
                            displayColor = "#94A3B8",
                            avatarImageUrl = null,
                            visibleRole = null,
                            isMine = false,
                            content = "В столовой снова нет сдачи",
                            status = ForumPostStatus.Active,
                            likesCount = 4,
                            commentsCount = 0,
                            likedByMe = false,
                            createdAtIso = "2026-09-01T18:20:00Z"
                        )
                    ),
                    isLoading = false
                ),
                onRetry = {},
                onLoadMore = {},
                onLikeClick = {},
                onReportClick = {},
                onDeleteClick = {},
                onPostClick = {},
                onCreatePostClick = {},
                onComposerDismiss = {},
                onComposerDraftChange = {},
                onComposerImagePicked = {},
                onComposerImageRemoved = {},
                onCreatePost = {},
                onImageClick = {}
            )
        }
    }
}
