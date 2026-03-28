package com.cleanpic.ui.home

import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.*
import androidx.compose.runtime.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cleanpic.model.MediaType
import com.cleanpic.permission.PermissionStatus
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.common.PermissionBanner
import com.cleanpic.viewmodel.HomeViewModel
import com.cleanpic.viewmodel.ViewerViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavHostController,
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
                    navController.navigate("viewer/${type.name}")
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

        Text(text = "\u2728", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "CleanPic",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(theme.colorText)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\u968f\u673a\u4e00\u5237\uff0c\u76f8\u518c\u6e05\u723d",
            fontSize = 14.sp,
            color = Color(theme.colorTextSecondary)
        )
        Spacer(modifier = Modifier.height(48.dp))

        ActionButton(
            text = "\ud83d\udcf7 \u968f\u673a\u6e05\u7406\u7167\u7247",
            color = Color(theme.colorPrimary),
            borderRadius = theme.borderRadius
        ) { launchViewer(MediaType.PHOTO) }

        Spacer(modifier = Modifier.height(16.dp))

        ActionButton(
            text = "\ud83c\udfac \u968f\u673a\u6e05\u7406\u89c6\u9891",
            color = Color(theme.colorAccent),
            borderRadius = theme.borderRadius
        ) { launchViewer(MediaType.VIDEO) }

        Spacer(modifier = Modifier.height(48.dp))

        IconButton(onClick = { navController.navigate("settings") }) {
            Text(text = "\u2699\ufe0f", fontSize = 28.sp)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u9700\u8981\u76f8\u518c\u6743\u9650") },
        text = { Text("\u8bf7\u6388\u6743\u8bbf\u95ee\u76f8\u518c\uff0c\u4ee5\u4fbf\u968f\u673a\u6e05\u7406\u7167\u7247\u548c\u89c6\u9891") },
        confirmButton = {
            TextButton(onClick = onRequest) { Text("\u6388\u6743") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") }
        }
    )
}

@Composable
private fun PermissionPermanentDialog(onGoSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u6743\u9650\u5df2\u88ab\u62d2\u7edd") },
        text = { Text("\u8bf7\u524d\u5f80\u7cfb\u7edf\u8bbe\u7f6e\u624b\u52a8\u5f00\u542f\u76f8\u518c\u8bbf\u95ee\u6743\u9650") },
        confirmButton = {
            TextButton(onClick = onGoSettings) { Text("\u53bb\u8bbe\u7f6e") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") }
        }
    )
}
