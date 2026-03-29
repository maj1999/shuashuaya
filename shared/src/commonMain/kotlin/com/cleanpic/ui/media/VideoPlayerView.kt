package com.cleanpic.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cleanpic.model.MediaItem

/**
 * 跨平台视频播放组件。
 * Android 使用 Media3 ExoPlayer + PlayerView，
 * iOS 暂用文案占位。
 */
@Composable
expect fun VideoPlayerView(
    item: MediaItem,
    isMuted: Boolean,
    modifier: Modifier = Modifier
)
