package com.cleanpic.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class UpdateCheckerTest {

    // ── U-UPD-01 版本号比较 ──

    @Test fun newer_version_detected() {
        assertTrue(UpdateChecker.isNewerVersion("1.1.0", "1.2.0"))
    }

    @Test fun same_version_not_newer() {
        assertFalse(UpdateChecker.isNewerVersion("1.2.0", "1.2.0"))
    }

    @Test fun local_ahead_not_newer() {
        assertFalse(UpdateChecker.isNewerVersion("1.3.0", "1.2.0"))
    }

    @Test fun patch_version_newer() {
        assertTrue(UpdateChecker.isNewerVersion("1.1.0", "1.1.1"))
    }

    @Test fun major_version_newer() {
        assertTrue(UpdateChecker.isNewerVersion("1.9.9", "2.0.0"))
    }

    // ── U-UPD-02 强制更新判定 ──

    @Test fun force_update_flag_true() {
        val result = UpdateChecker.determineStatus(
            currentVersion = "1.1.0",
            remoteVersion = "1.2.0",
            forceUpdate = true,
            minVersion = "1.0.0"
        )
        assertEquals(UpdateStatus.FORCE_UPDATE, result)
    }

    @Test fun below_min_version_forces_update() {
        val result = UpdateChecker.determineStatus(
            currentVersion = "0.9.0",
            remoteVersion = "1.2.0",
            forceUpdate = false,
            minVersion = "1.0.0"
        )
        assertEquals(UpdateStatus.FORCE_UPDATE, result)
    }

    @Test fun optional_update_when_newer() {
        val result = UpdateChecker.determineStatus(
            currentVersion = "1.1.0",
            remoteVersion = "1.2.0",
            forceUpdate = false,
            minVersion = "1.0.0"
        )
        assertEquals(UpdateStatus.OPTIONAL_UPDATE, result)
    }

    @Test fun up_to_date_when_same() {
        val result = UpdateChecker.determineStatus(
            currentVersion = "1.2.0",
            remoteVersion = "1.2.0",
            forceUpdate = false,
            minVersion = "1.0.0"
        )
        assertEquals(UpdateStatus.UP_TO_DATE, result)
    }

    // ── U-UPD-03 平台分发 ──

    @Test fun android_platform_extracts_correctly() {
        val response = VersionResponse(
            android = UpdateInfo(version = "1.2.0", downloadUrl = "https://example.com/app.apk"),
            ios = UpdateInfo(version = "1.3.0", downloadUrl = "https://testflight.apple.com/xxx")
        )
        val info = UpdateChecker.extractPlatformInfo(response, "Android")
        assertEquals("1.2.0", info?.version)
    }

    @Test fun ios_platform_extracts_correctly() {
        val response = VersionResponse(
            android = UpdateInfo(version = "1.2.0"),
            ios = UpdateInfo(version = "1.3.0")
        )
        val info = UpdateChecker.extractPlatformInfo(response, "iOS")
        assertEquals("1.3.0", info?.version)
    }

    @Test fun harmonyos_platform_extracts_correctly() {
        val response = VersionResponse(
            harmonyos = UpdateInfo(version = "1.4.0")
        )
        val info = UpdateChecker.extractPlatformInfo(response, "HarmonyOS")
        assertEquals("1.4.0", info?.version)
    }

    @Test fun unknown_platform_returns_null() {
        val response = VersionResponse(
            android = UpdateInfo(version = "1.2.0")
        )
        val info = UpdateChecker.extractPlatformInfo(response, "Unknown")
        assertEquals(null, info)
    }

    @Test fun missing_platform_returns_null() {
        val response = VersionResponse(android = null, ios = null, harmonyos = null)
        val info = UpdateChecker.extractPlatformInfo(response, "Android")
        assertEquals(null, info)
    }

    // ── U-UPD-04 完整检查流程 ──

    @Test fun check_returns_optional_update() {
        val response = VersionResponse(
            android = UpdateInfo(
                version = "1.2.0",
                forceUpdate = false,
                minVersion = "1.0.0",
                changelog = "Bug fixes",
                downloadUrl = "https://example.com/app.apk"
            )
        )
        val result = UpdateChecker.evaluateResponse(response, "1.1.0", "Android")
        assertEquals(UpdateStatus.OPTIONAL_UPDATE, result.status)
        assertEquals("1.2.0", result.updateInfo?.version)
        assertEquals("Bug fixes", result.updateInfo?.changelog)
    }

    @Test fun check_returns_up_to_date() {
        val response = VersionResponse(
            android = UpdateInfo(version = "1.1.0", minVersion = "1.0.0")
        )
        val result = UpdateChecker.evaluateResponse(response, "1.1.0", "Android")
        assertEquals(UpdateStatus.UP_TO_DATE, result.status)
        assertEquals(null, result.updateInfo)
    }

    @Test fun check_returns_force_for_low_version() {
        val response = VersionResponse(
            android = UpdateInfo(version = "2.0.0", minVersion = "1.5.0", forceUpdate = false)
        )
        val result = UpdateChecker.evaluateResponse(response, "1.0.0", "Android")
        assertEquals(UpdateStatus.FORCE_UPDATE, result.status)
    }

    @Test fun check_returns_up_to_date_for_missing_platform() {
        val response = VersionResponse(ios = UpdateInfo(version = "1.2.0"))
        val result = UpdateChecker.evaluateResponse(response, "1.1.0", "Android")
        assertEquals(UpdateStatus.UP_TO_DATE, result.status)
    }
}
