package com.example.ysukampus.data.session

import android.content.Context
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.db.DatabaseProvider
import com.example.ysukampus.data.local.db.ResponseCache
import com.example.ysukampus.data.network.auth.TokenRefresherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object SessionManagerProvider {
    @Volatile
    private var sessionManager: SessionManager? = null

    /**
     * Область для уборки после разлогина. Своя, а не viewModelScope: разлогин
     * прилетает откуда угодно (в том числе из OkHttp-аутентификатора), и чистка
     * не должна отменяться вместе с экраном, который её застал.
     */
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun get(context: Context): SessionManager {
        val appContext = context.applicationContext
        return sessionManager ?: synchronized(this) {
            sessionManager ?: SessionManager(
                tokenStorage = EncryptedPrefsTokenStorage(appContext),
                // Лениво: TokenRefresherProvider сам просит SessionManager,
                // поэтому обращаемся к нему только в момент проверки сессии,
                // когда синглтон уже создан.
                refreshAccessToken = { failedToken ->
                    TokenRefresherProvider.get(appContext).refresh(failedToken)
                },
                onSessionCleared = {
                    cleanupScope.launch {
                        ResponseCache(DatabaseProvider.cachedResponseDao(appContext)).clear()
                    }
                }
            ).also { sessionManager = it }
        }
    }
}
