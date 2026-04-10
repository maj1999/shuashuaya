package com.cleanpic.ui.media

import android.content.ContentUris
import android.provider.MediaStore
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
import com.cleanpic.model.MediaItem

@Composable
actual fun VideoPlayerView(
    item: MediaItem,
    isMuted: Boolean,
    modifier: Modifier
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

    DisposableEffect(item.id) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
        modifier = modifier
    )
}
