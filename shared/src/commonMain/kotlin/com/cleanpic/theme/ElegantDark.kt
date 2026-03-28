package com.cleanpic.theme

/**
 * 优雅暗黑主题 — 深紫金配色，暗色渐变，微光阴影，缩放动画
 */
val ElegantDarkTheme = ThemeTokens(
    id = "elegant-dark",
    name = "优雅暗黑",
    colorPrimary = 0xFF7B1FA2,    // 深紫色
    colorAccent = 0xFFFFD700,      // 金色
    colorBackground = 0xFF121212,  // 暗黑背景
    colorSurface = 0xFF1E1E1E,
    colorDanger = 0xFFCF6679,      // 柔红色
    colorSuccess = 0xFF03DAC6,     // 青绿色
    colorText = 0xFFE0E0E0,
    colorTextSecondary = 0xFF9E9E9E,
    gradientMain = GradientDef(
        angle = 160f,
        colors = listOf(0xFF1A0033, 0xFF7B1FA2, 0xFF2C003E)
    ),
    borderRadius = 12f,
    shadowStyle = ShadowDef(
        offsetX = 0f,
        offsetY = 2f,
        blur = 16f,
        color = 0x607B1FA2  // 紫色微光阴影
    ),
    fontFamily = "System",
    animDuration = 280L,
    animEasing = "easeInOutCubic",
    animButtonPress = ButtonPressAnim.SCALE
)
