package com.cleanpic.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.model.MediaType
import com.cleanpic.permission.PermissionStatus
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.common.PermissionBanner
import com.cleanpic.ui.common.SimpleDialog
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.ui.navigation.Route
import com.cleanpic.viewmodel.HomeViewModel
import com.cleanpic.viewmodel.ViewerViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    router: AppRouter,
    theme: ThemeTokens,
    viewerViewModel: ViewerViewModel
) {
    val homeViewModel = remember { HomeViewModel() }
    val scope = rememberCoroutineScope()
    var showDeniedDialog by remember { mutableStateOf(false) }
    var showPermanentDialog by remember { mutableStateOf(false) }

    fun launchViewer(type: MediaType) {
        val status = homeViewModel.checkPermission()
        when (status) {
            PermissionStatus.GRANTED, PermissionStatus.LIMITED -> {
                scope.launch {
                    viewerViewModel.loadMedia(type)
                    router.navigate(Route.Viewer(type))
                }
            }
            PermissionStatus.DENIED -> showDeniedDialog = true
            PermissionStatus.PERMANENTLY_DENIED -> showPermanentDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground))
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (homeViewModel.isLimitedAccess) {
            PermissionBanner(theme) {
                scope.launch { homeViewModel.requestPermission() }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(text = "✨", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "刷刷鸭",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(theme.colorText)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "随机一刷，相册清爽",
            fontSize = 14.sp,
            color = Color(theme.colorTextSecondary)
        )
        Spacer(modifier = Modifier.height(48.dp))

        ActionButton(
            text = "📷 随机清理照片",
            color = Color(theme.colorPrimary),
            borderRadius = theme.borderRadius
        ) { launchViewer(MediaType.PHOTO) }

        Spacer(modifier = Modifier.height(16.dp))

        ActionButton(
            text = "🎬 随机清理视频",
            color = Color(theme.colorAccent),
            borderRadius = theme.borderRadius
        ) { launchViewer(MediaType.VIDEO) }

        Spacer(modifier = Modifier.height(48.dp))

        TextButton(onClick = { router.navigate(Route.Settings) }) {
            Text(text = "⚙️", fontSize = 28.sp)
        }
    }

    if (showDeniedDialog) {
        PermissionDeniedDialog(
            onRequest = {
                showDeniedDialog = false
                scope.launch { homeViewModel.requestPermission() }
            },
            onDismiss = { showDeniedDialog = false }
        )
    }
    if (showPermanentDialog) {
        PermissionPermanentDialog(
            onGoSettings = {
                showPermanentDialog = false
                homeViewModel.openSettings()
            },
            onDismiss = { showPermanentDialog = false }
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    color: Color,
    borderRadius: Float,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(borderRadius.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text = text, fontSize = 18.sp, color = Color.White)
    }
}

@Composable
private fun PermissionDeniedDialog(onRequest: () -> Unit, onDismiss: () -> Unit) {
    SimpleDialog(
        title = "需要相册权限",
        message = "请授权访问相册，以便随机清理照片和视频",
        confirmText = "授权",
        dismissText = "取消",
        onConfirm = onRequest,
        onDismiss = onDismiss
    )
}

@Composable
private fun PermissionPermanentDialog(onGoSettings: () -> Unit, onDismiss: () -> Unit) {
    SimpleDialog(
        title = "权限已被拒绝",
        message = "请前往系统设置手动开启相册访问权限",
        confirmText = "去设置",
        dismissText = "取消",
        onConfirm = onGoSettings,
        onDismiss = onDismiss
    )
}
