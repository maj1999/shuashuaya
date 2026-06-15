package com.cleanpic.media

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.cleanpic.log.LogConfig
import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private class Rec : LogWriter() {
    val errors = mutableListOf<String>()
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (severity == Severity.Error) errors.add(message)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeleteLauncherLoggingTest {

    private val rec = Rec()

    @Before
    fun setup() {
        AndroidMediaRepository.deleteLauncher = null
        LogConfig.init(debug = true, extraWriters = listOf(rec))
    }

    @After
    fun tearDown() {
        AndroidMediaRepository.deleteLauncher = null
        LogConfig.init(debug = false, extraWriters = emptyList())
    }

    @Test
    fun logs_error_when_launcher_missing() = runBlocking {
        val repo = AndroidMediaRepository(RuntimeEnvironment.getApplication())
        val items = listOf(
            MediaItem(
                id = "1",
                type = MediaType.PHOTO,
                name = "x",
                size = 1L,
                date = 0L,
                width = 1,
                height = 1
            )
        )
        val result = repo.deleteMediaItems(items)
        assertTrue("应返回失败", result.isFailure)
        assertTrue("应打出 ERROR 日志", rec.errors.any { it.contains("deleteLauncher") })
    }
}
