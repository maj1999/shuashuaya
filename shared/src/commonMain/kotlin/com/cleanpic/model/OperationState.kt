package com.cleanpic.model

enum class OperationState { PENDING, KEPT, PENDING_DELETE }

data class ViewerItem(
    val media: MediaItem,
    val state: OperationState = OperationState.PENDING,
    val thumbnailLoaded: Boolean = false
)
