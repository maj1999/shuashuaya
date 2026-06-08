package com.cleanpic.stats

import com.cleanpic.model.LifetimeStats
import com.cleanpic.model.MediaTypeStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MilestonesTest {

    private fun lifetime(bytes: Long = 0, count: Int = 0, rounds: Int = 0) =
        LifetimeStats(photo = MediaTypeStats(bytes = bytes, count = count, rounds = rounds))

    @Test fun nothing_achieved_when_empty() {
        assertEquals(0, Milestones.achievedCount(lifetime()))
        assertTrue(Milestones.evaluate(lifetime()).none { it.achieved })
    }

    @Test fun one_gb_badge_unlocks_at_threshold() {
        val badges = Milestones.evaluate(lifetime(bytes = 1_000_000_000L))
        assertTrue(badges.first { it.id == "bytes_1g" }.achieved)
        assertFalse(badges.first { it.id == "bytes_10g" }.achieved)
    }

    @Test fun count_and_rounds_independent() {
        val badges = Milestones.evaluate(lifetime(count = 100, rounds = 10))
        assertTrue(badges.first { it.id == "count_100" }.achieved)
        assertTrue(badges.first { it.id == "rounds_10" }.achieved)
        assertFalse(badges.first { it.id == "count_1000" }.achieved)
        assertFalse(badges.first { it.id == "rounds_50" }.achieved)
    }

    @Test fun evaluate_order_is_stable() {
        val ids = Milestones.evaluate(lifetime()).map { it.id }
        assertEquals(
            listOf("bytes_1g", "bytes_10g", "bytes_100g", "count_100", "count_1000", "rounds_10", "rounds_50", "rounds_100"),
            ids
        )
    }

    @Test fun below_threshold_by_one_not_achieved() {
        val badges = Milestones.evaluate(lifetime(bytes = 999_999_999L))
        assertFalse(badges.first { it.id == "bytes_1g" }.achieved)
    }
}
