package com.cleanpic.model

enum class InteractionMode(val id: String, val label: String) {
    CAROUSEL("carousel", "轮播相册式"),
    SWIPE_CARD("swipe-card", "卡片左右滑"),
    FULLSCREEN("fullscreen", "全屏上下滑");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: CAROUSEL
    }
}
