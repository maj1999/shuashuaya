package com.cleanpic.android.wiring

import android.content.Context
import com.cleanpic.ui.AppHooks

/**
 * store flavor 的升级钩子接线 — 空实现。
 * 此文件不引用 com.cleanpic.update 任何符号，确保 store APK 不含升级类。
 */
object UpdateWiring {
    fun provideHooks(context: Context): AppHooks = AppHooks.Empty
}
