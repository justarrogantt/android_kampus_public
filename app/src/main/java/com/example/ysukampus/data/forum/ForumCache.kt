package com.example.ysukampus.data.forum

import com.example.ysukampus.data.local.db.ResponseCache
import com.example.ysukampus.data.network.dto.ForumActivityCommentsPageDto
import com.example.ysukampus.data.network.dto.ForumCommentsPageDto
import com.example.ysukampus.data.network.dto.ForumPostDto
import com.example.ysukampus.data.network.dto.ForumPostsPageDto
import com.example.ysukampus.data.network.dto.ForumUserProfileDto

/**
 * Дисковый кэш форума: первая страница ленты, посты для главной, открытые
 * треды, шапка профиля и первые страницы своей активности.
 *
 * Кэшируется исключительно первая страница (`cursor == null`). Страницы за
 * курсором сюда не попадают сознательно: курсор живёт ровно до следующей
 * публикации, и сохранённый вместе с ним «хвост» ленты после перезапуска
 * склеивался бы с новой первой страницей в кашу из дублей и дыр. Первая
 * страница даёт всё, ради чего кэш и заводился: экран открывается сразу и
 * работает без сети, а долистывание требует сети — как и раньше.
 */
class ForumCache(
    private val cache: ResponseCache
) {

    suspend fun readFeed(limit: Int, maxAge: Long?): ForumPostsPageDto? =
        cache.read(feedKey(limit), ForumPostsPageDto.serializer(), maxAge)

    suspend fun writeFeed(limit: Int, value: ForumPostsPageDto) {
        cache.write(feedKey(limit), ForumPostsPageDto.serializer(), value)
    }

    suspend fun readHomePosts(limit: Int, maxAge: Long?): ForumPostsPageDto? =
        cache.read(homePostsKey(limit), ForumPostsPageDto.serializer(), maxAge)

    suspend fun writeHomePosts(limit: Int, value: ForumPostsPageDto) {
        cache.write(homePostsKey(limit), ForumPostsPageDto.serializer(), value)
    }

    suspend fun readPost(postId: String, maxAge: Long?): ForumPostDto? =
        cache.read(postKey(postId), ForumPostDto.serializer(), maxAge)

    suspend fun writePost(postId: String, value: ForumPostDto) {
        cache.write(postKey(postId), ForumPostDto.serializer(), value)
    }

    suspend fun readComments(postId: String, limit: Int, maxAge: Long?): ForumCommentsPageDto? =
        cache.read(commentsKey(postId, limit), ForumCommentsPageDto.serializer(), maxAge)

    suspend fun writeComments(postId: String, limit: Int, value: ForumCommentsPageDto) {
        cache.write(commentsKey(postId, limit), ForumCommentsPageDto.serializer(), value)
    }

    suspend fun readUserProfile(profileId: String, maxAge: Long?): ForumUserProfileDto? =
        cache.read(profileKey(profileId), ForumUserProfileDto.serializer(), maxAge)

    suspend fun writeUserProfile(profileId: String, value: ForumUserProfileDto) {
        cache.write(profileKey(profileId), ForumUserProfileDto.serializer(), value)
    }

    suspend fun readMyPosts(maxAge: Long?): ForumPostsPageDto? =
        cache.read(MY_POSTS_KEY, ForumPostsPageDto.serializer(), maxAge)

    suspend fun writeMyPosts(value: ForumPostsPageDto) {
        cache.write(MY_POSTS_KEY, ForumPostsPageDto.serializer(), value)
    }

    suspend fun readMyComments(maxAge: Long?): ForumActivityCommentsPageDto? =
        cache.read(MY_COMMENTS_KEY, ForumActivityCommentsPageDto.serializer(), maxAge)

    suspend fun writeMyComments(value: ForumActivityCommentsPageDto) {
        cache.write(MY_COMMENTS_KEY, ForumActivityCommentsPageDto.serializer(), value)
    }

    /** Активность чужого профиля — тот же приём, что и [readMyPosts], но по `profileId`. */
    suspend fun readUserPosts(profileId: String, maxAge: Long?): ForumPostsPageDto? =
        cache.read(userPostsKey(profileId), ForumPostsPageDto.serializer(), maxAge)

    suspend fun writeUserPosts(profileId: String, value: ForumPostsPageDto) {
        cache.write(userPostsKey(profileId), ForumPostsPageDto.serializer(), value)
    }

    suspend fun readUserComments(profileId: String, maxAge: Long?): ForumActivityCommentsPageDto? =
        cache.read(userCommentsKey(profileId), ForumActivityCommentsPageDto.serializer(), maxAge)

    suspend fun writeUserComments(profileId: String, value: ForumActivityCommentsPageDto) {
        cache.write(userCommentsKey(profileId), ForumActivityCommentsPageDto.serializer(), value)
    }

    /**
     * Полный сброс форумного кэша после публикации или удаления.
     *
     * Целиком, а не точечно: свой новый пост меняет разом и ленту, и главную, и
     * свою активность, и счётчики в шапке профиля. Разбирать, что именно из
     * этого затронуто, — лишний повод ошибиться; цена промаха всего лишь один
     * сетевой запрос на следующем открытии экрана.
     */
    suspend fun evictContent() {
        cache.evictPrefix(PREFIX)
    }

    private companion object {
        const val PREFIX = "forum:"
        const val MY_POSTS_KEY = PREFIX + "my-posts"
        const val MY_COMMENTS_KEY = PREFIX + "my-comments"

        fun profileKey(profileId: String): String = "${PREFIX}profile:$profileId"

        fun userPostsKey(profileId: String): String = "${PREFIX}user-posts:$profileId"

        fun userCommentsKey(profileId: String): String = "${PREFIX}user-comments:$profileId"

        /**
         * `limit` в ключе: вкладка «Форум» просит страницу целиком, а главная —
         * несколько верхних постов той же ручкой. Под общим ключом они затирали
         * бы друг друга, и главная то и дело получала бы полную страницу.
         */
        fun feedKey(limit: Int): String = "${PREFIX}feed:$limit"

        fun homePostsKey(limit: Int): String = "${PREFIX}home-posts:$limit"

        /**
         * По записи на каждый открытый тред. Растёт это медленно и обрезается
         * общей уборкой по возрасту (`ResponseCache.evictStaleEntries`):
         * тред, открытый однажды две недели назад, второй раз вряд ли откроют.
         */
        fun postKey(postId: String): String = "${PREFIX}post:$postId"

        fun commentsKey(postId: String, limit: Int): String =
            "${PREFIX}comments:$postId:$limit"
    }
}
