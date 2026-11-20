package com.example.ysukampus.ui.main.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.theme.ForumTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Бейдж роли автора: текст — цветом автора, фон — им же с прозрачностью 15%
 * (в вебке это alpha-суффикс `26` к hex-цвету).
 */
@Composable
fun RoleBadge(
    name: String,
    authorColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = name,
        style = ForumTypography.RoleBadge,
        color = authorColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .widthIn(max = 140.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(authorColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun RoleBadgePreview() {
    YsukampusTheme(dynamicColor = false) {
        RoleBadge(
            name = "Модератор",
            authorColor = Color(0xFF3B82F6),
            modifier = Modifier.padding(8.dp)
        )
    }
}
