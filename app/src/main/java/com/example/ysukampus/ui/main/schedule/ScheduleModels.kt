package com.example.ysukampus.ui.main.schedule

/**
 * Доменные модели расписания.
 *
 * Отличие от DTO: сырые строки бэка (`kind_category`, `scope`, `type`) свёрнуты
 * в enum'ы. Благодаря этому `when` в Compose получается исчерпывающим, и
 * незнакомое значение с сервера деградирует в [LessonKind.Other], а не роняет UI.
 */

enum class LessonKind {
    Lecture,
    Lab,
    Practice,
    Exam,
    Credit,
    Retake,
    Other;

    companion object {
        fun fromApi(raw: String): LessonKind = when (raw) {
            "lecture" -> Lecture
            "lab" -> Lab
            "practice" -> Practice
            "exam" -> Exam
            "credit" -> Credit
            "retake" -> Retake
            else -> Other
        }
    }
}

enum class AudienceScope {
    Group,
    Subgroup,
    Stream,
    Unknown;

    companion object {
        fun fromApi(raw: String): AudienceScope = when (raw) {
            "group" -> Group
            "subgroup" -> Subgroup
            "stream" -> Stream
            else -> Unknown
        }
    }
}

enum class ScheduleEntityType {
    Group,
    Lecturer;

    companion object {
        fun fromApi(raw: String): ScheduleEntityType =
            if (raw == "lecturer") Lecturer else Group
    }
}

/** Группа или преподаватель, чьё расписание показываем. */
data class ScheduleEntity(
    val oid: String,
    val name: String,
    val type: ScheduleEntityType
)

/**
 * Одна группа из аудитории пары.
 *
 * [label] — как показать («ПИ41б/1»), [name] — имя в справочнике («ПИ41б»).
 * [oid] `null` — группы нет в справочнике, её расписание открыть нельзя.
 */
data class LessonAudienceMember(
    val label: String,
    val name: String,
    val oid: String?
)

data class LessonAudience(
    val label: String,
    val scope: AudienceScope,
    /**
     * Те же группы поштучно. Пустой список — ответ пришёл со старого бэка
     * или из его кэша; карточка в этом случае показывает готовый [label].
     */
    val members: List<LessonAudienceMember> = emptyList()
)

data class Lesson(
    val discipline: String,
    /** Сырой тип с сервера — его и показываем пользователю строкой. */
    val kindLabel: String,
    val kind: LessonKind,
    val lecturer: String,
    /** `null` — преподавателя нет в справочнике, его расписание открыть нельзя. */
    val lecturerOid: String? = null,
    val auditorium: String,
    val audience: LessonAudience
)

/** Временной слот (пара). Может содержать несколько занятий сразу — например, подгруппы. */
data class ScheduleSlot(
    val number: String,
    val startTime: String,
    val endTime: String,
    val lessons: List<Lesson>
)

/** Один день: ISO-дата и список пар. Пустой список слотов — «пар нет». */
data class ScheduleDay(
    val date: String,
    val slots: List<ScheduleSlot>
)

/** Ответ на день целиком: сама дата плюс сущность, для которой она пришла. */
data class ScheduleDayResult(
    val entity: ScheduleEntity,
    val day: ScheduleDay
)

/** Ответ на неделю. `days` может расти при догрузке следующей недели. */
data class ScheduleWeekResult(
    val entity: ScheduleEntity,
    val days: List<ScheduleDay>
)

/** Справочник для пикера. */
data class ScheduleDirectory(
    val groups: List<ScheduleEntity>,
    val lecturers: List<ScheduleEntity>
)
