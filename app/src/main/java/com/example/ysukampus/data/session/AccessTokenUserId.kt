package com.example.ysukampus.data.session

import android.util.Base64
import org.json.JSONObject

/**
 * Достаёт `user_id` из полезной нагрузки access-токена.
 *
 * Нужен как запасной источник для [CurrentUserStore]: карточка пользователя
 * приходит только в ответе авторизации, и у сессии, пережившей обновление
 * приложения, сохранённого id может не быть — а без него нельзя ни перечитать
 * профиль, ни его сохранить.
 *
 * Подпись здесь намеренно не проверяется: значение идёт исключительно в
 * запросы, которые бэк всё равно авторизует сам, — подделанный id просто
 * получит 403. Это способ не гонять пользователя на повторный вход, а не
 * средство аутентификации.
 */
fun TokenStorage.currentUserId(): Long? {
    val payload = getAccessToken()?.split(".")?.getOrNull(1) ?: return null

    return runCatching {
        val json = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        JSONObject(String(json)).optLong("user_id").takeIf { it > 0 }
    }.getOrNull()
}
