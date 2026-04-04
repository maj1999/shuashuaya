package com.cleanpic.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.ui.common.PermissionBanner
import androidx.compose.ui.platform.testTag

@Composable
fun PlayfulHomeLayout(state: HomeScreenState) {
    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // 顶栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "刷刷鸭",
                    fontSize = 16.sp,
                    fontWeight = FontWeight(700),
                    color = Color.White
                )
                // 毛玻璃设置按钮
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_button")
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1FFFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { state.onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    IconPainter(
                        name = "settings",
                        theme = state.theme,
                        size = 18.dp,
                        colorOverride = 0xFFFFFFFF.toLong()
                    )
                }
            }

            if (state.isLimitedAccess) {
                Spacer(modifier = Modifier.height(16.dp))
                PermissionBanner(state.theme) { state.onRequestPermission() }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 大毛玻璃卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x1FFFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .padding(28.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "选择要清理的内容",
                        fontSize = 12.sp,
                        color = Color(0x99FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        // 照片方块
                        PlayfulMediaBlock(
                            iconName = "photo",
                            label = "照片",
                            onClick = state.onStartPhoto,
                            theme = state.theme,
                            testTag = "start_photo"
                        )
                        // 视频方块
                        PlayfulMediaBlock(
                            iconName = "video",
                            label = "视频",
                            onClick = state.onStartVideo,
                            theme = state.theme,
                            testTag = "start_video"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部两个小圆点
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xB3FFFFFF))
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0x40FFFFFF))
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PlayfulMediaBlock(
    iconName: String,
    label: String,
    onClick: () -> Unit,
    theme: com.cleanpic.theme.ThemeTokens,
    testTag: String = ""
) {
    Column(
        modifier = Modifier
            .size(72.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x2EFFFFFF))
            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconPainter(
            name = iconName,
            theme = theme,
            size = 24.dp,
            colorOverride = 0xFFFFFFFF.toLong()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White
        )
    }
}
