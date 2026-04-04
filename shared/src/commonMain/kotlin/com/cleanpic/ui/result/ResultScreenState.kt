package com.cleanpic.ui.result

import com.cleanpic.model.MediaItem
import com.cleanpic.theme.ThemeTokens

/**
 * 结果页的共享状态 — 5 个布局变体通过此接口接收数据和回调。
 */
data class ResultScreenState(
    val theme: ThemeTokens,
    val deletedCount: Int,
    val keptCount: Int,
    val freedSpace: String,
    val pendingDeleteItems: List<MediaItem>,
    val isDeleting: Boolean,
    val deleteResult: String?,
    val onConfirmDelete: () -> Unit,
    val onCancelItem: (MediaItem) -> Unit,
    val onNextRound: () -> Unit,
    val onGoHome: () -> Unit
)
