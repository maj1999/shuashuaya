package com.cleanpic.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeManagerTest {
    @Test fun default_theme_is_warm() {
        val manager = ThemeManager()
        assertEquals("warm", manager.currentTheme.value.id)
        assertEquals(ThemeLayoutId.WARM, manager.currentTheme.value.layoutId)
    }

    @Test fun switch_theme_updates_current() {
        val manager = ThemeManager()
        manager.switchTheme("warm")
        assertEquals("warm", manager.currentTheme.value.id)
    }

    @Test fun unknown_theme_id_falls_back_to_default() {
        val manager = ThemeManager()
        manager.switchTheme("dreamy-gradient")
        assertEquals("warm", manager.currentTheme.value.id)
    }

    @Test fun old_theme_ids_fall_back_to_default() {
        val manager = ThemeManager()
        val oldIds = listOf("dreamy-gradient", "soft-minimal", "cute-playful", "elegant-dark", "natural-warm")
        for (oldId in oldIds) {
            manager.switchTheme(oldId)
            assertEquals("warm", manager.currentTheme.value.id, "旧 ID '$oldId' 应回退到 warm")
        }
    }

    @Test fun all_themes_have_complete_tokens() {
        val manager = ThemeManager()
        manager.allThemes.forEach { theme ->
            assertTrue(theme.colorPrimary != 0L, "${theme.id} 缺少 colorPrimary")
            assertTrue(theme.colorBackground != 0L, "${theme.id} 缺少 colorBackground")
            assertTrue(theme.colorDanger != 0L, "${theme.id} 缺少 colorDanger")
            assertTrue(theme.colorSuccess != 0L, "${theme.id} 缺少 colorSuccess")
            assertTrue(theme.borderRadius >= 0, "${theme.id} borderRadius 无效")
            assertTrue(theme.iconStrokeWidth > 0, "${theme.id} iconStrokeWidth 无效")
        }
    }

    @Test fun all_themes_available() {
        val manager = ThemeManager()
        assertEquals(1, manager.allThemes.size)
    }
}
