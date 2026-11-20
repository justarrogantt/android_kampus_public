package com.example.ysukampus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object YsuAdminColors {
    val Primary = Color(0xFF3B82F6)
    val PrimaryLight = Color(0xFF60A5FA)
    val PrimaryDark = Color(0xFF2D5FA8)
    val PrimaryBright = Color(0x1A3B82F6)
    val Background = Color(0xFFF0F4FF)
    val BackgroundSecondary = Color(0xFFE8EEFF)
    val BackgroundTertiary = Color(0xFFDCE7FE)
    val Surface = Color.White
    val SurfaceSecondary = Color(0x99FFFFFF)
    val Border = Color(0x383B82F6)
    val BorderStrong = Color(0xFF8AABF5)
    val Text = Color(0xFF0F1729)
    val TextSecondary = Color(0xFF2D5FA8)
    val TextMuted = Color(0xFF8FB4E0)
    val Success = Color(0xFF059669)
    val SuccessBg = Color(0x2D009966)
    val Warning = Color(0xFFE87000)
    val WarningBg = Color(0x26E87000)
    val Danger = Color(0xFFE80000)
    val DangerBg = Color(0x1BE80000)
}

object YsuAdminDimens {
    val ScreenPadding = 14.dp
    val HeaderTopPadding = 20.dp
    val HeaderBottomSpacing = 14.dp
    val NavHeight = 35.dp
    val SearchHeight = 35.dp
    val CardRadius = 20.dp
    val CardPadding = 15.dp
    val CardIconSize = 48.dp
    val CardIconRadius = 16.dp
    val BadgeHeight = 20.dp
    val SmallIconButton = 24.dp
    val CloseButton = 27.dp
    val DialogRadius = 28.dp
    val DialogPadding = 15.dp
    val SheetBlockRadius = 18.dp
    val SheetBlockPadding = 14.dp
    val InputHeight = 40.dp
    val TextAreaMinHeight = 92.dp
    val ActionButtonHeight = 44.dp
}

object YsuAdminTextStyles {
    val Logo @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
    val AdminSubtitle @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    val ScreenTitle @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
    val Tab @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val Count @Composable get() = TextStyle(fontFamily = Onest, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val TotalCount @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val CardTitle @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
    val CardSubtitle @Composable get() = TextStyle(fontFamily = Onest, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val Badge @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val DialogTitle @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    val SectionLabel @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.45.sp)
    val Input @Composable get() = TextStyle(fontFamily = Onest, fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val ActionButton @Composable get() = TextStyle(fontFamily = Unbounded, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

private val AdminScheme = lightColorScheme(
    primary = YsuAdminColors.Primary,
    onPrimary = Color.White,
    background = YsuAdminColors.Background,
    onBackground = YsuAdminColors.Text,
    surface = YsuAdminColors.Surface,
    onSurface = YsuAdminColors.Text,
    secondary = YsuAdminColors.PrimaryDark,
    outline = YsuAdminColors.BorderStrong
)

@Composable
fun YsuAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AdminScheme,
        typography = Typography,
        content = content
    )
}
