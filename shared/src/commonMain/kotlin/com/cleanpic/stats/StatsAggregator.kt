package com.cleanpic.stats

import com.cleanpic.model.*

/** 把单次清理事件并入快照的纯函数（无副作用、可测）。 */
object StatsAggregator {

    fun applyRound(s: StatsSnapshot, type: MediaType, nowMillis: Long, today: String): StatsSnapshot =
        apply(s, type, nowMillis, today) { it.copy(rounds = it.rounds + 1) }

    fun applyDeletion(s: StatsSnapshot, type: MediaType, nowMillis: Long, today: String, bytes: Long, count: Int): StatsSnapshot =
        apply(s, type, nowMillis, today) { it.copy(bytes = it.bytes + bytes, count = it.count + count) }

    private inline fun apply(
        s: StatsSnapshot, type: MediaType, nowMillis: Long, today: String,
        mutate: (MediaTypeStats) -> MediaTypeStats,
    ): StatsSnapshot {
        val l = s.lifetime
        val newLifetime = when (type) {
            MediaType.PHOTO -> l.copy(photo = mutate(l.photo))
            MediaType.VIDEO -> l.copy(video = mutate(l.video))
        }.copy(
            firstCleanupAt = if (l.firstCleanupAt == 0L) nowMillis else l.firstCleanupAt,
            lastCleanupAt = nowMillis,
        )
        val idx = s.daily.indexOfFirst { it.date == today }
        val newDaily = s.daily.toMutableList()
        val existing = if (idx >= 0) s.daily[idx] else DailyStat(date = today)
        val updated = when (type) {
            MediaType.PHOTO -> existing.copy(photo = mutate(existing.photo))
            MediaType.VIDEO -> existing.copy(video = mutate(existing.video))
        }
        if (idx >= 0) newDaily[idx] = updated else newDaily.add(updated)
        return StatsSnapshot(newLifetime, newDaily)
    }
}
