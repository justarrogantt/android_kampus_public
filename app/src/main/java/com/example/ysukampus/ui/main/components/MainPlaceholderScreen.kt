package com.example.ysukampus.ui.main.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.theme.Onest
import com.example.ysukampus.ui.theme.Unbounded

@Composable
fun MainPlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KampusColors.AppBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = KampusColors.TextMain,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Unbounded,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(28.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = KampusColors.Surface),
            border = BorderStroke(1.dp, KampusColors.Border),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = KampusColors.TextMain,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Unbounded,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Раздел в разработке",
                    color = KampusColors.TextMuted,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = Onest)
                )
            }
        }
    }
}
