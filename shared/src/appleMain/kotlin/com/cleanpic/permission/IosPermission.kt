package com.cleanpic.permission

class IosPermission : PermissionManager {
    // TODO: 通过 Kotlin/Native interop 使用 PHPhotoLibrary 实现

    override suspend fun requestPhotoPermission(): PermissionStatus =
        PermissionStatus.GRANTED

    override fun checkPermissionStatus(): PermissionStatus =
        PermissionStatus.GRANTED

    override fun openAppSettings() {
        // TODO: UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString))
    }
}
