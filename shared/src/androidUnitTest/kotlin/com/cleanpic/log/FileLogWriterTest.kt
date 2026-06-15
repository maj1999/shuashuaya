package com.cleanpic.log

import co.touchlab.kermit.Severity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class FileLogWriterTest {

    @Test
    fun writes_log_line_to_file() {
        val dir = Files.createTempDirectory("logtest").toFile()
        val writer = FileLogWriter(dir, maxBytes = 1024)
        writer.log(Severity.Warn, "hello-line", "Tag", null)
        val logFile = File(dir, "app.log")
        assertTrue("日志文件应存在", logFile.exists())
        assertTrue("应包含写入内容", logFile.readText().contains("hello-line"))
        assertTrue("应包含 tag", logFile.readText().contains("Tag"))
        assertTrue("应包含 severity 前缀", logFile.readText().contains("W/"))
    }

    @Test
    fun rotates_when_exceeding_max_bytes() {
        val dir = Files.createTempDirectory("logtest").toFile()
        val writer = FileLogWriter(dir, maxBytes = 200)
        repeat(50) { writer.log(Severity.Warn, "padding-line-$it-xxxxxxxxxx", "T", null) }
        val current = File(dir, "app.log")
        val rotated = File(dir, "app.log.1")
        assertTrue("轮转后应存在历史文件", rotated.exists())
        assertTrue("当前文件不应超过上限太多", current.length() <= 200 + 256)
    }
}
