package com.example.ysukampus.feature.registration.ui.components

import androidx.annotation.ColorRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.theme.YsukampusTheme


@Composable
fun PrimaryActionButton(
    text: String? = null,
    @ColorRes gradientStartColor: Int? = null,
    @ColorRes gradientEndColor: Int? = null,
    leadingIcon: Painter? = null,
    trailingIcon: Painter? = null,
    isBordered: Boolean? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startColor =
        gradientStartColor?.let { colorResource(it) } ?: colorResource(R.color.background_color)
    val endColor = gradientEndColor?.let { colorResource(it) } ?: startColor
    val gradient = Brush.linearGradient(
        colors = listOf(
            startColor,
            endColor
        )
    )
    Card(
        modifier = modifier
            .height(64.dp)
            .alpha(if (enabled) 1f else 0.5f),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        border = if (isBordered != null) {
            BorderStroke(
                width = 1.dp,
                color = colorResource(R.color.icon_border_default)
            )
        } else {
            BorderStroke(
                width = 0.dp,
                color = colorResource(R.color.icon_border_default)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradient),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        painter = leadingIcon,
                        contentDescription = null,
                        tint = colorResource(R.color.kampus_white)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (text != null) {
                    Text(
                        text = text,
                        style = if (gradientStartColor != null) {
                            MaterialTheme.typography.bodyLarge
                        } else {
                            MaterialTheme.typography.labelMedium
                        },
                        color = if (gradientStartColor != null) {
                            colorResource(R.color.kampus_white)
                        } else {
                            colorResource(R.color.label_text_color)
                        },
                    )
                }

                if (trailingIcon != null) {
                    Icon(
                        painter = trailingIcon,
                        contentDescription = null,
                        tint = colorResource(R.color.kampus_white)
                    )
                }
            }

        }
    }
}

@Preview(showBackground = false)
@Composable
fun previewPrimaryActionButton() {
    YsukampusTheme(dynamicColor = false) {
        PrimaryActionButton(
            text = stringResource(R.string.telegram_registration_button_text),
            gradientStartColor = R.color.telegram_fade_start,
            gradientEndColor = R.color.telegram_fade_end,
            leadingIcon = painterResource(R.drawable.telegramicon),
            onClick = {}
        )
    }

}
