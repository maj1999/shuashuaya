package com.cleanpic.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cleanpic.theme.IconStrokeCap
import com.cleanpic.theme.ThemeTokens

@Composable
fun IconPainter(
    name: String,
    theme: ThemeTokens,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    colorOverride: Long? = null
) {
    val icon = AppIcons.get(name, theme)
    val color = Color(colorOverride ?: icon.strokeColor)
    val cap = when (icon.strokeCap) {
        IconStrokeCap.BUTT -> StrokeCap.Butt
        IconStrokeCap.ROUND -> StrokeCap.Round
        IconStrokeCap.SQUARE -> StrokeCap.Square
    }

    Canvas(modifier = modifier.size(size)) {
        val path = parseSvgPath(icon.pathData)
        val scaleX = this.size.width / icon.viewportWidth
        val scaleY = this.size.height / icon.viewportHeight
        val matrix = Matrix().apply { scale(scaleX, scaleY) }
        path.transform(matrix)
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = icon.strokeWidth * scaleX,
                cap = cap,
                join = StrokeJoin.Round
            )
        )
    }
}
