package com.example.ysukampus.ui.main.profile.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ysukampus.data.auth.ProfileRepository
import com.example.ysukampus.data.forum.ForumRepository
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.network.di.createAuthorizedService
import com.example.ysukampus.data.session.SessionManagerProvider
import com.example.ysukampus.data.session.SharedPrefsCurrentUserStore

/** Ручной DI — как в ForumViewModelFactory: Hilt в проекте нет. */
class MyProfileViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyProfileViewModel::class.java)) {
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
            return MyProfileViewModel(
                forumRepository = ForumRepository(
                    context = appContext,
                    authorizedService = authorizedService
                ),
                // Нужен только ради `profile_id` у сессии, кэш которой его не знает.
                profileRepository = ProfileRepository(
                    context = appContext,
                    authorizedService = authorizedService
                ),
                currentUserStore = SharedPrefsCurrentUserStore(prefs),
                tokenStorage = tokenStorage
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
