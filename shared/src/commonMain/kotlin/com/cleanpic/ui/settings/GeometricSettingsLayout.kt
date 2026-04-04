package com.cleanpic.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.InteractionMode
import com.cleanpic.theme.ThemeTokens
import androidx.compose.ui.platform.testTag

@Composable
fun GeometricSettingsLayout(state: SettingsScreenState) {
    val theme = state.theme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground))
    ) {
        // 顶栏：返回 + 白色粗体 "设置"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .testTag("back_button")
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { state.onBack() },
                contentAlignment = Alignment.Center
            ) {
                IconPainter(
                    name = "back",
                    theme = theme,
                    size = 22.dp,
                    colorOverride = 0x99FFFFFF
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "设置",
                fontSize = 20.sp,
                fontWeight = FontWeight(800),
                color = Color.White
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // 主题选择
            item {
                GeometricSectionTitle("主题")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    items(state.allThemes) { t ->
                        GeometricThemeCard(
                            tokens = t,
                            isSelected = t.id == state.theme.id,
                            onClick = { state.onThemeChange(t.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 交互模式
            item {
                GeometricSectionTitle("交互模式")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InteractionMode.entries.forEach { mode ->
                        GeometricModeButton(
                            mode = mode,
                            isSelected = mode.id == state.currentMode,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            onClick = { state.onModeChange(mode.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 每轮数量
            item {
                GeometricSectionTitle("每轮数量")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20).forEach { count ->
                        GeometricCountChip(
                            count = count,
                            isSelected = count == state.currentCount,
                            modifier = Modifier.weight(1f),
                            onClick = { state.onCountChange(count) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 底部渐变分割线 + 版本信息
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE94560), Color(0xFF533483))
                            )
                        )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "刷刷鸭 v1.0.0  ·  纯本地处理，不收集任何数据",
                    fontSize = 12.sp,
                    color = Color(0x4DFFFFFF),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun GeometricSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight(700),
        color = Color(0x99FFFFFF),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun GeometricThemeCard(
    tokens: ThemeTokens,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val gradientColors = tokens.gradientMain?.colors?.map { Color(it) }
        ?: listOf(Color(tokens.colorPrimary), Color(tokens.colorAccent))

    val borderMod = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = Color(0xFFE94560),
            shape = RoundedCornerShape(16.dp)
        )
    } else Modifier

    Column(
        modifier = Modifier
            .width(80.dp)
            .then(borderMod)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16213E))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 主题色块预览
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .background(Brush.horizontalGradient(gradientColors))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tokens.name,
            fontSize = 10.sp,
            maxLines = 1,
            color = if (isSelected) Color(0xFFE94560) else Color(0x99FFFFFF),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFFE94560))
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun GeometricModeButton(
    mode: InteractionMode,
    isSelected: Boolean,
    theme: ThemeTokens,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val iconName = when (mode) {
        InteractionMode.CAROUSEL -> "photo"
        InteractionMode.SWIPE_CARD -> "video"
        InteractionMode.FULLSCREEN -> "play"
    }

    val bg = if (isSelected) Color(0x26E94560) else Color(0xFF16213E)
    val borderColor = if (isSelected) Color(0xFFE94560) else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconPainter(
            name = iconName,
            theme = theme,
            size = 24.dp,
            colorOverride = if (isSelected) 0xFFE94560 else 0x99FFFFFF
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = mode.label.take(4),
            fontSize = 11.sp,
            color = if (isSelected) Color(0xFFE94560) else Color(0x99FFFFFF),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun GeometricCountChip(
    count: Int,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected)
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFE94560), Color(0xFF533483))
                    )
                else
                    Brush.linearGradient(
                        colors = listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))
                    )
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight(800) else FontWeight.Normal,
            color = Color.White
        )
    }
}
