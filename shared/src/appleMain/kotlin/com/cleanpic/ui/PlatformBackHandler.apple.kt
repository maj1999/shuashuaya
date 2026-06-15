package com.cleanpic.ui

import androidx.compose.runtime.Composable

/** iOS 无统一系统返回键，返回交互由各自导航处理，此处 no-op。 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
