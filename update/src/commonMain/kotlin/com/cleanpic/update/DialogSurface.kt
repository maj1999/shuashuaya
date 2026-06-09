package com.cleanpic.update

import com.cleanpic.theme.ThemeTokens
import kotlin.math.pow

/**
 * 模态弹窗卡片的背景色。
 *
 * 把主题的 [ThemeTokens.colorSurface] 合成（source-over）到不透明的
 * [ThemeTokens.colorBackground] 之上，保证弹窗卡片**始终不透明**。
 *
 * 背景：Playful 主题的 `colorSurface = 0x1FFFFFFF` 是给首页毛玻璃卡片用的
 * 半透明 token（alpha 仅 12%）。更新弹窗若直接复用它当卡片背景，会把首页
 * 内容透上来、与遮罩叠成花屏（「发现新版本」弹窗叠影 bug）。这里统一把它
 * 合成到不透明底色上，既保留毛玻璃色调又不再透出背后内容。
 *
 * 颜色按 `0xAARRGGBB` 的 [Long] 表示。
 */
fun dialogSurfaceColor(theme: ThemeTokens): Long {
    val surface = theme.colorSurface
    val sa = ((surface ushr 24) and 0xFF).toInt()
    if (sa == 0xFF) return surface // 已不透明，原样返回

    val background = theme.colorBackground or 0xFF000000L // 背景视为不透明
    val sr = ((surface ushr 16) and 0xFF).toInt()
    val sg = ((surface ushr 8) and 0xFF).toInt()
    val sb = (surface and 0xFF).toInt()
    val br = ((background ushr 16) and 0xFF).toInt()
    val bg = ((background ushr 8) and 0xFF).toInt()
    val bb = (background and 0xFF).toInt()

    val a = sa / 255.0
    val r = (sr * a + br * (1 - a)).toInt().coerceIn(0, 255)
    val g = (sg * a + bg * (1 - a)).toInt().coerceIn(0, 255)
    val b = (sb * a + bb * (1 - a)).toInt().coerceIn(0, 255)

    return (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

/**
 * 弹窗内强调文字（如版本号）的颜色。
 *
 * 优先用主题的 [ThemeTokens.colorPrimary]；但当它与弹窗卡片底色
 * （[dialogSurfaceColor]）对比度不足时回退到 [ThemeTokens.colorText]，保证可读。
 *
 * 背景：Playful 主题的 `colorPrimary == colorBackground == 0xFF667EEA`，合成出的
 * 卡片底色是同色系的浅蓝紫，版本号若仍用 colorPrimary 会蓝字贴蓝底糊在一起。
 */
fun dialogAccentColor(theme: ThemeTokens): Long {
    val surface = dialogSurfaceColor(theme)
    val primary = theme.colorPrimary or 0xFF000000L
    return if (contrastRatio(primary, surface) >= MIN_ACCENT_CONTRAST) {
        theme.colorPrimary
    } else {
        theme.colorText
    }
}

/** 强调文字相对卡片底色的最小对比度（WCAG 大号文字阈值）。 */
internal const val MIN_ACCENT_CONTRAST = 3.0

/** 两个不透明颜色的 WCAG 对比度（1.0 ~ 21.0）。颜色按 `0xAARRGGBB`。 */
internal fun contrastRatio(a: Long, b: Long): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05) / (lo + 0.05)
}

private fun relativeLuminance(color: Long): Double {
    fun linear(channel: Int): Double {
        val s = channel / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    val r = linear(((color ushr 16) and 0xFF).toInt())
    val g = linear(((color ushr 8) and 0xFF).toInt())
    val b = linear((color and 0xFF).toInt())
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}
