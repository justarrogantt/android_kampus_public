package com.example.ysukampus.ui.main.home

import com.example.ysukampus.ui.main.schedule.ScheduleDates
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale

/**
 * Даты и время главной.
 *
 * Часовой пояс тот же, что в расписании ([ScheduleDates]): «сегодня» и «через
 * сколько» должны совпадать с вебкой независимо от настроек телефона.
 */
object HomeDates {

    private val universityZone: ZoneId = ZoneId.of("Asia/Yekaterinburg")
    private val russianLocale: Locale = Locale.forLanguageTag("ru")
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Сегодняшняя дата в формате ГГГГ-ММ-ДД — как в расписании. */
    val todayIso: String
        get() = ScheduleDates.todayIso()

    /** «Вторник, 25 марта» — подпись под приветствием. */
    fun formatGreetingDate(date: LocalDate = ScheduleDates.today()): String {
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, russianLocale)
        val month = date.month.getDisplayName(TextStyle.FULL, russianLocale)

        return "${weekday.replaceFirstChar { it.uppercase(russianLocale) }}, ${date.dayOfMonth} $month"
    }

    /** «ПН» — сокращение дня недели для полоски. */
    fun formatWeekdayShort(dateIso: String): String {
        val date = ScheduleDates.parse(dateIso) ?: return ""

        return date.dayOfWeek
            .getDisplayName(TextStyle.SHORT_STANDALONE, russianLocale)
            .replace(".", "")
            .uppercase(russianLocale)
    }

    fun dayOfMonth(dateIso: String): Int = ScheduleDates.parse(dateIso)?.dayOfMonth ?: 0

    /** Текущее время университета — от него считаются «ближайшая пара» и обратный отсчёт. */
    fun nowTime(): LocalTime = LocalTime.now(universityZone)

    /** Текущее время в минутах от полуночи. */
    fun nowMinutes(): Int = nowTime().let { it.hour * 60 + it.minute }

    /** «08:30» → минуты от полуночи. Некорректное время уводим в конец суток. */
    fun timeToMinutes(time: String): Int = parseTime(time)?.let { it.hour * 60 + it.minute } ?: Int.MAX_VALUE

    /**
     * Сколько секунд осталось до начала пары. Отрицательное значение (пара уже
     * началась) сворачивается в ноль — так же, как `Math.max(0, ...)` в вебке.
     */
    fun secondsUntil(startTime: String): Long {
        val start = parseTime(startTime) ?: return 0
        val seconds = Duration.between(nowTime(), start).seconds

        return seconds.coerceAtLeast(0)
    }

    private fun parseTime(time: String): LocalTime? =
        runCatching { LocalTime.parse(time, timeFormatter) }.getOrNull()

    /**
     * «12 мин.», «3 ч.» — возраст поста форума.
     *
     * Бэк отдаёт наивное UTC-время без суффикса `Z`, поэтому его приходится
     * дописывать: иначе пост «из будущего» на пять часов. Вебка делает то же самое.
     */
    fun forumAgeMinutes(createdAtIso: String): Long {
        val normalized = if (createdAtIso.endsWith("Z")) createdAtIso else "${createdAtIso}Z"
        val created = runCatching { Instant.parse(normalized) }.getOrElse { error ->
            if (error is DateTimeParseException) return 0 else throw error
        }

        return Duration.between(created, Instant.now()).toMinutes().coerceAtLeast(0)
    }
}
