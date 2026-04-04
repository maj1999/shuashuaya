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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.ui.common.PermissionBanner
import androidx.compose.ui.platform.testTag

@Composable
fun WarmHomeLayout(state: HomeScreenState) {
    val theme = state.theme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        if (state.isLimitedAccess) {
            PermissionBanner(theme) { state.onRequestPermission() }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 衬线字体标题
        Text(
            text = "刷刷鸭",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(theme.colorText)
        )
        Spacer(modifier = Modifier.height(6.dp))
        // 斜体副标题
        Text(
            text = "随机一刷，相册清爽",
            fontSize = 15.sp,
            fontStyle = FontStyle.Italic,
            color = Color(theme.colorTextSecondary)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 照片卡片
        MediaCard(
            iconName = "photo",
            title = "清理照片",
            description = "随机浏览，轻松决定",
            theme = theme,
            onClick = state.onStartPhoto,
            testTag = "start_photo"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 视频卡片
        MediaCard(
            iconName = "video",
            title = "清理视频",
            description = "随机浏览，轻松决定",
            theme = theme,
            onClick = state.onStartVideo,
            testTag = "start_video"
        )

        Spacer(modifier = Modifier.weight(1f))

        // 底部设置图标
        IconPainter(
            name = "settings",
            theme = theme,
            size = 28.dp,
            modifier = Modifier
                .testTag("settings_button")
                .clickable { state.onOpenSettings() }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun MediaCard(
    iconName: String,
    title: String,
    description: String,
    theme: com.cleanpic.theme.ThemeTokens,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(theme.borderRadius.dp),
                ambientColor = Color(0x145D4037),
                spotColor = Color(0x145D4037)
            )
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .background(Color(theme.colorSurface))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 暖色图标容器
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFBEEE6)),
            contentAlignment = Alignment.Center
        ) {
            IconPainter(
                name = iconName,
                theme = theme,
                size = 24.dp,
                colorOverride = theme.iconStrokeColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(theme.colorText)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(theme.colorTextSecondary)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 右侧箭头
        Text(
            text = "›",
            fontSize = 24.sp,
            color = Color(theme.colorTextSecondary)
        )
    }
}
