package com.example.ysukampus.ui.main.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ysukampus.data.forum.createForumRepository
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.network.di.createAuthorizedService
import com.example.ysukampus.data.schedule.createScheduleRepository
import com.example.ysukampus.data.session.SessionManagerProvider
import com.example.ysukampus.data.session.SharedPrefsCurrentUserStore
import com.example.ysukampus.data.weather.WeatherRepository

/** Ручной DI — как в ScheduleViewModelFactory: Hilt в проекте нет. */
class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val deviceIdStorage = SharedPrefsDeviceIdStorage(prefs)
            val tokenStorage = EncryptedPrefsTokenStorage(appContext)
            // Общий на процесс: только он двигает SessionManager.authState, за которым
            // следит RegistrationFlowScreen. Локальная копия проглатывала бы
            // разлогин по протухшему refresh — экран просто залипал на ошибке.
            val sessionManager = SessionManagerProvider.get(appContext)
            val authorizedService = createAuthorizedService(
                context = context,
                tokenStorage = tokenStorage,
                sessionManager = sessionManager,
                deviceIdStorage = deviceIdStorage
            )

            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                scheduleRepository = createScheduleRepository(appContext, authorizedService),
                weatherRepository = WeatherRepository(authorizedService),
                forumRepository = createForumRepository(appContext, authorizedService),
                currentUserStore = SharedPrefsCurrentUserStore(prefs)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
