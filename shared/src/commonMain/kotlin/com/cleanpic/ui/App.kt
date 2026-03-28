package com.cleanpic.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.cleanpic.di.ServiceLocator
import com.cleanpic.model.MediaType
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
    val navController = rememberNavController()
    val viewerViewModel = remember { ViewerViewModel() }

    NavHost(navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(theme) {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
        composable("home") {
            HomeScreen(navController, theme, viewerViewModel)
        }
        composable("viewer/{type}") { entry ->
            val type = MediaType.valueOf(
                entry.arguments?.getString("type") ?: "PHOTO"
            )
            ViewerScreen(navController, theme, viewerViewModel, type)
        }
        composable("result") {
            ResultScreen(navController, theme, viewerViewModel)
        }
        composable("settings") {
            SettingsScreen(navController, theme)
        }
    }
}
