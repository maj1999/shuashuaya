package com.cleanpic.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidMediaRepository(private val context: Context) : MediaRepository {

    override suspend fun queryPhotos(): List<MediaItem> = withContext(Dispatchers.IO) {
        queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.PHOTO)
    }

    override suspend fun queryVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO)
    }

    override suspend fun getThumbnail(id: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toLong()
            )
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getFullImage(id: String): ByteArray? = getThumbnail(id)

    override suspend fun deleteMedia(ids: List<String>): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val uris = ids.map {
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.toLong()
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // API 30+: createDeleteRequest 返回 PendingIntent，需要在 Activity 层处理
                    Result.success(ids.size)
                } else {
                    var deleted = 0
                    uris.forEach { uri ->
                        deleted += context.contentResolver.delete(uri, null, null)
                    }
                    Result.success(deleted)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun queryMedia(uri: Uri, type: MediaType): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(uri, projection, null, null, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol).toString()
                    val duration = if (type == MediaType.VIDEO) {
                        try {
                            val durationCol = cursor.getColumnIndex(
                                MediaStore.Video.VideoColumns.DURATION
                            )
                            if (durationCol >= 0) cursor.getLong(durationCol) else null
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                    items.add(
                        MediaItem(
                            id = id,
                            type = type,
                            name = cursor.getString(nameCol) ?: "unknown",
                            size = cursor.getLong(sizeCol),
                            date = cursor.getLong(dateCol) * 1000,
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            duration = duration
                        )
                    )
                }
            }
        return items
    }
}
