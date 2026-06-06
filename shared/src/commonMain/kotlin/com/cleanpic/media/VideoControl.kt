package com.cleanpic.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 视频播放控制桥 —— 把平台播放器（ExoPlayer/AVPlayer）的播放进度暴露给 Compose UI，
 * 并提供 seek 回调，供全屏底部进度条读取位置/总时长并拖拽跳转。
 *
 * 用法：调用方 remember 一个实例传给 [com.cleanpic.ui.media.VideoPlayerView]，
 * 平台实现负责轮询更新 [positionMs]/[durationMs] 并接管 [seek]；
 * UI 侧读取 position/duration 渲染进度条，拖拽时调用 [seek]。
 * 仅在需要进度条的场景（全屏）传入；不传则平台实现不做轮询，零开销。
 */
class VideoControl {
    /** 当前播放位置（毫秒）。平台实现轮询更新。 */
    var positionMs by mutableStateOf(0L)
        internal set

    /** 视频总时长（毫秒）；未就绪时为 0。平台实现就绪后更新。 */
    var durationMs by mutableStateOf(0L)
        internal set

    /** 跳转到指定位置（毫秒）。平台实现在挂载时接管；未接管时为空操作。 */
    var seek: (Long) -> Unit = {}
        internal set

    /** 平台实现更新进度。 */
    fun update(position: Long, duration: Long) {
        positionMs = position
        durationMs = duration
    }
}
