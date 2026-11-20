package com.example.ysukampus.data.notifications

import android.content.Context
import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.local.db.DatabaseProvider
import com.example.ysukampus.data.local.db.ResponseCache

/** Сборка [NotificationRepository] с дисковым кэшем — как `createForumRepository`. */
fun createNotificationRepository(
    context: Context,
    authorizedService: AuthorizedService
): NotificationRepository {
    val appContext = context.applicationContext
    return NotificationRepository(
        authorizedService = authorizedService,
        cache = NotificationCache(ResponseCache(DatabaseProvider.cachedResponseDao(appContext)))
    )
}
