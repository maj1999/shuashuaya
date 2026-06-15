package com.cleanpic.ui.viewer

import kotlin.test.Test
import kotlin.test.assertEquals

class SwipeGestureTest {

    private val dist = 200f      // 距离阈值 px
    private val fling = 800f     // 甩动阈值 px/s

    @Test fun slow_drag_below_distance_threshold_does_not_switch() {
        // 慢拖：位移不足、速度也不足 → 回弹
        assertEquals(SwipeDirection.NONE, decideSwipeDirection(-120f, -100f, dist, fling))
    }

    @Test fun drag_past_distance_threshold_switches_left() {
        // 慢拖但拖够了距离 → 左切
        assertEquals(SwipeDirection.LEFT, decideSwipeDirection(-250f, -50f, dist, fling))
    }

    @Test fun drag_past_distance_threshold_switches_right() {
        assertEquals(SwipeDirection.RIGHT, decideSwipeDirection(260f, 30f, dist, fling))
    }

    @Test fun quick_flick_short_distance_still_switches_left() {
        // 核心回归：快速轻扫，位移远不到阈值(80<200) 但甩动速度够(1500>800) → 仍然左切
        // 这是 1.17.1 之前会被错误回弹、用户抱怨「要滑很长」的场景
        assertEquals(SwipeDirection.LEFT, decideSwipeDirection(-80f, -1500f, dist, fling))
    }

    @Test fun quick_flick_short_distance_still_switches_right() {
        assertEquals(SwipeDirection.RIGHT, decideSwipeDirection(70f, 1400f, dist, fling))
    }

    @Test fun fast_velocity_opposite_to_offset_does_not_switch() {
        // 向左甩(velocity 负)但净位移在右(offset 正)：方向不一致 → 不切，防误判
        assertEquals(SwipeDirection.NONE, decideSwipeDirection(60f, -1500f, dist, fling))
    }

    @Test fun velocity_just_below_fling_threshold_with_short_distance_bounces() {
        // 速度刚好不到甩动阈值且位移不足 → 回弹（阈值边界）
        assertEquals(SwipeDirection.NONE, decideSwipeDirection(-80f, -799f, dist, fling))
    }

    @Test fun velocity_at_fling_threshold_switches() {
        // 速度恰好达到甩动阈值 → 触发
        assertEquals(SwipeDirection.LEFT, decideSwipeDirection(-80f, -800f, dist, fling))
    }
}
