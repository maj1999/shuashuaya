package com.cleanpic.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThemeManagerTest {
    @Test fun default_theme_is_dreamy_gradient() {
        val manager = ThemeManager()
        assertEquals("dreamy-gradient", manager.currentTheme.value.id)
    }
    @Test fun switch_theme_updates_current() {
        val manager = ThemeManager()
        manager.switchTheme("elegant-dark")
        assertEquals("elegant-dark", manager.currentTheme.value.id)
    }
    @Test fun all_themes_have_complete_tokens() {
        val manager = ThemeManager()
        manager.allThemes.forEach { theme ->
            assertNotNull(theme.colorPrimary, "${theme.id} missing colorPrimary")
            assertNotNull(theme.colorBackground, "${theme.id} missing colorBackground")
            assertNotNull(theme.colorDanger, "${theme.id} missing colorDanger")
            assertNotNull(theme.colorSuccess, "${theme.id} missing colorSuccess")
            assertTrue(theme.borderRadius > 0, "${theme.id} invalid borderRadius")
        }
    }
    @Test fun all_five_themes_available() {
        val manager = ThemeManager()
        assertEquals(5, manager.allThemes.size)
    }
}
