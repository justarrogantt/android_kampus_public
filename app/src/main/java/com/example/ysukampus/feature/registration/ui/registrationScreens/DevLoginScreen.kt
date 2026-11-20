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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.feature.registration.ui.components.PrimaryActionButton
import com.example.ysukampus.feature.registration.ui.components.ShowLogo
import com.example.ysukampus.feature.registration.ui.components.StateIcon
import com.example.ysukampus.feature.registration.ui.components.StateText
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun DevLoginScreen(
    message: String?,
    isLoading: Boolean,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(colorResource(R.color.background_color))
            .padding(horizontal = 32.dp)
    ) {
        ShowLogo(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        )
        Spacer(modifier = Modifier.weight(1f))
        StateIcon(
            painter = painterResource(R.drawable.baseline_check_36),
            shape = CircleShape,
            backgroundColorRes = R.color.success_registration_icon_background_color,
            borderColorRes = R.color.success_registration_icon_border_color,
            iconColorRes = R.color.success_registration_icon_color,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(32.dp))
        StateText(
            title = stringResource(R.string.dev_login_title_text),
            label = stringResource(R.string.dev_login_label_text),
            titleStyle = MaterialTheme.typography.titleLarge,
            labelStyle = MaterialTheme.typography.labelMedium
        )
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(R.color.main_text_color)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (!isLoading) {
            PrimaryActionButton(
                text = stringResource(R.string.dev_login_button_text),
                gradientStartColor = R.color.second_text_color,
                gradientEndColor = R.color.fade_end,
                trailingIcon = painterResource(R.drawable.ic_arrow_right),
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = false)
@Composable
private fun DevLoginScreenPreview() {
    YsukampusTheme(dynamicColor = false) {
        DevLoginScreen(
            message = "Dev backend доступен, можно войти без Telegram",
            isLoading = false,
            onLoginClick = {}
        )
    }
}
