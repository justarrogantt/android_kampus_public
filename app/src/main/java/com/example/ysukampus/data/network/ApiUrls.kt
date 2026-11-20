package com.example.ysukampus.data.network

import java.net.URI

/**
 * Достраивает относительный путь (`/storage/avatars/...`), который отдаёт бэк,
 * до абсолютного URL текущего окружения. Абсолютные ссылки возвращаются как есть.
 */
fun resolveApiFileUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path

    val baseUri = runCatching { URI(ApiEnvironmentHolder.baseUrl) }.getOrNull() ?: return path
    val normalized = if (path.startsWith("/")) path else "/$path"

    return "${baseUri.scheme}://${baseUri.authority}$normalized"
}
