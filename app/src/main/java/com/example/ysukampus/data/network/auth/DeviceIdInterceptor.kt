package com.example.ysukampus.data.network.auth

import com.example.ysukampus.data.session.DeviceIdStorage
import okhttp3.Interceptor
import okhttp3.Response

class DeviceIdInterceptor(
    private val deviceIdStorage: DeviceIdStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = deviceIdStorage.getOrCreateDeviceId()

        val newRequest = chain.request()
            .newBuilder()
            .addHeader("X-Device-Id", deviceId)
            .build()

        return chain.proceed(newRequest)
    }
}