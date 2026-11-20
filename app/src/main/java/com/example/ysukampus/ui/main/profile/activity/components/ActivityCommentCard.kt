package com.example.ysukampus.ui.main.profile.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.forum.ForumActivityComment
import com.example.ysukampus.ui.main.forum.ForumActivityContext
import com.example.ysukampus.ui.main.forum.ForumComment
import com.example.ysukampus.ui.main.forum.ForumPostStatus
import com.example.ysukampus.ui.main.forum.components.ContentImage
import com.example.ysukampus.ui.main.forum.components.RoleBadge
import com.example.ysukampus.ui.main.forum.parseAuthorColor
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.theme.ForumTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Комментарий в шторке «Профиль» — свой или чужой (публичный профиль другого
 * пользователя переиспользует ту же карточку).
 *
 * От [com.example.ysukampus.ui.main.forum.components.CommentCard] отличается
 * двумя вещами, ради которых карточка и заведена отдельно: над текстом стоит
 * контекст (пост или комментарий, на который отвечали), а тап по карточке
 * открывает тред — в самом треде открывать нечего. Кнопка справа — жалоба или
 * удаление — переключается по `comment.isMine`, как и в [CommentCard].
 */
@Composable
fun ActivityCommentCard(
    activity: ForumActivityComment,
    timeAgo: String,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val comment = activity.comment
    val authorColor = parseAuthorColor(comment.displayColor)
    // Ответ показывает родительский комментарий, обычный комментарий — пост.
    val context = activity.replyContext ?: activity.postContext

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(HomeColors.Surface)
            .border(1.5.dp, HomeColors.Border, CardShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActivityAvatar(imageUrl = comment.avatarImageUrl, authorColor = authorColor)

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
                    modifier = Modifier.weight(1f, fill = false)
                )

                comment.visibleRole?.let { role ->
                    RoleBadge(name = role, authorColor = authorColor)
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
                    ActivityActionButton(
                        iconRes = R.drawable.ic_forum_trash,
                        contentDescription = stringResource(R.string.forum_delete),
                        onClick = onDeleteClick
                    )
                } else {
                    ActivityActionButton(
                        iconRes = R.drawable.ic_forum_report,
                        contentDescription = stringResource(R.string.forum_report),
                        onClick = onReportClick
                    )
                }
            }

            ActivityContextBlock(
                context = context,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (comment.content.isNotBlank()) {
                Text(
                    text = comment.content,
                    style = ForumTypography.Body,
                    color = HomeColors.TextMain,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            comment.imagePreviewUrl?.let { previewUrl ->
                ContentImage(
                    previewUrl = previewUrl,
                    imageWidth = comment.imageWidth,
                    imageHeight = comment.imageHeight,
                    onClick = onImageClick,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityMetaButton(
                    iconRes = if (comment.likedByMe) {
                        R.drawable.ic_forum_like_filled
                    } else {
                        R.drawable.ic_forum_like
                    },
                    contentDescription = stringResource(R.string.forum_like),
                    text = comment.likesCount.toString(),
                    tint = if (comment.likedByMe) HomeColors.Danger else HomeColors.TextAccent,
                    onClick = onLikeClick
                )
                ActivityMetaButton(
                    iconRes = R.drawable.ic_forum_comment,
                    contentDescription = stringResource(R.string.forum_comments),
                    text = comment.repliesCount.toString(),
                    tint = HomeColors.TextAccent,
                    onClick = onClick
                )

                Spacer(modifier = Modifier.weight(1f))

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

/** Счётчик-пилюля под комментарием — тот же вид, что и у поста в `ThreadCard`. */
@Composable
private fun ActivityMetaButton(
    iconRes: Int,
    contentDescription: String,
    text: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(HomeColors.PrimarySoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = ForumTypography.MetaCounter,
            color = tint
        )
    }
}

/** Цитата того, на что отвечали: автор и начало текста. */
@Composable
private fun ActivityContextBlock(context: ForumActivityContext, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HomeColors.PrimarySoft),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Полоска слева — как `border-left` у цитаты в вебке.
        Box(
            modifier = Modifier
                .width(3.dp)
                .heightIn(min = 34.dp)
                .fillMaxHeight()
                .background(HomeColors.Primary)
        )

        Column(modifier = Modifier.padding(end = 10.dp, top = 6.dp, bottom = 6.dp)) {
        Text(
            text = context.displayName,
            style = ForumTypography.AuthorName,
            color = HomeColors.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            // Пост может состоять из одной картинки — тогда текста нет.
            text = context.content.ifBlank { stringResource(R.string.my_profile_context_photo) },
            style = ForumTypography.MetaCounter,
            color = HomeColors.TextMain,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        }
    }
}

@Composable
private fun ActivityAvatar(imageUrl: String?, authorColor: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(HomeColors.PrimarySoft)
            .then(
                if (imageUrl == null) Modifier.border(2.dp, authorColor, CircleShape) else Modifier
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
private fun ActivityActionButton(
    iconRes: Int,
    contentDescription: String,
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
            modifier = Modifier.size(13.dp)
        )
    }
}

private val CardShape = RoundedCornerShape(16.dp)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ActivityCommentCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        ActivityCommentCard(
            activity = ForumActivityComment(
                comment = ForumComment(
                    id = "c1",
                    postId = "p1",
                    profileId = "me",
                    displayName = "Эмиль",
                    displayColor = "#3B82F6",
                    avatarImageUrl = null,
                    visibleRole = null,
                    isMine = true,
                    isPostAuthor = false,
                    content = "Пара точно будет, расписание не меняли",
                    parentId = null,
                    status = ForumPostStatus.Active,
                    likesCount = 2,
                    repliesCount = 0,
                    likedByMe = false,
                    createdAtIso = "2026-09-02T09:00:00Z"
                ),
                postContext = ForumActivityContext(
                    id = "p1",
                    displayName = "Аноним",
                    content = "Кто-нибудь знает, будет ли пара по матанализу?"
                )
            ),
            timeAgo = "12 мин.",
            onClick = {},
            onLikeClick = {},
            onReportClick = {},
            onDeleteClick = {},
            onImageClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
