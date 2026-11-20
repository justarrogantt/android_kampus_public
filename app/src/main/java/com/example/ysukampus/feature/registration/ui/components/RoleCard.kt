package com.example.ysukampus.feature.registration.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import kotlinx.coroutines.selects.select

@Composable
fun RoleCard(
    title: String,
    description: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            colorResource(R.color.card_selected)
        } else {
            colorResource(R.color.kampus_white)
        },
        label = "containerColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            colorResource(R.color.second_text_color)
        } else {
            colorResource(R.color.icon_border_default)
        },
        label = "borderColor"
    )

//    val backgroundColor by animateColorAsState(
//        targetValue = if (selected) {
//            colorResource(R.color.icon_background_selected)
//        } else {
//            colorResource(R.color.icon_background_default)
//        }
//    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        label = "scale"
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(modifier),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(
                    vertical = 24.dp,
                    horizontal = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) {
                            colorResource(R.color.icon_background_selected)
                        } else {
                            colorResource(R.color.icon_background_default)
                        }
                    )
                    .border(
                        width = 1.dp,
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            colorResource(R.color.icon_border_selected)
                        } else {
                            colorResource(R.color.icon_border_default)
                        }

                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = colorResource(R.color.icon_color)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorResource(R.color.main_text_color)
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorResource(R.color.label_text_color)
                )
            }

            SelectionIndicator(selected = selected)
        }
    }
}