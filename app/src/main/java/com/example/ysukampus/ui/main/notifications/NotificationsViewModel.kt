package com.example.ysukampus.ui.main.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.network.ws.UserWebSocketClient
import com.example.ysukampus.data.network.ws.UserWsEvent
import com.example.ysukampus.data.notifications.Notification
import com.example.ysukampus.data.notifications.NotificationRepository
import com.example.ysukampus.data.notifications.NotificationUnavailableReason
import com.example.ysukampus.data.notifications.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Уведомления форума: список, счётчики и персональный сокет.
 *
 * Модель живёт на уровне всей авторизованной оболочки, а не одной шторки:
 * бейдж на колокольчике расписания и на пункте профиля должен обновляться,
 * пока шторка закрыта. Сокет при этом привязан не к модели, а к пребыванию
 * в авторизованной зоне — его включает и выключает [onEnterAuthorizedZone] /
 * [onLeaveAuthorizedZone], иначе после разлогина канал крутился бы вхолостую.
 */
class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val webSocketClient: UserWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadFirstPage()
        observeSocket()
    }

    /** Пользователь вошёл в авторизованную зону: поднимаем канал и догоняем счётчики. */
    fun onEnterAuthorizedZone() {
        webSocketClient.start()
        val state = _uiState.value
        // После разлогина и повторного входа список остаётся от прошлой сессии:
        // перечитываем первую страницу, чтобы счётчики не врали.
        if (!state.isLoading) loadFirstPage()
    }

    fun onLeaveAuthorizedZone() {
        webSocketClient.stop()
    }

    fun onOpenSheet() {
        _uiState.update { it.copy(isSheetOpen = true) }
        // Прошлая попытка кончилась ошибкой — повторяем её при открытии, как в вебке.
        if (_uiState.value.error != null) loadFirstPage()
    }

    fun onCloseSheet() {
        _uiState.update { it.copy(isSheetOpen = false) }
    }

    fun onRetry() {
        loadFirstPage()
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /** Список докрутился до конца — тянем следующую страницу. */
    fun onLoadMore() {
        val state = _uiState.value
        val cursor = state.nextCursor ?: return
        if (!state.canLoadMore) return

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            repository.getNotifications(limit = PAGE_SIZE, cursor = cursor)
                .onSuccess { page ->
                    _uiState.update { current ->
                        current.copy(
                            items = mergeNotifications(current.items, page.items),
                            nextCursor = page.nextCursor,
                            unreadCount = page.unreadCount,
                            moderationCount = page.moderationCount,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure { error ->
                    // Загруженное остаётся на экране: об ошибке догрузки говорит снекбар.
                    _uiState.update { it.copy(isLoadingMore = false, message = error.message) }
                }
        }
    }

    /**
     * Тап по карточке.
     *
     * Тред открывается сразу, не дожидаясь ответа об отметке прочитанного —
     * так же ведёт себя вебка. У недоступной цели остаётся только отметка и
     * сообщение о причине.
     */
    fun onNotificationClick(notificationId: Int) {
        val notification = _uiState.value.items.firstOrNull { it.id == notificationId } ?: return
        markRead(notification)

        if (!notification.target.isAvailable) {
            val message = when (notification.target.unavailableReason) {
                NotificationUnavailableReason.PostDeleted -> POST_GONE_MESSAGE
                else -> COMMENT_GONE_MESSAGE
            }
            _uiState.update { it.copy(message = message) }
            return
        }

        _uiState.update { state ->
            state.copy(
                isSheetOpen = false,
                openTarget = NotificationOpenTarget(
                    postId = notification.target.postId,
                    commentId = notification.target.commentId
                )
            )
        }
    }

    /** Оболочка увела пользователя в тред — запрос гасим, иначе он повторится. */
    fun onOpenTargetHandled() {
        _uiState.update { it.copy(openTarget = null) }
    }

    fun onReadAllClick() {
        val state = _uiState.value
        if (state.isReadingAll || state.unreadCount == 0) return

        _uiState.update { it.copy(isReadingAll = true) }

        viewModelScope.launch {
            repository.markAllRead()
                .onSuccess { unreadCount ->
                    val readAt = nowIso()
                    _uiState.update { current ->
                        current.copy(
                            items = current.items.map { notification ->
                                if (notification.isRead) {
                                    notification
                                } else {
                                    notification.copy(readAtIso = readAt)
                                }
                            },
                            unreadCount = unreadCount,
                            isReadingAll = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isReadingAll = false, message = error.message) }
                }
        }
    }

    override fun onCleared() {
        webSocketClient.cancel()
        super.onCleared()
    }

    private fun loadFirstPage() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            repository.getNotifications(limit = PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.update { current ->
                        current.copy(
                            // Карточки, прилетевшие сокетом во время запроса,
                            // остаются в начале списка — как в вебке.
                            items = mergeNotifications(current.items, page.items),
                            nextCursor = page.nextCursor,
                            unreadCount = page.unreadCount,
                            moderationCount = page.moderationCount,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Отметка одного уведомления.
     *
     * Ошибку молчим: пользователь шёл в тред, а не отмечать прочитанное, и
     * счётчик поправится на следующей загрузке страницы.
     */
    private fun markRead(notification: Notification) {
        if (notification.isRead) return

        viewModelScope.launch {
            repository.markRead(notification.id)
                .onSuccess { unreadCount ->
                    val readAt = nowIso()
                    _uiState.update { state ->
                        state.copy(
                            items = state.items.map { item ->
                                if (item.id == notification.id) {
                                    item.copy(readAtIso = item.readAtIso ?: readAt)
                                } else {
                                    item
                                }
                            },
                            unreadCount = unreadCount
                        )
                    }
                }
        }
    }

    private fun observeSocket() {
        viewModelScope.launch {
            webSocketClient.events.collect { event ->
                when (event) {
                    is UserWsEvent.NotificationCreated -> onNotificationCreated(event)
                    is UserWsEvent.NotificationTargetUnavailable -> onTargetUnavailable(event)
                }
            }
        }
    }

    /** Готовая карточка из сокета встаёт в начало списка и поднимает счётчик. */
    private fun onNotificationCreated(event: UserWsEvent.NotificationCreated) {
        val notification = event.notification.toDomain()
        _uiState.update { state ->
            if (state.items.any { it.id == notification.id }) return@update state

            state.copy(
                items = listOf(notification) + state.items,
                unreadCount = if (notification.isRead) {
                    state.unreadCount
                } else {
                    state.unreadCount + 1
                }
            )
        }
    }

    /**
     * Удалённая цель помечается, а не выкидывается: карточка остаётся в
     * списке, но перестаёт открываться и показывает причину.
     *
     * Пустой `comment_id` означает удаление всего поста — тогда недоступными
     * становятся все уведомления этого поста.
     */
    private fun onTargetUnavailable(event: UserWsEvent.NotificationTargetUnavailable) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { notification ->
                    val isTarget = if (event.commentId != null) {
                        notification.target.commentId == event.commentId
                    } else {
                        notification.target.postId == event.postId
                    }
                    if (!isTarget) return@map notification

                    notification.copy(
                        target = notification.target.copy(
                            preview = "",
                            imagePreviewUrl = null,
                            isAvailable = false,
                            unavailableReason = if (event.commentId != null) {
                                NotificationUnavailableReason.CommentDeleted
                            } else {
                                NotificationUnavailableReason.PostDeleted
                            }
                        )
                    )
                }
            )
        }
    }

    /** Склеивает два упорядоченных списка, не пуская одну карточку дважды. */
    private fun mergeNotifications(
        first: List<Notification>,
        second: List<Notification>
    ): List<Notification> {
        val seen = mutableSetOf<Int>()
        return (first + second).filter { seen.add(it.id) }
    }

    /**
     * Время отметки нужно только карточке в списке: сервер возвращает
     * счётчик, но не сам `read_at`, а точное значение здесь ни на что не влияет.
     */
    private fun nowIso(): String = java.time.Instant.now().toString()

    private companion object {
        const val PAGE_SIZE = 10
        const val POST_GONE_MESSAGE = "Пост не найден"
        const val COMMENT_GONE_MESSAGE = "Комментарий не найден"
    }
}
