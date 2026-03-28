package com.cleanpic

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {

    @Test
    fun platformNameIsNotBlank() {
        val name = getPlatformName()
        assertTrue(name.isNotBlank(), "平台名称不应为空")
    }
}
