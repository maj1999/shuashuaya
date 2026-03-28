package com.cleanpic.theme

/**
 * 自然温暖主题 — 灰棕暖黄配色，暖色渐变，温暖阴影，缩放动画
 */
val NaturalWarmTheme = ThemeTokens(
    id = "natural-warm",
    name = "自然温暖",
    colorPrimary = 0xFF8D6E63,    // 棕色
    colorAccent = 0xFFFFCA28,      // 暖黄色
    colorBackground = 0xFFF5F5F0,  // 灰白背景
    colorSurface = 0xFFFFFFFF,
    colorDanger = 0xFFE57373,      // 浅红色
    colorSuccess = 0xFFA5D6A7,     // 浅绿色
    colorText = 0xFF3E2723,
    colorTextSecondary = 0xFF8D6E63,
    gradientMain = GradientDef(
        angle = 120f,
        colors = listOf(0xFFD7CCC8, 0xFFFFCA28, 0xFF8D6E63)
    ),
    borderRadius = 8f,
    shadowStyle = ShadowDef(
        offsetX = 1f,
        offsetY = 3f,
        blur = 10f,
        color = 0x308D6E63  // 棕色温暖阴影
    ),
    fontFamily = "System",
    animDuration = 260L,
    animEasing = "easeOutCubic",
    animButtonPress = ButtonPressAnim.SCALE
)
