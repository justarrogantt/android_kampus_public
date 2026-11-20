package com.example.ysukampus.ui.main.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.core.content.IntentCompat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * Установка скачанного APK через [PackageInstaller] — без FileProvider и без
 * `ACTION_VIEW`: файл пишется в сессию установщика, итог приходит широковещалкой
 * в [BetaInstallResultReceiver].
 */
class BetaApkInstaller(context: Context) {
    private val appContext = context.applicationContext

    /** Разрешение «Установка из этого источника» (REQUEST_INSTALL_PACKAGES). */
    fun canInstallPackages(): Boolean = appContext.packageManager.canRequestPackageInstalls()

    suspend fun install(apk: File) = withContext(Dispatchers.IO) {
        val packageInstaller = appContext.packageManager.packageInstaller

        // Сессия от прошлой попытки (пользователь закрыл подтверждение) иначе
        // висит до перезапуска и держит копию APK.
        packageInstaller.mySessions.forEach { info ->
            runCatching { packageInstaller.abandonSession(info.sessionId) }
        }

        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            // Сессия примет только APK этого же приложения: чужой пакет
            // отвергнется установщиком, а не встанет рядом.
            setAppPackageName(appContext.packageName)
            setSize(apk.length())
        }

        val sessionId = packageInstaller.createSession(params)
        try {
            packageInstaller.openSession(sessionId).use { session ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    apk.inputStream().use { input -> input.copyTo(output) }
                    session.fsync(output)
                }

                val intent = Intent(appContext, BetaInstallResultReceiver::class.java)
                    .setPackage(appContext.packageName)
                // MUTABLE обязателен: установщик дописывает в интент статус и
                // интент подтверждения. Компонент явный, так что подменить
                // получателя нельзя.
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val pendingIntent = PendingIntent.getBroadcast(appContext, sessionId, intent, flags)
                session.commit(pendingIntent.intentSender)
            }
            Log.i(TAG, "Сессия установки $sessionId отправлена")
        } catch (e: Exception) {
            runCatching { packageInstaller.abandonSession(sessionId) }
            throw BetaUpdateException(
                BetaUpdateFailure.InstallFailed,
                "Не удалось передать APK установщику: ${e.message}",
                e
            )
        }
    }

    private companion object {
        const val TAG = "BetaUpdate"
    }
}

/**
 * Процессный канал результатов установки: широковещалку принимает
 * [BetaInstallResultReceiver], а слушает [BetaUpdateViewModel].
 */
object BetaInstallEvents {
    private val _events = MutableSharedFlow<BetaInstallEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BetaInstallEvent> = _events.asSharedFlow()

    fun emit(event: BetaInstallEvent) {
        if (!_events.tryEmit(event)) {
            Log.w("BetaUpdate", "Результат установки потерян, некому принять: $event")
        }
    }
}

/**
 * Принимает статус сессии PackageInstaller. Не экспортирован: сюда приходит
 * только наш собственный PendingIntent.
 *
 * Объявлен (вместе с REQUEST_INSTALL_PACKAGES) только в манифесте бета-типов
 * сборки — `app/src/beta/AndroidManifest.xml`, у betaRelease симлинком. В
 * release класс есть, но не зарегистрирован: установка там не запускается,
 * `BETA_UPDATES_ENABLED` = false.
 */
class BetaInstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.i(TAG, "Статус установки: $status ${message.orEmpty()}")

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
            if (confirm == null) {
                BetaInstallEvents.emit(
                    BetaInstallEvent.Failed(BetaUpdateFailure.InstallFailed, "Установщик не прислал подтверждение")
                )
                return
            }
            try {
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                BetaInstallEvents.emit(BetaInstallEvent.AwaitingConfirmation)
            } catch (e: Exception) {
                // MIUI без разрешения «Всплывающие окна в фоне» может не дать
                // открыть подтверждение — это надо показать, а не проглотить.
                Log.e(TAG, "Не удалось открыть подтверждение установки", e)
                BetaInstallEvents.emit(BetaInstallEvent.Failed(BetaUpdateFailure.InstallBlocked, e.message))
            }
            return
        }

        val event = if (status == PackageInstaller.STATUS_SUCCESS) {
            BetaInstallEvent.Installed
        } else {
            BetaInstallEvent.Failed(installStatusToFailure(status), message)
        }
        BetaInstallEvents.emit(event)
    }

    private companion object {
        const val TAG = "BetaUpdate"
    }
}

internal fun installStatusToFailure(status: Int): BetaUpdateFailure = when (status) {
    // Сюда же MIUI сворачивает отказ в разрешении на установку из источника.
    PackageInstaller.STATUS_FAILURE_ABORTED -> BetaUpdateFailure.InstallCancelled
    PackageInstaller.STATUS_FAILURE_BLOCKED -> BetaUpdateFailure.InstallBlocked
    PackageInstaller.STATUS_FAILURE_CONFLICT -> BetaUpdateFailure.InstallConflict
    PackageInstaller.STATUS_FAILURE_STORAGE -> BetaUpdateFailure.InstallStorage
    else -> BetaUpdateFailure.InstallFailed
}
