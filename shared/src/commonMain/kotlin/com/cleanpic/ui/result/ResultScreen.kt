package com.cleanpic.ui.result

import androidx.compose.runtime.*
import com.cleanpic.model.MediaType
import com.cleanpic.model.OperationState
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

    val pendingDeletes = items.filter { it.state == OperationState.PENDING_DELETE }
    val keptCount = items.count { it.state == OperationState.KEPT }
    val releasedBytes = pendingDeletes.sumOf { it.media.size }

    val state = ResultScreenState(
        theme = theme,
        deletedCount = pendingDeletes.size,
        keptCount = keptCount,
        freedSpace = formatBytes(releasedBytes),
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
            viewerViewModel.clearSession()
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
}
