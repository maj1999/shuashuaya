package com.cleanpic.ui

import androidx.compose.runtime.Composable

/**
 * 宿主可注入的钩子。shared 模块不感知具体功能，由各 flavor 注入实现。
 *
 * 设计原则：方法名严格泛型，不出现 "update" "升级" 等具体功能字样，
 * 避免商店渠道 APK 通过 shared 字节码暴露功能名称。
 */
interface AppHooks {
    /** Splash 启动时调用，宿主可用于发起后台任务 */
    fun onAppStart() = Unit

    /** 首页叠加层（弹窗、下载 overlay 等） */
    @Composable
    fun HomeOverlay() = Unit

    /** 设置页额外区块 */
    @Composable
    fun SettingsExtras() = Unit

    companion object {
        val Empty: AppHooks = object : AppHooks {}
    }
}
