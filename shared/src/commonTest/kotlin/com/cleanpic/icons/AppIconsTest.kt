package com.cleanpic.icons

import com.cleanpic.theme.IconStrokeCap
import com.cleanpic.theme.WarmTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppIconsTest {
    @Test fun all_icons_defined() {
        val iconNames = listOf(
            "back", "delete", "keep", "settings", "play",
            "mute", "unmute", "photo", "video", "refresh",
            "home", "warning", "close"
        )
        for (name in iconNames) {
            val icon = AppIcons.get(name, WarmTheme)
            assertTrue(icon.pathData.isNotEmpty(), "图标 '$name' 没有 path 数据")
        }
    }

    @Test fun icon_params_follow_theme() {
        val icon = AppIcons.get("delete", WarmTheme)
        assertEquals(WarmTheme.iconStrokeWidth, icon.strokeWidth)
        assertEquals(WarmTheme.iconStrokeColor, icon.strokeColor)
        assertEquals(WarmTheme.iconStrokeCap, icon.strokeCap)
    }

    @Test fun unknown_icon_name_throws() {
        try {
            AppIcons.get("nonexistent", WarmTheme)
            assertTrue(false, "应当抛出异常")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("nonexistent"))
        }
    }
}
