package com.cleanpic.ui.settings

import androidx.compose.runtime.*
import com.cleanpic.log.LogExportController
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.AppHooks
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    router: AppRouter,
    theme: ThemeTokens,
    hooks: AppHooks = AppHooks.Empty
) {
    val viewModel = remember { SettingsViewModel() }
    var selectedTheme by remember { mutableStateOf(viewModel.currentThemeId) }
    var selectedMode by remember { mutableStateOf(viewModel.currentMode) }
    var selectedCount by remember { mutableStateOf(viewModel.currentRoundCount) }

    val state = SettingsScreenState(
        theme = theme,
        allThemes = viewModel.allThemes,
        currentMode = selectedMode.id,
        currentCount = selectedCount,
        onThemeChange = { id ->
            selectedTheme = id
            viewModel.switchTheme(id)
        },
        onModeChange = { modeId ->
            val mode = com.cleanpic.model.InteractionMode.fromId(modeId)
            selectedMode = mode
            viewModel.switchInteractionMode(mode)
        },
        onCountChange = { count ->
            selectedCount = count
            viewModel.setRoundCount(count)
        },
        onResetHistory = { viewModel.resetBrowsingHistory() },
        onExportLogs = { LogExportController.export() },
        onBack = { router.popBackStack() },
        extras = { hooks.SettingsExtras() }
    )

    SharedSettingsLayout(state)
}
