package com.cleanpic.update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.runComposeUiTest
import com.cleanpic.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalTestApi::class)
class DownloadOverlayRecompositionTest {

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

    // ── 进度更新不应导致外部内容重组 ──

    @Test
    fun progress_update_does_not_recompose_sibling_content() = runComposeUiTest {
        val progressFlow = MutableStateFlow(0f)
        val stateFlow = MutableStateFlow(DownloadState.DOWNLOADING)
        var siblingCompositionCount = 0

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                // 模拟兄弟组件（主屏幕内容）
                SiblingContent(onComposed = { siblingCompositionCount++ })

                // 隔离的下载叠层
                IsolatedDownloadOverlay(
                    downloadState = stateFlow,
                    downloadProgress = progressFlow,
                    theme = testTheme
                )
            }
        }

        // 首次组合后 sibling 应该只组合了 1 次
        val initialCount = siblingCompositionCount

        // 更新进度多次
        progressFlow.value = 0.25f
        waitForIdle()
        progressFlow.value = 0.50f
        waitForIdle()
        progressFlow.value = 0.75f
        waitForIdle()

        // 兄弟组件不应被重新组合
        assertEquals(initialCount, siblingCompositionCount,
            "Sibling content should not recompose when download progress updates")
    }

    // ── 下载状态切换正确显示/隐藏弹窗 ──

    @Test
    fun download_overlay_shows_progress_dialog_when_downloading() = runComposeUiTest {
        val progressFlow = MutableStateFlow(0.5f)
        val stateFlow = MutableStateFlow(DownloadState.DOWNLOADING)

        setContent {
            IsolatedDownloadOverlay(
                downloadState = stateFlow,
                downloadProgress = progressFlow,
                theme = testTheme
            )
        }

        onNodeWithTag("download_progress_dialog").assertIsDisplayed()
    }

    @Test
    fun download_overlay_shows_failed_dialog_when_failed() = runComposeUiTest {
        val progressFlow = MutableStateFlow(0f)
        val stateFlow = MutableStateFlow(DownloadState.FAILED)

        setContent {
            IsolatedDownloadOverlay(
                downloadState = stateFlow,
                downloadProgress = progressFlow,
                theme = testTheme
            )
        }

        onNodeWithTag("update_failed_dialog").assertIsDisplayed()
    }

    @Test
    fun download_overlay_shows_nothing_when_idle() = runComposeUiTest {
        val progressFlow = MutableStateFlow(0f)
        val stateFlow = MutableStateFlow(DownloadState.IDLE)

        setContent {
            IsolatedDownloadOverlay(
                downloadState = stateFlow,
                downloadProgress = progressFlow,
                theme = testTheme
            )
        }

        onNodeWithTag("download_progress_dialog").assertDoesNotExist()
        onNodeWithTag("update_failed_dialog").assertDoesNotExist()
    }

    // ── 辅助组合函数 ──

    @Composable
    private fun SiblingContent(onComposed: () -> Unit) {
        onComposed()
        Text(
            text = "Sibling",
            modifier = Modifier.testTag("sibling_content")
        )
    }

    @Composable
    private fun IsolatedDownloadOverlay(
        downloadState: StateFlow<DownloadState>,
        downloadProgress: StateFlow<Float>,
        theme: ThemeTokens
    ) {
        val state by downloadState.collectAsState()
        val progress by downloadProgress.collectAsState()

        when (state) {
            DownloadState.DOWNLOADING -> {
                DownloadProgressDialog(theme = theme, progress = progress)
            }
            DownloadState.FAILED -> {
                UpdateFailedDialog(
                    theme = theme,
                    onRetry = {},
                    onDismiss = {}
                )
            }
            else -> {}
        }
    }
}
