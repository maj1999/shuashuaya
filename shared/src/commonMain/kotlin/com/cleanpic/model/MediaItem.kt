package com.cleanpic.model

data class MediaItem(
    val id: String,
    val type: MediaType,
    val name: String,
    val size: Long,
    val date: Long,
    val width: Int,
    val height: Int,
    val duration: Long? = null
)

enum class MediaType { PHOTO, VIDEO }
