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
import androidx.navigation.NavHostController
import com.cleanpic.di.ServiceLocator
import com.cleanpic.model.InteractionMode
import com.cleanpic.model.MediaType
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.common.EmptyStateScreen
import com.cleanpic.viewmodel.ViewerViewModel

@Composable
fun ViewerScreen(
    navController: NavHostController,
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
            navController.navigate("result") {
                popUpTo("viewer/{type}") { inclusive = true }
            }
        }
    }

    when {
        isLoading -> LoadingView(theme)
        isEmpty -> EmptyStateScreen(theme, type) {
            navController.popBackStack()
        }
        else -> {
            val mode = InteractionMode.fromId(
                ServiceLocator.appSettings.interactionMode
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(theme.colorBackground))
            ) {
                ProgressHeader(
                    theme = theme,
                    current = currentIndex + 1,
                    total = items.size
                )
                Box(modifier = Modifier.weight(1f)) {
                    when (mode) {
                        InteractionMode.CAROUSEL -> CarouselMode(
                            theme, viewerViewModel
                        )
                        InteractionMode.SWIPE_CARD -> SwipeCardMode(
                            theme, viewerViewModel
                        )
                        InteractionMode.FULLSCREEN -> FullscreenMode(
                            theme, viewerViewModel, navController
                        )
                    }
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
private fun ProgressHeader(theme: ThemeTokens, current: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
