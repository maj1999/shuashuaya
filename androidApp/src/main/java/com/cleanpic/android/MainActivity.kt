package com.cleanpic.android

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.cleanpic.di.ServiceLocator
import com.cleanpic.media.AndroidMediaRepository
import com.cleanpic.media.AndroidVideoPlayer
import com.cleanpic.permission.AndroidPermission
import com.cleanpic.settings.AndroidAppSettings
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

        ServiceLocator.initialize(
            mediaRepo = AndroidMediaRepository(applicationContext),
            settings = AndroidAppSettings(applicationContext),
            permission = androidPermission,
            player = AndroidVideoPlayer()
        )

        setContent {
            CleanPicApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidPermission.permissionLauncher = null
        AndroidMediaRepository.deleteLauncher = null
    }
}
