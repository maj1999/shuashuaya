package com.cleanpic.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun GeometricResultLayout(state: ResultScreenState) {
    val theme = state.theme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(48.dp))

            // 大尺寸 keep 图标
            IconPainter(
                name = "keep",
                theme = theme,
                size = 56.dp,
                colorOverride = 0xFFFFFFFF
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "清理完成",
                fontSize = 18.sp,
                fontWeight = FontWeight(800),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 渐变分割线
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFE94560), Color(0xFF533483))
                        )
                    )
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        // 统计色块卡片
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GeometricStatCard(
                    label = "DELETE",
                    value = "${state.deletedCount}",
                    valueColor = Color(0xFFE94560),
                    bgColor = Color(0x26E94560),
                    modifier = Modifier.weight(1f)
                )
                GeometricStatCard(
                    label = "KEEP",
                    value = "${state.keptCount}",
                    valueColor = Color(0xFF4CAF50),
                    bgColor = Color(0x264CAF50),
                    modifier = Modifier.weight(1f)
                )
                GeometricStatCard(
                    label = "MB FREE",
                    value = state.freedSpace,
                    valueColor = Color(0xFF9C6FDB),
                    bgColor = Color(0x33533483),
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
                    color = Color(theme.colorTextSecondary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                ) {
                    items(state.pendingDeleteItems, key = { it.id }) { mediaItem ->
                        GeometricDeletePreviewItem(
                            mediaItem = mediaItem,
                            theme = theme,
                            onCancel = { state.onCancelItem(mediaItem) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // 确认删除渐变按钮
        if (state.pendingDeleteItems.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (!state.isDeleting)
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFE94560), Color(0xFFB71C1C))
                                )
                            else
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0x88E94560), Color(0x88B71C1C))
                                )
                        )
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
                            fontWeight = FontWeight(800)
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
                    fontSize = 13.sp,
                    color = Color(theme.colorTextSecondary),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // 再来一轮
        item {
            GeometricActionButton(
                iconName = "refresh",
                label = "再来一轮",
                theme = theme,
                onClick = state.onNextRound
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 返回首页
        item {
            GeometricActionButton(
                iconName = "home",
                label = "返回首页",
                theme = theme,
                onClick = state.onGoHome
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun GeometricStatCard(
    label: String,
    value: String,
    valueColor: Color,
    bgColor: Color,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight(900),
            color = valueColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor.copy(alpha = 0.7f),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun GeometricDeletePreviewItem(
    mediaItem: MediaItem,
    theme: com.cleanpic.theme.ThemeTokens,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(theme.colorSurface)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MediaImage(
            item = mediaItem,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
        )
        Text(
            text = mediaItem.name,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color(theme.colorText),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        TextButton(
            onClick = onCancel,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(24.dp)
        ) {
            IconPainter(
                name = "close",
                theme = theme,
                size = 12.dp,
                colorOverride = theme.colorDanger
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = "取消", fontSize = 10.sp, color = Color(theme.colorDanger))
        }
    }
}

@Composable
private fun GeometricActionButton(
    iconName: String,
    label: String,
    theme: com.cleanpic.theme.ThemeTokens,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconPainter(
            name = iconName,
            theme = theme,
            size = 20.dp,
            colorOverride = theme.iconStrokeColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
