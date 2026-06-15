package com.cleanpic.log

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 把 filesDir/logs 下的日志拼装并生成带时间戳的导出文件名。 */
object LogExporter {

    /** 导出文件名：刷刷鸭-日志-yyyyMMdd-HHmmss.log，时间戳取点击导出时刻。 */
    fun exportFileName(nowMillis: Long): String {
        val fmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return "刷刷鸭-日志-${fmt.format(Date(nowMillis))}.log"
    }

    /** 拼装日志内容：历史(app.log.1) 在前，当前(app.log) 在后；都不存在则空串。 */
    fun collect(logsDir: File): String {
        val sb = StringBuilder()
        File(logsDir, "app.log.1").takeIf { it.exists() }?.let { sb.append(it.readText()) }
        File(logsDir, "app.log").takeIf { it.exists() }?.let { sb.append(it.readText()) }
        return sb.toString()
    }
}
