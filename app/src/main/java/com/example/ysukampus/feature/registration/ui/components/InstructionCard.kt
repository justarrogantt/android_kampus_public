package com.example.ysukampus.feature.registration.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun InstructionCard(modifier: Modifier = Modifier) {
    val steps = listOf(
        stringResource(R.string.error_registration_step_one_text),
        stringResource(R.string.error_registration_step_two_text),
        stringResource(R.string.error_registration_step_three_text)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.kampus_white)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            steps.forEachIndexed { index, step ->
                InstructionStep(
                    number = index + 1,
                    text = step
                )

                if (index != steps.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 36.dp),
                        color = colorResource(R.color.icon_border_default),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(
    number: Int,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = colorResource(R.color.icon_background_selected),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(R.color.second_text_color)
            )
        }

        Text(
            text = text,
            modifier = Modifier.width(0.dp).weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = colorResource(R.color.main_text_color)
        )
    }
}

@Preview(showBackground = false)
@Composable
fun InstructionCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        InstructionCard()
    }
}
