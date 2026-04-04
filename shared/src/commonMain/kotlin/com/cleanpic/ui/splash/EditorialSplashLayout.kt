package com.cleanpic.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.LogoPainter

private val EditorialBg = Color(0xFFFFFFF5)
private val EditorialText = Color(0xFF1A1A1A)
private val EditorialSecondary = Color(0xFF999999)
private val EditorialDivider = Color(0xFFE0DDD6)

@Composable
fun EditorialSplashLayout(state: SplashScreenState) {
    val theme = state.theme

    // 打字机效果：标题宽度从 0f → 1f（300ms）
    val widthFraction = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        widthFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // EST. 2025
            Text(
                text = "— EST. 2025 —",
                fontSize = 9.sp,
                fontFamily = FontFamily.Serif,
                color = EditorialSecondary,
                letterSpacing = 5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 主标题（打字机效果）
            Box(
                modifier = Modifier.clipToBounds()
            ) {
                val fraction = widthFraction.value
                Text(
                    text = "刷刷鸭",
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    color = EditorialText.copy(alpha = fraction),
                    modifier = Modifier
                        .widthIn(max = (80 * fraction + 0.1f).dp)
                        .clipToBounds()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 细分割线
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(1.dp)
                    .background(EditorialDivider)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 副标题
            Text(
                text = "随机一刷 · 相册清爽",
                fontSize = 10.sp,
                fontFamily = FontFamily.Serif,
                color = EditorialSecondary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 鸭子 logo 图标
            LogoPainter(size = 56.dp)
        }
    }
}
