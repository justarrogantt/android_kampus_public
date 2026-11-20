package com.example.ysukampus.ui.main.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ysukampus.data.auth.ProfileRepository
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.network.di.createAuthorizedService
import com.example.ysukampus.data.schedule.createScheduleRepository
import com.example.ysukampus.data.session.SharedPrefsCurrentUserStore
import com.example.ysukampus.data.session.SessionManagerProvider

class ProfileViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val prefs = context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val deviceIdStorage = SharedPrefsDeviceIdStorage(prefs)
            val tokenStorage = EncryptedPrefsTokenStorage(context.applicationContext)
            // Общий на процесс: только он двигает SessionManager.authState, за которым
            // следит RegistrationFlowScreen. Локальная копия проглатывала бы
            // разлогин по протухшему refresh — экран просто залипал на ошибке.
            val sessionManager = SessionManagerProvider.get(context)
            val authorizedService = createAuthorizedService(
                context = context,
                tokenStorage = tokenStorage,
                sessionManager = sessionManager,
                deviceIdStorage = deviceIdStorage
            )
            val repository = ProfileRepository(
                context = context.applicationContext,
                authorizedService = authorizedService
            )

            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                profileRepository = repository,
                // Тот же справочник групп и преподавателей, что и в пикере
                // расписания: своя копия репозитория лишь заново скачает список.
                scheduleRepository = createScheduleRepository(context.applicationContext, authorizedService),
                currentUserStore = SharedPrefsCurrentUserStore(prefs),
                tokenStorage = tokenStorage
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
