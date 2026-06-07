package com.cleanpic.icons

import androidx.compose.ui.graphics.Path
import kotlin.math.*

/**
 * SVG path data 解析器——将 SVG path data 字符串解析为 Compose Path 对象。
 *
 * 支持命令：M/m, L/l, H/h, V/v, C/c, S/s, A/a, Z/z
 */
fun parseSvgPath(data: String): Path {
    val path = Path()
    if (data.isBlank()) return path

    val tokens = tokenizeSvgPath(data)
    var i = 0
    var currentX = 0f
    var currentY = 0f
    var subpathStartX = 0f
    var subpathStartY = 0f
    var lastCommand = ' '
    // 上一个三次贝塞尔控制点（用于 S/s 命令的反射）
    var lastCubicCtrlX = 0f
    var lastCubicCtrlY = 0f

    fun nextFloat(): Float {
        if (i >= tokens.size) throw IllegalArgumentException("SVG path: 参数不足")
        return tokens[i++].toFloat()
    }

    fun hasMoreNumbers(): Boolean {
        if (i >= tokens.size) return false
        val t = tokens[i]
        return t[0].isDigit() || t[0] == '-' || t[0] == '.' || t[0] == '+'
    }

    while (i < tokens.size) {
        val token = tokens[i]
        val cmd: Char
        if (token.length == 1 && token[0].isLetter()) {
            cmd = token[0]
            i++
        } else {
            // Implicit repetition of previous command
            cmd = if (lastCommand == 'M') 'L'
                  else if (lastCommand == 'm') 'l'
                  else lastCommand
        }

        when (cmd) {
            'M' -> {
                do {
                    currentX = nextFloat(); currentY = nextFloat()
                    path.moveTo(currentX, currentY)
                    subpathStartX = currentX; subpathStartY = currentY
                } while (false) // only first pair is moveTo, rest are lineTo
                while (hasMoreNumbers()) {
                    currentX = nextFloat(); currentY = nextFloat()
                    path.lineTo(currentX, currentY)
                }
            }
            'm' -> {
                currentX += nextFloat(); currentY += nextFloat()
                path.moveTo(currentX, currentY)
                subpathStartX = currentX; subpathStartY = currentY
                while (hasMoreNumbers()) {
                    currentX += nextFloat(); currentY += nextFloat()
                    path.lineTo(currentX, currentY)
                }
            }
            'L' -> {
                do {
                    currentX = nextFloat(); currentY = nextFloat()
                    path.lineTo(currentX, currentY)
                } while (hasMoreNumbers())
            }
            'l' -> {
                do {
                    currentX += nextFloat(); currentY += nextFloat()
                    path.lineTo(currentX, currentY)
                } while (hasMoreNumbers())
            }
            'H' -> {
                do {
                    currentX = nextFloat()
                    path.lineTo(currentX, currentY)
                } while (hasMoreNumbers())
            }
            'h' -> {
                do {
                    currentX += nextFloat()
                    path.lineTo(currentX, currentY)
                } while (hasMoreNumbers())
            }
            'V' -> {
                do {
                    currentY = nextFloat()
                    path.lineTo(currentX, currentY)
                } while (hasMoreNumbers())
            }
            'v' -> {
                do {
                    currentY += nextFloat()
                    path.lineTo(currentX, currentY)
                } while (hasMoreNumbers())
            }
            'C' -> {
                do {
                    val x1 = nextFloat(); val y1 = nextFloat()
                    val x2 = nextFloat(); val y2 = nextFloat()
                    currentX = nextFloat(); currentY = nextFloat()
                    path.cubicTo(x1, y1, x2, y2, currentX, currentY)
                    lastCubicCtrlX = x2; lastCubicCtrlY = y2
                } while (hasMoreNumbers())
            }
            'c' -> {
                do {
                    val dx1 = nextFloat(); val dy1 = nextFloat()
                    val dx2 = nextFloat(); val dy2 = nextFloat()
                    val dx = nextFloat(); val dy = nextFloat()
                    val ax2 = currentX + dx2; val ay2 = currentY + dy2
                    path.cubicTo(
                        currentX + dx1, currentY + dy1,
                        ax2, ay2,
                        currentX + dx, currentY + dy
                    )
                    lastCubicCtrlX = ax2; lastCubicCtrlY = ay2
                    currentX += dx; currentY += dy
                } while (hasMoreNumbers())
            }
            'S' -> {
                do {
                    // 第一控制点是上一个三次贝塞尔控制点关于当前点的反射
                    val x1 = 2 * currentX - lastCubicCtrlX
                    val y1 = 2 * currentY - lastCubicCtrlY
                    val x2 = nextFloat(); val y2 = nextFloat()
                    currentX = nextFloat(); currentY = nextFloat()
                    path.cubicTo(x1, y1, x2, y2, currentX, currentY)
                    lastCubicCtrlX = x2; lastCubicCtrlY = y2
                } while (hasMoreNumbers())
            }
            's' -> {
                do {
                    val x1 = 2 * currentX - lastCubicCtrlX
                    val y1 = 2 * currentY - lastCubicCtrlY
                    val dx2 = nextFloat(); val dy2 = nextFloat()
                    val dx = nextFloat(); val dy = nextFloat()
                    val ax2 = currentX + dx2; val ay2 = currentY + dy2
                    path.cubicTo(x1, y1, ax2, ay2, currentX + dx, currentY + dy)
                    lastCubicCtrlX = ax2; lastCubicCtrlY = ay2
                    currentX += dx; currentY += dy
                } while (hasMoreNumbers())
            }
            'A', 'a' -> {
                val relative = cmd == 'a'
                do {
                    val rx = nextFloat(); val ry = nextFloat()
                    val xRotation = nextFloat()
                    val largeArc = nextFloat().toInt() != 0
                    val sweep = nextFloat().toInt() != 0
                    val ex: Float; val ey: Float
                    if (relative) {
                        val dx = nextFloat(); val dy = nextFloat()
                        ex = currentX + dx; ey = currentY + dy
                    } else {
                        ex = nextFloat(); ey = nextFloat()
                    }
                    drawArc(path, currentX, currentY, rx, ry, xRotation, largeArc, sweep, ex, ey)
                    currentX = ex; currentY = ey
                } while (hasMoreNumbers())
            }
            'Z', 'z' -> {
                path.close()
                currentX = subpathStartX; currentY = subpathStartY
            }
            else -> throw IllegalArgumentException("SVG path: 不支持的命令 '$cmd'")
        }
        // 非三次贝塞尔命令后重置控制点（使 S/s 退化为二次曲线）
        if (cmd !in listOf('C', 'c', 'S', 's')) {
            lastCubicCtrlX = currentX; lastCubicCtrlY = currentY
        }
        lastCommand = cmd
    }
    return path
}

