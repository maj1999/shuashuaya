package com.cleanpic.ui.media

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType

@Composable
actual fun MediaImage(
    item: MediaItem,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val uri = when (item.type) {
        MediaType.PHOTO -> ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id.toLong()
        )
        MediaType.VIDEO -> ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id.toLong()
        )
    }

    val context = LocalPlatformContext.current
    val request = ImageRequest.Builder(context)
        .data(uri)
        .crossfade(true)
        .decoderFactory(VideoFrameDecoder.Factory())
        .build()

    AsyncImage(
        model = request,
        contentDescription = item.name,
        contentScale = contentScale,
        modifier = modifier,
        placeholder = ColorPainter(Color(0xFF2A2A2A)),
        error = ColorPainter(Color(0xFF3A3A3A))
    )
}
