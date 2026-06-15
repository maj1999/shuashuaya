package com.cleanpic.update

import java.io.File
import java.security.MessageDigest

/**
 * 安装包完整性校验（方案 §12 #2 / F1）。
 *
 * 防止国内下载源（如 Gitee）触发防盗链/风控时返回的 `200 + HTML 验证页`被当成 .apk 安装——
 * 那会让用户进度走完后在系统安装器看到"解析包错误"，无日志可查。下载完成后先校验再安装：
 * 1. ZIP 魔数（APK 本质是 ZIP，必须以 `PK\x03\x04` 开头）——HTML 伪装页会被挡下；
 * 2. 文件大小（若 version.json 提供了 size）；
 * 3. sha256（若 version.json 提供了 sha256）。
 *
 * 完整性字段为空（兜底/旧 version.json）时跳过对应校验，但 ZIP 魔数兜底始终执行，保持向后兼容。
 */
internal object ApkIntegrity {

    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // "PK\x03\x04"

    fun verify(file: File, expectedSha256: String, expectedSize: Long): Boolean {
        if (!file.exists() || file.length() < ZIP_MAGIC.size) return false
        if (!hasZipMagic(file)) return false
        if (expectedSize > 0 && file.length() != expectedSize) return false
        if (expectedSha256.isNotBlank() && !sha256Hex(file).equals(expectedSha256, ignoreCase = true)) return false
        return true
    }

    /**
     * 能否复用本地已下载的安装包（免重复下载）。
     *
     * 仅当 version.json 提供了 **sha256** 时才允许复用：sha256 是防篡改的强校验，
     * 缺失时（兜底/旧 version.json）ZIP 魔数+大小校验过弱，宁可重新下载也不冒险装本地包。
     * 本地包被篡改/损坏 → sha256 不匹配 → 返回 false → 调用方回退重新下载。
     */
    fun canReuseLocalApk(file: File, expectedSha256: String, expectedSize: Long): Boolean =
        expectedSha256.isNotBlank() && file.exists() && verify(file, expectedSha256, expectedSize)

    private fun hasZipMagic(file: File): Boolean {
        val head = ByteArray(ZIP_MAGIC.size)
        file.inputStream().use { input ->
            if (input.read(head) != ZIP_MAGIC.size) return false
        }
        return head.contentEquals(ZIP_MAGIC)
    }

    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
}
