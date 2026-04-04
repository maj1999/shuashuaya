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

@Composable
fun MinimalSettingsLayout(state: SettingsScreenState) {
    val theme = state.theme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // 顶栏：返回 + 设置标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { state.onBack() },
                contentAlignment = Alignment.Center
            ) {
                IconPainter(
                    name = "back",
                    theme = theme,
                    size = 20.dp,
                    colorOverride = 0xFF333333
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "设置",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF333333),
                letterSpacing = 2.sp
            )
        }

        // 顶部分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE0E0E0))
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // THEME
            item {
                Spacer(modifier = Modifier.height(28.dp))
                MinimalSectionLabel("THEME")
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    items(state.allThemes) { t ->
                        MinimalThemeBlock(
                            tokens = t,
                            isSelected = t.id == state.theme.id,
                            onClick = { state.onThemeChange(t.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // MODE
            item {
                MinimalSectionLabel("MODE")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InteractionMode.entries.forEach { mode ->
                        MinimalModeButton(
                            mode = mode,
                            isSelected = mode.id == state.currentMode,
                            modifier = Modifier.weight(1f),
                            onClick = { state.onModeChange(mode.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // COUNT
            item {
                MinimalSectionLabel("COUNT")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(5, 10, 15, 20).forEach { count ->
                        MinimalCountBlock(
                            count = count,
                            isSelected = count == state.currentCount,
                            modifier = Modifier.weight(1f),
                            onClick = { state.onCountChange(count) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            // 关于
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE0E0E0))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "刷刷鸭 v1.0.0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFFBBBBBB),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }
}

@Composable
private fun MinimalSectionLabel(label: String) {
    Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF999999),
        letterSpacing = 2.sp
    )
}

@Composable
private fun MinimalThemeBlock(
    tokens: ThemeTokens,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val gradientColors = tokens.gradientMain?.colors?.map { Color(it) }
        ?: listOf(Color(tokens.colorPrimary), Color(tokens.colorAccent))

    val borderColor = if (isSelected) Color(0xFF333333) else Color(0xFFE0E0E0)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .width(60.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Brush.horizontalGradient(gradientColors))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tokens.name,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            color = Color(0xFF666666),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun MinimalModeButton(
    mode: InteractionMode,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF333333) else Color(0xFFE0E0E0)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val textColor = if (isSelected) Color(0xFF333333) else Color(0xFF999999)

    Box(
        modifier = modifier
            .height(44.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.label.take(4),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun MinimalCountBlock(
    count: Int,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF333333) else Color(0xFFE0E0E0)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val textColor = if (isSelected) Color(0xFF333333) else Color(0xFF999999)

    Box(
        modifier = modifier
            .height(44.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = textColor
        )
    }
}
