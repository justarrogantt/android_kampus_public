package com.example.ysukampus.ui.main.update

data class BetaUpdateState(
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val isBottomSheetVisible: Boolean = false,
    val isStartingUpdate: Boolean = false,
    val displayVersion: String? = null,
    val buildVersion: String? = null,
    val releaseNotes: String? = null,
    val errorMessage: String? = null
)
