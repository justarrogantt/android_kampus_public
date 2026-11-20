package com.example.ysukampus.data.forum

import android.content.Context
import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.local.db.DatabaseProvider
import com.example.ysukampus.data.local.db.ResponseCache

/**
 * Сборка [ForumRepository] с дисковым кэшем — по образцу
 * `createScheduleRepository`: репозиторий создают три фабрики, и все они должны
 * работать поверх одного экземпляра базы.
 */
fun createForumRepository(
    context: Context,
    authorizedService: AuthorizedService
): ForumRepository {
    val appContext = context.applicationContext
    return ForumRepository(
        context = appContext,
        authorizedService = authorizedService,
        cache = ForumCache(ResponseCache(DatabaseProvider.cachedResponseDao(appContext)))
    )
}
