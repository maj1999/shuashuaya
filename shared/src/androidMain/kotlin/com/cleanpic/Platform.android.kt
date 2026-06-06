package com.cleanpic

actual fun getPlatformName(): String = "Android"

actual fun currentEpochMillis(): Long = System.currentTimeMillis()
