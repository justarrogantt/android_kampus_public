package com.example.ysukampus.data.network.auth

import java.util.Base64
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `isAccessTokenExpired` — единственное, что спасает опционально
 * авторизованные ручки расписания от молчаливого «не вижу пользователя» на
 * просроченном токене (см. комментарий в `AccessTokenExpiry.kt`). Регрессия
 * здесь не бросается в глаза ни на одном экране: расписание просто грузится
 * через раз, в зависимости от того, кто уже успел обновить токен.
 */
class AccessTokenExpiryTest {

    private val now = 1_700_000_000L

    private fun jwtWithExp(exp: Long?): String {
        val header = """{"alg":"none"}"""
        val payload = if (exp == null) "{}" else """{"exp":$exp}"""
        fun encode(json: String) =
            Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
        return "${encode(header)}.${encode(payload)}.signature"
    }

    @Test
    fun liveTokenIsNotExpired() {
        val token = jwtWithExp(exp = now + 3_600)

        assertFalse(isAccessTokenExpired(token, nowSeconds = now))
    }

    @Test
    fun pastExpiryIsExpired() {
        val token = jwtWithExp(exp = now - 60)

        assertTrue(isAccessTokenExpired(token, nowSeconds = now))
    }

    /** Токен формально ещё жив, но истечёт раньше, чем успеет доехать запрос. */
    @Test
    fun tokenInsideSkewWindowIsTreatedAsExpired() {
        val token = jwtWithExp(exp = now + 29)

        assertTrue("Токен, живущий меньше 30с, должен считаться истёкшим", isAccessTokenExpired(token, nowSeconds = now))
    }

    /** Ровно на границе запаса — тоже истёк, сравнение нестрогое. */
    @Test
    fun tokenExactlyAtSkewBoundaryIsExpired() {
        val token = jwtWithExp(exp = now + 30)

        assertTrue(isAccessTokenExpired(token, nowSeconds = now))
    }

    @Test
    fun tokenJustOutsideSkewWindowIsNotExpired() {
        val token = jwtWithExp(exp = now + 31)

        assertFalse(isAccessTokenExpired(token, nowSeconds = now))
    }

    /** Неразобранный формат обязан означать «не истёк» — иначе refresh гонялся бы на каждый запрос. */
    @Test
    fun garbageInsteadOfJwtIsNotExpired() {
        assertFalse(isAccessTokenExpired("не.токен.вовсе", nowSeconds = now))
        assertFalse(isAccessTokenExpired("совсем не похоже на jwt", nowSeconds = now))
        assertFalse(isAccessTokenExpired("", nowSeconds = now))
    }

    @Test
    fun missingExpClaimIsNotExpired() {
        val token = jwtWithExp(exp = null)

        assertFalse(isAccessTokenExpired(token, nowSeconds = now))
    }

    @Test
    fun unparseableBase64PayloadIsNotExpired() {
        val token = "header.!!!not-base64!!!.signature"

        assertFalse(isAccessTokenExpired(token, nowSeconds = now))
    }
}
