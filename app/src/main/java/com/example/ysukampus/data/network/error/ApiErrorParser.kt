package com.example.ysukampus.data.network.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

private val errorJson = Json {
    ignoreUnknownKeys = true
}

@Serializable
private data class ApiValidationErrorResponse(
    val detail: List<ApiValidationErrorDetail> = emptyList()
)

@Serializable
private data class ApiValidationErrorDetail(
    val msg: String? = null,
    val type: String? = null,
    @SerialName("loc")
    val location: List<JsonElement> = emptyList()
)

fun Throwable.toReadableMessage(): String {
    if (this !is HttpException) {
        return message ?: "Unexpected error"
    }

    val errorBody = response()?.errorBody()?.string()
    if (errorBody.isNullOrBlank()) {
        return "HTTP ${code()}"
    }

    return when (code()) {
        422 -> parseValidationError(errorBody) ?: "Validation error"
        else -> message ?: "HTTP ${code()}"
    }
}

private fun parseValidationError(errorBody: String): String? {
    return runCatching {
        val parsed = errorJson.decodeFromString<ApiValidationErrorResponse>(errorBody)
        parsed.detail.firstOrNull()?.let { detail ->
            val fieldName = detail.location.lastOrNull()?.jsonPrimitive?.contentOrNull
            when {
                !detail.msg.isNullOrBlank() && !fieldName.isNullOrBlank() -> "$fieldName: ${detail.msg}"
                !detail.msg.isNullOrBlank() -> detail.msg
                else -> null
            }
        }
    }.getOrNull()
}
