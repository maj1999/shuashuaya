package com.cleanpic.theme

val EditorialTheme = ThemeTokens(
    id = "editorial", name = "杂志排版",
    colorPrimary = 0xFF1A1A1A, colorAccent = 0xFF999999,
    colorBackground = 0xFFFFFFF5, colorSurface = 0xFFF0EDE6,
    colorDanger = 0xFFC62828, colorSuccess = 0xFF2E7D32,
    colorText = 0xFF1A1A1A, colorTextSecondary = 0xFF555555,
    gradientMain = null, borderRadius = 2f,
    shadowStyle = ShadowDef(0f, 0f, 0f, 0x00000000),
    fontFamily = "Serif", titleFontFamily = "Serif",
    animDuration = 300L, animEasing = "easeInOutCubic",
    animButtonPress = ButtonPressAnim.NONE,
    layoutId = ThemeLayoutId.EDITORIAL, iconStrokeWidth = 1.0f,
    iconStrokeColor = 0xFF999999, iconStrokeCap = IconStrokeCap.BUTT,
    progressStyle = ProgressStyle.EDITORIAL, buttonStyle = ButtonStyle.TEXT
)
