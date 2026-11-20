package com.example.ysukampus.data.network.ws

import android.util.Log
import com.example.ysukampus.data.network.auth.RefreshResult
import com.example.ysukampus.data.network.auth.TokenRefresher
import com.example.ysukampus.data.session.TokenStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Общая механика сокетов приложения: сборка `ws`/`wss`-URL из `BASE_URL`,
 * токен в query-параметре, heartbeat, backoff-реконнект и обновление
 * протухшего токена по коду закрытия 1008.
 *
 * Канал параметризуется наследником: путь ([channelPath]) и разбор событий
 * ([parseEvent]). На этой же базе живёт персональный канал `/ws/user`.
 */
abstract class BaseWebSocketClient<E : Any>(
    private val baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val channelPath: String,
    private val logTag: String,
    private val tokenRefresher: TokenRefresher? = null,
    private val onNotAuthorized: (() -> Unit)? = null,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _events = MutableSharedFlow<E>(extraBufferCapacity = 32)
    private val _connectionState = MutableStateFlow(WsConnectionState.Disconnected)
    val events: SharedFlow<E> = _events
    val connectionState: StateFlow<WsConnectionState> = _connectionState

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val reconnectDelaysMs = longArrayOf(1_000L, 2_000L, 5_000L, 10_000L, 30_000L)
    private val lock = Any()

    @Volatile
    private var isStopped = true
    private var reconnectAttempt = 0
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    /** Токен, с которым открыто текущее соединение: с ним же сверяется refresh. */
    @Volatile
    private var currentToken: String? = null

    /** Сколько раз подряд сервер закрыл соединение с 1008. */
    private var unauthorizedAttempts = 0

    /** Разбирает входящий текстовый кадр; `null` — кадр игнорируется. */
    protected abstract fun parseEvent(payload: String): E?

    fun start() {
        synchronized(lock) {
            if (!isStopped && webSocket != null) return
            isStopped = false
            reconnectAttempt = 0
            unauthorizedAttempts = 0
        }
        Log.d(logTag, "Starting WebSocket $channelPath")
        connect(isReconnect = false)
    }

    fun stop() {
        synchronized(lock) {
            isStopped = true
        }
        reconnectJob?.cancel()
        reconnectJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        synchronized(lock) {
            webSocket?.close(CLOSE_NORMAL, "Stopped by client")
            webSocket = null
            unauthorizedAttempts = 0
        }
        _connectionState.value = WsConnectionState.Disconnected
        Log.d(logTag, "Stopped WebSocket $channelPath")
    }

    fun cancel() {
        stop()
        scope.cancel()
    }

    private fun connect(isReconnect: Boolean) {
        if (isStopped) return

        val accessToken = tokenStorage.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            _connectionState.value = WsConnectionState.Failed
            Log.w(logTag, "Skipping WebSocket connect: access token is missing")
            return
        }

        val wsUrl = runCatching { buildWebSocketUrl(accessToken) }
            .onFailure { error ->
                Log.e(logTag, "Failed to build WebSocket URL from BASE_URL=$baseUrl", error)
            }
            .getOrNull()
        if (wsUrl == null) {
            _connectionState.value = WsConnectionState.Failed
            Log.e(logTag, "Skipping WebSocket connect: invalid BASE_URL=$baseUrl")
            return
        }

        _connectionState.value = if (isReconnect) {
            WsConnectionState.Reconnecting
        } else {
            WsConnectionState.Connecting
        }
        Log.d(logTag, "Opening WebSocket: ${wsUrl.withoutQuery()}")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        currentToken = accessToken
        val socket = okHttpClient.newWebSocket(request, createListener())
        synchronized(lock) {
            webSocket = socket
        }
    }

    private fun createListener(): WebSocketListener =
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isCurrentSocket(webSocket) || isStopped) return
                reconnectAttempt = 0
                unauthorizedAttempts = 0
                _connectionState.value = WsConnectionState.Connected
                Log.d(logTag, "WebSocket connected")
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!isCurrentSocket(webSocket) || isStopped) return
                parseEvent(text)?.let { event ->
                    _events.tryEmit(event)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrentSocket(webSocket)) return
                Log.d(logTag, "WebSocket closing: code=$code reason=$reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrentSocket(webSocket)) return
                Log.d(logTag, "WebSocket closed: code=$code reason=$reason")
                handleDisconnect(closeCode = code, httpCode = null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isCurrentSocket(webSocket)) return
                Log.e(
                    logTag,
                    "WebSocket failure: code=${response?.code ?: "n/a"} message=${t.message}",
                    t
                )
                handleDisconnect(closeCode = null, httpCode = response?.code)
            }
        }

    private fun handleDisconnect(closeCode: Int?, httpCode: Int?) {
        heartbeatJob?.cancel()
        heartbeatJob = null
        synchronized(lock) {
            webSocket = null
        }
        if (isStopped) {
            _connectionState.value = WsConnectionState.Disconnected
            return
        }

        // Бэкенд принимает соединение и закрывает его с 1008, когда токен протух
        // (app/routers/ws.py::reject). Реконнект с тем же токеном даст такой же
        // отказ, поэтому перед следующей попыткой токен надо обновить.
        val isTokenRejected = closeCode == CLOSE_POLICY_VIOLATION ||
            httpCode == HTTP_UNAUTHORIZED ||
            httpCode == HTTP_FORBIDDEN
        if (isTokenRejected) {
            unauthorizedAttempts++
            if (unauthorizedAttempts > MAX_UNAUTHORIZED_ATTEMPTS) {
                Log.w(
                    logTag,
                    "WebSocket rejected $unauthorizedAttempts times in a row, giving up"
                )
                // Токен обновляли, а канал всё равно отказывает: дело не в нём
                // (нет прав на канал, отозванная сессия) — сессию не трогаем.
                giveUpUnauthorized(notifySession = false)
                return
            }
            scheduleReconnect(reason = closeCode ?: httpCode, refreshTokenFirst = true)
            return
        }

        scheduleReconnect(reason = closeCode ?: httpCode, refreshTokenFirst = false)
    }

    private fun giveUpUnauthorized(notifySession: Boolean) {
        synchronized(lock) {
            isStopped = true
            webSocket = null
        }
        reconnectJob?.cancel()
        reconnectJob = null
        _connectionState.value = WsConnectionState.Failed
        if (notifySession) {
            onNotAuthorized?.invoke()
        }
    }

    private fun scheduleReconnect(reason: Int?, refreshTokenFirst: Boolean) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = reconnectDelaysMs[reconnectAttempt.coerceAtMost(reconnectDelaysMs.lastIndex)]
            reconnectAttempt++
            _connectionState.value = WsConnectionState.Reconnecting
            Log.d(
                logTag,
                "Scheduling WebSocket reconnect in ${delayMs}ms (code=$reason, refresh=$refreshTokenFirst)"
            )
            delay(delayMs)
            if (isStopped) return@launch

            if (refreshTokenFirst) {
                val refresher = tokenRefresher
                if (refresher == null) {
                    Log.w(logTag, "WebSocket token rejected but no TokenRefresher is wired")
                    giveUpUnauthorized(notifySession = false)
                    return@launch
                }
                val refreshResult = refresher.refresh(currentToken)
                val newToken = (refreshResult as? RefreshResult.Success)?.accessToken
                if (newToken.isNullOrBlank()) {
                    // notifySession только когда сервер сам отверг refresh-токен:
                    // при недоступности сервера токены остаются рабочими, и
                    // разлогинивать из-за упавшего сокета нельзя.
                    val sessionIsOver = refreshResult is RefreshResult.SessionExpired
                    Log.w(logTag, "WebSocket token refresh failed (sessionIsOver=$sessionIsOver)")
                    giveUpUnauthorized(notifySession = sessionIsOver)
                    return@launch
                }
                Log.d(logTag, "WebSocket access token refreshed, reconnecting")
            }

            if (!isStopped) {
                connect(isReconnect = true)
            }
        }
    }

    private fun startHeartbeat(socket: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (!isStopped && isCurrentSocket(socket)) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (!isStopped && isCurrentSocket(socket)) {
                    val sent = socket.send("ping")
                    Log.d(logTag, "WebSocket heartbeat sent: success=$sent")
                }
            }
        }
    }

    private fun buildWebSocketUrl(token: String): String? {
        val httpUrl = baseUrl.toHttpUrlOrNull() ?: return null
        val wsScheme = when (httpUrl.scheme) {
            "http" -> "ws"
            "https" -> "wss"
            else -> return null
        }
        val path = httpUrl.encodedPath.removeSuffix("/") + channelPath
        val httpRequestUrl = httpUrl.newBuilder()
            .encodedPath(path)
            .setQueryParameter("token", token)
            .build()
        return httpRequestUrl.toWebSocketUrl(wsScheme)
    }

    private fun isCurrentSocket(socket: WebSocket): Boolean =
        synchronized(lock) { webSocket === socket }

    private companion object {
        const val CLOSE_NORMAL = 1000
        const val CLOSE_POLICY_VIOLATION = 1008
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HEARTBEAT_INTERVAL_MS = 28_000L

        /**
         * Сколько подряд идущих отказов по токену терпим. Один — на обновление
         * протухшего токена; больше означает, что refresh не помогает
         * (нет прав на канал, отозванная сессия), и крутить сокет бессмысленно.
         */
        const val MAX_UNAUTHORIZED_ATTEMPTS = 2
    }
}

/**
 * URL без query-строки.
 *
 * Токен доступа едет в `?token=`, а этот лог живёт и в сборке для тестеров:
 * logcat на устройстве читает любое приложение с разрешением READ_LOGS и любой,
 * у кого есть adb. Тот же довод, по которому в проекте по умолчанию выключен
 * `HTTP_LOG_ENABLED`. Для отладки соединения хватает схемы, хоста и канала —
 * именно их и видно после обрезки.
 */
private fun String.withoutQuery(): String = substringBefore('?')

private fun HttpUrl.toWebSocketUrl(wsScheme: String): String {
    val httpPrefix = "${scheme}://"
    return toString().replaceFirst(httpPrefix, "$wsScheme://")
}
