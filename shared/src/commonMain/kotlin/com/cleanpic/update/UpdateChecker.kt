package com.cleanpic.update

import com.cleanpic.AppInfo
import com.cleanpic.getPlatformName
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class UpdateChecker(
    private val apiUrl: String
) {
    private val httpClient: HttpClient = createDefaultClient()

    suspend fun checkForUpdate(
        currentVersion: String = AppInfo.VERSION,
        platform: String = getPlatformName()
    ): UpdateCheckResult {
        return try {
            val response: VersionResponse = httpClient.get("$apiUrl/api/version").body()
            evaluateResponse(response, currentVersion, platform)
        } catch (_: Exception) {
            UpdateCheckResult(UpdateStatus.UP_TO_DATE)
        }
    }

    companion object {
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

        fun determineStatus(
            currentVersion: String,
            remoteVersion: String,
            forceUpdate: Boolean,
            minVersion: String
        ): UpdateStatus {
            if (!isNewerVersion(currentVersion, remoteVersion)) {
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
            platform: String
        ): UpdateCheckResult {
            val info = extractPlatformInfo(response, platform)
                ?: return UpdateCheckResult(UpdateStatus.UP_TO_DATE)

            val status = determineStatus(
                currentVersion = currentVersion,
                remoteVersion = info.version,
                forceUpdate = info.forceUpdate,
                minVersion = info.minVersion
            )

            return if (status == UpdateStatus.UP_TO_DATE) {
                UpdateCheckResult(UpdateStatus.UP_TO_DATE)
            } else {
                UpdateCheckResult(status, info)
            }
        }

        private fun createDefaultClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        }
    }
}
