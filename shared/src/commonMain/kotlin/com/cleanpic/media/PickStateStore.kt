package com.cleanpic.media

import com.cleanpic.model.MediaType

/**
 * 浏览记忆的持久化抽象。按 [MediaType] 分别存取（照片/视频各一份）。
 *
 * Android 实现走 SharedPreferences；iOS / HarmonyOS 暂用内存实现占位。
 */
interface PickStateStore {
    fun load(type: MediaType): PickState
    fun save(type: MediaType, state: PickState)
    /** 重置浏览记录：清空全部记忆（US-CP-23）。 */
    fun clearAll()
}

/** 内存实现：用于单元测试与尚未接入持久化的平台（iOS / HarmonyOS）。 */
class InMemoryPickStateStore : PickStateStore {
    private val map = mutableMapOf<MediaType, PickState>()
    override fun load(type: MediaType): PickState = map[type] ?: PickState()
    override fun save(type: MediaType, state: PickState) { map[type] = state }
    override fun clearAll() { map.clear() }
}
