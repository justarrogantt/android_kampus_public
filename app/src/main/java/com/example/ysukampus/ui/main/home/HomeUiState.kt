package com.example.ysukampus.ui.main.home

import com.example.ysukampus.ui.main.forum.ForumPost
import com.example.ysukampus.ui.main.schedule.Lesson
import com.example.ysukampus.ui.main.schedule.ScheduleDay
import com.example.ysukampus.ui.main.schedule.ScheduleSlot
import com.example.ysukampus.ui.main.schedule.ScheduleWeekResult

/**
 * Снимок главной целиком.
 *
 * Три блока грузятся независимо (погода, неделя расписания, посты форума),
 * поэтому у каждого свои флаги загрузки и ошибки: медленная погода не держит
 * скелетон над расписанием, а упавшая лента не прячет ближайшую пару.
 */
data class HomeUiState(
    /** Имя из кэша профиля. Пусто — приветствуем без имени. */
    val greetingName: String = "",

    val weather: Weather? = null,

    /** Текущая неделя: из неё берутся и полоска дней, и ближайшая пара. */
    val week: ScheduleWeekResult? = null,
    val isWeekLoading: Boolean = true,
    /** Текст ошибки расписания — вместо полоски и карточки показываем повтор. */
    val weekError: String? = null,

    val posts: List<ForumPost> = emptyList(),
    val isPostsLoading: Boolean = true,
    val postsError: String? = null,

    /** Пост, для которого открыто подтверждение жалобы/удаления. */
    val reportPostId: String? = null,
    val deletePostId: String? = null,

    /**
     * Текущее время в минутах от полуночи. Живёт в состоянии, а не читается в
     * геттере: иначе «ближайшая пара» пересчитывалась бы только при других
     * изменениях состояния и не сменилась бы сама, когда пара уже началась.
     */
    val nowMinutes: Int = HomeDates.nowMinutes(),

    /** Разовое сообщение для снекбара. Экран гасит его через onMessageShown(). */
    val message: String? = null
) {
    /** Пустой список постов, когда загрузка уже закончилась без ошибки. */
    val isPostsEmpty: Boolean
        get() = !isPostsLoading && postsError == null && posts.isEmpty()

    val days: List<ScheduleDay>
        get() = week?.days.orEmpty()

    private val today: ScheduleDay?
        get() = days.firstOrNull { it.date == HomeDates.todayIso }

    /** Пары сегодня были, даже если все уже прошли. */
    val hasLessonsToday: Boolean
        get() = (today?.slots?.size ?: 0) > 0

    /** Первая пара, которая ещё не началась. */
    val nextSlot: ScheduleSlot?
        get() = today?.slots?.firstOrNull { slot ->
            HomeDates.timeToMinutes(slot.startTime) > nowMinutes
        }

    /** Занятие ближайшей пары. У подгрупп их несколько — показываем первое, как вебка. */
    val nextLesson: Lesson?
        get() = nextSlot?.lessons?.firstOrNull()
}
