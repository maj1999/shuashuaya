package com.cleanpic.ui.common

import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.Text
import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.cleanpic.model.MediaType
import com.cleanpic.theme.ThemeTokens

@Composable
fun EmptyStateScreen(theme: ThemeTokens, type: MediaType, onBack: () -> Unit) {
    val emoji = if (type == MediaType.PHOTO) "\ud83d\udcf7" else "\ud83c\udfac"
    val message = if (type == MediaType.PHOTO) "\u76f8\u518c\u7a7a\u7a7a\u5982\u4e5f" else "\u6ca1\u6709\u89c6\u9891"

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
            Text(text = "\u8fd4\u56de\u9996\u9875", color = Color.White)
        }
    }
}
