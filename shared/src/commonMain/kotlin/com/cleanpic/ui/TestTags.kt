package com.cleanpic.ui

import androidx.compose.ui.Modifier

/**
 * 在根布局启用「testTag 映射为平台可识别的资源 id」。
 *
 * Android 上通过 `testTagsAsResourceId = true` 把 Compose testTag 暴露成
 * resource-id，供 Maestro / UIAutomator 定位元素；
 * iOS / HarmonyOS 无此概念，actual 实现为 no-op。
 *
 * 抽成 expect/actual 是因为 `testTagsAsResourceId` 是 Android 独有 API，
 * 直接写在 commonMain 会导致 iOS / HarmonyOS target 编译失败。
 */
expect fun Modifier.enableTestTagsAsResourceId(): Modifier
