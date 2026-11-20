package com.example.ysukampus.ui.main.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.schedule.ScheduleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel вкладки «Расписание».
 *
 * Договор с UI: наружу торчит один [uiState], внутрь приходят только вызовы
 * методов-событий (`onViewChange`, `onEntitySelected`, ...). Composable никогда
 * не ходит в сеть сам и не знает про репозиторий.
 *
 * Почему ViewModel, а не состояние внутри Composable: ViewModel переживает
 * пересоздание Activity (поворот экрана, смена темы) и не теряет загруженное
 * расписание. Composable при этом можно свободно рекомпозировать сколько угодно.
 */
class ScheduleViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    /**
     * `asStateFlow()` отдаёт read-only обёртку: снаружи состояние поменять нельзя,
     * менять его вправе только сама ViewModel.
     */
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    /**
     * Текущая загрузка активной вкладки.
     *
     * Держим ссылку, чтобы при переключении вкладки отменить предыдущий запрос:
     * иначе поздний ответ «Сегодня» мог бы прилететь уже после того, как
     * пользователь ушёл на «Неделю», и сбросить чужой лоадер.
     */
    private var loadJob: Job? = null
    private var nextWeekJob: Job? = null
    private var directoryJob: Job? = null

    init {
        // Первый запрос идёт без oid: бэк сам подставит группу владельца токена.
        loadActiveView()
    }

    fun onViewChange(view: ScheduleView) {
        if (_uiState.value.view == view) return

        _uiState.update { it.copy(view = view) }
        loadActiveView()
    }

    /**
     * Пользователь выбрал другую группу/преподавателя.
     *
     * Единственный путь смены сущности: им пользуются и пикер, и тап по
     * преподавателю/группе в карточке пары, и возврат к своему расписанию —
     * параллельной загрузки чужого расписания нет.
     */
    fun onEntitySelected(entity: ScheduleEntity) {
        val current = _uiState.value.selectedEntity
        if (current?.oid == entity.oid && current.type == entity.type) {
            _uiState.update { it.copy(isPickerVisible = false) }
            return
        }

        // Кэш всех вкладок относится к прежней сущности — сбрасываем целиком.
        _uiState.update {
            it.copy(
                selectedEntity = entity,
                today = null,
                tomorrow = null,
                week = null,
                loadedExtraWeeks = 0,
                isPickerVisible = false,
                hasError = false
            )
        }
        loadActiveView()
    }

    /**
     * Тап по преподавателю или группе в карточке пары: сначала спрашиваем
     * подтверждение, потому что промахнуться по плотной карточке легко.
     */
    fun onLessonEntityClick(entity: ScheduleEntity) {
        _uiState.update { it.copy(pendingEntity = entity) }
    }

    /** Переход подтверждён — дальше обычный путь смены сущности. */
    fun onPendingEntityConfirmed() {
        val entity = _uiState.value.pendingEntity ?: return
        _uiState.update { it.copy(pendingEntity = null) }
        onEntitySelected(entity)
    }

    /** Шторку закрыли свайпом, «Отменой» или системным «назад». */
    fun onPendingEntityDismissed() {
        _uiState.update { it.copy(pendingEntity = null) }
    }

    /**
     * Возврат из чужого расписания к своему. Сбрасываем выбор на сущность
     * владельца токена — запрос уйдёт без параметра `group`.
     */
    fun onReturnToOwnEntity() {
        val owner = _uiState.value.ownerEntity ?: return
        onEntitySelected(owner)
    }

    fun onOpenPicker() {
        _uiState.update { current ->
            current.copy(
                isPickerVisible = true,
                pickerQuery = "",
                // Открываем пикер на той же вкладке, к которой относится текущая сущность.
                pickerMode = current.selectedEntity?.type ?: current.pickerMode
            )
        }
        loadDirectory()
    }

    fun onDismissPicker() {
        _uiState.update { it.copy(isPickerVisible = false) }
    }

    fun onPickerModeChange(mode: ScheduleEntityType) {
        _uiState.update { it.copy(pickerMode = mode) }
    }

    fun onPickerQueryChange(query: String) {
        _uiState.update { it.copy(pickerQuery = query) }
    }

    /**
     * Открытие с главной по клику по дню: переключаем на неделю и запоминаем дату
     * для скролла. Загрузку недели дальше делает общий [onViewChange].
     */
    fun onFocusDate(dateIso: String) {
        _uiState.update { it.copy(focusDate = dateIso) }
        onViewChange(ScheduleView.Week)
    }

    /** Список доскроллил до нужного дня — дата больше не нужна. */
    fun onFocusDateHandled() {
        _uiState.update { it.copy(focusDate = null) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /** Повторить загрузку после ошибки. */
    fun onRetry() {
        loadActiveView(force = true)
    }

    /**
     * Догрузка следующей недели при скролле до конца списка.
     * Тянем максимум [ScheduleUiState.MAX_EXTRA_WEEKS] недель сверх текущей —
     * каждый скролл до низа добавляет по одной.
     */
    fun onLoadNextWeek() {
        val state = _uiState.value
        val week = state.week ?: return
        if (!state.canLoadMoreWeeks || state.isLoadingNextWeek) return
        // Запрос уже в полёте — второй скролл-триггер не должен его дублировать.
        if (nextWeekJob?.isActive == true) return

        val lastDate = week.days.lastOrNull()?.date ?: return

        nextWeekJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNextWeek = true) }

            repository.getWeek(
                week = ScheduleDates.plusWeekIso(lastDate),
                oid = state.requestOid
            )
                .onSuccess { result ->
                    _uiState.update { current ->
                        val previous = current.week
                            ?: return@update current.copy(isLoadingNextWeek = false)

                        // distinctBy — страховка от дублей на стыке недель.
                        val mergedDays = (previous.days + result.days).distinctBy { day -> day.date }

                        current.copy(
                            week = previous.copy(days = mergedDays),
                            isLoadingNextWeek = false,
                            loadedExtraWeeks = current.loadedExtraWeeks + 1
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingNextWeek = false,
                            message = error.message
                        )
                    }
                }
        }
    }

    /**
     * Грузит активную вкладку, если её ещё нет в кэше.
     * @param force игнорировать кэш (кнопка «Повторить»).
     */
    private fun loadActiveView(force: Boolean = false) {
        val state = _uiState.value
        val isCached = when (state.view) {
            ScheduleView.Today -> state.today != null
            ScheduleView.Tomorrow -> state.tomorrow != null
            ScheduleView.Week -> state.week != null
        }
        if (isCached && !force) {
            // Данные уже есть — гасим возможную ошибку от прошлой вкладки.
            _uiState.update { it.copy(isLoading = false, hasError = false) }
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }

            when (state.view) {
                ScheduleView.Today -> loadDay(ScheduleDates.todayIso(), ScheduleView.Today)
                ScheduleView.Tomorrow -> loadDay(ScheduleDates.tomorrowIso(), ScheduleView.Tomorrow)
                ScheduleView.Week -> loadWeek()
            }
        }
    }

    private suspend fun loadDay(dateIso: String, view: ScheduleView) {
        repository.getDay(date = dateIso, oid = _uiState.value.requestOid)
            .onSuccess { result ->
                _uiState.update { current ->
                    current
                        .copy(
                            today = if (view == ScheduleView.Today) result else current.today,
                            tomorrow = if (view == ScheduleView.Tomorrow) result else current.tomorrow,
                            isLoading = false,
                            hasError = false
                        )
                        .withResolvedEntity(result.entity)
                }
            }
            .onFailure(::handleLoadFailure)
    }

    private suspend fun loadWeek() {
        repository.getWeek(
            week = ScheduleDates.todayIso(),
            oid = _uiState.value.requestOid
        )
            .onSuccess { result ->
                _uiState.update { current ->
                    current
                        .copy(
                            week = result,
                            isLoading = false,
                            loadedExtraWeeks = 0,
                            hasError = false
                        )
                        .withResolvedEntity(result.entity)
                }
            }
            .onFailure(::handleLoadFailure)
    }

    private fun handleLoadFailure(error: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                hasError = true,
                message = error.message
            )
        }
    }

    /**
     * Сущность из ответа сервера. Нужна, когда запрос ушёл без `group`:
     * так мы узнаём свою группу и подписываем ею кнопку выбора.
     */
    private fun ScheduleUiState.withResolvedEntity(entity: ScheduleEntity): ScheduleUiState =
        if (selectedEntity == null) {
            // Запрос ушёл без `group`, значит сервер ответил сущностью владельца токена.
            copy(selectedEntity = entity, ownerEntity = ownerEntity ?: entity)
        } else {
            this
        }

    private fun loadDirectory() {
        if (_uiState.value.directory != null) return
        if (directoryJob?.isActive == true) return

        directoryJob = viewModelScope.launch {
            _uiState.update { it.copy(isDirectoryLoading = true) }

            repository.getDirectory()
                .onSuccess { directory ->
                    _uiState.update {
                        it.copy(directory = directory, isDirectoryLoading = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isDirectoryLoading = false, message = error.message)
                    }
                }
        }
    }
}
