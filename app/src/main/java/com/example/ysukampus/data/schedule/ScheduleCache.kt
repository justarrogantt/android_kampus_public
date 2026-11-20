package com.example.ysukampus.data.schedule

import com.example.ysukampus.data.local.db.ResponseCache
import com.example.ysukampus.data.network.dto.ScheduleDayResponseDto
import com.example.ysukampus.data.network.dto.ScheduleDirectoryDto
import com.example.ysukampus.data.network.dto.ScheduleWeekResponseDto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Дисковый кэш расписания поверх [ResponseCache].
 *
 * Хранит именно DTO, а не доменные модели: DTO уже `@Serializable`, а в домен
 * они превращаются теми же `toDomain()`, что и ответ сети (`ScheduleMappers`).
 * Так у кэша и у сети один и тот же путь разбора — расходиться нечему.
 */
class ScheduleCache(
    private val cache: ResponseCache
) {

    suspend fun readDay(date: String, oid: String?, maxAge: Long?): ScheduleDayResponseDto? =
        cache.read(dayKey(date, oid), ScheduleDayResponseDto.serializer(), maxAge)

    suspend fun writeDay(date: String, oid: String?, value: ScheduleDayResponseDto) {
        cache.write(dayKey(date, oid), ScheduleDayResponseDto.serializer(), value)
    }

    suspend fun readWeek(week: String, oid: String?, maxAge: Long?): ScheduleWeekResponseDto? =
        cache.read(weekKey(week, oid), ScheduleWeekResponseDto.serializer(), maxAge)

    suspend fun writeWeek(week: String, oid: String?, value: ScheduleWeekResponseDto) {
        cache.write(weekKey(week, oid), ScheduleWeekResponseDto.serializer(), value)
    }

    suspend fun readDirectory(maxAge: Long?): ScheduleDirectoryDto? =
        cache.read(DIRECTORY_KEY, ScheduleDirectoryDto.serializer(), maxAge)

    suspend fun writeDirectory(value: ScheduleDirectoryDto) {
        cache.write(DIRECTORY_KEY, ScheduleDirectoryDto.serializer(), value)
    }

    /**
     * Уборка всего кэша по возрасту — не только расписания.
     *
     * Живёт здесь потому, что зовут её с вкладки расписания, самой частой
     * точки входа в приложение; чистит она при этом общую таблицу, включая
     * форум и уведомления. Чистка по возрасту, а не по префиксу: она сторожит
     * размер файла базы, и выделять кому-то исключения значит оставлять в нём
     * именно те записи, которыми никто не пользуется.
     */
    suspend fun evictStaleEntries() {
        cache.evictStaleEntries()
    }

    private companion object {
        const val PREFIX = "schedule:"
        const val DIRECTORY_KEY = PREFIX + "directory"

        val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        /**
         * `null` в oid означает «сущность владельца токена» — у запроса нет
         * параметра `group`, и сервер берёт группу из профиля. В ключе это
         * отдельное значение `self`; кэш разных аккаунтов на одном устройстве
         * не смешивается, потому что при выходе база чистится целиком
         * (см. `SessionManagerProvider`).
         */
        fun owner(oid: String?): String = oid?.takeIf { it.isNotBlank() } ?: "self"

        fun dayKey(date: String, oid: String?): String = "${PREFIX}day:${owner(oid)}:$date"

        /**
         * Неделя нормализуется до понедельника: запросы за вторник и за среду
         * одной недели возвращают одно и то же, и держать под них две записи
         * значило бы промахиваться мимо кэша каждый день.
         */
        fun weekKey(week: String, oid: String?): String {
            val normalized = runCatching {
                LocalDate.parse(week, isoFormatter)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .format(isoFormatter)
            }.getOrDefault(week)

            return "${PREFIX}week:${owner(oid)}:$normalized"
        }
    }
}
