package com.cleanpic.ui

import androidx.compose.runtime.Composable

/** HarmonyOS 返回交互由系统/导航容器处理，此处 no-op。 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
