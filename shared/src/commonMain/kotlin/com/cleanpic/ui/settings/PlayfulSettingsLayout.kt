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
fun PlayfulSettingsLayout(state: SettingsScreenState) {
    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏：毛玻璃返回按钮 + 白色标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("back_button")
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1FFFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { state.onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    IconPainter(
                        name = "back",
                        theme = state.theme,
                        size = 22.dp,
                        colorOverride = 0xFFFFFFFF.toLong()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "设置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // 主题区域
                item {
                    PlayfulSectionTitle("主题")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x1FFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                        ) {
                            items(state.allThemes) { t ->
                                PlayfulThemeCard(
                                    tokens = t,
                                    isSelected = t.id == state.theme.id,
                                    onClick = { state.onThemeChange(t.id) },
                                    testTag = "theme_${t.id}"
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 交互模式区域
                item {
                    PlayfulSectionTitle("交互模式")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x1FFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InteractionMode.entries.forEach { mode ->
                                PlayfulModeButton(
                                    mode = mode,
                                    isSelected = mode.id == state.currentMode,
                                    theme = state.theme,
                                    modifier = Modifier.weight(1f),
                                    onClick = { state.onModeChange(mode.id) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 每轮数量区域
                item {
                    PlayfulSectionTitle("每轮数量")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x1FFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15, 20).forEach { count ->
                                PlayfulCountChip(
                                    count = count,
                                    isSelected = count == state.currentCount,
                                    modifier = Modifier.weight(1f),
                                    onClick = { state.onCountChange(count) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 底部版本信息
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x1FFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "刷刷鸭 v1.0.0",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "纯本地处理，不收集任何数据",
                                fontSize = 13.sp,
                                color = Color(0xB3FFFFFF)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun PlayfulSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xCCFFFFFF),
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun PlayfulThemeCard(
    tokens: ThemeTokens,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    val gradientColors = tokens.gradientMain?.colors?.map { Color(it) }
        ?: listOf(Color(tokens.colorPrimary), Color(tokens.colorAccent))

    val borderColor = if (isSelected) Color.White else Color(0x40FFFFFF)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AFFFFFF))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Brush.horizontalGradient(gradientColors))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tokens.name,
            fontSize = 10.sp,
            maxLines = 1,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        if (isSelected) {
            IconPainter(
                name = "keep",
                theme = tokens,
                size = 14.dp,
                colorOverride = 0xFFFFFFFF.toLong()
            )
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun PlayfulModeButton(
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

    val bgColor = if (isSelected) Color(0x33FFFFFF) else Color.Transparent
    val borderColor = if (isSelected) Color(0x66FFFFFF) else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconPainter(
            name = iconName,
            theme = theme,
            size = 24.dp,
            colorOverride = 0xFFFFFFFF.toLong()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = mode.label.take(4),
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PlayfulCountChip(
    count: Int,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0x40FFFFFF) else Color.Transparent
    val borderColor = if (isSelected) Color(0x66FFFFFF) else Color(0x33FFFFFF)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
