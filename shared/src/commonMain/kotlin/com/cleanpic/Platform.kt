package com.cleanpic

import com.cleanpic.model.StorageInfo

/**
 * 平台标识接口
 *
 * 各平台 (Android / iOS / HarmonyOS) 提供具体实现，
 * 用于在共享代码中获取运行时平台信息。
 */
expect fun getPlatformName(): String

/** 当前 epoch 毫秒时间戳（用于浏览记忆的天数新鲜度）。 */
expect fun currentEpochMillis(): Long

/** 设备本地日期，格式 "yyyy-MM-dd"。 */
expect fun currentLocalDate(): String

/** 把 epoch 毫秒转设备本地 "yyyy-MM-dd"（用于连续清理 streak 判断）。 */
expect fun epochToLocalDate(millis: Long): String

/** 设备存储现状（主分区）。 */
expect fun deviceStorage(): StorageInfo
