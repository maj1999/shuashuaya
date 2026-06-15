package com.cleanpic.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.cleanpic.model.MediaType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class RecordingWriter : LogWriter() {
    val entries = mutableListOf<Pair<Severity, String>>()
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        entries.add(severity to message)
    }
}

class AppLogTest {
    @AfterTest
    fun tearDown() {
        LogConfig.init(debug = true, extraWriters = emptyList())
    }

    @Test
    fun release_filters_below_warn() {
        val rec = RecordingWriter()
        LogConfig.init(debug = false, extraWriters = listOf(rec))
        val log = logger("T")
        log.i { "info-msg" }
        log.w { "warn-msg" }
        log.e { "err-msg" }
        val severities = rec.entries.map { it.first }
        assertFalse(severities.contains(Severity.Info), "release 不应记录 Info")
        assertTrue(severities.contains(Severity.Warn))
        assertTrue(severities.contains(Severity.Error))
    }

    @Test
    fun debug_records_verbose() {
        val rec = RecordingWriter()
        LogConfig.init(debug = true, extraWriters = listOf(rec))
        logger("T").v { "verbose-msg" }
        assertTrue(rec.entries.any { it.first == Severity.Verbose })
    }

    @Test
    fun redact_count_outputs_number_only() {
        assertEquals("5", redactCount(5))
    }

    @Test
    fun redact_type_outputs_enum_name_not_content() {
        val out = redactType(MediaType.PHOTO)
        assertEquals("PHOTO", out)
    }
}
