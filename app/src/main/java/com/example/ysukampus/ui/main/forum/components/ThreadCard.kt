package com.example.ysukampus.ui.main.forum.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusMotion
import com.example.ysukampus.ui.main.forum.ForumPost
import com.example.ysukampus.ui.main.forum.ForumPostStatus
import com.example.ysukampus.ui.main.forum.parseAuthorColor
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.main.kampusPressable
import com.example.ysukampus.ui.main.popOnEnabled
import com.example.ysukampus.ui.theme.ForumTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Карточка поста форума.
 *
 * Живёт в пакете форума, потому что её же будет рисовать вкладка «Форум»;
 * главная показывает ей первые посты ленты.
 */
@Composable
fun ThreadCard(
    post: ForumPost,
    timeAgo: String,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    // По умолчанию картинка ведёт туда же, куда и карточка: на главной своего
    // просмотрщика нет, а во вкладке «Форум» тап открывает оригинал.
    onImageClick: () -> Unit = onClick,
    /** Тап по аватару или нику — открывает профиль автора. `null` — не кликабельны. */
    onAuthorClick: (() -> Unit)? = null
) {
    val authorColor = parseAuthorColor(post.displayColor)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .kampusPressable(onClick)
            .clip(CardShape)
            .background(HomeColors.Surface)
            .border(1.5.dp, HomeColors.Border, CardShape)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Avatar(
            imageUrl = post.avatarImageUrl,
            authorColor = authorColor,
            onClick = onAuthorClick
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = post.displayName,
                    style = ForumTypography.AuthorName,
                    color = HomeColors.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(
                            if (onAuthorClick != null) {
                                Modifier.kampusPressable(onAuthorClick, pressedScale = KampusMotion.ButtonPressScale)
                            } else {
                                Modifier
                            }
                        )
                )
                post.visibleRole?.let { role ->
                    RoleBadge(name = role, authorColor = authorColor)
                }

                Text(
                    text = timeAgo,
                    style = ForumTypography.Time,
                    color = HomeColors.TextMuted,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 5.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )

                if (post.isMine) {
                    ActionButton(
                        iconRes = R.drawable.ic_forum_trash,
                        contentDescription = stringResource(R.string.forum_delete),
                        iconSize = 13.dp,
                        onClick = onDeleteClick
                    )
                } else {
                    ActionButton(
                        iconRes = R.drawable.ic_forum_report,
                        contentDescription = stringResource(R.string.forum_report),
                        iconSize = 11.dp,
                        onClick = onReportClick
                    )
                }
            }

            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    style = ForumTypography.Body,
                    color = HomeColors.TextMain,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            // Пост может быть и без текста — тогда картинка идёт сразу под шапкой.
            post.imagePreviewUrl?.let { previewUrl ->
                ContentImage(
                    previewUrl = previewUrl,
                    imageWidth = post.imageWidth,
                    imageHeight = post.imageHeight,
                    onClick = onImageClick,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetaButton(
                    iconRes = if (post.likedByMe) R.drawable.ic_forum_like_filled else R.drawable.ic_forum_like,
                    contentDescription = stringResource(R.string.forum_like),
                    text = post.likesCount.toString(),
                    tint = if (post.likedByMe) HomeColors.Danger else HomeColors.TextAccent,
                    // Лайк — единственное действие карточки с мгновенным
                    // откликом в UI, поэтому подтверждение даёт сама иконка.
                    popWhen = post.likedByMe,
                    onClick = onLikeClick
                )
                MetaButton(
                    iconRes = R.drawable.ic_forum_comment,
                    contentDescription = stringResource(R.string.forum_comments),
                    text = post.commentsCount.toString(),
                    tint = HomeColors.TextAccent,
                    onClick = onClick
                )

                Icon(
                    painter = painterResource(R.drawable.ic_forum_open),
                    contentDescription = null,
                    tint = HomeColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun Avatar(
    imageUrl: String?,
    authorColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(HomeColors.PrimarySoft)
            .then(
                if (imageUrl == null) Modifier.border(2.dp, authorColor, CircleShape) else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.kampusPressable(onClick, pressedScale = KampusMotion.ButtonPressScale)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl == null) {
            Icon(
                painter = painterResource(R.drawable.ic_profile_avatar),
                contentDescription = null,
                tint = authorColor,
                modifier = Modifier.size(18.dp)
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.forum_avatar_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ActionButton(
    iconRes: Int,
    contentDescription: String,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(25.dp)
            .kampusPressable(onClick, pressedScale = KampusMotion.ButtonPressScale)
            .clip(CircleShape)
            .background(HomeColors.PrimarySoft),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = HomeColors.TextMuted,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun MetaButton(
    iconRes: Int,
    contentDescription: String,
    text: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    /** Включение состояния подтверждается коротким увеличением иконки. */
    popWhen: Boolean = false
) {
    val animatedTint by animateColorAsState(
        targetValue = tint,
        animationSpec = KampusMotion.enter(KampusMotion.ColorMs),
        label = "metaButtonTint"
    )

    Row(
        modifier = Modifier
            .height(28.dp)
            .kampusPressable(onClick, pressedScale = KampusMotion.ButtonPressScale)
            .clip(RoundedCornerShape(999.dp))
            .background(HomeColors.PrimarySoft)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = animatedTint,
            modifier = Modifier
                .popOnEnabled(popWhen)
                .size(12.dp)
        )
        Text(
            text = text,
            style = ForumTypography.MetaCounter,
            color = animatedTint
        )
    }
}

private val CardShape = RoundedCornerShape(24.dp)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ThreadCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThreadCard(
                post = ForumPost(
                    id = "1",
                    profileId = null,
                    displayName = "Аноним",
                    displayColor = "#3b82f6",
                    avatarImageUrl = null,
                    visibleRole = "Студент",
                    isMine = false,
                    content = "Кто-нибудь знает, будет ли пара по матанализу в пятницу?",
                    status = ForumPostStatus.Active,
                    likesCount = 12,
                    commentsCount = 3,
                    likedByMe = true,
                    createdAtIso = "2026-08-28T09:00:00"
                ),
                timeAgo = "12 мин.",
                onClick = {},
                onLikeClick = {},
                onReportClick = {},
                onDeleteClick = {}
            )

            ThreadCard(
                post = ForumPost(
                    id = "2",
                    profileId = null,
                    displayName = "Аноним",
                    displayColor = "#94A3B8",
                    avatarImageUrl = null,
                    visibleRole = null,
                    isMine = false,
                    content = "",
                    status = ForumPostStatus.Active,
                    likesCount = 3,
                    commentsCount = 0,
                    likedByMe = false,
                    createdAtIso = "2026-09-03T09:00:00Z",
                    // Ссылок нет — превью показывает рамку нужных пропорций.
                    imageUrl = "",
                    imagePreviewUrl = "",
                    imageWidth = 1600,
                    imageHeight = 900
                ),
                timeAgo = "5 мин.",
                onClick = {},
                onLikeClick = {},
                onReportClick = {},
                onDeleteClick = {}
            )
        }
    }
}
