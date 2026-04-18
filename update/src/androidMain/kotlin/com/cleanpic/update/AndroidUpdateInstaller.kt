package com.cleanpic.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
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

    override fun startUpdate(updateInfo: UpdateInfo) {
        // 如果正在下载中，不重复触发
        if (_downloadState.value == DownloadState.DOWNLOADING) return

        _downloadState.value = DownloadState.DOWNLOADING
        _downloadProgress.value = 0f

        val fileName = "cleanpic-${updateInfo.version}.apk"

        // 清理旧文件避免冲突
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        downloadDir?.listFiles()?.filter { it.name.startsWith("cleanpic-") && it.name.endsWith(".apk") }
            ?.forEach { it.delete() }

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("刷刷鸭 v${updateInfo.version}")
            .setDescription("正在下载更新...")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // 先注册 BroadcastReceiver，再 enqueue，避免竞态
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    progressJob?.cancel()
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: IllegalArgumentException) {
                        // 已注销
                    }
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = dm.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            _downloadProgress.value = 1f
                            _downloadState.value = DownloadState.DOWNLOADED
                            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val localUri = cursor.getString(uriIndex)
                            installApk(localUri)
                        } else {
                            _downloadState.value = DownloadState.FAILED
                        }
                        cursor.close()
                    } else {
                        _downloadState.value = DownloadState.FAILED
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }

        downloadId = dm.enqueue(request)

        // 启动进度轮询
        startProgressPolling(dm)
    }

    private fun startProgressPolling(dm: DownloadManager) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                delay(300L)
                if (_downloadState.value != DownloadState.DOWNLOADING) break
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = dm.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        if (bytesIndex >= 0 && totalIndex >= 0) {
                            val downloaded = cursor.getLong(bytesIndex)
                            val total = cursor.getLong(totalIndex)
                            if (total > 0) {
                                val newProgress = (downloaded.toFloat() / total).coerceIn(0f, 1f)
                                // 仅当整数百分比变化时才发射，减少不必要的 UI 重组
                                if ((newProgress * 100).toInt() != (_downloadProgress.value * 100).toInt()) {
                                    _downloadProgress.value = newProgress
                                }
                            }
                        }
                        cursor.close()
                    }
                } catch (_: Exception) {
                    // 查询失败时静默继续
                }
            }
        }
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
}
