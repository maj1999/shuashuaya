package com.cleanpic.stats

import com.cleanpic.model.LifetimeStats
import com.cleanpic.model.MediaTypeStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CleanupQuotesTest {

    private fun stats(rounds: Int) = LifetimeStats(photo = MediaTypeStats(rounds = rounds))

    @Test fun first_time_uses_first_pool() {
        val q = CleanupQuotes.pick(stats(rounds = 1), isStreak = false, seed = 0)
        assertTrue(q in CleanupQuotes.FIRST)
    }

    @Test fun streak_uses_streak_pool() {
        val q = CleanupQuotes.pick(stats(rounds = 10), isStreak = true, seed = 0)
        assertTrue(q in CleanupQuotes.STREAK)
    }

    @Test fun normal_uses_daily_pool() {
        val q = CleanupQuotes.pick(stats(rounds = 10), isStreak = false, seed = 0)
        assertTrue(q in CleanupQuotes.DAILY)
    }

    @Test fun same_seed_is_stable() {
        val a = CleanupQuotes.pick(stats(rounds = 10), isStreak = false, seed = 42)
        val b = CleanupQuotes.pick(stats(rounds = 10), isStreak = false, seed = 42)
        assertEquals(a, b)
    }

    @Test fun seed_selects_within_bounds() {
        repeat(20) { i ->
            assertTrue(CleanupQuotes.pick(stats(rounds = 5), isStreak = false, seed = i) in CleanupQuotes.DAILY)
        }
    }
}
