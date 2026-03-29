package com.cleanpic.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            Text(text = "🎉", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "本轮清理完成！",
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
                    label = "删除",
                    value = "${pendingDeletes.size}",
                    color = Color(theme.colorDanger),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "保留",
                    value = "$keptCount",
                    color = Color(theme.colorSuccess),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "释放",
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
                    text = "待删除项目",
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
                                    confirmResult = "已成功删除 $count 个文件"
                                },
                                onFailure = { e ->
                                    confirmResult = when {
                                        e.message?.contains("cancel", true) == true ->
                                            "已取消删除"
                                        else -> "删除失败：${e.message}"
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
                            text = "确认删除",
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
                Text(text = "🔄 再来一轮", fontSize = 16.sp)
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
                    text = "🏠 返回首页",
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
                text = if (item.media.type == MediaType.PHOTO) "🖼️" else "🎬",
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
            Text(text = "✕ 取消", fontSize = 10.sp, color = Color(theme.colorDanger))
        }
    }
}
