package com.cleanpic.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter

@Composable
fun PlayfulSplashLayout(state: SplashScreenState) {
    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    )

    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 毛玻璃方块内含 photo 图标
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x1FFFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                IconPainter(
                    name = "photo",
                    theme = state.theme,
                    size = 44.dp,
                    colorOverride = 0xFFFFFFFF.toLong()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "刷刷鸭",
                fontSize = 18.sp,
                fontWeight = FontWeight(700),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3 个小圆点
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 0) Color(0xCCFFFFFF) else Color(0x66FFFFFF)
                            )
                    )
                }
            }
        }
    }
}
