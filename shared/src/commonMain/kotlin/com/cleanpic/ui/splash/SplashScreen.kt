package com.cleanpic.ui.splash

import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.material3.Text
import androidx.compose.runtime.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.cleanpic.theme.ThemeTokens
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(theme: ThemeTokens, onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500L)
        onFinished()
    }

    val gradientColors = theme.gradientMain?.colors?.map { Color(it) }
        ?: listOf(Color(theme.colorPrimary), Color(theme.colorAccent))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\u2728",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CleanPic",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\u968f\u673a\u4e00\u5237\uff0c\u76f8\u518c\u6e05\u723d",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
