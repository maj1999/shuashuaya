package com.cleanpic.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.File
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Android 下载栈改造（DownloadManager → app 内 ktor）。
 * 用 MockEngine + 临时文件验证：选源竞速 + 流式下载 + 完整性校验 + 失败切源 + 进度回调。
 * 纯 JVM，无需 Robolectric / 真实网络。
 */
class ApkDownloaderTest {

    private val temp = mutableListOf<File>()
    private val gitee = "https://gitee.example/app.apk"
    private val github = "https://github.example/app.apk"

    // 合法 APK：ZIP 魔数 + 内容
    private val apkBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04) + ByteArray(2048) { (it % 251).toByte() }
    private val htmlBytes = "<html>风控验证</html>".toByteArray()

    private fun sha256(b: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(b)
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private fun dest() = File.createTempFile("apkdl-", ".apk").also { temp += it }

    /** Range 请求 = 探测；无 Range = 全量下载。fullBody 决定该源全量下载返回什么。 */
    private fun client(fullBody: (String) -> Pair<ByteArray, HttpStatusCode>?) =
        HttpClient(MockEngine { req ->
            val url = req.url.toString()
            val body = fullBody(url)
            when {
                body == null -> respondError(HttpStatusCode.NotFound)
                req.headers[HttpHeaders.Range] != null ->
                    respond(body.first.copyOfRange(0, 2), HttpStatusCode.PartialContent)
                else -> respond(
                    body.first, body.second,
                    headersOf(HttpHeaders.ContentLength, body.first.size.toString())
                )
            }
        })

    @AfterTest
    fun cleanup() { temp.forEach { it.delete() }; temp.clear() }

    @Test
    fun downloads_and_verifies_valid_apk() = runTest {
        val http = client { apkBytes to HttpStatusCode.OK }
        val out = dest()
        val ok = downloadApk(http, listOf(gitee), out, sha256(apkBytes), apkBytes.size.toLong()) {}
        assertTrue(ok)
        assertTrue(out.readBytes().contentEquals(apkBytes), "下载内容应与源字节一致")
    }

    @Test
    fun falls_back_to_next_source_when_first_unreachable() = runTest {
        // gitee 整体不可达（探测+下载都 404），github 正常 → 应自动用 github 下成
        val http = client { url -> if (url == github) apkBytes to HttpStatusCode.OK else null }
        val out = dest()
        val ok = downloadApk(http, listOf(gitee, github), out, sha256(apkBytes), apkBytes.size.toLong()) {}
        assertTrue(ok)
        assertTrue(out.readBytes().contentEquals(apkBytes))
    }

    @Test
    fun fails_when_all_sources_return_disguised_html() = runTest {
        // 探测都能成功，但全量下载返回 HTML 伪装页 → 完整性校验挡下 → 全部失败
        val http = client { htmlBytes to HttpStatusCode.OK }
        val out = dest()
        val ok = downloadApk(http, listOf(gitee, github), out, "", 0) {}
        assertFalse(ok)
    }

    @Test
    fun reports_progress_up_to_complete() = runTest {
        val http = client { apkBytes to HttpStatusCode.OK }
        val out = dest()
        val seen = mutableListOf<Float>()
        downloadApk(http, listOf(gitee), out, sha256(apkBytes), apkBytes.size.toLong()) { seen += it }
        assertTrue(seen.isNotEmpty(), "应有进度回调")
        assertEquals(1f, seen.last(), "下载完成进度应到 1.0")
    }
}
