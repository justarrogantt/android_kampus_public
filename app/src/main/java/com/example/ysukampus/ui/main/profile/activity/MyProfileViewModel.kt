package com.example.ysukampus.ui.main.profile.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.auth.ProfilePhotoDownloader
import com.example.ysukampus.data.auth.ProfileRepository
import com.example.ysukampus.data.forum.ForumRepository
import com.example.ysukampus.data.forum.ForumActivityCommentsPage
import com.example.ysukampus.data.forum.ForumFeedPage
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
 * Шторка «Профиль»: карточка пользователя на форуме и его последняя активность.
 *
 * Повторяет `UserProfileOverlay` вебки и обслуживает оба входа сразу: кнопку
 * «Открыть» про самого себя ([targetProfileId] = `null`, тогда шапка и
 * активность едут без `profile_id` — `forum/user-profile`, `forum/user-posts`,
 * `forum/user-comments`) и тап по автору чужого поста или комментария
 * ([targetProfileId] задан явно, те же ручки идут с ним). Оттуда же взято
 * поведение «постов нет, а комментарии есть — открываем сразу комментарии».
 *
 * `profile_id` для своей шапки берётся из кэша карточки пользователя, а если
 * сессия старше этого поля — перечитывается через `GET ref/user/{id}`. Для
 * чужого профиля он уже известен вызывающему (id автора приходит вместе с
 * постом или комментарием), поэтому такого резолва не требуется.
 */
