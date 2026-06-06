package com.cleanpic.viewmodel

import com.cleanpic.currentEpochMillis
import com.cleanpic.di.ServiceLocator
import com.cleanpic.media.RandomPicker
import com.cleanpic.media.SeenRecord
import com.cleanpic.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ViewerViewModel {
    private val repo get() = ServiceLocator.mediaRepository
    private val settings get() = ServiceLocator.appSettings
    private val pickStore get() = ServiceLocator.pickStateStore

    private val _items = MutableStateFlow<List<ViewerItem>>(emptyList())
    val items: StateFlow<List<ViewerItem>> = _items

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    private var currentType: MediaType? = null

    val totalCount: Int get() = _items.value.size
    val isComplete: Boolean get() = _currentIndex.value >= _items.value.size

    val pendingDeletes: List<ViewerItem>
        get() = _items.value.filter { it.state == OperationState.PENDING_DELETE }
    val keptCount: Int get() = _items.value.count { it.state == OperationState.KEPT }
    val deletedCount: Int get() = pendingDeletes.size
    val releasedBytes: Long get() = pendingDeletes.sumOf { it.media.size }

    suspend fun loadMedia(type: MediaType) {
        _isLoading.value = true
        _isEmpty.value = false
        val all = when (type) {
            MediaType.PHOTO -> repo.queryPhotos()
            MediaType.VIDEO -> repo.queryVideos()
        }
        if (all.isEmpty()) {
            _isEmpty.value = true
            _isLoading.value = false
            return
        }
        currentType = type
        val state = pickStore.load(type)
        val result = RandomPicker.pick(all, settings.roundCount, state, now = currentEpochMillis())
        pickStore.save(type, result.state)
        _items.value = result.items.map { ViewerItem(it) }
        _currentIndex.value = 0
        _isLoading.value = false
        resetUndo()
    }

    fun markKept() { val id = currentId(); updateCurrent(OperationState.KEPT); persistKept(id, true); advance() }
    fun markDelete() { val id = currentId(); updateCurrent(OperationState.PENDING_DELETE); persistKept(id, false); advance() }

    /**
     * 轮播模式：向后切换到下一个媒体（左滑）。
     * 离开当前媒体时，若仍未决策（PENDING）则默认标记为保留（KEPT）；
     * 若之前已选择过删除或保留，则保持原有决策不变。
     * 越过最后一个媒体即触发本轮完成。
     */
    fun goNext() {
        val idx = _currentIndex.value
        if (idx >= _items.value.size) return
        val list = _items.value.toMutableList()
        if (list[idx].state == OperationState.PENDING) {
            list[idx] = list[idx].copy(state = OperationState.KEPT)
            _items.value = list
            persistKept(list[idx].media.id, true)  // 默认保留：记忆沉底
        }
        _currentIndex.value = idx + 1
        refreshCanUndo()
    }

    /**
     * 轮播模式：向前切换到上一个媒体（右滑），仅移动位置，
     * 各媒体保持原有决策不变。已在第一个时无副作用。
     */
    fun goPrevious() {
        val idx = _currentIndex.value
        if (idx <= 0) return
        _currentIndex.value = idx - 1
        refreshCanUndo()
    }

    /**
     * 回退到上一个媒体并恢复其待决策态，可连续回退直至第一个媒体。
     * 已位于第一个（index 0）时无副作用。
     */
    fun undo() {
        val idx = _currentIndex.value
        if (idx <= 0) return
        val prev = idx - 1
        val list = _items.value.toMutableList()
        list[prev] = list[prev].copy(state = OperationState.PENDING)
        _items.value = list
        _currentIndex.value = prev
        refreshCanUndo()
    }

    /** 只要当前不在第一个媒体，就还能继续回退。 */
    private fun refreshCanUndo() {
        _canUndo.value = _currentIndex.value > 0
    }

    private fun resetUndo() {
        _canUndo.value = false
    }

    fun cancelDelete(id: String) {
        _items.value = _items.value.map {
            if (it.media.id == id) it.copy(state = OperationState.KEPT) else it
        }
    }

    suspend fun confirmDelete(): Result<Int> {
        val items = pendingDeletes.map { it.media }
        if (items.isEmpty()) return Result.success(0)
        val result = repo.deleteMediaItems(items)
        if (result.isSuccess) forgetRecords(items.map { it.id })
        return result
    }

    fun resetForNextRound() { _currentIndex.value = 0; resetUndo() }

    /** 重置浏览记录（US-CP-23）：清空全部浏览记忆。 */
    fun clearSession() { pickStore.clearAll() }

    private fun currentId(): String? = _items.value.getOrNull(_currentIndex.value)?.media?.id

    /** 把某媒体的"保留过"标记写入浏览记忆（沉底/取消沉底）。 */
    private fun persistKept(id: String?, kept: Boolean) {
        val type = currentType ?: return
        if (id == null) return
        val state = pickStore.load(type)
        val prev = state.records[id]
        val updated = if (prev != null) {
            prev.copy(kept = kept)
        } else {
            SeenRecord(lastDrawnCycle = state.cycle, lastSeenMillis = currentEpochMillis(), kept = kept)
        }
        pickStore.save(type, state.copy(records = state.records + (id to updated)))
    }

    /** 删除成功后，从浏览记忆移除这些 id（自愈）。 */
    private fun forgetRecords(ids: List<String>) {
        val type = currentType ?: return
        val state = pickStore.load(type)
        if (state.records.keys.none { it in ids }) return
        pickStore.save(type, state.copy(records = state.records.filterKeys { it !in ids }))
    }

    private fun updateCurrent(state: OperationState) {
        val idx = _currentIndex.value
        val list = _items.value.toMutableList()
        if (idx < list.size) { list[idx] = list[idx].copy(state = state); _items.value = list }
    }

    private fun advance() {
        if (_currentIndex.value < _items.value.size) _currentIndex.value++
        refreshCanUndo()
    }
}
