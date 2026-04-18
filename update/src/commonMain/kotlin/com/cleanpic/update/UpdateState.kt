package com.cleanpic.update

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 升级模块会话级状态。
 * 取代原 ServiceLocator.cachedUpdateResult，使 shared 不再持有升级相关状态。
 */
object UpdateState {
    val cachedResult = MutableStateFlow(UpdateCheckResult(UpdateStatus.UP_TO_DATE))
}
