package com.cleanpic.ui.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.model.MediaType
import com.cleanpic.model.ViewerItem
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.viewmodel.ViewerViewModel
import kotlinx.coroutines.launch

private const val SWIPE_THRESHOLD_DP = 150
private const val DISMISS_TARGET = 1000f

@Composable
fun SwipeCardMode(theme: ThemeTokens, viewerViewModel: ViewerViewModel) {
    val items by viewerViewModel.items.collectAsState()
    val currentIndex by viewerViewModel.currentIndex.collectAsState()
    if (items.isEmpty() || currentIndex >= items.size) return

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }

    // 每次 index 变化时重置偏移
    LaunchedEffect(currentIndex) { offsetX.snapTo(0f) }

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
        // 提示文案
        Text(
            text = "\u2190 \u5de6\u6ed1\u5220\u9664 \u00b7 \u53f3\u6ed1\u4fdd\u7559 \u2192",
            fontSize = 13.sp,
            color = Color(theme.colorTextSecondary),
            modifier = Modifier.padding(top = 8.dp)
        )

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
                    text = "\u5220\u9664",
                    color = Color(theme.colorDanger),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
                )
            }
            if (progress > 0.2f) {
                SwipeIndicator(
                    text = "\u4fdd\u7559",
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
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth(0.85f)
                    .graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = rotation
                        alpha = cardAlpha
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
private fun CardContent(item: ViewerItem, theme: ThemeTokens, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .background(Color(theme.colorSurface))
    ) {
        // 占位内容
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (item.media.type == MediaType.PHOTO) "\ud83d\uddbc\ufe0f" else "\ud83c\udfac",
                fontSize = 48.sp
            )
        }
        // 视频时长角标
        if (item.media.type == MediaType.VIDEO && item.media.duration != null) {
            DurationBadge(
                durationMs = item.media.duration,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            )
        }
        // 文件信息
        FileInfoOverlay(
            item = item,
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
