package com.example.ysukampus

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.feature.registration.ui.RegistrationFlowScreen
import com.example.ysukampus.ui.theme.YsukampusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Должно выполниться до создания любых Retrofit-клиентов:
        // они читают базовый URL из holder'а.
        ApiEnvironmentHolder.init(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.parseColor("#F0F4FF"),
                darkScrim = Color.parseColor("#F0F4FF")
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.parseColor("#F0F4FF"),
                darkScrim = Color.parseColor("#F0F4FF")
            )
        )
        setContent {
            YsukampusTheme {
                // Отладочный переключатель бэкенда живёт внизу профиля
                // (ui/main/profile/ProfileScreen.kt), чтобы не перекрывать интерфейс.
                RegistrationFlowScreen(appContext = applicationContext)
            }
        }
    }
}
