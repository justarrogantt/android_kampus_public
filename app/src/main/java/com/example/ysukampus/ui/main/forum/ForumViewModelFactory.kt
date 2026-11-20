package com.example.ysukampus.ui.main.forum

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ysukampus.data.forum.ForumImageDownloader
import com.example.ysukampus.data.forum.ForumRepository
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.network.di.createAuthorizedService
import com.example.ysukampus.data.session.SessionManagerProvider

/** Ручной DI — как в HomeViewModelFactory: Hilt в проекте нет. */
class ForumViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForumViewModel::class.java)) {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val tokenStorage = EncryptedPrefsTokenStorage(appContext)

            @Suppress("UNCHECKED_CAST")
            return ForumViewModel(
                forumRepository = ForumRepository(
                    context = appContext,
                    authorizedService = createAuthorizedService(
                        context = context,
                        tokenStorage = tokenStorage,
                        sessionManager = SessionManagerProvider.get(appContext),
                        deviceIdStorage = SharedPrefsDeviceIdStorage(prefs)
                    )
                ),
                imageDownloader = ForumImageDownloader(appContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
