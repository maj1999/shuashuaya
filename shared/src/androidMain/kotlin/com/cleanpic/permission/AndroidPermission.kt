package com.cleanpic.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.cleanpic.log.logger
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidPermission(private val context: Context) : PermissionManager {

    private var pendingCallback: ((PermissionStatus) -> Unit)? = null
    private val log = logger("Permission")

    override suspend fun requestPhotoPermission(): PermissionStatus {
        if (checkPermissionStatus() == PermissionStatus.GRANTED) {
            return PermissionStatus.GRANTED
        }
        return suspendCancellableCoroutine { cont ->
            pendingCallback = { status -> cont.resume(status) }
            val launcher = permissionLauncher
            if (launcher != null) {
                log.i { "请求相册权限" }
                launcher(requiredPermissions())
            } else {
                log.w { "permissionLauncher 未注册，直接 DENIED" }
                cont.resume(PermissionStatus.DENIED)
                pendingCallback = null
            }
        }
    }

    fun onPermissionResult(granted: Map<String, Boolean>) {
        val allGranted = granted.values.all { it }
        log.i { "权限回调 granted=$allGranted" }
        val status = if (allGranted) PermissionStatus.GRANTED
            else PermissionStatus.DENIED
        pendingCallback?.invoke(status)
        pendingCallback = null
    }

    override fun checkPermissionStatus(): PermissionStatus {
        val allGranted = requiredPermissions().all {
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

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    companion object {
        var permissionLauncher: ((Array<String>) -> Unit)? = null
    }
}
