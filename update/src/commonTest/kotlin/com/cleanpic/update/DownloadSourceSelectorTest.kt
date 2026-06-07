package com.cleanpic.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 选源竞速：并发探测候选下载源，自动选用当前网络下最快可达的源。
 * 替代"判断国内/境外地域"——用实际连通性说话，天然适配国内直连 / 境外梯子等任意网络。
 */
class DownloadSourceSelectorTest {

    private val gitee = "https://gitee.example/app.apk"
    private val github = "https://github.example/app.apk"

    private fun client(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData
    ) = HttpClient(MockEngine(handler)) {
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 5_000
        }
    }

    @Test
    fun returns_reachable_source_skipping_failed_one() = runTest {
        val http = client { req ->
            if (req.url.toString() == gitee) respondError(HttpStatusCode.InternalServerError)
            else respond("ok", HttpStatusCode.PartialContent)
        }
        val chosen = selectFastestSource(http, listOf(gitee, github))
        assertEquals(github, chosen)
    }

    @Test
    fun returns_fastest_when_both_reachable() = runTest {
        val http = client { req ->
            if (req.url.toString() == gitee) {
                delay(5_000)              // 慢源（境外卡 Gitee）
                respond("ok", HttpStatusCode.PartialContent)
            } else {
                delay(50)                 // 快源（梯子下 GitHub 快）
                respond("ok", HttpStatusCode.PartialContent)
            }
        }
        val chosen = selectFastestSource(http, listOf(gitee, github))
        assertEquals(github, chosen) // 竞速：最先成功的优先，而非列表顺序
    }

    @Test
    fun returns_null_when_all_fail() = runTest {
        val http = client { respondError(HttpStatusCode.NotFound) }
        val chosen = selectFastestSource(http, listOf(gitee, github))
        assertNull(chosen)
    }

    @Test
    fun probe_succeeds_when_server_ignores_range() = runTest {
        // 真实 Gitee CDN 不支持 Range：忽略 Range、对探测直接返回 200 + 完整 body。
        // 探测须基于响应头判定连通（只读 header、不下整包），不能依赖服务器 Range 支持。
        val full = ByteArray(64 * 1024) { it.toByte() }
        val http = client {
            respond(full, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, full.size.toString()))
        }
        val chosen = selectFastestSource(http, listOf(gitee))
        assertEquals(gitee, chosen)
    }
}
