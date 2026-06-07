package com.cleanpic.stats

import com.cleanpic.model.MediaType
import com.cleanpic.model.StatsSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsAggregatorTest {

    private val day = "2026-06-07"

    @Test fun round_only_increments_rounds_not_amount() {
        val s = StatsAggregator.applyRound(StatsSnapshot(), MediaType.PHOTO, 1000L, day)
        assertEquals(1, s.lifetime.photo.rounds)
        assertEquals(0L, s.lifetime.photo.bytes)
        assertEquals(0, s.lifetime.photo.count)
        assertEquals(1, s.lifetime.totalRounds)
        assertEquals(1, s.daily.single { it.date == day }.photo.rounds)
    }

    @Test fun deletion_accumulates_amount_by_type() {
        val s = StatsAggregator.applyDeletion(StatsSnapshot(), MediaType.VIDEO, 1000L, day, bytes = 500L, count = 3)
        assertEquals(500L, s.lifetime.video.bytes)
        assertEquals(3, s.lifetime.video.count)
        assertEquals(0L, s.lifetime.photo.bytes)
        assertEquals(500L, s.lifetime.totalBytes)
    }

    @Test fun first_and_last_timestamps_tracked() {
        var s = StatsAggregator.applyRound(StatsSnapshot(), MediaType.PHOTO, 1000L, day)
        s = StatsAggregator.applyDeletion(s, MediaType.PHOTO, 2000L, day, 10L, 1)
        assertEquals(1000L, s.lifetime.firstCleanupAt)
        assertEquals(2000L, s.lifetime.lastCleanupAt)
    }

    @Test fun same_day_events_merge_into_one_daily_row() {
        var s = StatsAggregator.applyRound(StatsSnapshot(), MediaType.PHOTO, 1000L, day)
        s = StatsAggregator.applyDeletion(s, MediaType.PHOTO, 1100L, day, 10L, 2)
        assertEquals(1, s.daily.size)
        assertEquals(1, s.daily[0].photo.rounds)
        assertEquals(10L, s.daily[0].photo.bytes)
        assertEquals(2, s.daily[0].photo.count)
    }

    @Test fun different_days_create_separate_rows() {
        var s = StatsAggregator.applyRound(StatsSnapshot(), MediaType.PHOTO, 1000L, "2026-06-06")
        s = StatsAggregator.applyRound(s, MediaType.PHOTO, 2000L, "2026-06-07")
        assertEquals(2, s.daily.size)
    }

    @Test fun photo_and_video_totals_are_sums() {
        var s = StatsAggregator.applyDeletion(StatsSnapshot(), MediaType.PHOTO, 1L, day, 100L, 5)
        s = StatsAggregator.applyDeletion(s, MediaType.VIDEO, 2L, day, 900L, 1)
        assertEquals(1000L, s.lifetime.totalBytes)
        assertEquals(6, s.lifetime.totalCount)
    }
}
