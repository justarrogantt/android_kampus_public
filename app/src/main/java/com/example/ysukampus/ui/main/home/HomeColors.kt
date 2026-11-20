package com.example.ysukampus.ui.main.home

import androidx.compose.ui.graphics.Color

/**
 * Цвета главной — перенесены из CSS-переменных светлой темы вебки
 * (`index.css`). Общая оболочка (фон, футер) продолжает жить в `KampusColors`,
 * здесь только то, чего в ней нет: мягкая заливка, акцентный текст, бейджи.
 */
object HomeColors {
    /** `--primary` */
    val Primary = Color(0xFF3B82F6)
    /** `--primary-light` — точки пар в карточке дня. */
    val PrimaryLight = Color(0xFF60A5FA)
    /** `--primary-soft`: rgba(59, 130, 246, 0.10) */
    val PrimarySoft = Color(0x1A3B82F6)
    /** `--on-primary` */
    val OnPrimary = Color(0xFFFFFFFF)

    /** `--border`: rgba(59, 131, 246, 0.173) */
    val Border = Color(0x2C3B83F6)
    /** `--border-strong` */
    val BorderStrong = Color(0xFF8AABF5)

    /** `--text-main` */
    val TextMain = Color(0xFF0F1729)
    /** `--text-accent` */
    val TextAccent = Color(0xFF2D5FA8)
    /** `--text-muted` */
    val TextMuted = Color(0xFF8FB3E0)

    /** `--neutral-badge-bg`: rgba(148, 163, 184, 0.15) */
    val NeutralBadgeBackground = Color(0x2694A3B8)
    /** `--neutral-badge-text` */
    val NeutralBadgeText = Color(0xFF94A3B8)

    /** `--danger` */
    val Danger = Color(0xFFDA1D1D)
    /** `--danger-soft`: #e800001b */
    val DangerSoft = Color(0x1BE80000)

    /** `--skeleton-base`: rgba(148, 163, 184, 0.14) */
    val SkeletonBase = Color(0x2494A3B8)
    /** `--skeleton-highlight`: rgba(148, 163, 184, 0.30) */
    val SkeletonHighlight = Color(0x4D94A3B8)

    val Surface = Color(0xFFFFFFFF)
}
