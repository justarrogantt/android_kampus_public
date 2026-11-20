package com.example.ysukampus.data.network.di

import android.content.Context
import com.example.ysukampus.BuildConfig
import com.example.ysukampus.data.auth.AuthService
import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.auth.PublicService
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.session.DeviceIdStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.network.admin.AdminApi
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.data.network.auth.AuthInterceptor
import com.example.ysukampus.data.network.auth.DeviceIdInterceptor
import com.example.ysukampus.data.network.auth.TokenAuthentificator
import com.example.ysukampus.data.network.auth.TokenRefresherProvider
import com.example.ysukampus.data.session.SessionManager
import com.example.ysukampus.data.session.SessionManagerProvider
import com.example.ysukampus.data.session.TokenStorage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

// Базовый URL берём из holder'а, а не из BuildConfig напрямую: в отладочной
// сборке его подменяет переключатель серверов (см. ApiEnvironmentHolder).
private val BASE_URL: String
    get() = ApiEnvironmentHolder.baseUrl
private val NetworkJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun createPublicAuthService(): PublicService {

    val okHttpClient = HttpClientProvider.newBuilder()
        .build()

    val retrofit = Retrofit.Builder()
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    return retrofit.create(PublicService::class.java)
}

fun createDeviceAuthService(
    context: Context,
    deviceIdStorage: DeviceIdStorage = SharedPrefsDeviceIdStorage(
        context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    )
): AuthService {
    val interceptor = DeviceIdInterceptor(deviceIdStorage)

    val okHttpClient = HttpClientProvider.newBuilder()
        .addInterceptor(interceptor)
        .build()

    val retrofit = Retrofit.Builder()
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    return retrofit.create(AuthService::class.java)
}

fun createAuthorizedService(
    context: Context,
    tokenStorage: TokenStorage = EncryptedPrefsTokenStorage(context.applicationContext),
    sessionManager: SessionManager = SessionManagerProvider.get(context),
    deviceIdStorage: DeviceIdStorage = SharedPrefsDeviceIdStorage(
        context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    )
): AuthorizedService {
    val authInterceptor = AuthInterceptor(tokenStorage)
    val deviceIdInterceptor = DeviceIdInterceptor(deviceIdStorage)
    // Refresh — процессный синглтон (см. TokenRefresherProvider): его же дёргает
    // WebSocket-клиент, когда бэкенд закрывает сокет с кодом 1008.
    val tokenAuthenticator = TokenAuthentificator(
        tokenStorage = tokenStorage,
        tokenRefresher = TokenRefresherProvider.get(context),
        onNotAuthorized = sessionManager::onNotAuthorized
    )

    val okHttpClient = HttpClientProvider.newBuilder()
        .addInterceptor(deviceIdInterceptor)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .build()

    val retrofit = Retrofit.Builder()
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    return retrofit.create(AuthorizedService::class.java)
}

fun createAdminApi(
    context: Context,
    tokenStorage: TokenStorage = EncryptedPrefsTokenStorage(context.applicationContext),
    sessionManager: SessionManager = SessionManagerProvider.get(context),
    deviceIdStorage: DeviceIdStorage = SharedPrefsDeviceIdStorage(
        context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    )
): AdminApi {
    val authInterceptor = AuthInterceptor(tokenStorage)
    val deviceIdInterceptor = DeviceIdInterceptor(deviceIdStorage)
    // Refresh — процессный синглтон (см. TokenRefresherProvider): его же дёргает
    // WebSocket-клиент, когда бэкенд закрывает сокет с кодом 1008.
    val tokenAuthenticator = TokenAuthentificator(
        tokenStorage = tokenStorage,
        tokenRefresher = TokenRefresherProvider.get(context),
        onNotAuthorized = sessionManager::onNotAuthorized
    )

    val okHttpClient = HttpClientProvider.newBuilder()
        .addInterceptor(deviceIdInterceptor)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .build()

    val retrofit = Retrofit.Builder()
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    return retrofit.create(AdminApi::class.java)
}
