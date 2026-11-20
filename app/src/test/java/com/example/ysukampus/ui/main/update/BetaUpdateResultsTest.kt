package com.example.ysukampus.ui.main.update

import android.content.pm.PackageInstaller
import com.example.ysukampus.data.network.dto.BetaBuildManifestDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BetaUpdateResultsTest {

    private fun manifest(
        versionCode: Int = 52,
        size: Long = 1024,
        sha256: String = "a".repeat(64),
        url: String = "/api/builds/beta/download?version_code=52"
    ) = BetaBuildManifestDto(
        versionCode = versionCode,
        versionName = "0.1.0-beta.$versionCode",
        notes = null,
        size = size,
        sha256 = sha256,
        url = url
    )

    @Test
    fun `404 means distribution is not configured, not up to date`() {
        assertEquals(
            BetaUpdateCheckResult.NotConfigured,
            classifyLatestBetaBuild(httpCode = 404, manifest = null, installedVersionCode = 10)
        )
    }

    @Test
    fun `newer version code is an available update`() {
        val m = manifest(versionCode = 52)
        assertEquals(
            BetaUpdateCheckResult.Available(m),
            classifyLatestBetaBuild(httpCode = 200, manifest = m, installedVersionCode = 51)
        )
    }

    @Test
    fun `same or older version code is up to date`() {
        assertEquals(
            BetaUpdateCheckResult.UpToDate(52),
            classifyLatestBetaBuild(httpCode = 200, manifest = manifest(52), installedVersionCode = 52)
        )
        assertEquals(
            BetaUpdateCheckResult.UpToDate(52),
            classifyLatestBetaBuild(httpCode = 200, manifest = manifest(52), installedVersionCode = 60)
        )
    }

    @Test
    fun `server errors and empty body are failures`() {
        assertTrue(classifyLatestBetaBuild(500, null, 1) is BetaUpdateCheckResult.Failed)
        assertTrue(classifyLatestBetaBuild(200, null, 1) is BetaUpdateCheckResult.Failed)
    }

    @Test
    fun `malformed manifest is a failure, not an update`() {
        assertTrue(classifyLatestBetaBuild(200, manifest(sha256 = "abc"), 1) is BetaUpdateCheckResult.Failed)
        assertTrue(classifyLatestBetaBuild(200, manifest(size = 0), 1) is BetaUpdateCheckResult.Failed)
        assertTrue(classifyLatestBetaBuild(200, manifest(url = " "), 1) is BetaUpdateCheckResult.Failed)
    }

    @Test
    fun `sha256 hex is lowercase and two digits per byte`() {
        assertEquals("000fff", byteArrayOf(0x00, 0x0f, 0xff.toByte()).toHex())
    }

    @Test
    fun `installer statuses map to distinct failures`() {
        assertEquals(BetaUpdateFailure.InstallCancelled, installStatusToFailure(PackageInstaller.STATUS_FAILURE_ABORTED))
        assertEquals(BetaUpdateFailure.InstallBlocked, installStatusToFailure(PackageInstaller.STATUS_FAILURE_BLOCKED))
        assertEquals(BetaUpdateFailure.InstallConflict, installStatusToFailure(PackageInstaller.STATUS_FAILURE_CONFLICT))
        assertEquals(BetaUpdateFailure.InstallStorage, installStatusToFailure(PackageInstaller.STATUS_FAILURE_STORAGE))
        assertEquals(BetaUpdateFailure.InstallFailed, installStatusToFailure(PackageInstaller.STATUS_FAILURE_INVALID))
    }
}
