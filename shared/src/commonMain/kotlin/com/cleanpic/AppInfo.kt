package com.cleanpic

object AppInfo {
    const val APP_NAME = "刷刷鸭"
    const val VERSION = "1.17.0"
    // 与 buildSrc/CleanPicBuildConfig.kt 的 VERSION_CODE 保持一致；发布脚本同步更新
    const val VERSION_CODE = 50
    val displayVersion: String get() = "$APP_NAME v$VERSION"
}
