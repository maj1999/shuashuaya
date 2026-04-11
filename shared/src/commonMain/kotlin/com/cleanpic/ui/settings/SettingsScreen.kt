package com.cleanpic.ui.settings

import androidx.compose.runtime.*
import com.cleanpic.di.ServiceLocator
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.update.UpdateCheckResult
import com.cleanpic.update.UpdateStatus
import com.cleanpic.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(router: AppRouter, theme: ThemeTokens) {
    val viewModel = remember { SettingsViewModel() }
    var selectedTheme by remember { mutableStateOf(viewModel.currentThemeId) }
    var selectedMode by remember { mutableStateOf(viewModel.currentMode) }
    var selectedCount by remember { mutableStateOf(viewModel.currentRoundCount) }

    // 自动升级状态
    var autoCheckUpdate by remember { mutableStateOf(ServiceLocator.appSettings.autoCheckUpdate) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var checkResultMessage by remember { mutableStateOf<String?>(null) }
    var manualCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    val scope = rememberCoroutineScope()

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
        onBack = { router.popBackStack() },
        autoCheckUpdate = autoCheckUpdate,
        onAutoCheckUpdateChange = { enabled ->
            autoCheckUpdate = enabled
            ServiceLocator.appSettings.autoCheckUpdate = enabled
        },
        isCheckingUpdate = isCheckingUpdate,
        onCheckUpdate = {
            scope.launch {
                isCheckingUpdate = true
                checkResultMessage = null
                val checker = ServiceLocator.updateChecker
                if (checker == null) {
                    checkResultMessage = "更新检查未启用"
                    isCheckingUpdate = false
                    return@launch
                }
                try {
                    val result = checker.checkForUpdate()
                    ServiceLocator.cachedUpdateResult.value = result
                    manualCheckResult = result
                    checkResultMessage = when (result.status) {
                        UpdateStatus.UP_TO_DATE -> "已是最新版本"
                        UpdateStatus.OPTIONAL_UPDATE -> "发现新版本 v${result.updateInfo?.version}"
                        UpdateStatus.FORCE_UPDATE -> "发现新版本 v${result.updateInfo?.version}（需要更新）"
                    }
                } catch (_: Exception) {
                    checkResultMessage = "网络不可用，请稍后再试"
                }
                isCheckingUpdate = false
            }
        },
        updateCheckResult = manualCheckResult ?: ServiceLocator.cachedUpdateResult.value.let {
            if (it.status != UpdateStatus.UP_TO_DATE) it else null
        },
        checkResultMessage = checkResultMessage,
        onStartUpdate = {
            val result = manualCheckResult ?: ServiceLocator.cachedUpdateResult.value
            val info = result.updateInfo ?: return@SettingsScreenState
            ServiceLocator.updateInstaller?.startUpdate(info)
        },
        isDebugBuild = ServiceLocator.isDebugBuild,
        onSimulateDownload = {
            ServiceLocator.updateInstaller?.simulateDownload()
        }
    )

    SharedSettingsLayout(state)
}
