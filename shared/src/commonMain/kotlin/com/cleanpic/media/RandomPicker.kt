package com.cleanpic.media

import com.cleanpic.model.MediaItem

object RandomPicker {
    fun pick(items: List<MediaItem>, count: Int, exclude: Set<String>): List<MediaItem> {
        if (items.isEmpty()) return emptyList()
        var available = items.filter { it.id !in exclude }
        if (available.isEmpty()) available = items
        return available.shuffled().take(count)
    }
}
