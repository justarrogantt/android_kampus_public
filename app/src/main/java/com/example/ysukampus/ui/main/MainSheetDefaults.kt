package com.example.ysukampus.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Общие размеры выезжающих снизу шторок-экранов.
 *
 * За эталон взята шторка уведомлений: она заметно ниже экрана, поэтому сверху
 * остаётся размытая оболочка и видно, откуда шторку открыли. Остальные шторки
 * раньше расходились (0.9 у подборщика групп, 0.92 у профиля и редактирования)
 * — теперь все берут высоту отсюда.
 *
 * Короткие шторки-подтверждения (`EntityConfirmSheet`, форма нового поста)
 * тянутся по содержимому и высоту отсюда не берут.
 */
object KampusSheet {
    /** Доля высоты окна, которую занимает шторка-экран. */
    const val HeightFraction = 0.82f
}

/**
 * Сколько шторок открыто прямо сейчас. Считаем, а не храним булев флаг: шторки
 * умеют перекрывать друг друга (из профиля открывается редактирование, из
 * расписания — подборщик и следом подтверждение), и закрытие верхней не должно
 * снимать размытие, пока под ней осталась ещё одна.
 */
@Stable
class KampusSheetBlurState {
    private var openCount by mutableIntStateOf(0)

    val isAnySheetOpen: Boolean get() = openCount > 0

    fun onSheetOpened() {
        openCount++
    }

    fun onSheetClosed() {
        openCount--
    }
}

/**
 * Счётчик уровня всей оболочки. Значение по умолчанию нужно только превью:
 * без [MainScreen] размывать всё равно нечего.
 */
val LocalKampusSheetBlur = staticCompositionLocalOf { KampusSheetBlurState() }

/**
 * Пока композабл жив, оболочка под ним размыта.
 *
 * Вызывается из самой шторки, а не из места её показа: состояние открытия у
 * каждой шторки своё (у одних — флаг в оболочке, у других — поле ViewModel
 * вкладки), а вот присутствие в композиции — общий и надёжный признак.
 *
 * Содержимое `ModalBottomSheet` живёт в отдельном окне и под это размытие не
 * попадает — размывается ровно то, что осталось позади.
 */
@Composable
fun BlurShellWhileVisible() {
    val blurState = LocalKampusSheetBlur.current
    DisposableEffect(blurState) {
        blurState.onSheetOpened()
        onDispose { blurState.onSheetClosed() }
    }
}
