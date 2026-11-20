package com.example.ysukampus.data.network.auth

import android.content.Context
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.network.di.createDeviceAuthService
import com.example.ysukampus.data.session.SessionManagerProvider

/**
 * Процессный синглтон [TokenRefresher].
 *
 * Refresh должен быть общим для всех потребителей: авторизованные Retrofit-клиенты
 * создаются по одному на ViewModel, а WebSocket-клиент живёт отдельно. Один инстанс —
 * один мьютекс, поэтому одновременный 401 в HTTP и 1008 в сокете дают один
 * refresh-запрос, а не гонку двух.
 *
 * Refresh-сервис строится на device-id-клиенте (не на авторизованном), иначе
 * обновление токена рекурсивно упёрлось бы в собственный аутентификатор.
 */
object TokenRefresherProvider {
    @Volatile
    private var instance: TokenRefresher? = null

    fun get(context: Context): TokenRefresher {
        val appContext = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: TokenRefresher(
                tokenStorage = EncryptedPrefsTokenStorage(appContext),
                authService = createDeviceAuthService(
                    appContext,
                    SharedPrefsDeviceIdStorage(
                        appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    )
                ),
                onNotAuthorized = SessionManagerProvider.get(appContext)::onNotAuthorized
            ).also { instance = it }
        }
    }
}
