package com.cleanpic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.cleanpic.di.ServiceLocator
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.ui.navigation.Route
import com.cleanpic.ui.navigation.rememberAppRouter
import com.cleanpic.ui.splash.SplashScreen
import com.cleanpic.ui.home.HomeScreen
import com.cleanpic.ui.viewer.ViewerScreen
import com.cleanpic.ui.result.ResultScreen
import com.cleanpic.ui.settings.SettingsScreen
import com.cleanpic.ui.stats.StatsScreen
import com.cleanpic.viewmodel.ViewerViewModel

@Composable
fun CleanPicApp(hooks: AppHooks = AppHooks.Empty) {
    val themeManager = ServiceLocator.themeManager
    val theme by themeManager.currentTheme.collectAsState()
    val router = rememberAppRouter()
    val viewerViewModel = remember { ViewerViewModel() }

    Box(modifier = Modifier.fillMaxSize().enableTestTagsAsResourceId()) {
        when (val route = router.currentRoute) {
            is Route.Splash -> SplashScreen(theme, hooks) {
                router.navigate(Route.Home, clearBackStackUpTo = Route.Splash, inclusive = true)
            }
            is Route.Home -> HomeScreen(router, theme, viewerViewModel)
            is Route.Viewer -> ViewerScreen(router, theme, viewerViewModel, route.type)
            is Route.Result -> ResultScreen(router, theme, viewerViewModel)
            is Route.Settings -> SettingsScreen(router, theme, hooks)
            is Route.Stats -> StatsScreen(router, theme)
        }
        // 跨路由 overlay（升级弹窗、下载进度等）——必须放在 when 之外，
        // 这样从设置页触发的下载进度 dialog 在 Settings 路由也可见。
        hooks.HomeOverlay()
    }
}
