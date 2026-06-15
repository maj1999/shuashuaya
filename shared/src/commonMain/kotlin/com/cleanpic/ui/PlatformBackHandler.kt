package com.cleanpic.ui

import androidx.compose.runtime.Composable

/**
 * 处理系统返回手势 / 返回键。
 *
 * [enabled] = true 时拦截返回并调用 [onBack]（用于 App 内逐级返回，而非直接退出）；
 * = false 时放行给系统默认处理（如已在首页则退出 App）。
 *
 * Android 用 `androidx.activity.compose.BackHandler` 接入 OnBackPressedDispatcher；
 * 多个 [PlatformBackHandler] 嵌套时，组合树中更内层（更晚组合）的已启用者优先处理，
 * 故子界面的局部返回（如退出全屏叠层）可压过外层的路由返回。
 * iOS / HarmonyOS 无统一返回键概念，actual 为 no-op。
 *
 * 抽成 expect/actual 是因为 BackHandler 是 Android 独有 API，写在 commonMain 会令其他 target 编译失败。
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
