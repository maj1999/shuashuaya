package com.cleanpic.theme

/**
 * 梦幻渐变主题 — 紫粉金配色，毛玻璃阴影，缩放动画
 */
val DreamyGradientTheme = ThemeTokens(
    id = "dreamy-gradient",
    name = "梦幻渐变",
    colorPrimary = 0xFF9C27B0,    // 紫色
    colorAccent = 0xFFE91E63,      // 粉色
    colorBackground = 0xFFF3E5F5,  // 浅紫背景
    colorSurface = 0xFFFFFFFF,
    colorDanger = 0xFFF44336,      // 红色
    colorSuccess = 0xFF4CAF50,     // 绿色
    colorText = 0xFF212121,
    colorTextSecondary = 0xFF757575,
    gradientMain = GradientDef(
        angle = 135f,
        colors = listOf(0xFF9C27B0, 0xFFE91E63, 0xFFFFD54F)
    ),
    borderRadius = 16f,
    shadowStyle = ShadowDef(
        offsetX = 0f,
        offsetY = 4f,
        blur = 20f,
        color = 0x40000000  // 毛玻璃风格半透明阴影
    ),
    fontFamily = "System",
    animDuration = 300L,
    animEasing = "easeInOutCubic",
    animButtonPress = ButtonPressAnim.SCALE
)
