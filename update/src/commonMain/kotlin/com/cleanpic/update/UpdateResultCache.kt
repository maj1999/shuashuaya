package com.cleanpic.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 会话级缓存：最近一次升级检查结果。
 *
 * - 由 SplashScreen / SettingsScreen 中的"检查更新"逻辑写入。
 * - 由 Home 弹窗、设置页"有新版本"提示读取。
 * - MutableStateFlow 本身线程安全，可跨协程读写。
 * - 进程销毁后状态丢失（无持久化），符合"会话级"语义。
 */
object UpdateResultCache {
    private val _value = MutableStateFlow(UpdateCheckResult(UpdateStatus.UP_TO_DATE))
    val value: StateFlow<UpdateCheckResult> = _value.asStateFlow()

    fun update(result: UpdateCheckResult) {
        _value.value = result
    }
}
