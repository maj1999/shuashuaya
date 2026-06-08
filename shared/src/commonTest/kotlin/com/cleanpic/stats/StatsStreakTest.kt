package com.cleanpic.stats

import com.cleanpic.model.DailyStat
import com.cleanpic.model.MediaTypeStats
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsStreakTest {

    private fun day(date: String) = DailyStat(date, MediaTypeStats(rounds = 1), MediaTypeStats())

    @Test fun empty_is_zero() {
        assertEquals(0, StatsStreak.current(emptyList(), "2026-06-08"))
    }

    @Test fun today_only_is_one() {
        assertEquals(1, StatsStreak.current(listOf(day("2026-06-08")), "2026-06-08"))
    }

    @Test fun three_consecutive_days_up_to_today() {
        val daily = listOf(day("2026-06-06"), day("2026-06-07"), day("2026-06-08"))
        assertEquals(3, StatsStreak.current(daily, "2026-06-08"))
    }

    @Test fun gap_breaks_the_streak() {
        // 06-08 今天, 06-07 有, 06-05 有但 06-06 缺 → streak = 2
        val daily = listOf(day("2026-06-05"), day("2026-06-07"), day("2026-06-08"))
        assertEquals(2, StatsStreak.current(daily, "2026-06-08"))
    }

    @Test fun yesterday_counts_when_today_idle() {
        // 今天还没清理，但昨天和前天连续 → 仍算 2，不清零
        val daily = listOf(day("2026-06-06"), day("2026-06-07"))
        assertEquals(2, StatsStreak.current(daily, "2026-06-08"))
    }

    @Test fun stale_streak_two_days_ago_is_zero() {
        // 最近活动在前天，今天与昨天都没有 → 0
        val daily = listOf(day("2026-06-05"), day("2026-06-06"))
        assertEquals(0, StatsStreak.current(daily, "2026-06-08"))
    }

    @Test fun spans_month_boundary() {
        val daily = listOf(day("2026-05-31"), day("2026-06-01"), day("2026-06-02"))
        assertEquals(3, StatsStreak.current(daily, "2026-06-02"))
    }
}
