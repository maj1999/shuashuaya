package com.cleanpic.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.MediaItem
import com.cleanpic.ui.media.MediaImage

@Composable
fun WarmResultLayout(state: ResultScreenState) {
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
            Spacer(modifier = Modifier.height(40.dp))

            // 完成图标：白色圆形内含 keep 图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x1F5D4037),
                        spotColor = Color(0x1F5D4037)
                    )
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                IconPainter(
                    name = "keep",
                    theme = theme,
                    size = 28.dp,
                    colorOverride = theme.iconStrokeColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 衬线字体标题
            Text(
                text = "本轮清理完成！",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(theme.colorText)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 斜体鼓励语
            Text(
                text = "干得漂亮！",
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                color = Color(theme.colorTextSecondary)
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // 统计卡片
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WarmStatCard(
                    label = "删除",
                    value = "${state.deletedCount}",
                    valueColor = Color(theme.colorDanger),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                WarmStatCard(
                    label = "保留",
                    value = "${state.keptCount}",
                    valueColor = Color(theme.colorSuccess),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                WarmStatCard(
                    label = "释放",
                    value = state.freedSpace,
                    valueColor = Color(0xFF8D6E63),
                    theme = theme,
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(theme.colorText)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                ) {
                    items(state.pendingDeleteItems, key = { it.id }) { mediaItem ->
                        WarmDeletePreviewItem(
                            mediaItem = mediaItem,
                            theme = theme,
                            onCancel = { state.onCancelItem(mediaItem) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 确认删除按钮
        if (state.pendingDeleteItems.isNotEmpty()) {
            item {
                Button(
                    onClick = state.onConfirmDelete,
                    enabled = !state.isDeleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(theme.colorDanger)
                    )
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
                    color = Color(theme.colorTextSecondary),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // 再来一轮按钮（白色阴影卡片）
        item {
            WarmActionCard(
                iconName = "refresh",
                label = "再来一轮",
                theme = theme,
                onClick = state.onNextRound
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 返回首页按钮（白色阴影卡片）
        item {
            WarmActionCard(
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
private fun WarmStatCard(
    label: String,
    value: String,
    valueColor: Color,
    theme: com.cleanpic.theme.ThemeTokens,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x1F5D4037),
                spotColor = Color(0x1F5D4037)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
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
            color = Color(theme.colorTextSecondary)
        )
    }
}

@Composable
private fun WarmDeletePreviewItem(
    mediaItem: MediaItem,
    theme: com.cleanpic.theme.ThemeTokens,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color(0x1F5D4037),
                spotColor = Color(0x1F5D4037)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White),
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
private fun WarmActionCard(
    iconName: String,
    label: String,
    theme: com.cleanpic.theme.ThemeTokens,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x1F5D4037),
                spotColor = Color(0x1F5D4037)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .then(
                Modifier.clickable(onClick = onClick)
            )
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(theme.colorText)
        )
    }
}
