package com.example.ysukampus.ui.main.forum

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.home.HomeDates

/** «только что», «12 мин.», «3 ч.» — возраст поста, как в вебке. */
@Composable
fun forumTimeAgo(createdAtIso: String): String {
    val minutes = HomeDates.forumAgeMinutes(createdAtIso)
    if (minutes < 1) return stringResource(R.string.forum_time_now)
    if (minutes < 60) return stringResource(R.string.forum_time_minutes, minutes.toInt())

    val hours = minutes / 60
    if (hours < 24) return stringResource(R.string.forum_time_hours, hours.toInt())

    val days = hours / 24
    if (days < 30) return stringResource(R.string.forum_time_days, days.toInt())
    if (days < 365) return stringResource(R.string.forum_time_months, (days / 30).toInt())

    return stringResource(R.string.forum_time_years, (days / 365).toInt())
}
