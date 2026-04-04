package com.cleanpic.media

import com.cleanpic.model.MediaItem

interface MediaRepository {
    suspend fun queryPhotos(): List<MediaItem>
    suspend fun queryVideos(): List<MediaItem>
    suspend fun getThumbnail(id: String): ByteArray?
    suspend fun getFullImage(id: String): ByteArray?
    suspend fun deleteMedia(ids: List<String>): Result<Int>
    suspend fun deleteMediaItems(items: List<MediaItem>): Result<Int> =
        deleteMedia(items.map { it.id })
}
