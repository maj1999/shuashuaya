package com.cleanpic.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeTokensTest {
    @Test fun theme_layout_id_enum_has_five_values() {
        val values = ThemeLayoutId.entries
        assertEquals(5, values.size)
        assertEquals(
            setOf("MINIMAL", "GEOMETRIC", "WARM", "PLAYFUL", "EDITORIAL"),
            values.map { it.name }.toSet()
        )
    }

    @Test fun progress_style_enum_has_five_values() {
        val values = ProgressStyle.entries
        assertEquals(5, values.size)
        assertEquals(
            setOf("THIN", "BOLD", "SOFT", "GLASS", "EDITORIAL"),
            values.map { it.name }.toSet()
        )
    }

    @Test fun button_style_enum_has_five_values() {
        val values = ButtonStyle.entries
        assertEquals(5, values.size)
        assertEquals(
            setOf("OUTLINED", "FILLED", "SHADOW", "GLASS", "TEXT"),
            values.map { it.name }.toSet()
        )
    }

    @Test fun warm_theme_has_correct_layout_id() {
        assertEquals(ThemeLayoutId.WARM, WarmTheme.layoutId)
        assertEquals("warm", WarmTheme.id)
        assertEquals("温暖手工感", WarmTheme.name)
    }

    @Test fun warm_theme_has_correct_icon_params() {
        assertEquals(1.8f, WarmTheme.iconStrokeWidth)
        assertEquals(IconStrokeCap.ROUND, WarmTheme.iconStrokeCap)
        assertEquals(ProgressStyle.SOFT, WarmTheme.progressStyle)
        assertEquals(ButtonStyle.SHADOW, WarmTheme.buttonStyle)
        assertEquals("Serif", WarmTheme.titleFontFamily)
    }
}
