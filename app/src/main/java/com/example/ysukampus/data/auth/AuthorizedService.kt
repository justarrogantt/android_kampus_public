package com.example.ysukampus.data.auth

import com.example.ysukampus.data.network.dto.CreateForumCommentRequestDto
import com.example.ysukampus.data.network.dto.CreateForumCommentResultDto
import com.example.ysukampus.data.network.dto.CreateForumPostRequestDto
import com.example.ysukampus.data.network.dto.CreateForumPostResultDto
import com.example.ysukampus.data.network.dto.ForumActivityCommentsPageDto
import com.example.ysukampus.data.network.dto.ForumAttachmentDto
import com.example.ysukampus.data.network.dto.ForumCommentsPageDto
import com.example.ysukampus.data.network.dto.ForumPostDto
import com.example.ysukampus.data.network.dto.ForumPostsPageDto
import com.example.ysukampus.data.network.dto.ForumReactionDto
import com.example.ysukampus.data.network.dto.ForumUserProfileDto
import com.example.ysukampus.data.network.dto.GetTokensResponse
import com.example.ysukampus.data.network.dto.NotificationsPageDto
import com.example.ysukampus.data.network.dto.ProfileJokeDto
import com.example.ysukampus.data.network.dto.ProfilePhotoDto
import com.example.ysukampus.data.network.dto.ReadNotificationsRequestDto
import com.example.ysukampus.data.network.dto.ReadNotificationsResultDto
import com.example.ysukampus.data.network.dto.ScheduleDayResponseDto
import com.example.ysukampus.data.network.dto.ScheduleDirectoryDto
import com.example.ysukampus.data.network.dto.ScheduleWeekResponseDto
import com.example.ysukampus.data.network.dto.TelegramNotificationsRequestDto
import com.example.ysukampus.data.network.dto.TelegramNotificationsResultDto
import com.example.ysukampus.data.network.dto.UserProfileDto
import com.example.ysukampus.data.network.dto.UserUpdateRequestDto
import com.example.ysukampus.data.network.dto.WeatherDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthorizedService {

    @GET("auth/tokens")
    suspend fun getTokens(): GetTokensResponse

    @POST("auth/out")
    suspend fun logout(): Response<ResponseBody>

    @POST("auth/out_all")
    suspend fun logoutAll(): Response<ResponseBody>

    /**
     * Загрузка аватара.
     *
     * Ручка живёт на пользователе, а не на профиле: тем же путём админ меняет
     * чужой аватар, поэтому `userId` обязателен даже для своего.
     */
    @Multipart
    @POST("ref/user/{userId}/avatar")
    suspend fun uploadUserAvatar(
        @Path("userId") userId: Long,
        @Part file: MultipartBody.Part
    ): ProfilePhotoDto

    @GET("profile/photo")
    suspend fun getProfilePhoto(): ProfilePhotoDto

    /**
     * Оригинал своего фото профиля (не уменьшенный квадратный аватар).
     * Ручка отдаёт только владельцу — `profile_id` в пути нет и быть не может.
     */
    @GET("profile/photo/download")
    suspend fun downloadProfilePhoto(): ResponseBody

    /**
     * Случайный анекдот для профиля.
     * @param seen id уже показанных анекдотов через запятую — бэк не повторяется,
     * пока не начнётся новый круг.
     */
    @GET("profile/joke")
    suspend fun getProfileJoke(
        @Query("seen") seen: String? = null
    ): ProfileJokeDto

    @DELETE("ref/user/{userId}/avatar")
    suspend fun deleteUserAvatar(
        @Path("userId") userId: Long
    ): Response<Unit>

    /** Карточка пользователя: своя — по праву `read-self-user`. */
    @GET("ref/user/{userId}")
    suspend fun getUser(
        @Path("userId") userId: Long
    ): UserProfileDto

    /**
     * Редактирование профиля.
     *
     * Пустое тело отбивается 400, занятый ник — 409, слишком частая смена
     * ника — 429, а запрет на смену — 423; разбор этих кодов лежит в
     * `ProfileRepository`.
     */
    @PUT("ref/user/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: Long,
        @Body body: UserUpdateRequestDto
    ): UserProfileDto

    /**
     * Справочник групп и преподавателей для селектора.
     * `type` — "group" | "lecturer" | null (оба списка).
     */
    @GET("schedule/directory")
    suspend fun getScheduleDirectory(
        @Query("type") type: String?
    ): ScheduleDirectoryDto

    /**
     * Расписание на один день.
     *
     * Ручка на бэке — optional auth: если `group` не передан, сервер берёт
     * группу/преподавателя из профиля владельца токена. Поэтому первый запрос
     * идёт с `group = null`, а `entity` в ответе сообщает нам, чьё расписание пришло.
     */
    @GET("schedule/day")
    suspend fun getScheduleDay(
        @Query("date") date: String,
        @Query("group") group: String?
    ): ScheduleDayResponseDto

    /** Расписание на неделю (Пн–Сб). `week` — любая дата внутри нужной недели. */
    @GET("schedule/week")
    suspend fun getScheduleWeek(
        @Query("week") week: String,
        @Query("group") group: String?
    ): ScheduleWeekResponseDto

    /** Текущая погода для города филиала пользователя (бэк кэширует по городу). */
    @GET("weather")
    suspend fun getWeather(): WeatherDto

    /** Лента форума с cursor-пагинацией. Главной хватает первых постов. */
    @GET("forum/posts")
    suspend fun getForumPosts(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): ForumPostsPageDto

    /**
     * Публичный профиль форума: ник, цвет, аватар, дата регистрации и счётчики.
     * Своя карточка запрашивается той же ручкой — `profile_id` обязателен.
     */
    @GET("forum/user-profile")
    suspend fun getForumUserProfile(
        @Query("profile_id") profileId: String
    ): ForumUserProfileDto

    /**
     * Посты автора для «Моих постов». Без `profile_id` бэк отдаёт свои —
     * ручка та же, что и у публичного профиля другого пользователя.
     */
    @GET("forum/user-posts")
    suspend fun getMyForumPosts(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): ForumPostsPageDto

    /**
     * Комментарии автора с контекстом поста и родительского комментария.
     * Как и `forum/user-posts`, без `profile_id` — про себя.
     */
    @GET("forum/user-comments")
    suspend fun getMyForumComments(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): ForumActivityCommentsPageDto

    /**
     * Посты произвольного пользователя для публичного профиля — та же ручка,
     * что и [getMyForumPosts], но с обязательным `profile_id`.
     */
    @GET("forum/user-posts")
    suspend fun getForumUserPosts(
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): ForumPostsPageDto

    /** Комментарии произвольного пользователя — аналог [getMyForumComments] с `profile_id`. */
    @GET("forum/user-comments")
    suspend fun getForumUserComments(
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): ForumActivityCommentsPageDto

    /** Переключает лайк поста и возвращает итоговое состояние со счётчиком. */
    @POST("forum/posts/{postId}/like")
    suspend fun toggleForumPostLike(
        @Path("postId") postId: String
    ): ForumReactionDto

    /**
     * Жалоба на пост. Повторная жалоба того же пользователя отдаёт 409.
     *
     * Возвращаем «голый» [ResponseBody], а не `Response<...>`: обёртка гасила бы
     * неуспешные коды, и 409 пришлось бы разбирать вручную вместо общего
     * перехвата `HttpException` в репозитории.
     */
    @POST("forum/posts/{postId}/report")
    suspend fun reportForumPost(
        @Path("postId") postId: String
    ): ResponseBody

    /** Удаление своего поста. Успех — 204 с пустым телом. */
    @DELETE("forum/posts/{postId}")
    suspend fun deleteForumPost(
        @Path("postId") postId: String
    ): ResponseBody

    /** Один пост — экран треда открывается по нему, даже если лента не загружена. */
    @GET("forum/posts/{postId}")
    suspend fun getForumPost(
        @Path("postId") postId: String
    ): ForumPostDto

    /**
     * Создание поста.
     *
     * Ответ — не карточка, а результат: пост мог уйти на модерацию, тогда
     * `post` придёт пустым.
     */
    @POST("forum/posts")
    suspend fun createForumPost(
        @Body body: CreateForumPostRequestDto
    ): CreateForumPostResultDto

    /**
     * Загрузка картинки для поста или комментария.
     *
     * Собрана по образцу [uploadProfilePhoto]: то же поле `file`, тот же набор
     * форматов и тот же лимит размера. Вложение сначала висит ничьим, а к
     * контенту привязывается при публикации по возвращённому `id`.
     */
    @Multipart
    @POST("forum/media")
    suspend fun uploadForumMedia(
        @Part file: MultipartBody.Part
    ): ForumAttachmentDto

    /** Комментарии поста с cursor-пагинацией, как и лента. */
    @GET("forum/posts/{postId}/comments")
    suspend fun getForumComments(
        @Path("postId") postId: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): ForumCommentsPageDto

    /** Создание комментария или ответа на комментарий (`parentId`). */
    @POST("forum/posts/{postId}/comments")
    suspend fun createForumComment(
        @Path("postId") postId: String,
        @Body body: CreateForumCommentRequestDto
    ): CreateForumCommentResultDto

    /** Переключает лайк комментария — как и у поста, отдаёт итоговый счётчик. */
    @POST("forum/comments/{commentId}/like")
    suspend fun toggleForumCommentLike(
        @Path("commentId") commentId: String
    ): ForumReactionDto

    /** Удаление своего комментария. Успех — 204 с пустым телом. */
    @DELETE("forum/comments/{commentId}")
    suspend fun deleteForumComment(
        @Path("commentId") commentId: String
    ): ResponseBody

    /** Жалоба на комментарий. Повторная жалоба того же пользователя отдаёт 409. */
    @POST("forum/comments/{commentId}/report")
    suspend fun reportForumComment(
        @Path("commentId") commentId: String
    ): ResponseBody

    /**
     * Уведомления с cursor-пагинацией — курсор устроен так же, как в ленте форума.
     *
     * Кроме карточек ответ приносит `unread_count` (по всему аккаунту, а не по
     * загруженным страницам) и `moderation_count` — размер очереди модерации,
     * который бэк считает только обладателям права `moderate-forum`.
     */
    @GET("notifications")
    suspend fun getNotifications(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): NotificationsPageDto

    /**
     * Отмечает прочитанным одно уведомление или сразу все.
     * В теле должно быть заполнено ровно одно поле — иначе 422.
     */
    @POST("notifications/read")
    suspend fun readNotifications(
        @Body body: ReadNotificationsRequestDto
    ): ReadNotificationsResultDto

    /**
     * Дублирование уведомлений в Telegram-бота.
     *
     * Отдельная ручка, а не поле в `PUT ref/user/{id}`: тумблер меняется
     * независимо от остальной карточки, и «пустое» обновление профиля
     * ради одного флага бэк отбил бы четырёхсоткой.
     */
    @PATCH("profile/telegram-notifications")
    suspend fun updateTelegramNotifications(
        @Body body: TelegramNotificationsRequestDto
    ): TelegramNotificationsResultDto
}
