package com.cleanpic.stats

import com.cleanpic.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsCodecTest {

    @Test fun roundtrip_full_snapshot() {
        val snap = StatsSnapshot(
            lifetime = LifetimeStats(
                photo = MediaTypeStats(bytes = 2_600_000_000L, count = 2890, rounds = 62),
                video = MediaTypeStats(bytes = 10_000_000_000L, count = 350, rounds = 24),
                firstCleanupAt = 1_700_000_000_000L,
                lastCleanupAt = 1_717_000_000_000L,
            ),
            daily = listOf(
                DailyStat("2026-06-07", MediaTypeStats(100L, 5, 1), MediaTypeStats(2000L, 2, 1)),
                DailyStat("2026-06-06", MediaTypeStats(50L, 3, 1), MediaTypeStats(0L, 0, 0)),
            ),
        )
        val decoded = StatsCodec.decode(StatsCodec.encode(snap))
        assertEquals(snap, decoded)
    }

    @Test fun empty_or_null_decodes_to_default() {
        assertEquals(StatsSnapshot(), StatsCodec.decode(null))
        assertEquals(StatsSnapshot(), StatsCodec.decode(""))
    }

    @Test fun garbage_decodes_to_default_without_throwing() {
        assertEquals(StatsSnapshot(), StatsCodec.decode("not-a-valid-blob"))
    }

    @Test fun no_daily_roundtrips() {
        val snap = StatsSnapshot(lifetime = LifetimeStats(photo = MediaTypeStats(1L, 1, 1)))
        assertEquals(snap, StatsCodec.decode(StatsCodec.encode(snap)))
    }
}
