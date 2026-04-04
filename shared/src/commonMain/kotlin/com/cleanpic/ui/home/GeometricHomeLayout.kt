package com.cleanpic.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
fun GeometricHomeLayout(state: HomeScreenState) {
    val theme = state.theme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "刷刷鸭",
                    fontSize = 16.sp,
                    fontWeight = FontWeight(800),
                    color = Color(0xFFE94560)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconPainter(
                    name = "settings",
                    theme = theme,
                    size = 24.dp,
                    colorOverride = 0x99FFFFFF,
                    modifier = Modifier
                        .testTag("settings_button")
                        .clickable { state.onOpenSettings() }
                )
            }

            // 权限横幅
            if (state.isLimitedAccess) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    PermissionBanner(theme) { state.onRequestPermission() }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // 双卡片并排
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 照片卡
                GeometricMediaCard(
                    iconName = "photo",
                    title = "照片",
                    subtitle = "PHOTO",
                    gradientBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE94560), Color(0xFF0F3460))
                    ),
                    modifier = Modifier.weight(1f).testTag("start_photo"),
                    onClick = state.onStartPhoto
                )
                // 视频卡
                GeometricMediaCard(
                    iconName = "video",
                    title = "视频",
                    subtitle = "VIDEO",
                    gradientBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF533483), Color(0xFF0F3460))
                    ),
                    modifier = Modifier.weight(1f).testTag("start_video"),
                    onClick = state.onStartVideo
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部渐变线条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFE94560), Color(0xFF533483))
                        )
                    )
            )
        }
    }
}

@Composable
private fun GeometricMediaCard(
    iconName: String,
    title: String,
    subtitle: String,
    gradientBrush: Brush,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        IconPainter(
            name = iconName,
            theme = com.cleanpic.theme.GeometricTheme,
            size = 32.dp,
            colorOverride = 0xFFFFFFFF
        )
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight(800),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0x80FFFFFF),
                letterSpacing = 1.sp
            )
        }
    }
}
