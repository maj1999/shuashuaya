package com.cleanpic.update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.cleanpic.theme.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalTestApi::class)
class UpdateDialogOverlayTest {

    private val testTheme = ThemeTokens(
        id = "test",
        name = "Test",
        colorPrimary = 0xFF6200EE,
        colorAccent = 0xFF03DAC5,
        colorBackground = 0xFFFFFFFF,
        colorSurface = 0xFFFFFFFF,
        colorDanger = 0xFFB00020,
        colorSuccess = 0xFF00C853,
        colorText = 0xFF000000,
        colorTextSecondary = 0xFF666666,
        gradientMain = null,
        borderRadius = 12f,
        shadowStyle = ShadowDef(0f, 2f, 4f, 0x40000000),
        fontFamily = "System",
        animDuration = 300,
        animEasing = "ease",
        animButtonPress = ButtonPressAnim.NONE
    )

    private val testUpdateInfo = UpdateInfo(
        version = "2.0.0",
        changelog = "Test update"
    )

    // ── 强制更新弹窗：遮罩阻止背景点击 ──

    @Test
    fun force_update_dialog_blocks_background_clicks() = runComposeUiTest {
        var backgroundClicked = false

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { backgroundClicked = true }
                        .testTag("background_button")
                ) {
                    Text("Background")
                }
                UpdateDialog(
                    theme = testTheme,
                    updateInfo = testUpdateInfo,
                    isForceUpdate = true,
                    onUpdate = {},
                    onDismiss = {}
                )
            }
        }

        onNodeWithTag("background_button").performClick()
        assertFalse(backgroundClicked, "Force update dialog should block background clicks")
    }

    // ── 可选更新弹窗：遮罩阻止背景点击 ──

    @Test
    fun optional_update_dialog_blocks_background_clicks() = runComposeUiTest {
        var backgroundClicked = false

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { backgroundClicked = true }
                        .testTag("background_button")
                ) {
                    Text("Background")
                }
                UpdateDialog(
                    theme = testTheme,
                    updateInfo = testUpdateInfo,
                    isForceUpdate = false,
                    onUpdate = {},
                    onDismiss = {}
                )
            }
        }

        onNodeWithTag("background_button").performClick()
        assertFalse(backgroundClicked, "Optional update dialog should block background clicks")
    }

    // ── 下载进度弹窗：遮罩阻止背景点击 ──

    @Test
    fun download_progress_dialog_blocks_background_clicks() = runComposeUiTest {
        var backgroundClicked = false

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { backgroundClicked = true }
                        .testTag("background_button")
                ) {
                    Text("Background")
                }
                DownloadProgressDialog(
                    theme = testTheme,
                    progress = 0.5f
                )
            }
        }

        onNodeWithTag("background_button").performClick()
        assertFalse(backgroundClicked, "Download progress dialog should block background clicks")
    }

    // ── 下载失败弹窗：遮罩阻止背景点击 ──

    @Test
    fun update_failed_dialog_blocks_background_clicks() = runComposeUiTest {
        var backgroundClicked = false

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { backgroundClicked = true }
                        .testTag("background_button")
                ) {
                    Text("Background")
                }
                UpdateFailedDialog(
                    theme = testTheme,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }

        onNodeWithTag("background_button").performClick()
        assertFalse(backgroundClicked, "Update failed dialog should block background clicks")
    }
}
