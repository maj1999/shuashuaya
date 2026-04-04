package com.cleanpic.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeManager {
    val allThemes = listOf(
        WarmTheme
        // Plan C 将添加：MinimalTheme、GeometricTheme、PlayfulTheme、EditorialTheme
    )

    private val _currentTheme = MutableStateFlow(WarmTheme)
    val currentTheme: StateFlow<ThemeTokens> = _currentTheme

    fun switchTheme(id: String) {
        val target = allThemes.find { it.id == id }
        _currentTheme.value = target ?: WarmTheme
    }
}
