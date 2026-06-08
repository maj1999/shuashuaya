package com.cleanpic.ui.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.cleanpic.currentLocalDate
import com.cleanpic.deviceStorage
import com.cleanpic.epochToLocalDate
import com.cleanpic.di.ServiceLocator
import com.cleanpic.icons.IconPainter
import com.cleanpic.model.MediaTypeStats
import com.cleanpic.stats.Badge
import com.cleanpic.stats.CleanupQuotes
import com.cleanpic.stats.Milestones
import com.cleanpic.stats.MonthlyReview
import com.cleanpic.stats.StatsStreak
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
    val streak = remember { StatsStreak.current(snapshot.daily, currentLocalDate()) }
    val badges = remember { Milestones.evaluate(l) }
    val achievedCount = badges.count { it.achieved }
    val months = remember { MonthlyReview.byMonth(snapshot.daily) }

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
            if (streak > 0) {
                Spacer(Modifier.height(5.dp))
                Text("已连续清理 $streak 天", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = accent)
            }
            Spacer(Modifier.height(13.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(sub.copy(alpha = 0.18f)))
            Spacer(Modifier.height(13.dp))
            Text(quote, fontSize = 13.sp, color = sub)
        }
        Spacer(Modifier.height(12.dp))

        // 里程碑徽章（阶段二）
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(surface).padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("里程碑", fontSize = 11.sp, color = sub, fontWeight = FontWeight.Medium)
                Text("$achievedCount / ${badges.size}", fontSize = 11.sp, color = sub)
            }
            Spacer(Modifier.height(14.dp))
            badges.chunked(4).forEach { rowBadges ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowBadges.forEach { BadgeChip(it, theme, Modifier.weight(1f)) }
                    repeat(4 - rowBadges.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        // 分类构成（阶段二：构成饼图 + 三元组占比条）
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(surface).padding(18.dp)) {
            Text("分类构成", fontSize = 11.sp, color = sub, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CompositionDonut(
                    photoBytes = l.photo.bytes,
                    videoBytes = l.video.bytes,
                    photoColor = accent,
                    videoColor = accent.copy(alpha = 0.42f),
                    trackColor = sub.copy(alpha = 0.15f),
                    centerText = formatBytes(l.totalBytes),
                    textColor = text,
                    subColor = sub,
                )
            }
            Spacer(Modifier.height(16.dp))
            TypeRow("photo", "照片", l.photo, l.totalBytes, "张", theme)
            Spacer(Modifier.height(14.dp))
            TypeRow("video", "视频", l.video, l.totalBytes, "个", theme)
        }
        Spacer(Modifier.height(12.dp))

        // 月度回顾（阶段三）：按自然月聚合的清理汇总
        if (months.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(surface).padding(18.dp)) {
                Text("月度回顾", fontSize = 11.sp, color = sub, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                months.take(3).forEachIndexed { i, m ->
                    if (i > 0) Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMonthLabel(m.yearMonth), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = text)
                        Text("${formatBytes(m.totalBytes)} · ${m.totalCount} 个 · ${m.totalRounds} 轮", fontSize = 12.sp, color = sub)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

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

/** "2026-06" → "2026 年 6 月"。 */
private fun formatMonthLabel(yearMonth: String): String {
    val parts = yearMonth.split("-")
    if (parts.size != 2) return yearMonth
    val y = parts[0]
    val m = parts[1].toIntOrNull() ?: return yearMonth
    return "$y 年 $m 月"
}

@Composable
private fun BadgeChip(badge: Badge, theme: ThemeTokens, modifier: Modifier) {
    val accent = Color(theme.colorAccent)
    val sub = Color(theme.colorTextSecondary)
    val on = badge.achieved
    // colorOverride 需 ARGB Long：未达成用 colorTextSecondary 降到 0x66 透明度。
    val dimLong = (0x66L shl 24) or (theme.colorTextSecondary and 0xFFFFFFL)
    val iconColorLong = if (on) theme.colorAccent else dimLong
    val tint = if (on) accent else sub.copy(alpha = 0.4f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (on) accent.copy(alpha = 0.14f) else sub.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            IconPainter(name = badge.icon, theme = theme, size = 20.dp, colorOverride = iconColorLong)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            badge.label,
            fontSize = 9.sp,
            color = tint,
            textAlign = TextAlign.Center,
            fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/** 照片/视频按字节构成的环形图；总量为 0 时画一圈灰底轨道。 */
@Composable
private fun CompositionDonut(
    photoBytes: Long,
    videoBytes: Long,
    photoColor: Color,
    videoColor: Color,
    trackColor: Color,
    centerText: String,
    textColor: Color,
    subColor: Color,
) {
    val total = photoBytes + videoBytes
    val photoFrac = if (total > 0) (photoBytes.toFloat() / total).coerceIn(0f, 1f) else 0f
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(132.dp)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            // 轨道底
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (total > 0) {
                val photoSweep = photoFrac * 360f
                drawArc(
                    color = photoColor,
                    startAngle = -90f,
                    sweepAngle = photoSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                drawArc(
                    color = videoColor,
                    startAngle = -90f + photoSweep,
                    sweepAngle = 360f - photoSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text("总计", fontSize = 10.sp, color = subColor)
        }
    }
}
