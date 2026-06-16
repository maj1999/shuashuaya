package com.cleanpic.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.ui.viewer.formatBytes

/** 成果卡的容器装饰风格——各主题套回自己的视觉签名。 */
enum class OutcomeCardDecoration {
    SHADOW,  // 柔和阴影白卡（Warm）
    BORDER,  // 毛玻璃半透明 + 描边（Playful）
    FLAT,    // 扁平 + 细线描边（Minimal）
    SOLID,   // 实心色块（Geometric）
    RULES,   // 无框，上下细分割线表格感（Editorial）
}

/**
 * 结果页「待确认 / 完成」两态共用的成果统一卡（US-CP-03 / US-CP-06 / US-CP-28）。
 *
 * 同一骨架（标签 + 放大主角 + 明细计数行 + 完成态注脚/语录），措辞与陪衬随态切换，
 * 消除原「三宫格 + 独立累计卡」里本轮字节重复出现的问题：
 *   待确认：标签「即将释放」+ 主角 freedSpace（静态）+ 明细「待删除 N · 拟保留 M」
 *   完成：  标签「本轮清理」+ 主角 roundBytes（滚动 0→X）+ 明细「已删除 N · 已保留 M」
 *           + 淡分隔 + 小字注脚「累计已清理 Y」+ 语录
 *
 * 结构统一、风格各异：容器装饰 / 圆角 / 字重 / 字间距 / 衬线 / 主角色由各主题布局传入，
 * 让 5 个主题在保持主次对调结构的同时，各自呈现应有的视觉签名。
 */
@Composable
fun ResultOutcomeCard(
    confirm: Boolean,
    freedSpace: String,
    roundBytes: Long,
    deletedCount: Int,
    keptCount: Int,
    lifetimeBytes: Long,
    quote: String?,
    textColor: Color,
    heroColor: Color,
    subColor: Color,
    surfaceColor: Color,
    decoration: OutcomeCardDecoration,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = Color.Transparent,
    heroWeight: FontWeight = FontWeight.Bold,
    labelLetterSpacing: TextUnit = 0.sp,
    serif: Boolean = false,
) {
    val font = if (serif) FontFamily.Serif else FontFamily.Default

    // 完成态主角本轮量从 0 滚到 roundBytes（900ms），给当下行为最强即时正反馈；
    // 待确认态主角为静态可释放量，不参与动画。
    val anim = remember { Animatable(0f) }
    LaunchedEffect(roundBytes, confirm) {
        if (confirm) {
            anim.snapTo(1f)
        } else {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        }
    }
    val heroText = if (confirm) freedSpace else formatBytes((roundBytes * anim.value).toLong())

    val label = if (confirm) "即将释放" else "本轮清理"
    val delLabel = if (confirm) "待删除" else "已删除"
    val keepLabel = if (confirm) "拟保留" else "已保留"

    val counts = buildAnnotatedString {
        append("$delLabel ")
        withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Bold)) { append("$deletedCount") }
        append("   ·   $keepLabel ")
        withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Bold)) { append("$keptCount") }
    }

    // 容器装饰：各主题套回自己的签名。RULES 无外框（上下细线在内容里画）。
    val shape = RoundedCornerShape(cornerRadius)
    val container = when (decoration) {
        OutcomeCardDecoration.SHADOW -> modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = shape, ambientColor = Color(0x1F5D4037), spotColor = Color(0x1F5D4037))
            .clip(shape)
            .background(surfaceColor)
            .padding(horizontal = 18.dp, vertical = 18.dp)
        OutcomeCardDecoration.BORDER -> modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 18.dp, vertical = 18.dp)
        OutcomeCardDecoration.FLAT -> modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 18.dp, vertical = 18.dp)
        OutcomeCardDecoration.SOLID -> modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .padding(horizontal = 18.dp, vertical = 20.dp)
        OutcomeCardDecoration.RULES -> modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    }

    val ruled = decoration == OutcomeCardDecoration.RULES

    Column(
        modifier = container,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (ruled) {
            HairLine(subColor)
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = font,
            color = subColor,
            letterSpacing = labelLetterSpacing,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = heroText,
            fontSize = 30.sp,
            fontFamily = font,
            fontWeight = heroWeight,
            // 完成态主角强制不透明：部分主题 colorSuccess 带低 alpha（如 Playful 0x4D…），
            // 直接用会让最重要的数字糊在背景上，故统一拉满 alpha 保证对比。
            color = if (confirm) textColor else heroColor.copy(alpha = 1f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = counts,
            fontSize = 13.sp,
            fontFamily = font,
            color = subColor,
        )

        if (!confirm) {
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(subColor.copy(alpha = 0.3f))
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "累计已清理 ${formatBytes(lifetimeBytes)}",
                fontSize = 11.sp,
                fontFamily = font,
                color = subColor,
                letterSpacing = labelLetterSpacing,
            )
            if (!quote.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = quote,
                    fontSize = 12.sp,
                    fontFamily = font,
                    color = subColor,
                )
            }
        }

        if (ruled) {
            Spacer(Modifier.height(18.dp))
            HairLine(subColor)
        }
    }
}

@Composable
private fun HairLine(color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color.copy(alpha = 0.35f))
    )
}
