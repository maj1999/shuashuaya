package com.cleanpic.stats

import com.cleanpic.model.MediaType
import com.cleanpic.model.StatsSnapshot

/** 清理统计持久化。一轮只属于一种 MediaType，故两个 record 都带 type。 */
interface StatsStore {
    fun load(): StatsSnapshot
    fun recordRoundReached(type: MediaType, nowMillis: Long, today: String)
    fun recordDeletion(type: MediaType, nowMillis: Long, today: String, bytes: Long, count: Int)
    fun reset()
}

/** 内存实现（测试 / 未接入平台时的默认）。 */
open class InMemoryStatsStore : StatsStore {
    private var snapshot = StatsSnapshot()
    override fun load(): StatsSnapshot = snapshot
    override fun recordRoundReached(type: MediaType, nowMillis: Long, today: String) {
        snapshot = StatsAggregator.applyRound(snapshot, type, nowMillis, today)
    }
    override fun recordDeletion(type: MediaType, nowMillis: Long, today: String, bytes: Long, count: Int) {
        snapshot = StatsAggregator.applyDeletion(snapshot, type, nowMillis, today, bytes, count)
    }
    override fun reset() { snapshot = StatsSnapshot() }
}
