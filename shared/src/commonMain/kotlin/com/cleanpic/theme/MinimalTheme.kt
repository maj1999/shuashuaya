package com.cleanpic.theme

val MinimalTheme = ThemeTokens(
    id = "minimal", name = "克制极简",
    colorPrimary = 0xFF333333, colorAccent = 0xFF999999,
    colorBackground = 0xFFFAFAFA, colorSurface = 0xFFFFFFFF,
    colorDanger = 0xFFE57373, colorSuccess = 0xFF81C784,
    colorText = 0xFF333333, colorTextSecondary = 0xFF999999,
    gradientMain = null, borderRadius = 2f,
    shadowStyle = ShadowDef(0f, 1f, 4f, 0x0A000000),
    fontFamily = "System", titleFontFamily = "System",
    animDuration = 250L, animEasing = "easeOutQuad",
    animButtonPress = ButtonPressAnim.NONE,
    layoutId = ThemeLayoutId.MINIMAL, iconStrokeWidth = 1.2f,
    iconStrokeColor = 0xFF333333, iconStrokeCap = IconStrokeCap.BUTT,
    progressStyle = ProgressStyle.THIN, buttonStyle = ButtonStyle.OUTLINED
)
