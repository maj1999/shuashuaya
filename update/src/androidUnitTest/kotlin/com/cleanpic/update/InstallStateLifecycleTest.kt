package com.cleanpic.update

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * U-UPD-12：从系统安装器返回后清理 INSTALLING 残留遮罩（[ResetInstallStateOnResume]）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalTestApi::class)
class InstallStateLifecycleTest {

    private class FakeInstaller(initial: DownloadState) : UpdateInstaller {
        private val state = MutableStateFlow(initial)
        override val downloadProgress = MutableStateFlow(0f)
        override val downloadState: StateFlow<DownloadState> = state
        var resetCount = 0
        override fun startUpdate(updateInfo: UpdateInfo) {}
        override fun resetState() {
            resetCount++
            state.value = DownloadState.IDLE
        }
    }

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.RESUMED
        }
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun resets_installing_state_after_returning_from_installer() = runComposeUiTest {
        val installer = FakeInstaller(DownloadState.INSTALLING)
        val owner = TestOwner()

        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                ResetInstallStateOnResume(installer)
            }
        }
        waitForIdle()

        // 初始组合时生命周期已是 RESUMED（addObserver 会回放一次 ON_RESUME），不应误重置
        assertEquals(0, installer.resetCount, "组合瞬间不应重置")
        assertEquals(DownloadState.INSTALLING, installer.downloadState.value)

        // 模拟：去系统安装器（ON_PAUSE）→ 取消返回（ON_RESUME）
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        waitForIdle()

        assertEquals(1, installer.resetCount, "从安装器返回且仍 INSTALLING 应重置一次")
        assertEquals(DownloadState.IDLE, installer.downloadState.value)
    }

    @Test
    fun does_not_reset_when_state_is_not_installing() = runComposeUiTest {
        // 下载中返回前台不应被重置（只针对卡死的 INSTALLING）
        val installer = FakeInstaller(DownloadState.DOWNLOADING)
        val owner = TestOwner()

        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                ResetInstallStateOnResume(installer)
            }
        }
        waitForIdle()

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        waitForIdle()

        assertEquals(0, installer.resetCount)
        assertEquals(DownloadState.DOWNLOADING, installer.downloadState.value)
    }
}
