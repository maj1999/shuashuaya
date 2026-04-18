package com.cleanpic.ui.settings

import com.cleanpic.AppInfo

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.InteractionMode
import com.cleanpic.theme.ThemeLayoutId
import com.cleanpic.theme.ThemeTokens

/**
 * 统一设置页布局 — 所有主题共享相同的文字大小、间距和内容结构。
 * 各主题通过 ThemeTokens 控制颜色、圆角、阴影、字体族等视觉差异。
 */
@Composable
fun SharedSettingsLayout(state: SettingsScreenState) {
    val theme = state.theme
    val titleFont = if (theme.titleFontFamily == "Serif") FontFamily.Serif else FontFamily.Default
    val radius = theme.borderRadius.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(settingsBackground(theme))
    ) {
        // 顶栏
        SettingsTopBar(theme, titleFont, state.onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // 主题选择
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle("主题", theme, titleFont)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                ) {
                    items(state.allThemes) { t ->
                        ThemeCard(
                            tokens = t,
                            isSelected = t.id == state.theme.id,
                            theme = theme,
                            radius = theme.borderRadius,
                            onClick = { state.onThemeChange(t.id) },
                            testTag = "theme_${t.id}"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 交互模式
            item {
                SectionTitle("交互模式", theme, titleFont)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InteractionMode.entries.forEach { mode ->
                        ModeButton(
                            mode = mode,
                            isSelected = mode.id == state.currentMode,
                            theme = theme,
                            radius = theme.borderRadius,
                            modifier = Modifier.weight(1f),
                            onClick = { state.onModeChange(mode.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 每轮数量
            item {
                SectionTitle("每轮数量", theme, titleFont)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20).forEach { count ->
                        CountChip(
                            count = count,
                            isSelected = count == state.currentCount,
                            theme = theme,
                            radius = theme.borderRadius,
                            modifier = Modifier.weight(1f),
                            onClick = { state.onCountChange(count) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 宿主注入区块（如版本更新）
            item {
                state.extras()
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 关于
            item {
                AboutSection(theme)
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// ── 顶栏 ──────────────────────────────────────────────

@Composable
private fun SettingsTopBar(theme: ThemeTokens, titleFont: FontFamily, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .testTag("back_button")
                .clip(RoundedCornerShape(theme.borderRadius.dp))
                .then(backButtonBackground(theme))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            IconPainter(
                name = "back",
                theme = theme,
                size = 20.dp,
                colorOverride = theme.iconStrokeColor
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "设置",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = titleFont,
            color = Color(theme.colorText)
        )
    }
}

// ── 分区标题 ──────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, theme: ThemeTokens, titleFont: FontFamily) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = titleFont,
        color = Color(theme.colorText).copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

// ── 主题卡片 ──────────────────────────────────────────────

@Composable
private fun ThemeCard(
    tokens: ThemeTokens,
    isSelected: Boolean,
    theme: ThemeTokens,
    radius: Float,
    onClick: () -> Unit,
    testTag: String
) {
    val gradientColors = tokens.gradientMain?.colors?.map { Color(it) }
        ?: listOf(Color(tokens.colorPrimary), Color(tokens.colorAccent))

    val borderMod = if (isSelected) {
        Modifier.border(2.dp, Color(theme.colorPrimary), RoundedCornerShape(radius.dp))
    } else Modifier

    Column(
        modifier = Modifier
            .width(60.dp)
            .then(borderMod)
            .clip(RoundedCornerShape(radius.dp))
            .background(cardBackground(theme))
            .testTag(testTag)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(topStart = radius.dp, topEnd = radius.dp))
                .background(Brush.horizontalGradient(gradientColors))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tokens.name,
            fontSize = 10.sp,
            maxLines = 1,
            color = Color(theme.colorTextSecondary),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        if (isSelected) {
            IconPainter(
                name = "keep",
                theme = theme,
                size = 14.dp,
                colorOverride = theme.colorPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

// ── 交互模式按钮 ──────────────────────────────────────────────

@Composable
private fun ModeButton(
    mode: InteractionMode,
    isSelected: Boolean,
    theme: ThemeTokens,
    radius: Float,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val iconName = when (mode) {
        InteractionMode.CAROUSEL -> "photo"
        InteractionMode.SWIPE_CARD -> "video"
        InteractionMode.FULLSCREEN -> "play"
    }

    val bg = if (isSelected) Color(theme.colorPrimary).copy(alpha = 0.12f) else cardBackground(theme)
    val borderColor = if (isSelected) Color(theme.colorPrimary) else Color.Transparent
    val contentColor = if (isSelected) theme.colorPrimary else theme.iconStrokeColor

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(radius.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconPainter(
            name = iconName,
            theme = theme,
            size = 22.dp,
            colorOverride = contentColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = mode.label.take(4),
            fontSize = 12.sp,
            color = Color(contentColor),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── 数量选择 ──────────────────────────────────────────────

@Composable
private fun CountChip(
    count: Int,
    isSelected: Boolean,
    theme: ThemeTokens,
    radius: Float,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(theme.colorPrimary) else cardBackground(theme)
    val textColor = if (isSelected) Color.White else Color(theme.colorText)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// ── 关于 ──────────────────────────────────────────────

@Composable
private fun AboutSection(theme: ThemeTokens) {
    Box(
        modifier = Modifier.fillMaxWidth().height(1.dp)
            .background(Color(theme.colorTextSecondary).copy(alpha = 0.2f))
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = AppInfo.displayVersion,
        fontSize = 13.sp,
        color = Color(theme.colorTextSecondary)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "纯本地处理，不收集任何数据",
        fontSize = 12.sp,
        color = Color(theme.colorTextSecondary).copy(alpha = 0.6f)
    )
}

// ── 主题差异化辅助函数 ──────────────────────────────────────────────

private fun settingsBackground(theme: ThemeTokens): Brush {
    return if (theme.gradientMain != null && theme.layoutId == ThemeLayoutId.PLAYFUL) {
        Brush.verticalGradient(theme.gradientMain!!.colors.map { Color(it) })
    } else {
        Brush.verticalGradient(listOf(Color(theme.colorBackground), Color(theme.colorBackground)))
    }
}

private fun cardBackground(theme: ThemeTokens): Color {
    return Color(theme.colorSurface)
}

private fun backButtonBackground(theme: ThemeTokens): Modifier {
    return when (theme.layoutId) {
        ThemeLayoutId.PLAYFUL -> Modifier
            .background(Color(0x1FFFFFFF))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(theme.borderRadius.dp))
        else -> Modifier
    }
}
