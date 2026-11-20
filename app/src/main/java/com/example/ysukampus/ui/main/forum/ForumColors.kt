package com.example.ysukampus.ui.main.forum

import androidx.compose.ui.graphics.Color

/** Разбирает `#rrggbb` из `display_color`. Некорректное значение — акцентный синий. */
fun parseAuthorColor(raw: String): Color {
    val normalized = raw.trim().removePrefix("#")
    if (normalized.length != 6) return FallbackAuthorColor

    val rgb = normalized.toLongOrNull(radix = 16) ?: return FallbackAuthorColor

    return Color(0xFF000000 or rgb)
}

private val FallbackAuthorColor = Color(0xFF3B82F6)
