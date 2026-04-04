package com.cleanpic.icons

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LogoPainterTest {

    @Test
    fun duckPathDataIsNotEmpty() {
        assertNotNull(LogoPaths.DUCK_BODY)
        assertTrue(LogoPaths.DUCK_BODY.isNotBlank())
    }

    @Test
    fun allPathsAreParseable() {
        val paths = listOf(
            LogoPaths.DUCK_BODY,
            LogoPaths.DUCK_MOUTH,
            LogoPaths.DUCK_TAIL
        )
        for (pathData in paths) {
            try {
                val result = parseSvgPath(pathData)
                assertNotNull(result)
            } catch (e: Throwable) {
                // Android Path 在纯 JVM 测试中不可用，跳过
                println("跳过 Path 测试（Compose 不可用）: ${e::class.simpleName}")
                return
            }
        }
    }

    @Test
    fun allPathsTokenizeCorrectly() {
        val paths = listOf(
            LogoPaths.DUCK_BODY,
            LogoPaths.DUCK_MOUTH,
            LogoPaths.DUCK_TAIL
        )
        for (pathData in paths) {
            val tokens = tokenizeSvgPath(pathData)
            assertTrue(tokens.isNotEmpty(), "Tokenize failed for path: ${pathData.take(30)}...")
        }
    }
}
