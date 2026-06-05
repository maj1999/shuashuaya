package com.cleanpic.ui.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.model.MediaType
import com.cleanpic.model.ViewerItem
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.media.MediaImage
import com.cleanpic.ui.media.VideoPlayerView
import com.cleanpic.viewmodel.ViewerViewModel
import kotlinx.coroutines.launch

private const val SWIPE_THRESHOLD_DP = 150
private const val DISMISS_TARGET = 1000f

@Composable
fun SwipeCardMode(
    theme: ThemeTokens,
    viewerViewModel: ViewerViewModel,
    onMediaClick: () -> Unit = {}
) {
    val items by viewerViewModel.items.collectAsState()
    val currentIndex by viewerViewModel.currentIndex.collectAsState()
    if (items.isEmpty() || currentIndex >= items.size) return

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }

    val canUndo by viewerViewModel.canUndo.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }

    // 每次 index 变化时重置偏移与播放状态
    LaunchedEffect(currentIndex) {
        offsetX.snapTo(0f)
        isPlaying = false
    }

    fun onSwipeComplete(toLeft: Boolean) {
        scope.launch {
            val target = if (toLeft) -DISMISS_TARGET else DISMISS_TARGET
            offsetX.animateTo(target, tween(200))
            if (toLeft) viewerViewModel.markDelete() else viewerViewModel.markKept()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 提示文案 + 撤销按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedActionButton(
                iconName = "undo",
                color = theme.colorTextSecondary,
                theme = theme,
                onClick = { viewerViewModel.undo() },
                size = 40.dp,
                testTag = "undo_button",
                enabled = canUndo
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "← 左滑删除 · 右滑保留 →",
                fontSize = 13.sp,
                color = Color(theme.colorTextSecondary)
            )
        }

        // 卡片堆叠区
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // 背景卡片
            if (currentIndex + 1 < items.size) {
                CardContent(
                    item = items[currentIndex + 1],
                    theme = theme,
                    modifier = Modifier
                        .fillMaxHeight(0.8f)
                        .fillMaxWidth(0.85f)
                        .graphicsLayer { scaleX = 0.92f; scaleY = 0.92f }
                        .alpha(0.5f)
                )
            }

            // 滑动方向指示
            val progress = (offsetX.value / thresholdPx).coerceIn(-1f, 1f)
            if (progress < -0.2f) {
                SwipeIndicator(
                    text = "删除",
                    color = Color(theme.colorDanger),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
                )
            }
            if (progress > 0.2f) {
                SwipeIndicator(
                    text = "保留",
                    color = Color(theme.colorSuccess),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                )
            }

            // 前景可拖拽卡片
            val rotation = offsetX.value * 0.05f
            val cardAlpha = 1f - (kotlin.math.abs(offsetX.value) / thresholdPx * 0.5f)
                .coerceIn(0f, 0.5f)

            CardContent(
                item = items[currentIndex],
                theme = theme,
                isPlaying = isPlaying,
                isMuted = isMuted,
                onPlayClick = { isPlaying = true },
                onToggleMute = { isMuted = !isMuted },
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth(0.85f)
                    .testTag("media_card")
                    .graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = rotation
                        alpha = cardAlpha
                    }
                    .pointerInput(currentIndex) {
                        detectTapGestures(onTap = { onMediaClick() })
                    }
                    .pointerInput(currentIndex) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX.value < -thresholdPx) {
                                    onSwipeComplete(true)
                                } else if (offsetX.value > thresholdPx) {
                                    onSwipeComplete(false)
                                } else {
                                    scope.launch { offsetX.animateTo(0f, tween(200)) }
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f, tween(200)) }
                            }
                        ) { _, dragAmount ->
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        }
                    }
            )
        }
    }
}

@Composable
private fun CardContent(
    item: ViewerItem,
    theme: ThemeTokens,
    modifier: Modifier,
    isPlaying: Boolean = false,
    isMuted: Boolean = true,
    onPlayClick: (() -> Unit)? = null,
    onToggleMute: (() -> Unit)? = null
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
        if (isVideo && !isPlaying && onPlayClick != null) {
            PlayButtonOverlay(
                onClick = onPlayClick,
                theme = theme,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // 文件信息
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
private fun SwipeIndicator(text: String, color: Color, modifier: Modifier) {
    Text(
        text = text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}
