package com.example.ysukampus.data.schedule

import android.content.Context
import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.local.db.DatabaseProvider
import com.example.ysukampus.data.local.db.ResponseCache

/**
 * Сборка [ScheduleRepository] с дисковым кэшем.
 *
 * Отдельная функция, а не конструктор по месту: репозиторий создают три
 * фабрики (расписание, главная, профиль), и все три должны получать кэш поверх
 * одного и того же экземпляра базы — иначе смысла в нём нет.
 */
fun createScheduleRepository(
    context: Context,
    authorizedService: AuthorizedService
): ScheduleRepository = ScheduleRepository(
    authorizedService = authorizedService,
    cache = ScheduleCache(ResponseCache(DatabaseProvider.cachedResponseDao(context)))
)
