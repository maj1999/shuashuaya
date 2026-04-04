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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.InteractionMode
import com.cleanpic.theme.ThemeTokens

@Composable
fun WarmSettingsLayout(state: SettingsScreenState) {
    val theme = state.theme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground))
    ) {
        // 顶栏：返回图标 + "设置"衬线字体标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { state.onBack() },
                contentAlignment = Alignment.Center
            ) {
                IconPainter(
                    name = "back",
                    theme = theme,
                    size = 22.dp,
                    colorOverride = theme.iconStrokeColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(theme.colorText)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // 主题选择
            item {
                WarmSectionTitle("主题")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    items(state.allThemes) { t ->
                        WarmThemeCard(
                            tokens = t,
                            isSelected = t.id == state.theme.id,
                            currentTheme = theme,
                            onClick = { state.onThemeChange(t.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 交互模式
            item {
                WarmSectionTitle("交互模式")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InteractionMode.entries.forEach { mode ->
                        WarmModeButton(
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
                WarmSectionTitle("每轮数量")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20).forEach { count ->
                        WarmCountChip(
                            count = count,
                            isSelected = count == state.currentCount,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            onClick = { state.onCountChange(count) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 关于
            item {
                WarmSectionTitle("关于")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(theme.borderRadius.dp),
                            ambientColor = Color(0x1F5D4037),
                            spotColor = Color(0x1F5D4037)
                        )
                        .clip(RoundedCornerShape(theme.borderRadius.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "刷刷鸭 v1.0.0",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(theme.colorText)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "纯本地处理，不收集任何数据",
                        fontSize = 13.sp,
                        color = Color(theme.colorTextSecondary)
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun WarmSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF5D4037),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun WarmThemeCard(
    tokens: ThemeTokens,
    isSelected: Boolean,
    currentTheme: ThemeTokens,
    onClick: () -> Unit
) {
    val gradientColors = tokens.gradientMain?.colors?.map { Color(it) }
        ?: listOf(Color(tokens.colorPrimary), Color(tokens.colorAccent))

    val borderMod = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = Color(currentTheme.colorPrimary),
            shape = RoundedCornerShape(currentTheme.borderRadius.dp)
        )
    } else Modifier

    Column(
        modifier = Modifier
            .width(80.dp)
            .shadow(
                elevation = if (isSelected) 4.dp else 2.dp,
                shape = RoundedCornerShape(currentTheme.borderRadius.dp),
                ambientColor = Color(0x1F5D4037),
                spotColor = Color(0x1F5D4037)
            )
            .then(borderMod)
            .clip(RoundedCornerShape(currentTheme.borderRadius.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 主题色块预览
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = currentTheme.borderRadius.dp,
                        topEnd = currentTheme.borderRadius.dp
                    )
                )
                .background(Brush.horizontalGradient(gradientColors))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tokens.name,
            fontSize = 10.sp,
            maxLines = 1,
            color = Color(0xFF5D4037),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        // 选中状态用 keep 图标代替 ✓
        if (isSelected) {
            IconPainter(
                name = "keep",
                theme = currentTheme,
                size = 14.dp,
                colorOverride = currentTheme.colorPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun WarmModeButton(
    mode: InteractionMode,
    isSelected: Boolean,
    theme: ThemeTokens,
    modifier: Modifier,
    onClick: () -> Unit
) {
    // photo → CAROUSEL（轮播相册），video → SWIPE_CARD（卡片滑），photo/fullscreen → FULLSCREEN
    val iconName = when (mode) {
        InteractionMode.CAROUSEL -> "photo"
        InteractionMode.SWIPE_CARD -> "video"
        InteractionMode.FULLSCREEN -> "play"
    }

    val bg = if (isSelected) Color(theme.colorPrimary).copy(alpha = 0.12f)
    else Color.White
    val borderColor = if (isSelected) Color(theme.colorPrimary) else Color.Transparent

    Column(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(theme.borderRadius.dp),
                ambientColor = Color(0x1F5D4037),
                spotColor = Color(0x1F5D4037)
            )
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(theme.borderRadius.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconPainter(
            name = iconName,
            theme = theme,
            size = 24.dp,
            colorOverride = if (isSelected) theme.colorPrimary else theme.iconStrokeColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = mode.label.take(4),
            fontSize = 11.sp,
            color = if (isSelected) Color(theme.colorPrimary) else Color(theme.colorText),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun WarmCountChip(
    count: Int,
    isSelected: Boolean,
    theme: ThemeTokens,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(theme.colorPrimary) else Color.White
    val textColor = if (isSelected) Color.White else Color(theme.colorText)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 4.dp else 2.dp,
                shape = RoundedCornerShape(theme.borderRadius.dp),
                ambientColor = Color(0x1F5D4037),
                spotColor = Color(0x1F5D4037)
            )
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
