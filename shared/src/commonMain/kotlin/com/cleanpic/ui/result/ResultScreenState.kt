package com.cleanpic.ui.result

import com.cleanpic.model.MediaItem
import com.cleanpic.theme.ThemeTokens

/**
 * 结果页的共享状态 — 5 个布局变体通过此接口接收数据和回调。
 */
data class ResultScreenState(
    val theme: ThemeTokens,
    /** 结果页当前阶段：CONFIRM=待确认（删除前），DONE=完成（删除后/无待删除） */
    val phase: ResultPhase,
    /** 待确认态标题，如"即将删除 4 项" */
    val confirmTitle: String,
    /** 待确认态的不可撤销提示文案 */
    val irreversibleHint: String,
    val deletedCount: Int,
    val keptCount: Int,
    val freedSpace: String,
    /** 本轮真实清理字节（完成态滚动动画用），与 freedSpace 同源。 */
    val freedBytes: Long = 0L,
    /** 累计已清理字节（含本轮），完成态成果注脚展示。 */
    val lifetimeBytes: Long = 0L,
    /** 完成态语录（治愈温柔·情境化选句）；待确认态为 null。 */
    val quote: String? = null,
    val pendingDeleteItems: List<MediaItem>,
    val isDeleting: Boolean,
    val deleteResult: String?,
    val onConfirmDelete: () -> Unit,
    val onCancelItem: (MediaItem) -> Unit,
    /** 点击待删除缩略图 → 进入全屏预览（US-CP-24） */
    val onPreviewItem: (MediaItem) -> Unit,
    val onNextRound: () -> Unit,
    val onGoHome: () -> Unit
)
