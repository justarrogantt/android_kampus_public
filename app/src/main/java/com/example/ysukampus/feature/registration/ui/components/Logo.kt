package com.example.ysukampus.feature.registration.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R

@Composable
fun ShowLogo (
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start
) {
    Row(
        modifier = modifier
            .padding(
                top = 32.dp,
                start = 24.dp,
                end = 24.dp
            )
            .systemBarsPadding(),
        horizontalArrangement = horizontalArrangement
    ) {
        Text(
            text = "ysu",
            style = MaterialTheme.typography.headlineMedium,
            color = colorResource(R.color.main_text_color)
        )
        Text(
            text = "k",
            style = MaterialTheme.typography.headlineMedium,
            color = colorResource(R.color.second_text_color)
        )
        Text(
            text = "ampus",
            style = MaterialTheme.typography.headlineMedium,
            color = colorResource(R.color.main_text_color)
        )
    }
}