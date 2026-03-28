package com.cleanpic.theme

/**
 * 可爱活泼主题 — 黄绿蓝多彩配色，多色渐变，弹跳动画
 */
val CutePlayfulTheme = ThemeTokens(
    id = "cute-playful",
    name = "可爱活泼",
    colorPrimary = 0xFFFFEB3B,    // 明黄色
    colorAccent = 0xFF4FC3F7,      // 天蓝色
    colorBackground = 0xFFFFFDE7,  // 浅黄背景
    colorSurface = 0xFFFFFFFF,
    colorDanger = 0xFFFF7043,      // 橙红色
    colorSuccess = 0xFF81C784,     // 草绿色
    colorText = 0xFF333333,
    colorTextSecondary = 0xFF888888,
    gradientMain = GradientDef(
        angle = 90f,
        colors = listOf(0xFFFFEB3B, 0xFF81C784, 0xFF4FC3F7)
    ),
    borderRadius = 24f,
    shadowStyle = ShadowDef(
        offsetX = 2f,
        offsetY = 4f,
        blur = 12f,
        color = 0x30FF9800  // 暖色调活泼阴影
    ),
    fontFamily = "System",
    animDuration = 350L,
    animEasing = "easeOutBounce",
    animButtonPress = ButtonPressAnim.BOUNCE
)
