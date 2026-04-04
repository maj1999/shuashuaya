package com.cleanpic.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.LogoPainter

@Composable
fun WarmSplashLayout(state: SplashScreenState) {
    val theme = state.theme

    // 入场动画：scale 0.8f→1f + alpha 0f→1f（280ms easeOutCubic）
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }
    val easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f) // easeOutCubic

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 280, easing = easing)
        )
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 280, easing = easing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 白色圆角卡片（80dp x 80dp，圆角 24dp，柔和阴影）内含 photo logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                LogoPainter(size = 56.dp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            // 衬线字体 "刷刷鸭"（18sp）
            Text(
                text = "刷刷鸭",
                fontSize = 18.sp,
                color = Color(theme.colorText)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 斜体副标题（11sp）
            Text(
                text = "随机一刷，相册清爽",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = Color(theme.colorTextSecondary)
            )
        }
    }
}
