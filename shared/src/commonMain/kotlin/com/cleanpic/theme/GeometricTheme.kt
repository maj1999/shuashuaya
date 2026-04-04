package com.cleanpic.theme

val GeometricTheme = ThemeTokens(
    id = "geometric", name = "大胆几何",
    colorPrimary = 0xFFE94560, colorAccent = 0xFF533483,
    colorBackground = 0xFF1A1A2E, colorSurface = 0xFF16213E,
    colorDanger = 0xFFE94560, colorSuccess = 0xFF4CAF50,
    colorText = 0xFFFFFFFF, colorTextSecondary = 0x66FFFFFF,
    gradientMain = GradientDef(180f, listOf(0xFFE94560, 0xFF0F3460)),
    borderRadius = 16f,
    shadowStyle = ShadowDef(0f, 4f, 16f, 0x60000000),
    fontFamily = "System", titleFontFamily = "System",
    animDuration = 300L, animEasing = "easeInOutCubic",
    animButtonPress = ButtonPressAnim.SCALE,
    layoutId = ThemeLayoutId.GEOMETRIC, iconStrokeWidth = 2.5f,
    iconStrokeColor = 0xFFFFFFFF, iconStrokeCap = IconStrokeCap.BUTT,
    progressStyle = ProgressStyle.BOLD, buttonStyle = ButtonStyle.FILLED
)
