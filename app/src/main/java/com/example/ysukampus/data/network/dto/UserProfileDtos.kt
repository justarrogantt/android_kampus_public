package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Карточка пользователя из `GET/PUT ref/user/{id}`.
 *
 * Отличается от [UserDto] из ответа авторизации: здесь нет ролей, прав и
 * `telegram_notifications_enabled` — эту настройку бэк отдаёт только в ответе
 * авторизации, поэтому её значение приложение хранит у себя и не перетирает
 * ответом этой ручки.
 */
@Serializable
data class UserProfileDto(
    /** telegram_id пользователя: за пределами диапазона Int. */
    val id: Long,
    @SerialName("profile_id")
    val profileId: String,
    val username: String? = null,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("forum_name")
    val forumName: String,
    @SerialName("forum_color")
    val forumColor: String,
    val role: String,
    @SerialName("entity_oid")
    val entityOid: String,
    @SerialName("entity_name")
    val entityName: String? = null,
    val faculty: String? = null,
    @SerialName("visible_role")
    val visibleRole: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

/**
 * Тело `PUT ref/user/{id}`.
 *
 * `explicitNulls = false` в общей конфигурации `Json` выкидывает null-поля из
 * запроса, поэтому неизменённые поля просто не уезжают на сервер. Пустое тело
 * бэк отбивает 400 — отправлять запрос без изменений нельзя.
 *
 * Тумблера Telegram-уведомлений здесь нет намеренно: он живёт на своей ручке
 * `PATCH profile/telegram-notifications`, иначе его одиночное переключение
 * пришлось бы слать «пустым» обновлением профиля.
 */
@Serializable
data class UserUpdateRequestDto(
    @SerialName("forum_name")
    val forumName: String? = null,
    @SerialName("forum_color")
    val forumColor: String? = null,
    val role: String? = null,
    @SerialName("entity_id")
    val entityId: Int? = null
)
