package com.example.ysukampus.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Одна закэшированная выдача сервера.
 *
 * Почему одна таблица со строкой JSON, а не отдельная схема под каждую сущность
 * (`lessons`, `slots`, `groups` с внешними ключами): кэшу не нужны запросы по
 * полям — он всегда читается целиком по ключу, которым и является сам запрос
 * («день такой-то для такого-то oid»). Нормализованная схема дала бы те же
 * данные ценой десятка таблиц, ручного маппинга в обе стороны и миграции на
 * каждое изменение ответа бэкенда. Здесь же в [payload] лежит ровно тот DTO,
 * который уже умеет разбирать `kotlinx.serialization`, и с `ignoreUnknownKeys`
 * новые поля бэка не ломают ни разбор, ни схему базы.
 *
 * Обратная сторона решения: выбрать «все пары по преподавателю X» из кэша
 * нельзя. Понадобится такое — это уже не кэш, а локальная модель данных, и её
 * надо описывать нормальными сущностями.
 */
@Entity(tableName = "cached_responses")
data class CachedResponseEntity(
    /** Ключ запроса, см. `ScheduleCacheKeys`. `key` — слово SQL, отсюда имя колонки. */
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val key: String,

    /** Сериализованный DTO. */
    @ColumnInfo(name = "payload")
    val payload: String,

    /** `System.currentTimeMillis()` момента записи — по нему считается возраст. */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Dao
interface CachedResponseDao {

    @Query("SELECT * FROM cached_responses WHERE cache_key = :key LIMIT 1")
    suspend fun find(key: String): CachedResponseEntity?

    /**
     * REPLACE, а не ABORT: повторная загрузка того же дня должна затирать
     * прошлую запись, а не падать на конфликте первичного ключа.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CachedResponseEntity)

    /** Сброс группы ключей — например, всего расписания при смене сущности. */
    @Query("DELETE FROM cached_responses WHERE cache_key LIKE :prefix || '%'")
    suspend fun deleteByPrefix(prefix: String)

    /** Чистка по возрасту, чтобы база не росла бесконечно. */
    @Query("DELETE FROM cached_responses WHERE updated_at < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("DELETE FROM cached_responses")
    suspend fun clear()
}
