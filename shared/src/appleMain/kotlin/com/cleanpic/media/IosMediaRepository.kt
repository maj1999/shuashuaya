package com.cleanpic.media

import com.cleanpic.model.MediaItem

class IosMediaRepository : MediaRepository {
    // TODO: 通过 Kotlin/Native interop 使用 PHAsset / PHImageManager 实现

    override suspend fun queryPhotos(): List<MediaItem> = emptyList()

    override suspend fun queryVideos(): List<MediaItem> = emptyList()

    override suspend fun getThumbnail(id: String): ByteArray? = null

    override suspend fun getFullImage(id: String): ByteArray? = null

    override suspend fun deleteMedia(ids: List<String>): Result<Int> = Result.success(0)
}
