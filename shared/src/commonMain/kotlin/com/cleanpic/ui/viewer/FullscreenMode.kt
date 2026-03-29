package com.cleanpic.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.model.MediaType
import com.cleanpic.model.ViewerItem
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.viewmodel.ViewerViewModel
import kotlinx.coroutines.launch

private const val VERTICAL_THRESHOLD_DP = 120

@Composable
fun FullscreenMode(
    theme: ThemeTokens,
    viewerViewModel: ViewerViewModel,
    router: AppRouter
) {
    val items by viewerViewModel.items.collectAsState()
    val currentIndex by viewerViewModel.currentIndex.collectAsState()
    if (items.isEmpty() || currentIndex >= items.size) return

    val currentItem = items[currentIndex]
    var isMuted by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 全屏内容区域（支持上下滑动切换，但此处仅用于交互反馈）
        FullscreenContent(
            item = currentItem,
            isMuted = isMuted
        )

        // 顶部浮动栏
        TopBar(
            theme = theme,
            current = currentIndex + 1,
            total = items.size,
            onExit = { router.popBackStack() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 右侧浮动操作按钮
        SideActions(
            theme = theme,
            onDelete = { viewerViewModel.markDelete() },
            onKeep = { viewerViewModel.markKept() },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        // 底部文件信息 + 视频控件
        BottomInfo(
            item = currentItem,
            isMuted = isMuted,
            onToggleMute = { isMuted = !isMuted },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FullscreenContent(item: ViewerItem, isMuted: Boolean) {
    // 占位全屏展示
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (item.media.type == MediaType.PHOTO) "\ud83d\uddbc\ufe0f" else "\ud83c\udfac",
            fontSize = 72.sp
        )
        if (item.media.type == MediaType.VIDEO) {
            Text(
                text = if (isMuted) "\ud83d\udd07 \u5df2\u9759\u97f3" else "\ud83d\udd0a \u64ad\u653e\u4e2d",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
    }
}

@Composable
private fun TopBar(
    theme: ThemeTokens,
    current: Int,
    total: Int,
    onExit: () -> Unit,
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
        TextButton(onClick = onExit) {
            Text(
                text = "\u2190 \u9000\u51fa",
                color = Color.White,
                fontSize = 15.sp
            )
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
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onDelete,
            shape = CircleShape,
            modifier = Modifier.size(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(theme.colorDanger)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(text = "\ud83d\uddd1\ufe0f", fontSize = 22.sp)
        }
        Button(
            onClick = onKeep,
            shape = CircleShape,
            modifier = Modifier.size(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(theme.colorSuccess)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "\u2713",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BottomInfo(
    item: ViewerItem,
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
                text = "${item.media.width}\u00d7${item.media.height}",
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
                TextButton(onClick = onToggleMute) {
                    Text(
                        text = if (isMuted) "\ud83d\udd07" else "\ud83d\udd0a",
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
