package com.cleanpic.theme

val PlayfulTheme = ThemeTokens(
    id = "playful", name = "活泼精致",
    colorPrimary = 0xFF667EEA, colorAccent = 0xFF764BA2,
    colorBackground = 0xFF667EEA, colorSurface = 0x1FFFFFFF,
    colorDanger = 0x4DE57373, colorSuccess = 0x4D81C784,
    colorText = 0xFFFFFFFF, colorTextSecondary = 0x99FFFFFF,
    gradientMain = GradientDef(160f, listOf(0xFF667EEA, 0xFF764BA2)),
    borderRadius = 20f,
    shadowStyle = ShadowDef(0f, 4f, 20f, 0x40000000),
    fontFamily = "System", titleFontFamily = "System",
    animDuration = 350L, animEasing = "easeOutBounce",
    animButtonPress = ButtonPressAnim.BOUNCE,
    layoutId = ThemeLayoutId.PLAYFUL, iconStrokeWidth = 2.0f,
    iconStrokeColor = 0xFFFFFFFF, iconStrokeCap = IconStrokeCap.ROUND,
    progressStyle = ProgressStyle.GLASS, buttonStyle = ButtonStyle.GLASS
)
