package com.example.ysukampus.ui.main.update

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.network.dto.BetaBuildManifestDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BetaUpdateViewModel(
    private val updateManager: BetaUpdateManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(BetaUpdateState())
    val uiState: StateFlow<BetaUpdateState> = _uiState.asStateFlow()

    private var hasCheckedThisSession = false
    private var availableManifest: BetaBuildManifestDto? = null
    private var updateJob: Job? = null

    init {
        viewModelScope.launch {
            updateManager.installEvents.collect(::onInstallEvent)
        }
    }

    fun checkForUpdate() {
        if (hasCheckedThisSession) return
        hasCheckedThisSession = true

        _uiState.update { it.copy(checkStatus = BetaUpdateCheckStatus.Checking, failure = null) }

        viewModelScope.launch {
            when (val result = updateManager.checkForUpdate()) {
                is BetaUpdateCheckResult.Available -> {
                    availableManifest = result.manifest
                    _uiState.update {
                        it.copy(
                            checkStatus = BetaUpdateCheckStatus.Available,
                            isUpdateAvailable = true,
                            isBottomSheetVisible = true,
                            displayVersion = result.manifest.versionName,
                            buildVersion = result.manifest.versionCode.toString(),
                            releaseNotes = result.manifest.notes,
                            failure = null
                        )
                    }
                }
                BetaUpdateCheckResult.Disabled -> setNoUpdate(BetaUpdateCheckStatus.Disabled)
                BetaUpdateCheckResult.NotConfigured -> setNoUpdate(BetaUpdateCheckStatus.NotConfigured)
                is BetaUpdateCheckResult.UpToDate -> setNoUpdate(BetaUpdateCheckStatus.UpToDate)
                // Сбой проверки пользователю не показываем — шторка про
                // обновление, а не про сеть. Причина уже в логе менеджера.
                is BetaUpdateCheckResult.Failed -> setNoUpdate(BetaUpdateCheckStatus.Failed)
            }
        }
    }

    fun dismissBottomSheet() {
        _uiState.update { it.copy(isBottomSheetVisible = false, failure = null) }
    }

    fun startUpdate() {
        val manifest = availableManifest ?: return
        if (_uiState.value.isStartingUpdate || updateJob?.isActive == true) return

        // Без разрешения PackageInstaller на MIUI не всегда даже спрашивает —
        // сессия просто отваливается. Проверяем заранее и ведём в настройки.
        if (!updateManager.canInstallPackages()) {
            val opened = updateManager.openInstallPermissionSettings()
            Log.w(TAG, "Нет разрешения на установку из этого источника, настройки открыты: $opened")
            showFailure(BetaUpdateFailure.InstallPermission)
            return
        }

        _uiState.update { it.copy(isStartingUpdate = true, failure = null) }

        updateJob = viewModelScope.launch {
            try {
                val apk = updateManager.download(manifest)
                updateManager.install(apk)
                // Дальше — подтверждение системы; итог придёт в onInstallEvent.
            } catch (e: CancellationException) {
                throw e
            } catch (e: BetaUpdateException) {
                Log.e(TAG, "Обновление не удалось: ${e.message}", e)
                showFailure(e.failure)
            } catch (e: Exception) {
                Log.e(TAG, "Обновление упало", e)
                showFailure(BetaUpdateFailure.InstallFailed)
            }
        }
    }

    private fun onInstallEvent(event: BetaInstallEvent) {
        when (event) {
            // Подтверждение уже на экране. Кнопку отпускаем: если пользователь
            // его закроет, а оболочка не пришлёт ABORTED (так бывает), он
            // сможет нажать «Обновить» ещё раз.
            BetaInstallEvent.AwaitingConfirmation -> _uiState.update {
                it.copy(isStartingUpdate = false, isBottomSheetVisible = false)
            }
            // Обычно процесс к этому моменту уже убит установкой.
            BetaInstallEvent.Installed -> _uiState.update { it.copy(isStartingUpdate = false) }
            is BetaInstallEvent.Failed -> {
                Log.e(TAG, "Установщик отказал: ${event.failure} ${event.detail.orEmpty()}")
                showFailure(event.failure)
            }
        }
    }

    private fun showFailure(failure: BetaUpdateFailure) {
        _uiState.update {
            it.copy(
                isStartingUpdate = false,
                // Нажать «Обновить» можно и из профиля, когда шторка закрыта, —
                // открываем её, иначе отказ остался бы тихим.
                isBottomSheetVisible = it.isUpdateAvailable,
                failure = failure
            )
        }
    }

    private fun setNoUpdate(status: BetaUpdateCheckStatus) {
        availableManifest = null
        _uiState.update {
            it.copy(
                checkStatus = status,
                isUpdateAvailable = false,
                isBottomSheetVisible = false,
                isStartingUpdate = false,
                displayVersion = null,
                buildVersion = null,
                releaseNotes = null,
                failure = null
            )
        }
    }

    private companion object {
        const val TAG = "BetaUpdate"
    }
}
