package com.cleanpic.ui.home

import com.cleanpic.theme.ThemeTokens

/**
 * 首页的共享状态 — 5 个布局变体通过此接口接收数据和回调。
 * 业务逻辑保留在 HomeScreen composable 中。
 */
data class HomeScreenState(
    val theme: ThemeTokens,
    val isLimitedAccess: Boolean,
    val onStartPhoto: () -> Unit,
    val onStartVideo: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onShowDeniedDialog: () -> Unit,
    val onShowPermanentDialog: () -> Unit
)
