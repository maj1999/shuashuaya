package com.cleanpic.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.model.MediaItem
import com.cleanpic.ui.media.MediaImage

private val EditorialBg = Color(0xFFFFFFF5)
private val EditorialText = Color(0xFF1A1A1A)
private val EditorialSecondary = Color(0xFF999999)
private val EditorialDivider = Color(0xFFE0DDD6)
private val EditorialDanger = Color(0xFFCC3333)

@Composable
fun EditorialResultLayout(state: ResultScreenState) {
    val theme = state.theme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialBg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 报头
        item {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "SUMMARY",
                fontSize = 9.sp,
                fontFamily = FontFamily.Serif,
                color = EditorialSecondary,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "清理报告",
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                color = EditorialText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(1.dp)
                    .background(EditorialDivider)
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        // 表格式统计
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialDivider, RectangleShape)
            ) {
                EditorialStatCell(
                    value = "${state.deletedCount}",
                    label = "DISCARD",
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(72.dp)
                        .background(EditorialDivider)
                )
                EditorialStatCell(
                    value = "${state.keptCount}",
                    label = "KEEP",
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(72.dp)
                        .background(EditorialDivider)
                )
                EditorialStatCell(
                    value = state.freedSpace,
                    label = "MB FREED",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // 待删除预览
        if (state.pendingDeleteItems.isNotEmpty()) {
            item {
                Text(
                    text = "PENDING DELETION",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Serif,
                    color = EditorialSecondary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    items(state.pendingDeleteItems, key = { it.id }) { mediaItem ->
                        EditorialDeletePreviewItem(
                            mediaItem = mediaItem,
                            onCancel = { state.onCancelItem(mediaItem) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 确认删除按钮
        if (state.pendingDeleteItems.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, EditorialDanger, RectangleShape)
                        .clickable(enabled = !state.isDeleting, onClick = state.onConfirmDelete),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = EditorialDanger,
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Text(
                            text = "CONFIRM DELETE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            color = EditorialDanger,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 删除结果消息
        if (state.deleteResult != null) {
            item {
                Text(
                    text = state.deleteResult,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    color = EditorialSecondary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(EditorialDivider)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 文字链接底部操作
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NEXT ROUND →",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = EditorialText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.clickable(onClick = state.onNextRound)
                )
                Text(
                    text = "HOME →",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = EditorialText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.clickable(onClick = state.onGoHome)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun EditorialStatCell(
    value: String,
    label: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .height(72.dp)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            color = EditorialText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 8.sp,
            fontFamily = FontFamily.Serif,
            color = EditorialSecondary,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EditorialDeletePreviewItem(
    mediaItem: MediaItem,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .border(1.dp, EditorialDivider, RectangleShape),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MediaImage(
            item = mediaItem,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "×",
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            color = EditorialSecondary,
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(bottom = 4.dp)
        )
    }
}
