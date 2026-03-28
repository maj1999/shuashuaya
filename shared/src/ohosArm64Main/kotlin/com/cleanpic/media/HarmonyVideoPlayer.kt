package com.cleanpic.media

class HarmonyVideoPlayer : VideoPlayer {
    // TODO: 通过 ArkUI AVPlayer 实现

    override fun prepare(mediaId: String) {}

    override fun play() {}

    override fun pause() {}

    override fun release() {}

    override fun setMuted(muted: Boolean) {}

    override val isPlaying: Boolean = false

    override val currentPosition: Long = 0L

    override val duration: Long = 0L
}
