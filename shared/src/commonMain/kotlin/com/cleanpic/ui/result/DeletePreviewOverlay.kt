package com.cleanpic.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.viewer.ZoomableMediaContent

/**
 * 待删除项全屏预览（US-CP-24）。
 *
 * 结果页待确认态点击缩略图进入：HorizontalPager 在待删除项间左右滑，
 * 每页复用 [ZoomableMediaContent]（照片缩放 / 视频播放）；底部「取消删除」作用于当前页。
 * 列表删空由父层（ResultScreen）感知并关闭。
 */
@Composable
fun DeletePreviewOverlay(
    items: List<MediaItem>,
    startIndex: Int,
    theme: ThemeTokens,
    onCancelDelete: (MediaItem) -> Unit,
    onBack: () -> Unit,
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, items.size - 1)
    ) { items.size }
    var isMuted by remember { mutableStateOf(true) }

    val safePage = pagerState.currentPage.coerceIn(0, items.size - 1)
    val currentMedia = items[safePage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("delete_preview_overlay")
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomableMediaContent(media = items[page], isMuted = isMuted)
        }

        // 顶栏：返回 + 进度
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(top = 44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.testTag("preview_back_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconPainter("back", theme, size = 15.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "返回", color = Color.White, fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${safePage + 1} / ${items.size}",
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // 底栏：取消删除（主题色半透明圆底座 + undo 撤销图标 + 底部渐变遮罩）+ 视频静音
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp, bottom = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 主题色半透明圆底座：保证全屏照片上有清晰可点区域，
                // 同时底座取主题主色、图标取主题 success 色（强制不透明），保留各主题差异。
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(theme.colorPrimary).copy(alpha = 0.65f))
                        .clickable { onCancelDelete(currentMedia) }
                        .testTag("preview_cancel_delete"),
                    contentAlignment = Alignment.Center
                ) {
                    IconPainter("undo", theme, size = 24.dp, colorOverride = theme.colorSuccess or 0xFF000000L)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "取消删除", color = Color.White, fontSize = 13.sp)
            }
            if (currentMedia.type == MediaType.VIDEO) {
                TextButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .testTag("preview_mute_button")
                ) {
                    IconPainter(if (isMuted) "mute" else "unmute", theme, size = 20.dp)
                }
            }
        }
    }
}
