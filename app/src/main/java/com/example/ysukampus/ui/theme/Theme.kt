package com.example.ysukampus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ysukampus.ui.main.KampusColors

/**
 * Единственная схема оболочки: дизайн светлый по макету, и компоненты Material,
 * которые не получили цвет явно (`Text`, `Surface`, ripple), должны брать
 * токены из [KampusColors], а не из системной темы или dynamic color.
 */
private val KampusColorScheme = lightColorScheme(
    primary = KampusColors.PrimaryBlue,
    onPrimary = Color.White,
    secondary = KampusColors.MutedBlue,
    onSecondary = Color.White,
    background = KampusColors.AppBackground,
    onBackground = KampusColors.TextMain,
    surface = KampusColors.Surface,
    onSurface = KampusColors.TextMain,
    surfaceVariant = KampusColors.FooterBackground,
    onSurfaceVariant = KampusColors.TextMuted,
    outline = KampusColors.Border,
    outlineVariant = KampusColors.Border
)

/**
 * Тёмной версии у оболочки нет: [darkTheme] и [dynamicColor] оставлены в сигнатуре
 * ради совместимости с вызовами и `@Preview`, но схема всегда светлая.
 */
@Composable
fun YsukampusTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KampusColorScheme,
        typography = Typography,
        content = content
    )
}
