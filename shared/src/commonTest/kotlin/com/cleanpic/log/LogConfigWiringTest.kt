package com.cleanpic.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.cleanpic.di.ServiceLocator
import com.cleanpic.mock.MockMediaRepository
import com.cleanpic.mock.MockAppSettings
import com.cleanpic.mock.MockPermissionManager
import com.cleanpic.mock.MockVideoPlayer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class LogConfigWiringTest {
    private class RecordingWriter : LogWriter() {
        val msgs = mutableListOf<String>()
        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) { msgs.add(message) }
    }

    @AfterTest
    fun tearDown() {
        LogConfig.init(debug = false, extraWriters = emptyList())
    }

    @Test
    fun initialize_applies_log_writers() {
        val rec = RecordingWriter()
        ServiceLocator.isDebugBuild = true
        ServiceLocator.initialize(
            mediaRepo = MockMediaRepository(),
            settings = MockAppSettings(),
            permission = MockPermissionManager(),
            player = MockVideoPlayer(),
            logWriters = listOf(rec)
        )
        logger("WireTest").i { "wired-ok" }
        assertTrue(rec.msgs.any { it == "wired-ok" }, "ServiceLocator.initialize 应把 logWriters 接入全局 logger")
    }
}
