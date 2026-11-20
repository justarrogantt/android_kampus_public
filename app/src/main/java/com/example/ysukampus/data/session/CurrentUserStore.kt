package com.example.ysukampus.data.session

import android.content.SharedPreferences
import com.example.ysukampus.data.network.dto.UserDto
import com.example.ysukampus.data.network.dto.UserProfileDto

/**
 * Кэш профиля текущего пользователя.
 *
 * Бэк отдаёт пользователя только в ответе авторизации (`auth/confirm`,
 * `auth/dev/login`, `auth/refresh`) — отдельной ручки «кто я» нет. Приложение же
 * после перезапуска восстанавливает сессию по токенам, без обращения к этим
 * ручкам, поэтому карточку приходится сохранять на диск.
 *
 * Отсюда же берётся `id` пользователя: без него нельзя ни перечитать карточку
 * (`GET ref/user/{id}`), ни сохранить её, ни поменять аватар.
 */
interface CurrentUserStore {
    fun save(user: UserDto)

    /**
     * Обновляет карточку после `PUT ref/user/{id}`.
     *
     * Настройка Telegram-уведомлений в ответе этой ручки не приходит, поэтому
     * её значение передаётся отдельно и сохраняется как есть.
     */
    fun update(user: UserProfileDto, telegramNotificationsEnabled: Boolean)
    fun updateAvatarUrl(avatarUrl: String?)

    /**
     * Тумблер Telegram-уведомлений меняется своей ручкой
     * (`PATCH profile/telegram-notifications`), которая карточку не возвращает,
     * поэтому кэш правится точечно.
     */
    fun updateTelegramNotifications(enabled: Boolean)
    fun get(): CurrentUser?
    fun clear()
}

/** Поля профиля, которые нужны UI вне экрана авторизации. */
data class CurrentUser(
    /**
     * `null` — сессия пережила обновление приложения, когда id ещё не
     * сохранялся. Приветствие на главной от этого не страдает, а вот
     * редактирование профиля без id невозможно и остаётся закрытым до
     * следующего входа.
     */
    val id: Long?,
    /**
     * UUID пользователя на форуме. `null` — карточка сохранена версией, которая
     * его ещё не знала; тогда профиль перечитывается через `GET ref/user/{id}`.
     */
    val profileId: String?,
    val forumName: String,
    val forumColor: String?,
    val role: String,
    val entityOid: String?,
    val entityName: String,
    val avatarUrl: String?,
    val telegramNotificationsEnabled: Boolean,
    /**
     * Слаги прав из ответа авторизации. `null` — карточка сохранена версией,
     * которая их ещё не знала: права неизвестны, и UI не должен ничего по ним
     * прятать, пусть решает бэкенд своими 403.
     */
    val permissions: Set<String>?
) {
    fun hasPermission(slug: String): Boolean = permissions?.contains(slug) ?: true

    /** Преподаватель выбирает себя в справочнике преподавателей, остальные — группу. */
    val isLecturer: Boolean get() = role == ROLE_TEACHER

    companion object {
        const val ROLE_TEACHER = "teacher"
        const val ROLE_STUDENT = "student"
    }
}

