package com.cleanpic.media

interface VideoPlayer {
    fun prepare(mediaId: String)
    fun play()
    fun pause()
    fun release()
    fun setMuted(muted: Boolean)
    val isPlaying: Boolean
    val currentPosition: Long
    val duration: Long
}
