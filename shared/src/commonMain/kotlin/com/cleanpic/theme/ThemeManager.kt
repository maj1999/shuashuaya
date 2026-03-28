package com.cleanpic.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeManager {
    val allThemes = listOf(
        DreamyGradientTheme,
        SoftMinimalTheme,
        CutePlayfulTheme,
        ElegantDarkTheme,
        NaturalWarmTheme
    )

    private val _currentTheme = MutableStateFlow(DreamyGradientTheme)
    val currentTheme: StateFlow<ThemeTokens> = _currentTheme

    fun switchTheme(id: String) {
        allThemes.find { it.id == id }?.let { _currentTheme.value = it }
    }
}
