package com.example.ysukampus.ui.main.profile.activity

import com.example.ysukampus.data.forum.ForumUserProfile
import com.example.ysukampus.ui.main.forum.ForumActivityComment
import com.example.ysukampus.ui.main.forum.ForumPost

/** Вкладки активности в шторке профиля — как в вебке: посты и комментарии. */
enum class MyProfileTab {
    Posts,
    Comments
}

/**
 * Снимок шторки «Профиль» — своей карточки на форуме и последней активности.
 *
 * Посты и комментарии листаются независимо: у каждой вкладки свой курсор и
 * свои флаги загрузки, поэтому переключение вкладок не сбрасывает соседнюю.
 */
data class MyProfileUiState(
    /** Шапка: ник, цвет, аватар, дата регистрации и счётчики. `null` — ещё едет. */
    val profile: ForumUserProfile? = null,

    val tab: MyProfileTab = MyProfileTab.Posts,

    val posts: List<ForumPost> = emptyList(),
    val comments: List<ForumActivityComment> = emptyList(),

    /** Первая загрузка активности: вместо списка показываем скелетон. */
    val isLoading: Boolean = true,
    val isLoadingMorePosts: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    /** Ошибка первой загрузки — вместо списков показываем повтор. */
    val error: String? = null,

    val postsCursor: String? = null,
    val commentsCursor: String? = null,

    /** Открыто подтверждение удаления своего поста или комментария. */
    val deletePostId: String? = null,
    val deleteCommentId: String? = null,

    /** Разовое сообщение для снекбара. */
    val message: String? = null
) {
    /** Активная вкладка загрузилась и оказалась пустой. */
    val isEmpty: Boolean
        get() = !isLoading && error == null && when (tab) {
            MyProfileTab.Posts -> posts.isEmpty()
            MyProfileTab.Comments -> comments.isEmpty()
        }

    val canLoadMorePosts: Boolean
        get() = postsCursor != null && !isLoading && !isLoadingMorePosts

    val canLoadMoreComments: Boolean
        get() = commentsCursor != null && !isLoading && !isLoadingMoreComments
}
