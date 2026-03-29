package com.cleanpic.ui.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType

@Composable
actual fun MediaImage(
    item: MediaItem,
    modifier: Modifier,
    contentScale: ContentScale
) {
    // TODO: 通过 PHImageManager 加载真实缩略图
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (item.type == MediaType.PHOTO) "🖼️" else "🎬",
            fontSize = 48.sp
        )
    }
}
