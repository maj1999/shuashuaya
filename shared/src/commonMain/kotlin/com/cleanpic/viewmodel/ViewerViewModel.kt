package com.cleanpic.viewmodel

import com.cleanpic.di.ServiceLocator
import com.cleanpic.media.RandomPicker
import com.cleanpic.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ViewerViewModel {
    private val repo get() = ServiceLocator.mediaRepository
    private val settings get() = ServiceLocator.appSettings

    private val _items = MutableStateFlow<List<ViewerItem>>(emptyList())
    val items: StateFlow<List<ViewerItem>> = _items

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty

    private val _shownIds = mutableSetOf<String>()

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
        val picked = RandomPicker.pick(all, settings.roundCount, _shownIds)
        _shownIds.addAll(picked.map { it.id })
        _items.value = picked.map { ViewerItem(it) }
        _currentIndex.value = 0
        _isLoading.value = false
    }

    fun markKept() { updateCurrent(OperationState.KEPT); advance() }
    fun markDelete() { updateCurrent(OperationState.PENDING_DELETE); advance() }

    fun cancelDelete(id: String) {
        _items.value = _items.value.map {
            if (it.media.id == id) it.copy(state = OperationState.KEPT) else it
        }
    }

    suspend fun confirmDelete(): Result<Int> {
        val items = pendingDeletes.map { it.media }
        if (items.isEmpty()) return Result.success(0)
        return repo.deleteMediaItems(items)
    }

    fun resetForNextRound() { _currentIndex.value = 0 }
    fun clearSession() { _shownIds.clear() }

    private fun updateCurrent(state: OperationState) {
        val idx = _currentIndex.value
        val list = _items.value.toMutableList()
        if (idx < list.size) { list[idx] = list[idx].copy(state = state); _items.value = list }
    }

    private fun advance() {
        if (_currentIndex.value < _items.value.size) _currentIndex.value++
    }
}
