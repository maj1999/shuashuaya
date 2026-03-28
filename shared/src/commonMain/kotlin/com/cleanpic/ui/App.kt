package com.cleanpic.ui

import androidx.compose.runtime.*
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
import com.cleanpic.viewmodel.ViewerViewModel

@Composable
fun CleanPicApp() {
    val themeManager = ServiceLocator.themeManager
    val theme by themeManager.currentTheme.collectAsState()
    val router = rememberAppRouter()
    val viewerViewModel = remember { ViewerViewModel() }

    when (val route = router.currentRoute) {
        is Route.Splash -> SplashScreen(theme) {
            router.navigate(Route.Home, clearBackStackUpTo = Route.Splash, inclusive = true)
        }
        is Route.Home -> HomeScreen(router, theme, viewerViewModel)
        is Route.Viewer -> ViewerScreen(router, theme, viewerViewModel, route.type)
        is Route.Result -> ResultScreen(router, theme, viewerViewModel)
        is Route.Settings -> SettingsScreen(router, theme)
    }
}
