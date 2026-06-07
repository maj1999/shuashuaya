package com.cleanpic.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.cleanpic.currentLocalDate
import com.cleanpic.deviceStorage
import com.cleanpic.epochToLocalDate
import com.cleanpic.di.ServiceLocator
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.MediaTypeStats
import com.cleanpic.stats.CleanupQuotes
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.ui.viewer.formatBytes

@Composable
fun StatsScreen(router: AppRouter, theme: ThemeTokens) {
    val snapshot = remember { ServiceLocator.statsStore.load() }
    val storage = remember { deviceStorage() }
    val l = snapshot.lifetime
    val isStreak = remember {
        l.lastCleanupAt > 0L && epochToLocalDate(l.lastCleanupAt) == currentLocalDate()
    }
    val quote = remember { CleanupQuotes.pick(l, isStreak, seed = l.totalRounds) }

    val bg = Color(theme.colorBackground)
    val surface = Color(theme.colorSurface)
    val text = Color(theme.colorText)
    val sub = Color(theme.colorTextSecondary)
    val accent = Color(theme.colorAccent)
    val radius = theme.borderRadius.dp

    Column(modifier = Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(50.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPainter(
                name = "back",
                theme = theme,
                size = 22.dp,
                colorOverride = theme.colorText,
                modifier = Modifier.clickable { router.popBackStack() }
            )
            Spacer(Modifier.width(12.dp))
            Text("清理成果", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = text)
        }
        Spacer(Modifier.height(14.dp))

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(surface).padding(18.dp)) {
            Text("累计已清理", fontSize = 13.sp, color = sub)
            Spacer(Modifier.height(6.dp))
            Text(formatBytes(l.totalBytes), fontSize = 44.sp, fontWeight = FontWeight.Black, color = text)
            Spacer(Modifier.height(7.dp))
            Text("共 ${l.totalCount} 个文件 · 完成 ${l.totalRounds} 轮清理", fontSize = 12.sp, color = sub)
            Spacer(Modifier.height(13.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(sub.copy(alpha = 0.18f)))
            Spacer(Modifier.height(13.dp))
            Text(quote, fontSize = 13.sp, color = sub)
        }
        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(surface).padding(18.dp)) {
            Text("分类构成", fontSize = 11.sp, color = sub, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            TypeRow("photo", "照片", l.photo, l.totalBytes, "张", theme)
            Spacer(Modifier.height(14.dp))
            TypeRow("video", "视频", l.video, l.totalBytes, "个", theme)
        }
        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(surface).padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconPainter(
                        name = "storage",
                        theme = theme,
                        size = 14.dp,
                        colorOverride = theme.colorTextSecondary
                    )
                    Spacer(Modifier.width(7.dp))
                    Text("设备存储", fontSize = 12.sp, color = sub)
                }
                Text("已用 ${formatBytes(storage.usedBytes)} / ${formatBytes(storage.totalBytes)}", fontSize = 12.sp, color = sub)
            }
            Spacer(Modifier.height(8.dp))
            val frac = if (storage.totalBytes > 0) (storage.usedBytes.toFloat() / storage.totalBytes).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(99.dp)).background(sub.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(frac).height(9.dp).clip(RoundedCornerShape(99.dp)).background(accent))
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPainter(
                name = "shield",
                theme = theme,
                size = 14.dp,
                colorOverride = theme.colorTextSecondary
            )
            Spacer(Modifier.width(8.dp))
            Text("全程离线 · 清理记录仅存本机，不上传", fontSize = 11.sp, color = sub)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TypeRow(icon: String, name: String, s: MediaTypeStats, totalBytes: Long, unit: String, theme: ThemeTokens) {
    val text = Color(theme.colorText)
    val sub = Color(theme.colorTextSecondary)
    val accent = Color(theme.colorAccent)
    val pct = if (totalBytes > 0) (s.bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPainter(
                name = icon,
                theme = theme,
                size = 16.dp,
                colorOverride = theme.colorAccent
            )
            Spacer(Modifier.width(7.dp))
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = text)
        }
        Text("占 ${(pct * 100).roundToInt()}%", fontSize = 12.sp, color = sub)
    }
    Spacer(Modifier.height(8.dp))
    Row {
        Metric(formatBytes(s.bytes), "大小", theme); Spacer(Modifier.width(20.dp))
        Metric("${s.count}", unit, theme); Spacer(Modifier.width(20.dp))
        Metric("${s.rounds}", "轮", theme)
    }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(sub.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxWidth(pct).height(8.dp).clip(RoundedCornerShape(99.dp)).background(accent))
    }
}

@Composable
private fun Metric(value: String, label: String, theme: ThemeTokens) {
    Column {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(theme.colorText))
        Text(label, fontSize = 11.sp, color = Color(theme.colorTextSecondary))
    }
}
