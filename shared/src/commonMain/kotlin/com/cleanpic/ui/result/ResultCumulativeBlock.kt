package com.cleanpic.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.ui.viewer.formatBytes

/**
 * 结果页「完成」态的成果注脚（spec §6.3）：
 * 本次清理 X + 累计已清理 Y（数字滚动动画）+ 一句语录。
 *
 * 5 个布局视觉各异，故配色由调用方传入；本块只负责结构 + 动画一致。
 * 累计数字从「上一次累计（Y − 本次）」滚动到「Y」，强调本轮带来的增量；
 * 本次数字从 0 滚动到 X。
 */
@Composable
fun ResultCumulativeBlock(
    roundBytes: Long,
    lifetimeBytes: Long,
    quote: String?,
    textColor: Color,
    subColor: Color,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    serif: Boolean = false,
) {
    val font = if (serif) FontFamily.Serif else FontFamily.Default
    val prevLifetime = (lifetimeBytes - roundBytes).coerceAtLeast(0L)

    val anim = remember { Animatable(0f) }
    LaunchedEffect(roundBytes, lifetimeBytes) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    val frac = anim.value
    val roundShown = (roundBytes * frac).toLong()
    val lifeShown = prevLifetime + ((lifetimeBytes - prevLifetime) * frac).toLong()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "本次清理",
            fontSize = 11.sp,
            fontFamily = font,
            color = subColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatBytes(roundShown),
            fontSize = 17.sp,
            fontFamily = font,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "累计已清理",
            fontSize = 11.sp,
            fontFamily = font,
            color = subColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatBytes(lifeShown),
            fontSize = 30.sp,
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
        if (!quote.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(subColor.copy(alpha = 0.3f))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = quote,
                fontSize = 12.sp,
                fontFamily = font,
                color = subColor,
            )
        }
    }
}
