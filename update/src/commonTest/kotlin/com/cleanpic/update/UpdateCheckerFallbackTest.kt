package com.cleanpic.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * U-UPD-08：检查端点超时与双端点 fallback（方案 §12 #1/#5）。
 * 用 MockEngine + HttpTimeout 验证主端点失败/超时时回退到兜底端点，且端点为完整 URL 直用。
 */
class UpdateCheckerFallbackTest {

    private val primary = "https://primary.example/update/version.json"
    private val fallback = "https://fallback.example/api/version"

    // 远端 9.9.9 > 本地 1.0.0 → OPTIONAL_UPDATE
    private val validJson =
        """{"android":{"version":"9.9.9","minVersion":"1.0.0","downloadUrl":"https://x/a.apk"}}"""

    private fun checker(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData
    ): UpdateChecker {
        val http = HttpClient(MockEngine(handler)) {
            install(HttpTimeout) {
                requestTimeoutMillis = 100
                connectTimeoutMillis = 100
                socketTimeoutMillis = 100
            }
        }
        return UpdateChecker(listOf(primary, fallback), http)
    }

    @Test
    fun primary_success_skips_fallback() = runTest {
        var fallbackHit = false
        val c = checker { req ->
            if (req.url.toString() == primary) {
                respond(validJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
            } else {
                fallbackHit = true
                respond("{}", HttpStatusCode.OK)
            }
        }
        val r = c.checkForUpdate(currentVersion = "1.0.0", currentVersionCode = 1, platform = "Android")
        assertEquals(UpdateStatus.OPTIONAL_UPDATE, r.status)
        assertEquals(false, fallbackHit) // 主端点成功就不该碰兜底
    }

    @Test
    fun fallback_used_when_primary_returns_garbage() = runTest {
        val c = checker { req ->
            if (req.url.toString() == primary) {
                respond("<html>登录验证</html>", HttpStatusCode.OK) // 防盗链伪装页 → 解析失败
            } else {
                respond(validJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
            }
        }
        val r = c.checkForUpdate(currentVersion = "1.0.0", currentVersionCode = 1, platform = "Android")
        assertEquals(UpdateStatus.OPTIONAL_UPDATE, r.status)
    }

    @Test
    fun fallback_used_when_primary_times_out() = runTest {
        val c = checker { req ->
            if (req.url.toString() == primary) {
                delay(10_000) // 远超 requestTimeoutMillis=100 → HttpTimeout 抛异常
                respond(validJson, HttpStatusCode.OK)
            } else {
                respond(validJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
            }
        }
        val r = c.checkForUpdate(currentVersion = "1.0.0", currentVersionCode = 1, platform = "Android")
        assertEquals(UpdateStatus.OPTIONAL_UPDATE, r.status) // 超时后兜底成功
    }

    @Test
    fun both_endpoints_fail_returns_up_to_date() = runTest {
        val c = checker { _ -> respond("not json", HttpStatusCode.InternalServerError) }
        val r = c.checkForUpdate(currentVersion = "1.0.0", currentVersionCode = 1, platform = "Android")
        assertEquals(UpdateStatus.UP_TO_DATE, r.status) // 不挂起、安全降级
    }

    @Test
    fun endpoint_full_url_used_verbatim() = runTest {
        var requestedPrimary: String? = null
        val c = checker { req ->
            if (req.url.toString() == primary) {
                requestedPrimary = req.url.toString()
                respond(validJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
            } else {
                respond("{}", HttpStatusCode.OK)
            }
        }
        c.checkForUpdate(currentVersion = "1.0.0", currentVersionCode = 1, platform = "Android")
        // 端点是完整 URL，不应被追加 /api/version
        assertEquals(primary, requestedPrimary)
    }
}
