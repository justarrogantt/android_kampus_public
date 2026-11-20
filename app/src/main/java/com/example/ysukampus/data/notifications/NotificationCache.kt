package com.example.ysukampus.data.notifications

import com.example.ysukampus.data.local.db.ResponseCache
import com.example.ysukampus.data.network.dto.NotificationsPageDto

/**
 * Дисковый кэш уведомлений — только первая страница, по тем же причинам, что и
 * у ленты форума (см. `ForumCache`): курсор за её пределами живёт до следующего
 * уведомления.
 */
class NotificationCache(
    private val cache: ResponseCache
) {

    suspend fun readFirstPage(limit: Int, maxAge: Long?): NotificationsPageDto? =
        cache.read(firstPageKey(limit), NotificationsPageDto.serializer(), maxAge)

    suspend fun writeFirstPage(limit: Int, value: NotificationsPageDto) {
        cache.write(firstPageKey(limit), NotificationsPageDto.serializer(), value)
    }

    /**
     * Сброс после отметки о прочтении. В кэше лежит и счётчик непрочитанных;
     * без сброса шторка после переоткрытия снова зажгла бы уже погашенный
     * бейдж и показала прочитанное непрочитанным.
     */
    suspend fun evictAll() {
        cache.evictPrefix(PREFIX)
    }

    private companion object {
        const val PREFIX = "notifications:"

        fun firstPageKey(limit: Int): String = "${PREFIX}first-page:$limit"
    }
}
