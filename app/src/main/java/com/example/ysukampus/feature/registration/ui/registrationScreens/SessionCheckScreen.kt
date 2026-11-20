package com.example.ysukampus.feature.registration.ui.registrationScreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.feature.registration.ui.components.ShowLogo
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Стартовый экран, пока проверяется сохранённая сессия. Осознанно без текста:
 * проверка занимает доли секунды, и подпись успевала бы только мигнуть.
 */
@Composable
fun SessionCheckScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_color))
            .systemBarsPadding()
    ) {
        ShowLogo(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = colorResource(R.color.second_text_color),
                strokeWidth = 3.dp
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
private fun SessionCheckScreenPreview() {
    YsukampusTheme(dynamicColor = false) {
        SessionCheckScreen()
    }
}
