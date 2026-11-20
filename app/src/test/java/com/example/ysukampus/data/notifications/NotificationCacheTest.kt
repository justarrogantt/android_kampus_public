package com.example.ysukampus.data.notifications

import com.example.ysukampus.data.local.db.FakeCachedResponseDao
import com.example.ysukampus.data.local.db.ResponseCache
import com.example.ysukampus.data.network.dto.NotificationsPageDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Отметка «прочитано» чистит кэш уведомлений через `evictAll()` — без этого
 * шторка после переоткрытия снова показывает погашенный бейдж непрочитанным
 * (см. комментарий в `NotificationCache.kt`). Контракт: чистится ровно
 * префикс `notifications:`, форум и расписание не трогаются.
 */
class NotificationCacheTest {

    private lateinit var dao: FakeCachedResponseDao
    private lateinit var notificationCache: NotificationCache

    private fun setUp() {
        dao = FakeCachedResponseDao()
        notificationCache = NotificationCache(ResponseCache(dao))
    }

    @Test
    fun evictAllDeletesNotificationsPrefix() = runBlocking {
        setUp()

        notificationCache.evictAll()

        assertEquals(listOf("notifications:"), dao.deletedPrefixes)
    }

    @Test
    fun evictAllRemovesNotificationEntriesButKeepsOtherPrefixes() = runBlocking {
        setUp()
        val rawCache = ResponseCache(dao)
        notificationCache.writeFirstPage(limit = 10, value = NotificationsPageDto())
        rawCache.write("forum:feed:20", String.serializer(), "непричастно")

        notificationCache.evictAll()

        assertNull(notificationCache.readFirstPage(limit = 10, maxAge = null))
        assertEquals("непричастно", rawCache.read("forum:feed:20", String.serializer()))
    }
}
