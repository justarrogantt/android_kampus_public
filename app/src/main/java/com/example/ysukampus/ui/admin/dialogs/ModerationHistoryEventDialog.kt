package com.example.ysukampus.ui.admin.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.admin.AdminActionKind
import com.example.ysukampus.ui.admin.components.AdminActionButton
import com.example.ysukampus.ui.admin.components.AdminDialogFrame
import com.example.ysukampus.ui.admin.components.AdminSheetBlock
import com.example.ysukampus.ui.admin.screens.formatModerationDateTime
import com.example.ysukampus.ui.admin.screens.moderationActionLabel
import com.example.ysukampus.ui.admin.screens.moderationStatusLabel
import com.example.ysukampus.ui.admin.state.AdminModerationEvent
import com.example.ysukampus.ui.admin.state.AdminModerationEventActor
import com.example.ysukampus.ui.admin.state.AdminModerationEventDetails
import com.example.ysukampus.ui.admin.state.AdminModerationEventTarget
import com.example.ysukampus.ui.admin.state.AdminModerationEventTargetType
import com.example.ysukampus.ui.admin.state.MODERATION_STATUS_HIDDEN

/**
 * «Событие модерации» — карточка из ленты истории: что произошло, кто это
 * сделал и почему. Единственное доступное действие — вернуть скрытый контент,
 * и только при праве `restore-hidden-content`. Порт web-модалки
 * `moderationHistory`.
 */
@Composable
fun ModerationHistoryEventDialog(
    event: AdminModerationEvent,
    canRestore: Boolean,
    isRestoring: Boolean,
    onRestore: () -> Unit,
    onClose: () -> Unit
) {
    val target = event.target
    val details = event.details
    // При смене статуса вебка показывает сам переход, иначе — подпись действия.
    val actionText = if (details.fromStatus != null && details.toStatus != null) {
        "${moderationStatusLabel(details.fromStatus)} → ${moderationStatusLabel(details.toStatus)}"
    } else {
        moderationActionLabel(event.action)
    }
    val showRestore = canRestore &&
        event.action.equals("hide", ignoreCase = true) &&
        target.type != AdminModerationEventTargetType.User &&
        target.status.equals(MODERATION_STATUS_HIDDEN, ignoreCase = true)

    AdminDialogFrame(title = "Событие модерации", onClose = onClose) {
        if (!target.content.isNullOrBlank() || target.imageUrl != null || target.imageHidden) {
            ModerationQuoteBlock(
                title = listOfNotNull(target.author, target.type.label).joinToString(" · "),
                titleColor = target.authorColor,
                content = target.content.orEmpty(),
                imageUrl = target.imagePreviewUrl ?: target.imageUrl,
                fullImageUrl = target.imageUrl ?: target.imagePreviewUrl,
                imageNote = "Картинка удалена автором".takeIf { target.imageHidden }
            )
            Spacer(Modifier.height(12.dp))
        }
        AdminSheetBlock {
            ModerationSummaryField("Действие", actionText)
            ModerationSummaryField(
                label = if (event.actor.kind.equals("author", ignoreCase = true)) "Автор" else "Модератор",
                value = event.actor.name
            )
            if (target.type == AdminModerationEventTargetType.User) {
                ModerationSummaryField(
                    label = "Объект",
                    value = listOfNotNull(target.type.label, target.preview).joinToString(" · ")
                )
            }
            ModerationSummaryField("Дата и время", formatModerationDateTime(event.ts))
            if (event.action.equals("ai_verdict", ignoreCase = true)) {
                ModerationSummaryField(
                    label = "Оценка",
                    value = "${zoneEmoji(details.zone)} ${details.score ?: "—"} / 100".trim()
                )
                if (!details.reason.isNullOrBlank()) {
                    ModerationSummaryField("Причина", details.reason)
                }
                if (details.apiError) {
                    ModerationSummaryField("Ошибка ИИ", "⚠️ Вердикт по фолбэку")
                }
            }
            if (event.action.equals("mute", ignoreCase = true)) {
                ModerationSummaryField(
                    label = "Срок",
                    value = details.hours?.let { "$it ч" } ?: "Навсегда"
                )
            }
            if (event.action.equals("resolve_report", ignoreCase = true)) {
                details.contentStatus?.let { status ->
                    ModerationSummaryField(
                        label = "Контент",
                        value = "${contentStatusEmoji(status)} ${moderationStatusLabel(status)}"
                    )
                }
                details.reportId?.let { reportId ->
                    ModerationSummaryField("ID жалобы", reportId)
                }
            }
        }
        if (showRestore) {
            Spacer(Modifier.height(12.dp))
            AdminActionButton(
                text = if (isRestoring) "Восстановление..." else "Восстановить ${target.type.label.lowercase()}",
                kind = AdminActionKind.Restore,
                onClick = { if (!isRestoring) onRestore() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRestoring
            )
        }
    }
}

internal val AdminModerationEventTargetType.label: String
    get() = when (this) {
        AdminModerationEventTargetType.Post -> "Пост"
        AdminModerationEventTargetType.Comment -> "Комментарий"
        AdminModerationEventTargetType.User -> "Пользователь"
    }

@Preview(showBackground = true, backgroundColor = 0xFFF0F4FF)
@Composable
private fun ModerationHistoryEventDialogPreview() {
    ModerationHistoryEventDialog(
        event = AdminModerationEvent(
            id = "chg_2",
            ts = "2026-06-16T23:04:00Z",
            action = "hide",
            actor = AdminModerationEventActor(kind = "moderator", name = "Администратор"),
            target = AdminModerationEventTarget(
                type = AdminModerationEventTargetType.Post,
                id = "p-201",
                preview = "Продам монстер вейпер 80мг",
                content = "Продам монстер вейпер 80мг",
                author = "rezak159",
                authorColor = "#3B82F6",
                status = "hidden"
            ),
            details = AdminModerationEventDetails(fromStatus = "review", toStatus = "hidden")
        ),
        canRestore = true,
        isRestoring = false,
        onRestore = {},
        onClose = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F4FF)
@Composable
private fun ModerationHistoryAiEventDialogPreview() {
    ModerationHistoryEventDialog(
        event = AdminModerationEvent(
            id = "ai_1",
            ts = "2026-06-16T22:52:10Z",
            action = "ai_verdict",
            actor = AdminModerationEventActor(kind = "ai", name = "ИИ"),
            target = AdminModerationEventTarget(
                type = AdminModerationEventTargetType.Comment,
                id = "c-410",
                preview = "Хо-ро-шо!",
                content = "Хо-ро-шо!",
                author = "ИИ",
                status = "active"
            ),
            details = AdminModerationEventDetails(
                zone = "red",
                score = 91,
                reason = "Подозрение на запрещённую продажу",
                apiError = true
            )
        ),
        canRestore = false,
        isRestoring = false,
        onRestore = {},
        onClose = {}
    )
}
