package com.example.ysukampus.data.network.auth

import android.util.Log
import com.example.ysukampus.BuildConfig

/**
 * Диагностика цепочки обновления токена.
 *
 * HTTP-лог показывает только, что refresh-запрос НЕ ушёл, но молчит о причине:
 * решение принимается внутри [TokenAuthentificator] и [TokenRefresher] ещё до
 * обращения к сети. Эти строки закрывают ровно тот разрыв.
 *
 * Живёт под тем же флагом, что и HTTP-лог, — в release и betaRelease константа
 * `false`, ветка сворачивается R8.
 *
 * Значения токенов не логируются никогда: только наличие и длина.
 */
internal object AuthLog {

    const val TAG = "YsuAuth"

    fun d(message: String) {
        if (!BuildConfig.HTTP_LOG_ENABLED) return
        Log.d(TAG, message)
    }

    /** Безопасное описание токена: есть ли он и какой длины, но не он сам. */
    fun describe(token: String?): String = when {
        token == null -> "null"
        token.isBlank() -> "blank"
        else -> "len=${token.length}"
    }
}
