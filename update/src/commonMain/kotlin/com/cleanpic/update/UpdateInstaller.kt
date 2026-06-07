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

/**
 * 按优先级返回去重后的下载候选源：主源（通常 Gitee 国内）在前，备用源（通常 GitHub 境外）在后。
 * 客户端依次尝试，前一个下载失败/卡死时自动回退到下一个。空串会被剔除。
 */
fun UpdateInfo.downloadCandidates(): List<String> =
    listOf(downloadUrl, downloadUrlFallback)
        .filter { it.isNotBlank() }
        .distinct()
