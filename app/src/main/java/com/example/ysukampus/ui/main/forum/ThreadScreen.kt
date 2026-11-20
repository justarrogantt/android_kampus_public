package com.example.ysukampus.ui.main.forum

import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.forum.components.CommentCard
import com.example.ysukampus.ui.main.forum.components.ImageAttachButton
import com.example.ysukampus.ui.main.forum.components.ImageAttachPreview
import com.example.ysukampus.ui.main.forum.components.ThreadCard
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.theme.ForumTypography
import com.example.ysukampus.ui.theme.HomeTypography
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

/**
 * Экран треда: пост и его комментарии.
 *
 * Отдельного маршрута у него нет — вкладка «Форум» рисует его вместо ленты,
 * пока в состоянии заполнен `openedPostId`. Комментарии листаются курсором
 * тем же приёмом, что и лента, а поле ввода снизу поднимается над клавиатурой.
 */
@Composable
fun ThreadScreen(
    state: ForumThreadUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onLikeClick: (String) -> Unit,
    onPostReportClick: (String) -> Unit,
    onPostDeleteClick: (String) -> Unit,
    onDraftChange: (String) -> Unit,
    onImagePicked: (Uri) -> Unit,
    onImageRemoved: () -> Unit,
    onSendComment: () -> Unit,
    onImageClick: (String) -> Unit,
    onCommentReportClick: (String) -> Unit,
    onCommentDeleteClick: (String) -> Unit,
    onDismissCommentConfirmation: () -> Unit,
    onConfirmCommentReport: () -> Unit,
    onConfirmCommentDelete: () -> Unit,
    onFocusCommentHandled: () -> Unit = {},
    /** Тап по автору поста или комментария — открывает его публичный профиль. */
    onAuthorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ThreadHeader(onBack = onBack)

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.error != null && state.post == null -> ForumErrorBlock(
                    message = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(16.dp)
                )

                state.isLoading && state.post == null -> ForumFeedSkeleton()

                else -> ThreadList(
                    state = state,
                    onLoadMore = onLoadMore,
                    onFocusCommentHandled = onFocusCommentHandled,
                    onLikeClick = onLikeClick,
                    onPostReportClick = onPostReportClick,
                    onPostDeleteClick = onPostDeleteClick,
                    onCommentReportClick = onCommentReportClick,
                    onCommentDeleteClick = onCommentDeleteClick,
                    onImageClick = onImageClick,
                    onAuthorClick = onAuthorClick
                )
            }
        }

        CommentComposer(
            draft = state.draft,
            limitSignal = state.draftLimitSignal,
            image = state.image,
            canSend = state.canSend,
            isSending = state.isSending,
            onDraftChange = onDraftChange,
            onImagePicked = onImagePicked,
            onImageRemoved = onImageRemoved,
            onSend = onSendComment
        )
    }

    if (state.reportCommentId != null) {
        ForumConfirmationDialog(
            title = stringResource(R.string.forum_comment_report_title),
            text = stringResource(R.string.forum_report_text),
            confirmLabel = stringResource(R.string.forum_report),
            onConfirm = onConfirmCommentReport,
            onDismiss = onDismissCommentConfirmation
        )
    }

    if (state.deleteCommentId != null) {
        ForumConfirmationDialog(
            title = stringResource(R.string.forum_comment_delete_title),
            text = stringResource(R.string.forum_comment_delete_text),
            confirmLabel = stringResource(R.string.forum_delete),
            onConfirm = onConfirmCommentDelete,
            onDismiss = onDismissCommentConfirmation
        )
    }
}

