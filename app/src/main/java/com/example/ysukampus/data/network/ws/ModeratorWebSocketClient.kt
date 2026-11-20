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
 * Канал модератора `/ws/moderator`: очередь модерации и её счётчик.
 * Вся механика соединения — в [BaseWebSocketClient]; здесь только разбор событий.
 */
class ModeratorWebSocketClient(
    baseUrl: String,
    tokenStorage: TokenStorage,
    tokenRefresher: TokenRefresher? = null,
    onNotAuthorized: (() -> Unit)? = null,
    okHttpClient: OkHttpClient = OkHttpClient(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseWebSocketClient<ModeratorWsEvent>(
    baseUrl = baseUrl,
    tokenStorage = tokenStorage,
    channelPath = "/ws/moderator",
    logTag = MODERATOR_TAG,
    tokenRefresher = tokenRefresher,
    onNotAuthorized = onNotAuthorized,
    okHttpClient = okHttpClient,
    dispatcher = dispatcher
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override fun parseEvent(payload: String): ModeratorWsEvent? =
        runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            when (root["type"]?.jsonPrimitive?.contentOrNull) {
                TYPE_NEW_POST -> json.decodeFromJsonElement<ModeratorWsEvent.NewPost>(root)
                TYPE_NEW_COMMENT -> json.decodeFromJsonElement<ModeratorWsEvent.NewComment>(root)
                TYPE_MODERATION_COUNT ->
                    json.decodeFromJsonElement<ModeratorWsEvent.ModerationCount>(root)
                else -> {
                    Log.w(TAG, "Ignoring unknown moderator event type: ${root["type"]}")
                    null
                }
            }
        }.getOrElse { error ->
            Log.e(TAG, "Failed to parse moderator WebSocket payload: $payload", error)
            null
        }

    private companion object {
        const val TAG = MODERATOR_TAG
        const val TYPE_NEW_POST = "new_post"
        const val TYPE_NEW_COMMENT = "new_comment"
        const val TYPE_MODERATION_COUNT = "moderation_count"
    }
}

private const val MODERATOR_TAG = "ModeratorWebSocket"
