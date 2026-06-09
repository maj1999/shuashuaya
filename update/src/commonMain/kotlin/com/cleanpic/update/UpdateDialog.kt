package com.cleanpic.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.theme.ThemeTokens

@Composable
fun UpdateDialog(
    theme: ThemeTokens,
    updateInfo: UpdateInfo,
    isForceUpdate: Boolean,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    // 半透明背景遮罩
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = { if (!isForceUpdate) onDismiss() }),
        contentAlignment = Alignment.Center
    ) {
        // 弹窗卡片
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(theme.borderRadius.dp))
                .background(Color(dialogSurfaceColor(theme)))
                .clickable(enabled = false) {} // 防止穿透点击
                .padding(24.dp)
                .testTag("update_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isForceUpdate) "需要更新" else "发现新版本",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(theme.colorText)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "v${updateInfo.version}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(dialogAccentColor(theme)),
                modifier = Modifier.testTag("update_version")
            )

            if (updateInfo.changelog.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "更新内容",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(theme.colorText).copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = updateInfo.changelog,
                    fontSize = 13.sp,
                    color = Color(theme.colorTextSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState()),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 立即更新按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(theme.borderRadius.dp))
                    .background(Color(theme.colorPrimary))
                    .clickable(onClick = onUpdate)
                    .padding(vertical = 12.dp)
                    .testTag("update_now_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "立即更新",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // 稍后提醒按钮（仅可选更新时显示）
            if (!isForceUpdate) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "稍后提醒",
                    fontSize = 14.sp,
                    color = Color(theme.colorTextSecondary),
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 8.dp)
                        .testTag("update_later_button"),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DownloadProgressDialog(
    theme: ThemeTokens,
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(theme.borderRadius.dp))
                .background(Color(dialogSurfaceColor(theme)))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = false,
                    onClick = {}
                )
                .padding(24.dp)
                .testTag("download_progress_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "正在下载更新...",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(theme.colorText)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // 简单进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(theme.colorTextSecondary).copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(theme.colorPrimary))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 13.sp,
                color = Color(theme.colorTextSecondary)
            )
        }
    }
}

@Composable
fun InstallingDialog(theme: ThemeTokens) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(theme.borderRadius.dp))
                .background(Color(dialogSurfaceColor(theme)))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = false,
                    onClick = {}
                )
                .padding(24.dp)
                .testTag("installing_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "正在准备安装...",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(theme.colorText)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "系统安装器即将启动",
                fontSize = 13.sp,
                color = Color(theme.colorTextSecondary)
            )
        }
    }
}

@Composable
fun UpdateFailedDialog(
    theme: ThemeTokens,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(theme.borderRadius.dp))
                .background(Color(dialogSurfaceColor(theme)))
                .clickable(enabled = false) {}
                .padding(24.dp)
                .testTag("update_failed_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "下载失败",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(theme.colorText)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请检查网络连接后重试",
                fontSize = 13.sp,
                color = Color(theme.colorTextSecondary)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(theme.borderRadius.dp))
                    .background(Color(theme.colorPrimary))
                    .clickable(onClick = onRetry)
                    .padding(vertical = 12.dp)
                    .testTag("update_retry_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "重试",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "取消",
                fontSize = 14.sp,
                color = Color(theme.colorTextSecondary),
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 8.dp)
                    .testTag("update_cancel_button"),
                textAlign = TextAlign.Center
            )
        }
    }
}
