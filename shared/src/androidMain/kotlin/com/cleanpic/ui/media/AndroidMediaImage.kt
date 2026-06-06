package com.cleanpic.ui.media

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val context = LocalPlatformContext.current
    // ImageRequest 未实现 equals，Coil 以引用判等；若每次重组都新建实例，
    // AsyncImage 会误判 model 变化而重新加载、闪一下占位色。用 remember 按媒体身份缓存请求，
    // 避免拖动等重组场景下的"闪黑"。
    val request = remember(item.id, item.type, context) {
        val uri = when (item.type) {
            MediaType.PHOTO -> ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id.toLong()
            )
            MediaType.VIDEO -> ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id.toLong()
            )
        }
        ImageRequest.Builder(context)
            .data(uri)
            .crossfade(true)
            .decoderFactory(VideoFrameDecoder.Factory())
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = item.name,
        contentScale = contentScale,
        modifier = modifier,
        placeholder = ColorPainter(Color(0xFF2A2A2A)),
        error = ColorPainter(Color(0xFF3A3A3A))
    )
}
