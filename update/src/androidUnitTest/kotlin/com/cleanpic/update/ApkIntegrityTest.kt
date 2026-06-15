package com.cleanpic.update

import java.io.File
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * U-UPD-10：下载完整性校验（方案 §12 #2 / F1）。纯 JVM 测试，无需 Robolectric。
 */
class ApkIntegrityTest {

    private val temp = mutableListOf<File>()

    private fun writeFile(name: String, bytes: ByteArray): File {
        // createTempFile 前缀要求 >=3 字符，统一加前缀避免短名报错
        val f = File.createTempFile("apkint-$name", ".bin").also { temp += it }
        f.writeBytes(bytes)
        return f
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    // 伪造一个"合法 APK"：以 ZIP 魔数开头 + 任意内容
    private val validApkBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04) + ByteArray(100) { it.toByte() }
    private val htmlBytes = "<html>登录验证</html>".toByteArray()

    @AfterTest
    fun cleanup() {
        temp.forEach { it.delete() }
        temp.clear()
    }

    @Test
    fun valid_apk_with_matching_hash_and_size_passes() {
        val f = writeFile("ok", validApkBytes)
        assertTrue(ApkIntegrity.verify(f, sha256(validApkBytes), validApkBytes.size.toLong()))
    }

    @Test
    fun html_disguised_as_apk_fails_on_zip_magic() {
        val f = writeFile("html", htmlBytes)
        // 即使不提供 sha256/size，ZIP 魔数也挡下 HTML 伪装包
        assertFalse(ApkIntegrity.verify(f, "", 0))
    }

    @Test
    fun wrong_size_fails() {
        val f = writeFile("size", validApkBytes)
        assertFalse(ApkIntegrity.verify(f, sha256(validApkBytes), validApkBytes.size.toLong() + 1))
    }

    @Test
    fun wrong_hash_fails() {
        val f = writeFile("hash", validApkBytes)
        assertFalse(ApkIntegrity.verify(f, "deadbeef", validApkBytes.size.toLong()))
    }

    @Test
    fun missing_integrity_fields_skips_hash_but_keeps_zip_check() {
        // 兜底/旧 version.json 无 sha256/size：合法 ZIP 仍通过（向后兼容）
        val ok = writeFile("compat-ok", validApkBytes)
        assertTrue(ApkIntegrity.verify(ok, "", 0))
        // 但 HTML 仍被 ZIP 魔数挡下
        val bad = writeFile("compat-bad", htmlBytes)
        assertFalse(ApkIntegrity.verify(bad, "", 0))
    }

    @Test
    fun sha256_hex_is_correct() {
        val f = writeFile("digest", validApkBytes)
        assertEquals(sha256(validApkBytes), ApkIntegrity.sha256Hex(f))
    }

    // ---- canReuseLocalApk：本地已下载包复用判定（免重复下载 + 防篡改）----

    @Test
    fun reuse_local_apk_when_present_and_hash_matches() {
        // 本地存在、sha256/大小匹配 → 复用（不再重新下载）
        val f = writeFile("reuse-ok", validApkBytes)
        assertTrue(ApkIntegrity.canReuseLocalApk(f, sha256(validApkBytes), validApkBytes.size.toLong()))
    }

    @Test
    fun do_not_reuse_tampered_local_apk() {
        // 本地包被篡改：仍是合法 ZIP、大小不变，但内容改了 → sha256 不匹配 → 不复用，回退重下
        val original = validApkBytes
        val tampered = validApkBytes.copyOf().also { it[10] = (it[10] + 1).toByte() }
        val f = writeFile("reuse-tampered", tampered)
        assertFalse(ApkIntegrity.canReuseLocalApk(f, sha256(original), original.size.toLong()))
    }

    @Test
    fun do_not_reuse_when_sha256_absent() {
        // version.json 无 sha256（兜底/旧版）：校验过弱，宁可重新下载也不复用本地包
        val f = writeFile("reuse-no-sha", validApkBytes)
        assertFalse(ApkIntegrity.canReuseLocalApk(f, "", validApkBytes.size.toLong()))
    }

    @Test
    fun do_not_reuse_when_file_absent() {
        // 本地没下载过 → 不复用
        val f = File.createTempFile("reuse-absent", ".bin").also { it.delete() }
        assertFalse(ApkIntegrity.canReuseLocalApk(f, sha256(validApkBytes), validApkBytes.size.toLong()))
    }
}
