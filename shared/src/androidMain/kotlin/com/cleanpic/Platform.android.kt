package com.cleanpic

import android.os.Environment
import android.os.StatFs
import com.cleanpic.model.StorageInfo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

actual fun getPlatformName(): String = "Android"

actual fun currentEpochMillis(): Long = System.currentTimeMillis()

actual fun currentLocalDate(): String = LocalDate.now().toString()  // ISO "yyyy-MM-dd"

actual fun epochToLocalDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

actual fun deviceStorage(): StorageInfo {
    val stat = StatFs(Environment.getDataDirectory().path)
    val total = stat.blockCountLong * stat.blockSizeLong
    val available = stat.availableBlocksLong * stat.blockSizeLong
    return StorageInfo(totalBytes = total, availableBytes = available)
}
