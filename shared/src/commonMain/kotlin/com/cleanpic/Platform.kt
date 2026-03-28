package com.cleanpic

/**
 * 平台标识接口
 *
 * 各平台 (Android / iOS / HarmonyOS) 提供具体实现，
 * 用于在共享代码中获取运行时平台信息。
 */
expect fun getPlatformName(): String
