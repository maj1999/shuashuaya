package com.cleanpic.theme

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsLightColorTest {

    @Test fun pure_white_is_light() {
        assertTrue(isLightColor(0xFFFFFFFF))
    }

    @Test fun pure_black_is_not_light() {
        assertFalse(isLightColor(0xFF000000))
    }

    @Test fun warm_theme_background_is_light() {
        assertTrue(isLightColor(WarmTheme.colorBackground))
    }

    @Test fun warm_theme_text_is_not_light() {
        assertFalse(isLightColor(WarmTheme.colorText))
    }

    @Test fun minimal_theme_background_is_light() {
        assertTrue(isLightColor(MinimalTheme.colorBackground))
    }

    @Test fun minimal_theme_text_is_not_light() {
        assertFalse(isLightColor(MinimalTheme.colorText))
    }

    @Test fun geometric_theme_background_is_not_light() {
        assertFalse(isLightColor(GeometricTheme.colorBackground))
    }

    @Test fun geometric_theme_text_is_light() {
        assertTrue(isLightColor(GeometricTheme.colorText))
    }

    @Test fun playful_theme_background_is_borderline_light() {
        // 0xFF667EEA — 蓝紫色，亮度 ~0.514，刚好超过 0.5 阈值
        // 状态栏图标由 colorText 决定，不受此影响
        assertTrue(isLightColor(PlayfulTheme.colorBackground))
    }

    @Test fun playful_theme_text_is_light() {
        assertTrue(isLightColor(PlayfulTheme.colorText))
    }

    @Test fun editorial_theme_background_is_light() {
        assertTrue(isLightColor(EditorialTheme.colorBackground))
    }

    @Test fun editorial_theme_text_is_not_light() {
        assertFalse(isLightColor(EditorialTheme.colorText))
    }

    @Test fun status_bar_icons_correct_for_all_themes() {
        // 状态栏图标明暗由 colorText 决定：浅色文字 → 浅色图标，深色文字 → 深色图标
        // 验证白色文字主题得到浅色图标（isAppearanceLightStatusBars = false）
        // 深色文字主题得到深色图标（isAppearanceLightStatusBars = true）
        val lightTextThemes = listOf(GeometricTheme, PlayfulTheme)
        val darkTextThemes = listOf(WarmTheme, MinimalTheme, EditorialTheme)

        for (theme in lightTextThemes) {
            assertTrue(
                isLightColor(theme.colorText),
                "${theme.id}: 白色文字应判定为浅色 → 状态栏用浅色图标"
            )
        }
        for (theme in darkTextThemes) {
            assertFalse(
                isLightColor(theme.colorText),
                "${theme.id}: 深色文字应判定为非浅色 → 状态栏用深色图标"
            )
        }
    }
}
