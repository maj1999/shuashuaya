package com.cleanpic.di

import com.cleanpic.media.MediaRepository
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

    fun initialize(
        mediaRepo: MediaRepository,
        settings: AppSettings,
        permission: PermissionManager,
        player: VideoPlayer
    ) {
        mediaRepository = mediaRepo
        appSettings = settings
        permissionManager = permission
        videoPlayer = player
        themeManager.switchTheme(settings.theme)
    }
}
