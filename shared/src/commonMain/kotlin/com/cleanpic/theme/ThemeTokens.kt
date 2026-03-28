package com.cleanpic.theme

/**
 * 主题渐变定义
 */
data class GradientDef(
    val angle: Float,
    val colors: List<Long>
)

/**
 * 阴影样式定义
 */
data class ShadowDef(
    val offsetX: Float,
    val offsetY: Float,
    val blur: Float,
    val color: Long
)

/**
 * 按钮按下动画类型
 */
enum class ButtonPressAnim { NONE, SCALE, BOUNCE }

/**
 * 主题设计令牌 — 承载完整的主题视觉定义
 */
data class ThemeTokens(
    val id: String,
    val name: String,
    val colorPrimary: Long,
    val colorAccent: Long,
    val colorBackground: Long,
    val colorSurface: Long,
    val colorDanger: Long,
    val colorSuccess: Long,
    val colorText: Long,
    val colorTextSecondary: Long,
    val gradientMain: GradientDef?,
    val borderRadius: Float,
    val shadowStyle: ShadowDef,
    val fontFamily: String,
    val animDuration: Long,
    val animEasing: String,
    val animButtonPress: ButtonPressAnim
)
