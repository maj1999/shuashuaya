package com.cleanpic.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Android 更新安装器。下载走 **App 内 ktor**（[downloadApk]），而非系统 DownloadManager。
 *
 * 为什么不用 DownloadManager：它是独立系统进程，在梯子等环境下与 App 网络栈不一致，
 * 表现为"能检查到更新却下载 0% 卡死"。改用与检查更新同一条 HttpClient 通道下载，
 * 配合选源竞速（自动选当前网络最快可达的源）+ socketTimeout 防卡死 + 完整性校验。
 */
class AndroidUpdateInstaller internal constructor(
    private val context: Context,
    private val httpClient: HttpClient, // internal：测试可注入 MockEngine
) : UpdateInstaller {

    constructor(context: Context) : this(context.applicationContext, createDownloadClient())

    private val _downloadProgress = MutableStateFlow(0f)
    override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    override val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var downloadJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun startUpdate(updateInfo: UpdateInfo) {
        if (_downloadState.value == DownloadState.DOWNLOADING) return // 不重复触发

        val candidates = updateInfo.downloadCandidates()
        if (candidates.isEmpty()) {
            _downloadState.value = DownloadState.FAILED
            return
        }

        _downloadState.value = DownloadState.DOWNLOADING
        _downloadProgress.value = 0f

        downloadJob?.cancel()
        downloadJob = scope.launch {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            // 清理旧残留，避免占空间/混淆
            dir?.listFiles()
                ?.filter { it.name.startsWith("cleanpic-") && it.name.endsWith(".apk") }
                ?.forEach { it.delete() }

            val destFile = File(dir, "cleanpic-${updateInfo.version}.apk")
            val ok = downloadApk(
                client = httpClient,
                candidates = candidates,
                destFile = destFile,
                expectedSha256 = updateInfo.sha256,
                expectedSize = updateInfo.size,
                onProgress = { p ->
                    // 仅整数百分比变化时发射，减少不必要的 UI 重组
                    if ((p * 100).toInt() != (_downloadProgress.value * 100).toInt()) {
                        _downloadProgress.value = p
                    }
                },
            )

            // 外部已取消（resetState）则不再推进状态
            if (_downloadState.value != DownloadState.DOWNLOADING) return@launch

            if (ok) {
                _downloadProgress.value = 1f
                _downloadState.value = DownloadState.DOWNLOADED
                installApk(destFile)
            } else {
                _downloadState.value = DownloadState.FAILED
            }
        }
    }

    private fun installApk(file: File) {
        _downloadState.value = DownloadState.INSTALLING
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            _downloadState.value = DownloadState.FAILED
        }
    }

    override fun resetState() {
        downloadJob?.cancel()
        _downloadState.value = DownloadState.IDLE
        _downloadProgress.value = 0f
    }

    override fun simulateDownload() {
        if (_downloadState.value == DownloadState.DOWNLOADING) return
        _downloadState.value = DownloadState.DOWNLOADING
        _downloadProgress.value = 0f
        downloadJob?.cancel()
        downloadJob = scope.launch {
            val totalSteps = 200
            for (i in 1..totalSteps) {
                delay(100L)
                if (_downloadState.value != DownloadState.DOWNLOADING) break
                _downloadProgress.value = (i.toFloat() / totalSteps).coerceIn(0f, 1f)
            }
            if (_downloadState.value == DownloadState.DOWNLOADING) {
                _downloadProgress.value = 1f
                _downloadState.value = DownloadState.IDLE
            }
        }
    }

    private companion object {
        private fun createDownloadClient(): HttpClient = HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 8_000
                // 不设 requestTimeout（整包耗时不定）；socketTimeout 充当"卡死看门狗"：
                // 15s 无字节进展即抛超时 → downloadApk 自动切下一个源。
                socketTimeoutMillis = 15_000
            }
            // 部分 CDN（Gitee）对空/异常 UA 触发风控；用常见浏览器 UA 提高匿名下载成功率。
            install(UserAgent) {
                agent = "Mozilla/5.0 (Linux; Android) CleanPic-Updater"
            }
        }
    }
}
