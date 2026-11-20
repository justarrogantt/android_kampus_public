package com.example.ysukampus.ui.main.profile.joke

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ysukampus.data.auth.ProfileRepository
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.network.di.createAuthorizedService
import com.example.ysukampus.data.session.SessionManagerProvider

/** Ручной DI — как в ProfileViewModelFactory: Hilt в проекте нет. */
class JokeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JokeViewModel::class.java)) {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val tokenStorage = EncryptedPrefsTokenStorage(appContext)
            val authorizedService = createAuthorizedService(
                context = context,
                tokenStorage = tokenStorage,
                sessionManager = SessionManagerProvider.get(appContext),
                deviceIdStorage = SharedPrefsDeviceIdStorage(prefs)
            )

            @Suppress("UNCHECKED_CAST")
            return JokeViewModel(
                profileRepository = ProfileRepository(
                    context = appContext,
                    authorizedService = authorizedService
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
