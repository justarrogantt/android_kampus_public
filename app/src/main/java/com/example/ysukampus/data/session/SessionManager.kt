package com.example.ysukampus.data.session
import com.example.ysukampus.data.network.auth.RefreshResult
import com.example.ysukampus.data.network.auth.isAccessTokenExpired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionManager(
    private val tokenStorage: TokenStorage,
    /**
     * Обновление access-токена (тот же процессный `TokenRefresher`, что у
     * OkHttp-стека). Передаётся лямбдой, а не инстансом, чтобы не завязывать
     * создание [SessionManager] на `TokenRefresherProvider`, который сам
     * зависит от [SessionManager] (см. `SessionManagerProvider`).
     */
    private val refreshAccessToken: (suspend (String?) -> RefreshResult)? = null,
    /**
     * Вызывается, когда сессия закончилась: пора выбросить всё, что осталось от
     * пользователя на устройстве (локальный кэш ответов). Лямбда, а не вызов
     * базы напрямую, — чтобы [SessionManager] не знал ни про Room, ни про
     * Context и оставался проверяемым обычным unit-тестом.
     */
    private val onSessionCleared: (() -> Unit)? = null
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState

    /**
     * Проверяет сохранённую сессию на старте.
     *
     * Наличия токенов в хранилище недостаточно: access живёт минуты, поэтому в
     * подавляющем большинстве запусков он уже просрочен. Раньше состояние
     * становилось [AuthState.Authorized] сразу по факту наличия токенов —
     * главный экран успевал начать загрузку и только потом его выбрасывало на
     * регистрацию, когда падал первый же запрос. Теперь просроченный токен
     * обновляется здесь, до показа авторизованной зоны, и до конца проверки
     * состояние остаётся [AuthState.Checking].
     */
    suspend fun restoreSession() {
        _authState.value = AuthState.Checking

        val access = tokenStorage.getAccessToken()
        val refresh = tokenStorage.getRefreshToken()
        if (access == null || refresh == null) {
            _authState.value = AuthState.NotAuthorized
            return
        }

        val isSessionAlive = when {
            !isAccessTokenExpired(access) -> true
            // refreshAccessToken == null — проверить нечем, ведём себя как
            // раньше: пускаем внутрь, дальше сработает путь через 401.
            refreshAccessToken == null -> true
            else -> when (refreshAccessToken.invoke(access)) {
                is RefreshResult.Success -> true
                // Сервер отверг refresh-токен: TokenRefresher уже почистил
                // хранилище и сообщил о разлогине.
                RefreshResult.SessionExpired -> false
                // Сервер недоступен (типичный случай — старт без сети).
                // Токены целы, и выбрасывать пользователя на регистрацию
                // не за что: пускаем внутрь, экраны покажут ошибку загрузки,
                // а refresh повторится на первом же удачном запросе.
                is RefreshResult.Unavailable -> true
            }
        }

        _authState.value = if (isSessionAlive) AuthState.Authorized else AuthState.NotAuthorized
    }

    fun onAuthorized() {
        _authState.value = AuthState.Authorized
    }

    fun onNotAuthorized() {
        tokenStorage.clearTokens()
        // Кэш расписания и профиля — персональные данные. Оставить их на
        // устройстве после выхода означает показать их следующему, кто войдёт
        // с этого телефона.
        onSessionCleared?.invoke()
        _authState.value = AuthState.NotAuthorized
    }
}
