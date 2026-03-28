package com.cleanpic.ui.result

import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.*
import androidx.compose.runtime.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.cleanpic.model.MediaType
import com.cleanpic.model.ViewerItem
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

    val pendingDeletes = items.filter {
        it.state == com.cleanpic.model.OperationState.PENDING_DELETE
    }
    val keptCount = items.count {
        it.state == com.cleanpic.model.OperationState.KEPT
    }
    val releasedBytes = pendingDeletes.sumOf { it.media.size }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.colorBackground))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "\uD83C\uDF89", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "\u672c\u8f6e\u6e05\u7406\u5b8c\u6210\uff01",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(theme.colorText)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Stat cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "\u5220\u9664",
                    value = "${pendingDeletes.size}",
                    color = Color(theme.colorDanger),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "\u4fdd\u7559",
                    value = "$keptCount",
                    color = Color(theme.colorSuccess),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "\u91ca\u653e",
                    value = formatBytes(releasedBytes),
                    color = Color(0xFF9C27B0),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Pending deletes preview
        if (pendingDeletes.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "\u5f85\u5220\u9664\u9879\u76ee",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(theme.colorText)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    items(pendingDeletes, key = { it.media.id }) { item ->
                        DeletePreviewItem(item, theme) {
                            viewerViewModel.cancelDelete(item.media.id)
                        }
                    }
                }
            }
        }

        // Confirm delete button
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
        if (pendingDeletes.isNotEmpty()) {
            item {
                Button(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            viewerViewModel.confirmDelete().fold(
                                onSuccess = { count ->
                                    confirmResult = "\u5df2\u6210\u529f\u5220\u9664 $count \u4e2a\u6587\u4ef6"
                                },
                                onFailure = { e ->
                                    confirmResult = when {
                                        e.message?.contains("cancel", true) == true ->
                                            "\u5df2\u53d6\u6d88\u5220\u9664"
                                        else -> "\u5220\u9664\u5931\u8d25\uff1a${e.message}"
                                    }
                                }
                            )
                            isDeleting = false
                        }
                    },
                    enabled = !isDeleting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(theme.borderRadius.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(theme.colorDanger)
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "\u786e\u8ba4\u5220\u9664",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Result message
        if (confirmResult != null) {
            item {
                Text(
                    text = confirmResult ?: "",
                    fontSize = 14.sp,
                    color = Color(theme.colorTextSecondary),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // Restart round
        item {
            OutlinedButton(
                onClick = {
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
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(theme.borderRadius.dp)
            ) {
                Text(text = "\uD83D\uDD04 \u518d\u6765\u4e00\u8f6e", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Go home
        item {
            TextButton(onClick = {
                viewerViewModel.clearSession()
                router.navigate(
                    Route.Home,
                    clearBackStackUpTo = Route.Home,
                    inclusive = true
                )
            }) {
                Text(
                    text = "\uD83C\uDFE0 \u8fd4\u56de\u9996\u9875",
                    fontSize = 16.sp,
                    color = Color(theme.colorPrimary)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    theme: ThemeTokens,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color(theme.colorTextSecondary))
    }
}

@Composable
private fun DeletePreviewItem(
    item: ViewerItem,
    theme: ThemeTokens,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(theme.colorSurface)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(60.dp).background(Color(theme.colorSurface)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (item.media.type == MediaType.PHOTO) "\uD83D\uDDBC\uFE0F" else "\uD83C\uDFAC",
                fontSize = 24.sp
            )
        }
        Text(
            text = item.media.name,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        TextButton(
            onClick = onCancel,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(24.dp)
        ) {
            Text(text = "\u2715 \u53d6\u6d88", fontSize = 10.sp, color = Color(theme.colorDanger))
        }
    }
}
