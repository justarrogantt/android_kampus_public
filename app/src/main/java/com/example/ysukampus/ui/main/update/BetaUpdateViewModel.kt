package com.example.ysukampus.ui.main.update

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BetaUpdateViewModel(
    private val updateManager: BetaUpdateManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(BetaUpdateState())
    val uiState: StateFlow<BetaUpdateState> = _uiState.asStateFlow()

    private var hasCheckedThisSession = false

    fun checkForUpdate() {
        if (hasCheckedThisSession) return
        hasCheckedThisSession = true

        _uiState.update {
            it.copy(
                isChecking = updateManager.isSupportedBuild(),
                errorMessage = null
            )
        }

        updateManager.checkForUpdate(
            onUpdateAvailable = { displayVersion, buildVersion, releaseNotes ->
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        isUpdateAvailable = true,
                        isBottomSheetVisible = true,
                        displayVersion = displayVersion,
                        buildVersion = buildVersion,
                        releaseNotes = releaseNotes,
                        errorMessage = null
                    )
                }
            },
            onNoUpdate = {
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        isUpdateAvailable = false,
                        isBottomSheetVisible = false,
                        isStartingUpdate = false,
                        displayVersion = null,
                        buildVersion = null,
                        releaseNotes = null,
                        errorMessage = null
                    )
                }
            },
            onError = { throwable ->
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        isUpdateAvailable = false,
                        isBottomSheetVisible = false,
                        isStartingUpdate = false,
                        displayVersion = null,
                        buildVersion = null,
                        releaseNotes = null,
                        errorMessage = throwable.message
                    )
                }
            }
        )
    }

    fun dismissBottomSheet() {
        _uiState.update { it.copy(isBottomSheetVisible = false) }
    }

    fun startUpdate() {
        if (!_uiState.value.isUpdateAvailable || _uiState.value.isStartingUpdate) {
            return
        }

        _uiState.update {
            it.copy(
                isStartingUpdate = true,
                errorMessage = null
            )
        }

        updateManager.startUpdate(
            onStarted = {
                _uiState.update {
                    it.copy(
                        isStartingUpdate = false,
                        isBottomSheetVisible = false
                    )
                }
            },
            onError = { throwable ->
                _uiState.update {
                    it.copy(
                        isStartingUpdate = false,
                        errorMessage = throwable.message
                    )
                }
            }
        )
    }
}
