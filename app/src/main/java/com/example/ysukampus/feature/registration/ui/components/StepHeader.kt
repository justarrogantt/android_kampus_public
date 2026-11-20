package com.example.ysukampus.feature.registration.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R

@Composable
fun ShowStepHeader(
    step: String,
    questionText: String,
    questionDescription: String,
    modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(
                top = 32.dp,
                start = 24.dp,
                end = 16.dp
            )
    ) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(R.color.label_text_color),
        )
        Text(
            text = questionText,
            style = MaterialTheme.typography.titleLarge,
            color = colorResource(R.color.main_text_color),
            modifier = Modifier
                .padding(
                    top = 16.dp
                )
        )
        Text(
            text = questionDescription,
            style = MaterialTheme.typography.labelLarge,
            color = colorResource(R.color.label_text_color),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
