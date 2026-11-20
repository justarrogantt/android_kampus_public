package com.example.ysukampus.data.network.admin

import android.util.Log
import com.example.ysukampus.data.network.resolveApiFileUrl
import com.example.ysukampus.ui.admin.state.AdminModerationEvent
import com.example.ysukampus.ui.admin.state.AdminModerationEventActor
import com.example.ysukampus.ui.admin.state.AdminModerationEventDetails
import com.example.ysukampus.ui.admin.state.AdminModerationEventTarget
import com.example.ysukampus.ui.admin.state.AdminModerationEventTargetType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * `GET forum/moderation/history` отдаёт страницу событий; `details` у каждого
 * источника свой, поэтому он разбирается по known-полям, а неизвестные молча
 * игнорируются.
 */
fun JsonElement.toAdminModerationEvents(): List<AdminModerationEvent> =
    historyItemsArray().mapNotNull { element ->
        runCatching { element.jsonObject.toAdminModerationEvent() }
            .onFailure { Log.e(TAG, "Failed to map moderation history event: $element", it) }
            .getOrNull()
    }

private fun JsonObject.toAdminModerationEvent(): AdminModerationEvent {
    val target = (get("target") as? JsonObject) ?: JsonObject(emptyMap())
    val actor = (get("actor") as? JsonObject) ?: JsonObject(emptyMap())
    val details = (get("details") as? JsonObject) ?: JsonObject(emptyMap())
    return AdminModerationEvent(
        id = string("id") ?: throw IllegalArgumentException("Missing moderation event id"),
        ts = string("ts") ?: string("created_at") ?: "",
        action = string("action").orEmpty(),
        actor = AdminModerationEventActor(
            kind = actor.string("kind").orEmpty(),
            name = actor.string("name") ?: "Неизвестно"
        ),
        target = AdminModerationEventTarget(
            type = AdminModerationEventTargetType.fromApi(target.string("type")),
            id = target.string("id").orEmpty(),
            preview = target.string("preview"),
            content = target.string("content"),
            author = target.string("author"),
            authorColor = target.string("author_color"),
            status = target.string("status"),
            deletedByAuthor = target.boolean("deleted_by_author") ?: false,
            imageUrl = resolveApiFileUrl(target.string("image_url")),
            imagePreviewUrl = resolveApiFileUrl(target.string("image_preview_url")),
            imageHidden = target.boolean("image_hidden") ?: false
        ),
        details = AdminModerationEventDetails(
            fromStatus = details.string("from_status"),
            toStatus = details.string("to_status"),
            zone = details.string("zone"),
            score = details.int("score"),
            reason = details.string("reason"),
            apiError = details.boolean("api_error") ?: false,
            hours = details.int("hours"),
            contentStatus = details.string("content_status"),
            reportId = details.string("report_id")
        )
    )
}

private fun JsonElement.historyItemsArray(): JsonArray =
    when (this) {
        is JsonArray -> this
        is JsonObject -> (get("items") as? JsonArray) ?: JsonArray(emptyList())
        else -> JsonArray(emptyList())
    }

private fun JsonObject.string(key: String): String? =
    get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

private fun JsonObject.int(key: String): Int? =
    get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull

private fun JsonObject.boolean(key: String): Boolean? =
    get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.booleanOrNull

private const val TAG = "AdminModerHistoryMap"
