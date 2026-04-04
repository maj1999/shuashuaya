package com.cleanpic.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.ui.common.PermissionBanner
import androidx.compose.ui.platform.testTag

@Composable
fun MinimalHomeLayout(state: HomeScreenState) {
    val theme = state.theme
    var selectedTab by remember { mutableStateOf("photo") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // 顶栏：标题 + 设置图标
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "刷刷鸭",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF333333),
                letterSpacing = 3.sp
            )
            IconPainter(
                name = "settings",
                theme = theme,
                size = 20.dp,
                colorOverride = 0xFF999999,
                modifier = Modifier
                    .testTag("settings_button")
                    .clickable { state.onOpenSettings() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 40dp 宽细分割线
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(Color(0xFF333333))
                .align(Alignment.Start)
        )

        if (state.isLimitedAccess) {
            Spacer(modifier = Modifier.height(20.dp))
            PermissionBanner(theme) { state.onRequestPermission() }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 分段标签：照片 / 视频
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            MinimalTab(
                label = "照片",
                isSelected = selectedTab == "photo",
                onClick = { selectedTab = "photo" }
            )
            Spacer(modifier = Modifier.width(24.dp))
            MinimalTab(
                label = "视频",
                isSelected = selectedTab == "video",
                onClick = { selectedTab = "video" }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 大淡色图标
        IconPainter(
            name = if (selectedTab == "photo") "photo" else "video",
            theme = theme,
            size = 56.dp,
            colorOverride = 0xFFCCCCCC
        )

        Spacer(modifier = Modifier.weight(1f))

        // 描边按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(if (selectedTab == "photo") "start_photo" else "start_video")
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(2.dp))
                .clickable {
                    if (selectedTab == "photo") state.onStartPhoto()
                    else state.onStartVideo()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "开始",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF333333),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(56.dp))
    }
}

@Composable
private fun MinimalTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) Color(0xFF333333) else Color(0xFF999999)
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(1.dp)
                    .background(Color(0xFF333333))
            )
        } else {
            Box(modifier = Modifier.height(1.dp))
        }
    }
}
