package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthentificationConfirmedResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("refresh_token")
    val refreshToken: String,

    val user: UserDto
)

@Serializable
data class UserDto(
    /** telegram_id пользователя: за пределами диапазона Int. */
    val id: Long,

    /** UUID профиля: по нему собирается ссылка на аватар. */
    @SerialName("profile_id")
    val profileId: String? = null,
    val username: String? = null,

    @SerialName("first_name")
    val firstName: String,

    @SerialName("forum_name")
    val forumName: String? = null,
    @SerialName("forum_color")
    val forumColor: String? = null,
    val role: String? = null,
    @SerialName("entity_id")
    val entityId: Int? = null,
    @SerialName("entity_oid")
    val entityOid: String? = null,
    @SerialName("entity_name")
    val entityName: String? = null,
    val faculty: String? = null,
    @SerialName("forum_rules_accepted")
    val forumRulesAccepted: Boolean? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("telegram_notifications_enabled")
    val telegramNotificationsEnabled: Boolean? = null,
    val roles: List<AuthRoleDto> = emptyList(),
    /** Слаги прав пользователя: приходят только с ответом авторизации. */
    val permissions: List<String> = emptyList()
)

@Serializable
data class AuthRoleDto(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null
)
