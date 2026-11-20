package com.example.ysukampus.feature.registration.ui.registrationScreens


import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.R
import com.example.ysukampus.feature.registration.ui.components.PrimaryActionButton
import com.example.ysukampus.feature.registration.ui.components.ShowLogo
import com.example.ysukampus.feature.registration.ui.components.StateIcon
import com.example.ysukampus.feature.registration.ui.components.StateText
import com.example.ysukampus.feature.registration.ui.components.TelegramRegistrationCard
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun TelegramRegistrationScreen(
    code: String,
    demoMessage: String? = null,
    isCodeReady: Boolean = code.isNotBlank(),
    onGetNewCodeClick: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_color))
            .systemBarsPadding()
    ) {
        ShowLogo(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        )
        Spacer(modifier = Modifier.weight(1f))
        StateIcon(
            painter = painterResource(R.drawable.baseline_article_24),
            shape = RoundedCornerShape(24.dp),
            borderColorRes = R.color.icon_border_selected,
            backgroundColorRes = R.color.icon_background_selected,
            iconColorRes = R.color.second_text_color,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.weight(1f))
        StateText(
            title = stringResource(R.string.telegram_registration_title_text),
            label = stringResource(R.string.telegram_registration_label_text),
            titleStyle = MaterialTheme.typography.titleLarge,
            labelStyle = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        TelegramRegistrationCard(
            code = code,
            modifier = Modifier
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.telegram_registration_confirmation_waiting_text),
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(R.color.label_text_color),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        if (!demoMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = demoMessage,
                style = MaterialTheme.typography.labelMedium,
                color = colorResource(R.color.main_text_color),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        PrimaryActionButton(
            text = stringResource(R.string.telegram_registration_button_text),
            gradientStartColor = R.color.telegram_fade_start,
            gradientEndColor = R.color.telegram_fade_end,
            leadingIcon = painterResource(R.drawable.telegramicon),
            enabled = isCodeReady,
            onClick = {
                uriHandler.openUri(telegramBotLinkWithStartPayload(code))
            },
            modifier = Modifier
                .padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryActionButton(
            text = stringResource(R.string.telegram_registration_get_new_registration_code_button_text),
            isBordered = true,
            onClick = onGetNewCodeClick,
            modifier = Modifier
                .padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.padding(bottom = 32.dp))
    }
}

private fun telegramBotLinkWithStartPayload(code: String): String {
    val botLink = ApiEnvironmentHolder.telegramBotLink
    val normalizedCode = code.trim()
    if (normalizedCode.isBlank()) return botLink

    val separator = if (botLink.contains("?")) "&" else "?"
    return "$botLink${separator}start=${Uri.encode(normalizedCode)}"
}

@Preview(showBackground = false)
@Composable
fun TelegramRegistrationScreenPreview() {
    YsukampusTheme(dynamicColor = false) {
        TelegramRegistrationScreen(
            code = "",
            demoMessage = null,
            isCodeReady = false,
            onGetNewCodeClick = {}
        )
    }
}
