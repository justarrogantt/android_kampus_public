package com.example.ysukampus.data.auth

import com.example.ysukampus.BuildConfig
import com.example.ysukampus.data.network.dto.DevLoginRequestDto

enum class AuthMode {
    Dev,
    Telegram
}

data class AuthConfig(
    val mode: AuthMode,
    val devLoginRequest: DevLoginRequestDto
) {
    val isDevMode: Boolean
        get() = mode == AuthMode.Dev

    companion object {
        fun fromBuildConfig(): AuthConfig {
            val mode = when (BuildConfig.AUTH_MODE.lowercase()) {
                "dev" -> AuthMode.Dev
                else -> AuthMode.Telegram
            }

            return AuthConfig(
                mode = mode,
                devLoginRequest = DevLoginRequestDto(
                    userId = BuildConfig.DEV_USER_ID,
                    role = BuildConfig.DEV_ROLE,
                    entityId = BuildConfig.DEV_ENTITY_ID,
                    firstName = BuildConfig.DEV_FIRST_NAME
                )
            )
        }
    }
}
