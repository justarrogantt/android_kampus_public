package com.example.ysukampus.ui.admin.data

import android.content.Context
import com.example.ysukampus.data.network.di.createAdminApi
import com.example.ysukampus.data.network.ws.ModeratorWebSocketClient
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.network.auth.TokenRefresherProvider
import com.example.ysukampus.data.session.SessionManagerProvider
import com.example.ysukampus.data.session.SharedPrefsCurrentUserStore

object AdminRepositoryProvider {
    fun create(
        context: Context,
        mode: AdminDataSourceMode = AdminDataSourceMode.Fake
    ): AdminRepository =
        when (mode) {
            AdminDataSourceMode.Fake -> FakeAdminRepository()
            AdminDataSourceMode.Remote -> {
                val appContext = context.applicationContext
                val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                RemoteAdminRepository(
                    api = createAdminApi(appContext),
                    moderatorWebSocketClient = ModeratorWebSocketClient(
                        baseUrl = com.example.ysukampus.data.network.ApiEnvironmentHolder.baseUrl,
                        tokenStorage = EncryptedPrefsTokenStorage(appContext),
                        // Тот же refresh, что и у Retrofit-клиентов: сокет обновляет
                        // протухший токен, когда бэкенд закрывает его с кодом 1008.
                        tokenRefresher = TokenRefresherProvider.get(appContext),
                        onNotAuthorized = SessionManagerProvider.get(appContext)::onNotAuthorized
                    ),
                    // Права нужны, чтобы не показывать вкладки и кнопки, на которые
                    // бэкенд всё равно ответит 403 — как это делает вебка.
                    currentUserStore = SharedPrefsCurrentUserStore(prefs),
                    prefs = prefs
                )
            }
        }
}
