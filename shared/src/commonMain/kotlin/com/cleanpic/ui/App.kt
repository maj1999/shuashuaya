package com.cleanpic.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.cleanpic.di.ServiceLocator
import com.cleanpic.model.MediaType
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.ui.navigation.Route
import com.cleanpic.ui.navigation.rememberAppRouter
import com.cleanpic.ui.splash.SplashScreen
import com.cleanpic.ui.home.HomeScreen
import com.cleanpic.ui.viewer.ViewerScreen
import com.cleanpic.ui.result.ResultScreen
import com.cleanpic.ui.settings.SettingsScreen
import com.cleanpic.update.DownloadProgressDialog
import com.cleanpic.update.DownloadState
import com.cleanpic.update.UpdateDialog
import com.cleanpic.update.UpdateStatus
import com.cleanpic.viewmodel.ViewerViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CleanPicApp() {
    val themeManager = ServiceLocator.themeManager
    val theme by themeManager.currentTheme.collectAsState()
    val router = rememberAppRouter()
    val viewerViewModel = remember { ViewerViewModel() }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDialogShown by remember { mutableStateOf(false) }

    // 监听下载状态
    val installer = ServiceLocator.updateInstaller
    val downloadState = installer?.downloadState?.collectAsState()
    val downloadProgress = installer?.downloadProgress?.collectAsState()

    Box(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        when (val route = router.currentRoute) {
            is Route.Splash -> SplashScreen(theme) {
                router.navigate(Route.Home, clearBackStackUpTo = Route.Splash, inclusive = true)
            }
            is Route.Home -> {
                HomeScreen(router, theme, viewerViewModel)
                // 响应式监听更新结果
                val updateResult by ServiceLocator.cachedUpdateResult.collectAsState()
                if (!updateDialogShown && updateResult.status != UpdateStatus.UP_TO_DATE && updateResult.updateInfo != null) {
                    showUpdateDialog = true
                    updateDialogShown = true
                }
            }
            is Route.Viewer -> ViewerScreen(router, theme, viewerViewModel, route.type)
            is Route.Result -> ResultScreen(router, theme, viewerViewModel)
            is Route.Settings -> SettingsScreen(router, theme)
        }

        // 更新弹窗
        if (showUpdateDialog) {
            val result = ServiceLocator.cachedUpdateResult.value
            val info = result.updateInfo
            if (info != null) {
                UpdateDialog(
                    theme = theme,
                    updateInfo = info,
                    isForceUpdate = result.status == UpdateStatus.FORCE_UPDATE,
                    onUpdate = {
                        showUpdateDialog = false
                        installer?.startUpdate(info)
                    },
                    onDismiss = {
                        showUpdateDialog = false
                    }
                )
            }
        }

        // 下载进度弹窗
        if (downloadState?.value == DownloadState.DOWNLOADING) {
            DownloadProgressDialog(
                theme = theme,
                progress = downloadProgress?.value ?: 0f
            )
        }
    }
}
