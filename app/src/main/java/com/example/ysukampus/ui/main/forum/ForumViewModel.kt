package com.example.ysukampus.ui.main.forum

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.forum.ForumImageDownloader
import com.example.ysukampus.data.forum.ForumPublication
import com.example.ysukampus.data.forum.ForumRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Лента вкладки «Форум» и открытый из неё тред.
 *
 * Действия над постом (лайк, жалоба, удаление) повторяют главную — это тот же
 * репозиторий и те же тексты ошибок. Отличие в пагинации: лента листается
 * курсором из `next_cursor`, а не перезапрашивает фиксированный лимит.
 *
 * Тред живёт в том же состоянии: комментарии запрашиваются только после
 * onPostClick() и сбрасываются вместе с закрытием треда.
 */
class ForumViewModel(
    private val forumRepository: ForumRepository,
    private val imageDownloader: ForumImageDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumUiState())
    val uiState: StateFlow<ForumUiState> = _uiState.asStateFlow()

    /**
     * Текущие загрузки вложений.
     *
     * Пользователь может убрать картинку или выбрать другую, пока предыдущая
     * ещё летит на сервер: ответ на устаревшую загрузку нам не нужен, поэтому
     * задачу отменяем, а не фильтруем её результат.
     */
    private var composerUploadJob: Job? = null
    private var commentUploadJob: Job? = null

    /** Первая страница ленты: одна на все входы во вкладку. */
    private var firstPageJob: Job? = null

    /**
     * Вкладка открыта — грузим ленту, если её ещё нет.
     *
     * Раньше первая страница грузилась в `init`, но ViewModel теперь создаётся
     * вместе с оболочкой: тред живёт над вкладками и должен открываться с
     * главной и из уведомления, не заходя в форум. Грузить ленту тем, кто в
     * форум так и не зашёл, незачем — поэтому загрузку начинает экран.
     */
    fun onScreenShown() {
        if (_uiState.value.posts.isEmpty() && firstPageJob?.isActive != true) loadFirstPage()
    }

    fun onRetry() {
        loadFirstPage()
    }

    /** Список докрутился до конца — тянем следующую страницу. */
    fun onLoadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (!_uiState.value.canLoadMore) return

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            forumRepository.getFeed(limit = PAGE_SIZE, cursor = cursor)
                .onSuccess { page ->
                    _uiState.update { state ->
                        state.copy(
                            // Бэк может отдать пост, который уже пришёл на прошлой
                            // странице (лента живая) — дубликаты сломали бы ключи LazyColumn.
                            posts = state.posts + page.posts.filterNot { new ->
                                state.posts.any { it.id == new.id }
                            },
                            nextCursor = page.nextCursor,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure { error ->
                    // Уже загруженное остаётся на экране: об ошибке догрузки
                    // сообщаем снекбаром, а не пустой лентой.
                    _uiState.update { it.copy(isLoadingMore = false, message = error.message) }
                }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * Лайк переключается оптимистично: счётчик меняется сразу, а ответ сервера
     * его уточняет. При ошибке состояние откатывается на прежнее.
     */
    fun onLikeClick(postId: String) {
        val state = _uiState.value
        val current = state.posts.firstOrNull { it.id == postId }
            ?: state.thread.post?.takeIf { it.id == postId }
            ?: return
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

    fun onReportClick(postId: String) {
        _uiState.update { it.copy(reportPostId = postId) }
    }

    fun onDeleteClick(postId: String) {
        _uiState.update { it.copy(deletePostId = postId) }
    }

    fun onDismissConfirmation() {
        _uiState.update { it.copy(reportPostId = null, deletePostId = null) }
    }

    fun onConfirmReport() {
        val postId = _uiState.value.reportPostId ?: return
        _uiState.update { it.copy(reportPostId = null) }

        viewModelScope.launch {
            val message = forumRepository.reportPost(postId)
                .fold(onSuccess = { REPORT_SENT_MESSAGE }, onFailure = { it.message })
            _uiState.update { it.copy(message = message) }
        }
    }

    fun onConfirmDelete() {
        val postId = _uiState.value.deletePostId ?: return
        _uiState.update { it.copy(deletePostId = null) }

        viewModelScope.launch {
            forumRepository.deletePost(postId)
                .onSuccess {
                    // Ленту не перезапрашиваем: курсор указывает на середину
                    // списка, и повторный первый запрос сбросил бы прокрутку.
                    _uiState.update { state ->
                        // Удалённый пост мог быть открыт в треде — тогда возвращаемся в ленту.
                        val threadClosed = state.openedPostId == postId
                        state.copy(
                            posts = state.posts.filterNot { it.id == postId },
                            openedPostId = state.openedPostId.takeUnless { threadClosed },
                            thread = if (threadClosed) ForumThreadUiState() else state.thread,
                            message = DELETE_DONE_MESSAGE
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(message = error.message) }
                }
        }
    }

    // --- Тред ------------------------------------------------------------

    /** Карточка поста нажата: открываем тред и грузим пост с комментариями. */
    /**
     * @param focusCommentId комментарий, к которому надо прокрутить тред —
     * так открывается уведомление. `null` — обычное открытие из ленты.
     */
    fun onPostClick(postId: String, focusCommentId: String? = null) {
        if (_uiState.value.openedPostId == postId && focusCommentId == null) return

        // Пост из ленты уже есть — показываем его, пока едет запрос.
        val known = _uiState.value.posts.firstOrNull { it.id == postId }
        _uiState.update {
            it.copy(
                openedPostId = postId,
                thread = ForumThreadUiState(post = known, focusCommentId = focusCommentId)
            )
        }
        loadThread(postId)
    }

    /** Тред прокручен к нужному комментарию и подсвечен — запрос гасим. */
    fun onFocusCommentHandled() {
        updateThread { it.copy(focusCommentId = null) }
    }

    /** Системный «назад» или кнопка в шапке: тред закрыт, состояние сброшено. */
    fun onThreadClose() {
        commentUploadJob?.cancel()
        commentUploadJob = null
        _uiState.update { it.copy(openedPostId = null, thread = ForumThreadUiState()) }
    }

    fun onThreadRetry() {
        val postId = _uiState.value.openedPostId ?: return
        loadThread(postId)
    }

    /** Список комментариев докрутился до конца — тянем следующую страницу. */
    fun onThreadLoadMore() {
        val postId = _uiState.value.openedPostId ?: return
        val thread = _uiState.value.thread
        val cursor = thread.nextCursor ?: return
        if (!thread.canLoadMore) return

        updateThread { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            forumRepository.getComments(postId = postId, limit = PAGE_SIZE, cursor = cursor)
                .onSuccess { page ->
                    updateThread { state ->
                        state.copy(
                            // Тред живой: тот же комментарий мог приехать дважды,
                            // а дубликаты сломали бы ключи LazyColumn.
                            comments = state.comments + page.comments.filterNot { new ->
                                state.comments.any { it.id == new.id }
                            },
                            nextCursor = page.nextCursor,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure { error ->
                    updateThread { it.copy(isLoadingMore = false) }
                    _uiState.update { it.copy(message = error.message) }
                }
        }
    }

    fun onCommentDraftChange(draft: String) {
        // Обрезка живёт здесь, а не в композабле: только модель видит исходную
        // длину и может отличить упор в предел от обычного ввода.
        val clamped = draft.take(FORUM_CONTENT_MAX_LENGTH)
        updateThread { thread ->
            thread.copy(
                draft = clamped,
                draftLimitSignal = if (draft.length >= FORUM_CONTENT_MAX_LENGTH) {
                    thread.draftLimitSignal + 1
                } else {
                    thread.draftLimitSignal
                }
            )
        }
    }

    /** Выбрана картинка для комментария — грузим её, пока дописывается текст. */
    fun onCommentImagePicked(uri: Uri) {
        commentUploadJob?.cancel()
        updateThread { it.copy(image = ForumImageDraft(uri = uri, isUploading = true)) }

        commentUploadJob = viewModelScope.launch {
            forumRepository.uploadImage(uri)
                .onSuccess { attachment ->
                    updateThread {
                        it.copy(image = it.image.copy(attachmentId = attachment.id, isUploading = false))
                    }
                }
                .onFailure { error ->
                    // Картинку убираем: прикреплять нечего, а причина видна в снекбаре.
                    updateThread { it.copy(image = ForumImageDraft()) }
                    _uiState.update { it.copy(message = error.message) }
                }
        }
    }

    fun onCommentImageRemoved() {
        commentUploadJob?.cancel()
        commentUploadJob = null
        updateThread { it.copy(image = ForumImageDraft()) }
    }

    fun onSendComment() {
        val postId = _uiState.value.openedPostId ?: return
        val thread = _uiState.value.thread
        if (!thread.canSend) return
        val content = thread.draft.trim()
        val imageId = thread.image.attachmentId

        updateThread { it.copy(isSending = true) }

        viewModelScope.launch {
            forumRepository.addComment(postId = postId, content = content, imageId = imageId)
                .onSuccess { publication ->
                    when (publication) {
                        is ForumPublication.Published -> {
                            // Комментарии приходят от новых к старым — свой ставим в начало.
                            updateThread { state ->
                                state.copy(
                                    comments = listOf(publication.content) + state.comments
                                        .filterNot { it.id == publication.content.id },
                                    draft = "",
                                    image = ForumImageDraft(),
                                    isSending = false
                                )
                            }
                            updatePost(postId) { it.copy(commentsCount = it.commentsCount + 1) }
                            _uiState.update { it.copy(message = COMMENT_SENT_MESSAGE) }
                        }

                        ForumPublication.ModerationRequired -> {
                            updateThread {
                                it.copy(draft = "", image = ForumImageDraft(), isSending = false)
                            }
                            _uiState.update { it.copy(message = COMMENT_MODERATION_MESSAGE) }
                        }
                    }
                }
                .onFailure { error ->
                    // Черновик остаётся в поле: пользователю есть что отправить повторно.
                    updateThread { it.copy(isSending = false) }
                    _uiState.update { it.copy(message = error.message) }
                }
        }
    }

    fun onCommentReportClick(commentId: String) {
        updateThread { it.copy(reportCommentId = commentId) }
    }

    fun onCommentDeleteClick(commentId: String) {
        updateThread { it.copy(deleteCommentId = commentId) }
    }

    fun onDismissCommentConfirmation() {
        updateThread { it.copy(reportCommentId = null, deleteCommentId = null) }
    }

    fun onConfirmCommentReport() {
        val commentId = _uiState.value.thread.reportCommentId ?: return
        updateThread { it.copy(reportCommentId = null) }

        viewModelScope.launch {
            val message = forumRepository.reportComment(commentId)
                .fold(onSuccess = { REPORT_SENT_MESSAGE }, onFailure = { it.message })
            _uiState.update { it.copy(message = message) }
        }
    }

    fun onConfirmCommentDelete() {
        val commentId = _uiState.value.thread.deleteCommentId ?: return
        val postId = _uiState.value.openedPostId
        updateThread { it.copy(deleteCommentId = null) }

        viewModelScope.launch {
            forumRepository.deleteComment(commentId)
                .onSuccess {
                    updateThread { state ->
                        state.copy(comments = state.comments.filterNot { it.id == commentId })
                    }
                    postId?.let { id ->
                        updatePost(id) {
                            it.copy(commentsCount = (it.commentsCount - 1).coerceAtLeast(0))
                        }
                    }
                    _uiState.update { it.copy(message = COMMENT_DELETE_DONE_MESSAGE) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(message = error.message) }
                }
        }
    }

    // --- Новый пост ------------------------------------------------------

    fun onCreatePostClick() {
        _uiState.update { it.copy(isComposerOpen = true) }
    }

    fun onComposerDismiss() {
        if (_uiState.value.isPublishing) return
        composerUploadJob?.cancel()
        composerUploadJob = null
        _uiState.update {
            it.copy(isComposerOpen = false, composerDraft = "", composerImage = ForumImageDraft())
        }
    }

    /** Выбрана картинка для поста — грузим её, пока дописывается текст. */
    fun onComposerImagePicked(uri: Uri) {
        composerUploadJob?.cancel()
        _uiState.update { it.copy(composerImage = ForumImageDraft(uri = uri, isUploading = true)) }

        composerUploadJob = viewModelScope.launch {
            forumRepository.uploadImage(uri)
                .onSuccess { attachment ->
                    _uiState.update { state ->
                        state.copy(
                            composerImage = state.composerImage.copy(
                                attachmentId = attachment.id,
                                isUploading = false
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(composerImage = ForumImageDraft(), message = error.message)
                    }
                }
        }
    }

    fun onComposerImageRemoved() {
        composerUploadJob?.cancel()
        composerUploadJob = null
        _uiState.update { it.copy(composerImage = ForumImageDraft()) }
    }

    fun onComposerDraftChange(draft: String) {
        _uiState.update { it.copy(composerDraft = draft) }
    }

    fun onCreatePost() {
        val state = _uiState.value
        if (!state.canPublish) return
        val content = state.composerDraft.trim()
        val imageId = state.composerImage.attachmentId

        _uiState.update { it.copy(isPublishing = true) }

        viewModelScope.launch {
            forumRepository.createPost(content = content, imageId = imageId)
                .onSuccess { publication ->
                    when (publication) {
                        is ForumPublication.Published -> _uiState.update { current ->
                            current.copy(
                                // Лента отсортирована от новых к старым, поэтому
                                // свежий пост встаёт в начало без перезапроса.
                                posts = listOf(publication.content) + current.posts
                                    .filterNot { it.id == publication.content.id },
                                isComposerOpen = false,
                                composerDraft = "",
                                composerImage = ForumImageDraft(),
                                isPublishing = false,
                                message = POST_CREATED_MESSAGE
                            )
                        }

                        ForumPublication.ModerationRequired -> _uiState.update {
                            it.copy(
                                isComposerOpen = false,
                                composerDraft = "",
                                composerImage = ForumImageDraft(),
                                isPublishing = false,
                                message = POST_MODERATION_MESSAGE
                            )
                        }
                    }
                }
                .onFailure { error ->
                    // Лист остаётся открытым с черновиком — отправку можно повторить.
                    _uiState.update { it.copy(isPublishing = false, message = error.message) }
                }
        }
    }

    // --- Просмотр картинки ------------------------------------------------

    /** Тап по картинке в карточке: открываем оригинал, а не миниатюру. */
    fun onImageClick(imageUrl: String) {
        _uiState.update { it.copy(viewerImageUrl = imageUrl) }
    }

    fun onImageViewerClose() {
        if (_uiState.value.isSavingImage) return
        _uiState.update { it.copy(viewerImageUrl = null) }
    }

    fun onImageDownload() {
        val imageUrl = _uiState.value.viewerImageUrl ?: return
        if (_uiState.value.isSavingImage) return

        _uiState.update { it.copy(isSavingImage = true) }

        viewModelScope.launch {
            val message = imageDownloader.downloadToGallery(imageUrl)
                .fold(onSuccess = { IMAGE_SAVED_MESSAGE }, onFailure = { it.message })
            _uiState.update { it.copy(isSavingImage = false, message = message) }
        }
    }

    /** Пост и первая страница комментариев. Комментарии грузятся только отсюда. */
    private fun loadThread(postId: String) {
        viewModelScope.launch {
            // Тред, открытый раньше, рисуется с диска сразу — в том числе когда
            // в него пришли по уведомлению и ленты на экране не было.
            val hasCache = showCachedThread(postId)

            updateThread { it.copy(isLoading = !hasCache, error = null) }

            val post = forumRepository.getPost(postId)
                .getOrElse { error ->
                    updateThread {
                        it.copy(
                            isLoading = false,
                            // Тред на экране есть, просто не обновился.
                            error = if (hasCache) null else error.message ?: THREAD_ERROR_MESSAGE
                        )
                    }
                    return@launch
                }

            forumRepository.getComments(postId = postId, limit = PAGE_SIZE, cursor = null)
                .onSuccess { page ->
                    updateThread {
                        it.copy(
                            post = post,
                            comments = page.comments,
                            nextCursor = page.nextCursor,
                            isLoading = false
                        )
                    }
                    loadUntilFocusComment(postId)
                }
                .onFailure { error ->
                    updateThread {
                        it.copy(
                            post = post,
                            isLoading = false,
                            error = if (hasCache) {
                                null
                            } else {
                                error.message ?: COMMENTS_ERROR_MESSAGE
                            }
                        )
                    }
                }
        }
    }

    /**
     * Рисует сохранённый на диске тред.
     *
     * Признаком служат именно комментарии: сам пост при открытии из ленты уже
     * лежит в состоянии (см. [onPostClick]), и ждать в треде нечего, кроме них.
     *
     * @return был ли тред в кэше.
     */
    private suspend fun showCachedThread(postId: String): Boolean {
        val comments = forumRepository.peekCachedComments(postId, PAGE_SIZE) ?: return false
        val post = forumRepository.peekCachedPost(postId)

        updateThread { thread ->
            thread.copy(
                post = post ?: thread.post,
                comments = comments.comments,
                // Курсор из кэша указывает в тред на момент прошлого открытия;
                // настоящий принесёт сетевой ответ секундой позже.
                nextCursor = null,
                isLoading = false,
                error = null
            )
        }
        return true
    }

    /**
     * Дотягивает страницы комментариев, пока не найдётся тот, к которому
     * ведёт уведомление.
     *
     * Ограничение по числу страниц намеренное: у длинного треда искомый
     * комментарий может лежать очень глубоко, и тянуть его целиком ради
     * прокрутки дороже, чем показать тред с начала.
     */
    private suspend fun loadUntilFocusComment(postId: String) {
        val focusCommentId = _uiState.value.thread.focusCommentId ?: return

        repeat(FOCUS_COMMENT_MAX_PAGES) {
            val thread = _uiState.value.thread
            if (thread.comments.any { it.id == focusCommentId }) return
            val cursor = thread.nextCursor ?: return

            val page = forumRepository.getComments(
                postId = postId,
                limit = PAGE_SIZE,
                cursor = cursor
            ).getOrElse { return }

            updateThread { state ->
                state.copy(
                    comments = state.comments + page.comments.filterNot { new ->
                        state.comments.any { it.id == new.id }
                    },
                    nextCursor = page.nextCursor
                )
            }
        }
    }

    private fun loadFirstPage() {
        firstPageJob?.cancel()
        firstPageJob = viewModelScope.launch {
            // Сначала — прошлая первая страница с диска: вкладка открывается
            // мгновенно и показывает ленту даже без сети.
            val hasCache = showCachedFirstPage()

            _uiState.update { it.copy(isLoading = !hasCache, error = null) }

            forumRepository.getFeed(limit = PAGE_SIZE, cursor = null)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            posts = page.posts,
                            nextCursor = page.nextCursor,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    // Пустая лента вместо ошибки соврала бы, что постов нет.
                    // Но когда лента уже на экране из кэша, ошибка не нужна:
                    // это всего лишь неудавшееся фоновое обновление.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (hasCache) null else error.message ?: FEED_ERROR_MESSAGE
                        )
                    }
                }
        }
    }

    /**
     * Рисует сохранённую на диске первую страницу ленты.
     * @return была ли она там вообще.
     */
    private suspend fun showCachedFirstPage(): Boolean {
        val page = forumRepository.peekCachedFeed(PAGE_SIZE) ?: return false

        _uiState.update {
            it.copy(
                posts = page.posts,
                // Курсор из кэша уже не годится для догрузки: лента живая, и он
                // указывает в неё на момент прошлого открытия. Настоящий
                // принесёт сетевой ответ секундой позже.
                nextCursor = null,
                isLoading = false,
                error = null
            )
        }
        return true
    }

    /** Пост может быть виден дважды — в ленте и в открытом треде: правим оба. */
    private fun updatePost(postId: String, transform: (ForumPost) -> ForumPost) {
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { post -> if (post.id == postId) transform(post) else post },
                thread = state.thread.copy(
                    post = state.thread.post?.takeIf { it.id == postId }?.let(transform)
                        ?: state.thread.post
                )
            )
        }
    }

    private fun updateThread(transform: (ForumThreadUiState) -> ForumThreadUiState) {
        _uiState.update { it.copy(thread = transform(it.thread)) }
    }

    private companion object {
        /** Размер страницы ленты. Бэк ограничивает лимит сотней. */
        const val PAGE_SIZE = 20

        /** Сколько страниц комментариев готовы дотянуть ради уведомления. */
        const val FOCUS_COMMENT_MAX_PAGES = 5
        const val FEED_ERROR_MESSAGE = "Не удалось загрузить посты"
        const val THREAD_ERROR_MESSAGE = "Не удалось загрузить пост"
        const val COMMENTS_ERROR_MESSAGE = "Не удалось загрузить комментарии"
        const val REPORT_SENT_MESSAGE = "Жалоба отправлена"
        const val DELETE_DONE_MESSAGE = "Пост удален"
        const val COMMENT_DELETE_DONE_MESSAGE = "Комментарий удален"
        const val COMMENT_SENT_MESSAGE = "Комментарий добавлен"
        const val POST_CREATED_MESSAGE = "Пост опубликован"
        const val POST_MODERATION_MESSAGE = "Пост отправлен на проверку модератору"
        const val COMMENT_MODERATION_MESSAGE = "Комментарий отправлен на проверку модератору"
        const val IMAGE_SAVED_MESSAGE = "Картинка сохранена в галерею"
    }
}
