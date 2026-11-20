package com.example.ysukampus.feature.registration.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun StateText(
    title: String,
    label: String,
    titleStyle: TextStyle,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = titleStyle,
            color = colorResource(R.color.main_text_color),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = label,
            style = labelStyle,
            color = colorResource(R.color.label_text_color),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = false)
@Composable
fun TelegramStateTextPreview() {
    YsukampusTheme(dynamicColor = false) {
        StateText(
            title = stringResource(R.string.telegram_registration_title_text),
            label = stringResource(R.string.telegram_registration_label_text),
            titleStyle = MaterialTheme.typography.titleLarge,
            labelStyle = MaterialTheme.typography.labelMedium
        )
    }
}

@Preview(showBackground = false)
@Composable
fun SuccessStateTextPreview() {
    YsukampusTheme(dynamicColor = false) {
        StateText(
            title = stringResource(R.string.success_registration_title_text),
            label = stringResource(R.string.success_registration_label_text),
            titleStyle = MaterialTheme.typography.titleLarge,
            labelStyle = MaterialTheme.typography.labelMedium
        )
    }
}

@Preview(showBackground = false)
@Composable
fun ErrorStateTextPreview() {
    YsukampusTheme(dynamicColor = false) {
        StateText(
            title = stringResource(R.string.error_registration_title_text),
            label = stringResource(R.string.error_registration_label_text),
            titleStyle = MaterialTheme.typography.titleLarge,
            labelStyle = MaterialTheme.typography.labelMedium
        )
    }
}
