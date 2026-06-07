package com.cleanpic

import com.cleanpic.model.StorageInfo
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

actual fun getPlatformName(): String = "iOS"

actual fun currentEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

private fun ymd(date: NSDate): String {
    val f = NSDateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    return f.stringFromDate(date)
}

actual fun currentLocalDate(): String = ymd(NSDate())

actual fun epochToLocalDate(millis: Long): String =
    ymd(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))

actual fun deviceStorage(): StorageInfo {
    val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath("/", null)
    val total = (attrs?.get(NSFileSystemSize) as? Long) ?: 0L
    val free = (attrs?.get(NSFileSystemFreeSize) as? Long) ?: 0L
    return StorageInfo(totalBytes = total, availableBytes = free)
}
