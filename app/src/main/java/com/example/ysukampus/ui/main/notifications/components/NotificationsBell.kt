package com.example.ysukampus.ui.main.notifications.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.KampusMotion
import com.example.ysukampus.ui.main.kampusPressable
import com.example.ysukampus.ui.main.notifications.NotificationsColors
import com.example.ysukampus.ui.theme.NotificationsTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Колокольчик с бейджем непрочитанных — шапка расписания.
 *
 * Как и в вебке, число показывается только для уведомлений: очередь модерации
 * даёт голую точку, а её размер видно уже внутри шторки.
 *
 * @param unreadCount непрочитанные по всему аккаунту.
 * @param moderationCount размер очереди модерации; 0 у тех, кто её не разбирает.
 */
@Composable
fun NotificationsBell(
    unreadCount: Int,
    moderationCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when {
        unreadCount == 0 -> ""
        unreadCount > 9 -> "9+"
        else -> unreadCount.toString()
    }
    val contentDescription = if (unreadCount > 0) {
        stringResource(R.string.notifications_unread_count, unreadCount)
    } else {
        stringResource(R.string.notifications_open)
    }

    Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .kampusPressable(onClick, pressedScale = KampusMotion.ButtonPressScale)
                .clip(CircleShape)
                .background(KampusColors.Surface)
                .border(1.dp, KampusColors.Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_schedule_notifications),
                contentDescription = contentDescription,
                tint = KampusColors.PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
        }

        // Бейдж не появляется мгновенно: сокет приносит новое уведомление в
        // произвольный момент, и без анимации точка просто «выскакивает» на
        // экране, за которым человек не следил.
        AnimatedVisibility(
            visible = unreadCount > 0 || moderationCount > 0,
            enter = scaleIn(KampusMotion.lively(), initialScale = 0.4f) + fadeIn(KampusMotion.enter(160)),
            exit = scaleOut(KampusMotion.exit(140), targetScale = 0.4f) + fadeOut(KampusMotion.exit(140)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (-1).dp, y = 1.dp)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(CircleShape)
                    .background(KampusColors.Surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(NotificationsColors.Badge),
                contentAlignment = Alignment.Center
            ) {
                // Счётчик меняется, пока бейдж уже на экране: цифра
                // перекатывается снизу вверх, как в счётчиках-«одометрах».
                AnimatedContent(
                    targetState = label,
                    transitionSpec = {
                        (slideInVertically(KampusMotion.enter(220)) { height -> height } +
                            fadeIn(KampusMotion.enter(220)))
                            .togetherWith(
                                slideOutVertically(KampusMotion.exit(140)) { height -> -height } +
                                    fadeOut(KampusMotion.exit(140))
                            )
                    },
                    label = "bell-badge-count"
                ) { value ->
                    if (value.isNotEmpty()) {
                        Text(
                            text = value,
                            style = NotificationsTypography.BellBadge,
                            color = KampusColors.Surface,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Бейдж пункта меню профиля: тот же счётчик, но крупный и в строке, а не
 * поверх иконки (веб-класс `.notificationBadge`).
 */
@Composable
fun NotificationsMenuBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(NotificationsColors.Badge)
            .padding(horizontal = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = NotificationsTypography.Badge,
                color = KampusColors.Surface
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun NotificationsBellPreview() {
    YsukampusTheme(dynamicColor = false) {
        Row(
            modifier = Modifier
                .background(KampusColors.AppBackground)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotificationsBell(unreadCount = 0, moderationCount = 0, onClick = {})
            NotificationsBell(unreadCount = 3, moderationCount = 0, onClick = {})
            NotificationsBell(unreadCount = 42, moderationCount = 0, onClick = {})
            NotificationsBell(unreadCount = 0, moderationCount = 5, onClick = {})
            NotificationsMenuBadge(label = "9+")
        }
    }
}
