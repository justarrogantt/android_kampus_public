# Заметки по Compose

Разбор приёмов, которые появились в проекте вместе со шторкой редактирования профиля
(`ui/main/profile/components/EditProfileSheet.kt`). Здесь не пересказ документации, а
объяснение, зачем каждый из них тут понадобился и на что он меняет поведение — чтобы
следующий экран не пришлось изобретать заново.

## Снап-флинг: колесо цветов

Веб-версия (`Profile/BottomSheet/EditProfile`) крутит палитру вручную: слушает `onScroll`,
читает `scrollLeft`, делит на ширину шага и коалесцирует всё это через
`requestAnimationFrame`, чтобы не дёргать layout по сорок раз за кадр. В Compose всё это
уже есть.

`FlingBehavior` — объект, который решает, куда доедет список после того, как палец
оторвался. Дефолтный просто гасит скорость по кривой затухания. Снап-версия дополнительно
доводит ближайший элемент до позиции прилипания:

```kotlin
val listState = rememberLazyListState(initialFirstVisibleItemIndex = ForumColors.indexOf(selected))
val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider)
```

### Прилипание к центру, а не к краю

По умолчанию элемент прилипает к **началу** области содержимого. Колесу нужен центр. Два
способа, и в проекте используются оба сразу:

```kotlin
SnapLayoutInfoProvider(listState, SnapPosition.Center)
```

и симметричный `contentPadding` шириной с половину свободного места:

```kotlin
val sidePadding = (maxWidth - ITEM_WIDTH) / 2
LazyRow(contentPadding = PaddingValues(horizontal = sidePadding), ...)
```

Отступы тут нужны не для красоты: без них крайние точки палитры физически не могут
доехать до центра экрана — списку некуда прокручиваться дальше своего содержимого.

### Ноль подлёта: один бросок — один цвет

Первая версия улетала от края до края палитры от любого резкого движения. Причина в том,
что снап работает в две фазы: сначала **подлёт** (decay — обычное инерционное затухание,
как у любого скролла), потом прилипание к ближайшему элементу. На десяти точках шириной
56 dp фаза подлёта пролетает всю палитру.

Лечится переопределением одного метода:

```kotlin
val base = SnapLayoutInfoProvider(listState, SnapPosition.Center)
object : SnapLayoutInfoProvider by base {
    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0f
}
```

`by base` — делегирование интерфейса: все методы уходят в `base`, кроме явно
переопределённого. Нулевой подлёт означает «не разгоняйся, сразу прилипай к соседу»; это
же поведение известно как short snapping и подходит любому пикеру с крупными шагами
(колёса дат, выбор количества). Длинным лентам, наоборот, нужен подлёт — иначе они станут
вязкими.

## `derivedStateOf`: считать реже, чем меняется источник

Какой цвет считается выбранным, определяется по позиции скролла:

```kotlin
val centerIndex by remember {
    derivedStateOf {
        val layout = listState.layoutInfo
        val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
        layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }?.index ?: 0
    }
}
```

`listState.layoutInfo` меняется на каждом кадре скролла. Прочитать его прямо в теле
композабла — значит перерисовывать всё вокруг шестьдесят раз в секунду.
`derivedStateOf` считает производное значение и уведомляет подписчиков, только когда
изменился **результат** — то есть когда центр реально переехал на другую точку. Это ровно
то, ради чего в вебе городили коалесцирование через `requestAnimationFrame`, только
декларативно.

Правило простое: если из часто меняющегося состояния получается редко меняющееся
(индекс, булев флаг «прокручено ли ниже шапки», отфильтрованный список) — это `derivedStateOf`.

## Однонаправленный поток и петли эффектов

Колесо и состояние связаны в обе стороны: скролл задаёт цвет, тап по точке задаёт скролл.
Это классический источник бесконечной петли. Разводится двумя `LaunchedEffect` с явным
условием выхода:

