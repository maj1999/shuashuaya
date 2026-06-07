package com.cleanpic.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
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

class AndroidUpdateInstaller(private val context: Context) : UpdateInstaller {

    private val _downloadProgress = MutableStateFlow(0f)
    override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    override val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var downloadId: Long = -1
    private var progressJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 依次尝试所有候选下载源（主源 Gitee 在前、备用源 GitHub 在后）。
     * 任一源下载失败、完整性校验失败、或长时间零进展（卡死，境外访问 Gitee 的典型表现）时，
     * 自动回退到下一个源；全部耗尽才置 FAILED。整个过程由单个轮询协程驱动，
     * 直接读 DownloadManager 状态，不再依赖 BroadcastReceiver（便于在卡死时主动切源）。
     */
    override fun startUpdate(updateInfo: UpdateInfo) {
        // 如果正在下载中，不重复触发
        if (_downloadState.value == DownloadState.DOWNLOADING) return

        val candidates = updateInfo.downloadCandidates()
        if (candidates.isEmpty()) {
            _downloadState.value = DownloadState.FAILED
            return
        }

        _downloadState.value = DownloadState.DOWNLOADING
        _downloadProgress.value = 0f

        progressJob?.cancel()
        progressJob = scope.launch {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            for ((index, url) in candidates.withIndex()) {
                val localUri = downloadOnce(dm, updateInfo, url)
                if (localUri != null) {
                    _downloadProgress.value = 1f
                    _downloadState.value = DownloadState.DOWNLOADED
                    installApk(localUri)
                    return@launch
                }
                // 外部已取消（resetState）则停止；否则若还有备用源，重置进度后继续
                if (_downloadState.value != DownloadState.DOWNLOADING) return@launch
                if (index < candidates.lastIndex) _downloadProgress.value = 0f
            }
            _downloadState.value = DownloadState.FAILED
        }
    }

    /**
     * 下载单个源并轮询至终态。成功且完整性校验通过 → 返回本地 URI；
     * 失败 / 校验不过 / 卡死 → 清理后返回 null。
     */
    private suspend fun downloadOnce(dm: DownloadManager, updateInfo: UpdateInfo, url: String): String? {
        // 清理旧文件避免冲突（含上一个源的残留）
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        downloadDir?.listFiles()?.filter { it.name.startsWith("cleanpic-") && it.name.endsWith(".apk") }
            ?.forEach { it.delete() }

        val fileName = "cleanpic-${updateInfo.version}.apk"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("刷刷鸭 v${updateInfo.version}")
            .setDescription("正在下载更新...")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val id = try {
            dm.enqueue(request)
        } catch (_: Exception) {
            return null
        }
        downloadId = id

        var lastBytes = -1L
        var stalledTicks = 0
        try {
            while (true) {
                delay(POLL_INTERVAL_MS)
                // 外部取消（resetState / 重新触发）
                if (_downloadState.value != DownloadState.DOWNLOADING) {
                    dm.remove(id)
                    return null
                }
                val snap = querySnapshot(dm, id) ?: run {
                    dm.remove(id)
                    return null
                }
                when (snap.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val file = snap.localUri?.let {
                            runCatching { File(Uri.parse(it).path!!) }.getOrNull()
                        }
                        // 完整性校验：挡住 Gitee 防盗链/风控返回的 HTML 伪装包，避免装坏包（F1）
                        return if (file != null && ApkIntegrity.verify(file, updateInfo.sha256, updateInfo.size)) {
                            snap.localUri
                        } else {
                            dm.remove(id)
                            null
                        }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        dm.remove(id)
                        return null
                    }
                    else -> {
                        if (snap.total > 0) {
                            val p = (snap.bytes.toFloat() / snap.total).coerceIn(0f, 1f)
                            // 仅当整数百分比变化时才发射，减少不必要的 UI 重组
                            if ((p * 100).toInt() != (_downloadProgress.value * 100).toInt()) {
                                _downloadProgress.value = p
                            }
                        }
                        // 卡死看门狗：连续 STALL_TICKS 次轮询字节数零增长 → 判定本源卡死，放弃并切下一个源
                        if (snap.bytes > lastBytes) {
                            lastBytes = snap.bytes
                            stalledTicks = 0
                        } else {
                            stalledTicks++
                            if (stalledTicks >= STALL_TICKS) {
                                dm.remove(id)
                                return null
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            runCatching { dm.remove(id) }
            throw e
        } catch (_: Exception) {
            runCatching { dm.remove(id) }
            return null
        }
    }

    private data class Snapshot(
        val status: Int,
        val bytes: Long,
        val total: Long,
        val localUri: String?
    )

    private fun querySnapshot(dm: DownloadManager, id: Long): Snapshot? = runCatching {
        dm.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            Snapshot(
                status = cursor.getLongOrIntColumn(DownloadManager.COLUMN_STATUS).toInt(),
                bytes = cursor.getLongOrIntColumn(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                total = cursor.getLongOrIntColumn(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                localUri = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    .takeIf { it >= 0 }?.let { cursor.getString(it) }
            )
        }
    }.getOrNull()

    private fun android.database.Cursor.getLongOrIntColumn(name: String): Long {
        val idx = getColumnIndex(name)
        return if (idx >= 0) getLong(idx) else 0L
    }

    private fun installApk(localUri: String) {
        _downloadState.value = DownloadState.INSTALLING
        try {
            val file = File(Uri.parse(localUri).path!!)
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
        progressJob?.cancel()
        _downloadState.value = DownloadState.IDLE
        _downloadProgress.value = 0f
    }

    override fun simulateDownload() {
        if (_downloadState.value == DownloadState.DOWNLOADING) return
        _downloadState.value = DownloadState.DOWNLOADING
        _downloadProgress.value = 0f
        progressJob?.cancel()
        progressJob = scope.launch {
            // 模拟 20 秒下载，每 100ms 更新一次进度（高频率以验证闪烁修复）
            val totalSteps = 200
            val stepDelay = 100L
            for (i in 1..totalSteps) {
                delay(stepDelay)
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
        const val POLL_INTERVAL_MS = 300L
        // 100 * 300ms = 30s 内字节数零增长即判定卡死（境外访问 Gitee CDN 的典型表现），切换下一个源
        const val STALL_TICKS = 100
    }
}
