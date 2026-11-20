package com.example.ysukampus.feature.registration.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun StudentCard(
    name: String,
    usernametg: String,
    role: String,
    group: String,
    modifier: Modifier = Modifier
){
    val subtitleTop = listOf(usernametg, role)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    val subtitle = if (group.isNotBlank()) {
        listOf(subtitleTop, group)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    } else {
        subtitleTop
    }

    val initials = name
        .trim()
        .split("\\s+".toRegex())
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")


    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.kampus_white)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colorResource(R.color.icon_border_default)
        ),
        modifier = modifier
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.second_text_color)
                ),
                modifier = Modifier
                    .width(42.dp)
                    .height(42.dp),
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorResource(R.color.kampus_white),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = colorResource(R.color.main_text_color)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorResource(R.color.label_text_color)
                )
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewStudentCard(){
    YsukampusTheme(dynamicColor = false) {
        StudentCard(
            name = "Егор Дудоров",
            usernametg = "@alihor123",
            role = "Студент",
            group = "ПИ-41Б"
        )
    }
}
