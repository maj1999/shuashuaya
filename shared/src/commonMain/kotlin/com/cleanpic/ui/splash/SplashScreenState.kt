package com.cleanpic.ui.splash

import com.cleanpic.theme.ThemeTokens

/**
 * 闪屏的共享状态 — 5 个布局变体通过此接口接收主题和完成回调。
 */
data class SplashScreenState(
    val theme: ThemeTokens,
    val onSplashComplete: () -> Unit
)
