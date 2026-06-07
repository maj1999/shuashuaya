package com.cleanpic.android

import android.app.Activity
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
import com.cleanpic.media.AndroidMediaRepository
import com.cleanpic.media.AndroidPickStateStore
import com.cleanpic.media.AndroidVideoPlayer
import com.cleanpic.permission.AndroidPermission
import com.cleanpic.settings.AndroidAppSettings
import com.cleanpic.theme.isLightColor
import com.cleanpic.ui.CleanPicApp

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

        ServiceLocator.initialize(
            mediaRepo = AndroidMediaRepository(applicationContext),
            settings = AndroidAppSettings(applicationContext),
            permission = androidPermission,
            player = AndroidVideoPlayer(),
            pickStateStore = AndroidPickStateStore(applicationContext),
            statsStore = com.cleanpic.stats.AndroidStatsStore(applicationContext)
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
    }
}
