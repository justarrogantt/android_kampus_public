package com.example.ysukampus.ui.main.notifications

import com.example.ysukampus.data.notifications.Notification

/**
 * Снимок уведомлений.
 *
 * Состояние общее для всей авторизованной зоны: бейдж на колокольчике
 * расписания и на пункте профиля читают те же счётчики, что и открытая шторка.
 * Первая загрузка и догрузка страницы разведены так же, как в ленте форума
 * ([com.example.ysukampus.ui.main.forum.ForumUiState]).
 */
data class NotificationsUiState(
    val items: List<Notification> = emptyList(),

    /** Первая загрузка: список пуст, рисуем скелетон. */
    val isLoading: Boolean = true,
    /** Идёт запрос следующей страницы. */
    val isLoadingMore: Boolean = false,
    /** Идёт массовая отметка «прочитать все». */
    val isReadingAll: Boolean = false,
    /** Ошибка первой загрузки — вместо списка показываем повтор. */
    val error: String? = null,

    /** Курсор следующей страницы. `null` — уведомления кончились. */
    val nextCursor: String? = null,

    /** Непрочитанные по всему аккаунту, а не среди загруженных карточек. */
    val unreadCount: Int = 0,
    /** Размер очереди модерации; 0 у тех, у кого нет права её разбирать. */
    val moderationCount: Int = 0,

    /** Открыта ли шторка. Закрытие не сбрасывает загруженный список. */
    val isSheetOpen: Boolean = false,

    /** Запрос на открытие треда — его исполняет оболочка, а не шторка. */
    val openTarget: NotificationOpenTarget? = null,

    /** Разовое сообщение для снекбара. */
    val message: String? = null
) {
/**
     * Показывать пустое состояние.
     *
     * Строка очереди модерации — тоже контент: пока она на экране, «здесь
     * пока ничего нет» не показываем (так же ведёт себя вебка).
     */
    val isEmpty: Boolean
        get() = !isLoading && error == null && items.isEmpty() && moderationCount == 0

    val canLoadMore: Boolean
        get() = nextCursor != null && !isLoading && !isLoadingMore

    /** Есть что показать на колокольчике: непрочитанные или очередь модерации. */
    val hasBadge: Boolean
        get() = unreadCount > 0 || moderationCount > 0

    /**
     * Подпись бейджа. Для очереди модерации, как и в вебке, показываем только
     * точку: её размер живёт на своей строке внутри шторки.
     */
    val badgeLabel: String
        get() = when {
            unreadCount == 0 -> ""
            unreadCount > 9 -> "9+"
            else -> unreadCount.toString()
        }
}

/** Куда уйти по тапу: пост и комментарий внутри него. */
data class NotificationOpenTarget(
    val postId: String,
    val commentId: String
)
