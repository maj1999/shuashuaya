package com.cleanpic.permission

class HarmonyPermission : PermissionManager {
    // TODO: 通过 ohos.permission.READ_IMAGEVIDEO 实现

    override suspend fun requestPhotoPermission(): PermissionStatus =
        PermissionStatus.GRANTED

    override fun checkPermissionStatus(): PermissionStatus =
        PermissionStatus.GRANTED

    override fun openAppSettings() {
        // TODO: 打开鸿蒙系统的应用设置页面
    }
}
