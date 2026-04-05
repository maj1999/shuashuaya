package com.cleanpic.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsTest {
    @Test fun defaults_are_correct() {
        val settings = InMemoryAppSettings()
        assertEquals("warm", settings.theme)
        assertEquals("carousel", settings.interactionMode)
        assertEquals(10, settings.roundCount)
    }

    @Test fun write_and_read() {
        val settings = InMemoryAppSettings()
        settings.roundCount = 20
        assertEquals(20, settings.roundCount)
    }

    @Test fun invalid_round_count_falls_back() {
        val settings = InMemoryAppSettings()
        settings.roundCount = 99
        assertEquals(10, settings.roundCount)
    }

    @Test fun auto_check_update_defaults_to_true() {
        val settings = InMemoryAppSettings()
        assertEquals(true, settings.autoCheckUpdate)
    }

    @Test fun auto_check_update_write_and_read() {
        val settings = InMemoryAppSettings()
        settings.autoCheckUpdate = false
        assertEquals(false, settings.autoCheckUpdate)
    }
}
