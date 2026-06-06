package com.cleanpic

/**
 * 平台标识接口
 *
 * 各平台 (Android / iOS / HarmonyOS) 提供具体实现，
 * 用于在共享代码中获取运行时平台信息。
 */
expect fun getPlatformName(): String

/** 当前 epoch 毫秒时间戳（用于浏览记忆的天数新鲜度）。 */
expect fun currentEpochMillis(): Long
