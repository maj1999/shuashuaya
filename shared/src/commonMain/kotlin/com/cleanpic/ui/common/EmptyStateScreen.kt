package com.cleanpic.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.model.MediaType
import com.cleanpic.theme.ThemeTokens

@Composable
fun EmptyStateScreen(theme: ThemeTokens, type: MediaType, onBack: () -> Unit) {
    val emoji = if (type == MediaType.PHOTO) "📷" else "🎬"
    val message = if (type == MediaType.PHOTO) "相册空空如也" else "没有视频"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 72.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 20.sp,
            color = Color(theme.colorText)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(theme.borderRadius.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(theme.colorPrimary)
            )
        ) {
            Text(text = "返回首页", color = Color.White)
        }
    }
}
