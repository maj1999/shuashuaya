package com.cleanpic.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MinimalSplashLayout(state: SplashScreenState) {
    val theme = state.theme

    // 极简动画：仅 alpha 0f→1f（250ms），无缩放
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 250, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 线条相机图标（用 IconPainter 线宽 1.2px）
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                com.cleanpic.icons.LogoPainter(size = 56.dp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 主标题：细字 letter-spacing 4dp
            Text(
                text = "刷刷鸭",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF333333),
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 细分割线
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(1.dp)
                    .background(Color(0xFFDDDDDD))
            )
        }
    }
}
