package com.cleanpic.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppHooksTest {

    @Test
    fun empty_onAppStart_does_not_throw() {
        AppHooks.Empty.onAppStart()
    }

    @Test
    fun empty_is_singleton() {
        assertSame(AppHooks.Empty, AppHooks.Empty)
    }

    @Test
    fun custom_implementation_can_override_onAppStart() {
        var triggered = 0
        val hooks = object : AppHooks {
            override fun onAppStart() { triggered++ }
        }
        hooks.onAppStart()
        hooks.onAppStart()
        assertEquals(2, triggered)
    }
}
