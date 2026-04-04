package com.cleanpic.mock

import com.cleanpic.media.MediaRepository
import com.cleanpic.model.MediaItem

class MockMediaRepository(
    private val photos: List<MediaItem> = emptyList(),
    private val videos: List<MediaItem> = emptyList()
) : MediaRepository {
    val deletedIds = mutableListOf<String>()
    val deletedItems = mutableListOf<MediaItem>()
    var shouldFail = false
    var failureMessage = "mock deletion failed"

    override suspend fun queryPhotos(): List<MediaItem> = photos
    override suspend fun queryVideos(): List<MediaItem> = videos
    override suspend fun getThumbnail(id: String): ByteArray? = ByteArray(100)
    override suspend fun getFullImage(id: String): ByteArray? = ByteArray(1000)
    override suspend fun deleteMedia(ids: List<String>): Result<Int> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        deletedIds.addAll(ids)
        return Result.success(ids.size)
    }
    override suspend fun deleteMediaItems(items: List<MediaItem>): Result<Int> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        deletedItems.addAll(items)
        deletedIds.addAll(items.map { it.id })
        return Result.success(items.size)
    }
}
