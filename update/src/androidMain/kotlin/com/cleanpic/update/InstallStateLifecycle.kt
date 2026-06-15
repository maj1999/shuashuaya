package com.cleanpic.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 修复「从系统安装器返回后仍卡在『正在启动安装器』」。
 *
 * 触发安装会 `startActivity` 拉起系统安装器，此时下载状态停在 [DownloadState.INSTALLING]。
 * 该状态原本只在「安装成功、App 进程被替换」时消失；用户在安装器点取消/返回时，
 * 没有任何回调清理它，导致全屏的 InstallingDialog 永远挂着挡住 App。
 *
 * 本组件监听宿主生命周期：App 回到前台（[Lifecycle.Event.ON_RESUME]）时若仍处于 INSTALLING，
 * 说明是从安装器返回而非安装成功 → 重置状态撤掉遮罩。用户如仍想安装可再次点击更新
 * （此时本地包已校验通过，会直接复用安装，见 [ApkIntegrity.canReuseLocalApk]）。
 *
 * [leftForeground] 守卫：仅在「确实离开过前台再回来」时才重置。addObserver 会把观察者
 * 补齐到当前状态（RESUMED 时会立即回放一次 ON_RESUME），不加守卫会在组合瞬间误重置。
 */
@Composable
fun ResetInstallStateOnResume(installer: UpdateInstaller) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var leftForeground = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> leftForeground = true
                Lifecycle.Event.ON_RESUME -> {
                    if (leftForeground && installer.downloadState.value == DownloadState.INSTALLING) {
                        installer.resetState()
                    }
                    leftForeground = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
