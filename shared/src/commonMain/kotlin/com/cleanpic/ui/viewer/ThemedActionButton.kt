package com.cleanpic.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cleanpic.icons.IconPainter
import com.cleanpic.theme.ButtonStyle
import com.cleanpic.theme.ThemeLayoutId
import com.cleanpic.theme.ThemeTokens

/**
 * 主题化操作按钮 — 根据当前主题的 buttonStyle 呈现不同风格。
 * 用于 Viewer 页面的删除/保留按钮。
 */
@Composable
fun ThemedActionButton(
    iconName: String,
    color: Long,
    theme: ThemeTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    testTag: String = "",
    enabled: Boolean = true
) {
    val shape = when (theme.buttonStyle) {
        ButtonStyle.OUTLINED -> if (theme.layoutId == ThemeLayoutId.MINIMAL) RoundedCornerShape(0.dp) else CircleShape
        ButtonStyle.FILLED -> RoundedCornerShape(theme.borderRadius.dp)
        ButtonStyle.SHADOW -> CircleShape
        ButtonStyle.GLASS -> RoundedCornerShape(theme.borderRadius.dp)
        ButtonStyle.TEXT -> RoundedCornerShape(4.dp)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .then(buttonBackground(theme.buttonStyle, color, theme))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        IconPainter(
            iconName, theme,
            size = (size.value * 0.38f).dp,
            colorOverride = iconColor(theme.buttonStyle, color)
        )
    }
}

/**
 * 按钮背景 Modifier，根据 ButtonStyle 生成不同效果
 */
private fun buttonBackground(style: ButtonStyle, color: Long, theme: ThemeTokens): Modifier {
    return when (style) {
        ButtonStyle.OUTLINED -> Modifier.border(
            width = if (theme.layoutId == ThemeLayoutId.MINIMAL) 1.dp else 1.5.dp,
            color = Color(color),
            shape = if (theme.layoutId == ThemeLayoutId.MINIMAL) RoundedCornerShape(0.dp) else CircleShape
        )
        ButtonStyle.FILLED -> Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(Color(color), Color(color).copy(alpha = 0.7f))
            )
        )
        ButtonStyle.SHADOW -> Modifier.background(Color.White)
        ButtonStyle.GLASS -> Modifier
            .background(Color(color).copy(alpha = 0.25f))
            .border(1.dp, Color(color).copy(alpha = 0.4f), RoundedCornerShape(theme.borderRadius.dp))
        ButtonStyle.TEXT -> Modifier
    }
}

/**
 * 图标颜色：填充/毛玻璃按钮用白色，描边/阴影/文字按钮用主题色
 */
private fun iconColor(style: ButtonStyle, color: Long): Long {
    return when (style) {
        ButtonStyle.FILLED -> 0xFFFFFFFF
        ButtonStyle.GLASS -> 0xFFFFFFFF
        ButtonStyle.OUTLINED -> color
        ButtonStyle.SHADOW -> color
        ButtonStyle.TEXT -> color
    }
}
