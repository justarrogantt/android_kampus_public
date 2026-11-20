package com.example.ysukampus.data.auth

import com.example.ysukampus.data.network.dto.AuthentificationConfirmedResponse
import com.example.ysukampus.data.network.dto.RefreshRequestDTO
import com.example.ysukampus.data.network.dto.RefreshTokensResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthService {

    @POST("auth/code/confirm/{code}")
    suspend fun getAuthentificationConfirmation(
        @Path("code") code: String
    ): Response<ResponseBody>

    @POST("auth/refresh")
    suspend fun refreshTokens(
        @Body request: RefreshRequestDTO
    ): RefreshTokensResponse
}
