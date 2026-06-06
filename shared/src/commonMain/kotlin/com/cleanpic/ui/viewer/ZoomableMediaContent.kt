package com.cleanpic.ui.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import com.cleanpic.media.VideoControl
import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType
import com.cleanpic.ui.media.MediaImage
import com.cleanpic.ui.media.VideoPlayerView
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * 缩放/播放内核（无状态）—— 照片双击 + 双指捏合缩放；视频自动播放。
 *
 * 抽取自 FullscreenViewer，供浏览页全屏与结果页待删除预览（DeletePreviewOverlay）共用。
 * 嵌入 HorizontalPager 时：1× 横滑由 zoomable 默认 ScrollGesturePropagation 传播给 Pager 翻页，
 * 放大后横向拖动为平移。
 */
@Composable
fun ZoomableMediaContent(
    media: MediaItem,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    control: VideoControl? = null,
) {
    val zoomState = rememberZoomState(
        contentSize = Size(media.width.toFloat(), media.height.toFloat()),
        maxScale = 5f
    )
    // 切换媒体时自动复位到 1×
    LaunchedEffect(media.id) { zoomState.reset() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .zoomable(zoomState, enableOneFingerZoom = false),
        contentAlignment = Alignment.Center
    ) {
        if (media.type == MediaType.VIDEO) {
            VideoPlayerView(
                item = media,
                isMuted = isMuted,
                modifier = Modifier.fillMaxSize(),
                control = control
            )
        } else {
            MediaImage(
                item = media,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
