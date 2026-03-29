package com.cleanpic.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.model.InteractionMode
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(router: AppRouter, theme: ThemeTokens) {
    val viewModel = remember { SettingsViewModel() }
    var selectedTheme by remember { mutableStateOf(viewModel.currentThemeId) }
    var selectedMode by remember { mutableStateOf(viewModel.currentMode) }
    var selectedCount by remember { mutableStateOf(viewModel.currentRoundCount) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { router.popBackStack() }) {
                Text(text = "←", fontSize = 20.sp, color = Color(theme.colorText))
            }
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
            // Theme selection
            item {
                SectionTitle("主题")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                ) {
                    items(viewModel.allThemes) { t ->
                        ThemeCard(
                            tokens = t,
                            isSelected = t.id == selectedTheme,
                            borderRadius = theme.borderRadius,
                            onClick = {
                                selectedTheme = t.id
                                viewModel.switchTheme(t.id)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Interaction mode
            item {
                SectionTitle("交互模式")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InteractionMode.entries.forEach { mode ->
                        ModeButton(
                            mode = mode,
                            isSelected = mode == selectedMode,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedMode = mode
                                viewModel.switchInteractionMode(mode)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Round count
            item {
                SectionTitle("每轮数量")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20).forEach { count ->
                        CountChip(
                            count = count,
                            isSelected = count == selectedCount,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedCount = count
                                viewModel.setRoundCount(count)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // About
            item {
                SectionTitle("关于")
                Text(
                    text = "刷刷鸭 v1.0.0",
                    fontSize = 14.sp,
                    color = Color(theme.colorText)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "纯本地处理，不收集任何数据",
                    fontSize = 13.sp,
                    color = Color(theme.colorTextSecondary)
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun ThemeCard(
    tokens: ThemeTokens,
    isSelected: Boolean,
    borderRadius: Float,
    onClick: () -> Unit
) {
    val gradientColors = tokens.gradientMain?.colors?.map { Color(it) }
        ?: listOf(Color(tokens.colorPrimary), Color(tokens.colorAccent))
    val borderMod = if (isSelected) {
        Modifier.border(2.dp, Color(tokens.colorPrimary), RoundedCornerShape(borderRadius.dp))
    } else Modifier

    Column(
        modifier = Modifier
            .width(72.dp)
            .then(borderMod)
            .clip(RoundedCornerShape(borderRadius.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Brush.horizontalGradient(gradientColors))
        )
        Text(
            text = tokens.name,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        if (isSelected) {
            Text(text = "✓", fontSize = 14.sp, color = Color(tokens.colorPrimary))
        }
    }
}

@Composable
private fun ModeButton(
    mode: InteractionMode,
    isSelected: Boolean,
    theme: ThemeTokens,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val icon = when (mode) {
        InteractionMode.CAROUSEL -> "🖼️"
        InteractionMode.SWIPE_CARD -> "🃏"
        InteractionMode.FULLSCREEN -> "📱"
    }
    val bg = if (isSelected) Color(theme.colorPrimary).copy(alpha = 0.15f)
    else Color(theme.colorSurface)
    val borderColor = if (isSelected) Color(theme.colorPrimary) else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .border(1.dp, borderColor, RoundedCornerShape(theme.borderRadius.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = mode.label.take(2), fontSize = 12.sp, color = Color(theme.colorText))
    }
}

@Composable
private fun CountChip(
    count: Int,
    isSelected: Boolean,
    theme: ThemeTokens,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(theme.colorPrimary) else Color(theme.colorSurface)
    val textColor = if (isSelected) Color.White else Color(theme.colorText)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$count", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}
