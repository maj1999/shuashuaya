package com.cleanpic.ui.viewer

import androidx.compose.runtime.*
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.viewmodel.ViewerViewModel

/**
 * 「全屏上下滑」交互模式 — [FullscreenViewer] 的薄封装。
 * 媒体本就全屏展示；返回 = 退出浏览（popBackStack）。
 */
@Composable
fun FullscreenMode(
    theme: ThemeTokens,
    viewerViewModel: ViewerViewModel,
    router: AppRouter
) {
    val items by viewerViewModel.items.collectAsState()
    val currentIndex by viewerViewModel.currentIndex.collectAsState()
    val canUndo by viewerViewModel.canUndo.collectAsState()
    if (items.isEmpty() || currentIndex >= items.size) return

    FullscreenViewer(
        item = items[currentIndex],
        theme = theme,
        current = currentIndex + 1,
        total = items.size,
        canUndo = canUndo,
        onUndo = { viewerViewModel.undo() },
        onDelete = { viewerViewModel.markDelete() },
        onKeep = { viewerViewModel.markKept() },
        onBack = { router.popBackStack() },
        backLabel = "退出"
    )
}
