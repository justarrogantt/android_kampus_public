package com.example.ysukampus.feature.registration.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun TelegramRegistrationCard(
    code: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.kampus_white)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 32.dp
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.telegram_registration_card_text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorResource(R.color.label_text_color)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = code,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge,
                    color = colorResource(R.color.second_text_color)
                )
            }
            Card(
                onClick = {},
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = colorResource(R.color.icon_border_default)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.icon_background_default)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd),
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_content_copy_24),
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = colorResource(R.color.icon_border_selected),
                )
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun TelegramRegistrationCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        TelegramRegistrationCard(code = "xK9mP2")
    }
}