@Composable
private fun ThreadList(
    state: ForumThreadUiState,
    onLoadMore: () -> Unit,
    onFocusCommentHandled: () -> Unit,
    onLikeClick: (String) -> Unit,
    onPostReportClick: (String) -> Unit,
    onPostDeleteClick: (String) -> Unit,
    onCommentReportClick: (String) -> Unit,
    onCommentDeleteClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    // Догрузка комментариев — тот же приём, что в ленте: snapshotFlow по
    // последнему видимому элементу и distinctUntilChanged, чтобы триггер не
    // стрелял на каждый пиксель скролла.
    LaunchedEffect(listState, state.comments.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val total = listState.layoutInfo.totalItemsCount
                if (lastVisible != null && total > 0 && lastVisible >= total - 3) {
                    onLoadMore()
                }
            }
    }

    // Тред открыт из уведомления: прокручиваем к нужному комментарию и держим
    // подсветку, пока модель не погасит запрос.
    LaunchedEffect(state.focusCommentId, state.comments) {
        val focusCommentId = state.focusCommentId ?: return@LaunchedEffect
        val commentIndex = state.comments.indexOfFirst { it.id == focusCommentId }
        if (commentIndex < 0) return@LaunchedEffect

        // Перед комментариями в списке идут пост и заголовок «Комментарии».
        val headerCount = if (state.post != null) 2 else 1
        listState.animateScrollToItem(commentIndex + headerCount)
        delay(FOCUS_HIGHLIGHT_MS)
        onFocusCommentHandled()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        state.post?.let { post ->
            item(key = "thread-post") {
                ThreadCard(
                    post = post,
                    timeAgo = forumTimeAgo(post.createdAtIso),
                    // Тред уже открыт — карточке некуда вести.
                    onClick = {},
                    onLikeClick = { onLikeClick(post.id) },
                    onReportClick = { onPostReportClick(post.id) },
                    onDeleteClick = { onPostDeleteClick(post.id) },
                    onImageClick = { post.imageUrl?.let(onImageClick) },
                    onAuthorClick = post.profileId?.let { id -> { onAuthorClick(id) } }
                )
            }
        }

        item(key = "thread-comments-title") {
            Text(
                text = stringResource(R.string.forum_thread_comments_title),
                style = HomeTypography.AllPosts,
                color = HomeColors.TextAccent,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (state.isEmpty) {
            item(key = "thread-comments-empty") {
                Text(
                    text = stringResource(R.string.forum_thread_empty),
                    style = ForumTypography.Body,
                    color = HomeColors.TextMuted
                )
            }
        }

        items(state.comments, key = { comment -> comment.id }) { comment ->
            CommentCard(
                comment = comment,
                timeAgo = forumTimeAgo(comment.createdAtIso),
                onReportClick = { onCommentReportClick(comment.id) },
                onDeleteClick = { onCommentDeleteClick(comment.id) },
                onImageClick = { comment.imageUrl?.let(onImageClick) },
                isHighlighted = comment.id == state.focusCommentId,
                onAuthorClick = comment.profileId?.let { id -> { onAuthorClick(id) } }
            )
        }

        if (state.isLoadingMore) {
            item(key = "thread-loading-more") {
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

/** Сколько держится подсветка комментария из уведомления — как в вебке. */
private const val FOCUS_HIGHLIGHT_MS = 3_000L

/** Шапка треда: кнопка назад и заголовок. */
@Composable
private fun ThreadHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.forum_back),
                tint = KampusColors.TextMain,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = stringResource(R.string.forum_thread_title),
            style = ScheduleTypography.ScreenTitle,
            color = KampusColors.TextMain
        )
    }
}

/**
 * Поле ввода комментария.
 *
 * Отступ снизу — максимум из инсета клавиатуры и системной навигации: над
 * закрытой клавиатурой поле стоит над навигацией, над открытой — вплотную
 * к ней.
 */
@Composable
private fun CommentComposer(
    draft: String,
    limitSignal: Int,
    image: ForumImageDraft,
    canSend: Boolean,
    isSending: Boolean,
    onDraftChange: (String) -> Unit,
    onImagePicked: (Uri) -> Unit,
    onImageRemoved: () -> Unit,
    onSend: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onImagePicked) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KampusColors.AppBackground)
            // `union` берёт больший из отступов: под открытой клавиатурой её
            // инсет уже включает область навигации, и складывать их нельзя —
            // поле повисло бы над клавиатурой с лишним зазором.
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Превью выбранной картинки стоит над полем: в одну строку с ним оно
        // прижимало бы ввод к краю экрана.
        if (image.hasImage) {
            ImageAttachPreview(image = image, onRemove = onImageRemoved)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ImageAttachButton(
                enabled = !image.hasImage && !isSending,
                onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                shape = RoundedCornerShape(18.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.forum_comment_hint),
                        style = ForumTypography.Body,
                        color = HomeColors.TextMuted
                    )
                },
                textStyle = ForumTypography.Body,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HomeColors.Primary,
                    unfocusedBorderColor = HomeColors.Border,
                    focusedTextColor = HomeColors.TextMain,
                    unfocusedTextColor = HomeColors.TextMain,
                    cursorColor = HomeColors.Primary,
                    focusedContainerColor = KampusColors.AppBackground,
                    unfocusedContainerColor = KampusColors.AppBackground
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) HomeColors.Primary else HomeColors.PrimarySoft)
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        color = HomeColors.Primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.forum_comment_send),
                        tint = if (canSend) HomeColors.OnPrimary else HomeColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Счётчик появляется только на подходе к пределу: в шторке нового поста
        // он висит всегда, но здесь это одна строка поверх ленты комментариев,
        // и держать её ради пустого запаса незачем.
        if (draft.length >= FORUM_CONTENT_MAX_LENGTH - COMMENT_COUNTER_THRESHOLD) {
            CommentCounter(length = draft.length, limitSignal = limitSignal)
        }
    }
}

/**
 * Счётчик символов в комментарии.
 *
 * Цвет перетекает из приглушённого в красный по мере расхода последних
 * [COMMENT_COUNTER_THRESHOLD] символов, а на каждой попытке набрать лишний
 * символ счётчик вздрагивает и отдаёт вибрацией: ввод там обрывается молча, и
 * без отклика это читается как залипшая клавиатура.
 */
