package com.cleanpic.update

import kotlinx.coroutines.flow.StateFlow

enum class DownloadState {
    IDLE, DOWNLOADING, DOWNLOADED, INSTALLING, FAILED
}

interface UpdateInstaller {
    val downloadProgress: StateFlow<Float>
    val downloadState: StateFlow<DownloadState>
    fun startUpdate(updateInfo: UpdateInfo)
    fun resetState()
    /** 模拟下载进度（调试用），不实际下载任何文件 */
    fun simulateDownload() {}
}
