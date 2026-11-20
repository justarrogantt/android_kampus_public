package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Зеркало `app/schemas/schedule_schemas.py` из бэкенда.
 *
 * Поля именуются как в Kotlin, snake_case из JSON привязывается через [SerialName].
 * Общий Json настроен с `ignoreUnknownKeys = true`, поэтому новые поля на бэке
 * не будут ломать разбор ответа.
 */

@Serializable
data class ScheduleMemberDto(
    val name: String,
    val oid: String,
    val faculty: String? = null
)

@Serializable
data class ScheduleDirectoryDto(
    val groups: List<ScheduleMemberDto> = emptyList(),
    val lecturers: List<ScheduleMemberDto> = emptyList()
)

@Serializable
data class ScheduleEntityDto(
    val name: String,
    val type: String,
    val oid: String
)

/**
 * Одна группа из аудитории пары.
 *
 * [label] — как показать («ПИ41б/1»), [name] — имя в справочнике («ПИ41б»),
 * [oid] — идентификатор. `null` в [oid] означает, что группы нет в справочнике
 * и тап по ней не нужен.
 */
@Serializable
data class ScheduleAudienceMemberDto(
    val label: String,
    val name: String,
    val oid: String? = null
)

@Serializable
data class ScheduleAudienceDto(
    val label: String,
    val scope: String,
    /**
     * Те же группы поштучно. По умолчанию пустой список: старые ответы
     * (и закэшированные бэком недели) поля ещё не содержат, и разбор
     * не должен на этом падать.
     */
    val members: List<ScheduleAudienceMemberDto> = emptyList()
)

@Serializable
data class ScheduleLessonDto(
    val discipline: String,
    val kind: String,
    @SerialName("kind_category")
    val kindCategory: String,
    val lecturer: String,
    /** `null` — преподавателя нет в справочнике, тап не нужен. */
    @SerialName("lecturer_oid")
    val lecturerOid: String? = null,
    val auditorium: String,
    val audience: ScheduleAudienceDto
)

@Serializable
data class ScheduleSlotDto(
    val number: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    val lessons: List<ScheduleLessonDto> = emptyList()
)

@Serializable
data class ScheduleDayDto(
    val date: String,
    val slots: List<ScheduleSlotDto> = emptyList()
)

@Serializable
data class ScheduleWeekResponseDto(
    val entity: ScheduleEntityDto,
    val source: String,
    @SerialName("synced_at")
    val syncedAt: Double? = null,
    val days: List<ScheduleDayDto> = emptyList()
)

@Serializable
data class ScheduleDayResponseDto(
    val entity: ScheduleEntityDto,
    val source: String,
    @SerialName("synced_at")
    val syncedAt: Double? = null,
    val day: ScheduleDayDto
)
