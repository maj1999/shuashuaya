package com.cleanpic.media

import com.cleanpic.log.logger

class AndroidVideoPlayer : VideoPlayer {
    private var _isPlaying = false
    private var _muted = false
    private var _currentPosition = 0L
    private var _duration = 0L
    private var _prepared = false
    private val log = logger("VideoPlayer")

    override fun prepare(mediaId: String) {
        // TODO: 使用 ExoPlayer 通过 ContentResolver 加载媒体 URI
        _prepared = true
        _duration = 30_000L // 占位值
        log.i { "prepare 完成" }
    }

    override fun play() {
        if (_prepared) {
            _isPlaying = true
            log.i { "play 开始" }
        } else {
            log.e { "play 失败：未 prepared" }
        }
    }

    override fun pause() {
        _isPlaying = false
        log.i { "暂停" }
    }

    override fun release() {
        _isPlaying = false
        _prepared = false
        log.i { "释放" }
    }

    override fun setMuted(muted: Boolean) {
        _muted = muted
    }

    override val isPlaying: Boolean get() = _isPlaying
    override val currentPosition: Long get() = _currentPosition
    override val duration: Long get() = _duration
}
