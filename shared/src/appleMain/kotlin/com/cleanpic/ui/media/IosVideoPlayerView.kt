package com.cleanpic.ui.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.cleanpic.model.MediaItem

@Composable
actual fun VideoPlayerView(
    item: MediaItem,
    isMuted: Boolean,
    modifier: Modifier
) {
    // TODO: 通过 Kotlin/Native interop 使用 AVPlayer 实现
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎬 视频播放待实现",
            fontSize = 16.sp,
            color = Color.White
        )
    }
}
