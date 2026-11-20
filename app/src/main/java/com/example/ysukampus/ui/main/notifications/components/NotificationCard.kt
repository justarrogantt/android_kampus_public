package com.example.ysukampus.ui.main.notifications.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ysukampus.R
import com.example.ysukampus.data.notifications.Notification
import com.example.ysukampus.data.notifications.NotificationActor
import com.example.ysukampus.data.notifications.NotificationKind
import com.example.ysukampus.data.notifications.NotificationTarget
import com.example.ysukampus.data.notifications.NotificationUnavailableReason
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.forum.parseAuthorColor
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.theme.NotificationsTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Карточка уведомления — перенос веб-компонента `NotificationCard` (вариант
 * `.large`, которым пользуется шторка).
 *
 * Непрочитанное подсвечивается мягкой заливкой и синей полосой слева.
 * Недоступная цель не открывается: вместо превью карточка показывает причину,
 * а сам тап уходит в модель только ради отметки прочитанного.
 */
@Composable
fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = parseAuthorColor(notification.actor.displayColor)
    val target = notification.target
    val previewText = when {
        target.isAvailable -> target.preview.ifBlank {
            stringResource(R.string.notifications_target_photo)
        }

        target.unavailableReason == NotificationUnavailableReason.PostDeleted ->
            stringResource(R.string.notifications_target_post_deleted)

        else -> stringResource(R.string.notifications_target_comment_deleted)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
            .background(if (notification.isRead) Color.Transparent else HomeColors.PrimarySoft)
            .clickable(onClick = onClick)
            .padding(start = if (notification.isRead) 10.dp else 7.dp, end = 10.dp)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Синяя полоса слева — веб-аналог `inset 3px 0 0 var(--primary)`.
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(HomeColors.Primary)
            )
        }

        NotificationAvatar(actor = notification.actor, accent = accent)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val action = when (notification.kind) {
                NotificationKind.Comment -> stringResource(R.string.notifications_kind_comment)
                NotificationKind.Reply -> stringResource(R.string.notifications_kind_reply)
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(notification.actor.displayName)
                    }
                    append(" ")
                    append(action)
                },
                style = NotificationsTypography.Message,
                color = KampusColors.TextMain
            )
            Text(
                text = previewText,
                style = NotificationsTypography.Preview,
                color = HomeColors.TextMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        val imagePreviewUrl = target.imagePreviewUrl
        if (target.isAvailable && imagePreviewUrl != null) {
            AsyncImage(
                model = imagePreviewUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HomeColors.PrimarySoft)
            )
        }
    }
}

/** Аватар автора: фото либо заглушка в кружке его цвета — как в ленте форума. */
@Composable
private fun NotificationAvatar(actor: NotificationActor, accent: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(HomeColors.PrimarySoft)
            .then(
                if (actor.avatarImageUrl == null) {
                    Modifier.border(2.dp, accent, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val avatarUrl = actor.avatarImageUrl
        if (avatarUrl == null) {
            Icon(
                painter = painterResource(R.drawable.ic_profile_avatar),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        } else {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NotificationCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(KampusColors.AppBackground)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NotificationCard(
                notification = previewNotification(
                    id = 1,
                    kind = NotificationKind.Comment,
                    readAtIso = null
                ),
                onClick = {}
            )
            NotificationCard(
                notification = previewNotification(
                    id = 2,
                    kind = NotificationKind.Reply,
                    readAtIso = "2026-09-04T10:00:00Z"
                ),
                onClick = {}
            )
            NotificationCard(
                notification = previewNotification(
                    id = 3,
                    kind = NotificationKind.Reply,
                    readAtIso = null,
                    isAvailable = false
                ),
                onClick = {}
            )
        }
    }
}

private fun previewNotification(
    id: Int,
    kind: NotificationKind,
    readAtIso: String?,
    isAvailable: Boolean = true
): Notification = Notification(
    id = id,
    kind = kind,
    actor = NotificationActor(
        profileId = "1a2b",
        displayName = "Onblessedboy",
        displayColor = "#3B82F6",
        avatarImageUrl = null
    ),
    target = NotificationTarget(
        postId = "post-1",
        commentId = "comment-$id",
        preview = if (isAvailable) "Да, пара будет, только в другой аудитории" else "",
        imagePreviewUrl = null,
        isAvailable = isAvailable,
        unavailableReason = if (isAvailable) null else NotificationUnavailableReason.CommentDeleted
    ),
    createdAtIso = "2026-09-04T09:00:00Z",
    readAtIso = readAtIso
)
