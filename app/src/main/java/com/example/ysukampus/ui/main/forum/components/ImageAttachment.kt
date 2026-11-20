package com.example.ysukampus.ui.main.forum.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.forum.ForumImageDraft
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Кнопка «прикрепить картинку».
 *
 * Сам выбор файла запускает вызывающий экран: `PickVisualMedia` требует
 * launcher'а, а карточка кнопки должна оставаться пригодной для @Preview.
 * Пока картинка уже выбрана, кнопка выключена — вложение всегда одно.
 */
@Composable
fun ImageAttachButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(HomeColors.PrimarySoft)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_forum_image),
            contentDescription = stringResource(R.string.forum_image_attach),
            tint = HomeColors.Primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Превью выбранной картинки в черновике.
 *
 * Пока файл летит на сервер, превью приглушено и поверх крутится индикатор:
 * отправка в это время всё равно заблокирована — `image_id` ещё не пришёл.
 * Крестик убирает вложение и отменяет незавершённую загрузку.
 */
@Composable
fun ImageAttachPreview(
    image: ForumImageDraft,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uri = image.uri ?: return

    Box(
        modifier = modifier
            .size(72.dp)
            .clip(PreviewShape)
            .background(HomeColors.PrimarySoft)
            .border(1.5.dp, HomeColors.Border, PreviewShape)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = stringResource(R.string.forum_image_content_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (image.isUploading) UPLOADING_ALPHA else 1f)
        )

        if (image.isUploading) {
            CircularProgressIndicator(
                color = HomeColors.Primary,
                strokeWidth = 2.5.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(HomeColors.TextMain)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_forum_close),
                contentDescription = stringResource(R.string.forum_image_remove),
                tint = KampusColors.Surface,
                modifier = Modifier.size(8.dp)
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.45f
private const val UPLOADING_ALPHA = 0.45f

private val PreviewShape = RoundedCornerShape(14.dp)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ImageAttachmentPreview() {
    YsukampusTheme(dynamicColor = false) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageAttachButton(enabled = true, onClick = {})
            ImageAttachButton(enabled = false, onClick = {})
            // Uri без файла: превью останется пустой рамкой, но покажет индикатор.
            ImageAttachPreview(
                image = ForumImageDraft(uri = Uri.EMPTY, isUploading = true),
                onRemove = {}
            )
            ImageAttachPreview(
                image = ForumImageDraft(uri = Uri.EMPTY, attachmentId = "a1"),
                onRemove = {}
            )
        }
    }
}
