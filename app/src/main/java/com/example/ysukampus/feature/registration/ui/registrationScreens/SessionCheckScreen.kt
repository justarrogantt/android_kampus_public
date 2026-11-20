package com.example.ysukampus.feature.registration.ui.registrationScreens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.ysukampus.R
import com.example.ysukampus.feature.registration.ui.components.ShowLogo
import com.example.ysukampus.ui.theme.YsukampusTheme

private const val SPIN_DURATION_MS = 1400
private const val PULSE_DURATION_MS = 700

/**
 * Стартовый экран, пока проверяется сохранённая сессия: access-токен живёт
 * минуты, поэтому почти на каждом запуске его приходится обновлять, и до
 * ответа бэкенда неизвестно, куда вести — в приложение или на авторизацию.
 *
 * Осознанно без текста: подпись успевала бы только мигнуть, а крутящаяся
 * фирменная «k» читается и за долю секунды.
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
            SpinningLetterK()
        }
    }
}

@Composable
private fun SpinningLetterK(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "session-check")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SPIN_DURATION_MS, easing = LinearEasing)
        ),
        label = "rotation"
    )
    // Небольшое «дыхание» вместе с вращением: без него равномерный оборот
    // выглядит как зависшая картинка, если проверка затягивается.
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Text(
        text = "k",
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 56.sp),
        color = colorResource(R.color.second_text_color),
        modifier = modifier
            .scale(scale)
            .graphicsLayer { rotationZ = rotation }
    )
}

@Preview(showBackground = false)
@Composable
private fun SessionCheckScreenPreview() {
    YsukampusTheme(dynamicColor = false) {
        SessionCheckScreen()
    }
}
