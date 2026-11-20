# Theme tokens for Jetpack Compose

Используй этот файл как источник дизайн-токенов для Android UI.

## Colors

Создай файл вроде `YsuAdminColors.kt`.

```kotlin
package your.package.ui.theme

import androidx.compose.ui.graphics.Color

object YsuAdminColors {
    val Primary = Color(0xFF3B82F6)
    val PrimaryLight = Color(0xFF60A5FA)
    val PrimaryDark = Color(0xFF2D5FA8)
    val PrimaryBright = Color(0x1A3B82F6)

    val Background = Color(0xFFF0F4FF)
    val BackgroundSecondary = Color(0xFFE8EEFF)
    val BackgroundTertiary = Color(0xFFDCE7FE)
    val BackgroundQuaternary = Color(0xFFD0DCFF)

    val Surface = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0x99FFFFFF)

    val Border = Color(0x383B82F6)
    val BorderStrong = Color(0xFF8AABF5)

    val Text = Color(0xFF0F1729)
    val TextSecondary = Color(0xFF2D5FA8)
    val TextMuted = Color(0xFF8FB4E0)

    val Success = Color(0xFF059669)
    val SuccessBg = Color(0x2D009966)
    val SuccessBorder = Color(0xFF6EE7B7)

    val Warning = Color(0xFFE87000)
    val WarningBg = Color(0x26E87000)

    val Danger = Color(0xFFE80000)
    val DangerBg = Color(0x1BE80000)

    val Scrim = Color(0x1A000000)
}
```

## Gradient

Основная синяя кнопка использует градиент:

```kotlin
Brush.linearGradient(
    colors = listOf(YsuAdminColors.Primary, YsuAdminColors.PrimaryLight)
)
```

## Typography

В идеале подключить `Unbounded` и `Onest`, если они уже находятся в Android-проекте. Если нет — использовать `FontFamily.SansSerif` и сохранить веса/размеры.

Рекомендуемые стили:

```kotlin
object YsuAdminTextStyles {
    val Logo = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 28.sp
    )

    val AdminSubtitle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp
    )

    val ScreenTitle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
        lineHeight = 20.sp
    )

    val Tab = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )

    val Count = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )

    val CardTitle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp
    )

    val CardSubtitle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp
    )

    val Badge = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )

    val DialogTitle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.25.sp
    )

    val SectionLabel = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.45.sp
    )

    val Input = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )

    val ActionButton = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold
    )
}
```

## Dimensions

Создай объект вроде `YsuAdminDimens`.

```kotlin
object YsuAdminDimens {
    val ScreenPadding = 14.dp
    val HeaderTopPadding = 20.dp
    val HeaderGap = 6.dp
    val HeaderBottomSpacing = 14.dp

    val TitleRowHeight = 24.dp
    val NavHeight = 35.dp
    val NavHorizontalPadding = 16.dp
    val NavGap = 5.dp

    val SearchHeight = 35.dp
    val SearchRadius = 15.dp
    val SearchStartPadding = 44.dp

    val CardRadius = 20.dp
    val CardPadding = 15.dp
    val CardBottomSpacing = 8.dp
    val CardIconSize = 48.dp
    val CardIconRadius = 16.dp
    val CardIconInnerSize = 20.dp
    val CardRowGap = 12.dp

    val BadgeHeight = 20.dp
    val BadgeHorizontalPadding = 10.dp

    val SmallIconButton = 24.dp
    val CloseButton = 27.dp

    val DialogRadius = 28.dp
    val DialogPadding = 15.dp
    val DialogWidthFraction = 0.90f
    val DialogHeightFraction = 0.90f

    val SheetBlockRadius = 18.dp
    val SheetBlockPadding = 14.dp
    val SheetBlockBottomSpacing = 12.dp

    val InputHeight = 40.dp
    val InputRadius = 12.dp
    val TextAreaMinHeight = 92.dp
    val TextAreaRadius = 14.dp

    val ActionButtonHeight = 44.dp
    val ActionButtonRadius = 16.dp
    val ActionGap = 8.dp
}
```

## Shapes

```kotlin
val PillShape = RoundedCornerShape(999.dp)
val CardShape = RoundedCornerShape(20.dp)
val IconBoxShape = RoundedCornerShape(16.dp)
val DialogShape = RoundedCornerShape(28.dp)
val SheetBlockShape = RoundedCornerShape(18.dp)
val InputShape = RoundedCornerShape(12.dp)
val ButtonShape = RoundedCornerShape(16.dp)
```

## Border rules

- Common border: `1.dp` with `YsuAdminColors.Border`.
- Strong border: `1.dp` or `1.5.dp` equivalent with `YsuAdminColors.BorderStrong`.
- Selected list item border: `YsuAdminColors.PrimaryLight`.
- Inputs focused state: `PrimaryLight` border.

Compose does not support 1.5dp borders perfectly in every context. Use `1.dp` or `1.5.dp` if available; visual consistency is more important than exact CSS values.

## Elevation rules

Use soft elevations:

```kotlin
CardDefaults.cardElevation(defaultElevation = 2.dp)
ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 1.dp)
```

For the dialog panel:

```kotlin
CardDefaults.cardElevation(defaultElevation = 6.dp)
```

## Status badge colors

```text
Active:    text Success, background SuccessBg
Soft del:  text Warning, background WarningBg
Danger:    text Danger, background DangerBg
Primary:   text PrimaryDark, background PrimaryBright
```

Status texts:

```text
активна
активно
удалена
удалено
```
