package com.cleanpic.di

import com.cleanpic.media.MediaRepository
import com.cleanpic.media.VideoPlayer
import com.cleanpic.permission.PermissionManager
import com.cleanpic.settings.AppSettings
import com.cleanpic.theme.ThemeManager
import com.cleanpic.update.UpdateCheckResult
import com.cleanpic.update.UpdateChecker
import com.cleanpic.update.UpdateInstaller
import com.cleanpic.update.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow

object ServiceLocator {
    lateinit var mediaRepository: MediaRepository
    lateinit var appSettings: AppSettings
    lateinit var permissionManager: PermissionManager
    lateinit var videoPlayer: VideoPlayer
    val themeManager: ThemeManager = ThemeManager()

    // 自动升级
    var updateChecker: UpdateChecker? = null
    var updateInstaller: UpdateInstaller? = null
    val cachedUpdateResult = MutableStateFlow(UpdateCheckResult(UpdateStatus.UP_TO_DATE))

    fun initialize(
        mediaRepo: MediaRepository,
        settings: AppSettings,
        permission: PermissionManager,
        player: VideoPlayer,
        updater: UpdateChecker? = null,
        installer: UpdateInstaller? = null
    ) {
        mediaRepository = mediaRepo
        appSettings = settings
        permissionManager = permission
        videoPlayer = player
        updateChecker = updater
        updateInstaller = installer
        themeManager.switchTheme(settings.theme)
    }
}
