package com.cleanpic.ui.settings

import com.cleanpic.theme.ThemeTokens
import com.cleanpic.update.UpdateCheckResult

/**
 * 设置页的共享状态 — 5 个布局变体通过此接口接收数据和回调。
 */
data class SettingsScreenState(
    val theme: ThemeTokens,
    val allThemes: List<ThemeTokens>,
    val currentMode: String,
    val currentCount: Int,
    val onThemeChange: (String) -> Unit,
    val onModeChange: (String) -> Unit,
    val onCountChange: (Int) -> Unit,
    val onBack: () -> Unit,
    // 自动升级相关
    val autoCheckUpdate: Boolean = true,
    val onAutoCheckUpdateChange: (Boolean) -> Unit = {},
    val isCheckingUpdate: Boolean = false,
    val onCheckUpdate: () -> Unit = {},
    val updateCheckResult: UpdateCheckResult? = null,
    val checkResultMessage: String? = null,
    val onStartUpdate: () -> Unit = {},
    // 调试用
    val isDebugBuild: Boolean = false,
    val onSimulateDownload: () -> Unit = {}
)
