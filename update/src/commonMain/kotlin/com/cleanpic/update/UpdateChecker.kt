package com.cleanpic.update

import com.cleanpic.AppInfo
import com.cleanpic.getPlatformName
import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

/**
 * 版本检查。支持多端点按序回退：
 * - endpoints 中每个元素是**指向版本 JSON 的完整 URL**（如 Gitee raw 的 version.json，
 *   或 Worker 的 `.../api/version`）。主端点失败/超时则尝试下一个，全失败返回 UP_TO_DATE。
 * - 国内主端点走 Gitee raw、海外兜底走 Worker，由调用方（UpdateWiring）拼好完整 URL 传入。
 */
class UpdateChecker internal constructor(
    private val endpoints: List<String>,
    private val httpClient: HttpClient   // internal：仅测试注入 MockEngine；不暴露给无 ktor 依赖的 androidApp
) {
    /** androidApp 入口：多端点，按序回退。签名不含 HttpClient，避免下游模块需要 ktor。 */
    constructor(endpoints: List<String>) : this(endpoints, createDefaultClient())

    /** 兼容旧用法：单一 Worker base，内部补 `/api/version`。 */
    constructor(apiUrl: String) : this(listOf("$apiUrl/api/version"), createDefaultClient())

    suspend fun checkForUpdate(
        currentVersion: String = AppInfo.VERSION,
        currentVersionCode: Int? = AppInfo.VERSION_CODE,
        platform: String = getPlatformName()
    ): UpdateCheckResult {
        for (url in endpoints) {
            val response = tryFetch(url) ?: continue   // 超时/异常 → 试下一个端点
            return evaluateResponse(response, currentVersion, platform, currentVersionCode)
        }
        return UpdateCheckResult(UpdateStatus.UP_TO_DATE)
    }

    private suspend fun tryFetch(url: String): VersionResponse? =
        try {
            // 手动取文本 + 反序列化：不依赖 Content-Type 匹配，兼容 Gitee raw 的 text/plain（方案验证项 #4）
            val text = httpClient.get(url).bodyAsText()
            jsonParser.decodeFromString(VersionResponse.serializer(), text)
        } catch (_: Exception) {
            null
        }

    companion object {
        private val jsonParser = Json { ignoreUnknownKeys = true }

        fun isNewerVersion(current: String, remote: String): Boolean {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(currentParts.size, remoteParts.size)
            for (i in 0 until maxLen) {
                val c = currentParts.getOrElse(i) { 0 }
                val r = remoteParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }

        /**
         * 是否有更新：当本地与远端 versionCode 均已知时**优先用 versionCode**（更可靠，
         * 可识别"同 version 名仅 bump code"的热修）；否则回退到 version 字符串比较。
         */
        fun isNewer(
            currentVersion: String,
            remoteVersion: String,
            currentVersionCode: Int? = null,
            remoteVersionCode: Int? = null
        ): Boolean {
            if (currentVersionCode != null && remoteVersionCode != null) {
                return remoteVersionCode > currentVersionCode
            }
            return isNewerVersion(currentVersion, remoteVersion)
        }

        fun determineStatus(
            currentVersion: String,
            remoteVersion: String,
            forceUpdate: Boolean,
            minVersion: String,
            currentVersionCode: Int? = null,
            remoteVersionCode: Int? = null
        ): UpdateStatus {
            if (!isNewer(currentVersion, remoteVersion, currentVersionCode, remoteVersionCode)) {
                return UpdateStatus.UP_TO_DATE
            }
            if (forceUpdate || isNewerVersion(currentVersion, minVersion)) {
                return UpdateStatus.FORCE_UPDATE
            }
            return UpdateStatus.OPTIONAL_UPDATE
        }

        fun extractPlatformInfo(response: VersionResponse, platform: String): UpdateInfo? {
            return when (platform) {
                "Android" -> response.android
                "iOS" -> response.ios
                "HarmonyOS" -> response.harmonyos
                else -> null
            }
        }

        fun evaluateResponse(
            response: VersionResponse,
            currentVersion: String,
            platform: String,
            currentVersionCode: Int? = null
        ): UpdateCheckResult {
            val info = extractPlatformInfo(response, platform)
                ?: return UpdateCheckResult(UpdateStatus.UP_TO_DATE)

            val status = determineStatus(
                currentVersion = currentVersion,
                remoteVersion = info.version,
                forceUpdate = info.forceUpdate,
                minVersion = info.minVersion,
                currentVersionCode = currentVersionCode,
                remoteVersionCode = info.versionCode
            )

            return if (status == UpdateStatus.UP_TO_DATE) {
                UpdateCheckResult(UpdateStatus.UP_TO_DATE)
            } else {
                UpdateCheckResult(status, info)
            }
        }

        private fun createDefaultClient(): HttpClient {
            return HttpClient {
                // 关键：无超时则 GFW"连上但 hang"会让检查协程永久挂起、双端点 fallback 永不触发（方案 §12 #1）
                install(HttpTimeout) {
                    connectTimeoutMillis = 8_000
                    requestTimeoutMillis = 10_000
                    socketTimeoutMillis = 10_000
                }
            }
        }
    }
}
