package com.cleanpic.ui.result

import androidx.compose.runtime.*
import com.cleanpic.currentLocalDate
import com.cleanpic.di.ServiceLocator
import com.cleanpic.epochToLocalDate
import com.cleanpic.model.MediaType
import com.cleanpic.model.OperationState
import com.cleanpic.stats.CleanupQuotes
import com.cleanpic.theme.ThemeLayoutId
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.ui.navigation.Route
import com.cleanpic.ui.viewer.formatBytes
import com.cleanpic.viewmodel.ViewerViewModel
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(
    router: AppRouter,
    theme: ThemeTokens,
    viewerViewModel: ViewerViewModel
) {
    val scope = rememberCoroutineScope()
    val items by viewerViewModel.items.collectAsState()
    var confirmResult by remember { mutableStateOf<String?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteConfirmed by remember { mutableStateOf(false) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    val pendingDeletes = items.filter { it.state == OperationState.PENDING_DELETE }
    val keptCount = items.count { it.state == OperationState.KEPT }
    val releasedBytes = pendingDeletes.sumOf { it.media.size }

    val phase = resolveResultPhase(pendingDeletes.size, deleteConfirmed)

    // 完成态成果注脚：累计统计随删除落库后刷新；语录情境化选句。
    // 轮次已在到达结果页时记入；删除在 confirmDelete 落库，故 deleteConfirmed 翻转后重读。
    val lifetime = remember(deleteConfirmed) { ServiceLocator.statsStore.load().lifetime }
    val quote = remember(deleteConfirmed) {
        val isStreak = lifetime.lastCleanupAt > 0L &&
            epochToLocalDate(lifetime.lastCleanupAt) == currentLocalDate()
        CleanupQuotes.pick(lifetime, isStreak, seed = lifetime.totalRounds)
    }

    val state = ResultScreenState(
        theme = theme,
        phase = phase,
        confirmTitle = "即将删除 ${pendingDeletes.size} 项",
        irreversibleHint = "删除后不可在 App 内撤销",
        deletedCount = pendingDeletes.size,
        keptCount = keptCount,
        freedSpace = formatBytes(releasedBytes),
        freedBytes = releasedBytes,
        lifetimeBytes = lifetime.totalBytes,
        quote = if (phase == ResultPhase.DONE) quote else null,
        pendingDeleteItems = if (!deleteConfirmed) pendingDeletes.map { it.media } else emptyList(),
        isDeleting = isDeleting,
        deleteResult = confirmResult,
        onConfirmDelete = {
            isDeleting = true
            scope.launch {
                viewerViewModel.confirmDelete().fold(
                    onSuccess = { count ->
                        confirmResult = "已成功删除 $count 个文件"
                        deleteConfirmed = true
                    },
                    onFailure = { e ->
                        confirmResult = when {
                            e.message?.contains("cancel", true) == true -> "已取消删除"
                            else -> "删除失败：${e.message}"
                        }
                    }
                )
                isDeleting = false
            }
        },
        onCancelItem = { mediaItem ->
            viewerViewModel.cancelDelete(mediaItem.id)
        },
        onPreviewItem = { mediaItem ->
            val i = pendingDeletes.indexOfFirst { it.media.id == mediaItem.id }
            if (i >= 0) previewIndex = i
        },
        onNextRound = {
            val type = items.firstOrNull()?.media?.type ?: MediaType.PHOTO
            scope.launch {
                viewerViewModel.loadMedia(type)
                router.navigate(
                    Route.Viewer(type),
                    clearBackStackUpTo = Route.Result,
                    inclusive = true
                )
            }
        },
        onGoHome = {
            // 不再清空浏览记忆：回首页应保留记忆，避免下轮重复（US-CP-22）。
            // 清空仅由设置页「重置浏览记录」显式触发。
            router.navigate(
                Route.Home,
                clearBackStackUpTo = Route.Home,
                inclusive = true
            )
        }
    )

    when (theme.layoutId) {
        ThemeLayoutId.MINIMAL   -> MinimalResultLayout(state)
        ThemeLayoutId.GEOMETRIC -> GeometricResultLayout(state)
        ThemeLayoutId.WARM      -> WarmResultLayout(state)
        ThemeLayoutId.PLAYFUL   -> PlayfulResultLayout(state)
        ThemeLayoutId.EDITORIAL -> EditorialResultLayout(state)
    }

    // 待删除项全屏预览（US-CP-24）—— 仅待确认态、列表非空时
    val previewItems = pendingDeletes.map { it.media }
    LaunchedEffect(previewItems.isEmpty()) {
        if (previewItems.isEmpty()) previewIndex = null
    }
    val idx = previewIndex
    if (idx != null && !deleteConfirmed && previewItems.isNotEmpty()) {
        DeletePreviewOverlay(
            items = previewItems,
            startIndex = idx,
            theme = theme,
            onCancelDelete = { viewerViewModel.cancelDelete(it.id) },
            onBack = { previewIndex = null }
        )
    }
}
