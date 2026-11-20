package com.example.ysukampus.ui.theme


import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ysukampus.R

// Шрифты синхронизированы с вебкой (`frontend/react/src/index.css`): там подключены те же
// вариативные файлы с осью `wght`, поэтому в приложении объявляем всю лестницу начертаний —
// иначе Compose подменяет 500/600/800 ближайшим статическим файлом и синтезирует жирность.

/** Начертание вариативного шрифта: сам вес + значение оси `wght`. */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/** Заголовки. Ось `wght` файла — 200..900, как в `@font-face` вебки. */
val Unbounded = FontFamily(
    variableFont(R.font.unbounded_variable, FontWeight.ExtraLight),
    variableFont(R.font.unbounded_variable, FontWeight.Light),
    variableFont(R.font.unbounded_variable, FontWeight.Normal),
    variableFont(R.font.unbounded_variable, FontWeight.Medium),
    variableFont(R.font.unbounded_variable, FontWeight.SemiBold),
    variableFont(R.font.unbounded_variable, FontWeight.Bold),
    variableFont(R.font.unbounded_variable, FontWeight.ExtraBold),
    variableFont(R.font.unbounded_variable, FontWeight.Black)
)

/** Основной текст. Ось `wght` файла — 100..900, вебка объявляет 200..900. */
val Onest = FontFamily(
    variableFont(R.font.onest_variable, FontWeight.ExtraLight),
    variableFont(R.font.onest_variable, FontWeight.Light),
    variableFont(R.font.onest_variable, FontWeight.Normal),
    variableFont(R.font.onest_variable, FontWeight.Medium),
    variableFont(R.font.onest_variable, FontWeight.SemiBold),
    variableFont(R.font.onest_variable, FontWeight.Bold),
    variableFont(R.font.onest_variable, FontWeight.ExtraBold),
    variableFont(R.font.onest_variable, FontWeight.Black)
)

/** Моноширинный. Ось `wght` — 100..800, начертания Black у файла нет. */
val JetBrainsMono = FontFamily(
    variableFont(R.font.jetbrainsmono_variable, FontWeight.Thin),
    variableFont(R.font.jetbrainsmono_variable, FontWeight.ExtraLight),
    variableFont(R.font.jetbrainsmono_variable, FontWeight.Light),
    variableFont(R.font.jetbrainsmono_variable, FontWeight.Normal),
    variableFont(R.font.jetbrainsmono_variable, FontWeight.Medium),
    variableFont(R.font.jetbrainsmono_variable, FontWeight.SemiBold),
    variableFont(R.font.jetbrainsmono_variable, FontWeight.Bold),
    variableFont(R.font.jetbrainsmono_variable, FontWeight.ExtraBold)
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = 0.12.em
    ),
    headlineMedium = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    labelLarge = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.08.em
    )
)

object ProfileTypography {
    val ScreenTitle = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp
    )

    val UserName = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        letterSpacing = 0.02.em
    )

    val GroupName = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    )

    val EditButton = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )

    val MenuItem = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp
    )

    val MenuSubtitle = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp
    )

    val AppVersion = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp
    )

    /**
     * Переключатель «Посты» / «Комментарии» в шторке активности — веб-класс
     * `.tab` из `UserProfileOverlay`: заголовочный Unbounded, а не Onest.
     */
    val ActivityTab = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp
    )

    val ThemeOption = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp
    )

    // --- Шторка редактирования профиля ---

    val SheetTitle = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
    )

    val FieldLabel = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )

    val FieldValue = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp
    )

    val FieldHint = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )

    val SaveButton = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
}

/**
 * Стили вкладки «Расписание». Собраны из CSS вебки:
 * заголовки — Unbounded, тело — Onest, время и аудитория — JetBrains Mono.
 */
object ScheduleTypography {
    val ScreenTitle = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.5).sp
    )

    val EntityButton = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )

    val Tab = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )

    val DayLabel = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.2).sp
    )

    /** «Пар в этот день нет» внутри списка недели. */
    val DayEmpty = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    val SlotNumber = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
    )

    val SlotTime = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp
    )

    val Auditorium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = (-0.3).sp
    )

    val AuditoriumInline = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = (-0.3).sp
    )

    val Discipline = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.25).sp
    )

    val LessonKind = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W800,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )

    val Meta = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val WeekDivider = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.3).sp
    )

    val Empty = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val PickerTitle = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = (-0.3).sp
    )

    val PickerItem = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp
    )

    val ConfirmTitle = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.3).sp
    )

    val ConfirmText = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )

    val ConfirmButton = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
    )
}

/**
 * Стили главной. Собраны из CSS вебки: заголовки — Unbounded, тело — Onest,
 * погода, время и «через сколько» — JetBrains Mono.
 */
object HomeTypography {
    val Logo = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp
    )

    val BetaBadge = TextStyle(
        fontFamily = JetBrainsMono,
        // Веб просит 900, но ось `wght` у JetBrains Mono кончается на 800 — берём максимум.
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp
    )

    val GreetingName = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.5).sp
    )

    val GreetingDate = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )

    val Weather = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )

    val DayCardName = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.sp,
        letterSpacing = 0.5.sp
    )

    val DayCardNumber = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 16.sp
    )

    val NextChip = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp
    )

    val NextTime = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )

    val NextSubject = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.5).sp
    )

    val NextKind = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 17.sp
    )

    val NextMeta = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )

    val NextEmpty = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    val SectionLabel = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.3.sp
    )

    val AllPosts = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp
    )
}

/** Стили карточки поста форума — используются главной и будущей вкладкой «Форум». */
object ForumTypography {
    val AuthorName = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 14.sp
    )

    val RoleBadge = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp
    )

    val Time = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp
    )

    val Body = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    val MetaCounter = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )

    /** Подпись пилюли «+ Новый пост» — веб-класс `.newThread`. */
    val NewPostPill = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 12.sp
    )
}

/**
 * Уведомления — перенос веб-классов `NotificationCard.module.css` и
 * `NotificationsBottomSheet.module.css` (вариант `.large`, шторка на весь экран).
 */
object NotificationsTypography {
    /** `.title` шторки. */
    val SheetTitle = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 19.sp
    )

    /** `.message`: «ник ответил(а) на ваш пост». Ник внутри — жирным. */
    val Message = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )

    /** `.preview` — текст комментария под сообщением. */
    val Preview = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp
    )

    /** `.emptyTitle` пустого состояния. */
    val EmptyTitle = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 18.sp
    )

    /** `.emptyText` — пояснение под ним. */
    val EmptyText = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp
    )

    /** `.moderationText` и `.readAllButton` — оба 14sp bold. */
    val Action = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )

    /** `.moderationCount` и бейдж на пункте профиля. */
    val Badge = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        lineHeight = 12.sp
    )

    /** `.unreadBadge` на колокольчике — он меньше и живёт поверх иконки. */
    val BellBadge = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 8.sp,
        lineHeight = 8.sp
    )
}
