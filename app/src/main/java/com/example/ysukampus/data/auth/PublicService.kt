package com.example.ysukampus.data.auth

import com.example.ysukampus.data.network.dto.AuthentificationCodeResponse
import com.example.ysukampus.data.network.dto.BetaBuildManifestDto
import com.example.ysukampus.data.network.dto.AuthentificationConfirmedResponse
import com.example.ysukampus.data.network.dto.DevLoginRequestDto
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

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

    /**
     * Манифест последней бета-сборки. `Response`, а не голое тело: 404 здесь —
     * не сбой, а «на этом стенде раздача не настроена», и его надо отличить.
     */
    @GET("builds/beta/latest")
    suspend fun getLatestBetaBuild(): Response<BetaBuildManifestDto>

    /**
     * APK по абсолютному адресу из манифеста. `Call`, а не `suspend`: общий
     * callTimeout клиента (60 с) покрывает и чтение тела, а десятки мегабайт на
     * мобильной сети в него не укладываются — вызывающий продлевает таймаут
     * через `Call.timeout()` конкретного вызова, не заводя отдельный клиент.
     */
    @Streaming
    @GET
    fun downloadBetaBuild(@Url url: String): Call<ResponseBody>
}
