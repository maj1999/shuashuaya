package com.cleanpic.viewmodel

class ResultViewModel(private val viewerViewModel: ViewerViewModel) {
    val pendingDeletes get() = viewerViewModel.pendingDeletes
    val keptCount get() = viewerViewModel.keptCount
    val deletedCount get() = viewerViewModel.deletedCount
    val releasedBytes get() = viewerViewModel.releasedBytes

    fun cancelDelete(id: String) = viewerViewModel.cancelDelete(id)
    suspend fun confirmDelete() = viewerViewModel.confirmDelete()
}
