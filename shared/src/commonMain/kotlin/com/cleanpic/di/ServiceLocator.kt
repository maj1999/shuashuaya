package com.cleanpic.di

import co.touchlab.kermit.LogWriter
import com.cleanpic.log.LogConfig
import com.cleanpic.media.InMemoryPickStateStore
import com.cleanpic.media.MediaRepository
import com.cleanpic.media.PickStateStore
import com.cleanpic.media.VideoPlayer
import com.cleanpic.permission.PermissionManager
import com.cleanpic.settings.AppSettings
import com.cleanpic.theme.ThemeManager

object ServiceLocator {
    lateinit var mediaRepository: MediaRepository
    lateinit var appSettings: AppSettings
    lateinit var permissionManager: PermissionManager
    lateinit var videoPlayer: VideoPlayer
    val themeManager: ThemeManager = ThemeManager()

    /** 浏览记忆持久化。平台侧可在 initialize 时注入；默认内存实现（测试/未接入平台）。 */
    var pickStateStore: PickStateStore = InMemoryPickStateStore()

    /** 清理统计持久化。平台侧可在 initialize 时注入；默认内存实现。 */
    var statsStore: com.cleanpic.stats.StatsStore = com.cleanpic.stats.InMemoryStatsStore()

    // 调试标记（平台侧设置）
    var isDebugBuild: Boolean = false

    fun initialize(
        mediaRepo: MediaRepository,
        settings: AppSettings,
        permission: PermissionManager,
        player: VideoPlayer,
        pickStateStore: PickStateStore = InMemoryPickStateStore(),
        statsStore: com.cleanpic.stats.StatsStore = com.cleanpic.stats.InMemoryStatsStore(),
        logWriters: List<LogWriter> = emptyList()
    ) {
        mediaRepository = mediaRepo
        appSettings = settings
        permissionManager = permission
        videoPlayer = player
        this.pickStateStore = pickStateStore
        this.statsStore = statsStore
        themeManager.switchTheme(settings.theme)
        LogConfig.init(isDebugBuild, logWriters)
    }
}
