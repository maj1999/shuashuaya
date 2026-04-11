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
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.update.DownloadProgressDialog
import com.cleanpic.update.DownloadState
import com.cleanpic.update.InstallingDialog
import com.cleanpic.update.UpdateDialog
import com.cleanpic.update.UpdateFailedDialog
import com.cleanpic.update.UpdateInstaller
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

    val installer = ServiceLocator.updateInstaller

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

        // 下载相关弹窗（独立组合函数，隔离进度更新的重组范围）
        DownloadOverlay(installer = installer, theme = theme)
    }
}

/**
 * 独立组合函数：将下载状态收集隔离在此作用域内，
 * 避免进度更新导致 CleanPicApp 整棵组合树重组。
 */
@Composable
private fun DownloadOverlay(installer: UpdateInstaller?, theme: ThemeTokens) {
    if (installer == null) return

    val downloadState by installer.downloadState.collectAsState()
    val downloadProgress by installer.downloadProgress.collectAsState()

    when (downloadState) {
        DownloadState.DOWNLOADING -> {
            DownloadProgressDialog(
                theme = theme,
                progress = downloadProgress
            )
        }
        DownloadState.INSTALLING -> {
            InstallingDialog(theme = theme)
        }
        DownloadState.FAILED -> {
            val result = ServiceLocator.cachedUpdateResult.value
            val info = result.updateInfo
            UpdateFailedDialog(
                theme = theme,
                onRetry = {
                    installer.resetState()
                    if (info != null) {
                        installer.startUpdate(info)
                    }
                },
                onDismiss = {
                    installer.resetState()
                }
            )
        }
        else -> {}
    }
}
