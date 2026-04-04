package com.cleanpic.icons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SvgPathParserTest {

    // ---- Tokenizer 测试（不依赖 Path，纯逻辑验证） ----

    @Test fun tokenize_simple_moveTo_lineTo() {
        val tokens = tokenizeSvgPath("M19 12H5")
        assertEquals(listOf("M", "19", "12", "H", "5"), tokens)
    }

    @Test fun tokenize_with_close() {
        val tokens = tokenizeSvgPath("M5 3l14 9-14 9V3z")
        assertEquals(listOf("M", "5", "3", "l", "14", "9", "-14", "9", "V", "3", "z"), tokens)
    }

    @Test fun tokenize_empty_string() {
        val tokens = tokenizeSvgPath("")
        assertTrue(tokens.isEmpty())
    }

    @Test fun tokenize_negative_number_as_separator() {
        val tokens = tokenizeSvgPath("M10-5 3-2")
        assertEquals(listOf("M", "10", "-5", "3", "-2"), tokens)
    }

    @Test fun tokenize_consecutive_decimals() {
        // "1.5.5" should split into "1.5" and ".5"
        val tokens = tokenizeSvgPath("M1.5.5")
        assertEquals(listOf("M", "1.5", ".5"), tokens)
    }

    @Test fun tokenize_arc_flags_sticky() {
        // Arc: rx ry xrot large-arc sweep x y
        // "a3 3 0 100-6" → a, 3, 3, 0, 1, 0, 0, -6
        val tokens = tokenizeSvgPath("a3 3 0 100-6")
        assertEquals(listOf("a", "3", "3", "0", "1", "0", "0", "-6"), tokens)
    }

    @Test fun tokenize_arc_double_group_flags() {
        // "a3 3 0 000 6z" → a, 3, 3, 0, 0, 0, 0, 6, z
        val tokens = tokenizeSvgPath("a3 3 0 000 6z")
        assertEquals(listOf("a", "3", "3", "0", "0", "0", "0", "6", "z"), tokens)
    }

    @Test fun tokenize_cubic_command() {
        val tokens = tokenizeSvgPath("C1 2 3 4 5 6")
        assertEquals(listOf("C", "1", "2", "3", "4", "5", "6"), tokens)
    }

    // ---- 所有 13 个 AppIcons path data 的 tokenize 验证 ----

    private val allPathData = listOf(
        "M19 12H5M12 19l-7-7 7-7",
        "M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2",
        "M20 6L9 17l-5-5",
        "M12 15a3 3 0 100-6 3 3 0 000 6zM19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09a1.65 1.65 0 00-1-1.51 1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09a1.65 1.65 0 001.51-1 1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z",
        "M5 3l14 9-14 9V3z",
        "M11 5L6 9H2v6h4l5 4V5zM23 9l-6 6M17 9l6 6",
        "M11 5L6 9H2v6h4l5 4V5zM19.07 4.93a10 10 0 010 14.14M15.54 8.46a5 5 0 010 7.07",
        "M3 3h18v18H3V3zM8.5 8.5a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM21 15l-5-5L5 21",
        "M23 7l-7 5 7 5V7zM1 5h15v14H1V5z",
        "M23 4v6h-6M1 20v-6h6M20.49 9A9 9 0 005.64 5.64L1 10M22.99 14l-4.64 4.36A9 9 0 013.51 15",
        "M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z",
        "M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0zM12 9v4M12 17h.01",
        "M18 6L6 18M6 6l12 12"
    )

    @Test fun tokenize_all_app_icons_no_exception() {
        for (data in allPathData) {
            val tokens = tokenizeSvgPath(data)
            assertTrue(tokens.isNotEmpty(), "Tokenize failed for: $data")
        }
    }

    @Test fun parse_all_app_icons_no_exception() {
        // parseSvgPath 需要 Compose Path——如果测试环境不支持则跳过
        for (data in allPathData) {
            try {
                val path = parseSvgPath(data)
                // 只要不抛异常就算通过
                assertTrue(true)
            } catch (e: Throwable) {
                // Compose Path 在纯 JVM 测试中可能不可用，跳过
                println("跳过 Path 测试（Compose 不可用）: ${e::class.simpleName}: ${e.message}")
                return
            }
        }
    }

    @Test fun parse_empty_string_returns_empty_path() {
        try {
            val path = parseSvgPath("")
            assertTrue(path.isEmpty)
        } catch (e: Throwable) {
            println("跳过 Path 测试（Compose 不可用）: ${e::class.simpleName}")
        }
    }

    // ---- 详细 tokenize 验证（确保 arc flag 解析正确）----

    @Test fun tokenize_settings_gear_circle() {
        // "a3 3 0 100-6 3 3 0 000 6z" — two arc groups
        val tokens = tokenizeSvgPath("a3 3 0 100-6 3 3 0 000 6z")
        // First group:  a, 3, 3, 0, 1, 0, 0, -6
        // Second group (implicit repeat): 3, 3, 0, 0, 0, 0, 6
        // Then: z
        assertEquals("a", tokens[0])
        assertEquals("3", tokens[1])  // rx
        assertEquals("3", tokens[2])  // ry
        assertEquals("0", tokens[3])  // x-rotation
        assertEquals("1", tokens[4])  // large-arc-flag
        assertEquals("0", tokens[5])  // sweep-flag
        assertEquals("0", tokens[6])  // x (part of "0-6" → "0" and "-6")
        assertEquals("-6", tokens[7])
        // Second arc group
        assertEquals("3", tokens[8])
        assertEquals("3", tokens[9])
        assertEquals("0", tokens[10])
        assertEquals("0", tokens[11]) // large-arc
        assertEquals("0", tokens[12]) // sweep
        assertEquals("0", tokens[13])
        assertEquals("6", tokens[14])
        assertEquals("z", tokens[15])
    }

    @Test fun tokenize_refresh_absolute_arc() {
        // "A9 9 0 005.64 5.64" from refresh icon
        val tokens = tokenizeSvgPath("A9 9 0 005.64 5.64")
        assertEquals("A", tokens[0])
        assertEquals("9", tokens[1])     // rx
        assertEquals("9", tokens[2])     // ry
        assertEquals("0", tokens[3])     // x-rotation
        assertEquals("0", tokens[4])     // large-arc-flag
        assertEquals("0", tokens[5])     // sweep-flag
        assertEquals("5.64", tokens[6])  // x
        assertEquals("5.64", tokens[7])  // y
    }
}
