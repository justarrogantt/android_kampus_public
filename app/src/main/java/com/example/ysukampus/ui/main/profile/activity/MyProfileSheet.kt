package com.example.ysukampus.ui.main.profile.activity

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ysukampus.R
import com.example.ysukampus.data.forum.ForumUserProfile
import com.example.ysukampus.ui.main.BlurShellWhileVisible
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.KampusSheet
import com.example.ysukampus.ui.main.forum.ForumActivityComment
import com.example.ysukampus.ui.main.forum.ForumActivityContext
import com.example.ysukampus.ui.main.forum.ForumComment
import com.example.ysukampus.ui.main.forum.ForumConfirmationDialog
import com.example.ysukampus.ui.main.forum.ForumErrorBlock
import com.example.ysukampus.ui.main.forum.ForumPost
import com.example.ysukampus.ui.main.forum.ForumPostStatus
import com.example.ysukampus.ui.main.forum.components.ThreadCard
import com.example.ysukampus.ui.main.forum.forumTimeAgo
import com.example.ysukampus.ui.main.forum.parseAuthorColor
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.main.home.components.BuzzListSkeleton
import com.example.ysukampus.ui.main.profile.activity.components.ActivityCommentCard
import com.example.ysukampus.ui.theme.ForumTypography
import com.example.ysukampus.ui.theme.HomeTypography
import com.example.ysukampus.ui.theme.ProfileTypography
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Шторка «Профиль» — свой профиль форума с последней активностью.
 *
 * Повторяет `UserProfileOverlay` вебки: шапка (аватар, ник, дата регистрации,
 * счётчики), заголовок «Последняя активность» и две вкладки со своими постами
 * и комментариями. Открывается кнопкой «Открыть» на экране профиля; тап по
 * карточке уводит в тред на вкладке «Форум» ([onOpenThread]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileSheet(
    onDismiss: () -> Unit,
    onOpenThread: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: MyProfileViewModel = viewModel(
        factory = remember(context) { MyProfileViewModelFactory(context) }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BlurShellWhileVisible()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KampusColors.AppBackground
    ) {
        MyProfileContent(
            uiState = uiState,
            onRetry = viewModel::onRetry,
            onTabSelected = viewModel::onTabSelected,
            onLoadMore = viewModel::onLoadMore,
            onPostLikeClick = viewModel::onPostLikeClick,
            onCommentLikeClick = viewModel::onCommentLikeClick,
            onOpenThread = onOpenThread,
            onDeletePostClick = viewModel::onDeletePostClick,
            onDeleteCommentClick = viewModel::onDeleteCommentClick,
            onDismissConfirmation = viewModel::onDismissConfirmation,
            onConfirmDeletePost = viewModel::onConfirmDeletePost,
            onConfirmDeleteComment = viewModel::onConfirmDeleteComment,
            onMessageShown = viewModel::onMessageShown
        )
    }
}

/** Отрисовка без ViewModel — содержимое шторки покрывается @Preview без сети. */
@Composable
fun MyProfileContent(
    uiState: MyProfileUiState,
    onRetry: () -> Unit,
    onTabSelected: (MyProfileTab) -> Unit,
    onLoadMore: () -> Unit,
    onPostLikeClick: (String) -> Unit,
    onCommentLikeClick: (String) -> Unit,
    onOpenThread: (String) -> Unit,
    onDeletePostClick: (String) -> Unit,
    onDeleteCommentClick: (String) -> Unit,
    onDismissConfirmation: () -> Unit,
    onConfirmDeletePost: () -> Unit,
    onConfirmDeleteComment: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val isLoadingMore = when (uiState.tab) {
        MyProfileTab.Posts -> uiState.isLoadingMorePosts
        MyProfileTab.Comments -> uiState.isLoadingMoreComments
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onMessageShown()
    }

    // Догрузка страницы — тот же приём, что в ленте форума: триггер стреляет,
    // когда до конца списка осталось меньше трёх карточек.
    LaunchedEffect(listState, uiState.tab, uiState.posts.size, uiState.comments.size) {
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
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "my-profile-hero") {
                MyProfileHero(profile = uiState.profile)
            }

            item(key = "my-profile-activity-title") {
                Text(
                    text = stringResource(R.string.my_profile_activity_title),
                    style = ScheduleTypography.ScreenTitle,
                    color = KampusColors.TextMain,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item(key = "my-profile-tabs") {
                MyProfileTabs(tab = uiState.tab, onTabSelected = onTabSelected)
            }

            when {
                uiState.error != null -> item(key = "my-profile-error") {
                    ForumErrorBlock(message = uiState.error, onRetry = onRetry)
                }

                uiState.isLoading -> item(key = "my-profile-skeleton") {
                    BuzzListSkeleton()
                }

                uiState.isEmpty -> item(key = "my-profile-empty") {
                    Text(
                        text = stringResource(R.string.my_profile_empty),
                        style = HomeTypography.AllPosts,
                        color = HomeColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }

                uiState.tab == MyProfileTab.Posts ->
                    items(uiState.posts, key = { post -> post.id }) { post ->
                        ThreadCard(
                            post = post,
                            timeAgo = forumTimeAgo(post.createdAtIso),
                            onClick = { onOpenThread(post.id) },
                            onLikeClick = { onPostLikeClick(post.id) },
                            // Свои посты не на что жаловаться — остаётся удаление.
                            onReportClick = { onDeletePostClick(post.id) },
                            onDeleteClick = { onDeletePostClick(post.id) }
                        )
                    }

                else -> items(uiState.comments, key = { it.comment.id }) { activity ->
                    ActivityCommentCard(
                        activity = activity,
                        timeAgo = forumTimeAgo(activity.comment.createdAtIso),
                        onClick = { onOpenThread(activity.comment.postId) },
                        onLikeClick = { onCommentLikeClick(activity.comment.id) },
                        onDeleteClick = { onDeleteCommentClick(activity.comment.id) },
                        onImageClick = { onOpenThread(activity.comment.postId) }
                    )
                }
            }

            if (isLoadingMore) {
                item(key = "my-profile-loading-more") {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = HomeColors.Primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (uiState.deletePostId != null) {
        ForumConfirmationDialog(
            title = stringResource(R.string.forum_delete_title),
            text = stringResource(R.string.forum_delete_text),
            confirmLabel = stringResource(R.string.forum_delete),
            onConfirm = onConfirmDeletePost,
            onDismiss = onDismissConfirmation
        )
    }

    if (uiState.deleteCommentId != null) {
        ForumConfirmationDialog(
            title = stringResource(R.string.forum_comment_delete_title),
            text = stringResource(R.string.forum_comment_delete_text),
            confirmLabel = stringResource(R.string.forum_delete),
            onConfirm = onConfirmDeleteComment,
            onDismiss = onDismissConfirmation
        )
    }
}

/** Шапка: заголовок, аватар, ник, дата регистрации и счётчики активности. */
@Composable
private fun MyProfileHero(profile: ForumUserProfile?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.my_profile_title),
            style = ProfileTypography.ScreenTitle,
            color = KampusColors.TextMain
        )

        Spacer(Modifier.height(18.dp))

        val accent = profile?.displayColor?.let(::parseAuthorColor) ?: HomeColors.Primary
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(HomeColors.PrimarySoft)
                .border(4.dp, accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val avatarUrl = profile?.avatarImageUrl
            if (avatarUrl == null) {
                Icon(
                    painter = painterResource(R.drawable.ic_profile_avatar),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(52.dp)
                )
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(R.string.forum_avatar_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = profile?.displayName.orEmpty(),
            style = ProfileTypography.UserName,
            color = KampusColors.TextMain
        )

        // Пока шапка едет, вместо даты и счётчиков просто пусто: подставлять
        // ноль постов было бы враньём.
        if (profile != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.my_profile_registered,
                    MyProfileDates.formatRegistration(profile.createdAtIso)
                ),
                style = ForumTypography.Body,
                color = HomeColors.TextMuted
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.my_profile_counters,
                    profile.postsCount,
                    pluralPosts(profile.postsCount),
                    profile.commentsCount,
                    pluralComments(profile.commentsCount)
                ),
                style = ProfileTypography.EditButton,
                color = KampusColors.PrimaryBlue
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** Переключатель вкладок с едущей пилюлей — как в вебке. */
@Composable
private fun MyProfileTabs(tab: MyProfileTab, onTabSelected: (MyProfileTab) -> Unit) {
    val pillOffset by animateFloatAsState(
        targetValue = if (tab == MyProfileTab.Comments) 1f else 0f,
        label = "my-profile-tab-pill"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(HomeColors.PrimarySoft)
    ) {
        val halfWidth = maxWidth / 2

        Box(
            modifier = Modifier
                .offset(x = halfWidth * pillOffset)
                .fillMaxHeight()
                .width(halfWidth)
                .padding(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(HomeColors.Primary)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            MyProfileTabButton(
                title = stringResource(R.string.my_profile_tab_posts),
                isActive = tab == MyProfileTab.Posts,
                onClick = { onTabSelected(MyProfileTab.Posts) },
                modifier = Modifier.weight(1f)
            )
            MyProfileTabButton(
                title = stringResource(R.string.my_profile_tab_comments),
                isActive = tab == MyProfileTab.Comments,
                onClick = { onTabSelected(MyProfileTab.Comments) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MyProfileTabButton(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = ProfileTypography.ActivityTab,
            color = if (isActive) HomeColors.OnPrimary else HomeColors.TextAccent
        )
    }
}

/** «6 постов», «3 комментария» — та же таблица окончаний, что и в вебке. */
private fun pluralPosts(count: Int): String = plural(count, "пост", "поста", "постов")

private fun pluralComments(count: Int): String =
    plural(count, "комментарий", "комментария", "комментариев")

private fun plural(count: Int, one: String, few: String, many: String): String {
    val remainder100 = count % 100
    if (remainder100 in 11..14) return many

    return when (count % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MyProfileContentPreview() {
    YsukampusTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(KampusColors.AppBackground)) {
            MyProfileContent(
                uiState = MyProfileUiState(
                    isLoading = false,
                    profile = ForumUserProfile(
                        profileId = "me",
                        displayName = "justarrogant",
                        displayColor = "#3B82F6",
                        avatarImageUrl = null,
                        createdAtIso = "2026-06-14T09:00:00Z",
                        postsCount = 6,
                        commentsCount = 3
                    ),
                    posts = listOf(
                        ForumPost(
                            id = "1",
                            profileId = "me",
                            displayName = "justarrogant",
                            displayColor = "#3B82F6",
                            avatarImageUrl = null,
                            visibleRole = "a4dev",
                            isMine = true,
                            content = "Кто-нибудь знает, будет ли пара по матанализу?",
                            status = ForumPostStatus.Active,
                            likesCount = 3,
                            commentsCount = 1,
                            likedByMe = false,
                            createdAtIso = "2026-08-02T09:00:00Z"
                        )
                    ),
                    comments = listOf(
                        ForumActivityComment(
                            comment = ForumComment(
                                id = "c1",
                                postId = "1",
                                profileId = "me",
                                displayName = "justarrogant",
                                displayColor = "#3B82F6",
                                avatarImageUrl = null,
                                visibleRole = "a4dev",
                                isMine = true,
                                isPostAuthor = false,
                                content = "чд в кд",
                                parentId = null,
                                status = ForumPostStatus.Active,
                                likesCount = 1,
                                repliesCount = 0,
                                likedByMe = false,
                                createdAtIso = "2026-07-02T10:00:00Z"
                            ),
                            postContext = ForumActivityContext(
                                id = "1",
                                displayName = "Onblessedboy",
                                content = "Кд чд братаны"
                            )
                        )
                    )
                ),
                onRetry = {},
                onTabSelected = {},
                onLoadMore = {},
                onPostLikeClick = {},
                onCommentLikeClick = {},
                onOpenThread = {},
                onDeletePostClick = {},
                onDeleteCommentClick = {},
                onDismissConfirmation = {},
                onConfirmDeletePost = {},
                onConfirmDeleteComment = {},
                onMessageShown = {}
            )
        }
    }
}
