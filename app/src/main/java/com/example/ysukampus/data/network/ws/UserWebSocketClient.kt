package com.example.ysukampus.data.network.ws

import android.util.Log
import com.example.ysukampus.data.network.auth.TokenRefresher
import com.example.ysukampus.data.session.TokenStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient

/**
 * Персональный канал `/ws/user`: уведомления о комментариях и ответах.
 * Вся механика соединения — в [BaseWebSocketClient]; здесь только разбор событий.
 */
class UserWebSocketClient(
    baseUrl: String,
    tokenStorage: TokenStorage,
    tokenRefresher: TokenRefresher? = null,
    onNotAuthorized: (() -> Unit)? = null,
    okHttpClient: OkHttpClient = OkHttpClient(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseWebSocketClient<UserWsEvent>(
    baseUrl = baseUrl,
    tokenStorage = tokenStorage,
    channelPath = "/ws/user",
    logTag = USER_TAG,
    tokenRefresher = tokenRefresher,
    onNotAuthorized = onNotAuthorized,
    okHttpClient = okHttpClient,
    dispatcher = dispatcher
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override fun parseEvent(payload: String): UserWsEvent? =
        runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            when (root["type"]?.jsonPrimitive?.contentOrNull) {
                TYPE_NOTIFICATION_CREATED ->
                    json.decodeFromJsonElement<UserWsEvent.NotificationCreated>(root)
                TYPE_TARGET_UNAVAILABLE ->
                    json.decodeFromJsonElement<UserWsEvent.NotificationTargetUnavailable>(root)
                else -> {
                    Log.w(TAG, "Ignoring unknown user event type: ${root["type"]}")
                    null
                }
            }
        }.getOrElse { error ->
            Log.e(TAG, "Failed to parse user WebSocket payload: $payload", error)
            null
        }

    private companion object {
        const val TAG = USER_TAG
        const val TYPE_NOTIFICATION_CREATED = "notification_created"
        const val TYPE_TARGET_UNAVAILABLE = "notification_target_unavailable"
    }
}

private const val USER_TAG = "UserWebSocket"
