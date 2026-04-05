package com.cleanpic.ui.splash

import androidx.compose.runtime.*
import com.cleanpic.di.ServiceLocator
import com.cleanpic.theme.ThemeLayoutId
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.update.UpdateCheckResult
import com.cleanpic.update.UpdateStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(theme: ThemeTokens, onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        // 启动时后台检查更新（如果启用）
        launch {
            val checker = ServiceLocator.updateChecker
            val settings = ServiceLocator.appSettings
            if (checker != null && settings.autoCheckUpdate) {
                try {
                    val result = checker.checkForUpdate()
                    ServiceLocator.cachedUpdateResult = result
                } catch (_: Exception) {
                    // 静默失败
                }
            }
        }
        delay(1500L)
        onFinished()
    }
    val state = SplashScreenState(theme = theme, onSplashComplete = onFinished)
    when (theme.layoutId) {
        ThemeLayoutId.MINIMAL   -> MinimalSplashLayout(state)
        ThemeLayoutId.GEOMETRIC -> GeometricSplashLayout(state)
        ThemeLayoutId.WARM      -> WarmSplashLayout(state)
        ThemeLayoutId.PLAYFUL   -> PlayfulSplashLayout(state)
        ThemeLayoutId.EDITORIAL -> EditorialSplashLayout(state)
    }
}