// ------ Tokenizer ------

/**
 * 将 SVG path data 拆分为 token 列表（命令字母 + 数值字符串）。
 *
 * 处理：
 * - 逗号/空格分隔
 * - 负号作为分隔符（如 `10-5` → `10`, `-5`）
 * - 连续小数点（如 `1.5.5` → `1.5`, `.5`）
 * - arc flag 粘连（如 `01-2` 中的 flag 值）
 */
internal fun tokenizeSvgPath(data: String): List<String> {
    val tokens = mutableListOf<String>()
    var i = 0
    val len = data.length

    // Track which command we're in and how many numbers consumed for that command,
    // so we can correctly split arc flags.
    var currentCommand = ' '
    var paramIndex = 0  // number of numeric params consumed since last command letter

    fun isCommandLetter(c: Char) = c.isLetter() && c != 'e' && c != 'E'

    fun arcFlagPosition(): Boolean {
        // For A/a commands, params 3 and 4 (0-indexed) are flags (0 or 1).
        // Pattern per arc group: rx ry xrot flag flag x y  (7 params)
        if (currentCommand != 'A' && currentCommand != 'a') return false
        val posInGroup = paramIndex % 7
        return posInGroup == 3 || posInGroup == 4
    }

    while (i < len) {
        val c = data[i]
        when {
            c == ' ' || c == ',' || c == '\n' || c == '\r' || c == '\t' -> { i++; continue }
            isCommandLetter(c) -> {
                tokens.add(c.toString())
                currentCommand = c
                paramIndex = 0
                i++
            }
            else -> {
                // Parse a number
                if (arcFlagPosition()) {
                    // Arc flag: consume exactly one character (0 or 1)
                    tokens.add(c.toString())
                    paramIndex++
                    i++
                } else {
                    val sb = StringBuilder()
                    if (c == '-' || c == '+') { sb.append(c); i++ }
                    var hasDot = false
                    while (i < len) {
                        val ch = data[i]
                        when {
                            ch.isDigit() -> { sb.append(ch); i++ }
                            ch == '.' && !hasDot -> { sb.append(ch); hasDot = true; i++ }
                            ch == '.' && hasDot -> break  // second dot starts new number
                            ch == 'e' || ch == 'E' -> {
                                // Scientific notation
                                sb.append(ch); i++
                                if (i < len && (data[i] == '+' || data[i] == '-')) {
                                    sb.append(data[i]); i++
                                }
                            }
                            else -> break
                        }
                    }
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        paramIndex++
                    }
                }
            }
        }
    }
    return tokens
}

