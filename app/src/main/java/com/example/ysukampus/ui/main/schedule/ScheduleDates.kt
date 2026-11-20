package com.example.ysukampus.ui.main.schedule

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Работа с датами расписания.
 *
 * Часовой пояс жёстко зафиксирован: университет живёт по Екатеринбургу, и
 * «сегодня» должно совпадать с вебкой независимо от настроек телефона —
 * иначе студент в поездке увидит расписание не того дня.
 */
object ScheduleDates {

    private val universityZone: ZoneId = ZoneId.of("Asia/Yekaterinburg")
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val russianLocale: Locale = Locale.forLanguageTag("ru")

    fun today(): LocalDate = LocalDate.now(universityZone)

    fun todayIso(): String = today().format(isoFormatter)

    fun tomorrowIso(): String = today().plusDays(1).format(isoFormatter)

    /** Прибавляет неделю к ISO-дате — так запрашиваем следующую порцию дней. */
    fun plusWeekIso(dateIso: String): String =
        parse(dateIso)?.plusWeeks(1)?.format(isoFormatter) ?: dateIso

    fun parse(dateIso: String): LocalDate? = runCatching { LocalDate.parse(dateIso, isoFormatter) }.getOrNull()

    /** "Понедельник, 5 мая" — заголовок дня. */
    fun formatDayTitle(dateIso: String): String {
        val date = parse(dateIso) ?: return dateIso
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, russianLocale)
        val month = date.month.getDisplayName(TextStyle.FULL, russianLocale)

        return "${weekday.replaceFirstChar { it.uppercase(russianLocale) }}, ${date.dayOfMonth} $month"
    }

    /** "5 мая" — для плашки диапазона следующей недели. */
    fun formatShortDate(dateIso: String): String {
        val date = parse(dateIso) ?: return dateIso
        val month = date.month.getDisplayName(TextStyle.FULL, russianLocale)

        return "${date.dayOfMonth} $month"
    }

    /**
     * "Иванов Иван Иванович" → "Иванов И. И.".
     * Нужно, чтобы длинное ФИО не растягивало кнопку выбора сущности.
     */
    fun formatLecturerShortName(fullName: String): String {
        val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
        val lastName = parts.getOrNull(0)
        val firstName = parts.getOrNull(1)
        if (lastName == null || firstName == null) return fullName

        val patronymicInitial = parts.getOrNull(2)?.firstOrNull()?.let { " $it." }.orEmpty()

        return "$lastName ${firstName.first()}.$patronymicInitial"
    }
}
