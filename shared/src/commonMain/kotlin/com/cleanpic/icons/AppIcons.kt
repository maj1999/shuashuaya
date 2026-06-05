package com.cleanpic.icons

import com.cleanpic.theme.IconStrokeCap
import com.cleanpic.theme.ThemeTokens

/**
 * 主题化矢量图标描述（平台无关数据）。
 * Compose 层通过 toImageVector() 扩展转为 ImageVector 或直接用 Canvas 绘制。
 */
data class IconDef(
    val name: String,
    val pathData: String,
    val viewportWidth: Float = 24f,
    val viewportHeight: Float = 24f,
    val strokeWidth: Float,
    val strokeColor: Long,
    val strokeCap: IconStrokeCap
)

/**
 * 统一图标入口 — 根据名称和主题返回 IconDef。
 */
object AppIcons {

    private val paths = mapOf(
        "back" to "M19 12H5M12 19l-7-7 7-7",
        "delete" to "M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2",
        "keep" to "M20 6L9 17l-5-5",
        "settings" to "M12 15a3 3 0 100-6 3 3 0 000 6zM19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09a1.65 1.65 0 00-1-1.51 1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09a1.65 1.65 0 001.51-1 1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z",
        "play" to "M5 3l14 9-14 9V3z",
        "mute" to "M11 5L6 9H2v6h4l5 4V5zM23 9l-6 6M17 9l6 6",
        "unmute" to "M11 5L6 9H2v6h4l5 4V5zM19.07 4.93a10 10 0 010 14.14M15.54 8.46a5 5 0 010 7.07",
        "photo" to "M3 3h18v18H3V3zM8.5 8.5a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM21 15l-5-5L5 21",
        "video" to "M23 7l-7 5 7 5V7zM1 5h15v14H1V5z",
        "refresh" to "M23 4v6h-6M1 20v-6h6M20.49 9A9 9 0 005.64 5.64L1 10M22.99 14l-4.64 4.36A9 9 0 013.51 15",
        "home" to "M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z",
        "warning" to "M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0zM12 9v4M12 17h.01",
        "close" to "M18 6L6 18M6 6l12 12",
        "undo" to "M1 4v6h6M3.51 15a9 9 0 102.13-9.36L1 10",
        "fullscreen" to "M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"
    )

    fun get(name: String, theme: ThemeTokens): IconDef {
        val pathData = paths[name]
            ?: throw IllegalArgumentException("未知图标：$name")
        return IconDef(
            name = name,
            pathData = pathData,
            strokeWidth = theme.iconStrokeWidth,
            strokeColor = theme.iconStrokeColor,
            strokeCap = theme.iconStrokeCap
        )
    }

    /** 所有可用的图标名称 */
    val allNames: Set<String> get() = paths.keys
}
