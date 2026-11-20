package com.example.ysukampus.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.Role

/**
 * Единый набор кривых и пружин оболочки.
 *
 * Тайминги держим в одном месте по той же причине, по которой цвета лежат в
 * [KampusColors]: движение должно читаться как одна система, а не как набор
 * случайных `tween(180)` по экранам. Правило простое — появление медленнее
 * исчезновения, нажатие отвечает мгновенно, а «дорогое» ощущение даёт
 * недоведённая до нуля пружина, а не длинная анимация.
 */
object KampusMotion {

    /** Основной вход: быстрый разгон, мягкое торможение (Material «emphasized decelerate»). */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Основной выход: содержимое уходит резче, чем приходит. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Нейтральная кривая для цветов и мелких размеров. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /**
     * Экран наезжает сбоку: смена вкладки и открытие треда.
     *
     * Длительность общая не для красоты: вкладки листаются пальцем, и всё,
     * что уезжает по горизонтали, должно двигаться в одном темпе — иначе
     * переход между вкладками и переход в тред читаются как разные жесты.
     *
     * Вебка здесь не образец: там вкладки только растворяются одна в другой
     * (`MainPage.tsx`), но у неё и жеста нет — а под пальцем перетекание
     * вместо движения выглядит неправильно.
     */
    const val ScreenSlideInMs = 340

    /** Экран уходит обратно. Как и везде, короче входа. */
    const val ScreenSlideOutMs = 220

    /** Перекраска, подсветка, смена иконки. */
    const val ColorMs = 200

    /** Ответ на палец: без отскока, иначе кнопка «дребезжит». */
    val PressSpring: FiniteAnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    /** Пилюля таббара, бейдж — всё, что должно чуть «пружинить». */
    fun <T> lively(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

    /** Появление блока внутри экрана. */
    fun <T> enter(durationMillis: Int = 300, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = EmphasizedDecelerate)

    /** Исчезновение блока внутри экрана. */
    fun <T> exit(durationMillis: Int = 150): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EmphasizedAccelerate)

    /** Насколько «проседает» карточка под пальцем. */
    const val CardPressScale = 0.975f

    /** Мелкие круглые кнопки жмутся заметнее — площадь меньше. */
    const val ButtonPressScale = 0.9f
}

/**
 * Сжимает элемент, пока его держат.
 *
 * Ставится **до** `clip`/`background` в цепочке модификаторов: `graphicsLayer`
 * масштабирует только то, что нарисовано после него, и фон, добавленный
 * раньше, остался бы на месте.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = KampusMotion.CardPressScale,
    enabled: Boolean = true
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = KampusMotion.PressSpring,
        label = "press-scale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Кликабельность оболочки: без ряби Material, но с откликом на нажатие.
 *
 * Рябь в этом дизайне не используется нигде (везде `indication = null`), и без
 * замены нажатие остаётся вообще без обратной связи — её и даёт масштаб.
 */
@Composable
fun Modifier.kampusPressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = KampusMotion.CardPressScale,
    role: Role? = null,
    onClickLabel: String? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .pressScale(interactionSource, pressedScale, enabled)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
}

/**
 * Короткий «пух» при включении состояния: лайк, новый бейдж.
 *
 * Возвращает готовый модификатор, а не значение, чтобы вызывающему не
 * приходилось помнить про [TransformOrigin] и держать свой [Animatable].
 */
@Composable
fun Modifier.popOnEnabled(
    enabled: Boolean,
    peakScale: Float = 1.3f
): Modifier {
    val scale = remember { Animatable(1f) }
    // Первый проход только запоминает состояние: анимируем переход, а не то,
    // что уже было включено при появлении на экране.
    var wasEnabled by remember { mutableStateOf(enabled) }
    LaunchedEffect(enabled) {
        if (enabled && !wasEnabled) {
            scale.animateTo(peakScale, tween(120, easing = KampusMotion.EmphasizedDecelerate))
            scale.animateTo(1f, KampusMotion.lively())
        }
        wasEnabled = enabled
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        transformOrigin = TransformOrigin.Center
    }
}
