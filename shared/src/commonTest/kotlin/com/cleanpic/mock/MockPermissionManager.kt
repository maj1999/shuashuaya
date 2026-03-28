package com.cleanpic.mock

import com.cleanpic.permission.PermissionManager
import com.cleanpic.permission.PermissionStatus

class MockPermissionManager(
    var status: PermissionStatus = PermissionStatus.GRANTED
) : PermissionManager {
    override suspend fun requestPhotoPermission(): PermissionStatus = status
    override fun checkPermissionStatus(): PermissionStatus = status
    override fun openAppSettings() {}
}
