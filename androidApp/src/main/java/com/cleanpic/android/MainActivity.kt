package com.cleanpic.android

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.cleanpic.android.wiring.UpdateWiring
import com.cleanpic.di.ServiceLocator
import com.cleanpic.log.FileLogWriter
import com.cleanpic.log.LogExportController
import com.cleanpic.log.LogExporter
import com.cleanpic.log.logger
import com.cleanpic.media.AndroidMediaRepository
import com.cleanpic.media.AndroidPickStateStore
import com.cleanpic.media.AndroidVideoPlayer
import com.cleanpic.permission.AndroidPermission
import com.cleanpic.settings.AndroidAppSettings
import com.cleanpic.theme.isLightColor
import com.cleanpic.ui.CleanPicApp
import java.io.File

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        (ServiceLocator.permissionManager as? AndroidPermission)
            ?.onPermissionResult(granted)
    }

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val granted = result.resultCode == Activity.RESULT_OK
        (ServiceLocator.mediaRepository as? AndroidMediaRepository)
            ?.onDeleteResult(granted)
    }

    private val logsDir by lazy { File(filesDir, "logs") }
    private val log = logger("MainActivity")

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) {
            log.i { "导出取消" }
            return@registerForActivityResult
        }
        val out = contentResolver.openOutputStream(uri)
        if (out == null) {
            log.w { "导出失败：openOutputStream 返回 null" }
            return@registerForActivityResult
        }
        runCatching {
            out.use {
                val content = LogExporter.collect(logsDir).ifEmpty { "(暂无日志)" }
                it.write(content.toByteArray())
            }
            log.i { "导出成功" }
        }.onFailure { log.e(it) { "导出写入失败" } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val androidPermission = AndroidPermission(applicationContext)
        AndroidPermission.permissionLauncher = { permissions ->
            permissionLauncher.launch(permissions)
        }

        AndroidMediaRepository.deleteLauncher = { intentSender ->
            val request = IntentSenderRequest.Builder(intentSender).build()
            deleteLauncher.launch(request)
        }

        ServiceLocator.isDebugBuild = BuildConfig.DEBUG

        LogExportController.onRequestExport = {
            exportLauncher.launch(LogExporter.exportFileName(System.currentTimeMillis()))
        }

        ServiceLocator.initialize(
            mediaRepo = AndroidMediaRepository(applicationContext),
            settings = AndroidAppSettings(applicationContext),
            permission = androidPermission,
            player = AndroidVideoPlayer(),
            pickStateStore = AndroidPickStateStore(applicationContext),
            statsStore = com.cleanpic.stats.AndroidStatsStore(applicationContext),
            logWriters = listOf(FileLogWriter(logsDir))
        )

        val hooks = UpdateWiring.provideHooks(this)

        setContent {
            val theme by ServiceLocator.themeManager.currentTheme.collectAsState()
            val statusBarColor = theme.colorBackground.toInt()
            val lightStatusBar = !isLightColor(theme.colorText)

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    @Suppress("DEPRECATION")
                    window.statusBarColor = statusBarColor
                    WindowCompat.getInsetsController(window, view)
                        .isAppearanceLightStatusBars = lightStatusBar
                }
            }

            CleanPicApp(hooks = hooks)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidPermission.permissionLauncher = null
        AndroidMediaRepository.deleteLauncher = null
        LogExportController.onRequestExport = null
    }
}
