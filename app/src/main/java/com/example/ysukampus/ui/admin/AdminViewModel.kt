package com.example.ysukampus.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.network.admin.toAdminModerationItem
import com.example.ysukampus.data.network.ws.ModeratorWsEvent
import com.example.ysukampus.ui.admin.data.AdminRepository
import com.example.ysukampus.ui.admin.state.AdminEvent
import com.example.ysukampus.ui.admin.state.ADMIN_CHART_MAX_METRICS
import com.example.ysukampus.ui.admin.state.AdminAccessSection
import com.example.ysukampus.ui.admin.state.AdminGenerateReportResult
import com.example.ysukampus.ui.admin.state.AdminHistoryEntityType
import com.example.ysukampus.ui.admin.state.AdminReportJobStatus
import com.example.ysukampus.ui.admin.state.AdminReportListItem
import com.example.ysukampus.ui.admin.state.AdminReportMetric
import com.example.ysukampus.ui.admin.state.AdminReportPeriod
import com.example.ysukampus.ui.admin.state.AdminStatsView
import com.example.ysukampus.ui.admin.state.PERMISSION_GENERATE_DAILY_REPORT
import com.example.ysukampus.ui.admin.state.PERMISSION_VIEW_DAILY_REPORT
import com.example.ysukampus.ui.admin.state.availableAccessSections
import com.example.ysukampus.ui.admin.state.availableAdminTabs
import com.example.ysukampus.ui.admin.state.AdminModerationItem
import com.example.ysukampus.ui.admin.state.AdminModerationEvent
import com.example.ysukampus.ui.admin.state.AdminModerationEventTargetType
import com.example.ysukampus.ui.admin.state.AdminModerationReport
import com.example.ysukampus.ui.admin.state.MODERATION_STATUS_ACTIVE
import com.example.ysukampus.ui.admin.state.PERMISSION_MODERATION_HISTORY
import com.example.ysukampus.ui.admin.state.PERMISSION_RESTORE_CONTENT
import com.example.ysukampus.ui.admin.state.MODERATION_STATUS_HIDDEN
import com.example.ysukampus.ui.admin.state.AdminModerationTab
import com.example.ysukampus.ui.admin.state.AdminModerationTargetType
import com.example.ysukampus.ui.admin.state.AdminRequestLogsFilter
import com.example.ysukampus.ui.admin.state.AdminUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: AdminRepository
) : ViewModel() {
    /**
     * Права приходят вместе с карточкой пользователя в ответе авторизации.
     * `null` — сессия старая и прав в кэше нет: тогда ничего не прячем и
     * полагаемся на 403 от бэкенда, как раньше.
     */
    private val permissions: Set<String>? = repository.currentUserPermissions()
    private val tabs: List<AdminTab> = availableAdminTabs(permissions)
    private val accessSections: List<AdminAccessSection> = availableAccessSections(permissions)
    private val _uiState = MutableStateFlow(
        AdminUiState(
            // Вход остаётся на «Пользователях»: так модерация не поднимает свой
            // сокет до того, как её реально открыли. Если права на них закрыты —
            // берём первую доступную вкладку, как вебка.
            selectedTab = if (AdminTab.Users in tabs) AdminTab.Users else tabs.firstOrNull() ?: AdminTab.Users,
            availableTabs = tabs,
            selectedAccessSection = accessSections.firstOrNull() ?: AdminAccessSection.Roles,
            availableAccessSections = accessSections,
            isLoading = true,
            canViewModerationHistory = hasPermission(PERMISSION_MODERATION_HISTORY),
            canRestoreModerationContent = hasPermission(PERMISSION_RESTORE_CONTENT),
            canViewReports = hasPermission(PERMISSION_VIEW_DAILY_REPORT),
            canGenerateReports = hasPermission(PERMISSION_GENERATE_DAILY_REPORT),
            generateReportDate = yesterdayIsoDate()
        )
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()
    private var moderationLoadJob: Job? = null
    private var statsPollJob: Job? = null
    private var chartLoadJob: Job? = null

    init {
        observeModeratorSocketState()
        observeModeratorEvents()
        loadInitialTab()
    }

    fun onEvent(event: AdminEvent) {
        when (event) {
            is AdminEvent.TabSelected -> selectTab(event.tab)
            is AdminEvent.ModerationTabSelected -> selectModerationTab(event.tab)
            is AdminEvent.AccessSectionSelected -> selectAccessSection(event.section)
            is AdminEvent.StatsViewSelected -> selectStatsView(event.view)
            is AdminEvent.ChartMetricToggled -> toggleChartMetric(event.metric)
            is AdminEvent.ChartPeriodSelected -> selectChartPeriod(event.period)
            is AdminEvent.ReportSelected -> openReport(event.date)
            AdminEvent.GenerateReportPanelOpened -> openGeneratePanel()
            AdminEvent.GenerateReportPanelClosed -> closeGeneratePanel()
            is AdminEvent.GenerateReportDateChanged -> updateGenerateDate(event.date)
            is AdminEvent.GenerateReportSubmitted -> submitGenerateReport(event.force)
            is AdminEvent.SearchChanged -> updateSearch(event.query)
            AdminEvent.FreshModerationContentClicked -> applyFreshModerationContent()
            is AdminEvent.ModerationItemSelected -> selectModerationItem(event.item)
            is AdminEvent.ModerationReportSelected -> selectModerationReport(event.report)
            is AdminEvent.ModerationVerdictClicked -> applyModerationVerdict(event)
            is AdminEvent.ModerationHistoryEventSelected -> selectModerationEvent(event.event)
            is AdminEvent.RestoreModerationContentClicked ->
                restoreModerationContent(event.targetType, event.targetId)
            is AdminEvent.UserSelected -> selectUser(event.user)
            is AdminEvent.RoleSelected -> selectRole(event.role)
            is AdminEvent.PermissionSelected -> selectPermission(event.permission)
            AdminEvent.DialogClosed -> closeDialog()
            AdminEvent.CreateRoleClicked -> openAddRoleDialog()
            AdminEvent.CreatePermissionClicked -> openAddPermissionDialog()
            AdminEvent.RefreshClicked -> refreshCurrentTab()
            AdminEvent.ErrorConsumed -> _uiState.update { it.copy(errorMessage = null) }
            AdminEvent.MessageConsumed -> _uiState.update { it.copy(messageText = null) }
            is AdminEvent.SaveRoleClicked -> saveRole(event)
            is AdminEvent.SavePermissionClicked -> savePermission(event)
            is AdminEvent.SoftDeleteRoleClicked -> softDeleteRole(event.roleId)
            is AdminEvent.HardDeleteRoleClicked -> hardDeleteRole(event.roleId)
            is AdminEvent.RestoreRoleClicked -> restoreRole(event.roleId)
            is AdminEvent.SoftDeletePermissionClicked -> softDeletePermission(event.permissionId)
            is AdminEvent.HardDeletePermissionClicked -> hardDeletePermission(event.permissionId)
            is AdminEvent.RestorePermissionClicked -> restorePermission(event.permissionId)
            is AdminEvent.AssignRoleToUserClicked -> assignRoleToUser(event.userId, event.roleId)
            is AdminEvent.SoftRemoveRoleFromUserClicked -> softRemoveRoleFromUser(event.userId, event.roleId)
            is AdminEvent.HardRemoveRoleFromUserClicked -> hardRemoveRoleFromUser(event.userId, event.roleId)
            is AdminEvent.AssignPermissionToRoleClicked -> assignPermissionToRole(event.roleId, event.permissionId)
            is AdminEvent.SoftRemovePermissionFromRoleClicked -> softRemovePermissionFromRole(event.roleId, event.permissionId)
            is AdminEvent.HardRemovePermissionFromRoleClicked -> hardRemovePermissionFromRole(event.roleId, event.permissionId)
            is AdminEvent.HistoryEntitySelected -> selectHistoryEntity(event.entityType, event.entityId)
            is AdminEvent.HistoryRecordSelected -> selectHistoryRecord(event.recordId)
            AdminEvent.HistoryBackToRecordsClicked -> backToHistoryRecords()
            is AdminEvent.RestoreHistoryRecordClicked -> restoreHistoryRecord(event.logId)
            AdminEvent.HistoryRefreshClicked -> loadHistoryChangedLists()
            AdminEvent.RequestLogsRefreshClicked -> refreshRequestLogs()
            is AdminEvent.RequestLogsPageChanged -> changeRequestLogsPage(event.page)
            AdminEvent.RequestLogsFilterClicked -> openRequestLogsFilter()
            is AdminEvent.RequestLogsFilterApplied -> applyRequestLogsFilter(event.filter)
            AdminEvent.RequestLogsFilterReset -> resetRequestLogsFilter()
            is AdminEvent.RequestLogSelected -> selectRequestLog(event.logId)
            is AdminEvent.DeleteRequestLogClicked -> deleteRequestLog(event.logId)
            is AdminEvent.RequestLogDeleteConfirmationRequested -> openDeleteRequestLog(event.logId)
        }
    }

    override fun onCleared() {
        statsPollJob?.cancel()
        repository.stopModeratorSocket()
        super.onCleared()
    }

    private fun openDeleteRequestLog(logId: Int) {
        _uiState.update {
            it.copy(activeDialog = AdminDialogState.ConfirmDeleteRequestLog(logId))
        }
    }

    private fun loadInitialTab() = loadTabData(_uiState.value.selectedTab)

    /** Каждая вкладка грузит только свои данные — так же, как вкладки вебки. */
    private fun loadTabData(tab: AdminTab) {
        when (tab) {
            AdminTab.Moderation -> {
                markModerationTabOpened()
                loadModerationData(initialLoad = true)
                repository.startModeratorSocket()
            }
            AdminTab.Stats -> loadStatsData()
            AdminTab.Access -> loadAccessSectionData(_uiState.value.selectedAccessSection)
            AdminTab.Logging -> loadRequestLogs(page = 1, filter = _uiState.value.requestLogsFilter)
            AdminTab.Users -> loadAdminData()
        }
    }

    private fun loadAccessSectionData(section: AdminAccessSection) {
        when (section) {
            AdminAccessSection.History -> loadHistoryChangedLists()
            else -> loadAdminData()
        }
    }

    private fun loadAdminData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                Triple(
                    repository.getUsers(),
                    repository.getRoles(),
                    repository.getPermissions()
                )
            }.onSuccess { (users, roles, permissions) ->
                val rolePermissions = selectedRolePermissions()
                _uiState.update {
                    it.copy(
                        users = users,
                        roles = roles,
                        permissions = permissions,
                        selectedRolePermissions = rolePermissions,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить данные админ-панели"
                    )
                }
            }
        }
    }

    private fun refreshAdminData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null, messageText = null) }
            runCatching {
                Triple(
                    repository.getUsers(),
                    repository.getRoles(),
                    repository.getPermissions()
                )
            }.onSuccess { (users, roles, permissions) ->
                val rolePermissions = selectedRolePermissions()
                _uiState.update {
                    it.copy(
                        users = users,
                        roles = roles,
                        permissions = permissions,
                        selectedRolePermissions = rolePermissions,
                        isRefreshing = false,
                        messageText = "Данные обновлены"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "Не удалось обновить данные админ-панели"
                    )
                }
            }
        }
    }

    private fun selectTab(tab: AdminTab) {
        val previousTab = _uiState.value.selectedTab
        _uiState.update {
            it.copy(
                selectedTab = tab,
                searchQuery = "",
                selectedUser = null,
                selectedRole = null,
                selectedPermission = null,
                selectedRolePermissions = emptyList(),
                selectedHistoryEntityType = null,
                selectedHistoryEntityId = null,
                selectedHistoryRecords = emptyList(),
                selectedHistoryRecord = null,
                selectedRequestLog = null,
                activeDialog = null
            )
        }
        if (previousTab == AdminTab.Moderation && tab != AdminTab.Moderation) {
            repository.stopModeratorSocket()
        }
        if (previousTab == AdminTab.Stats && tab != AdminTab.Stats) {
            statsPollJob?.cancel()
        }
        loadTabData(tab)
    }

    private fun selectAccessSection(section: AdminAccessSection) {
        _uiState.update {
            it.copy(
                selectedAccessSection = section,
                searchQuery = "",
                activeDialog = null
            )
        }
        loadAccessSectionData(section)
    }

    private fun refreshCurrentTab() {
        when (_uiState.value.selectedTab) {
            AdminTab.Moderation -> loadModerationData(isManualRefresh = true)
            AdminTab.Stats -> loadStatsData(isManualRefresh = true)
            AdminTab.Access -> when (_uiState.value.selectedAccessSection) {
                AdminAccessSection.History -> loadHistoryChangedLists(isManualRefresh = true)
                else -> refreshAdminData()
            }
            AdminTab.Logging -> refreshRequestLogs()
            AdminTab.Users -> refreshAdminData()
        }
    }

    /**
     * Перед переключением запоминаем прошлую отметку просмотра вкладки, а сам
     * заход сразу отмечаем — так карточки остаются «новыми» до следующего раза.
     */
    private fun selectModerationTab(tab: AdminModerationTab) {
        val seenAt = repository.moderationTabSeenAt(tab)
        repository.markModerationTabSeen(tab)
        _uiState.update {
            it.copy(
                selectedModerationTab = tab,
                searchQuery = "",
                moderationSeenAtMillis = seenAt
            )
        }
    }

    // --- Статистика -------------------------------------------------------

    private fun selectStatsView(view: AdminStatsView) {
        _uiState.update { it.copy(selectedStatsView = view, isGeneratePanelOpen = false) }
        when (view) {
            // Живой слепок за сегодня перезапрашиваем при каждом заходе: он
            // считается на лету и за время просмотра успевает устареть.
            AdminStatsView.Today -> loadTodayMetrics(force = true)
            AdminStatsView.Chart -> loadChartSeries()
            AdminStatsView.Reports -> Unit
        }
    }

    private fun toggleChartMetric(metric: AdminReportMetric) {
        val current = _uiState.value.chartMetrics
        val next = when {
            metric in current -> current - metric
            current.size >= ADMIN_CHART_MAX_METRICS -> current
            else -> current + metric
        }
        // Пустой график рисовать нечем — последнюю метрику не снимаем.
        if (next.isEmpty() || next == current) return
        _uiState.update { it.copy(chartMetrics = next) }
        loadChartSeries()
    }

    private fun selectChartPeriod(period: AdminReportPeriod) {
        if (_uiState.value.chartPeriod == period) return
        _uiState.update { it.copy(chartPeriod = period) }
        // Набор метрик тот же, изменился только диапазон — кэш тут не подходит.
        loadChartSeries(force = true)
    }

    private fun loadStatsData(isManualRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isReportsLoading = true,
                    isRefreshing = isManualRefresh,
                    errorMessage = null,
                    messageText = if (isManualRefresh) null else it.messageText
                )
            }
            runCatching { repository.getDailyReports() }
                .onSuccess { reports ->
                    _uiState.update {
                        it.copy(
                            reports = reports,
                            isReportsLoading = false,
                            isRefreshing = false,
                            isLoading = false
                        )
                    }
                    startStatsPollingIfNeeded()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isReportsLoading = false,
                            isRefreshing = false,
                            isLoading = false,
                            errorMessage = error.message ?: "Не удалось загрузить отчёты"
                        )
                    }
                }
        }
        loadTodayMetrics(force = isManualRefresh)
        if (_uiState.value.selectedStatsView == AdminStatsView.Chart) loadChartSeries(force = true)
    }

    private fun loadTodayMetrics(force: Boolean = false) {
        if (!force && _uiState.value.todayReportMetrics != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTodayReportLoading = true) }
            runCatching { repository.getTodayReportMetrics() }
                .onSuccess { metrics ->
                    _uiState.update { it.copy(todayReportMetrics = metrics, isTodayReportLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isTodayReportLoading = false,
                            errorMessage = error.message ?: "Не удалось загрузить метрики за сегодня"
                        )
                    }
                }
        }
    }

    private fun loadChartSeries(force: Boolean = false) {
        val state = _uiState.value
        val metrics = state.chartMetrics
        val (from, to) = chartRange(state.chartPeriod, state.reports.lastOrNull()?.date)
        val alreadyLoaded = state.chartSeries.map { it.metric } == metrics
        if (!force && alreadyLoaded && !state.isChartLoading) return

        chartLoadJob?.cancel()
        chartLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isChartLoading = true) }
            runCatching {
                metrics.mapNotNull { metric -> repository.getReportSeries(metric, from, to) }
            }.onSuccess { series ->
                _uiState.update { it.copy(chartSeries = series, isChartLoading = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isChartLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить динамику метрик"
                    )
                }
            }
        }
    }

    /** Диапазон графика: «Всё время» — от самого старого отчёта до сегодня. */
    private fun chartRange(period: AdminReportPeriod, oldestReportDate: String?): Pair<String, String> {
        val today = LocalDate.now()
        val from = when (val days = period.days) {
            null -> oldestReportDate ?: today.minusDays(365).toString()
            else -> today.minusDays((days - 1).toLong()).toString()
        }
        return from to today.toString()
    }

    /**
     * Пока за дату считается отчёт, тихо перезапрашиваем список, чтобы поймать
     * переход «в очереди»/«считается» → «готов» и сказать об этом.
     */
    private fun startStatsPollingIfNeeded() {
        val state = _uiState.value
        if (state.selectedTab != AdminTab.Stats || state.reports.none { it.isActive }) {
            statsPollJob?.cancel()
            return
        }
        if (statsPollJob?.isActive == true) return
        statsPollJob = viewModelScope.launch {
            while (true) {
                delay(STATS_POLL_INTERVAL_MS)
                val current = _uiState.value
                if (current.selectedTab != AdminTab.Stats || current.reports.none { it.isActive }) break
                val previousStatuses = current.reports.associate { it.date to it.status }
                val next = runCatching { repository.getDailyReports() }.getOrNull() ?: continue
                _uiState.update { it.copy(reports = next) }
                announceFinishedReports(previousStatuses, next)
            }
        }
    }

    private fun announceFinishedReports(
        previousStatuses: Map<String, AdminReportJobStatus?>,
        reports: List<AdminReportListItem>
    ) {
        reports.forEach { report ->
            val previous = previousStatuses[report.date]
            val wasActive = previous == AdminReportJobStatus.Queued ||
                previous == AdminReportJobStatus.Running
            if (!wasActive) return@forEach
            when (report.status) {
                AdminReportJobStatus.Done -> _uiState.update {
                    it.copy(messageText = "Отчёт за ${formatReportDate(report.date)} готов")
                }
                AdminReportJobStatus.Failed -> _uiState.update {
                    it.copy(errorMessage = "Не удалось собрать отчёт за ${formatReportDate(report.date)}")
                }
                else -> Unit
            }
        }
    }

    /**
     * Клик по отчёту. Если файла нет, задача в БД осталась, а сам файл потерян —
     * открывать нечего, поэтому просто сообщаем об этом.
     */
    private fun openReport(date: String) {
        val report = _uiState.value.reports.firstOrNull { it.date == date }
        if (report != null && !report.hasFile) {
            _uiState.update {
                it.copy(errorMessage = "Файл отчёта за ${formatReportDate(date)} удалён")
            }
            return
        }
        _uiState.update {
            it.copy(
                activeDialog = AdminDialogState.Report(date),
                selectedReport = null,
                isReportDetailsLoading = true
            )
        }
        viewModelScope.launch {
            runCatching { repository.getDailyReport(date) }
                .onSuccess { report ->
                    _uiState.update { it.copy(selectedReport = report, isReportDetailsLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isReportDetailsLoading = false,
                            activeDialog = null,
                            errorMessage = error.message ?: "Не удалось открыть отчёт"
                        )
                    }
                }
        }
    }

    private fun openGeneratePanel() {
        _uiState.update {
            it.copy(
                isGeneratePanelOpen = true,
                generateReportDate = yesterdayIsoDate(),
                generateNeedsForce = false
            )
        }
    }

    private fun closeGeneratePanel() {
        _uiState.update { it.copy(isGeneratePanelOpen = false, generateNeedsForce = false) }
    }

    private fun updateGenerateDate(date: String) {
        _uiState.update { it.copy(generateReportDate = date, generateNeedsForce = false) }
    }

    private fun submitGenerateReport(force: Boolean) {
        val state = _uiState.value
        if (state.isGeneratingReport) return
        val date = state.generateReportDate
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingReport = true, errorMessage = null) }
            runCatching { repository.generateDailyReport(date, force) }
                .onSuccess { result ->
                    when (result) {
                        AdminGenerateReportResult.Queued -> {
                            _uiState.update {
                                it.copy(
                                    isGeneratingReport = false,
                                    isGeneratePanelOpen = false,
                                    generateNeedsForce = false,
                                    messageText = "Отчёт поставлен в очередь"
                                )
                            }
                            reloadReportsSilently()
                        }
                        // Файл за дату уже есть — предлагаем пересобрать с перезаписью.
                        AdminGenerateReportResult.Exists -> _uiState.update {
                            it.copy(isGeneratingReport = false, generateNeedsForce = true)
                        }
                        AdminGenerateReportResult.Error -> _uiState.update {
                            it.copy(
                                isGeneratingReport = false,
                                errorMessage = "Не удалось поставить отчёт в очередь"
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingReport = false,
                            errorMessage = error.message ?: "Не удалось поставить отчёт в очередь"
                        )
                    }
                }
        }
    }

    private fun reloadReportsSilently() {
        viewModelScope.launch {
            val reports = runCatching { repository.getDailyReports() }.getOrNull() ?: return@launch
            _uiState.update { it.copy(reports = reports) }
            startStatsPollingIfNeeded()
        }
    }

    private fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun observeModeratorSocketState() {
        repository.observeModeratorWsConnectionState()
            .onEach { state ->
                _uiState.update { current ->
                    current.copy(moderatorWsConnectionState = state)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeModeratorEvents() {
        repository.observeModeratorWsEvents()
            .onEach { event ->
                if (event is ModeratorWsEvent.ModerationCount) {
                    // Счётчик очереди живёт отдельно от списка: он только обновляет
                    // цифру, элементы в список по-прежнему не вставляются сами.
                    _uiState.update { current ->
                        current.copy(moderationQueueCount = event.count)
                    }
                    return@onEach
                }
                val newItems = listOfNotNull(event.toAdminModerationItem())
                if (newItems.isEmpty()) return@onEach
                _uiState.update { current ->
                    val pendingItems = mergeModerationItems(
                        current.pendingModerationItems,
                        newItems
                    )
                    val freshCount = current.freshEventsCount + 1
                    current.copy(
                        hasFreshContent = true,
                        freshEventsCount = freshCount,
                        pendingModerationItems = pendingItems
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadModerationData(
        initialLoad: Boolean = false,
        isManualRefresh: Boolean = false,
        clearFreshMarker: Boolean = false
    ) {
        moderationLoadJob?.cancel()
        moderationLoadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isModerationLoading = true,
                    isLoading = if (initialLoad) true else it.isLoading,
                    isRefreshing = isManualRefresh,
                    errorMessage = null,
                    messageText = if (isManualRefresh) null else it.messageText
                )
            }
            val canViewHistory = hasPermission(PERMISSION_MODERATION_HISTORY)
            runCatching {
                val content = repository.getModerationContent()
                val reports = repository.getModerationReports()
                // История — отдельная лента событий и отдельное право, поэтому
                // без него её просто не запрашиваем, как и вебка.
                // Права известны не всегда (старая сессия — пустой кэш), поэтому
                // 403 по истории не должен ронять весь раздел: просто прячем вкладку.
                val history = if (canViewHistory) {
                    runCatching { repository.getModerationHistory() }.getOrNull()
                } else {
                    null
                }
                Triple(content, reports, history)
            }.onSuccess { (content, reports, history) ->
                val pendingItems = if (clearFreshMarker) _uiState.value.pendingModerationItems else emptyList()
                val moderationItems = mergeModerationItems(content, pendingItems)
                _uiState.update {
                    it.copy(
                        moderationItems = moderationItems,
                        moderationReports = reports,
                        moderationHistoryEvents = history ?: emptyList(),
                        canViewModerationHistory = history != null,
                        selectedModerationTab = if (history == null &&
                            it.selectedModerationTab == AdminModerationTab.History
                        ) {
                            AdminModerationTab.Moderation
                        } else {
                            it.selectedModerationTab
                        },
                        pendingModerationItems = if (clearFreshMarker) emptyList() else it.pendingModerationItems,
                        isModerationLoading = false,
                        isLoading = false,
                        isRefreshing = false,
                        hasFreshContent = if (clearFreshMarker) false else it.hasFreshContent,
                        freshEventsCount = if (clearFreshMarker) 0 else it.freshEventsCount,
                        moderationRefreshTick = if (clearFreshMarker) it.moderationRefreshTick + 1 else it.moderationRefreshTick,
                        messageText = when {
                            clearFreshMarker -> "Список модерации обновлён"
                            isManualRefresh -> "Модерация обновлена"
                            else -> it.messageText
                        }
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    val mergedItems = if (clearFreshMarker) {
                        mergeModerationItems(it.moderationItems, it.pendingModerationItems)
                    } else {
                        it.moderationItems
                    }
                    it.copy(
                        moderationItems = mergedItems,
                        pendingModerationItems = if (clearFreshMarker) emptyList() else it.pendingModerationItems,
                        isModerationLoading = false,
                        isLoading = false,
                        isRefreshing = false,
                        hasFreshContent = if (clearFreshMarker) false else it.hasFreshContent,
                        freshEventsCount = if (clearFreshMarker) 0 else it.freshEventsCount,
                        moderationRefreshTick = if (clearFreshMarker) it.moderationRefreshTick + 1 else it.moderationRefreshTick,
                        errorMessage = if (clearFreshMarker) null else error.message ?: "Не удалось загрузить раздел модерации",
                        messageText = if (clearFreshMarker && it.pendingModerationItems.isNotEmpty()) {
                            "Свежие посты добавлены из WebSocket"
                        } else {
                            it.messageText
                        }
                    )
                }
            }
        }
    }

    private fun applyFreshModerationContent() {
        loadModerationData(clearFreshMarker = true)
    }

    /**
     * Порядок очереди задаёт бэкенд (сначала review по возрастанию AI-скора,
     * потом pending по времени), поэтому список не пересортировывается —
     * свежие элементы из сокета просто встают наверх.
     */
    private fun mergeModerationItems(
        existing: List<AdminModerationItem>,
        incoming: List<AdminModerationItem>
    ): List<AdminModerationItem> =
        (incoming + existing).distinctBy { "${it.targetType}:${it.id}" }

    private fun selectModerationItem(item: AdminModerationItem) {
        _uiState.update {
            it.copy(
                selectedModerationItem = item,
                selectedModerationReport = null,
                selectedModerationEvent = null,
                selectedUser = null,
                selectedRole = null,
                selectedPermission = null,
                activeDialog = AdminDialogState.ModerationItem(
                    targetType = item.targetType,
                    itemId = item.id
                )
            )
        }
    }

    private fun selectModerationReport(report: AdminModerationReport) {
        _uiState.update {
            it.copy(
                selectedModerationItem = null,
                selectedModerationReport = report,
                selectedModerationEvent = null,
                selectedUser = null,
                selectedRole = null,
                selectedPermission = null,
                activeDialog = AdminDialogState.ModerationReport(reportId = report.id)
            )
        }
    }

    /**
     * Вердикт модератора, как в вебке: сначала статус контенту, затем — если
     * решали по жалобе — закрываем саму жалобу, и только потом перезагружаем
     * раздел и закрываем окно.
     */
    private fun applyModerationVerdict(event: AdminEvent.ModerationVerdictClicked) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                repository.updateModerationStatus(
                    targetType = event.targetType,
                    targetId = event.targetId,
                    status = event.status
                )
                event.reportId?.let { reportId -> repository.resolveModerationReport(reportId) }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        selectedModerationItem = null,
                        selectedModerationReport = null,
                        activeDialog = null,
                        messageText = moderationVerdictMessage(event.targetType, event.status)
                    )
                }
                loadModerationData()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Не удалось применить вердикт"
                    )
                }
            }
        }
    }

    private fun moderationVerdictMessage(
        targetType: AdminModerationTargetType,
        status: String
    ): String {
        val target = when (targetType) {
            AdminModerationTargetType.Post -> "Пост"
            AdminModerationTargetType.Comment -> "Комментарий"
        }
        val verdict = when (status) {
            MODERATION_STATUS_ACTIVE -> "одобрен"
            MODERATION_STATUS_HIDDEN -> "скрыт"
            else -> "обновлён"
        }
        return "$target $verdict"
    }

    private fun selectModerationEvent(event: AdminModerationEvent) {
        _uiState.update {
            it.copy(
                selectedModerationItem = null,
                selectedModerationReport = null,
                selectedModerationEvent = event,
                selectedUser = null,
                selectedRole = null,
                selectedPermission = null,
                activeDialog = AdminDialogState.ModerationHistoryEvent(eventId = event.id)
            )
        }
    }

    /** Возврат скрытого контента из карточки истории: `hidden` → `active`. */
    private fun restoreModerationContent(
        targetType: AdminModerationEventTargetType,
        targetId: String
    ) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                repository.restoreModerationContent(targetType.apiValue, targetId)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        selectedModerationEvent = null,
                        activeDialog = null,
                        messageText = "Контент восстановлен"
                    )
                }
                loadModerationData()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Не удалось восстановить контент"
                    )
                }
            }
        }
    }

    private fun markModerationTabOpened() {
        val tab = _uiState.value.selectedModerationTab
        val seenAt = repository.moderationTabSeenAt(tab)
        repository.markModerationTabSeen(tab)
        _uiState.update { it.copy(moderationSeenAtMillis = seenAt) }
    }

    private fun hasPermission(slug: String): Boolean = permissions?.contains(slug) ?: true

    /** Отчёт по умолчанию — за вчера: сегодняшний день ещё не закончился. */
    private fun yesterdayIsoDate(): String = LocalDate.now().minusDays(1).toString()

    /** «2026-06-15» → «15.06.2026», как в подписях вебки. */
    private fun formatReportDate(value: String): String {
        val parts = value.split("-")
        if (parts.size != 3) return value
        return "${parts[2]}.${parts[1]}.${parts[0]}"
    }

    private fun selectUser(user: AdminUser) {
        _uiState.update {
            it.copy(
                selectedUser = user,
                selectedRole = null,
                selectedPermission = null,
                selectedHistoryRecord = null,
                selectedRequestLog = null,
                activeDialog = AdminDialogState.UserSettings(user.id)
            )
        }
    }

    private fun selectRole(role: AdminRole) {
        _uiState.update {
            it.copy(
                selectedUser = null,
                selectedRole = role,
                selectedPermission = null,
                selectedRolePermissions = emptyList(),
                selectedHistoryRecord = null,
                selectedRequestLog = null,
                activeDialog = AdminDialogState.RoleSettings(role.id)
            )
        }
        loadSelectedRolePermissions(role.id.toLong())
    }

    private fun selectPermission(permission: AdminPermission) {
        _uiState.update {
            it.copy(
                selectedUser = null,
                selectedRole = null,
                selectedPermission = permission,
                selectedRolePermissions = emptyList(),
                selectedHistoryRecord = null,
                selectedRequestLog = null,
                activeDialog = AdminDialogState.PermissionSettings(permission.id)
            )
        }
    }

    private fun closeDialog() {
        _uiState.update {
            it.copy(
                selectedModerationItem = null,
                selectedModerationReport = null,
                selectedModerationEvent = null,
                selectedUser = null,
                selectedRole = null,
                selectedPermission = null,
                selectedRolePermissions = emptyList(),
                selectedHistoryRecord = null,
                selectedRequestLog = null,
                selectedReport = null,
                activeDialog = null
            )
        }
    }

    private fun openAddRoleDialog() {
        _uiState.update {
            it.copy(
                selectedUser = null,
                selectedRole = null,
                selectedPermission = null,
                selectedRolePermissions = emptyList(),
                selectedHistoryRecord = null,
                selectedRequestLog = null,
                activeDialog = AdminDialogState.AddRole
            )
        }
    }

    private fun openAddPermissionDialog() {
        _uiState.update {
            it.copy(
                selectedUser = null,
                selectedRole = null,
                selectedPermission = null,
                selectedRolePermissions = emptyList(),
                selectedHistoryRecord = null,
                selectedRequestLog = null,
                activeDialog = AdminDialogState.AddPermission
            )
        }
    }

    private fun saveRole(event: AdminEvent.SaveRoleClicked) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, messageText = null) }
            runCatching {
                if (event.roleId == null) {
                    val createdRole = repository.createRole(event.input)
                    event.permissionIds.forEach { permissionId ->
                        repository.assignPermissionToRole(createdRole.id.toLong(), permissionId)
                    }
                    "Роль создана"
                } else {
                    repository.updateRole(event.roleId, event.input)
                    "Роль обновлена"
                }
            }.onSuccess { message ->
                val users = repository.getUsers()
                val roles = repository.getRoles()
                _uiState.update {
                    it.copy(
                        users = users,
                        roles = roles,
                        selectedRole = null,
                        selectedRolePermissions = emptyList(),
                        activeDialog = null,
                        isSaving = false,
                        messageText = message
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Не удалось сохранить роль"
                    )
                }
            }
        }
    }

    private fun savePermission(event: AdminEvent.SavePermissionClicked) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, messageText = null) }
            runCatching {
                if (event.permissionId == null) {
                    repository.createPermission(event.input)
                    "Разрешение создано"
                } else {
                    repository.updatePermission(event.permissionId, event.input)
                    "Разрешение обновлено"
                }
            }.onSuccess { message ->
                val permissions = repository.getPermissions()
                val rolePermissions = selectedRolePermissions()
                _uiState.update {
                    it.copy(
                        permissions = permissions,
                        selectedRolePermissions = rolePermissions,
                        selectedPermission = null,
                        activeDialog = null,
                        isSaving = false,
                        messageText = message
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Не удалось сохранить разрешение"
                    )
                }
            }
        }
    }

    private fun softDeleteRole(roleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.softDeleteRole(roleId)
            }.onSuccess { role ->
                val users = repository.getUsers()
                val roles = repository.getRoles()
                val rolePermissions = selectedRolePermissions(role.id.toLong())
                _uiState.update {
                    it.copy(
                        users = users,
                        roles = roles,
                        selectedRole = role,
                        selectedRolePermissions = rolePermissions,
                        isDeleting = false,
                        messageText = "Роль мягко удалена"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось удалить роль"
                    )
                }
            }
        }
    }

    private fun hardDeleteRole(roleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.hardDeleteRole(roleId)
            }.onSuccess {
                val users = repository.getUsers()
                val roles = repository.getRoles()
                _uiState.update {
                    it.copy(
                        users = users,
                        roles = roles,
                        selectedRole = null,
                        selectedRolePermissions = emptyList(),
                        activeDialog = null,
                        isDeleting = false,
                        messageText = "Роль удалена"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось удалить роль"
                    )
                }
            }
        }
    }

    private fun restoreRole(roleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.restoreRole(roleId)
            }.onSuccess { role ->
                val users = repository.getUsers()
                val roles = repository.getRoles()
                val rolePermissions = selectedRolePermissions(role.id.toLong())
                _uiState.update {
                    it.copy(
                        users = users,
                        roles = roles,
                        selectedRole = role,
                        selectedRolePermissions = rolePermissions,
                        isDeleting = false,
                        messageText = "Роль восстановлена"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось восстановить роль"
                    )
                }
            }
        }
    }

    private fun softDeletePermission(permissionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.softDeletePermission(permissionId)
            }.onSuccess { permission ->
                val permissions = repository.getPermissions()
                val rolePermissions = selectedRolePermissions()
                _uiState.update {
                    it.copy(
                        permissions = permissions,
                        selectedRolePermissions = rolePermissions,
                        selectedPermission = permission,
                        isDeleting = false,
                        messageText = "Разрешение мягко удалено"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось удалить разрешение"
                    )
                }
            }
        }
    }

    private fun hardDeletePermission(permissionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.hardDeletePermission(permissionId)
            }.onSuccess {
                val permissions = repository.getPermissions()
                val rolePermissions = selectedRolePermissions()
                _uiState.update {
                    it.copy(
                        permissions = permissions,
                        selectedRolePermissions = rolePermissions,
                        selectedPermission = null,
                        activeDialog = null,
                        isDeleting = false,
                        messageText = "Разрешение удалено"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось удалить разрешение"
                    )
                }
            }
        }
    }

    private fun restorePermission(permissionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.restorePermission(permissionId)
            }.onSuccess { permission ->
                val permissions = repository.getPermissions()
                val rolePermissions = selectedRolePermissions()
                _uiState.update {
                    it.copy(
                        permissions = permissions,
                        selectedRolePermissions = rolePermissions,
                        selectedPermission = permission,
                        isDeleting = false,
                        messageText = "Разрешение восстановлено"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось восстановить разрешение"
                    )
                }
            }
        }
    }

    private fun loadSelectedRolePermissions(roleId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.getRolePermissions(roleId)
            }.onSuccess { permissions ->
                _uiState.update { it.copy(selectedRolePermissions = permissions) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Не удалось загрузить разрешения роли")
                }
            }
        }
    }

    private suspend fun selectedRolePermissions(
        roleId: Long? = _uiState.value.selectedRole?.id?.toLong()
    ): List<AdminPermission> = roleId?.let { repository.getRolePermissions(it) } ?: emptyList()

    private fun assignRoleToUser(userId: Long, roleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.assignRoleToUser(userId, roleId)
            }.onSuccess { user ->
                val users = repository.getUsers()
                _uiState.update {
                    it.copy(
                        users = users,
                        selectedUser = user,
                        isSaving = false,
                        messageText = "Роль назначена пользователю"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Не удалось назначить роль"
                    )
                }
            }
        }
    }

    private fun softRemoveRoleFromUser(userId: Long, roleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.softRemoveRoleFromUser(userId, roleId)
            }.onSuccess { user ->
                val users = repository.getUsers()
                _uiState.update {
                    it.copy(
                        users = users,
                        selectedUser = user,
                        isDeleting = false,
                        messageText = "Роль мягко снята с пользователя"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось снять роль"
                    )
                }
            }
        }
    }

    private fun hardRemoveRoleFromUser(userId: Long, roleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.hardRemoveRoleFromUser(userId, roleId)
            }.onSuccess { user ->
                val users = repository.getUsers()
                _uiState.update {
                    it.copy(
                        users = users,
                        selectedUser = user,
                        isDeleting = false,
                        messageText = "Роль снята с пользователя"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось снять роль"
                    )
                }
            }
        }
    }

    private fun assignPermissionToRole(roleId: Long, permissionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.assignPermissionToRole(roleId, permissionId)
            }.onSuccess { permissions ->
                _uiState.update {
                    it.copy(
                        selectedRolePermissions = permissions,
                        isSaving = false,
                        messageText = "Разрешение назначено роли"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Не удалось назначить разрешение"
                    )
                }
            }
        }
    }

    private fun softRemovePermissionFromRole(roleId: Long, permissionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.softRemovePermissionFromRole(roleId, permissionId)
            }.onSuccess { permissions ->
                _uiState.update {
                    it.copy(
                        selectedRolePermissions = permissions,
                        isDeleting = false,
                        messageText = "Разрешение мягко снято с роли"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось снять разрешение"
                    )
                }
            }
        }
    }

    private fun hardRemovePermissionFromRole(roleId: Long, permissionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.hardRemovePermissionFromRole(roleId, permissionId)
            }.onSuccess { permissions ->
                _uiState.update {
                    it.copy(
                        selectedRolePermissions = permissions,
                        isDeleting = false,
                        messageText = "Разрешение снято с роли"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Не удалось снять разрешение"
                    )
                }
            }
        }
    }

    private fun loadHistoryChangedLists(isManualRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isHistoryLoading = true,
                    isRefreshing = isManualRefresh,
                    errorMessage = null,
                    messageText = if (isManualRefresh) null else it.messageText
                )
            }
            runCatching {
                Triple(
                    repository.getChangedUsers(),
                    repository.getChangedRoles(),
                    repository.getChangedPermissions()
                )
            }.onSuccess { (changedUsers, changedRoles, changedPermissions) ->
                _uiState.update {
                    it.copy(
                        changedUsers = changedUsers,
                        changedRoles = changedRoles,
                        changedPermissions = changedPermissions,
                        isHistoryLoading = false,
                        isRefreshing = false,
                        messageText = if (isManualRefresh) "История обновлена" else it.messageText
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isHistoryLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Не удалось загрузить историю изменений"
                    )
                }
            }
        }
    }

    private fun selectHistoryEntity(entityType: AdminHistoryEntityType, entityId: Long) {
        _uiState.update {
            it.copy(
                selectedUser = null,
                selectedRole = null,
                selectedPermission = null,
                selectedRolePermissions = emptyList(),
                selectedHistoryEntityType = entityType,
                selectedHistoryEntityId = entityId,
                selectedHistoryRecord = null,
                selectedRequestLog = null,
                activeDialog = AdminDialogState.HistoryRecords(entityType, entityId)
            )
        }
        loadHistoryRecords(entityType, entityId)
    }

    private fun loadHistoryRecords(entityType: AdminHistoryEntityType, entityId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isHistoryRecordsLoading = true, errorMessage = null) }
            runCatching {
                loadHistoryRecordsFor(entityType, entityId.toLong())
            }.onSuccess { records ->
                _uiState.update {
                    it.copy(
                        selectedHistoryRecords = records,
                        selectedHistoryRecord = it.selectedHistoryRecord?.let { selected ->
                            records.firstOrNull { record -> record.id == selected.id }
                        },
                        isHistoryRecordsLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isHistoryRecordsLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить записи истории"
                    )
                }
            }
        }
    }

    private fun selectHistoryRecord(recordId: Int) {
        val record = _uiState.value.selectedHistoryRecords.firstOrNull { it.id == recordId }
        if (record == null) {
            _uiState.update { it.copy(errorMessage = "Запись истории не найдена") }
            return
        }
        _uiState.update {
            it.copy(
                selectedHistoryRecord = record,
                activeDialog = AdminDialogState.HistoryRecord(record.id)
            )
        }
    }

    private fun backToHistoryRecords() {
        val state = _uiState.value
        val entityType = state.selectedHistoryEntityType
        val entityId = state.selectedHistoryEntityId
        _uiState.update {
            it.copy(
                selectedHistoryRecord = null,
                activeDialog = if (entityType != null && entityId != null) {
                    AdminDialogState.HistoryRecords(entityType, entityId)
                } else {
                    null
                }
            )
        }
    }

    private fun restoreHistoryRecord(logId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoringHistory = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.restoreChangeLog(logId)
            }.onSuccess {
                val users = repository.getUsers()
                val roles = repository.getRoles()
                val permissions = repository.getPermissions()
                val changedUsers = repository.getChangedUsers()
                val changedRoles = repository.getChangedRoles()
                val changedPermissions = repository.getChangedPermissions()
                val state = _uiState.value
                val entityType = state.selectedHistoryEntityType
                val entityId = state.selectedHistoryEntityId
                val records = if (entityType != null && entityId != null) {
                    loadHistoryRecordsFor(entityType, entityId.toLong())
                } else {
                    emptyList()
                }
                val selectedRecord = records.firstOrNull { record -> record.id.toLong() == logId }
                _uiState.update {
                    it.copy(
                        users = users,
                        roles = roles,
                        permissions = permissions,
                        changedUsers = changedUsers,
                        changedRoles = changedRoles,
                        changedPermissions = changedPermissions,
                        selectedHistoryRecords = records,
                        selectedHistoryRecord = selectedRecord,
                        activeDialog = when {
                            selectedRecord != null -> AdminDialogState.HistoryRecord(selectedRecord.id)
                            entityType != null && entityId != null -> {
                                AdminDialogState.HistoryRecords(entityType, entityId)
                            }
                            else -> null
                        },
                        isRestoringHistory = false,
                        messageText = "Запись истории восстановлена"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRestoringHistory = false,
                        errorMessage = error.message ?: "Не удалось восстановить запись истории"
                    )
                }
            }
        }
    }

    private suspend fun loadHistoryRecordsFor(
        entityType: AdminHistoryEntityType,
        entityId: Long
    ) = when (entityType) {
        AdminHistoryEntityType.User -> repository.getUserStory(entityId)
        AdminHistoryEntityType.Role -> repository.getRoleStory(entityId)
        AdminHistoryEntityType.Permission -> repository.getPermissionStory(entityId)
        AdminHistoryEntityType.Unknown -> throw IllegalArgumentException("Неизвестный тип сущности истории")
    }

    private fun refreshRequestLogs() {
        val state = _uiState.value
        loadRequestLogs(
            page = state.requestLogsPage,
            filter = state.requestLogsFilter,
            isManualRefresh = true
        )
    }

    private fun changeRequestLogsPage(page: Int) {
        if (page < 1) return
        val state = _uiState.value
        loadRequestLogs(page = page, filter = state.requestLogsFilter)
    }

    private fun openRequestLogsFilter() {
        _uiState.update {
            it.copy(
                selectedUser = null,
                selectedRole = null,
                selectedPermission = null,
                selectedRolePermissions = emptyList(),
                selectedHistoryRecord = null,
                selectedRequestLog = null,
                activeDialog = AdminDialogState.RequestLogsFilter
            )
        }
    }

    private fun applyRequestLogsFilter(filter: AdminRequestLogsFilter) {
        _uiState.update {
            it.copy(
                requestLogsFilter = filter,
                requestLogsPage = 1,
                activeDialog = null
            )
        }
        loadRequestLogs(page = 1, filter = filter)
    }

    private fun resetRequestLogsFilter() {
        val filter = AdminRequestLogsFilter()
        _uiState.update {
            it.copy(
                requestLogsFilter = filter,
                requestLogsPage = 1,
                activeDialog = null
            )
        }
        loadRequestLogs(page = 1, filter = filter)
    }

    private fun selectRequestLog(logId: Int) {
        _uiState.update {
            it.copy(
                selectedRequestLog = null,
                isRequestLogDetailsLoading = true,
                errorMessage = null,
                activeDialog = AdminDialogState.RequestLogDetails(logId)
            )
        }
        viewModelScope.launch {
            runCatching {
                repository.getRequestLog(logId.toLong())
            }.onSuccess { details ->
                _uiState.update {
                    it.copy(
                        selectedRequestLog = details,
                        isRequestLogDetailsLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRequestLogDetailsLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить детали лога"
                    )
                }
            }
        }
    }

    private fun deleteRequestLog(logId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingRequestLog = true, errorMessage = null, messageText = null) }
            runCatching {
                repository.deleteRequestLog(logId)
            }.onSuccess {
                val state = _uiState.value
                var targetPage = state.requestLogsPage
                var logsPage = repository.getRequestLogs(targetPage, state.requestLogsFilter)
                if (logsPage.items.isEmpty() && targetPage > 1) {
                    targetPage -= 1
                    logsPage = repository.getRequestLogs(targetPage, state.requestLogsFilter)
                }
                _uiState.update {
                    it.copy(
                        requestLogs = logsPage.items,
                        requestLogsPage = logsPage.page,
                        requestLogsTotalPages = logsPage.totalPages,
                        requestLogsTotalCount = logsPage.totalCount,
                        requestLogsPerPage = logsPage.perPage,
                        selectedRequestLog = null,
                        activeDialog = null,
                        isDeletingRequestLog = false,
                        messageText = "Лог запроса удалён"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeletingRequestLog = false,
                        errorMessage = error.message ?: "Не удалось удалить лог запроса"
                    )
                }
            }
        }
    }

    private fun loadRequestLogs(
        page: Int,
        filter: AdminRequestLogsFilter,
        isManualRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRequestLogsLoading = true,
                    isRefreshing = isManualRefresh,
                    errorMessage = null,
                    messageText = if (isManualRefresh) null else it.messageText
                )
            }
            runCatching {
                repository.getRequestLogs(page = page, filter = filter)
            }.onSuccess { logsPage ->
                _uiState.update {
                    it.copy(
                        requestLogs = logsPage.items,
                        requestLogsPage = logsPage.page,
                        requestLogsTotalPages = logsPage.totalPages,
                        requestLogsTotalCount = logsPage.totalCount,
                        requestLogsPerPage = logsPage.perPage,
                        isRequestLogsLoading = false,
                        isRefreshing = false,
                        messageText = if (isManualRefresh) "Логи обновлены" else it.messageText
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRequestLogsLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Не удалось загрузить логи запросов"
                    )
                }
            }
        }
    }
}

/** Шаг тихого перезапроса списка отчётов, пока какая-то задача считается. */
private const val STATS_POLL_INTERVAL_MS = 4_000L
