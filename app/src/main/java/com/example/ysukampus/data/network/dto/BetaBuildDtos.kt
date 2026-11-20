package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET builds/beta/latest` — манифест последней бета-сборки на выбранном стенде.
 *
 * [url] относительный (`/api/builds/beta/download?version_code=N`) и ведёт на
 * конкретную версию, чтобы скачался ровно тот файл, чей [sha256] здесь указан.
 * Достраивается до абсолютного через `resolveApiFileUrl`, как аватарки.
 */
@Serializable
data class BetaBuildManifestDto(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    val notes: String? = null,
    val size: Long,
    val sha256: String,
    val url: String
)
