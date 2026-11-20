package com.example.ysukampus.ui.main.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.local.SharedPrefsDeviceIdStorage
import com.example.ysukampus.data.network.di.createAuthorizedService
import com.example.ysukampus.data.schedule.ScheduleRepository
import com.example.ysukampus.data.session.SessionManagerProvider

/**
 * Ручной DI: в проекте нет Hilt, поэтому зависимости собираются здесь —
 * так же, как это делает ProfileViewModelFactory.
 */
class ScheduleViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
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
            return ScheduleViewModel(ScheduleRepository(authorizedService)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
