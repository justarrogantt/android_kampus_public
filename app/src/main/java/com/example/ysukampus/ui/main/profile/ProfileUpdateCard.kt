package com.example.ysukampus.ui.main.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.theme.ProfileTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun ProfileUpdateCard(
    isVisible: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KampusColors.Surface),
        border = BorderStroke(1.dp, KampusColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFEAF2FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_update_sync),
                    contentDescription = stringResource(R.string.beta_update_icon_description),
                    tint = if (enabled) KampusColors.PrimaryBlue else KampusColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.profile_update_card_title),
                    color = if (enabled) KampusColors.TextMain else KampusColors.TextMuted,
                    style = ProfileTypography.MenuItem
                )
                Text(
                    text = stringResource(R.string.profile_update_card_subtitle),
                    color = KampusColors.TextMuted,
                    style = ProfileTypography.MenuSubtitle
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = stringResource(R.string.profile_update_card_title),
                tint = if (enabled) KampusColors.MutedBlue else KampusColors.TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileUpdateCardPreview() {
    YsukampusTheme(dynamicColor = false) {
        ProfileUpdateCard(
            isVisible = true,
            enabled = true,
            onClick = {}
        )
    }
}
