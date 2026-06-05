package com.cleanpic.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json

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

    // ── U-UPD-09 versionCode 优先比较 ──

    @Test fun same_version_name_but_code_bumped_is_newer() {
        // 同 version 名仅 bump versionCode 的热修，应判为有新版
        assertTrue(UpdateChecker.isNewer("1.6.0", "1.6.0", currentVersionCode = 29, remoteVersionCode = 30))
    }

    @Test fun version_name_newer_still_detected() {
        assertTrue(UpdateChecker.isNewer("1.5.0", "1.6.0", currentVersionCode = 29, remoteVersionCode = 30))
    }

    @Test fun equal_version_and_code_not_newer() {
        assertFalse(UpdateChecker.isNewer("1.6.0", "1.6.0", currentVersionCode = 30, remoteVersionCode = 30))
    }

    @Test fun lower_remote_code_not_newer() {
        assertFalse(UpdateChecker.isNewer("1.6.0", "1.6.0", currentVersionCode = 30, remoteVersionCode = 29))
    }

    @Test fun falls_back_to_version_string_when_code_missing() {
        // remote 无 versionCode（旧响应）→ 回退 version 字符串比较
        assertTrue(UpdateChecker.isNewer("1.5.0", "1.6.0", currentVersionCode = 30, remoteVersionCode = null))
        assertFalse(UpdateChecker.isNewer("1.6.0", "1.6.0", currentVersionCode = 30, remoteVersionCode = null))
    }

    @Test fun evaluate_uses_versioncode_for_hotfix() {
        val response = VersionResponse(
            android = UpdateInfo(version = "1.6.0", versionCode = 31, minVersion = "1.0.0")
        )
        // 本地 1.6.0/code30，远端 1.6.0/code31 → 同名但 code 更新 → OPTIONAL
        val result = UpdateChecker.evaluateResponse(response, "1.6.0", "Android", currentVersionCode = 30)
        assertEquals(UpdateStatus.OPTIONAL_UPDATE, result.status)
    }

    // ── U-UPD-11 version.json schema（含完整性字段）反序列化 ──

    @Test fun version_json_with_integrity_fields_parses() {
        val raw = """
            {"android":{"version":"1.7.0","versionCode":31,"forceUpdate":false,
            "minVersion":"1.0.0","changelog":"x","downloadUrl":"https://gitee.com/o/r/releases/download/v1.7.0/a.apk",
            "sha256":"abc123","size":14016754}}
        """.trimIndent()
        val resp = Json { ignoreUnknownKeys = true }.decodeFromString(VersionResponse.serializer(), raw)
        assertEquals("abc123", resp.android?.sha256)
        assertEquals(14016754L, resp.android?.size)
        assertEquals(31, resp.android?.versionCode)
    }

    @Test fun version_json_without_integrity_fields_defaults() {
        val raw = """{"android":{"version":"1.7.0"}}"""
        val resp = Json { ignoreUnknownKeys = true }.decodeFromString(VersionResponse.serializer(), raw)
        assertEquals("", resp.android?.sha256)
        assertEquals(0L, resp.android?.size)
    }
}
