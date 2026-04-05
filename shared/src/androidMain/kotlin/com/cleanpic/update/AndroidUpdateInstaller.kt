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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AndroidUpdateInstaller(private val context: Context) : UpdateInstaller {

    private val _downloadProgress = MutableStateFlow(0f)
    override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    override val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var downloadId: Long = -1

    override fun startUpdate(updateInfo: UpdateInfo) {
        _downloadState.value = DownloadState.DOWNLOADING
        _downloadProgress.value = 0f

        val fileName = "cleanpic-${updateInfo.version}.apk"
        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("刷刷鸭 v${updateInfo.version}")
            .setDescription("正在下载更新...")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
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
    }

    private fun installApk(localUri: String) {
        _downloadState.value = DownloadState.INSTALLING
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
    }

    companion object {
        fun resetState(installer: AndroidUpdateInstaller) {
            installer._downloadState.value = DownloadState.IDLE
            installer._downloadProgress.value = 0f
        }
    }
}
