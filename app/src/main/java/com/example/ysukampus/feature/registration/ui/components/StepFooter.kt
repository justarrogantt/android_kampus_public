package com.example.ysukampus.feature.registration.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun ShowStepFooter(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .width(20.dp)
                .height(4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.second_text_color)
            )
        ) {

        }
        Spacer(modifier = Modifier.width(4.dp))
        Card(
            modifier = Modifier
                .width(8.dp)
                .height(4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.label_text_color)
            )
        ) { }
    }
}

@Preview(showBackground = false)
@Composable
fun previewShowStepFooter() {
    YsukampusTheme(dynamicColor = false) {
        ShowStepFooter()
    }
}