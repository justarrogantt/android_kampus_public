package com.example.ysukampus.feature.registration.ui.registrationScreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.feature.registration.ui.components.PrimaryActionButton
import com.example.ysukampus.feature.registration.ui.components.ShowLogo

@Composable
fun AuthorizedActionsScreen(
    statusMessage: String? = null,
    onOpenAdminPanelClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_color))
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        ShowLogo(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.authorized_actions_title),
            style = MaterialTheme.typography.titleLarge,
            color = colorResource(R.color.main_text_color),
            fontWeight = FontWeight.Bold
        )
        if (!statusMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(R.color.label_text_color)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryActionButton(
            text = "Открыть админ панель",
            gradientStartColor = R.color.telegram_fade_start,
            gradientEndColor = R.color.telegram_fade_end,
            onClick = onOpenAdminPanelClick,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
