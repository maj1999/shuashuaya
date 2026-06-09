package com.cleanpic.update

import com.cleanpic.theme.PlayfulTheme
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.theme.ButtonPressAnim
import com.cleanpic.theme.ShadowDef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DialogSurfaceColorTest {

    private fun theme(surface: Long, background: Long) = ThemeTokens(
        id = "test",
        name = "Test",
        colorPrimary = 0xFF6200EE,
        colorAccent = 0xFF03DAC5,
        colorBackground = background,
        colorSurface = surface,
        colorDanger = 0xFFB00020,
        colorSuccess = 0xFF00C853,
        colorText = 0xFFFFFFFF,
        colorTextSecondary = 0x99FFFFFF,
        gradientMain = null,
        borderRadius = 12f,
        shadowStyle = ShadowDef(0f, 2f, 4f, 0x40000000),
        fontFamily = "System",
        animDuration = 300,
        animEasing = "ease",
        animButtonPress = ButtonPressAnim.NONE
    )

    private fun alphaOf(color: Long) = ((color ushr 24) and 0xFF).toInt()
    private fun redOf(color: Long) = ((color ushr 16) and 0xFF).toInt()
    private fun greenOf(color: Long) = ((color ushr 8) and 0xFF).toInt()
    private fun blueOf(color: Long) = (color and 0xFF).toInt()

    // ── 已不透明的 surface 原样返回 ──
    @Test
    fun opaque_surface_returned_unchanged() {
        val t = theme(surface = 0xFFFFFFFF, background = 0xFFFAFAFA)
        assertEquals(0xFFFFFFFFL, dialogSurfaceColor(t))
    }

    // ── 半透明 surface 合成后必须完全不透明 ──
    @Test
    fun translucent_surface_becomes_opaque() {
        val t = theme(surface = 0x1FFFFFFF, background = 0xFF667EEA)
        val result = dialogSurfaceColor(t)
        assertEquals(0xFF, alphaOf(result), "弹窗卡片背景必须完全不透明")
    }

    // ── 半透明白 over 紫底：结果应朝白色变浅、各通道介于背景与白色之间 ──
    @Test
    fun translucent_white_lightens_toward_white() {
        val bg = 0xFF667EEA
        val t = theme(surface = 0x1FFFFFFF, background = bg)
        val result = dialogSurfaceColor(t)
        assertTrue(redOf(result) in redOf(bg)..255)
        assertTrue(greenOf(result) in greenOf(bg)..255)
        assertTrue(blueOf(result) in blueOf(bg)..255)
        assertTrue(result != bg, "合成后应与纯背景色不同")
    }

    // ── 回归守门：Playful 主题（截图复现的皮肤）弹窗卡片必须不透明 ──
    @Test
    fun playful_theme_dialog_surface_is_opaque() {
        val result = dialogSurfaceColor(PlayfulTheme)
        assertEquals(0xFF, alphaOf(result), "Playful 主题弹窗卡片不能半透明（否则首页内容透出）")
    }

    // ── 强调色：对比度足够时保留主题 colorPrimary ──
    @Test
    fun accent_keeps_primary_when_contrast_is_sufficient() {
        // 深紫 primary 在白色卡片上对比度高
        val t = theme(surface = 0xFFFFFFFF, background = 0xFFFFFFFF)
        assertEquals(t.colorPrimary, dialogAccentColor(t))
    }

    // ── 强调色：Playful 版本号不能再用与底色同色系的 colorPrimary ──
    @Test
    fun accent_falls_back_when_primary_blends_with_surface() {
        // primary == background，合成后卡片同色系 → 必须回退
        val accent = dialogAccentColor(PlayfulTheme)
        assertEquals(PlayfulTheme.colorText, accent, "版本号应回退到高对比的 colorText")
        assertTrue(
            contrastRatio(accent or 0xFF000000L, dialogSurfaceColor(PlayfulTheme)) >= MIN_ACCENT_CONTRAST,
            "回退后的版本号文字与卡片底色对比度必须达标"
        )
    }
}
