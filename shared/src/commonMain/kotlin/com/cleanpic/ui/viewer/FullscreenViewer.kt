package com.cleanpic.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.MediaType
import com.cleanpic.model.ViewerItem
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.media.MediaImage
import com.cleanpic.ui.media.VideoPlayerView

/**
 * 无状态全屏展示组件 — 三种交互模式复用。
 * - 全屏上下滑交互模式：由 [FullscreenMode] 薄封装，onBack = 退出浏览。
 * - 轮播/卡片模式：点击媒体时以叠加层弹出，onBack = 关闭叠加层。
 *
 * 照片全屏铺满展示，视频直接播放；提供删除/保留/撤销/返回操作。
 */
@Composable
fun FullscreenViewer(
    item: ViewerItem,
    theme: ThemeTokens,
    current: Int,
    total: Int,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    onBack: () -> Unit,
    backLabel: String = "退出"
) {
    var isMuted by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fullscreen_viewer")
            .background(Color.Black)
    ) {
        FullscreenContent(item = item, isMuted = isMuted)

        TopBar(
            theme = theme,
            current = current,
            total = total,
            backLabel = backLabel,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        SideActions(
            theme = theme,
            canUndo = canUndo,
            onUndo = onUndo,
            onDelete = onDelete,
            onKeep = onKeep,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        BottomInfo(
            item = item,
            theme = theme,
            isMuted = isMuted,
            onToggleMute = { isMuted = !isMuted },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FullscreenContent(item: ViewerItem, isMuted: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (item.media.type == MediaType.VIDEO) {
            VideoPlayerView(
                item = item.media,
                isMuted = isMuted,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MediaImage(
                item = item.media,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun TopBar(
    theme: ThemeTokens,
    current: Int,
    total: Int,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .padding(top = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.testTag("exit_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconPainter("back", theme, size = 15.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = backLabel,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$current / $total",
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SideActions(
    theme: ThemeTokens,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ThemedActionButton(
            iconName = "undo",
            color = theme.colorTextSecondary,
            theme = theme,
            onClick = onUndo,
            size = 48.dp,
            testTag = "undo_button",
            enabled = canUndo
        )
        ThemedActionButton(
            iconName = "delete",
            color = theme.colorDanger,
            theme = theme,
            onClick = onDelete,
            size = 56.dp,
            testTag = "delete_button"
        )
        ThemedActionButton(
            iconName = "keep",
            color = theme.colorSuccess,
            theme = theme,
            onClick = onKeep,
            size = 56.dp,
            testTag = "keep_button"
        )
    }
}

@Composable
private fun BottomInfo(
    item: ViewerItem,
    theme: ThemeTokens,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 34.dp)
    ) {
        Text(
            text = item.media.name,
            fontSize = 14.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatBytes(item.media.size),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${item.media.width}×${item.media.height}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            if (item.media.type == MediaType.VIDEO && item.media.duration != null) {
                Spacer(modifier = Modifier.width(12.dp))
                val seconds = (item.media.duration / 1000) % 60
                val minutes = (item.media.duration / 1000 / 60) % 60
                Text(
                    text = "$minutes:${seconds.toString().padStart(2, '0')}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onToggleMute,
                    modifier = Modifier.testTag("mute_button")
                ) {
                    IconPainter(if (isMuted) "mute" else "unmute", theme, size = 20.dp)
                }
            }
        }
    }
}
