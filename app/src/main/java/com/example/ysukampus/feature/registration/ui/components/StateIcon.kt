package com.example.ysukampus.feature.registration.ui.components

import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.theme.YsukampusTheme
import com.example.ysukampus.R

@Composable
fun StateIcon(
    painter: Painter,
    shape: Shape,
    @ColorRes backgroundColorRes: Int,
    @ColorRes borderColorRes: Int,
    @ColorRes iconColorRes: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = colorResource(borderColorRes),
                shape = shape
            )
            .clip(shape)
            .background(colorResource(backgroundColorRes))
            .size(72.dp),
        contentAlignment = Alignment.Center,

    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = colorResource(iconColorRes),
        )
    }
}

@Preview(showBackground = false)
@Composable
fun TelegramStateIconPreview() {
    YsukampusTheme(dynamicColor = false) {
        StateIcon(
            painter = painterResource(R.drawable.baseline_article_24),
            shape = RoundedCornerShape(16.dp),
            borderColorRes = R.color.icon_border_selected,
            backgroundColorRes = R.color.icon_background_selected,
            iconColorRes = R.color.second_text_color
        )
    }
}

@Preview(showBackground = false)
@Composable
fun SuccessStateIconPreview() {
    YsukampusTheme(dynamicColor = false) {
        StateIcon(
            painter = painterResource(R.drawable.baseline_check_36),
            shape = CircleShape,
            borderColorRes = R.color.success_registration_icon_border_color,
            backgroundColorRes = R.color.success_registration_icon_background_color,
            iconColorRes = R.color.success_registration_icon_color
        )
    }
}

@Preview(showBackground = false)
@Composable
fun ErrorStateIconPreview() {
    YsukampusTheme(dynamicColor = false) {
        StateIcon(
            painter = painterResource(R.drawable.baseline_warning_amber_24),
            shape = RoundedCornerShape(16.dp),
            borderColorRes = R.color.error_registration_icon_border_color,
            backgroundColorRes = R.color.error_registration_icon_background_color,
            iconColorRes = R.color.error_registration_icon_color
        )
    }
}

