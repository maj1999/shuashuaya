package com.cleanpic.ui.settings

import com.cleanpic.AppInfo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.model.InteractionMode
import com.cleanpic.theme.ThemeTokens
import androidx.compose.ui.platform.testTag

private val EditorialBg = Color(0xFFFFFFF5)
private val EditorialText = Color(0xFF1A1A1A)
private val EditorialSecondary = Color(0xFF999999)
private val EditorialDivider = Color(0xFFE0DDD6)

@Composable
fun EditorialSettingsLayout(state: SettingsScreenState) {
    val theme = state.theme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialBg)
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "← BACK",
                fontSize = 10.sp,
                fontFamily = FontFamily.Serif,
                color = EditorialSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .testTag("back_button")
                    .clickable { state.onBack() }
            )
            Text(
                text = "设置",
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                color = EditorialText
            )
        }

        // 顶部分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EditorialDivider)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // 主题选择
            item {
                Spacer(modifier = Modifier.height(24.dp))

                EditorialSectionLabel("THEME")

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    items(state.allThemes) { t ->
                        EditorialThemeStrip(
                            tokens = t,
                            isSelected = t.id == state.theme.id,
                            onClick = { state.onThemeChange(t.id) },
                            testTag = "theme_${t.id}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 交互模式
            item {
                EditorialSectionLabel("MODE")

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InteractionMode.entries.forEach { mode ->
                        EditorialModeOption(
                            label = mode.label.take(4),
                            isSelected = mode.id == state.currentMode,
                            onClick = { state.onModeChange(mode.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 每轮数量
            item {
                EditorialSectionLabel("COUNT")

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    listOf(5, 10, 15, 20).forEach { count ->
                        EditorialCountOption(
                            count = count,
                            isSelected = count == state.currentCount,
                            onClick = { state.onCountChange(count) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // 分割线
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(EditorialDivider)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 脚注
            item {
                Text(
                    text = AppInfo.displayVersion,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Serif,
                    color = EditorialSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "纯本地处理",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Serif,
                    color = EditorialSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun EditorialSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontFamily = FontFamily.Serif,
        color = EditorialSecondary,
        letterSpacing = 3.sp
    )
}

@Composable
private fun EditorialThemeStrip(
    tokens: ThemeTokens,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    val gradientColors = tokens.gradientMain?.colors?.map { Color(it) }
        ?: listOf(Color(tokens.colorPrimary), Color(tokens.colorAccent))

    val underlineColor = if (isSelected) EditorialText else Color.Transparent

    Column(
        modifier = Modifier
            .width(40.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 色条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Brush.horizontalGradient(gradientColors))
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 选中下方细线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(underlineColor)
        )
    }
}

@Composable
private fun EditorialModeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val underlineColor = if (isSelected) EditorialText else Color.Transparent

    Text(
        text = label,
        fontSize = 12.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isSelected) EditorialText else EditorialSecondary,
        modifier = Modifier
            .clickable(onClick = onClick)
            .drawBehind {
                if (isSelected) {
                    drawLine(
                        color = EditorialText,
                        start = Offset(0f, size.height + 2.dp.toPx()),
                        end = Offset(size.width, size.height + 2.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .padding(bottom = 4.dp)
    )
}

@Composable
private fun EditorialCountOption(
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = "$count",
        fontSize = 14.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) EditorialText else EditorialSecondary,
        modifier = Modifier.clickable(onClick = onClick)
    )
}
