package com.cleanpic.theme

/**
 * 温暖手工感主题 — 暖色调、大圆角、柔和阴影、衬线标题
 * 参考：Bear / Things
 */
val WarmTheme = ThemeTokens(
    id = "warm",
    name = "温暖手工感",
    colorPrimary = 0xFF5D4037,
    colorAccent = 0xFF8D6E63,
    colorBackground = 0xFFFFF8F0,
    colorSurface = 0xFFFFFFFF,
    colorDanger = 0xFFE57373,
    colorSuccess = 0xFF81C784,
    colorText = 0xFF5D4037,
    colorTextSecondary = 0xFFA1887F,
    gradientMain = null,
    borderRadius = 20f,
    shadowStyle = ShadowDef(
        offsetX = 0f,
        offsetY = 2f,
        blur = 12f,
        color = 0x1F5D4037
    ),
    fontFamily = "System",
    titleFontFamily = "Serif",
    animDuration = 280L,
    animEasing = "easeOutCubic",
    animButtonPress = ButtonPressAnim.SCALE,
    layoutId = ThemeLayoutId.WARM,
    iconStrokeWidth = 1.8f,
    iconStrokeColor = 0xFF8D6E63,
    iconStrokeCap = IconStrokeCap.ROUND,
    progressStyle = ProgressStyle.SOFT,
    buttonStyle = ButtonStyle.SHADOW
)
