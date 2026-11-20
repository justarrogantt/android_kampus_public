package com.example.ysukampus.feature.registration.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R

@Composable
fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = Color.Unspecified
        )
    } else {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = colorResource(R.color.icon_border_default),
                    shape = CircleShape
                )
        )
    }
}