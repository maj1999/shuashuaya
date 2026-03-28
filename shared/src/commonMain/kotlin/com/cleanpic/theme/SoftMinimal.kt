package com.cleanpic.theme

/**
 * 柔和极简主题 — 柔粉白配色，纸张阴影，无特效动画
 */
val SoftMinimalTheme = ThemeTokens(
    id = "soft-minimal",
    name = "柔和极简",
    colorPrimary = 0xFFF8BBD0,    // 柔粉色
    colorAccent = 0xFFCE93D8,      // 淡紫色
    colorBackground = 0xFFFFFBFE,  // 近白背景
    colorSurface = 0xFFFFFFFF,
    colorDanger = 0xFFEF5350,
    colorSuccess = 0xFF66BB6A,
    colorText = 0xFF424242,
    colorTextSecondary = 0xFF9E9E9E,
    gradientMain = null,           // 无渐变
    borderRadius = 20f,
    shadowStyle = ShadowDef(
        offsetX = 0f,
        offsetY = 2f,
        blur = 8f,
        color = 0x1A000000  // 纸张风格轻柔阴影
    ),
    fontFamily = "System",
    animDuration = 250L,
    animEasing = "easeOutQuad",
    animButtonPress = ButtonPressAnim.NONE
)