// ------ Arc helper ------

/**
 * 将 SVG arc 命令转化为 cubicTo 调用绘制到 Path 上。
 * 实现基于 SVG 规范的 endpoint-to-center 参数化转换。
 */
private fun drawArc(
    path: Path,
    x1: Float, y1: Float,
    rxIn: Float, ryIn: Float,
    xRotationDeg: Float,
    largeArc: Boolean,
    sweep: Boolean,
    x2: Float, y2: Float
) {
    if (x1 == x2 && y1 == y2) return
    var rx = abs(rxIn)
    var ry = abs(ryIn)
    if (rx == 0f || ry == 0f) {
        path.lineTo(x2, y2)
        return
    }

    val phi = xRotationDeg.toDouble() * PI / 180.0
    val cosPhi = cos(phi)
    val sinPhi = sin(phi)

    // Step 1: compute (x1', y1')
    val dx2 = (x1 - x2) / 2.0
    val dy2 = (y1 - y2) / 2.0
    val x1p = cosPhi * dx2 + sinPhi * dy2
    val y1p = -sinPhi * dx2 + cosPhi * dy2

    // Step 2: compute (cx', cy')
    var rxSq = (rx * rx).toDouble()
    var rySq = (ry * ry).toDouble()
    val x1pSq = x1p * x1p
    val y1pSq = y1p * y1p

    // Ensure radii are large enough
    val lambda = x1pSq / rxSq + y1pSq / rySq
    if (lambda > 1.0) {
        val lambdaSqrt = sqrt(lambda)
        rx = (lambdaSqrt * rx).toFloat()
        ry = (lambdaSqrt * ry).toFloat()
        rxSq = (rx * rx).toDouble()
        rySq = (ry * ry).toDouble()
    }

    var sq = ((rxSq * rySq - rxSq * y1pSq - rySq * x1pSq) /
              (rxSq * y1pSq + rySq * x1pSq)).coerceAtLeast(0.0)
    sq = sqrt(sq)
    if (largeArc == sweep) sq = -sq

    val cxp = sq * rx * y1p / ry
    val cyp = -sq * ry * x1p / rx

    // Step 3: compute (cx, cy) from (cx', cy')
    val cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2.0
    val cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2.0

    // Step 4: compute theta1 and dTheta
    fun angle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
        val dot = ux * vx + uy * vy
        val len = sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy)
        var ang = acos((dot / len).coerceIn(-1.0, 1.0))
        if (ux * vy - uy * vx < 0) ang = -ang
        return ang
    }

    val theta1 = angle(1.0, 0.0, (x1p - cxp) / rx, (y1p - cyp) / ry)
    var dTheta = angle(
        (x1p - cxp) / rx, (y1p - cyp) / ry,
        (-x1p - cxp) / rx, (-y1p - cyp) / ry
    )

    if (!sweep && dTheta > 0) dTheta -= 2 * PI
    if (sweep && dTheta < 0) dTheta += 2 * PI

    // Approximate arc with cubic bezier segments (max 90 degrees each)
    val segments = ceil(abs(dTheta) / (PI / 2)).toInt().coerceAtLeast(1)
    val segmentAngle = dTheta / segments

    for (s in 0 until segments) {
        val t1 = theta1 + s * segmentAngle
        val t2 = t1 + segmentAngle
        val alpha = sin(segmentAngle) * (sqrt(4.0 + 3.0 * tan(segmentAngle / 2).pow(2)) - 1) / 3.0

        val cosT1 = cos(t1); val sinT1 = sin(t1)
        val cosT2 = cos(t2); val sinT2 = sin(t2)

        val ep1x = rx * cosT1; val ep1y = ry * sinT1
        val ep2x = rx * cosT2; val ep2y = ry * sinT2

        val q1x = ep1x + alpha * (-rx * sinT1)
        val q1y = ep1y + alpha * (ry * cosT1)
        val q2x = ep2x - alpha * (-rx * sinT2)
        val q2y = ep2y - alpha * (ry * cosT2)

        fun transformX(px: Double, py: Double) = (cosPhi * px - sinPhi * py + cx).toFloat()
        fun transformY(px: Double, py: Double) = (sinPhi * px + cosPhi * py + cy).toFloat()

        path.cubicTo(
            transformX(q1x, q1y), transformY(q1x, q1y),
            transformX(q2x, q2y), transformY(q2x, q2y),
            transformX(ep2x, ep2y), transformY(ep2x, ep2y)
        )
    }
}
