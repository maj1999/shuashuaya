package com.cleanpic.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GeometricSplashLayout(state: SplashScreenState) {
    val theme = state.theme
    val easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f) // easeInOutCubic

    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 15f,
            animationSpec = tween(durationMillis = 300, easing = easing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        // 背景几何动画
        Canvas(modifier = Modifier.size(220.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val rectSize = Size(160.dp.toPx(), 160.dp.toPx())
            val rectOffset = Offset(center.x - rectSize.width / 2f, center.y - rectSize.height / 2f)

            // 旋转方形边框（红色）
            rotate(rotation.value, pivot = center) {
                drawRect(
                    color = Color(0xFFE94560),
                    topLeft = rectOffset,
                    size = rectSize,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Butt)
                )
            }

            // 渐变填充小方块（逆向旋转，紫→红）
            val smallSize = Size(60.dp.toPx(), 60.dp.toPx())
            val smallOffset = Offset(center.x - smallSize.width / 2f, center.y - smallSize.height / 2f)
            rotate(-10f, pivot = center) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF533483), Color(0xFFE94560)),
                        start = Offset(smallOffset.x, smallOffset.y),
                        end = Offset(smallOffset.x + smallSize.width, smallOffset.y + smallSize.height)
                    ),
                    topLeft = smallOffset,
                    size = smallSize
                )
            }

            // 白色描边圆形
            drawCircle(
                color = Color.White,
                radius = 90.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 文字区
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(160.dp))

            Text(
                text = "刷刷鸭",
                fontSize = 18.sp,
                fontWeight = FontWeight(900),
                color = Color(0xFFE94560)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 渐变线条
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFE94560), Color(0xFF533483))
                        )
                    )
            )
        }
    }
}
