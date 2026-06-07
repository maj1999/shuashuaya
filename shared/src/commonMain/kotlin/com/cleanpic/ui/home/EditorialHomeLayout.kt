package com.cleanpic.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.icons.IconPainter
import com.cleanpic.ui.common.PermissionBanner
import androidx.compose.ui.platform.testTag

private val EditorialBg = Color(0xFFFFFFF5)
private val EditorialText = Color(0xFF1A1A1A)
private val EditorialSecondary = Color(0xFF999999)
private val EditorialDivider = Color(0xFFE0DDD6)
private val EditorialSurface = Color(0xFFF0EDE6)

@Composable
fun EditorialHomeLayout(state: HomeScreenState) {
    val theme = state.theme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialBg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        if (state.isLimitedAccess) {
            PermissionBanner(theme) { state.onRequestPermission() }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 报头：EST. 2025
        Text(
            text = "EST. 2025",
            fontSize = 9.sp,
            fontFamily = FontFamily.Serif,
            color = EditorialSecondary,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 主标题
        Text(
            text = "刷刷鸭",
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            color = EditorialText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 装饰线
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(1.dp)
                .background(EditorialDivider)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 副标题
        Text(
            text = "随机一刷 · 相册清爽",
            fontSize = 10.sp,
            fontFamily = FontFamily.Serif,
            color = EditorialSecondary,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 双栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 左栏：照片
            EditorialMediaColumn(
                label = "PHOTOS",
                iconName = "photo",
                description = "浏览并整理\n你的照片库",
                theme = theme,
                modifier = Modifier.weight(1f),
                onClick = state.onStartPhoto,
                testTag = "start_photo"
            )

            // 竖分割线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(EditorialDivider)
            )

            // 右栏：视频
            EditorialMediaColumn(
                label = "VIDEOS",
                iconName = "video",
                description = "浏览并整理\n你的视频库",
                theme = theme,
                modifier = Modifier.weight(1f),
                onClick = state.onStartVideo,
                testTag = "start_video"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 底部分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EditorialDivider)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 统计图标 + 设置图标
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPainter(
                name = "stats",
                theme = theme,
                size = 20.dp,
                colorOverride = 0xFF999999,
                modifier = Modifier
                    .testTag("stats_button")
                    .clickable { state.onOpenStats() }
            )
            Spacer(modifier = Modifier.width(20.dp))
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

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun EditorialMediaColumn(
    label: String,
    iconName: String,
    description: String,
    theme: com.cleanpic.theme.ThemeTokens,
    modifier: Modifier,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // 标签
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Serif,
            color = EditorialSecondary,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 色块图标区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(EditorialSurface),
            contentAlignment = Alignment.Center
        ) {
            IconPainter(
                name = iconName,
                theme = theme,
                size = 28.dp,
                colorOverride = 0xFFBBB8B0
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 描述文字
        Text(
            text = description,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            color = EditorialText,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 开始链接
        Text(
            text = "开始 →",
            fontSize = 10.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            color = EditorialText,
            letterSpacing = 1.sp,
            modifier = Modifier
                .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
                .clickable(onClick = onClick)
        )
    }
}
