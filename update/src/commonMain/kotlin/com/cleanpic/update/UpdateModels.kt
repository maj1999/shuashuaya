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
    // 备用下载源（如 GitHub Releases）。Gitee 附件 CDN 在境外常超时/卡死，主源失败时客户端自动回退到此源。
    // 两源 APK 为同一构建，sha256 一致，故下方完整性校验对两源通用。新增字段，老客户端 ignoreUnknownKeys 会忽略。
    val downloadUrlFallback: String = "",
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
