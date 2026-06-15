package com.cleanpic.ui.settings

import androidx.compose.runtime.Composable
import com.cleanpic.theme.ThemeTokens

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
    /** 重置浏览记录（US-CP-23）：清空全部浏览/保留记忆 */
    val onResetHistory: () -> Unit,
    val onBack: () -> Unit,
    /** 导出诊断日志（SAF 下载） */
    val onExportLogs: () -> Unit = {},
    /** 由宿主 flavor 注入的额外区块（如升级 UI），默认空 */
    val extras: @Composable () -> Unit = {}
)
