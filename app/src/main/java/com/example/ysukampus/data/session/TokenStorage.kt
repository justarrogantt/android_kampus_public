package com.example.ysukampus.data.session

interface TokenStorage {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String, refreshToken: String?)
    fun clearTokens()
}