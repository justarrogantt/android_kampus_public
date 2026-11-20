package com.example.ysukampus.ui.main.profile.activity

import java.time.Instant
import java.time.ZoneId

/** Дата регистрации в шапке профиля: «июня 2026», как в вебке. */
object MyProfileDates {

    private val universityZone: ZoneId = ZoneId.of("Asia/Yekaterinburg")

    private val months = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    /**
     * @param createdAtIso время из `forum/user-profile`. Бэк отдаёт UTC без
     * суффикса `Z` — как и в остальном форуме, дописываем его сами.
     */
    fun formatRegistration(createdAtIso: String): String {
        val normalized = if (createdAtIso.endsWith("Z")) createdAtIso else "${createdAtIso}Z"
        val instant = runCatching { Instant.parse(normalized) }.getOrNull() ?: return ""
        val date = instant.atZone(universityZone).toLocalDate()

        return "${months[date.monthValue - 1]} ${date.year}"
    }
}
