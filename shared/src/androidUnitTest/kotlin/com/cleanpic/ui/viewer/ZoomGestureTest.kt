package com.cleanpic.ui.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 验证全屏查看的缩放手势（US-CP-20）。
 * 用 Compose 测试框架确定性触发双击，断言 zoomState.scale 变化——
 * 这是 Maestro/adb 无法可靠模拟双击/捏合手势的可靠替代验证。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ZoomGestureTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun doubleTap_zooms_in_then_restores() = runComposeUiTest {
        lateinit var zoomState: ZoomState
        setContent {
            ZoomTarget(onState = { zoomState = it })
        }

        // 初始 1×
        assertTrue(zoomState.scale in 0.99f..1.01f, "初始应为 1×, scale=${zoomState.scale}")

        // 双击放大
        onNodeWithTag("zoomBox").performTouchInput { doubleClick() }
        waitForIdle()
        assertTrue(zoomState.scale > 1.5f, "双击后应放大, scale=${zoomState.scale}")

        // 再次双击还原
        onNodeWithTag("zoomBox").performTouchInput { doubleClick() }
        waitForIdle()
        assertTrue(zoomState.scale in 0.99f..1.01f, "再次双击应还原, scale=${zoomState.scale}")
    }

    @Composable
    private fun ZoomTarget(onState: (ZoomState) -> Unit) {
        val zoomState = rememberZoomState(contentSize = Size(1000f, 1000f), maxScale = 5f)
        onState(zoomState)
        Box(
            modifier = Modifier
                .size(300.dp)
                .testTag("zoomBox")
                .zoomable(zoomState, enableOneFingerZoom = false)
        )
    }
}
