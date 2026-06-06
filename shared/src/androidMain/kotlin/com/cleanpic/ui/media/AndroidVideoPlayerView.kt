package com.cleanpic.ui.media

import android.content.ContentUris
import android.provider.MediaStore
import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cleanpic.media.VideoControl
import com.cleanpic.model.MediaItem
import com.cleanpic.shared.R
import kotlinx.coroutines.delay

@Composable
actual fun VideoPlayerView(
    item: MediaItem,
    isMuted: Boolean,
    modifier: Modifier,
    control: VideoControl?
) {
    val context = LocalContext.current

    val exoPlayer = remember(item.id) {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id.toLong()
        )
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(contentUri))
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            prepare()
            play()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // 传入 control 时接管 seek 并轮询上报播放进度，供外部进度条使用；不传则无开销。
    if (control != null) {
        LaunchedEffect(exoPlayer) {
            control.seek = { positionMs -> exoPlayer.seekTo(positionMs.coerceAtLeast(0L)) }
            while (true) {
                val dur = exoPlayer.duration
                control.update(
                    position = exoPlayer.currentPosition.coerceAtLeast(0L),
                    // duration 未就绪时 ExoPlayer 返回 C.TIME_UNSET(负值)，归一为 0
                    duration = if (dur > 0L) dur else 0L
                )
                delay(250)
            }
        }
    }

    DisposableEffect(item.id) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            // 从 XML inflate，使用 TextureView surface（见 cleanpic_video_player.xml），
            // 以便全屏缩放时视频画面随变换平滑渲染
            val playerView = LayoutInflater.from(ctx)
                .inflate(R.layout.cleanpic_video_player, null) as PlayerView
            playerView.player = exoPlayer
            playerView
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
        modifier = modifier
    )
}
