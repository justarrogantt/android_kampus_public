package com.example.ysukampus.ui.debug

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage
import com.example.ysukampus.data.network.ApiEnvironment
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme
import kotlin.system.exitProcess

/**
 * Отладочный переключатель бэкенда: тестовый ↔ прод.
 *
 * Виден только в debug-сборках — в release/beta `isSwitchAvailable` false, и
 * композабл не рисует ничего.
 *
 * Почему после переключения перезапускается процесс: Retrofit-клиенты,
 * `SessionManagerProvider` и WebSocket-клиент — синглтоны на процесс, созданные
 * со старым адресом. Перебрать их «на лету» значит протащить инвалидацию через
 * весь граф зависимостей; для отладочной кнопки это неоправданно. Токены при
 * этом чистятся: они подписаны ключом конкретного сервера и на другом дадут 401.
 */
@Composable
fun ServerSwitcher(
    modifier: Modifier = Modifier,
    /** Компактный вид: короткие подписи и уменьшенные отступы — для тесных строк. */
    compact: Boolean = false
) {
    if (!ApiEnvironmentHolder.isSwitchAvailable) return

    val context = LocalContext.current
    var current by remember { mutableStateOf(ApiEnvironmentHolder.environment) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(KampusColors.Surface)
            .border(1.dp, KampusColors.Border, RoundedCornerShape(10.dp))
            .padding(if (compact) 2.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ApiEnvironment.entries.forEach { environment ->
            val isSelected = environment == current
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) KampusColors.PrimaryBlue else KampusColors.Surface
                    )
                    .clickable(enabled = !isSelected) {
                        current = environment
                        switchEnvironment(context, environment)
                    }
                    .padding(
                        horizontal = if (compact) 8.dp else 10.dp,
                        vertical = if (compact) 4.dp else 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) KampusColors.Surface else KampusColors.MutedBlue
                        )
                )
                Text(
                    text = if (compact) environment.shortTitle else environment.title,
                    style = ScheduleTypography.EntityButton,
                    color = if (isSelected) KampusColors.Surface else KampusColors.TextMuted
                )
            }
        }
    }
}

/**
 * Сохраняет выбор, чистит сессию и перезапускает приложение.
 *
 * Токены прошлого сервера намеренно удаляются: access/refresh подписаны его
 * SECRET_KEY, и на другом бэкенде дали бы бесконечный цикл 401 → refresh → 401.
 */
private fun switchEnvironment(context: Context, environment: ApiEnvironment) {
    ApiEnvironmentHolder.select(context, environment)

    runCatching { EncryptedPrefsTokenStorage(context.applicationContext).clearTokens() }

    Toast.makeText(
        context,
        "Сервер: ${environment.title}. Перезапуск…",
        Toast.LENGTH_SHORT
    ).show()

    restartApp(context)
}

private fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    if (intent == null) {
        (context as? Activity)?.recreate()
        return
    }

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    (context as? Activity)?.finish()
    // Гарантированно сбрасываем синглтоны, созданные со старым базовым URL.
    exitProcess(0)
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F7FF)
@Composable
private fun ServerSwitcherPreview() {
    YsukampusTheme(dynamicColor = false) {
        ServerSwitcher(modifier = Modifier.padding(16.dp))
    }
}
