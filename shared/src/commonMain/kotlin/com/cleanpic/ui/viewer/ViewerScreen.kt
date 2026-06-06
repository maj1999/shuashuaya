package com.cleanpic.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.di.ServiceLocator
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.InteractionMode
import androidx.compose.ui.platform.testTag
import com.cleanpic.model.MediaType
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.common.EmptyStateScreen
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.ui.navigation.Route
import com.cleanpic.viewmodel.ViewerViewModel

@Composable
fun ViewerScreen(
    router: AppRouter,
    theme: ThemeTokens,
    viewerViewModel: ViewerViewModel,
    type: MediaType
) {
    val items by viewerViewModel.items.collectAsState()
    val currentIndex by viewerViewModel.currentIndex.collectAsState()
    val isLoading by viewerViewModel.isLoading.collectAsState()
    val isEmpty by viewerViewModel.isEmpty.collectAsState()

    LaunchedEffect(viewerViewModel.isComplete, currentIndex) {
        if (items.isNotEmpty() && currentIndex >= items.size) {
            router.navigate(
                Route.Result,
                clearBackStackUpTo = Route.Viewer(type),
                inclusive = true
            )
        }
    }

    when {
        isLoading -> LoadingView(theme)
        isEmpty -> EmptyStateScreen(theme, type) {
            router.popBackStack()
        }
        else -> {
            val mode = InteractionMode.fromId(
                ServiceLocator.appSettings.interactionMode
            )
            val canUndo by viewerViewModel.canUndo.collectAsState()
            // 轮播/卡片模式下点击媒体进入的全屏叠层；全屏上下滑模式本就全屏，不使用
            var showFullscreen by remember { mutableStateOf(false) }
            LaunchedEffect(currentIndex) { showFullscreen = false }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(theme.colorBackground))
                ) {
                    if (mode != InteractionMode.FULLSCREEN) {
                        ProgressHeader(
                            theme = theme,
                            current = currentIndex + 1,
                            total = items.size,
                            onExit = { router.popBackStack() }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        // 全屏叠层打开时不组合底层交互模式：否则底层视频播放器仍在后台播放，
                        // 会与全屏播放器同时发声，导致"全屏图标显示静音但仍有声音"。
                        // 移出组合树会触发其 ExoPlayer 的 onDispose 释放，保证同一时刻只有一个播放器在放。
                        if (!showFullscreen) {
                            when (mode) {
                                InteractionMode.CAROUSEL -> CarouselMode(
                                    theme, viewerViewModel,
                                    onMediaClick = { showFullscreen = true }
                                )
                                InteractionMode.SWIPE_CARD -> SwipeCardMode(
                                    theme, viewerViewModel,
                                    onMediaClick = { showFullscreen = true }
                                )
                                InteractionMode.FULLSCREEN -> FullscreenMode(
                                    theme, viewerViewModel, router
                                )
                            }
                        }
                    }
                }

                // 点击进入全屏查看（盖住整屏，含 ProgressHeader）
                if (showFullscreen && currentIndex < items.size) {
                    FullscreenViewer(
                        item = items[currentIndex],
                        theme = theme,
                        current = currentIndex + 1,
                        total = items.size,
                        canUndo = canUndo,
                        onUndo = { viewerViewModel.undo(); showFullscreen = false },
                        onDelete = { viewerViewModel.markDelete(); showFullscreen = false },
                        onKeep = { viewerViewModel.markKept(); showFullscreen = false },
                        onBack = { showFullscreen = false },
                        backLabel = "返回"
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingView(theme: ThemeTokens) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(theme.colorPrimary))
    }
}

@Composable
private fun ProgressHeader(
    theme: ThemeTokens,
    current: Int,
    total: Int,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onExit,
                modifier = Modifier.testTag("exit_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconPainter("back", theme, size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "退出",
                        fontSize = 14.sp,
                        color = Color(theme.colorTextSecondary)
                    )
                }
            }
            Text(
                text = "$current / $total",
                fontSize = 14.sp,
                color = Color(theme.colorTextSecondary)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { current.toFloat() / total.coerceAtLeast(1) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color(theme.colorPrimary),
            trackColor = Color(theme.colorSurface)
        )
    }
}
