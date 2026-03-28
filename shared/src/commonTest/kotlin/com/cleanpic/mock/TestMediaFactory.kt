package com.cleanpic.mock

import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType

object TestMediaFactory {
    fun photos(n: Int) = (1..n).map {
        MediaItem(
            id = "photo_$it",
            type = MediaType.PHOTO,
            name = "IMG_$it.jpg",
            size = (it * 1024).toLong(),
            date = 1700000000000L + it * 86400000L,
            width = 4032,
            height = 3024
        )
    }

    fun videos(n: Int) = (1..n).map {
        MediaItem(
            id = "video_$it",
            type = MediaType.VIDEO,
            name = "VID_$it.mp4",
            size = (it * 10240).toLong(),
            date = 1700000000000L + it * 86400000L,
            width = 1920,
            height = 1080,
            duration = (it * 5000).toLong()
        )
    }
}
