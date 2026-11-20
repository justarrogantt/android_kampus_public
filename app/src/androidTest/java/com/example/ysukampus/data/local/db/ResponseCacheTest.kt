package com.example.ysukampus.data.local.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Проверка кэша на настоящем SQLite, но в памяти: база живёт только на время
 * теста и не оставляет файла на устройстве.
 *
 * Запуск: ./gradlew :app:connectedStagingDebugAndroidTest (нужен подключённый
 * телефон или эмулятор — Room выполняет реальные запросы).
 */
class ResponseCacheTest {

    @Serializable
    private data class Payload(val name: String, val count: Int)

    private lateinit var database: KampusDatabase
    private lateinit var cache: ResponseCache
    private var now = 1_000L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, KampusDatabase::class.java).build()
        cache = ResponseCache(dao = database.cachedResponseDao(), now = { now })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readsBackWhatWasWritten() = runBlocking {
        cache.write("schedule:day:self:2026-09-19", Payload.serializer(), Payload("пары", 4))

        val restored = cache.read("schedule:day:self:2026-09-19", Payload.serializer())

        assertEquals(Payload("пары", 4), restored)
    }

    @Test
    fun missingKeyReturnsNull() = runBlocking {
        assertNull(cache.read("нет такого ключа", Payload.serializer()))
    }

    /** Повторная запись по тому же ключу обязана затирать прошлую, а не падать. */
    @Test
    fun writeReplacesPreviousValue() = runBlocking {
        cache.write("key", Payload.serializer(), Payload("старое", 1))
        cache.write("key", Payload.serializer(), Payload("новое", 2))

        assertEquals(Payload("новое", 2), cache.read("key", Payload.serializer()))
    }

    /** Главное свойство кэша: запись старше maxAge не отдаётся. */
    @Test
    fun entryOlderThanMaxAgeIsNotReturned() = runBlocking {
        cache.write("key", Payload.serializer(), Payload("свежее", 1))
        now += 10_000

        assertNull(cache.read("key", Payload.serializer(), maxAge = 5_000))
        // Без ограничения возраста та же запись по-прежнему доступна:
        // на этом держится показ расписания без сети.
        assertEquals(Payload("свежее", 1), cache.read("key", Payload.serializer()))
    }

    @Test
    fun clearRemovesEverything() = runBlocking {
        cache.write("a", Payload.serializer(), Payload("a", 1))
        cache.write("b", Payload.serializer(), Payload("b", 2))

        cache.clear()

        assertNull(cache.read("a", Payload.serializer()))
        assertNull(cache.read("b", Payload.serializer()))
    }

    @Test
    fun evictPrefixRemovesOnlyMatchingKeys() = runBlocking {
        cache.write("schedule:day:self:2026-09-19", Payload.serializer(), Payload("день", 1))
        cache.write("forum:feed", Payload.serializer(), Payload("лента", 2))

        cache.evictPrefix("schedule:")

        assertNull(cache.read("schedule:day:self:2026-09-19", Payload.serializer()))
        assertEquals(Payload("лента", 2), cache.read("forum:feed", Payload.serializer()))
    }

    /** Битую запись кэш не отдаёт и не роняет вызывающего. */
    @Test
    fun brokenPayloadIsDroppedInsteadOfCrashing() = runBlocking {
        database.cachedResponseDao().put(
            CachedResponseEntity(key = "key", payload = "{не json", updatedAt = now)
        )

        assertNull(cache.read("key", Payload.serializer()))
        // И следующая запись по этому ключу проходит штатно.
        cache.write("key", String.serializer(), "ok")
        assertEquals("ok", cache.read("key", String.serializer()))
    }
}
