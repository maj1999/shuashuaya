package com.cleanpic.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val version: String,
    val versionCode: Int? = null,
    val forceUpdate: Boolean = false,
    val minVersion: String = "0.0.0",
    val changelog: String = "",
    val downloadUrl: String = "",
    // 安装包完整性校验：下载后比对，防止 Gitee 防盗链/风控返回的 HTML 伪装包被当成 APK 安装（方案 §12 #2）
    val sha256: String = "",
    val size: Long = 0
)

enum class UpdateStatus {
    UP_TO_DATE,
    OPTIONAL_UPDATE,
    FORCE_UPDATE
}

data class UpdateCheckResult(
    val status: UpdateStatus,
    val updateInfo: UpdateInfo? = null
)

@Serializable
data class VersionResponse(
    val android: UpdateInfo? = null,
    val ios: UpdateInfo? = null,
    val harmonyos: UpdateInfo? = null
)
