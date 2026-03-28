package com.cleanpic.viewmodel

import com.cleanpic.di.ServiceLocator
import com.cleanpic.model.InteractionMode

class SettingsViewModel {
    private val settings get() = ServiceLocator.appSettings
    private val themeManager get() = ServiceLocator.themeManager

    val allThemes get() = themeManager.allThemes
    val currentThemeId get() = settings.theme
    val currentMode get() = InteractionMode.fromId(settings.interactionMode)
    val currentRoundCount get() = settings.roundCount

    fun switchTheme(id: String) {
        settings.theme = id
        themeManager.switchTheme(id)
    }

    fun switchInteractionMode(mode: InteractionMode) {
        settings.interactionMode = mode.id
    }

    fun setRoundCount(count: Int) {
        settings.roundCount = count
    }
}
