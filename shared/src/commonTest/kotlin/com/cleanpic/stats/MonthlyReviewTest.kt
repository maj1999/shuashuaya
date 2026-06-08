package com.cleanpic.stats

import com.cleanpic.model.DailyStat
import com.cleanpic.model.MediaTypeStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MonthlyReviewTest {

    private fun day(date: String, pBytes: Long = 0, pCount: Int = 0, vBytes: Long = 0, vCount: Int = 0) =
        DailyStat(date, MediaTypeStats(pBytes, pCount, 1), MediaTypeStats(vBytes, vCount, 0))

    @Test fun empty_yields_no_months() {
        assertEquals(emptyList(), MonthlyReview.byMonth(emptyList()))
    }

    @Test fun same_month_days_merge() {
        val daily = listOf(
            day("2026-06-06", pBytes = 100, pCount = 2),
            day("2026-06-07", pBytes = 50, pCount = 1, vBytes = 900, vCount = 1),
        )
        val months = MonthlyReview.byMonth(daily)
        assertEquals(1, months.size)
        val m = months[0]
        assertEquals("2026-06", m.yearMonth)
        assertEquals(1050L, m.totalBytes)
        assertEquals(4, m.totalCount)
        assertEquals(2, m.totalRounds)  // 两天各 1 轮（photo.rounds=1）
    }

    @Test fun different_months_separate_and_sorted_desc() {
        val daily = listOf(
            day("2026-05-30", pBytes = 10),
            day("2026-06-01", pBytes = 20),
            day("2026-07-15", pBytes = 30),
        )
        val months = MonthlyReview.byMonth(daily)
        assertEquals(listOf("2026-07", "2026-06", "2026-05"), months.map { it.yearMonth })
        assertEquals(30L, months[0].totalBytes)
    }

    @Test fun for_month_picks_target_or_null() {
        val daily = listOf(day("2026-06-07", pBytes = 100))
        assertEquals(100L, MonthlyReview.forMonth(daily, "2026-06")?.totalBytes)
        assertNull(MonthlyReview.forMonth(daily, "2026-05"))
    }
}
