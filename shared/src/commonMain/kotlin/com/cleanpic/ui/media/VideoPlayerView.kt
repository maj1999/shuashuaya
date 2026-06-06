package com.cleanpic.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cleanpic.media.VideoControl
import com.cleanpic.model.MediaItem

/**
 * 跨平台视频播放组件。
 * Android 使用 Media3 ExoPlayer + PlayerView，
 * iOS 暂用文案占位。
 *
 * @param control 传入非 null 时，平台实现轮询上报播放进度并接管 seek，供外部进度条使用；
 *                为 null 时不做轮询（轮播小卡等无需进度条的场景），零开销。
 */
@Composable
expect fun VideoPlayerView(
    item: MediaItem,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    control: VideoControl? = null
)
