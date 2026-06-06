package com.cleanpic

actual fun getPlatformName(): String = "HarmonyOS"

actual fun currentEpochMillis(): Long = kotlin.system.getTimeMillis()
