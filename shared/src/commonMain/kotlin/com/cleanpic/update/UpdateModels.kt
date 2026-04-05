package com.cleanpic.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val version: String,
    val versionCode: Int? = null,
    val forceUpdate: Boolean = false,
    val minVersion: String = "0.0.0",
    val changelog: String = "",
    val downloadUrl: String = ""
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
