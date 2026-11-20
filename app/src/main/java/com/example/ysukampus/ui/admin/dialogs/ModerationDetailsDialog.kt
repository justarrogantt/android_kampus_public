package com.example.ysukampus.ui.admin.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ysukampus.ui.admin.AdminActionKind
import com.example.ysukampus.ui.admin.components.AdminActionButton
import com.example.ysukampus.ui.admin.components.AdminDialogFrame
import com.example.ysukampus.ui.admin.components.AdminSheetBlock
import com.example.ysukampus.ui.admin.screens.formatModerationDateTime
import com.example.ysukampus.ui.admin.screens.moderationStatusLabel
import com.example.ysukampus.ui.admin.state.AdminModerationItem
import com.example.ysukampus.ui.admin.state.AdminModerationReport
import com.example.ysukampus.ui.admin.state.AdminModerationTargetType
import com.example.ysukampus.ui.admin.state.MODERATION_STATUS_ACTIVE
import com.example.ysukampus.ui.admin.state.MODERATION_STATUS_HIDDEN
import com.example.ysukampus.ui.main.forum.components.ForumImageViewer
import com.example.ysukampus.ui.main.forum.parseAuthorColor
import com.example.ysukampus.ui.theme.YsuAdminColors
import com.example.ysukampus.ui.theme.YsuAdminDimens
import com.example.ysukampus.ui.theme.YsuAdminTextStyles

/**
 * «Детали модерации» для карточки очереди или истории: цитата контента, сводка
 * по нему и две кнопки вердикта. Повторяет окно вебки (`moderationCard`).
 */
@Composable
fun ModerationItemDialog(
    item: AdminModerationItem,
    isSaving: Boolean,
    onVerdict: (status: String) -> Unit,
    onClose: () -> Unit
) {
    ModerationDetailsDialog(
        targetType = item.targetType,
        authorName = item.authorName,
        authorColor = item.authorColor,
        content = item.content,
        imageUrl = item.imageUrl,
        imagePreviewUrl = item.imagePreviewUrl,
        createdAt = item.createdAt,
        contentStatus = item.status,
        reportsCount = null,
        zone = item.zone,
        aiScore = item.aiScore,
        aiReason = item.aiReason,
        isSaving = isSaving,
        onVerdict = onVerdict,
        onClose = onClose
    )
}

/**
 * То же окно для карточки жалобы: у неё есть счётчик жалоб, а статус берётся у
 * контента, а не у самой жалобы.
 */
@Composable
fun ModerationReportDialog(
    report: AdminModerationReport,
    isSaving: Boolean,
    onVerdict: (status: String) -> Unit,
    onClose: () -> Unit
) {
    ModerationDetailsDialog(
        targetType = report.targetType,
        authorName = report.authorName,
        authorColor = report.authorColor,
        content = report.content.ifBlank { report.reportReason.orEmpty() },
        imageUrl = report.imageUrl,
        imagePreviewUrl = report.imagePreviewUrl,
        createdAt = report.createdAt,
        contentStatus = report.contentStatus ?: report.status.orEmpty(),
        reportsCount = report.reportsCount,
        zone = report.zone,
        aiScore = report.aiScore,
        aiReason = report.aiReason,
        isSaving = isSaving,
        onVerdict = onVerdict,
        onClose = onClose
    )
}

