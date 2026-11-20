package com.example.ysukampus.data.forum

import com.example.ysukampus.data.local.db.FakeCachedResponseDao
import com.example.ysukampus.data.local.db.ResponseCache
import com.example.ysukampus.data.network.dto.ForumPostsPageDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Публикация и удаление в форуме чистят кэш вызовом `evictContent()`
 * (см. комментарий в `ForumCache.kt`), и до сих пор это держалось на честном
 * слове репозитория — здесь фиксируется сам контракт: чистится ровно префикс
 * `forum:`, соседние ключи (расписание, уведомления) не трогаются.
 */
class ForumCacheTest {

    private lateinit var dao: FakeCachedResponseDao
    private lateinit var forumCache: ForumCache

    private fun setUp() {
        dao = FakeCachedResponseDao()
        forumCache = ForumCache(ResponseCache(dao))
    }

    @Test
    fun evictContentDeletesForumPrefix() = runBlocking {
        setUp()

        forumCache.evictContent()

        assertEquals(listOf("forum:"), dao.deletedPrefixes)
    }

    @Test
    fun evictContentRemovesForumEntriesButKeepsOtherPrefixes() = runBlocking {
        setUp()
        val rawCache = ResponseCache(dao)
        forumCache.writeFeed(limit = 20, value = ForumPostsPageDto())
        rawCache.write("notifications:first-page:10", String.serializer(), "непричастно")
        rawCache.write("schedule:day:self:2026-09-23", String.serializer(), "тоже непричастно")

        forumCache.evictContent()

        assertNull(forumCache.readFeed(limit = 20, maxAge = null))
        assertEquals("непричастно", rawCache.read("notifications:first-page:10", String.serializer()))
        assertEquals("тоже непричастно", rawCache.read("schedule:day:self:2026-09-23", String.serializer()))
    }
}
