package com.cleanpic.viewmodel

import com.cleanpic.di.ServiceLocator
import com.cleanpic.permission.PermissionStatus

class HomeViewModel {
    private val permissionManager get() = ServiceLocator.permissionManager

    fun checkPermission(): PermissionStatus = permissionManager.checkPermissionStatus()

    suspend fun requestPermission(): PermissionStatus = permissionManager.requestPhotoPermission()

    fun openSettings() = permissionManager.openAppSettings()

    val isLimitedAccess: Boolean
        get() = permissionManager.checkPermissionStatus() == PermissionStatus.LIMITED
}
