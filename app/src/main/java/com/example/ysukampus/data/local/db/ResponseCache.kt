package com.example.ysukampus.data.local.db

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Типизированная обёртка над [CachedResponseDao]: кладёт и достаёт DTO, а не
 * строки, и знает про возраст записи.
 *
 * Про свежесть: Room хранит данные, но ничего не знает о том, когда они
 * протухают, — политику задаёт вызывающий, передавая `maxAge` в [read].
 *
 * Ни один метод не бросает: сбой кэша не должен ронять экран, которому в
 * худшем случае просто придётся сходить в сеть. Битая запись (например, DTO
 * изменился несовместимо) молча удаляется.
 */
class ResponseCache(
    private val dao: CachedResponseDao,
    private val json: Json = defaultJson,
    /** Подменяется в тестах, чтобы не ждать реального TTL. */
    private val now: () -> Long = System::currentTimeMillis
) {

    /**
     * @param maxAge предельный возраст записи. `null` — возраст не важен
     * (offline-fallback: лучше показать вчерашнее, чем пустой экран).
     * @return `null`, если записи нет, она старше [maxAge] или не читается.
     */
    suspend fun <T> read(key: String, serializer: KSerializer<T>, maxAge: Long? = null): T? {
        val entity = runCatching { dao.find(key) }
            .onFailure { Log.w(TAG, "Не удалось прочитать кэш $key", it) }
            .getOrNull() ?: return null

        if (maxAge != null && now() - entity.updatedAt > maxAge) return null

        return runCatching { json.decodeFromString(serializer, entity.payload) }
            .getOrElse { error ->
                Log.w(TAG, "Битая запись кэша $key, удаляю", error)
                runCatching { dao.deleteByPrefix(key) }
                null
            }
    }

    suspend fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        runCatching {
            dao.put(
                CachedResponseEntity(
                    key = key,
                    payload = json.encodeToString(serializer, value),
                    updatedAt = now()
                )
            )
        }.onFailure { Log.w(TAG, "Не удалось записать кэш $key", it) }
    }

    suspend fun evictPrefix(prefix: String) {
        runCatching { dao.deleteByPrefix(prefix) }
            .onFailure { Log.w(TAG, "Не удалось очистить кэш $prefix", it) }
    }

    /** Удаляет всё старше [maxAge]. */
    suspend fun evictOlderThan(maxAge: Long) {
        runCatching { dao.deleteOlderThan(now() - maxAge) }
            .onFailure { Log.w(TAG, "Не удалось подчистить старый кэш", it) }
    }

    /**
     * Уборка, сторожащая размер файла базы: всё, к чему не обращались две
     * недели, не понадобится и дальше — расписание за прошлый месяц не
     * показывается, а тред, открытый один раз, вряд ли откроют снова.
     */
    suspend fun evictStaleEntries() {
        evictOlderThan(STALE_ENTRY_MAX_AGE_MS)
    }

    /** Полный сброс: вызывается при выходе из аккаунта. */
    suspend fun clear() {
        runCatching { dao.clear() }
            .onFailure { Log.w(TAG, "Не удалось очистить кэш", it) }
    }

    companion object {
        private const val TAG = "ResponseCache"

        /** Возраст, после которого запись считается никому не нужной. */
        const val STALE_ENTRY_MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000

        /** Та же конфигурация, что у сетевого слоя: читаем ровно те же DTO. */
        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
