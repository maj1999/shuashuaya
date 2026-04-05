package com.cleanpic.ui.splash

import androidx.compose.runtime.*
import com.cleanpic.di.ServiceLocator
import com.cleanpic.theme.ThemeLayoutId
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.update.UpdateCheckResult
import com.cleanpic.update.UpdateStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 独立于 Compose 生命周期的协程作用域，避免 Splash 页面离开时取消网络请求
private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

@Composable
fun SplashScreen(theme: ThemeTokens, onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        // 在独立 scope 中后台检查更新
        val checker = ServiceLocator.updateChecker
        val settings = ServiceLocator.appSettings
        if (checker != null && settings.autoCheckUpdate) {
            updateScope.launch {
                try {
                    val result = checker.checkForUpdate()
                    ServiceLocator.cachedUpdateResult.value = result
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
