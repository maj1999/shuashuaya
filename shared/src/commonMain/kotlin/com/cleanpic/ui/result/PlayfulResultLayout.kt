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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.MediaItem
import com.cleanpic.ui.media.MediaImage

@Composable
fun PlayfulResultLayout(state: ResultScreenState) {
    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    )
    val confirm = state.phase == ResultPhase.CONFIRM
    val title = if (confirm) state.confirmTitle else "本轮清理完成"
    val delLabel = if (confirm) "待删除" else "已删除"
    val keepLabel = if (confirm) "拟保留" else "已保留"
    val freeLabel = if (confirm) "可释放" else "已释放"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(48.dp))

                // 毛玻璃完成图标方块
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x1FFFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconPainter(
                        name = "keep",
                        theme = state.theme,
                        size = 28.dp,
                        colorOverride = 0xFFFFFFFF.toLong()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (confirm) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.irreversibleHint,
                        fontSize = 13.sp,
                        color = Color(0xFFFFD4D4)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 3 个毛玻璃统计卡片
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayfulStatCard(
                        label = delLabel,
                        value = "${state.deletedCount}",
                        valueColor = Color(0xFFFF8A80),
                        modifier = Modifier.weight(1f)
                    )
                    PlayfulStatCard(
                        label = keepLabel,
                        value = "${state.keptCount}",
                        valueColor = Color(0xFF80FFB4),
                        modifier = Modifier.weight(1f)
                    )
                    PlayfulStatCard(
                        label = freeLabel,
                        value = state.freedSpace,
                        valueColor = Color(0xFFE0D0FF),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 待删除预览
            if (state.pendingDeleteItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "待删除项目",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xCCFFFFFF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                    ) {
                        items(state.pendingDeleteItems, key = { it.id }) { mediaItem ->
                            PlayfulDeletePreviewItem(
                                mediaItem = mediaItem,
                                theme = state.theme,
                                onCancel = { state.onCancelItem(mediaItem) },
                                onPreview = { state.onPreviewItem(mediaItem) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 半透明毛玻璃删除按钮（红色调）
            if (state.pendingDeleteItems.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x4DFF3B30))
                            .border(1.dp, Color(0x80FF3B30), RoundedCornerShape(24.dp))
                            .clickable(enabled = !state.isDeleting) { state.onConfirmDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "确认删除",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 删除结果消息
            if (state.deleteResult != null) {
                item {
                    Text(
                        text = state.deleteResult,
                        fontSize = 14.sp,
                        color = Color(0xCCFFFFFF),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // 毛玻璃再来一轮
            item {
                PlayfulActionCard(
                    iconName = "refresh",
                    label = "再来一轮",
                    theme = state.theme,
                    onClick = state.onNextRound
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 毛玻璃返回首页
            item {
                PlayfulActionCard(
                    iconName = "home",
                    label = "返回首页",
                    theme = state.theme,
                    onClick = state.onGoHome
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun PlayfulStatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xB3FFFFFF)
        )
    }
}

@Composable
private fun PlayfulDeletePreviewItem(
    mediaItem: MediaItem,
    theme: com.cleanpic.theme.ThemeTokens,
    onCancel: () -> Unit,
    onPreview: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MediaImage(
            item = mediaItem,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .clickable(onClick = onPreview)
                .testTag("delete_thumb")
        )
        Text(
            text = mediaItem.name,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .height(24.dp)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "取消", fontSize = 10.sp, color = Color(0xFFFF8A80))
        }
    }
}

@Composable
private fun PlayfulActionCard(
    iconName: String,
    label: String,
    theme: com.cleanpic.theme.ThemeTokens,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1FFFFFFF))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconPainter(
            name = iconName,
            theme = theme,
            size = 20.dp,
            colorOverride = 0xFFFFFFFF.toLong()
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
