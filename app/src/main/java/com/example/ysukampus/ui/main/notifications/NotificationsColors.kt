package com.example.ysukampus.ui.main.notifications

import androidx.compose.ui.graphics.Color

/**
 * Цвета уведомлений — CSS-переменные светлой темы вебки (`index.css`),
 * которых нет ни в `KampusColors`, ни в `HomeColors`.
 */
object NotificationsColors {
    /** `--notification-badge` — красный кружок счётчика. */
    val Badge = Color(0xFFDC4A4A)

    /** `--warning` — строка очереди модерации. */
    val Warning = Color(0xFFE87000)

    /** `--warning-soft`: rgba(232, 112, 0, 0.15) */
    val WarningSoft = Color(0x26E87000)
}
