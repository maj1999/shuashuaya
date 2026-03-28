package com.cleanpic.media

import com.cleanpic.model.MediaItem

class HarmonyMediaRepository : MediaRepository {
    // TODO: 通过 OHOS interop 使用 photoAccessHelper 实现

    override suspend fun queryPhotos(): List<MediaItem> = emptyList()

    override suspend fun queryVideos(): List<MediaItem> = emptyList()

    override suspend fun getThumbnail(id: String): ByteArray? = null

    override suspend fun getFullImage(id: String): ByteArray? = null

    override suspend fun deleteMedia(ids: List<String>): Result<Int> = Result.success(0)
}
