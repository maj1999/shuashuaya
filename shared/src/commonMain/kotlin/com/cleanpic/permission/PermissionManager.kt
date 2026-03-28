package com.cleanpic.permission

enum class PermissionStatus { GRANTED, LIMITED, DENIED, PERMANENTLY_DENIED }

interface PermissionManager {
    suspend fun requestPhotoPermission(): PermissionStatus
    fun checkPermissionStatus(): PermissionStatus
    fun openAppSettings()
}
