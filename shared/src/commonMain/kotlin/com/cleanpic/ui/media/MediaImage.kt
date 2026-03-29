package com.cleanpic.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.cleanpic.model.MediaItem

/**
 * 跨平台媒体缩略图组件。
 * Android 使用 Coil AsyncImage 加载 content:// URI，
 * iOS 暂用 emoji 占位。
 */
@Composable
expect fun MediaImage(
    item: MediaItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
)
