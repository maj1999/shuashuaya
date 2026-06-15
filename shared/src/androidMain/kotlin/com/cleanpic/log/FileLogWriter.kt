package com.cleanpic.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import java.io.File

/**
 * 把日志滚动写入 dir/app.log，超过 maxBytes 轮转为 app.log.1（保留 1 份历史，总占用 ≤ 2*maxBytes）。
 * 线程安全：写操作整体加锁。仅落脱敏内容（调用方保证不传 PII）。
 */
class FileLogWriter(
    private val dir: File,
    private val maxBytes: Long = 1_000_000L
) : LogWriter() {

    private val lock = Any()
    private val current: File get() = File(dir, "app.log")
    private val rotated: File get() = File(dir, "app.log.1")

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        synchronized(lock) {
            if (!dir.exists()) dir.mkdirs()
            val line = buildString {
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
            val f = current
            if (f.exists() && f.length() + line.length > maxBytes) {
                if (rotated.exists()) rotated.delete()
                f.renameTo(rotated)
            }
            current.appendText(line)
        }
    }
}
