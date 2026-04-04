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
 * 主题布局标识 — 决定每个页面使用哪种布局
 */
enum class ThemeLayoutId { MINIMAL, GEOMETRIC, WARM, PLAYFUL, EDITORIAL }

/**
 * 进度条视觉样式
 */
enum class ProgressStyle { THIN, BOLD, SOFT, GLASS, EDITORIAL }

/**
 * 按钮视觉样式
 */
enum class ButtonStyle { OUTLINED, FILLED, SHADOW, GLASS, TEXT }

/**
 * 图标线端样式
 */
enum class IconStrokeCap { BUTT, ROUND, SQUARE }

/**
 * 主题设计令牌 — 承载完整的主题视觉定义
 */
data class ThemeTokens(
    val id: String,
    val name: String,
    // 颜色
    val colorPrimary: Long,
    val colorAccent: Long,
    val colorBackground: Long,
    val colorSurface: Long,
    val colorDanger: Long,
    val colorSuccess: Long,
    val colorText: Long,
    val colorTextSecondary: Long,
    // 渐变与形状
    val gradientMain: GradientDef?,
    val borderRadius: Float,
    val shadowStyle: ShadowDef,
    // 字体
    val fontFamily: String,
    val titleFontFamily: String = "System",
    // 动画
    val animDuration: Long,
    val animEasing: String,
    val animButtonPress: ButtonPressAnim,
    // v2：布局与图标
    val layoutId: ThemeLayoutId = ThemeLayoutId.WARM,
    val iconStrokeWidth: Float = 1.8f,
    val iconStrokeColor: Long = 0xFF333333,
    val iconStrokeCap: IconStrokeCap = IconStrokeCap.ROUND,
    val progressStyle: ProgressStyle = ProgressStyle.SOFT,
    val buttonStyle: ButtonStyle = ButtonStyle.SHADOW
)
