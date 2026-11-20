package com.example.ysukampus.data.schedule

import com.example.ysukampus.data.network.dto.ScheduleAudienceDto
import com.example.ysukampus.data.network.dto.ScheduleAudienceMemberDto
import com.example.ysukampus.data.network.dto.ScheduleDayDto
import com.example.ysukampus.data.network.dto.ScheduleDayResponseDto
import com.example.ysukampus.data.network.dto.ScheduleDirectoryDto
import com.example.ysukampus.data.network.dto.ScheduleEntityDto
import com.example.ysukampus.data.network.dto.ScheduleLessonDto
import com.example.ysukampus.data.network.dto.ScheduleMemberDto
import com.example.ysukampus.data.network.dto.ScheduleSlotDto
import com.example.ysukampus.data.network.dto.ScheduleWeekResponseDto
import com.example.ysukampus.ui.main.schedule.AudienceScope
import com.example.ysukampus.ui.main.schedule.Lesson
import com.example.ysukampus.ui.main.schedule.LessonAudience
import com.example.ysukampus.ui.main.schedule.LessonAudienceMember
import com.example.ysukampus.ui.main.schedule.LessonKind
import com.example.ysukampus.ui.main.schedule.ScheduleDay
import com.example.ysukampus.ui.main.schedule.ScheduleDayResult
import com.example.ysukampus.ui.main.schedule.ScheduleDirectory
import com.example.ysukampus.ui.main.schedule.ScheduleEntity
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType
import com.example.ysukampus.ui.main.schedule.ScheduleSlot
import com.example.ysukampus.ui.main.schedule.ScheduleWeekResult

/** DTO → домен. Единственное место, где строковые коды бэка превращаются в enum'ы. */

internal fun ScheduleEntityDto.toDomain(): ScheduleEntity = ScheduleEntity(
    oid = oid,
    name = name,
    type = ScheduleEntityType.fromApi(type)
)

internal fun ScheduleMemberDto.toDomain(type: ScheduleEntityType): ScheduleEntity = ScheduleEntity(
    oid = oid,
    name = name,
    type = type
)

internal fun ScheduleAudienceMemberDto.toDomain(): LessonAudienceMember = LessonAudienceMember(
    label = label,
    name = name,
    oid = oid?.takeIf { it.isNotBlank() }
)

internal fun ScheduleAudienceDto.toDomain(): LessonAudience = LessonAudience(
    label = label,
    scope = AudienceScope.fromApi(scope),
    members = members.map { it.toDomain() }
)

internal fun ScheduleLessonDto.toDomain(): Lesson = Lesson(
    discipline = discipline,
    kindLabel = kind,
    kind = LessonKind.fromApi(kindCategory),
    lecturer = lecturer,
    lecturerOid = lecturerOid?.takeIf { it.isNotBlank() },
    auditorium = auditorium,
    audience = audience.toDomain()
)

internal fun ScheduleSlotDto.toDomain(): ScheduleSlot = ScheduleSlot(
    number = number,
    startTime = startTime,
    endTime = endTime,
    lessons = lessons.map { it.toDomain() }
)

internal fun ScheduleDayDto.toDomain(): ScheduleDay = ScheduleDay(
    date = date,
    slots = slots.map { it.toDomain() }
)

internal fun ScheduleDayResponseDto.toDomain(): ScheduleDayResult = ScheduleDayResult(
    entity = entity.toDomain(),
    day = day.toDomain()
)

internal fun ScheduleWeekResponseDto.toDomain(): ScheduleWeekResult = ScheduleWeekResult(
    entity = entity.toDomain(),
    days = days.map { it.toDomain() }
)

internal fun ScheduleDirectoryDto.toDomain(): ScheduleDirectory = ScheduleDirectory(
    groups = groups.map { it.toDomain(ScheduleEntityType.Group) },
    lecturers = lecturers.map { it.toDomain(ScheduleEntityType.Lecturer) }
)
