package com.example.ysukampus.ui.main.profile.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.auth.ProfileRepository
import com.example.ysukampus.data.forum.ForumRepository
import com.example.ysukampus.data.forum.ForumUserProfile
import com.example.ysukampus.data.session.CurrentUserStore
import com.example.ysukampus.data.session.TokenStorage
import com.example.ysukampus.data.session.currentUserId
import com.example.ysukampus.ui.main.forum.ForumActivityComment
import com.example.ysukampus.ui.main.forum.ForumComment
import com.example.ysukampus.ui.main.forum.ForumPost
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Шторка «Профиль»: своя карточка на форуме и последняя активность.
 *
 * Повторяет `UserProfileOverlay` вебки, только всегда про себя: шапка приходит
 * из `forum/user-profile`, посты и комментарии — из `forum/user-posts` и
 * `forum/user-comments` без `profile_id`. Оттуда же взято поведение «постов
 * нет, а комментарии есть — открываем сразу комментарии».
 *
 * `profile_id` для шапки берётся из кэша карточки пользователя, а если сессия
 * старше этого поля — перечитывается через `GET ref/user/{id}`.
 */
class MyProfileViewModel(
    private val forumRepository: ForumRepository,
    private val profileRepository: ProfileRepository,
    private val currentUserStore: CurrentUserStore,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init {
        loadFirstPages()
    }

    fun onRetry() {
        loadFirstPages()
    }

    fun onTabSelected(tab: MyProfileTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /** Список докрутился до конца — тянем следующую страницу активной вкладки. */
    fun onLoadMore() {
        when (_uiState.value.tab) {
            MyProfileTab.Posts -> loadMorePosts()
            MyProfileTab.Comments -> loadMoreComments()
        }
    }

    /** Лайк переключается оптимистично, ответ сервера уточняет счётчик. */
    fun onPostLikeClick(postId: String) {
        val current = _uiState.value.posts.firstOrNull { it.id == postId } ?: return
        updatePost(postId) { post ->
            post.copy(
                likedByMe = !post.likedByMe,
                likesCount = (post.likesCount + if (post.likedByMe) -1 else 1).coerceAtLeast(0)
            )
        }

        viewModelScope.launch {
            forumRepository.toggleLike(postId)
                .onSuccess { like ->
                    updatePost(postId) { it.copy(likedByMe = like.liked, likesCount = like.count) }
                }
                .onFailure { error ->
                    updatePost(postId) {
                        it.copy(likedByMe = current.likedByMe, likesCount = current.likesCount)
                    }
                    _uiState.update { it.copy(message = error.message) }
                }
        }
    }

    fun onCommentLikeClick(commentId: String) {
        val current = _uiState.value.comments
            .firstOrNull { it.comment.id == commentId }
            ?.comment
            ?: return
        updateComment(commentId) { comment ->
            comment.copy(
                likedByMe = !comment.likedByMe,
                likesCount = (comment.likesCount + if (comment.likedByMe) -1 else 1)
                    .coerceAtLeast(0)
            )
        }

        viewModelScope.launch {
            forumRepository.toggleCommentLike(commentId)
                .onSuccess { like ->
                    updateComment(commentId) {
                        it.copy(likedByMe = like.liked, likesCount = like.count)
                    }
                }
                .onFailure { error ->
                    updateComment(commentId) {
                        it.copy(likedByMe = current.likedByMe, likesCount = current.likesCount)
                    }
                    _uiState.update { it.copy(message = error.message) }
                }
        }
    }

    fun onDeletePostClick(postId: String) {
        _uiState.update { it.copy(deletePostId = postId) }
    }

    fun onDeleteCommentClick(commentId: String) {
        _uiState.update { it.copy(deleteCommentId = commentId) }
    }

    fun onDismissConfirmation() {
        _uiState.update { it.copy(deletePostId = null, deleteCommentId = null) }
    }

    fun onConfirmDeletePost() {
        val postId = _uiState.value.deletePostId ?: return
        _uiState.update { it.copy(deletePostId = null) }

        viewModelScope.launch {
            forumRepository.deletePost(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.filterNot { it.id == postId },
                            // Счётчик в шапке пересчитывать некому: правим сами.
                            profile = state.profile?.let { profile ->
                                profile.copy(postsCount = (profile.postsCount - 1).coerceAtLeast(0))
                            },
                            message = DELETE_DONE_MESSAGE
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
        }
    }

    fun onConfirmDeleteComment() {
        val commentId = _uiState.value.deleteCommentId ?: return
        _uiState.update { it.copy(deleteCommentId = null) }

        viewModelScope.launch {
            forumRepository.deleteComment(commentId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            comments = state.comments.filterNot { it.comment.id == commentId },
                            profile = state.profile?.let { profile ->
                                profile.copy(
                                    commentsCount = (profile.commentsCount - 1).coerceAtLeast(0)
                                )
                            },
                            message = COMMENT_DELETE_DONE_MESSAGE
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
        }
    }

    private fun loadFirstPages() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val postsRequest = async { forumRepository.getMyPosts(limit = PAGE_SIZE) }
            val commentsRequest = async { forumRepository.getMyComments(limit = PAGE_SIZE) }
            val profileRequest = async { loadProfileHeader() }

            val postsPage = postsRequest.await()
            val commentsPage = commentsRequest.await()
            val profile = profileRequest.await()

            val error = postsPage.exceptionOrNull() ?: commentsPage.exceptionOrNull()
            if (error != null) {
                _uiState.update { it.copy(isLoading = false, error = error.message) }
                return@launch
            }

            val posts = postsPage.getOrThrow()
            val comments = commentsPage.getOrThrow()

            _uiState.update { state ->
                state.copy(
                    // Шапка необязательна: без неё видна активность, а не ошибка.
                    profile = profile ?: state.profile,
                    posts = posts.posts,
                    comments = comments.comments,
                    postsCursor = posts.nextCursor,
                    commentsCursor = comments.nextCursor,
                    // Постов нет, а комментарии есть — открываем сразу их, как в вебке.
                    tab = if (posts.posts.isEmpty() && comments.comments.isNotEmpty()) {
                        MyProfileTab.Comments
                    } else {
                        state.tab
                    },
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /** @return шапку профиля либо `null`, если `profile_id` или сама ручка не дались. */
    private suspend fun loadProfileHeader(): ForumUserProfile? {
        val profileId = resolveProfileId() ?: return null
        return forumRepository.getUserProfile(profileId).getOrNull()
    }

    private suspend fun resolveProfileId(): String? {
        val cached = currentUserStore.get()
        cached?.profileId?.let { return it }

        // Сессия старше поля `profile_id` в кэше — перечитываем карточку.
        val userId = cached?.id ?: tokenStorage.currentUserId() ?: return null
        return profileRepository.getUser(userId).getOrNull()?.profileId
    }

    private fun loadMorePosts() {
        val cursor = _uiState.value.postsCursor ?: return
        if (!_uiState.value.canLoadMorePosts) return

        _uiState.update { it.copy(isLoadingMorePosts = true) }

        viewModelScope.launch {
            forumRepository.getMyPosts(limit = PAGE_SIZE, cursor = cursor)
                .onSuccess { page ->
                    _uiState.update { state ->
                        state.copy(
                            // Активность живая: тот же пост мог приехать дважды,
                            // а дубликаты сломали бы ключи LazyColumn.
                            posts = state.posts + page.posts.filterNot { new ->
                                state.posts.any { it.id == new.id }
                            },
                            postsCursor = page.nextCursor,
                            isLoadingMorePosts = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingMorePosts = false, message = error.message) }
                }
        }
    }

    private fun loadMoreComments() {
        val cursor = _uiState.value.commentsCursor ?: return
        if (!_uiState.value.canLoadMoreComments) return

        _uiState.update { it.copy(isLoadingMoreComments = true) }

        viewModelScope.launch {
            forumRepository.getMyComments(limit = PAGE_SIZE, cursor = cursor)
                .onSuccess { page ->
                    _uiState.update { state ->
                        state.copy(
                            comments = state.comments + page.comments.filterNot { new ->
                                state.comments.any { it.comment.id == new.comment.id }
                            },
                            commentsCursor = page.nextCursor,
                            isLoadingMoreComments = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoadingMoreComments = false, message = error.message)
                    }
                }
        }
    }

    private fun updatePost(postId: String, transform: (ForumPost) -> ForumPost) {
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { post -> if (post.id == postId) transform(post) else post }
            )
        }
    }

    private fun updateComment(commentId: String, transform: (ForumComment) -> ForumComment) {
        _uiState.update { state ->
            state.copy(
                comments = state.comments.map { activity: ForumActivityComment ->
                    if (activity.comment.id == commentId) {
                        activity.copy(comment = transform(activity.comment))
                    } else {
                        activity
                    }
                }
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 10
        const val DELETE_DONE_MESSAGE = "Пост удален"
        const val COMMENT_DELETE_DONE_MESSAGE = "Комментарий удален"
    }
}
