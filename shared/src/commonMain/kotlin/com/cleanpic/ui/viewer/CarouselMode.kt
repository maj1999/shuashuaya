package com.cleanpic.ui.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
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
// 完成一次切换所需的滑动距离占容器宽度的比例（用于视觉过渡映射）
private const val CAROUSEL_FULL_SWIPE_RATIO = 0.55f
// 相邻槽位中心相对容器中心的水平间距占宽度的比例
private const val CAROUSEL_SLOT_SPACING_RATIO = 0.46f
// 侧边（相邻）卡片相对主卡的缩放与透明度
private const val CAROUSEL_SIDE_SCALE = 0.52f
private const val CAROUSEL_SIDE_ALPHA = 0.4f
// 松手后过渡到完成位 / 回弹位的动画时长（毫秒）
private const val CAROUSEL_TRANSITION_MS = 220
private const val CAROUSEL_SETTLE_MS = 200

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
    val scope = rememberCoroutineScope()
    // 拖动偏移；用 Animatable 以便松手时平滑过渡到完成位/回弹位
    val offsetX = remember { Animatable(0f) }
    var containerWidth by remember { mutableStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    val thresholdPx = with(LocalDensity.current) { CAROUSEL_SWIPE_THRESHOLD_DP.dp.toPx() }

    // 切换到下一项时复位偏移与播放状态
    LaunchedEffect(currentIndex) {
        offsetX.snapTo(0f)
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
                .onSizeChanged { containerWidth = it.width.toFloat() }
                .pointerInput(currentIndex, items.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val width = if (containerWidth > 0f) containerWidth else 1f
                            // 一次完整切换的目标偏移：当前卡滑出、相邻卡滑入主位
                            val full = width * CAROUSEL_FULL_SWIPE_RATIO
                            // 末张往后无下一张可补位：整卡直接平移滑出屏幕，目标取容器宽度多一点确保完全划出
                            val exitOff = width * 1.05f
                            val isLastItem = currentIndex == items.size - 1
                            val off = offsetX.value
                            when {
                                // 末张往后：整卡滑出屏幕 → 触发本轮完成进结果页（未决策默认保留）
                                off <= -thresholdPx && isLastItem -> scope.launch {
                                    offsetX.animateTo(-exitOff, tween(CAROUSEL_TRANSITION_MS))
                                    viewerViewModel.goNext()
                                }
                                // 向左滑：当前卡滑出、下一张过渡到主位（未决策默认保留）
                                off <= -thresholdPx -> scope.launch {
                                    offsetX.animateTo(-full, tween(CAROUSEL_TRANSITION_MS))
                                    viewerViewModel.goNext()
                                }
                                // 向右滑：当前卡滑出、上一张过渡到主位（保持原决策）
                                off >= thresholdPx && currentIndex > 0 -> scope.launch {
                                    offsetX.animateTo(full, tween(CAROUSEL_TRANSITION_MS))
                                    viewerViewModel.goPrevious()
                                }
                                // 未达阈值或已到首张：回弹复位
                                else -> scope.launch { offsetX.animateTo(0f, tween(CAROUSEL_SETTLE_MS)) }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, tween(CAROUSEL_SETTLE_MS)) }
                        }
                    ) { _, dragAmount ->
                        // 跟手 1:1 移动，避免主卡滞后于手指
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val safeWidth = if (containerWidth > 0f) containerWidth else 1f
            val fullSwipe = safeWidth * CAROUSEL_FULL_SWIPE_RATIO
            val slotSpacing = safeWidth * CAROUSEL_SLOT_SPACING_RATIO
            // 拖动进度：正=右滑（看上一张），负=左滑（看下一张），范围 [-1, 1]
            val drag = (offsetX.value / fullSwipe).coerceIn(-1f, 1f)
            // 末张：往后无下一张可补位，主卡随手指整卡平移滑出（不缩小到侧边）
            val isLastItem = currentIndex == items.size - 1

            // 渲染 前一张/当前/后一张 三张卡片，按各自的"逻辑槽位"随手指连续过渡。
            // visualSlot = baseSlot + drag：左滑(drag→-1)时后一张(+1)滑到中心(0)，当前卡(0)退到左侧(-1)。
            // 越靠近中心(|v|越小)越后绘制，保证过渡中目标卡逐渐压到顶层。
            listOf(-1, 0, 1)
                .filter { currentIndex + it in items.indices }
                .map { base -> base to (base + drag) }
                .sortedByDescending { kotlin.math.abs(it.second) }
                .forEach { (base, v) ->
                    val focused = base == 0
                    val av = kotlin.math.abs(v).coerceIn(0f, 1f)
                    val scale = lerp(1f, CAROUSEL_SIDE_SCALE, av)
                    val cardAlpha = if (kotlin.math.abs(v) <= 1f) {
                        lerp(1f, CAROUSEL_SIDE_ALPHA, av)
                    } else {
                        lerp(CAROUSEL_SIDE_ALPHA, 0f, (kotlin.math.abs(v) - 1f).coerceIn(0f, 1f))
                    }

                    // 末张主卡往后滑（offsetX<0）时：整卡 1:1 跟手平移滑出，保持原尺寸不缩小
                    val slideOff = focused && isLastItem
                    var cardModifier = Modifier
                        .fillMaxHeight(0.85f)
                        .fillMaxWidth(0.78f)
                        .graphicsLayer {
                            if (slideOff && offsetX.value < 0f) {
                                translationX = offsetX.value
                                scaleX = 1f
                                scaleY = 1f
                                alpha = 1f
                            } else {
                                translationX = v * slotSpacing
                                scaleX = scale
                                scaleY = scale
                                alpha = cardAlpha
                            }
                        }
                    if (focused) {
                        cardModifier = cardModifier
                            .testTag("media_card")
                            .pointerInput(currentIndex) {
                                detectTapGestures(onTap = { onMediaClick() })
                            }
                    }

                    CarouselCard(
                        item = items[currentIndex + base],
                        theme = theme,
                        focused = focused,
                        isPlaying = focused && isPlaying,
                        isMuted = isMuted,
                        onPlayClick = { isPlaying = true },
                        onToggleMute = { isMuted = !isMuted },
                        modifier = cardModifier
                    )
                }
        }

        // 操作按钮
        ActionButtons(theme, viewerViewModel, canUndo)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 轮播卡片：三张共用同一组件，仅靠外部 graphicsLayer 的位移/缩放/透明度区分主次，
 * 从而实现"相邻媒体随手指连续过渡到主位"的效果。
 * 仅 [focused]（中心主卡）显示播放按钮、文件信息与静音控制。
 */
@Composable
private fun CarouselCard(
    item: ViewerItem,
    theme: ThemeTokens,
    focused: Boolean,
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
        if (focused && isVideo && isPlaying) {
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
        if (isVideo && !(focused && isPlaying) && item.media.duration != null) {
            DurationBadge(
                durationMs = item.media.duration,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            )
        }
        // 播放图标（中心视频未播放时显示）
        if (focused && isVideo && !isPlaying) {
            PlayButtonOverlay(
                onClick = onPlayClick,
                theme = theme,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // 文件信息叠层（仅中心主卡）
        if (focused) {
            FileInfoOverlay(
                item = item,
                theme = theme,
                isMuted = if (isVideo && isPlaying) isMuted else null,
                onToggleMute = onToggleMute,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
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
