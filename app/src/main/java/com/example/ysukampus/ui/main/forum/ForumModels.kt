package com.example.ysukampus.ui.main.forum

/**
 * Доменные модели форума.
 *
 * Пока используются только главной («Сейчас между парами»), но лежат в пакете
 * форума: вкладка «Форум» будет переиспользовать и модель, и карточку поста.
 */

/**
 * Статус поста или комментария — на бэке это один `ContentStatus`.
 * Лента и тред отдают только публичные, остальное — на будущее.
 */
enum class ForumPostStatus {
    Active,
    Review,
    Hidden,
    Pending,
    Deleted,
    Unknown;

    companion object {
        fun fromApi(raw: String): ForumPostStatus = when (raw) {
            "active" -> Active
            "review" -> Review
            "hidden" -> Hidden
            "pending" -> Pending
            "deleted" -> Deleted
            else -> Unknown
        }
    }
}

data class ForumPost(
    val id: String,
    val profileId: String?,
    val displayName: String,
    /** Цвет автора в формате `#rrggbb` — им же красится бейдж роли и аватар-заглушка. */
    val displayColor: String,
    /** Готовый абсолютный URL аватара либо `null` — тогда рисуем иконку-заглушку. */
    val avatarImageUrl: String?,
    val visibleRole: String?,
    val isMine: Boolean,
    val content: String,
    val status: ForumPostStatus,
    val likesCount: Int,
    val commentsCount: Int,
    val likedByMe: Boolean,
    /** ISO-время публикации (или создания, если пост ещё не публиковался). */
    val createdAtIso: String,
    /** Готовый абсолютный URL оригинала вложенной картинки либо `null`. */
    val imageUrl: String? = null,
    /** Готовый абсолютный URL миниатюры — её показывают карточки. */
    val imagePreviewUrl: String? = null,
    /** Размеры оригинала: по ним карточка держит пропорции до загрузки. */
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
) {
    /** У поста есть картинка, которую можно показать и открыть на весь экран. */
    val hasImage: Boolean
        get() = imagePreviewUrl != null && imageUrl != null
}

/**
 * Комментарий поста.
 *
 * Ответы приходят плоским списком: у ответа на другой комментарий заполнен
 * [parentId], у комментария первого уровня он `null`.
 */
data class ForumComment(
    val id: String,
    val postId: String,
    val profileId: String?,
    val displayName: String,
    /** Цвет автора в формате `#rrggbb` — им же красится бейдж роли и аватар-заглушка. */
    val displayColor: String,
    /** Готовый абсолютный URL аватара либо `null` — тогда рисуем иконку-заглушку. */
    val avatarImageUrl: String?,
    val visibleRole: String?,
    val isMine: Boolean,
    /** Автор комментария — автор поста: тред помечает его отдельно. */
    val isPostAuthor: Boolean,
    val content: String,
    val parentId: String?,
    val status: ForumPostStatus,
    val likesCount: Int,
    val repliesCount: Int,
    val likedByMe: Boolean,
    /** ISO-время публикации (или создания, если комментарий ещё не публиковался). */
    val createdAtIso: String,
    /** Готовый абсолютный URL оригинала вложенной картинки либо `null`. */
    val imageUrl: String? = null,
    /** Готовый абсолютный URL миниатюры — её показывают карточки. */
    val imagePreviewUrl: String? = null,
    /** Размеры оригинала: по ним карточка держит пропорции до загрузки. */
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
) {
    val hasImage: Boolean
        get() = imagePreviewUrl != null && imageUrl != null
}

/**
 * Пост или комментарий, в ответ на который написан комментарий пользователя.
 * Показывается строкой контекста над карточкой в «Моих постах».
 */
data class ForumActivityContext(
    val id: String,
    val displayName: String,
    val content: String
)

/**
 * Свой комментарий в «Моих постах»: сам комментарий и то, к чему он написан.
 *
 * [replyContext] заполнен только у ответа на другой комментарий — у
 * комментария первого уровня контекст один, пост.
 */
data class ForumActivityComment(
    val comment: ForumComment,
    val postContext: ForumActivityContext,
    val replyContext: ForumActivityContext? = null
)
