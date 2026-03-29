package com.cleanpic.ui.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.model.MediaType
import com.cleanpic.model.ViewerItem
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.viewmodel.ViewerViewModel

@Composable
fun CarouselMode(theme: ThemeTokens, viewerViewModel: ViewerViewModel) {
    val items by viewerViewModel.items.collectAsState()
    val currentIndex by viewerViewModel.currentIndex.collectAsState()
    if (items.isEmpty() || currentIndex >= items.size) return

    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX)

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
                .pointerInput(currentIndex) {
                    detectHorizontalDragGestures(
                        onDragEnd = { offsetX = 0f },
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
            // 当前主卡片
            MainCard(
                item = items[currentIndex],
                theme = theme,
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .fillMaxWidth(0.75f)
                    .graphicsLayer { translationX = animatedOffset }
            )
        }

        // 操作按钮
        ActionButtons(theme, viewerViewModel)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MainCard(item: ViewerItem, theme: ThemeTokens, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .background(Color(theme.colorSurface))
    ) {
        // 占位内容（后续替换为真实图片加载）
        Box(
            modifier = Modifier.fillMaxSize().background(Color(theme.colorSurface)),
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
        // 文件信息叠层
        FileInfoOverlay(
            item = item,
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
        Text(
            text = if (item.media.type == MediaType.PHOTO) "\ud83d\uddbc\ufe0f" else "\ud83c\udfac",
            fontSize = 24.sp
        )
    }
}

@Composable
private fun ActionButtons(theme: ThemeTokens, viewerViewModel: ViewerViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { viewerViewModel.markDelete() },
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(theme.colorDanger)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(text = "\ud83d\uddd1\ufe0f", fontSize = 24.sp)
        }
        Button(
            onClick = { viewerViewModel.markKept() },
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(theme.colorSuccess)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(text = "\u2713", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
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
internal fun FileInfoOverlay(item: ViewerItem, modifier: Modifier = Modifier) {
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
        Row {
            Text(
                text = formatBytes(item.media.size),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${item.media.width}\u00d7${item.media.height}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024 -> "${(bytes * 10 / 1024).toDouble() / 10.0} KB"
    bytes < 1024L * 1024 * 1024 -> "${(bytes * 10 / (1024L * 1024)).toDouble() / 10.0} MB"
    else -> "${(bytes * 100 / (1024L * 1024 * 1024)).toDouble() / 100.0} GB"
}
