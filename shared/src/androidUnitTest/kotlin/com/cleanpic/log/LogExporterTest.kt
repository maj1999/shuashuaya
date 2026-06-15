package com.cleanpic.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Calendar
import java.util.TimeZone

class LogExporterTest {

    @Test
    fun filename_contains_timestamp() {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            clear(); set(2026, Calendar.JUNE, 15, 15, 30, 12)
        }
        val name = LogExporter.exportFileName(cal.timeInMillis)
        assertEquals("刷刷鸭-日志-20260615-153012.log", name)
    }

    @Test
    fun collect_concatenates_current_and_rotated() {
        val dir = Files.createTempDirectory("exp").toFile()
        File(dir, "app.log.1").writeText("OLD\n")
        File(dir, "app.log").writeText("NEW\n")
        val content = LogExporter.collect(dir)
        assertTrue(content.indexOf("OLD") < content.indexOf("NEW"))
    }

    @Test
    fun collect_empty_when_no_files() {
        val dir = Files.createTempDirectory("exp2").toFile()
        assertEquals("", LogExporter.collect(dir))
    }

    @Test
    fun collect_only_current_when_no_rotated() {
        val dir = Files.createTempDirectory("exp3").toFile()
        File(dir, "app.log").writeText("ONLY-CURRENT\n")
        assertEquals("ONLY-CURRENT\n", LogExporter.collect(dir))
    }

    @Test
    fun collect_only_rotated_when_no_current() {
        val dir = Files.createTempDirectory("exp4").toFile()
        File(dir, "app.log.1").writeText("ONLY-ROTATED\n")
        assertEquals("ONLY-ROTATED\n", LogExporter.collect(dir))
    }
}