@Composable
private fun CommentCounter(length: Int, limitSignal: Int) {
    val counterStart = FORUM_CONTENT_MAX_LENGTH - COMMENT_COUNTER_THRESHOLD
    val progress = ((length - counterStart).toFloat() / COMMENT_COUNTER_THRESHOLD)
        .coerceIn(0f, 1f)

    val vibrate = rememberLimitVibration()
    val shake = remember { Animatable(0f) }
    val shakeDistance = with(LocalDensity.current) { COMMENT_COUNTER_SHAKE_DISTANCE.toPx() }

    // Ключ — счётчик упоров из модели: каждая попытка набрать лишний символ
    // приходит отдельным значением, поэтому отклик повторяется, хотя сам
    // черновик уже не меняется.
    LaunchedEffect(limitSignal) {
        if (limitSignal == 0) return@LaunchedEffect
        vibrate()
        shake.snapTo(0f)
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = COMMENT_COUNTER_SHAKE_MS
                0f at 0
                1f at 50
                -1f at 100
                0.6f at 150
                -0.4f at 200
                0f at COMMENT_COUNTER_SHAKE_MS
            }
        )
    }

    Text(
        text = stringResource(
            R.string.forum_create_post_counter,
            length,
            FORUM_CONTENT_MAX_LENGTH
        ),
        style = ForumTypography.MetaCounter,
        color = lerp(HomeColors.TextMuted, HomeColors.Danger, progress),
        textAlign = TextAlign.End,
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(x = (shake.value * shakeDistance).roundToInt(), y = 0) }
    )
}

/** За сколько символов до предела показывать счётчик в комментарии. */
private const val COMMENT_COUNTER_THRESHOLD = 50

/**
 * Вибрация на упоре в предел длины.
 *
 * Собственный `VibrationEffect`, а не `LocalHapticFeedback`: системный
 * короткий щелчок теряется на фоне отдачи самой клавиатуры. Двойной толчок на
 * полной амплитуде читается как «дальше нельзя», а не как ещё одна нажатая
 * буква.
 */
@Composable
private fun rememberLimitVibration(): () -> Unit {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    return remember(vibrator) {
        {
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        COMMENT_LIMIT_VIBRATION_PATTERN,
                        COMMENT_LIMIT_VIBRATION_AMPLITUDES,
                        // -1: без повторов.
                        -1
                    )
                )
            }
        }
    }
}

/** Размах и длительность тряски счётчика на пределе. */
private val COMMENT_COUNTER_SHAKE_DISTANCE = 6.dp
private const val COMMENT_COUNTER_SHAKE_MS = 250

/** Пауза-толчок-пауза-толчок, оба на полной амплитуде. */
private val COMMENT_LIMIT_VIBRATION_PATTERN = longArrayOf(0, 35, 55, 55)
private val COMMENT_LIMIT_VIBRATION_AMPLITUDES = intArrayOf(0, 255, 0, 255)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ThreadScreenPreview() {
    YsukampusTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(KampusColors.AppBackground)) {
            ThreadScreen(
                state = ForumThreadUiState(
                    post = ForumPost(
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
                        commentsCount = 2,
                        likedByMe = true,
                        createdAtIso = "2026-09-02T09:00:00Z"
                    ),
                    comments = listOf(
                        ForumComment(
                            id = "c1",
                            postId = "1",
                            profileId = "p2",
                            displayName = "Аноним",
                            displayColor = "#94A3B8",
                            avatarImageUrl = null,
                            visibleRole = "Студент",
                            isMine = false,
                            isPostAuthor = false,
                            content = "Будет, расписание не меняли.",
                            parentId = null,
                            status = ForumPostStatus.Active,
                            likesCount = 0,
                            repliesCount = 0,
                            likedByMe = false,
                            createdAtIso = "2026-09-02T09:20:00Z"
                        ),
                        ForumComment(
                            id = "c2",
                            postId = "1",
                            profileId = "p1",
                            displayName = "Эмиль",
                            displayColor = "#3B82F6",
                            avatarImageUrl = null,
                            visibleRole = "Староста",
                            isMine = true,
                            isPostAuthor = true,
                            content = "Спасибо!",
                            parentId = "c1",
                            status = ForumPostStatus.Active,
                            likesCount = 0,
                            repliesCount = 0,
                            likedByMe = false,
                            createdAtIso = "2026-09-02T09:25:00Z"
                        )
                    ),
                    isLoading = false,
                    draft = "А аудитория та же?"
                ),
                onBack = {},
                onRetry = {},
                onLoadMore = {},
                onLikeClick = {},
                onPostReportClick = {},
                onPostDeleteClick = {},
                onDraftChange = {},
                onImagePicked = {},
                onImageRemoved = {},
                onSendComment = {},
                onImageClick = {},
                onCommentReportClick = {},
                onCommentDeleteClick = {},
                onDismissCommentConfirmation = {},
                onConfirmCommentReport = {},
                onConfirmCommentDelete = {}
            )
        }
    }
}
