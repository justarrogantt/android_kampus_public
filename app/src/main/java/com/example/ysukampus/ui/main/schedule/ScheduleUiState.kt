package com.example.ysukampus.ui.main.schedule

/** Три режима вкладки, как в вебке. */
enum class ScheduleView {
    Today,
    Tomorrow,
    Week
}

/**
 * Полный снимок экрана расписания.
 *
 * Один immutable-объект на весь экран: Compose перерисовывает только то, что
 * реально изменилось, а мы получаем предсказуемое состояние без разъезжающихся
 * между собой отдельных `mutableStateOf`. Все поля — `val`, новое состояние
 * создаётся через `copy()`.
 *
 * Загруженные вкладки кэшируются здесь же ([today]/[tomorrow]/[week]) — это
 * мобильный аналог ScheduleContext из вебки. Возврат на уже загруженную вкладку
 * не порождает запрос.
 */
data class ScheduleUiState(
    val view: ScheduleView = ScheduleView.Today,

    /**
     * Чьё расписание показываем. `null` — сущность ещё не известна: первый запрос
     * уходит без параметра `group`, и сервер подставляет группу из профиля.
     * После первого успешного ответа заполняется из `entity`.
     */
    val selectedEntity: ScheduleEntity? = null,

    /**
     * Сущность владельца токена — та, что пришла в ответе на первый запрос
     * (он уходит без `group`). Нужна, чтобы отличать своё расписание от чужого
     * и возвращаться к себе из карточки чужой пары.
     */
    val ownerEntity: ScheduleEntity? = null,

    val today: ScheduleDayResult? = null,
    val tomorrow: ScheduleDayResult? = null,
    val week: ScheduleWeekResult? = null,

    /** Идёт загрузка активной вкладки — показываем скелетон. */
    val isLoading: Boolean = false,
    /** Догрузка следующей недели внизу списка. */
    val isLoadingNextWeek: Boolean = false,
    /** Сколько дополнительных недель уже догружено; потолок — [MAX_EXTRA_WEEKS]. */
    val loadedExtraWeeks: Int = 0,
    /** Активную вкладку загрузить не удалось — вместо списка показываем заглушку. */
    val hasError: Boolean = false,

    /**
     * Группа или преподаватель, по которым тапнули в карточке пары: ждём
     * подтверждения перехода. Карточка плотная, промахнуться легко — поэтому
     * спрашиваем, а не уводим с экрана сразу.
     */
    val pendingEntity: ScheduleEntity? = null,

    val isPickerVisible: Boolean = false,
    val directory: ScheduleDirectory? = null,
    val isDirectoryLoading: Boolean = false,
    val pickerMode: ScheduleEntityType = ScheduleEntityType.Group,
    val pickerQuery: String = "",

    /**
     * День, к которому нужно проскроллить неделю. Приходит с главной по клику
     * по карточке дня и гасится, как только список к нему прокрутился.
     */
    val focusDate: String? = null,

    /** Разовое сообщение для снекбара. Экран гасит его через onMessageShown(). */
    val message: String? = null
) {
    /**
     * `oid` для запроса. Для своей сущности параметр не отправляем: бэк с
     * опциональной авторизацией сам подставит группу владельца токена — так же,
     * как на самом первом запросе, когда сущность ещё не известна.
     */
    val requestOid: String?
        get() = selectedEntity?.oid?.takeIf { it != ownerEntity?.oid }

    /** Открыто чужое расписание — показываем плашку возврата к своему. */
    val isForeignEntity: Boolean
        get() = ownerEntity != null && selectedEntity != null && selectedEntity.oid != ownerEntity.oid

    /** Осталось ли что догружать вниз списка. */
    val canLoadMoreWeeks: Boolean
        get() = loadedExtraWeeks < MAX_EXTRA_WEEKS

    /** Данные активной вкладки для дневных режимов. */
    val activeDay: ScheduleDayResult?
        get() = when (view) {
            ScheduleView.Today -> today
            ScheduleView.Tomorrow -> tomorrow
            ScheduleView.Week -> null
        }

    /** Есть ли что показать в текущей вкладке. */
    val hasContent: Boolean
        get() = if (view == ScheduleView.Week) week != null else activeDay != null

    /**
     * Подпись пилюли выбора: у преподавателей сначала сокращаем ФИО до
     * «Фамилия И. О.», а затем обрезаем по длине — иначе длинная фамилия
     * растянула бы пилюлю на всю шапку и вытеснила заголовок с колокольчиком.
     */
    val selectedEntityLabel: String
        get() {
            val entity = selectedEntity ?: return "Выбрать"
            if (entity.name.isBlank()) return "Выбрать"

            val label = when (entity.type) {
                ScheduleEntityType.Lecturer -> ScheduleDates.formatLecturerShortName(entity.name)
                ScheduleEntityType.Group -> entity.name
            }

            return clampEntityLabel(label)
        }

    /**
     * Имя выбранной группы — нужно карточке пары, чтобы для потоковых занятий
     * показать «ПИ41б и ещё 4» вместо простыни из групп.
     */
    val selectedGroupName: String
        get() = selectedEntity?.takeIf { it.type == ScheduleEntityType.Group }?.name.orEmpty()

    /** Отфильтрованный по поиску список для пикера. */
    val pickerMembers: List<ScheduleEntity>
        get() {
            val source = when (pickerMode) {
                ScheduleEntityType.Group -> directory?.groups
                ScheduleEntityType.Lecturer -> directory?.lecturers
            }.orEmpty()

            val query = pickerQuery.trim()
            if (query.isEmpty()) return source

            return source.filter { it.name.contains(query, ignoreCase = true) }
        }

    companion object {
        /**
         * Сколько недель догружаем сверх текущей. Две — это горизонт «на две
         * недели вперёд»: дальше расписание на бэке всё равно обычно пустое.
         */
        const val MAX_EXTRA_WEEKS = 2

        /**
         * Сколько символов помещается в пилюлю выбора. Значение подобрано под
         * самый узкий экран: «Долматов А. В.» влезает целиком, а всё, что длиннее,
         * обрезается по границе слова и получает многоточие.
         */
        const val ENTITY_LABEL_MAX_CHARS = 13

        /** Обрезает подпись по последнему пробелу, чтобы не рвать слово посередине. */
        private fun clampEntityLabel(label: String): String {
            if (label.length <= ENTITY_LABEL_MAX_CHARS) return label

            val head = label.take(ENTITY_LABEL_MAX_CHARS).trimEnd()
            val wordBreak = head.lastIndexOf(' ')
            // Слишком ранний пробел («А. Долматов») не режем — иначе от подписи
            // остался бы один инициал.
            val cut = if (wordBreak >= ENTITY_LABEL_MAX_CHARS / 2) head.take(wordBreak) else head

            return cut.trimEnd(' ', '.', ',') + "…"
        }
    }
}
