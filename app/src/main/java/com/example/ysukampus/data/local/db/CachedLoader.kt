package com.example.ysukampus.data.local.db

/**
 * Общий сценарий «свежий кэш → сеть → протухший кэш как запасной аэродром».
 *
 * 1. Есть запись моложе [freshMaxAge] и не просили обновить — отдаём её,
 *    в сеть не идём вовсе.
 * 2. Иначе запрос; успех — пишем в кэш и отдаём.
 * 3. Запрос не удался, но на диске что-то есть — отдаём это *успехом*.
 *    Показать вчерашние данные лучше, чем заглушку «нет сети»; на экране они
 *    от свежих не отличаются, и это сознательный размен.
 * 4. Ни сети, ни кэша — отдаём ошибку сети, как и раньше.
 *
 * Живёт отдельно от репозиториев, потому что у расписания и форума он один и
 * тот же, а вот обработка ошибок — своя: [fetch] принимает уже готовый
 * `Result`, собранный тем `runApiCatching`, который принят в этом репозитории.
 *
 * @param readCache чтение по ключу; аргумент — предельный возраст, `null` —
 * возраст не важен.
 * @param toDomain превращение закэшированного DTO в доменную модель. Один и тот
 * же для кэша и для сети — расходиться нечему.
 */
suspend fun <D, T> loadCached(
    readCache: suspend (Long?) -> D?,
    writeCache: suspend (D) -> Unit,
    forceRefresh: Boolean = false,
    freshMaxAge: Long,
    fetch: suspend () -> Result<D>,
    toDomain: (D) -> T
): Result<T> {
    if (!forceRefresh) {
        readCache(freshMaxAge)?.let { return Result.success(toDomain(it)) }
    }

    return fetch().fold(
        onSuccess = { dto ->
            writeCache(dto)
            Result.success(toDomain(dto))
        },
        onFailure = { error ->
            val stale = readCache(null)
            if (stale != null) Result.success(toDomain(stale)) else Result.failure(error)
        }
    )
}
