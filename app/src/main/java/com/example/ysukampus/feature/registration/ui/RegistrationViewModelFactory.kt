package com.example.ysukampus.feature.registration.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ysukampus.data.auth.AuthConfig
import com.example.ysukampus.data.auth.AuthRepository
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.auth.RegistrationRepository
import com.example.ysukampus.data.auth.SessionRepository
import com.example.ysukampus.data.network.di.createAuthorizedService
import com.example.ysukampus.data.network.di.createDeviceAuthService
import com.example.ysukampus.data.network.di.createPublicAuthService
import com.example.ysukampus.data.session.SessionManagerProvider
import com.example.ysukampus.data.session.SharedPrefsCurrentUserStore
import com.example.ysukampus.feature.registration.domain.DefaultRegistrationInteractor
import com.example.ysukampus.feature.registration.domain.SessionsSummaryFormatter

class RegistrationViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrationViewModel::class.java)) {
            val publicService = createPublicAuthService()
            val prefs = context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val deviceIdStorage = SharedPrefsDeviceIdStorage(prefs)
            val authService = createDeviceAuthService(context, deviceIdStorage)
            val tokenStorage = EncryptedPrefsTokenStorage(context.applicationContext)
            val sessionManager = SessionManagerProvider.get(context)
            val authorizedService = createAuthorizedService(
                context = context,
                tokenStorage = tokenStorage,
                sessionManager = sessionManager,
                deviceIdStorage = deviceIdStorage
            )
            val registrationRepository = RegistrationRepository(publicService, authService, tokenStorage)
            val authRepository = AuthRepository(
                authConfig = AuthConfig.fromBuildConfig(),
                registrationRepository = registrationRepository
            )
            val sessionRepository = SessionRepository(
                authorizedService = authorizedService,
                tokenStorage = tokenStorage,
                sessionManager = sessionManager,
                deviceIdStorage = deviceIdStorage
            )
            val interactor = DefaultRegistrationInteractor(
                authRepository = authRepository,
                registrationRepository = registrationRepository,
                sessionRepository = sessionRepository,
                sessionManager = sessionManager,
                currentUserStore = SharedPrefsCurrentUserStore(prefs),
                sessionsSummaryFormatter = SessionsSummaryFormatter()
            )

            @Suppress("UNCHECKED_CAST")
            return RegistrationViewModel(
                interactor = interactor
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
