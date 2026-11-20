package com.example.ysukampus.ui.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.forum.ForumRepository
import com.example.ysukampus.data.schedule.ScheduleRepository
import com.example.ysukampus.data.session.CurrentUserStore
import com.example.ysukampus.data.weather.WeatherRepository
import com.example.ysukampus.ui.main.forum.ForumPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Состояние главной.
 *
 * Три источника грузятся параллельно и независимо друг от друга: упавшая ручка
 * показывает ошибку со своей кнопкой «Повторить» и не мешает остальным блокам.
 * Возврат на вкладку ([onScreenShown]) тоже повторяет неудавшиеся запросы — как
 * в вебке, где Home перемонтируется при переходе.
 *
 * Неделя запрашивается без `oid`: ручка расписания с optional auth сама берёт
 * группу владельца токена — тот же приём, что и на вкладке «Расписание».
 */
class HomeViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val weatherRepository: WeatherRepository,
    private val forumRepository: ForumRepository,
    currentUserStore: CurrentUserStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(greetingName = currentUserStore.get()?.forumName.orEmpty())
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadWeek()
        loadWeather()
        loadPosts()
        startClock()
    }

    /**
     * Двигает «сейчас» в состоянии, пока экран жив. Полминуты хватает: обратный
     * отсчёт до пары рисует сама карточка, а здесь важен только момент, когда
     * ближайшая пара сменяется следующей.
     */
    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                delay(CLOCK_TICK_MS)
                _uiState.update { it.copy(nowMinutes = HomeDates.nowMinutes()) }
            }
        }
    }

    /** Вкладка снова открыта — дозагружаем то, что в прошлый раз не пришло. */
    fun onScreenShown() {
        loadMissingSections()
    }

    /** Повтор после ошибки расписания. */
    fun onRetryWeek() {
        loadWeek()
    }

    /** Повтор после ошибки ленты форума. */
    fun onRetryPosts() {
        loadPosts()
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * Лайк переключается оптимистично: счётчик меняется сразу, а ответ сервера
     * его уточняет. При ошибке состояние откатывается на прежнее.
     */
    fun onLikeClick(postId: String) {
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
                    // Удалённый пост убираем из ленты и подтягиваем следующий,
                    // чтобы на главной снова было два поста.
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.filterNot { it.id == postId },
                            message = DELETE_DONE_MESSAGE
                        )
                    }
                    loadPosts()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(message = error.message) }
                }
        }
    }

    private fun loadMissingSections() {
        val state = _uiState.value
        if (state.week == null && !state.isWeekLoading) loadWeek()
        if (state.weather == null) loadWeather()
        if (state.posts.isEmpty() && !state.isPostsLoading) loadPosts()
    }

    private fun loadWeek() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeekLoading = true, weekError = null) }

            scheduleRepository.getWeek(week = HomeDates.todayIso, oid = null)
                .onSuccess { week ->
                    _uiState.update { it.copy(week = week, isWeekLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isWeekLoading = false, weekError = error.message ?: WEEK_ERROR_MESSAGE)
                    }
                }
        }
    }

    private fun loadWeather() {
        viewModelScope.launch {
            // Погода — украшение шапки: об ошибке не сообщаем, просто держим скелетон.
            weatherRepository.getWeather()
                .onSuccess { weather -> _uiState.update { it.copy(weather = weather) } }
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPostsLoading = true, postsError = null) }

            forumRepository.getPosts(limit = HOME_POSTS_LIMIT)
                .onSuccess { posts ->
                    _uiState.update { it.copy(posts = posts, isPostsLoading = false) }
                }
                .onFailure { error ->
                    // Пустая лента вместо ошибки соврала бы, что постов нет.
                    _uiState.update {
                        it.copy(isPostsLoading = false, postsError = error.message ?: POSTS_ERROR_MESSAGE)
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

    private companion object {
        const val CLOCK_TICK_MS = 30_000L

        /** Столько постов показывает блок «Сейчас между парами». */
        const val HOME_POSTS_LIMIT = 2
        const val WEEK_ERROR_MESSAGE = "Не удалось загрузить расписание"
        const val POSTS_ERROR_MESSAGE = "Не удалось загрузить посты"
        const val REPORT_SENT_MESSAGE = "Жалоба отправлена"
        const val DELETE_DONE_MESSAGE = "Пост удален"
    }
}
