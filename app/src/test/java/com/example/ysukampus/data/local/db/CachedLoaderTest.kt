package com.example.ysukampus.data.local.db

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Политика кэширования отдельно от Room: [loadCached] — чистая функция, и её
 * четыре ветки проверяются обычным JVM-тестом без устройства.
 */
class CachedLoaderTest {

    private class FakeCache(var value: String? = null, var isFresh: Boolean = true) {
        var writes = 0
            private set

        val read: suspend (Long?) -> String? = { maxAge ->
            value?.takeIf { maxAge == null || isFresh }
        }
        val write: suspend (String) -> Unit = { new ->
            value = new
            isFresh = true
            writes++
        }
    }

    @Test
    fun freshCacheIsReturnedWithoutNetwork() = runBlocking {
        val cache = FakeCache(value = "с диска")
        var fetched = false

        val result = loadCached(
            readCache = cache.read,
            writeCache = cache.write,
            freshMaxAge = 1_000,
            fetch = { fetched = true; Result.success("из сети") },
            toDomain = { it }
        )

        assertEquals("с диска", result.getOrNull())
        assertFalse("Свежий кэш не должен ходить в сеть", fetched)
    }

    @Test
    fun staleCacheTriggersNetworkAndIsOverwritten() = runBlocking {
        val cache = FakeCache(value = "старое", isFresh = false)

        val result = loadCached(
            readCache = cache.read,
            writeCache = cache.write,
            freshMaxAge = 1_000,
            fetch = { Result.success("свежее") },
            toDomain = { it }
        )

        assertEquals("свежее", result.getOrNull())
        assertEquals("свежее", cache.value)
        assertEquals(1, cache.writes)
    }

    @Test
    fun forceRefreshIgnoresFreshCache() = runBlocking {
        val cache = FakeCache(value = "с диска")

        val result = loadCached(
            readCache = cache.read,
            writeCache = cache.write,
            forceRefresh = true,
            freshMaxAge = 1_000,
            fetch = { Result.success("из сети") },
            toDomain = { it }
        )

        assertEquals("из сети", result.getOrNull())
    }

    /** Ради этой ветки всё и затевалось: офлайн показываем старое, а не ошибку. */
    @Test
    fun networkFailureFallsBackToStaleCacheAsSuccess() = runBlocking {
        val cache = FakeCache(value = "старое", isFresh = false)

        val result = loadCached(
            readCache = cache.read,
            writeCache = cache.write,
            freshMaxAge = 1_000,
            fetch = { Result.failure(IllegalStateException("нет сети")) },
            toDomain = { it }
        )

        assertTrue(result.isSuccess)
        assertEquals("старое", result.getOrNull())
        assertEquals("Провалившийся запрос не должен трогать кэш", 0, cache.writes)
    }

    @Test
    fun networkFailureWithoutCacheIsAFailure() = runBlocking {
        val cache = FakeCache(value = null)

        val result = loadCached(
            readCache = cache.read,
            writeCache = cache.write,
            freshMaxAge = 1_000,
            fetch = { Result.failure(IllegalStateException("нет сети")) },
            toDomain = { it }
        )

        assertTrue(result.isFailure)
        assertEquals("нет сети", result.exceptionOrNull()?.message)
    }

    @Test
    fun domainMappingIsAppliedToBothPaths() = runBlocking {
        val fromNetwork = loadCached(
            readCache = { null },
            writeCache = {},
            freshMaxAge = 1_000,
            fetch = { Result.success("12") },
            toDomain = { it.toInt() }
        )
        val fromCache = loadCached(
            readCache = { "34" },
            writeCache = {},
            freshMaxAge = 1_000,
            fetch = { Result.failure(IllegalStateException()) },
            toDomain = { it.toInt() }
        )

        assertEquals(12, fromNetwork.getOrNull())
        assertEquals(34, fromCache.getOrNull())
    }
}
