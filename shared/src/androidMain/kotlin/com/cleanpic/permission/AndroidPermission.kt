package com.cleanpic.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

class AndroidPermission(private val context: Context) : PermissionManager {

    override suspend fun requestPhotoPermission(): PermissionStatus {
        // 实际的权限请求需要 Activity.requestPermissions()
        // 这里仅检查当前状态；真正的请求流程在 Activity 层处理
        return checkPermissionStatus()
    }

    override fun checkPermissionStatus(): PermissionStatus {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) ==
                PackageManager.PERMISSION_GRANTED
        }

        return if (allGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
