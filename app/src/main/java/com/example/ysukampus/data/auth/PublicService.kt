package com.example.ysukampus.data.auth

import com.example.ysukampus.data.network.dto.AuthentificationCodeResponse
import com.example.ysukampus.data.network.dto.AuthentificationConfirmedResponse
import com.example.ysukampus.data.network.dto.DevLoginRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PublicService {

    @POST("auth/code")
    suspend fun getAuthentificationCode(): AuthentificationCodeResponse

    @POST("auth/dev/login")
    suspend fun loginAsDev(
        @Body request: DevLoginRequestDto
    ): AuthentificationConfirmedResponse

    @GET("auth/status/{code}")
    suspend fun getAuthentificationStatus(
        @Path("code") code: String
    ): Response<ResponseBody>
}
