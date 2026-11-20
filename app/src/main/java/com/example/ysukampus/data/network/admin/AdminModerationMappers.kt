package com.example.ysukampus.data.network.admin

import android.util.Log
import com.example.ysukampus.data.network.resolveApiFileUrl
import com.example.ysukampus.data.network.ws.ModeratorWsEvent
import com.example.ysukampus.ui.admin.state.AdminModerationItem
import com.example.ysukampus.ui.admin.state.AdminModerationReport
import com.example.ysukampus.ui.admin.state.AdminModerationTargetType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun JsonElement.toAdminModerationItems(): List<AdminModerationItem> =
    extractItemsArray().mapNotNull { element ->
        runCatching { element.jsonObject.toAdminModerationItem() }
            .onFailure { Log.e(TAG, "Failed to map moderation content item: $element", it) }
            .getOrNull()
    }

fun JsonElement.toAdminModerationReports(): List<AdminModerationReport> =
    extractItemsArray().mapNotNull { element ->
        runCatching { element.jsonObject.toAdminModerationReport() }
            .onFailure { Log.e(TAG, "Failed to map moderation report item: $element", it) }
            .getOrNull()
    }

/** `moderation_count` карточкой не является — он только обновляет счётчик очереди. */
fun ModeratorWsEvent.toAdminModerationItem(): AdminModerationItem? =
    when (this) {
        is ModeratorWsEvent.NewPost -> AdminModerationItem(
            id = id,
            targetType = AdminModerationTargetType.Post,
            authorId = authorId,
            authorName = displayName,
            authorColor = displayColor,
            content = content,
            status = status,
            createdAt = createdAt,
            zone = zone,
            aiScore = aiScore,
            aiReason = aiReason
        )
        is ModeratorWsEvent.NewComment -> AdminModerationItem(
            id = id,
            targetType = AdminModerationTargetType.Comment,
            postId = postId,
            parentId = parentId,
            authorId = authorId,
            authorName = displayName,
            authorColor = displayColor,
            content = content,
            status = status,
            createdAt = createdAt,
            zone = zone,
            aiScore = aiScore,
            aiReason = aiReason
        )
        is ModeratorWsEvent.ModerationCount -> null
    }

private fun JsonObject.toAdminModerationItem(): AdminModerationItem {
    val type = when (string("type")?.lowercase()) {
        "new_post", "post" -> AdminModerationTargetType.Post
        "new_comment", "comment" -> AdminModerationTargetType.Comment
        else -> {
            val targetType = string("target_type")?.lowercase()
            if (targetType == "comment") AdminModerationTargetType.Comment else AdminModerationTargetType.Post
        }
    }
    val id = string("id") ?: return throw IllegalArgumentException("Missing moderation item id")
    val authorName = string("display_name")
        ?: string("author_name")
        ?: string("username")
        ?: string("name")
        ?: "Неизвестный автор"
    return AdminModerationItem(
        id = id,
        targetType = type,
        postId = string("post_id"),
        parentId = nullableString("parent_id"),
        authorId = long("author_id"),
        authorName = authorName,
        authorColor = string("display_color"),
        content = string("content") ?: string("text") ?: "",
        status = string("status") ?: if (boolean("resolved") == true) "resolved" else "review",
        createdAt = string("created_at") ?: string("updated_at") ?: "",
        // Решение AI приезжает вложенным объектом `ai_log`; плоские поля
        // оставлены как запасной вариант для старых ответов.
        zone = aiLog()?.string("zone") ?: nullableString("zone"),
        aiScore = aiLog()?.double("score") ?: double("ai_score"),
        aiReason = aiLog()?.nullableString("reason") ?: nullableString("ai_reason"),
        reportsCount = int("reports_count") ?: 0,
        imageUrl = resolveApiFileUrl(nullableString("image_url")),
        imagePreviewUrl = resolveApiFileUrl(nullableString("image_preview_url"))
    )
}

private fun JsonObject.toAdminModerationReport(): AdminModerationReport {
    val targetType = when (string("target_type")?.lowercase()) {
        "comment" -> AdminModerationTargetType.Comment
        else -> AdminModerationTargetType.Post
    }
    // Бэк группирует жалобы по цели и отдаёт список их id (`report_ids`).
    // Резолвится каждая по отдельности, поэтому карточка живёт по первой из них.
    val reportId = intList("report_ids").firstOrNull()?.toString()
        ?: string("report_id")
        ?: string("id")
        ?: return throw IllegalArgumentException("Missing report id")
    val targetId = string("target_id")
        ?: string("content_id")
        ?: string("post_id")
        ?: string("comment_id")
        ?: reportId
    val authorName = string("display_name")
        ?: string("author_name")
        ?: string("username")
        ?: string("name")
        ?: "Неизвестный автор"
    return AdminModerationReport(
        id = reportId,
        targetType = targetType,
        targetId = targetId,
        authorName = authorName,
        authorColor = string("display_color"),
        content = string("content") ?: string("text") ?: string("report_text") ?: "",
        status = string("report_status") ?: string("status"),
        contentStatus = string("content_status") ?: string("status"),
        createdAt = string("content_created_at") ?: string("created_at") ?: string("updated_at") ?: "",
        reportReason = string("reason") ?: string("report_reason") ?: string("message"),
        zone = aiLog()?.string("zone") ?: nullableString("zone"),
        aiScore = aiLog()?.double("score") ?: double("ai_score"),
        aiReason = aiLog()?.nullableString("reason") ?: nullableString("ai_reason"),
        reportsCount = int("reports_count") ?: 0,
        imageUrl = resolveApiFileUrl(nullableString("image_url")),
        imagePreviewUrl = resolveApiFileUrl(nullableString("image_preview_url"))
    )
}

/** Последнее решение AI-модерации: и в очереди контента, и в жалобах. */
private fun JsonObject.aiLog(): JsonObject? =
    get("ai_log")?.takeUnless { it is JsonNull } as? JsonObject

private fun JsonObject.intList(key: String): List<Int> =
    (get(key) as? JsonArray)?.mapNotNull { it.jsonPrimitive.intOrNull }.orEmpty()

private fun JsonElement.extractItemsArray(): JsonArray =
    when (this) {
        is JsonArray -> this
        is JsonObject -> {
            sequenceOf("items", "data", "results")
                .mapNotNull { key -> get(key)?.takeIf { it is JsonArray }?.jsonArray }
                .firstOrNull()
                ?: JsonArray(emptyList())
        }
        else -> JsonArray(emptyList())
    }

private fun JsonObject.string(key: String): String? =
    get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

private fun JsonObject.nullableString(key: String): String? =
    string(key)?.takeIf { it != "null" }

private fun JsonObject.long(key: String): Long? =
    get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.longOrNull

private fun JsonObject.int(key: String): Int? =
    get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull

private fun JsonObject.double(key: String): Double? =
    get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.doubleOrNull

private fun JsonObject.boolean(key: String): Boolean? =
    get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.booleanOrNull

private const val TAG = "AdminModerationMap"