```kotlin
LaunchedEffect(centerIndex) {          // скролл -> состояние
    val color = ForumColors.values.getOrNull(centerIndex) ?: return@LaunchedEffect
    if (color != selected) onSelect(color)
}

LaunchedEffect(selected) {             // состояние -> скролл
    val target = ForumColors.indexOf(selected)
    if (!listState.isScrollInProgress && target != centerIndex) {
        listState.animateScrollToItem(target)
    }
}
```

Оба эффекта — «сообщи, если стало иначе»; совпадение значений обрывает цепочку.
Проверка `isScrollInProgress` во втором обязательна: без неё эффект начнёт доскроливать
список прямо под пальцем пользователя.

## `Crossfade`: две страницы в одной шторке

Веб гоняет форму и пикер через `AnimatePresence` + `motion.div` с `opacity`. Прямой аналог:

```kotlin
Crossfade(targetState = state.page, label = "edit-profile-page", modifier = Modifier.weight(1f))
```

Держит в композиции старое и новое состояние одновременно, пока идёт перекрёстное
затухание. `weight(1f)` здесь не косметика: без него внутренний `Column` с
`fillMaxHeight()` мерялся бы по содержимому и кнопка «Сохранить» уехала бы за экран.
`label` — имя для инспектора анимаций в Android Studio, на поведение не влияет.

Вложенную `ModalBottomSheet` вместо страницы ставить не стоит: две шторки друг над другом
конфликтуют за перетаскивание и за системную «назад».

## `BackHandler`: системная «назад» на второй странице

```kotlin
BackHandler(enabled = state.page == EditProfilePage.Picker) { onCloseEntityPicker() }
```

Регистрирует колбэк в `OnBackPressedDispatcher` и снимает его при уходе из композиции;
`enabled` включает и выключает перехват без пересборки. Так «назад» с пикера возвращает в
форму, а не схлопывает всю шторку — в вебе за это отвечал `TelegramBackButtonController`.

## `BoxWithConstraints`: layout от реальной ширины

```kotlin
BoxWithConstraints { val sidePadding = (maxWidth - ITEM_WIDTH) / 2 }
```

Даёт содержимому доступ к собственным констрейнтам, то есть позволяет считать размеры от
фактически доступного места, а не от догадок про экран. Стоит дороже обычного `Box`
(содержимое собирается в фазе измерения), так что для статичной вёрстки он не нужен —
только там, где число действительно зависит от ширины.

## `enabled` против `readOnly` у полей ввода

Выключенное `OutlinedTextField` блекнет — это правильный сигнал «сюда нельзя», но на время
короткого сохранения он читается как поломка. Если правку надо запретить, а текст оставить
читаемым:

```kotlin
readOnly = isBusy   // вместо enabled = !isBusy
```

Та же история у `Switch`: дефолтные `disabled`-цвета Material 3 делают из тумблера тёмную
кляксу. Если выключенное состояние живёт доли секунды, лучше задать `disabledChecked*` /
`disabledUnchecked*` теми же цветами, что и включённые, а блокировку показать в другом
месте — например, надписью на кнопке («Сохраняем…»).

## Анимации значений

`animateFloatAsState` / `animateColorAsState` — прямой аналог CSS-переходов: значение живёт
в состоянии, а композабл сам анимирует переход к новому.

```kotlin
val scale by animateFloatAsState(targetValue = if (isActive) 1.35f else 0.62f, label = "color-dot-scale")
```

Для точек палитры этого хватает. Сложные последовательности (несколько свойств с разными
задержками) делаются через `updateTransition`, а не через горсть отдельных `animate*AsState`.

## Чего в Compose нет из коробки

`mask-image` с градиентом — растворяющиеся края у колеса в вебе. В Compose это
`Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)` плюс
`drawWithContent` с `BlendMode.DstIn`: содержимое рисуется в отдельный слой, поверх
которого градиентом «выедается» альфа. Сейчас в проекте не сделано.
