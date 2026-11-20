package com.example.ysukampus.data.schedule

import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.ui.main.schedule.ScheduleDayResult
import com.example.ysukampus.ui.main.schedule.ScheduleDirectory
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType
import com.example.ysukampus.ui.main.schedule.ScheduleWeekResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val authorizedService: AuthorizedService
) {
    // Справочник групп/преподов большой и не меняется в рамках сессии — держим в памяти.
    // Mutex, чтобы два одновременных открытия пикера не отправили два запроса.
    private val directoryMutex = Mutex()
    private var cachedDirectory: ScheduleDirectory? = null

    /**
     * @param oid идентификатор группы/препода. `null` — сервер возьмёт сущность
     * из профиля владельца токена (ручка с optional auth).
     */
    suspend fun getDay(date: String, oid: String?): Result<ScheduleDayResult> = runApiCatching {
        authorizedService.getScheduleDay(date = date, group = oid.orNullIfBlank()).toDomain()
    }

    /** @param week любая дата внутри нужной недели, формат `YYYY-MM-DD`. */
    suspend fun getWeek(week: String, oid: String?): Result<ScheduleWeekResult> = runApiCatching {
        authorizedService.getScheduleWeek(week = week, group = oid.orNullIfBlank()).toDomain()
    }

    /** Справочник для пикера. Повторные вызовы отдают закэшированный результат. */
    suspend fun getDirectory(): Result<ScheduleDirectory> {
        cachedDirectory?.let { return Result.success(it) }

        return directoryMutex.withLock {
            // Пока ждали мьютекс, кэш мог заполнить другой вызов.
            cachedDirectory?.let { return@withLock Result.success(it) }

            runApiCatching {
                authorizedService.getScheduleDirectory(type = null).toDomain()
            }.onSuccess { cachedDirectory = it }
        }
    }

    private fun String?.orNullIfBlank(): String? = this?.takeIf { it.isNotBlank() }

    /**
     * Общая обработка ошибок сети с повтором.
     *
     * Зачем повтор: ручки расписания — идемпотентные GET, а одиночная сетевая
     * икота или холодный 503 (бэк ещё не успел сходить за расписанием) раньше
     * сразу превращались в экран ошибки. Повторяем только то, что имеет шанс
     * пройти со второй попытки: IO-ошибки и 503. Остальные коды — окончательные.
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
                return Result.success(block())
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
                if (error.code() != SERVICE_UNAVAILABLE) return Result.failure(failure)
                lastError = failure
            } catch (error: Exception) {
                // Например, SerializationException на неожиданном теле ответа.
                // Раньше такое летело наружу из viewModelScope и роняло процесс.
                return Result.failure(ScheduleException(DEFAULT_ERROR_MESSAGE, error))
            }

            if (attempt < MAX_ATTEMPTS - 1) {
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }

        return Result.failure(lastError ?: ScheduleException(DEFAULT_ERROR_MESSAGE))
    }

    private fun HttpException.toScheduleMessage(): String = when (code()) {
        // Бэк отдаёт 503, когда расписание не удалось загрузить и кэша нет.
        // Важно не спутать это с честным «пар нет» — там приходит 200.
        SERVICE_UNAVAILABLE -> UNAVAILABLE_ERROR_MESSAGE
        404 -> "Группа или преподаватель не найдены в справочнике"
        else -> DEFAULT_ERROR_MESSAGE
    }

    private companion object {
        const val SERVICE_UNAVAILABLE = 503

        /** Одна повторная попытка сверх первой: больше — это уже ожидание на минуту. */
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 700L

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
