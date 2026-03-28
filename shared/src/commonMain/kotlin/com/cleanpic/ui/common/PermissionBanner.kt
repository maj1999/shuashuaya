package com.cleanpic.ui.common

import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.cleanpic.theme.ThemeTokens

@Composable
fun PermissionBanner(theme: ThemeTokens, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF3E0))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "\u26a0\ufe0f", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "\u5f53\u524d\u4ec5\u80fd\u8bbf\u95ee\u90e8\u5206\u7167\u7247\uff0c\u70b9\u51fb\u6388\u6743\u5168\u90e8",
            fontSize = 13.sp,
            color = Color(0xFF795548)
        )
    }
}
