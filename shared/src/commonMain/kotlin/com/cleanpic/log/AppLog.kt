package com.cleanpic.log

import co.touchlab.kermit.Logger
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.cleanpic.model.MediaType

/** 取一个带 tag 的 logger。约定：各模块 private val log = logger("ModuleTag")。 */
fun logger(tag: String): Logger = Logger.withTag(tag)

/**
 * 全局日志配置。在平台初始化（ServiceLocator.initialize）时调用一次。
 * - debug: 控制最低级别。debug=true → Verbose 全开；false → 仅 Warn/Error。
 * - extraWriters: 平台注入的额外 writer（如 Android 的 FileLogWriter）。
 */
object LogConfig {
    fun init(debug: Boolean, extraWriters: List<LogWriter> = emptyList()) {
        Logger.setLogWriters(listOf(platformLogWriter()) + extraWriters)
        Logger.setMinSeverity(if (debug) Severity.Verbose else Severity.Warn)
    }
}

/** 脱敏：数量直接输出，不含任何内容。 */
fun redactCount(n: Int): String = n.toString()

/** 脱敏：仅输出媒体类型枚举名，不含文件名/路径/URI。 */
fun redactType(type: MediaType): String = type.name
