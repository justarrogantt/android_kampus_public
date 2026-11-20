package com.example.ysukampus.feature.registration.ui.registrationScreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.theme.YsukampusTheme
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.R
import com.example.ysukampus.feature.registration.ui.components.InstructionCard
import com.example.ysukampus.feature.registration.ui.components.PrimaryActionButton
import com.example.ysukampus.feature.registration.ui.components.ShowLogo
import com.example.ysukampus.feature.registration.ui.components.StateIcon
import com.example.ysukampus.feature.registration.ui.components.StateText

@Composable
fun ErrorRegistrationScreen(
    errorMessage: String? = null,
    onRetryClick: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colorResource(R.color.background_color)
            )
            .systemBarsPadding()
    ) {
        ShowLogo(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        )
        Spacer(modifier = Modifier.weight(1f))
        StateIcon(
            painter = painterResource(R.drawable.baseline_warning_amber_24),
            shape = RoundedCornerShape(24.dp),
            backgroundColorRes = R.color.error_registration_icon_background_color,
            borderColorRes = R.color.error_registration_icon_border_color,
            iconColorRes = R.color.error_registration_icon_color,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.weight(1f))
        StateText(
            title = stringResource(R.string.error_registration_title_text),
            label = errorMessage ?: stringResource(R.string.error_registration_label_text),
            titleStyle = MaterialTheme.typography.titleLarge,
            labelStyle = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        InstructionCard(
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryActionButton(
            text = stringResource(R.string.error_registration_button_text),
            gradientStartColor = R.color.telegram_fade_start,
            gradientEndColor = R.color.telegram_fade_end,
            leadingIcon = painterResource(R.drawable.telegramicon),
            onClick = {
                uriHandler.openUri(ApiEnvironmentHolder.telegramBotLink)
            },
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        PrimaryActionButton(
            text = stringResource(R.string.error_registration_retry_button_text),
            isBordered = true,
            onClick = onRetryClick,
            modifier = Modifier.padding(32.dp)
        )
    }

}

@Preview(showBackground = false)
@Composable
fun ErrorRegistrationScreenPreview() {
    YsukampusTheme(dynamicColor = false) {
        ErrorRegistrationScreen()
    }
}
