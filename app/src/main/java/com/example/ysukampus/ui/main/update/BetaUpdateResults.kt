package com.example.ysukampus.ui.main.update

import com.example.ysukampus.data.network.dto.BetaBuildManifestDto

/** Итог проверки обновления. Каждое состояние — отдельное, ни одно не сворачивается в другое. */
sealed interface BetaUpdateCheckResult {
    /** Сборка не бета: проверка выключена флагом `BETA_UPDATES_ENABLED`. */
    data object Disabled : BetaUpdateCheckResult

    /**
     * `latest` ответил 404: на выбранном стенде раздача не настроена (сейчас
     * она есть только на проде). Это не «обновлений нет» и не ошибка.
     */
    data object NotConfigured : BetaUpdateCheckResult

    /** Манифест есть, но его version_code не новее установленного. */
    data class UpToDate(val latestVersionCode: Int) : BetaUpdateCheckResult

    data class Available(val manifest: BetaBuildManifestDto) : BetaUpdateCheckResult

    data class Failed(val cause: Throwable) : BetaUpdateCheckResult
}

/** Что показать пользователю, когда скачивание или установка не удались. */
enum class BetaUpdateFailure {
    /** Сеть или сервер: сборку не скачать. */
    Download,

    /** Скачанный файл не совпал с манифестом по размеру или sha256. */
    Checksum,

    /** Нет разрешения «Установка из этого источника» — открыли настройки. */
    InstallPermission,

    /** Пользователь закрыл подтверждение установки (или MIUI закрыл его сам). */
    InstallCancelled,

    /** Установку заблокировала система или оболочка (MIUI, политика устройства). */
    InstallBlocked,

    /** Подпись сборки не совпадает с установленной. */
    InstallConflict,

    /** Не хватает места. */
    InstallStorage,

    /** Любой другой отказ установщика; подробность — в detail. */
    InstallFailed
}

class BetaUpdateException(
    val failure: BetaUpdateFailure,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/** Результаты PackageInstaller, пришедшие в [BetaInstallResultReceiver]. */
sealed interface BetaInstallEvent {
    /** Система показала подтверждение установки — дальше решает пользователь. */
    data object AwaitingConfirmation : BetaInstallEvent

    data object Installed : BetaInstallEvent

    data class Failed(val failure: BetaUpdateFailure, val detail: String?) : BetaInstallEvent
}

/**
 * Разбор ответа `builds/beta/latest`. Вынесен из менеджера, чтобы проверяться
 * юнит-тестом без Android и сети.
 */
internal fun classifyLatestBetaBuild(
    httpCode: Int,
    manifest: BetaBuildManifestDto?,
    installedVersionCode: Int
): BetaUpdateCheckResult {
    if (httpCode == 404) return BetaUpdateCheckResult.NotConfigured
    if (httpCode !in 200..299) {
        return BetaUpdateCheckResult.Failed(IllegalStateException("builds/beta/latest ответил HTTP $httpCode"))
    }
    if (manifest == null) {
        return BetaUpdateCheckResult.Failed(IllegalStateException("builds/beta/latest вернул пустое тело"))
    }
    // Битый манифест не должен превращаться ни в «обновлений нет», ни в
    // обновление, которое потом не пройдёт сверку, — только в явный сбой.
    val problem = when {
        manifest.versionCode <= 0 -> "version_code=${manifest.versionCode}"
        manifest.size <= 0 -> "size=${manifest.size}"
        !manifest.sha256.isSha256Hex() -> "sha256 не похож на хеш"
        manifest.url.isBlank() -> "пустой url"
        else -> null
    }
    if (problem != null) {
        return BetaUpdateCheckResult.Failed(IllegalStateException("Некорректный манифест сборки: $problem"))
    }
    return if (manifest.versionCode > installedVersionCode) {
        BetaUpdateCheckResult.Available(manifest)
    } else {
        BetaUpdateCheckResult.UpToDate(manifest.versionCode)
    }
}

private val SHA256_HEX = Regex("^[0-9a-fA-F]{64}$")

internal fun String.isSha256Hex(): Boolean = SHA256_HEX.matches(this)

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