class SharedPrefsCurrentUserStore(
    private val prefs: SharedPreferences
) : CurrentUserStore {

    override fun save(user: UserDto) {
        prefs.edit()
            .putLong(KEY_ID, user.id)
            .putString(KEY_PROFILE_ID, user.profileId)
            // forum_name — публичное имя на форуме; вебка приветствует именно им.
            .putString(KEY_FORUM_NAME, user.forumName ?: user.firstName)
            .putString(KEY_FORUM_COLOR, user.forumColor)
            .putString(KEY_ROLE, user.role ?: CurrentUser.ROLE_STUDENT)
            .putString(KEY_ENTITY_OID, user.entityOid)
            .putString(KEY_ENTITY_NAME, user.entityName.orEmpty())
            .putString(KEY_AVATAR_URL, user.avatarUrl)
            .putBoolean(KEY_TELEGRAM_NOTIFICATIONS, user.telegramNotificationsEnabled ?: true)
            .putStringSet(KEY_PERMISSIONS, user.permissions.toSet())
            .apply()
    }

    override fun update(user: UserProfileDto, telegramNotificationsEnabled: Boolean) {
        prefs.edit()
            .putLong(KEY_ID, user.id)
            .putString(KEY_PROFILE_ID, user.profileId)
            .putString(KEY_FORUM_NAME, user.forumName)
            .putString(KEY_FORUM_COLOR, user.forumColor)
            .putString(KEY_ROLE, user.role)
            .putString(KEY_ENTITY_OID, user.entityOid)
            .putString(KEY_ENTITY_NAME, user.entityName.orEmpty())
            .putString(KEY_AVATAR_URL, user.avatarUrl)
            .putBoolean(KEY_TELEGRAM_NOTIFICATIONS, telegramNotificationsEnabled)
            .apply()
    }

    override fun updateAvatarUrl(avatarUrl: String?) {
        prefs.edit().putString(KEY_AVATAR_URL, avatarUrl).apply()
    }

    override fun updateTelegramNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TELEGRAM_NOTIFICATIONS, enabled).apply()
    }

    override fun get(): CurrentUser? {
        val forumName = prefs.getString(KEY_FORUM_NAME, null) ?: return null

        return CurrentUser(
            id = readId(),
            profileId = prefs.getString(KEY_PROFILE_ID, null),
            forumName = forumName,
            forumColor = prefs.getString(KEY_FORUM_COLOR, null),
            role = prefs.getString(KEY_ROLE, null) ?: CurrentUser.ROLE_STUDENT,
            entityOid = prefs.getString(KEY_ENTITY_OID, null),
            entityName = prefs.getString(KEY_ENTITY_NAME, null).orEmpty(),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, null),
            telegramNotificationsEnabled = prefs.getBoolean(KEY_TELEGRAM_NOTIFICATIONS, true),
            permissions = prefs.getStringSet(KEY_PERMISSIONS, null)
        )
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_ID)
            .remove(KEY_PROFILE_ID)
            .remove(KEY_FORUM_NAME)
            .remove(KEY_FORUM_COLOR)
            .remove(KEY_ROLE)
            .remove(KEY_ENTITY_OID)
            .remove(KEY_ENTITY_NAME)
            .remove(KEY_AVATAR_URL)
            .remove(KEY_TELEGRAM_NOTIFICATIONS)
            .remove(KEY_PERMISSIONS)
            .apply()
    }

    /**
     * `id` раньше сохранялся как `Int` — до того, как выяснилось, что это
     * telegram_id и в Int он не помещается. У переживших обновление сессий
     * ключ до сих пор лежит целым, и `getLong` на нём бросает
     * `ClassCastException`, поэтому старое значение читаем и переписываем.
     */
    private fun readId(): Long? {
        val stored = runCatching { prefs.getLong(KEY_ID, 0L) }
            .getOrElse {
                val legacy = prefs.getInt(KEY_ID, 0).toLong()
                if (legacy > 0) prefs.edit().putLong(KEY_ID, legacy).apply()
                legacy
            }
        return stored.takeIf { it > 0 }
    }

    private companion object {
        const val KEY_ID = "current_user_id"
        const val KEY_PROFILE_ID = "current_user_profile_id"
        const val KEY_FORUM_NAME = "current_user_forum_name"
        const val KEY_FORUM_COLOR = "current_user_forum_color"
        const val KEY_ROLE = "current_user_role"
        const val KEY_ENTITY_OID = "current_user_entity_oid"
        const val KEY_ENTITY_NAME = "current_user_entity_name"
        const val KEY_AVATAR_URL = "current_user_avatar_url"
        const val KEY_TELEGRAM_NOTIFICATIONS = "current_user_telegram_notifications"
        const val KEY_PERMISSIONS = "current_user_permissions"
    }
}
