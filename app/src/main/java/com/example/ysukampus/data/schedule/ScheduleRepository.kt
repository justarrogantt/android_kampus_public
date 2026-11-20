package com.example.ysukampus.data.schedule

import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.local.db.loadCached
import com.example.ysukampus.data.network.dto.ScheduleDayResponseDto
import com.example.ysukampus.data.network.dto.ScheduleDirectoryDto
import com.example.ysukampus.data.network.dto.ScheduleWeekResponseDto
import com.example.ysukampus.ui.main.schedule.ScheduleDayResult
import com.example.ysukampus.ui.main.schedule.ScheduleDirectory
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType
import com.example.ysukampus.ui.main.schedule.ScheduleWeekResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Единственная точка доступа к расписанию.
 *
 * Возвращает [Result], а не бросает исключения: ViewModel не должна знать про
 * Retrofit и HTTP-коды, ей нужен либо результат, либо готовый текст ошибки.
 *
 * Все методы `suspend` — вызывать их можно только из корутины, а значит
 * блокировки главного потока по построению не случится: Retrofit сам уводит
 * запрос на свой пул и возвращает результат в вызывающий контекст.
 */
class ScheduleRepository(
    private val authorizedService: AuthorizedService,
    /**
     * Дисковый кэш. `null` — работаем как раньше, только по сети: так
     * собираются @Preview и тесты, которым Room не нужен.
     */
    private val cache: ScheduleCache? = null
) {
    // Справочник групп/преподов большой и не меняется в рамках сессии — держим в памяти.
    // Mutex, чтобы два одновременных открытия пикера не отправили два запроса.
    private val directoryMutex = Mutex()
    private var cachedDirectory: ScheduleDirectory? = null

    /**
     * @param oid идентификатор группы/препода. `null` — сервер возьмёт сущность
     * из профиля владельца токена (ручка с optional auth).
     * @param forceRefresh игнорировать свежий кэш («Повторить», pull-to-refresh).
     */
    suspend fun getDay(
        date: String,
        oid: String?,
        forceRefresh: Boolean = false
    ): Result<ScheduleDayResult> = loadCached(
        readCache = { maxAge -> cache?.readDay(date, oid, maxAge) },
        writeCache = { dto -> cache?.writeDay(date, oid, dto) },
        forceRefresh = forceRefresh,
        freshMaxAge = DAY_MAX_AGE_MS,
        fetch = {
            runApiCatching {
                authorizedService.getScheduleDay(date = date, group = oid.orNullIfBlank())
            }
        },
        toDomain = ScheduleDayResponseDto::toDomain
    )

    /** @param week любая дата внутри нужной недели, формат `YYYY-MM-DD`. */
    suspend fun getWeek(
        week: String,
        oid: String?,
        forceRefresh: Boolean = false
    ): Result<ScheduleWeekResult> = loadCached(
        readCache = { maxAge -> cache?.readWeek(week, oid, maxAge) },
        writeCache = { dto -> cache?.writeWeek(week, oid, dto) },
        forceRefresh = forceRefresh,
        freshMaxAge = WEEK_MAX_AGE_MS,
        fetch = {
            runApiCatching {
                authorizedService.getScheduleWeek(week = week, group = oid.orNullIfBlank())
            }
        },
        toDomain = ScheduleWeekResponseDto::toDomain
    )

    /**
     * Справочник для пикера. Три уровня: память → диск → сеть. Память живёт до
     * смерти процесса, диск — [DIRECTORY_MAX_AGE_MS].
     */
    suspend fun getDirectory(): Result<ScheduleDirectory> {
        cachedDirectory?.let { return Result.success(it) }

        return directoryMutex.withLock {
            // Пока ждали мьютекс, кэш мог заполнить другой вызов.
            cachedDirectory?.let { return@withLock Result.success(it) }

            loadCached(
                readCache = { maxAge -> cache?.readDirectory(maxAge) },
                writeCache = { dto -> cache?.writeDirectory(dto) },
                forceRefresh = false,
                freshMaxAge = DIRECTORY_MAX_AGE_MS,
                fetch = { runApiCatching { authorizedService.getScheduleDirectory(type = null) } },
                toDomain = ScheduleDirectoryDto::toDomain
            ).onSuccess { cachedDirectory = it }
        }
    }

    /**
     * Что лежит на диске, независимо от возраста и не трогая сеть.
     *
     * Нужно экрану, чтобы нарисовать прошлое расписание сразу при открытии
     * вкладки — вместо скелетона на время запроса. Свежесть подтянет обычный
     * [getDay]/[getWeek], который пойдёт следом.
     */
    suspend fun peekCachedDay(date: String, oid: String?): ScheduleDayResult? =
        cache?.readDay(date, oid, maxAge = null)?.toDomain()

    suspend fun peekCachedWeek(week: String, oid: String?): ScheduleWeekResult? =
        cache?.readWeek(week, oid, maxAge = null)?.toDomain()

    /**
     * Уборка просроченных записей — всего кэша, а не только расписания.
     * Вызывается один раз при открытии вкладки.
     */
    suspend fun pruneStaleCache() {
        cache?.evictStaleEntries()
    }

    private fun String?.orNullIfBlank(): String? = this?.takeIf { it.isNotBlank() }

    /**
     * Общая обработка ошибок сети с повтором.
     *
     * Зачем повтор: ручки расписания — идемпотентные GET, а одиночная сетевая
     * икота или холодная 5xx (бэк ещё не успел сходить за расписанием) раньше
     * сразу превращались в экран ошибки. Повторяем только то, что имеет шанс
     * пройти со следующей попытки: IO-ошибки, таймауты, любые 5xx и 429
     * (бэк просит подождать). Остальные коды — окончательные: 401 разрулит
     * TokenAuthentificator, 404 повтором не исправишь.
     *
     * Каждая попытка ограничена [ATTEMPT_TIMEOUT_MS] через `withTimeoutOrNull`,
     * и это не дубль таймаутов OkHttp: read-таймаут в 30 с оправдан для честно
     * отвечающего бэкенда, но повисший запрос расписания при нём занимал всё
     * окно повторов целиком — вторая попытка просто не успевала случиться.
     * Отмена по таймауту закрывает и сам call: Retrofit-suspend отменяем
     * штатно. Основной темп повторов задаёт ScheduleViewModel (каждые 3 с в
     * первые 30 с), здесь остаётся одна быстрая подстраховка от икоты.
     *
     * `CancellationException` пробрасываем явно: она приходит от отменённого
     * [kotlinx.coroutines.Job] вызывающего (см. loadJob в ScheduleViewModel), и
     * ловить её в общий `catch` нельзя — иначе отменённая загрузка вернёт
     * «ошибку» и зажжёт её в UI уже после ухода со вкладки.
     */
    private suspend fun <T> runApiCatching(block: suspend () -> T): Result<T> {
        var lastError: ScheduleException? = null

        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                // null — только истёкший таймаут: ручки расписания отдают
                // непустой домен, `null` от них прийти не может.
                val value = withTimeoutOrNull(ATTEMPT_TIMEOUT_MS) { block() }
                if (value != null) return Result.success(value)

                lastError = ScheduleException(TIMEOUT_ERROR_MESSAGE)
            } catch (error: CancellationException) {
                throw error
            } catch (error: SocketTimeoutException) {
                // Отдельно от прочих IOException: интернет у пользователя есть,
                // и советовать «проверьте соединение» здесь только сбивает с толку.
                lastError = ScheduleException(TIMEOUT_ERROR_MESSAGE, error)
            } catch (error: IOException) {
                lastError = ScheduleException(NETWORK_ERROR_MESSAGE, error)
            } catch (error: HttpException) {
                val failure = ScheduleException(error.toScheduleMessage(), error)
                if (!error.isRetryable()) return Result.failure(failure)
                lastError = failure
            } catch (error: Exception) {
                // Например, SerializationException на неожиданном теле ответа.
                // Раньше такое летело наружу из viewModelScope и роняло процесс.
                return Result.failure(ScheduleException(DEFAULT_ERROR_MESSAGE, error))
            }

            if (attempt < MAX_ATTEMPTS - 1) {
                delay(RETRY_DELAY_MS shl attempt)
            }
        }

        return Result.failure(lastError ?: ScheduleException(DEFAULT_ERROR_MESSAGE))
    }

    /**
     * Стоит ли повторять запрос. 5xx — состояние бэкенда, а не запроса: оно
     * меняется само. 429 — прямая просьба подождать.
     */
    private fun HttpException.isRetryable(): Boolean =
        code() >= 500 || code() == TOO_MANY_REQUESTS

    private fun HttpException.toScheduleMessage(): String = when (code()) {
        // Бэк отдаёт 503, когда расписание не удалось загрузить и кэша нет.
        // Важно не спутать это с честным «пар нет» — там приходит 200.
        // Прочие 5xx и 429 для пользователя означают то же самое: подожди.
        TOO_MANY_REQUESTS, in 500..599 -> UNAVAILABLE_ERROR_MESSAGE
        404 -> "Группа или преподаватель не найдены в справочнике"
        else -> DEFAULT_ERROR_MESSAGE
    }

    private companion object {
        const val TOO_MANY_REQUESTS = 429

        /**
         * Сколько кэш считается свежим. День перезапрашиваем чаще недели:
         * именно его пользователь смотрит по многу раз за пару, и именно в нём
         * заметнее всего правка в расписании. Справочник — раз в неделю: он
         * большой и меняется раз в семестр.
         */
        const val DAY_MAX_AGE_MS = 15L * 60 * 1000
        const val WEEK_MAX_AGE_MS = 60L * 60 * 1000
        const val DIRECTORY_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

        /**
         * Попыток внутри одного вызова: одна повторная сверх первой. Дальше
         * повторы ведёт вызывающая ViewModel — она держит ровный темп и
         * показывает пользователю скелетон, пока пробует.
         */
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 700L

        /**
         * Потолок на одну попытку. Меньше read-таймаута OkHttp: расписание за
         * это время либо приходит, либо не придёт вовсе, а повисший запрос не
         * должен выедать окно повторов.
         */
        const val ATTEMPT_TIMEOUT_MS = 8_000L

        const val DEFAULT_ERROR_MESSAGE = "Не удалось загрузить расписание"
        const val NETWORK_ERROR_MESSAGE =
            "Не удалось связаться с сервером. Проверьте соединение с интернетом"
        const val UNAVAILABLE_ERROR_MESSAGE =
            "Не удалось загрузить расписание, попробуйте позже"
        const val TIMEOUT_ERROR_MESSAGE =
            "Сервер слишком долго не отвечает. Попробуйте ещё раз"
    }
}

/** Ошибка с готовым для показа текстом. */
class ScheduleException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

/** Тип сущности по роли пользователя — преподавателю показываем расписание преподавателя. */
fun scheduleEntityTypeForRole(role: String?): ScheduleEntityType =
    if (role.equals("teacher", ignoreCase = true)) {
        ScheduleEntityType.Lecturer
    } else {
        ScheduleEntityType.Group
    }
