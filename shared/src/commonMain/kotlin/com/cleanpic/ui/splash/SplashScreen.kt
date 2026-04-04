package com.cleanpic.ui.splash

import androidx.compose.runtime.*
import com.cleanpic.theme.ThemeLayoutId
import com.cleanpic.theme.ThemeTokens
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(theme: ThemeTokens, onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
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