class MyProfileViewModel(
    private val forumRepository: ForumRepository,
    private val profileRepository: ProfileRepository,
    private val currentUserStore: CurrentUserStore,
    private val tokenStorage: TokenStorage,
    private val photoDownloader: ProfilePhotoDownloader,
    /** `null` — про себя, иначе `profile_id` пользователя, чей профиль открыт. */
    private val targetProfileId: String? = null
) : ViewModel() {

    private val isOwnProfile: Boolean get() = targetProfileId == null

    private val _uiState = MutableStateFlow(MyProfileUiState(isOwnProfile = isOwnProfile))
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

    fun onReportPostClick(postId: String) {
        _uiState.update { it.copy(reportPostId = postId) }
    }

    fun onReportCommentClick(commentId: String) {
        _uiState.update { it.copy(reportCommentId = commentId) }
    }

    fun onDeletePostClick(postId: String) {
        _uiState.update { it.copy(deletePostId = postId) }
    }

    fun onDeleteCommentClick(commentId: String) {
        _uiState.update { it.copy(deleteCommentId = commentId) }
    }

    fun onDismissConfirmation() {
        _uiState.update {
            it.copy(
                reportPostId = null,
                reportCommentId = null,
                deletePostId = null,
                deleteCommentId = null
            )
        }
    }

    fun onConfirmReportPost() {
        val postId = _uiState.value.reportPostId ?: return
        _uiState.update { it.copy(reportPostId = null) }

        viewModelScope.launch {
            val message = forumRepository.reportPost(postId)
                .fold(onSuccess = { REPORT_SENT_MESSAGE }, onFailure = { it.message })
            _uiState.update { it.copy(message = message) }
        }
    }

    fun onConfirmReportComment() {
        val commentId = _uiState.value.reportCommentId ?: return
        _uiState.update { it.copy(reportCommentId = null) }

        viewModelScope.launch {
            val message = forumRepository.reportComment(commentId)
                .fold(onSuccess = { REPORT_SENT_MESSAGE }, onFailure = { it.message })
            _uiState.update { it.copy(message = message) }
        }
    }

    /** Тап по аватару в шапке — на весь экран открывается, только если он есть. */
    fun onAvatarClick() {
        if (_uiState.value.profile?.avatarImageUrl == null) return
        _uiState.update { it.copy(isAvatarViewerOpen = true) }
    }

    fun onAvatarViewerClose() {
        if (_uiState.value.isAvatarDownloading) return
        _uiState.update { it.copy(isAvatarViewerOpen = false) }
    }

    /** Кнопка просмотрщика не рисуется для чужого профиля — [isOwnProfile] лишь подстраховка. */
    fun onAvatarDownloadClick() {
        if (!isOwnProfile || _uiState.value.isAvatarDownloading) return
        _uiState.update { it.copy(isAvatarDownloading = true) }

        viewModelScope.launch {
            val message = photoDownloader.downloadToGallery()
                .fold(onSuccess = { PHOTO_SAVED_MESSAGE }, onFailure = { it.message })
            _uiState.update { it.copy(isAvatarDownloading = false, message = message) }
        }
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
        viewModelScope.launch {
            // Сначала — прошлое открытие шторки с диска: она рисуется сразу и
            // без скелетона, в том числе когда сети нет вовсе.
            val hasCache = showCachedFirstPages()

            _uiState.update { it.copy(isLoading = !hasCache, error = null) }

            val postsRequest = async { fetchPosts(cursor = null) }
            val commentsRequest = async { fetchComments(cursor = null) }
            val profileRequest = async { loadProfileHeader() }

            val postsPage = postsRequest.await()
            val commentsPage = commentsRequest.await()
            val profile = profileRequest.await()

            val error = postsPage.exceptionOrNull() ?: commentsPage.exceptionOrNull()
            if (error != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // Данные на экране есть, просто не обновились: заглушку
                        // с ошибкой вместо них показывать нельзя.
                        error = if (hasCache) null else error.message
                    )
                }
                return@launch
            }

            applyFirstPages(
                posts = postsPage.getOrThrow(),
                comments = commentsPage.getOrThrow(),
                profile = profile,
                keepCursors = true
            )
        }
    }

    /**
     * Рисует сохранённую на диске активность.
     * @return была ли она там вообще.
     */
    private suspend fun showCachedFirstPages(): Boolean {
        val posts = if (targetProfileId != null) {
            forumRepository.peekCachedUserPosts(targetProfileId)
        } else {
            forumRepository.peekCachedMyPosts()
        }
        val comments = if (targetProfileId != null) {
            forumRepository.peekCachedUserComments(targetProfileId)
        } else {
            forumRepository.peekCachedMyComments()
        }
        if (posts == null && comments == null) return false

        // Только из кэша карточки: перечитывать её по сети здесь нельзя —
        // весь смысл этого шага в том, чтобы нарисовать экран без ожидания.
        val profile = (targetProfileId ?: currentUserStore.get()?.profileId)
            ?.let { forumRepository.peekCachedUserProfile(it) }

        applyFirstPages(
            posts = posts,
            comments = comments,
            profile = profile,
            // Курсор из кэша уже не годится для догрузки: он указывает в ленту
            // на момент прошлого открытия. Настоящие курсоры принесёт сетевой
            // ответ секундой позже, а до тех пор «ещё» просто недоступно.
            keepCursors = false
        )
        return true
    }

    private fun applyFirstPages(
        posts: ForumFeedPage?,
        comments: ForumActivityCommentsPage?,
        profile: ForumUserProfile?,
        keepCursors: Boolean
    ) {
        _uiState.update { state ->
            val postItems = posts?.posts ?: state.posts
            val commentItems = comments?.comments ?: state.comments

            state.copy(
                // Шапка необязательна: без неё видна активность, а не ошибка.
                profile = profile ?: state.profile,
                posts = postItems,
                comments = commentItems,
                postsCursor = if (keepCursors) posts?.nextCursor else null,
                commentsCursor = if (keepCursors) comments?.nextCursor else null,
                // Постов нет, а комментарии есть — открываем сразу их, как в вебке.
                tab = if (postItems.isEmpty() && commentItems.isNotEmpty()) {
                    MyProfileTab.Comments
                } else {
                    state.tab
                },
                isLoading = false,
                error = null
            )
        }
    }

    /** @return шапку профиля либо `null`, если `profile_id` или сама ручка не дались. */
    private suspend fun loadProfileHeader(): ForumUserProfile? {
        val profileId = targetProfileId ?: resolveProfileId() ?: return null
        return forumRepository.getUserProfile(profileId).getOrNull()
    }

    /** Свой `profile_id` — только когда [targetProfileId] не задан явно. */
    private suspend fun resolveProfileId(): String? {
        val cached = currentUserStore.get()
        cached?.profileId?.let { return it }

        // Сессия старше поля `profile_id` в кэше — перечитываем карточку.
        val userId = cached?.id ?: tokenStorage.currentUserId() ?: return null
        return profileRepository.getUser(userId).getOrNull()?.profileId
    }

    private suspend fun fetchPosts(cursor: String?): Result<ForumFeedPage> =
        if (targetProfileId != null) {
            forumRepository.getUserPosts(targetProfileId, limit = PAGE_SIZE, cursor = cursor)
        } else {
            forumRepository.getMyPosts(limit = PAGE_SIZE, cursor = cursor)
        }

    private suspend fun fetchComments(cursor: String?): Result<ForumActivityCommentsPage> =
        if (targetProfileId != null) {
            forumRepository.getUserComments(targetProfileId, limit = PAGE_SIZE, cursor = cursor)
        } else {
            forumRepository.getMyComments(limit = PAGE_SIZE, cursor = cursor)
        }

    private fun loadMorePosts() {
        val cursor = _uiState.value.postsCursor ?: return
        if (!_uiState.value.canLoadMorePosts) return

        _uiState.update { it.copy(isLoadingMorePosts = true) }

        viewModelScope.launch {
            fetchPosts(cursor)
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
            fetchComments(cursor)
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
        const val REPORT_SENT_MESSAGE = "Жалоба отправлена"
        const val PHOTO_SAVED_MESSAGE = "Фото сохранено в галерею"
    }
}
