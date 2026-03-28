package com.cleanpic.media

class AndroidVideoPlayer : VideoPlayer {
    private var _isPlaying = false
    private var _muted = false
    private var _currentPosition = 0L
    private var _duration = 0L
    private var _prepared = false

    override fun prepare(mediaId: String) {
        // TODO: 使用 ExoPlayer 通过 ContentResolver 加载媒体 URI
        _prepared = true
        _duration = 30_000L // 占位值
    }

    override fun play() {
        if (_prepared) _isPlaying = true
    }

    override fun pause() {
        _isPlaying = false
    }

    override fun release() {
        _isPlaying = false
        _prepared = false
    }

    override fun setMuted(muted: Boolean) {
        _muted = muted
    }

    override val isPlaying: Boolean get() = _isPlaying
    override val currentPosition: Long get() = _currentPosition
    override val duration: Long get() = _duration
}
