package com.cleanpic.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosUpdateInstaller : UpdateInstaller {

    private val _downloadProgress = MutableStateFlow(0f)
    override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    override val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    override fun startUpdate(updateInfo: UpdateInfo) {
        val url = NSURL.URLWithString(updateInfo.downloadUrl) ?: run {
            _downloadState.value = DownloadState.FAILED
            return
        }
        UIApplication.sharedApplication.openURL(url)
    }

    override fun resetState() {
        _downloadState.value = DownloadState.IDLE
        _downloadProgress.value = 0f
    }
}
