package com.example.ysukampus.ui.main.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import com.example.ysukampus.BuildConfig
import com.example.ysukampus.data.auth.PublicService
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.data.network.di.createPublicAuthService
import com.example.ysukampus.data.network.dto.BetaBuildManifestDto
import com.example.ysukampus.data.network.resolveApiFileUrl
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.await

/**
 * Обновление бета-сборки со своего бэкенда: манифест `builds/beta/latest`,
 * скачивание APK, сверка sha256 и установка через PackageInstaller.
 *
 * Раньше здесь был Firebase App Distribution, но он перестал отдавать бинарь
 * (403 на скачивании у всех, см. docs/BETA_READINESS.md), а заодно требовал
 * входа тестера гугловым аккаунтом в браузере. Теперь ни аккаунта, ни браузера.
 *
 * Манифест и APK идут с того стенда, что выбран переключателем серверов:
 * клиент — [PublicService] (без авторизации, эндпоинты публичные), адрес
 * скачивания достраивается через [resolveApiFileUrl].
 */
class BetaUpdateManager(
    context: Context,
    private val serviceProvider: () -> PublicService = ::createPublicAuthService,
    private val installer: BetaApkInstaller = BetaApkInstaller(context)
) {
    private val appContext = context.applicationContext

    val installEvents: SharedFlow<BetaInstallEvent> = BetaInstallEvents.events

    /**
     * Сборка раздаётся тестерам, и проверять обновление есть смысл.
     *
     * Флаг, а не сравнение `BuildConfig.BUILD_TYPE` со строкой «beta»: раздачу
     * тестерам перевели с `beta` на минифицированный `betaRelease`, и проверка
     * молча выключилась у всех, кому эта сборка и уходит. Флаг задаётся рядом с
     * остальными в `app/build.gradle.kts` и переименование типа сборки переживёт.
     */
    fun isSupportedBuild(): Boolean = BuildConfig.BETA_UPDATES_ENABLED

    suspend fun checkForUpdate(): BetaUpdateCheckResult {
        if (!isSupportedBuild()) {
            Log.i(TAG, "Проверка обновления выключена: BETA_UPDATES_ENABLED=false у этого типа сборки")
            return BetaUpdateCheckResult.Disabled
        }

        val result = try {
            val response = serviceProvider().getLatestBetaBuild()
            classifyLatestBetaBuild(
                httpCode = response.code(),
                manifest = response.body(),
                installedVersionCode = BuildConfig.VERSION_CODE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BetaUpdateCheckResult.Failed(e)
        }

        val environment = ApiEnvironmentHolder.environment
        when (result) {
            BetaUpdateCheckResult.NotConfigured -> Log.i(
                TAG,
                "На стенде $environment раздача бета-сборок не настроена (latest → 404)"
            )
            is BetaUpdateCheckResult.UpToDate -> Log.i(
                TAG,
                "Обновлений нет: на стенде ${result.latestVersionCode}, установлена ${BuildConfig.VERSION_CODE}"
            )
            is BetaUpdateCheckResult.Available -> Log.i(
                TAG,
                "Доступна сборка ${result.manifest.versionCode} (${result.manifest.versionName}), " +
                    "установлена ${BuildConfig.VERSION_CODE}"
            )
            is BetaUpdateCheckResult.Failed -> Log.e(TAG, "Проверка обновления на стенде $environment не удалась", result.cause)
            BetaUpdateCheckResult.Disabled -> Unit
        }
        return result
    }

    fun canInstallPackages(): Boolean = installer.canInstallPackages()

    /**
     * Открывает системный экран «Установка из этого источника» для приложения.
     * Возвращает false, если оболочка такого экрана не знает.
     */
    fun openInstallPermissionSettings(): Boolean {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${appContext.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            appContext.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Экран разрешения на установку недоступен", e)
            false
        }
    }

    /**
     * Скачивает APK из манифеста и сверяет размер и sha256. Бросает
     * [BetaUpdateException]; файл, не прошедший сверку, удаляется и до
     * установщика не доходит.
     */
    suspend fun download(manifest: BetaBuildManifestDto): File = withContext(Dispatchers.IO) {
        val url = resolveApiFileUrl(manifest.url)
            ?: throw BetaUpdateException(BetaUpdateFailure.Download, "Пустой url в манифесте")

        val dir = File(appContext.cacheDir, DOWNLOAD_DIR).apply { mkdirs() }
        val target = File(dir, "ysukampus-${manifest.versionCode}.apk")

        // Пользователь мог закрыть подтверждение установки и нажать «Обновить»
        // ещё раз — второй раз те же мегабайты не качаем, но сверку повторяем.
        if (target.isFile && target.length() == manifest.size &&
            target.sha256().equals(manifest.sha256, ignoreCase = true)
        ) {
            Log.i(TAG, "Сборка ${manifest.versionCode} уже скачана и сверена")
            return@withContext target
        }
        // Прежние версии и недокачанные куски: кэш держит только одну сборку.
        dir.listFiles()?.forEach { it.delete() }

        val part = File(dir, "${target.name}.part")
        val digest = MessageDigest.getInstance("SHA-256")
        var written = 0L

        try {
            val call = serviceProvider().downloadBetaBuild(url)
            call.timeout().timeout(DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            val body = call.await()
            body.byteStream().use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        written += read
                        if (written > manifest.size) {
                            throw BetaUpdateException(
                                BetaUpdateFailure.Checksum,
                                "Сервер отдал больше ${manifest.size} байт, заявленных в манифесте"
                            )
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
        } catch (e: CancellationException) {
            part.delete()
            throw e
        } catch (e: BetaUpdateException) {
            part.delete()
            throw e
        } catch (e: HttpException) {
            part.delete()
            throw BetaUpdateException(BetaUpdateFailure.Download, "Скачивание ответило HTTP ${e.code()}", e)
        } catch (e: IOException) {
            part.delete()
            throw BetaUpdateException(BetaUpdateFailure.Download, "Скачивание оборвалось: ${e.message}", e)
        }

        val actualSha = digest.digest().toHex()
        if (written != manifest.size || !actualSha.equals(manifest.sha256, ignoreCase = true)) {
            part.delete()
            throw BetaUpdateException(
                BetaUpdateFailure.Checksum,
                "Файл не совпал с манифестом: размер $written из ${manifest.size}, " +
                    "sha256 $actualSha вместо ${manifest.sha256}"
            )
        }
        if (!part.renameTo(target)) {
            part.delete()
            throw BetaUpdateException(BetaUpdateFailure.InstallStorage, "Не удалось переименовать ${part.name}")
        }
        Log.i(TAG, "Сборка ${manifest.versionCode} скачана, sha256 сошёлся")
        target
    }

    /** Отдаёт APK системному установщику. Итог приходит в [installEvents]. */
    suspend fun install(apk: File) = installer.install(apk)

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private companion object {
        const val TAG = "BetaUpdate"
        const val DOWNLOAD_DIR = "beta_updates"
        const val BUFFER_SIZE = 64 * 1024

        /** Потолок на всё скачивание: десятки мегабайт на слабой мобильной сети. */
        const val DOWNLOAD_TIMEOUT_MINUTES = 20L
    }
}
