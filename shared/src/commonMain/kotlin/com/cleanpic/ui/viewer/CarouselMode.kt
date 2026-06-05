package com.cleanpic.ui.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.MediaType
import androidx.compose.ui.platform.testTag
import com.cleanpic.model.ViewerItem
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.media.MediaImage
import com.cleanpic.ui.media.VideoPlayerView
import com.cleanpic.viewmodel.ViewerViewModel

// 左右滑动切换前后媒体的触发阈值
private const val CAROUSEL_SWIPE_THRESHOLD_DP = 72

@Composable
fun CarouselMode(
    theme: ThemeTokens,
    viewerViewModel: ViewerViewModel,
    onMediaClick: () -> Unit = {}
) {
    val items by viewerViewModel.items.collectAsState()
    val currentIndex by viewerViewModel.currentIndex.collectAsState()
    if (items.isEmpty() || currentIndex >= items.size) return

    val canUndo by viewerViewModel.canUndo.collectAsState()
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX)
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    val thresholdPx = with(LocalDensity.current) { CAROUSEL_SWIPE_THRESHOLD_DP.dp.toPx() }

    // 切换到下一项时重置偏移与播放状态
    LaunchedEffect(currentIndex) {
        offsetX = 0f
        isPlaying = false
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 轮播区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(currentIndex, items.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                // 向左滑：切到下一个媒体（未决策默认保留，已决策保持原样）
                                offsetX <= -thresholdPx -> viewerViewModel.goNext()
                                // 向右滑：切到上一个媒体，复查时保持各自原有决策
                                offsetX >= thresholdPx && currentIndex > 0 -> viewerViewModel.goPrevious()
                                // 未达阈值或已到首张：回弹复位
                                else -> offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f }
                    ) { _, dragAmount -> offsetX += dragAmount }
                },
            contentAlignment = Alignment.Center
        ) {
            // 左侧预览
            if (currentIndex > 0) {
                PreviewCard(
                    item = items[currentIndex - 1],
                    theme = theme,
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .fillMaxWidth(0.3f)
                        .align(Alignment.CenterStart)
                        .alpha(0.4f)
                        .graphicsLayer {
                            scaleX = 0.85f; scaleY = 0.85f
                            translationX = animatedOffset * 0.3f
                        }
                )
            }
            // 右侧预览
            if (currentIndex < items.size - 1) {
                PreviewCard(
                    item = items[currentIndex + 1],
                    theme = theme,
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .fillMaxWidth(0.3f)
                        .align(Alignment.CenterEnd)
                        .alpha(0.4f)
                        .graphicsLayer {
                            scaleX = 0.85f; scaleY = 0.85f
                            translationX = animatedOffset * 0.3f
                        }
                )
            }
            // 当前主卡片（点击进入全屏查看）
            MainCard(
                item = items[currentIndex],
                theme = theme,
                isPlaying = isPlaying,
                isMuted = isMuted,
                onPlayClick = { isPlaying = true },
                onToggleMute = { isMuted = !isMuted },
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .fillMaxWidth(0.75f)
                    .graphicsLayer { translationX = animatedOffset }
                    .testTag("media_card")
                    .clickable { onMediaClick() }
            )
        }

        // 操作按钮
        ActionButtons(theme, viewerViewModel, canUndo)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MainCard(
    item: ViewerItem,
    theme: ThemeTokens,
    isPlaying: Boolean,
    isMuted: Boolean,
    onPlayClick: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier
) {
    val isVideo = item.media.type == MediaType.VIDEO

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .background(Color(theme.colorSurface))
    ) {
        if (isVideo && isPlaying) {
            VideoPlayerView(
                item = item.media,
                isMuted = isMuted,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MediaImage(
                item = item.media,
                modifier = Modifier.fillMaxSize()
            )
        }
        // 视频时长角标（未播放时显示）
        if (isVideo && !isPlaying && item.media.duration != null) {
            DurationBadge(
                durationMs = item.media.duration,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            )
        }
        // 播放图标（视频未播放时显示）
        if (isVideo && !isPlaying) {
            PlayButtonOverlay(
                onClick = onPlayClick,
                theme = theme,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // 文件信息叠层
        FileInfoOverlay(
            item = item,
            theme = theme,
            isMuted = if (isVideo && isPlaying) isMuted else null,
            onToggleMute = onToggleMute,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PreviewCard(item: ViewerItem, theme: ThemeTokens, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .background(Color(theme.colorSurface)),
        contentAlignment = Alignment.Center
    ) {
        MediaImage(
            item = item.media,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ActionButtons(theme: ThemeTokens, viewerViewModel: ViewerViewModel, canUndo: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemedActionButton(
            iconName = "undo",
            color = theme.colorTextSecondary,
            theme = theme,
            onClick = { viewerViewModel.undo() },
            size = 48.dp,
            testTag = "undo_button",
            enabled = canUndo
        )
        ThemedActionButton(
            iconName = "delete",
            color = theme.colorDanger,
            theme = theme,
            onClick = { viewerViewModel.markDelete() },
            size = 64.dp,
            testTag = "delete_button"
        )
        ThemedActionButton(
            iconName = "keep",
            color = theme.colorSuccess,
            theme = theme,
            onClick = { viewerViewModel.markKept() },
            size = 64.dp,
            testTag = "keep_button"
        )
    }
}

@Composable
internal fun DurationBadge(durationMs: Long, modifier: Modifier = Modifier) {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / 1000 / 60) % 60
    val text = "$minutes:${seconds.toString().padStart(2, '0')}"
    Text(
        text = text,
        fontSize = 12.sp,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
internal fun PlayButtonOverlay(onClick: () -> Unit, theme: ThemeTokens, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .testTag("play_button")
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        IconPainter("play", theme, size = 24.dp, colorOverride = 0xFFFFFFFF)
    }
}

@Composable
internal fun FileInfoOverlay(
    item: ViewerItem,
    theme: ThemeTokens,
    modifier: Modifier = Modifier,
    isMuted: Boolean? = null,
    onToggleMute: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = item.media.name,
            fontSize = 13.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatBytes(item.media.size),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${item.media.width}×${item.media.height}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            if (isMuted != null && onToggleMute != null) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onToggleMute,
                    modifier = Modifier.testTag("mute_button")
                ) {
                    IconPainter(if (isMuted) "mute" else "unmute", theme, size = 18.dp)
                }
            }
        }
    }
}

internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024 -> "${(bytes * 10 / 1024).toDouble() / 10.0} KB"
    bytes < 1024L * 1024 * 1024 -> "${(bytes * 10 / (1024L * 1024)).toDouble() / 10.0} MB"
    else -> "${(bytes * 100 / (1024L * 1024 * 1024)).toDouble() / 100.0} GB"
}
