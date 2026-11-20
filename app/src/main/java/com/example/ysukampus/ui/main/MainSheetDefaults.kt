package com.example.ysukampus.ui.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.launch

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
 * Шторка на `ModalBottomSheet` передаёт свой [sheetState]: сама она просит её
 * закрыть (`onDismissRequest`) только после того, как доехала вниз, и по одному
 * присутствию в композиции блюр начинал гаснуть, когда шторки на экране уже нет.
 * По `targetValue` блюр снимается в тот же кадр, когда шторка поехала вниз, и
 * гаснет вместе с ней. Если при свайпе её вернули обратно, блюр вернётся тоже.
 *
 * Содержимое `ModalBottomSheet` живёт в отдельном окне и под это размытие не
 * попадает — размывается ровно то, что осталось позади.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurShellWhileVisible(sheetState: SheetState) {
    BlurShellWhile(isBlurring = sheetState.targetValue != SheetValue.Hidden)
}

/**
 * Закрытие шторки с кнопки внутри неё. Прямой вызов `onDismiss` убирает
 * `ModalBottomSheet` из композиции сразу, и шторка пропадает без анимации;
 * здесь она сперва уезжает вниз — так же, как при свайпе или тапе по скриму,
 * — и только потом сообщает оболочке, что закрыта.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAnimatedSheetDismiss(sheetState: SheetState, onDismiss: () -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    return remember(sheetState, scope) {
        {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) currentOnDismiss()
            }
        }
    }
}

/** Для самописных шторок без [SheetState]: блюр держится, пока композабл жив. */
@Composable
fun BlurShellWhileVisible() {
    BlurShellWhile(isBlurring = true)
}

@Composable
private fun BlurShellWhile(isBlurring: Boolean) {
    val blurState = LocalKampusSheetBlur.current
    DisposableEffect(blurState, isBlurring) {
        if (isBlurring) blurState.onSheetOpened()
        onDispose { if (isBlurring) blurState.onSheetClosed() }
    }
}
