package com.cleanpic.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 把日志滚动写入 dir/app.log，超过 maxBytes 轮转为 app.log.1（保留 1 份历史，总占用 ≤ 2*maxBytes）。
 * 注意：≤ 2*maxBytes 的上限假设单行字节数小于 maxBytes。
 * 轮转使用 File.renameTo（best-effort）：若 renameTo 失败，当前文件继续追加直到下次触发时重试，不抛异常。
 * 线程安全：写操作整体加锁。仅落脱敏内容（调用方保证不传 PII）。
 */
class FileLogWriter(
    private val dir: File,
    private val maxBytes: Long = 1_000_000L
) : LogWriter() {

    private val lock = Any()
    private val current: File get() = File(dir, "app.log")
    private val rotated: File get() = File(dir, "app.log.1")
    // SimpleDateFormat 非线程安全；格式化在 synchronized(lock) 内完成，安全。
    private val sdf = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        synchronized(lock) {
            if (!dir.exists()) dir.mkdirs()
            val timestamp = sdf.format(Date(System.currentTimeMillis()))
            val line = buildString {
                append(timestamp)
                append(' ')
                append(severity.name.first())
                append('/')
                append(tag)
                append(": ")
                append(message)
                if (throwable != null) {
                    append(" | ")
                    append(throwable::class.simpleName)
                    append(": ")
                    append(throwable.message ?: "")
                }
                append('\n')
            }
            // 编码一次；用字节数做容量检测，对 CJK（每字符 3 字节 UTF-8）准确。
            val bytes = line.toByteArray(Charsets.UTF_8)
            val f = current
            if (f.exists() && f.length() + bytes.size > maxBytes) {
                // best-effort rotation：renameTo 失败则当前文件继续追加，不抛异常。
                if (rotated.exists()) rotated.delete()
                f.renameTo(rotated)
            }
            current.appendBytes(bytes)
        }
    }
}
