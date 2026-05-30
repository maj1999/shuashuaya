package com.cleanpic.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.MediaItem
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.media.MediaImage

@Composable
fun MinimalResultLayout(state: ResultScreenState) {
    val theme = state.theme
    val confirm = state.phase == ResultPhase.CONFIRM
    val title = if (confirm) state.confirmTitle else "本轮清理完成"
    val delLabel = if (confirm) "待删除" else "已删除"
    val keepLabel = if (confirm) "拟保留" else "已保留"
    val freeLabel = if (confirm) "可释放" else "已释放"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(56.dp))

            // 小字英文标签
            Text(
                text = if (confirm) "CONFIRM" else "COMPLETE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = if (confirm) Color(0xFFCC3333) else Color(0xFF999999),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 标题：细字
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF333333)
            )

            if (confirm) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.irreversibleHint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFCC3333)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 分割线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE0E0E0))
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 统计行
        item {
            MinimalStatRow(label = delLabel, value = "${state.deletedCount}", valueColor = Color(0xFFCC3333))
            MinimalDivider()
            MinimalStatRow(label = keepLabel, value = "${state.keptCount}", valueColor = Color(0xFF333333))
            MinimalDivider()
            MinimalStatRow(label = freeLabel, value = state.freedSpace, valueColor = Color(0xFF666666))
            MinimalDivider()

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 待删除缩略图
        if (state.pendingDeleteItems.isNotEmpty()) {
            item {
                Text(
                    text = "待删除",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF999999),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    items(state.pendingDeleteItems, key = { it.id }) { mediaItem ->
                        MinimalDeletePreviewItem(
                            mediaItem = mediaItem,
                            theme = theme,
                            onCancel = { state.onCancelItem(mediaItem) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // 确认删除按钮（红色描边）
        if (state.pendingDeleteItems.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Color(0xFFCC3333), RoundedCornerShape(2.dp))
                        .clickable(enabled = !state.isDeleting, onClick = state.onConfirmDelete),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFCC3333),
                            strokeWidth = 1.dp
                        )
                    } else {
                        Text(
                            text = "确认删除",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFCC3333),
                            letterSpacing = 1.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 删除结果消息
        if (state.deleteResult != null) {
            item {
                Text(
                    text = state.deleteResult,
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // 再来一轮 + 返回首页（灰色描边按钮）
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MinimalOutlineButton(
                    label = "再来一轮",
                    modifier = Modifier.weight(1f),
                    onClick = state.onNextRound
                )
                MinimalOutlineButton(
                    label = "返回首页",
                    modifier = Modifier.weight(1f),
                    onClick = state.onGoHome
                )
            }
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

@Composable
private fun MinimalStatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF333333)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = valueColor
        )
    }
}

@Composable
private fun MinimalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE0E0E0))
    )
}

@Composable
private fun MinimalDeletePreviewItem(
    mediaItem: MediaItem,
    theme: ThemeTokens,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onCancel)
    ) {
        MediaImage(
            item = mediaItem,
            modifier = Modifier.fillMaxSize()
        )
        // 取消覆盖层
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopEnd)
                .background(Color(0x88FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            IconPainter(
                name = "close",
                theme = theme,
                size = 10.dp,
                colorOverride = 0xFFCC3333
            )
        }
    }
}

@Composable
private fun MinimalOutlineButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .border(1.dp, Color(0xFF999999), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}
