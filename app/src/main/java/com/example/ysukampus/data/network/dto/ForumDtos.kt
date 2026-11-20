package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Зеркало форумных схем из `app/schemas/forum_schemas.py`.
 *
 * Перенесена та часть, которая нужна главной и вкладке «Форум»: лента постов,
 * действия над постом и комментарии треда.
 */

@Serializable
data class ForumPostDto(
    val id: String,
    @SerialName("profile_id")
    val profileId: String? = null,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("display_color")
    val displayColor: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("visible_role")
    val visibleRole: String? = null,
    @SerialName("is_mine")
    val isMine: Boolean = false,
    val content: String,
    val status: String,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("comments_count")
    val commentsCount: Int = 0,
    @SerialName("liked_by_me")
    val likedByMe: Boolean = false,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("published_at")
    val publishedAt: String? = null,
    /** Оригинал картинки (`forum/media/{id}`); `null` — вложения нет. */
    @SerialName("image_url")
    val imageUrl: String? = null,
    /** Миниатюра (`forum/media/{id}?size=preview`) — её и показывает лента. */
    @SerialName("image_preview_url")
    val imagePreviewUrl: String? = null,
    /** Размеры оригинала: по ним карточка держит пропорции до загрузки. */
    @SerialName("image_width")
    val imageWidth: Int? = null,
    @SerialName("image_height")
    val imageHeight: Int? = null
)

@Serializable
data class ForumPostsPageDto(
    val items: List<ForumPostDto> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null
)

/** Результат переключения лайка: итоговое состояние и счётчик с сервера. */
@Serializable
data class ForumReactionDto(
    val liked: Boolean,
    val count: Int
)

/**
 * Тело запроса на создание поста (бэк ограничивает длину 500 символами).
 *
 * Текст и картинка по отдельности необязательны, но пост без обоих бэк
 * отклоняет, поэтому [content] по умолчанию пустой.
 */
@Serializable
data class CreateForumPostRequestDto(
    val content: String = "",
    /** Id вложения из `POST forum/media`; привязывается к посту при публикации. */
    @SerialName("image_id")
    val imageId: String? = null
)

/**
 * Комментарий поста — зеркало `CommentResponse`.
 *
 * `parentId` заполнен у ответов на другой комментарий; у комментария первого
 * уровня он `null`.
 */
@Serializable
data class ForumCommentDto(
    val id: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("profile_id")
    val profileId: String? = null,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("display_color")
    val displayColor: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("visible_role")
    val visibleRole: String? = null,
    @SerialName("is_mine")
    val isMine: Boolean = false,
    @SerialName("is_post_author")
    val isPostAuthor: Boolean = false,
    val content: String,
    @SerialName("parent_id")
    val parentId: String? = null,
    val status: String,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("replies_count")
    val repliesCount: Int = 0,
    @SerialName("liked_by_me")
    val likedByMe: Boolean = false,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("published_at")
    val publishedAt: String? = null,
    /** Оригинал картинки (`forum/media/{id}`); `null` — вложения нет. */
    @SerialName("image_url")
    val imageUrl: String? = null,
    /** Миниатюра (`forum/media/{id}?size=preview`) — её и показывает лента. */
    @SerialName("image_preview_url")
    val imagePreviewUrl: String? = null,
    /** Размеры оригинала: по ним карточка держит пропорции до загрузки. */
    @SerialName("image_width")
    val imageWidth: Int? = null,
    @SerialName("image_height")
    val imageHeight: Int? = null
)

@Serializable
data class ForumCommentsPageDto(
    val items: List<ForumCommentDto> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null
)

/**
 * Тело запроса на создание комментария. `parentId` задан только у ответа.
 *
 * Как и у поста, текст без картинки и картинка без текста допустимы, а пустой
 * комментарий без вложения бэк отклоняет.
 */
@Serializable
data class CreateForumCommentRequestDto(
    val content: String = "",
    @SerialName("parent_id")
    val parentId: String? = null,
    /** Id вложения из `POST forum/media`. */
    @SerialName("image_id")
    val imageId: String? = null
)

/**
 * Ответ на создание поста или комментария.
 *
 * Бэк не отдаёт готовую карточку, если контент ушёл на модерацию: тогда
 * `result` — `moderation_required`, а сам объект `null`.
 */
@Serializable
data class CreateForumPostResultDto(
    val result: String,
    val post: ForumPostDto? = null
)

@Serializable
data class CreateForumCommentResultDto(
    val result: String,
    val comment: ForumCommentDto? = null
)

/**
 * Загруженная картинка — ответ `POST forum/media`.
 *
 * Пока не привязана ни к посту, ни к комментарию: её [id] уходит в `image_id`
 * при публикации.
 */
@Serializable
data class ForumAttachmentDto(
    val id: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("image_preview_url")
    val imagePreviewUrl: String,
    @SerialName("image_width")
    val imageWidth: Int,
    @SerialName("image_height")
    val imageHeight: Int
)

/**
 * Пост или комментарий, в ответ на который написан комментарий пользователя —
 * зеркало `ActivityContextResponse`.
 */
@Serializable
data class ForumActivityContextDto(
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    val content: String
)

/**
 * Комментарий из «Моих постов» — `ActivityCommentResponse`: те же поля, что и
 * у [ForumCommentDto], плюс контекст поста и родительского комментария.
 */
@Serializable
data class ForumActivityCommentDto(
    val id: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("profile_id")
    val profileId: String? = null,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("display_color")
    val displayColor: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("visible_role")
    val visibleRole: String? = null,
    @SerialName("is_mine")
    val isMine: Boolean = false,
    @SerialName("is_post_author")
    val isPostAuthor: Boolean = false,
    val content: String,
    @SerialName("parent_id")
    val parentId: String? = null,
    val status: String,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("replies_count")
    val repliesCount: Int = 0,
    @SerialName("liked_by_me")
    val likedByMe: Boolean = false,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("published_at")
    val publishedAt: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("image_preview_url")
    val imagePreviewUrl: String? = null,
    @SerialName("image_width")
    val imageWidth: Int? = null,
    @SerialName("image_height")
    val imageHeight: Int? = null,
    /** Пост, под которым оставлен комментарий. */
    @SerialName("post_context")
    val postContext: ForumActivityContextDto,
    /** Комментарий, на который это ответ; `null` — комментарий первого уровня. */
    @SerialName("reply_context")
    val replyContext: ForumActivityContextDto? = null
)

@Serializable
data class ForumActivityCommentsPageDto(
    val items: List<ForumActivityCommentDto> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null
)

/**
 * Публичный профиль форума — `ForumUserProfileResponse`.
 *
 * Своя карточка запрашивается тем же `forum/user-profile`, что и чужая:
 * отдельной ручки «мой профиль» на бэке нет.
 */
@Serializable
data class ForumUserProfileDto(
    @SerialName("profile_id")
    val profileId: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("display_color")
    val displayColor: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("posts_count")
    val postsCount: Int = 0,
    @SerialName("comments_count")
    val commentsCount: Int = 0
)
