package com.example.ysukampus.ui.main.forum.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.forum.ForumComment
import com.example.ysukampus.ui.main.forum.ForumPostStatus
import com.example.ysukampus.ui.main.forum.parseAuthorColor
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.theme.ForumTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Карточка комментария в треде.
 *
 * Собрана по образцу [ThreadCard]: та же шапка «аватар — имя — роль — время»
 * и то же действие справа (удалить своё, пожаловаться на чужое). Лайков и
 * счётчиков у комментария нет, поэтому нижней строки с метриками тоже нет,
 * а ответ на комментарий отмечается сдвигом влево.
 */
@Composable
fun CommentCard(
    comment: ForumComment,
    timeAgo: String,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Комментарий, к которому привело уведомление: подсвечен несколько секунд. */
    isHighlighted: Boolean = false,
    /** Тап по аватару или нику — открывает профиль автора. `null` — не кликабельны. */
    onAuthorClick: (() -> Unit)? = null
) {
    val authorColor = parseAuthorColor(comment.displayColor)

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Ответ на другой комментарий втянут вправо — видно, что он вложенный.
            .padding(start = if (comment.parentId != null) 24.dp else 0.dp)
            .clip(CardShape)
            .background(if (isHighlighted) HomeColors.PrimarySoft else HomeColors.Surface)
            .border(
                1.5.dp,
                if (isHighlighted) HomeColors.Primary else HomeColors.Border,
                CardShape
            )
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Avatar(
            imageUrl = comment.avatarImageUrl,
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
                    text = comment.displayName,
                    style = ForumTypography.AuthorName,
                    color = HomeColors.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(
                            if (onAuthorClick != null) Modifier.clickable(onClick = onAuthorClick) else Modifier
                        )
                )

                if (comment.isPostAuthor) {
                    RoleBadge(
                        name = stringResource(R.string.forum_comment_author_badge),
                        authorColor = authorColor
                    )
                } else {
                    comment.visibleRole?.let { role ->
                        RoleBadge(name = role, authorColor = authorColor)
                    }
                }

                Text(
                    text = timeAgo,
                    style = ForumTypography.Time,
                    color = HomeColors.TextMuted,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 5.dp)
                )

                if (comment.isMine) {
                    CommentActionButton(
                        iconRes = R.drawable.ic_forum_trash,
                        contentDescription = stringResource(R.string.forum_delete),
                        iconSize = 13.dp,
                        onClick = onDeleteClick
                    )
                } else {
                    CommentActionButton(
                        iconRes = R.drawable.ic_forum_report,
                        contentDescription = stringResource(R.string.forum_report),
                        iconSize = 11.dp,
                        onClick = onReportClick
                    )
                }
            }

            if (comment.content.isNotBlank()) {
                Text(
                    text = comment.content,
                    style = ForumTypography.Body,
                    color = HomeColors.TextMain,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            // Комментарий может состоять из одной картинки — текста тогда нет.
            comment.imagePreviewUrl?.let { previewUrl ->
                ContentImage(
                    previewUrl = previewUrl,
                    imageWidth = comment.imageWidth,
                    imageHeight = comment.imageHeight,
                    onClick = onImageClick,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Avatar(imageUrl: String?, authorColor: Color, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(HomeColors.PrimarySoft)
            .then(
                if (imageUrl == null) Modifier.border(2.dp, authorColor, CircleShape) else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl == null) {
            Icon(
                painter = painterResource(R.drawable.ic_profile_avatar),
                contentDescription = null,
                tint = authorColor,
                modifier = Modifier.size(16.dp)
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
private fun CommentActionButton(
    iconRes: Int,
    contentDescription: String,
    iconSize: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(25.dp)
            .clip(CircleShape)
            .background(HomeColors.PrimarySoft)
            .clickable(onClick = onClick),
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

private val CardShape = RoundedCornerShape(18.dp)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CommentCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CommentCard(
                comment = previewComment(
                    id = "1",
                    name = "Эмиль",
                    content = "Да, пара будет — расписание не меняли.",
                    isPostAuthor = true
                ),
                timeAgo = "12 мин.",
                onReportClick = {},
                onDeleteClick = {},
                onImageClick = {}
            )
            CommentCard(
                comment = previewComment(
                    id = "2",
                    name = "Аноним",
                    content = "Спасибо, а то уже собрался прогуливать.",
                    isMine = true,
                    parentId = "1"
                ),
                timeAgo = "5 мин.",
                onReportClick = {},
                onDeleteClick = {},
                onImageClick = {}
            )
        }
    }
}

/** Данные для превью: карточка рисуется без сети. */
private fun previewComment(
    id: String,
    name: String,
    content: String,
    isMine: Boolean = false,
    isPostAuthor: Boolean = false,
    parentId: String? = null
) = ForumComment(
    id = id,
    postId = "post-1",
    profileId = null,
    displayName = name,
    displayColor = "#3B82F6",
    avatarImageUrl = null,
    visibleRole = "Студент",
    isMine = isMine,
    isPostAuthor = isPostAuthor,
    content = content,
    parentId = parentId,
    status = ForumPostStatus.Active,
    likesCount = 0,
    repliesCount = 0,
    likedByMe = false,
    createdAtIso = "2026-09-02T09:00:00Z"
)
