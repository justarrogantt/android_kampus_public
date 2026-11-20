package com.example.ysukampus.ui.main.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Картинка поста или комментария.
 *
 * В карточке показываем миниатюру (`forum/media/{id}?size=preview`), а по тапу
 * вызывающая сторона открывает оригинал на весь экран.
 *
 * Пропорции берём из размеров, пришедших вместе с постом: место под картинку
 * занято ещё до её загрузки, поэтому лента не прыгает, когда миниатюра доедет.
 */
@Composable
fun ContentImage(
    previewUrl: String,
    imageWidth: Int?,
    imageHeight: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = when {
        imageWidth != null && imageHeight != null && imageWidth > 0 && imageHeight > 0 ->
            // Вертикальные снимки ограничиваем: иначе одна картинка занимает
            // весь экран. Лишнее обрезается — как object-fit: cover в вебе.
            (imageWidth.toFloat() / imageHeight.toFloat()).coerceAtLeast(MIN_ASPECT_RATIO)

        else -> FALLBACK_ASPECT_RATIO
    }

    AsyncImage(
        model = previewUrl,
        // Картинка кликабельна: описание говорит, что произойдёт по тапу.
        contentDescription = stringResource(R.string.forum_image_open),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(ImageShape)
            .background(HomeColors.PrimarySoft)
            .border(1.5.dp, HomeColors.Border, ImageShape)
            .clickable(onClick = onClick)
    )
}

/** Размеров нет — считаем картинку альбомной 4:3, как это делает вебка. */
private const val FALLBACK_ASPECT_RATIO = 4f / 3f
private const val MIN_ASPECT_RATIO = 0.75f

private val ImageShape = RoundedCornerShape(16.dp)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ContentImagePreview() {
    YsukampusTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Без сети AsyncImage покажет пустую рамку — важно, что место занято.
            ContentImage(
                previewUrl = "",
                imageWidth = 1600,
                imageHeight = 900,
                onClick = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
