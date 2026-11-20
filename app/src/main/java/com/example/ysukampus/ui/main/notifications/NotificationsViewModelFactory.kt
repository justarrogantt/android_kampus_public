package com.example.ysukampus.ui.main.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.data.network.auth.TokenRefresherProvider
import com.example.ysukampus.data.network.di.createAuthorizedService
import com.example.ysukampus.data.network.ws.UserWebSocketClient
import com.example.ysukampus.data.notifications.NotificationRepository
import com.example.ysukampus.data.session.SessionManagerProvider

/** Ручной DI — как в ForumViewModelFactory: Hilt в проекте нет. */
class NotificationsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val tokenStorage = EncryptedPrefsTokenStorage(appContext)

            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(
                repository = NotificationRepository(
                    authorizedService = createAuthorizedService(
                        context = appContext,
                        tokenStorage = tokenStorage,
                        sessionManager = SessionManagerProvider.get(appContext),
                        deviceIdStorage = SharedPrefsDeviceIdStorage(prefs)
                    )
                ),
                webSocketClient = UserWebSocketClient(
                    baseUrl = ApiEnvironmentHolder.baseUrl,
                    tokenStorage = tokenStorage,
                    // Тот же refresh, что и у Retrofit-клиентов: сокет обновляет
                    // протухший токен, когда бэкенд закрывает его с кодом 1008.
                    tokenRefresher = TokenRefresherProvider.get(appContext),
                    onNotAuthorized = SessionManagerProvider.get(appContext)::onNotAuthorized
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
