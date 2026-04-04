package com.cleanpic.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 鸭子 Logo 的 SVG path data。
 * 素材来源：https://www.svgrepo.com/svg/117055/small-duck（CC0 公共领域）
 * 原始 viewBox: 0 0 209.322 209.322
 */
object LogoPaths {
    /** 原始 viewBox 尺寸 */
    const val VIEWPORT = 209.322f

    /** 鸭身主路径 */
    const val DUCK_BODY = "M105.572,101.811c9.889,-6.368 27.417,-16.464 28.106,-42.166c0.536,-20.278 -9.971,-49.506 -49.155,-50.878C53.041,7.659 39.9,28.251 36.071,46.739l-0.928,-0.126c-1.932,0 -3.438,1.28 -5.34,2.889c-2.084,1.784 -4.683,3.979 -7.792,4.308c-3.573,0.361 -8.111,-1.206 -11.698,-2.449c-4.193,-1.431 -6.624,-2.047 -8.265,-0.759c-1.503,1.163 -2.178,3.262 -2.028,6.226c0.331,6.326 4.971,18.917 16.016,25.778c7.67,4.765 16.248,5.482 20.681,5.482c0.006,0 0.006,0 0.006,0c2.37,0 4.945,-0.239 7.388,-0.726c2.741,4.218 5.228,7.476 6.037,9.752c2.054,5.851 -27.848,25.087 -27.848,55.01c0,29.916 22.013,48.475 56.727,48.475h55.004c30.593,0 70.814,-29.908 75.291,-92.48C180.781,132.191 167.028,98.15 105.572,101.811z"

    /** 嘴巴路径 */
    const val DUCK_MOUTH = "M18.941,77.945C8.775,71.617 4.992,58.922 5.294,55.525c0.897,0.24 2.194,0.689 3.228,1.042c4.105,1.415 9.416,3.228 14.068,2.707c4.799,-0.499 8.253,-3.437 10.778,-5.574c0.607,-0.509 1.393,-1.176 1.872,-1.491c0.87,0.315 0.962,0.693 1.176,3.14c0.196,2.26 0.473,5.37 2.362,9.006c1.437,2.761 3.581,5.705 5.646,8.542c1.701,2.336 4.278,5.871 4.535,6.404c-0.445,1.184 -4.907,3.282 -12.229,3.282C30.177,82.591 23.69,80.904 18.941,77.945z"

    /** 尾巴花纹路径 */
    const val DUCK_TAIL = "M149.159,155.398l-20.63,11.169l13.408,9.293c0,0 -49.854,15.813 -72.198,-6.885c-11.006,-11.16 -13.06,-28.533 4.124,-38.84c17.184,-10.312 84.609,3.943 84.609,3.943L134.295,147.8L149.159,155.398z"
}

/**
 * 绘制鸭子 Logo 的 Composable。
 * 在闪屏页面和其他品牌展示场景中使用。
 */
@Composable
fun LogoPainter(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val bodyPath = remember { parseSvgPath(LogoPaths.DUCK_BODY) }
    val mouthPath = remember { parseSvgPath(LogoPaths.DUCK_MOUTH) }
    val tailPath = remember { parseSvgPath(LogoPaths.DUCK_TAIL) }

    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / LogoPaths.VIEWPORT
        val matrix = Matrix().apply { scale(scale, scale) }

        // 鸭身（暖黄）
        val body = Path().apply { addPath(bodyPath); transform(matrix) }
        drawPath(body, color = Color(0xFFFFD54F), style = Fill)

        // 嘴巴（橙色）
        val mouth = Path().apply { addPath(mouthPath); transform(matrix) }
        drawPath(mouth, color = Color(0xFFFF8F00), style = Fill)

        // 尾巴花纹（浅金）
        val tail = Path().apply { addPath(tailPath); transform(matrix) }
        drawPath(tail, color = Color(0xFFFFCA28), style = Fill)

        // 眼睛（深棕圆）
        val eyeX = 65.8f * scale
        val eyeY = 49.4f * scale
        val eyeR = 8.9f * scale
        drawCircle(color = Color(0xFF3E2723), radius = eyeR, center = Offset(eyeX, eyeY))

        // 眼睛高光
        val hlX = 63f * scale
        val hlY = 46.5f * scale
        val hlR = 3.5f * scale
        drawCircle(color = Color(0xD9FFFFFF), radius = hlR, center = Offset(hlX, hlY))

        // 腮红
        val blushX = 50f * scale
        val blushY = 72f * scale
        drawOval(
            color = Color(0x59FFAB91),
            topLeft = Offset(blushX - 8f * scale, blushY - 5f * scale),
            size = Size(16f * scale, 10f * scale)
        )
    }
}
