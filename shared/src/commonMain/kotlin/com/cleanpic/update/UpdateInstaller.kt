package com.cleanpic.update

import kotlinx.coroutines.flow.StateFlow

enum class DownloadState {
    IDLE, DOWNLOADING, DOWNLOADED, INSTALLING, FAILED
}

interface UpdateInstaller {
    val downloadProgress: StateFlow<Float>
    val downloadState: StateFlow<DownloadState>
    fun startUpdate(updateInfo: UpdateInfo)
}
