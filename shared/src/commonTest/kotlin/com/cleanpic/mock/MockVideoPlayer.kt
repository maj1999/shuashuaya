package com.cleanpic.mock

import com.cleanpic.media.VideoPlayer

class MockVideoPlayer : VideoPlayer {
    override fun prepare(mediaId: String) {}
    override fun play() {}
    override fun pause() {}
    override fun release() {}
    override fun setMuted(muted: Boolean) {}
    override val isPlaying: Boolean = false
    override val currentPosition: Long = 0L
    override val duration: Long = 0L
}
