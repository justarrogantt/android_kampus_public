package com.example.ysukampus.ui.main.forum.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ysukampus.R
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Полноэкранный просмотр картинки поверх открытого поста.
 *
 * Живёт в [Dialog] с выключенной шириной по умолчанию: так он перекрывает и
 * нижние вкладки, а системный «назад» и тап по подложке приходят одним и тем же
 * `onDismissRequest` — отдельный `BackHandler` не нужен.
 *
 * Масштаб и сдвиг держим локально: наружу это состояние никому не нужно, а
 * двойной тап возвращает картинку в исходный размер.
 */
@Composable
fun ForumImageViewer(
    imageUrl: String,
    isSaving: Boolean,
    onClose: () -> Unit,
    onDownload: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    /** В админке сохранять чужой контент в галерею незачем — кнопку прячем. */
    canDownload: Boolean = true
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        // Возврат к исходному масштабу анимируем: скачок читается как сбой.
        val animatedScale by animateFloatAsState(scale, label = "viewerScale")
        val animatedOffsetX by animateFloatAsState(offsetX, label = "viewerOffsetX")
        val animatedOffsetY by animateFloatAsState(offsetY, label = "viewerOffsetY")

        fun reset() {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
                // Тап мимо картинки закрывает просмотр; рябь тут не нужна.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.forum_image_content_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                // На исходном масштабе двигать нечего.
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClose() },
                            onDoubleTap = {
                                if (scale > 1f) reset() else scale = DOUBLE_TAP_SCALE
                            }
                        )
                    }
                    .graphicsLayer(
                        scaleX = animatedScale,
                        scaleY = animatedScale,
                        translationX = animatedOffsetX,
                        translationY = animatedOffsetY
                    )
            )

            ViewerActionButton(
                iconRes = R.drawable.ic_forum_close,
                contentDescription = stringResource(R.string.forum_image_viewer_close),
                iconSize = 12.dp,
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            )

            if (!canDownload) {
                // Место под кнопку не занимаем: просмотр остаётся «только смотреть».
            } else if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(22.dp)
                        .size(20.dp)
                )
            } else {
                ViewerActionButton(
                    iconRes = R.drawable.ic_forum_download,
                    contentDescription = stringResource(R.string.forum_image_download),
                    iconSize = 16.dp,
                    onClick = onDownload,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                )
            }

            // Итог сохранения виден поверх диалога, а не под ним.
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                snackbarHost()
            }
        }
    }
}

@Composable
private fun ViewerActionButton(
    iconRes: Int,
    contentDescription: String,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(ButtonColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

private val ScrimColor = Color(0xF2000000)
private val ButtonColor = Color(0x59FFFFFF)

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ForumImageViewerPreview() {
    YsukampusTheme(dynamicColor = false) {
        // Ссылки нет — превью показывает подложку и обе кнопки, сеть не нужна.
        ForumImageViewer(
            imageUrl = "",
            isSaving = false,
            onClose = {},
            onDownload = {}
        )
    }
}
