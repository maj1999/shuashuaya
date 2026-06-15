package com.cleanpic.ui.viewer

/** 松手时的滑动判定结果：朝左 / 朝右 / 不切换（回弹）。方向语义由各调用方自行映射。 */
enum class SwipeDirection { LEFT, RIGHT, NONE }

/**
 * 根据松手瞬间的累计位移与水平甩动速度，判定本次滑动是否触发切换、朝哪个方向。
 *
 * 触发规则（任一满足即触发，且方向必须一致）：
 *  1. 位移过阈值：|offset| >= distanceThreshold
 *  2. 甩动过阈值：|velocity| >= flingThreshold 且位移方向与甩动方向同向
 *
 * 引入规则 2 的原因：真实用户翻看是「甩」而非「慢拖」，手指移动快但行程短，
 * 仅凭位移阈值会把这类快速轻扫判为「未达阈值」而回弹，表现为「必须滑很长才翻页」。
 * 位移方向同向的约束防止「向左甩但净位移在右」之类的误判。
 *
 * @param offsetPx 松手时累计水平位移（左为负、右为正），单位 px
 * @param velocityPxPerSec 松手瞬间水平速度（左为负、右为正），单位 px/s
 * @param distanceThresholdPx 触发切换的位移阈值（正数），单位 px
 * @param flingThresholdPxPerSec 触发切换的甩动速度阈值（正数），单位 px/s
 */
fun decideSwipeDirection(
    offsetPx: Float,
    velocityPxPerSec: Float,
    distanceThresholdPx: Float,
    flingThresholdPxPerSec: Float
): SwipeDirection {
    val left = offsetPx <= -distanceThresholdPx ||
        (velocityPxPerSec <= -flingThresholdPxPerSec && offsetPx < 0f)
    val right = offsetPx >= distanceThresholdPx ||
        (velocityPxPerSec >= flingThresholdPxPerSec && offsetPx > 0f)
    return when {
        left -> SwipeDirection.LEFT
        right -> SwipeDirection.RIGHT
        else -> SwipeDirection.NONE
    }
}
