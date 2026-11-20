package com.example.ysukampus.ui.main.update

/**
 * Чем кончилась проверка обновления. В UI сейчас рисуется только [Available],
 * но состояния держатся раздельно: 404 от `latest` ([NotConfigured]) — это
 * «на стенде раздача не настроена», а не «обновлений нет» и не ошибка.
 */
enum class BetaUpdateCheckStatus {
    Idle,
    Checking,
    Disabled,
    NotConfigured,
    UpToDate,
    Available,
    Failed
}

data class BetaUpdateState(
    val checkStatus: BetaUpdateCheckStatus = BetaUpdateCheckStatus.Idle,
    val isUpdateAvailable: Boolean = false,
    val isBottomSheetVisible: Boolean = false,
    val isStartingUpdate: Boolean = false,
    val displayVersion: String? = null,
    val buildVersion: String? = null,
    val releaseNotes: String? = null,
    /** Почему не удалось скачать или поставить сборку; показывается в шторке. */
    val failure: BetaUpdateFailure? = null
)