@Composable
private fun ModerationDetailsDialog(
    targetType: AdminModerationTargetType,
    authorName: String,
    authorColor: String?,
    content: String,
    imageUrl: String?,
    imagePreviewUrl: String?,
    createdAt: String,
    contentStatus: String,
    reportsCount: Int?,
    zone: String?,
    aiScore: Double?,
    aiReason: String?,
    isSaving: Boolean,
    onVerdict: (status: String) -> Unit,
    onClose: () -> Unit
) {
    val targetLabel = targetType.targetLabel.lowercase()
    AdminDialogFrame(title = "Детали модерации", onClose = onClose) {
        ModerationQuoteBlock(
            title = "$authorName · ${targetType.targetLabel}",
            titleColor = authorColor,
            content = content,
            imageUrl = imagePreviewUrl ?: imageUrl,
            fullImageUrl = imageUrl ?: imagePreviewUrl
        )
        Spacer(Modifier.height(12.dp))
        AdminSheetBlock {
            ModerationSummaryField("Дата и время", formatModerationDateTime(createdAt))
            if (reportsCount != null) {
                ModerationSummaryField("Жалоб", reportsCount.toString())
            }
            ModerationSummaryField(
                label = "Статус контента",
                value = "${contentStatusEmoji(contentStatus)} ${moderationStatusLabel(contentStatus)}".trim()
            )
            ModerationSummaryField("Оценка ИИ", aiVerdictValue(zone, aiScore))
            if (!aiReason.isNullOrBlank()) {
                ModerationSummaryField("Комментарий ИИ", aiReason)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Кнопки идут в столбик, а не в ряд, как в вебке: «Одобрить комментарий»
        // в половину ширины диалога не помещается в одну строку.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminActionButton(
                text = if (isSaving) "Сохранение..." else "Скрыть $targetLabel",
                kind = AdminActionKind.Danger,
                onClick = { if (!isSaving) onVerdict(MODERATION_STATUS_HIDDEN) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            AdminActionButton(
                text = if (isSaving) "Сохранение..." else "Одобрить $targetLabel",
                kind = AdminActionKind.Restore,
                onClick = { if (!isSaving) onVerdict(MODERATION_STATUS_ACTIVE) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
        }
    }
}

/**
 * Цитата модерируемого контента: автор с его цветом, текст и картинка.
 * Превью открывается на весь экран, как по клику в вебке, — оригинал берётся из
 * [fullImageUrl], потому что в списке приходит уменьшенная копия.
 */
@Composable
internal fun ModerationQuoteBlock(
    title: String,
    titleColor: String?,
    content: String,
    imageUrl: String?,
    fullImageUrl: String? = imageUrl,
    imageNote: String? = null
) {
    var openedImageUrl by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(YsuAdminDimens.SheetBlockRadius))
            .background(YsuAdminColors.BackgroundSecondary)
            .border(
                1.dp,
                YsuAdminColors.Border,
                RoundedCornerShape(YsuAdminDimens.SheetBlockRadius)
            )
            .padding(YsuAdminDimens.SheetBlockPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = YsuAdminTextStyles.CardTitle,
            color = titleColor?.let { parseAuthorColor(it) } ?: YsuAdminColors.PrimaryDark
        )
        if (content.isNotBlank()) {
            Text(
                text = content,
                style = YsuAdminTextStyles.CardSubtitle,
                color = YsuAdminColors.Text
            )
        }
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                // Вебка показывает картинку целиком (`object-fit: contain`),
                // обрезка тут прячет как раз то, из-за чего контент на модерации.
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(YsuAdminColors.BackgroundTertiary)
                    .clickable { openedImageUrl = fullImageUrl ?: imageUrl }
            )
        }
        if (imageNote != null) {
            Text(
                text = imageNote,
                style = YsuAdminTextStyles.CardSubtitle,
                color = YsuAdminColors.TextSecondary
            )
        }
    }
    openedImageUrl?.let { url ->
        ForumImageViewer(
            imageUrl = url,
            isSaving = false,
            onClose = { openedImageUrl = null },
            onDownload = {},
            canDownload = false
        )
    }
}

@Composable
internal fun ModerationSummaryField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = YsuAdminTextStyles.CardTitle, color = YsuAdminColors.TextSecondary)
        Text(value, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.Text)
    }
}

private val AdminModerationTargetType.targetLabel: String
    get() = when (this) {
        AdminModerationTargetType.Post -> "Пост"
        AdminModerationTargetType.Comment -> "Комментарий"
    }

internal fun contentStatusEmoji(status: String): String = when (status.lowercase()) {
    "active" -> "🟢"
    "hidden" -> "🔴"
    else -> "🟡"
}

internal fun zoneEmoji(zone: String?): String = when (zone?.lowercase()) {
    "green" -> "🟢"
    "yellow" -> "🟡"
    "red" -> "🔴"
    else -> ""
}

/** Вебка показывает оценку как `score / 100`, а без `ai_log` — заглушку. */
private fun aiVerdictValue(zone: String?, aiScore: Double?): String {
    if (aiScore == null) return "Данные не найдены"
    val score = if (aiScore % 1.0 == 0.0) aiScore.toInt().toString() else aiScore.toString()
    return "${zoneEmoji(zone)} $score / 100".trim()
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F4FF)
@Composable
private fun ModerationItemDialogPreview() {
    ModerationItemDialog(
        item = AdminModerationItem(
            id = "p-201",
            targetType = AdminModerationTargetType.Post,
            authorName = "rezak159",
            authorColor = "#3B82F6",
            content = "Продам монстер вейпер 80мг",
            status = "review",
            createdAt = "2026-06-16T22:52:00Z",
            zone = "red",
            aiScore = 91.0,
            aiReason = "Подозрение на запрещённую продажу"
        ),
        isSaving = false,
        onVerdict = {},
        onClose = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F4FF)
@Composable
private fun ModerationReportDialogPreview() {
    ModerationReportDialog(
        report = AdminModerationReport(
            id = "17",
            targetType = AdminModerationTargetType.Comment,
            targetId = "c-410",
            authorName = "ИИ",
            content = "Хо-ро-шо!",
            status = "new",
            contentStatus = "active",
            createdAt = "2026-06-15T22:33:00Z",
            reportReason = "Оскорбление",
            zone = "yellow",
            aiScore = 68.0,
            aiReason = "Саркастичный комментарий",
            reportsCount = 3
        ),
        isSaving = false,
        onVerdict = {},
        onClose = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F4FF)
@Composable
private fun ModerationItemDialogSavingPreview() {
    ModerationItemDialog(
        item = AdminModerationItem(
            id = "c-388",
            targetType = AdminModerationTargetType.Comment,
            postId = "p-172",
            authorName = "rezak159",
            authorColor = "#3B82F6",
            content = "когда выход на ipo?",
            status = "hidden",
            createdAt = "2026-06-15T12:42:00Z"
        ),
        isSaving = true,
        onVerdict = {},
        onClose = {}
    )
}
