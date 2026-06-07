package com.cleanpic.ui.home

import androidx.compose.runtime.*
import com.cleanpic.model.MediaType
import com.cleanpic.permission.PermissionStatus
import com.cleanpic.theme.ThemeLayoutId
import com.cleanpic.theme.ThemeTokens
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

    val state = HomeScreenState(
        theme = theme,
        isLimitedAccess = homeViewModel.isLimitedAccess,
        onStartPhoto = { launchViewer(MediaType.PHOTO) },
        onStartVideo = { launchViewer(MediaType.VIDEO) },
        onOpenSettings = { router.navigate(Route.Settings) },
        onOpenStats = { router.navigate(Route.Stats) },
        onRequestPermission = { scope.launch { homeViewModel.requestPermission() } },
        onShowDeniedDialog = { showDeniedDialog = true },
        onShowPermanentDialog = { showPermanentDialog = true }
    )

    when (theme.layoutId) {
        ThemeLayoutId.MINIMAL   -> MinimalHomeLayout(state)
        ThemeLayoutId.GEOMETRIC -> GeometricHomeLayout(state)
        ThemeLayoutId.WARM      -> WarmHomeLayout(state)
        ThemeLayoutId.PLAYFUL   -> PlayfulHomeLayout(state)
        ThemeLayoutId.EDITORIAL -> EditorialHomeLayout(state)
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
