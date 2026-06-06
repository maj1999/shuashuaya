package com.cleanpic.media

import com.cleanpic.model.MediaItem
import kotlin.random.Random

/**
 * 随机选取（纯函数）—— Shuffle Bag 洗牌袋 + 保留沉底 + 删除自愈。
 *
 * 设计：docs/architecture/cleanpic/random-picker.md
 *
 * 不变式：
 * 1. 单轮结果内绝不重复；
 * 2. 同一 cycle 内不重复（洗牌袋，抽空才进位）；
 * 3. 保留过的项仅在非保留项耗尽时才出现（沉底）。
 */
object RandomPicker {
    private const val DAY_MILLIS = 86_400_000L
    private const val CAP_FLOOR = 2000

    fun pick(
        live: List<MediaItem>,
        count: Int,
        state: PickState,
        now: Long = 0L,
        freshDays: Int = 1,
        random: Random = Random.Default,
    ): PickResult {
        if (live.isEmpty() || count <= 0) return PickResult(emptyList(), state)

        val byId = live.associateBy { it.id }
        var records = reconcile(state.records, byId.keys)
        var cycle = state.cycle
        val liveIds = live.map { it.id }

        fun kept(id: String) = records[id]?.kept == true
        fun drawnThisCycle(id: String) = records[id]?.lastDrawnCycle == cycle
        fun recentlySeen(id: String): Boolean {
            val r = records[id] ?: return false
            return freshDays > 0 && now - r.lastSeenMillis < freshDays * DAY_MILLIS
        }

        val resultIds = LinkedHashSet<String>()
        fun fill(pool: List<String>) {
            for (id in pool) {
                if (resultIds.size >= count) break
                resultIds.add(id)  // LinkedHashSet 自动去重，保证单轮不重复
            }
        }

        // L1 主池·新鲜
        fill(liveIds.filter { !kept(it) && !drawnThisCycle(it) && !recentlySeen(it) }.shuffled(random))
        // L2 主池·含近期
        if (resultIds.size < count) {
            fill(liveIds.filter { !kept(it) && !drawnThisCycle(it) }.shuffled(random))
        }
        // 进位：本循环非保留项已抽空，但仍有非保留项 → cycle++ 开新循环
        if (resultIds.size < count) {
            val nonKept = liveIds.filter { !kept(it) }
            if (nonKept.size > resultIds.size) {
                cycle += 1
                fill(nonKept.filter { it !in resultIds }.shuffled(random))
            }
        }
        // 兜底：整库非保留项 < count → 动用保留集（最久优先）
        if (resultIds.size < count) {
            val anyNonKept = liveIds.any { !kept(it) }
            if (!anyNonKept) {
                // 大循环重置：整库都保留过，清零 kept、进位，重新开始
                cycle += 1
                records = records.mapValues { (_, r) -> if (r.kept) r.copy(kept = false) else r }
            }
            val keptOldestFirst = liveIds
                .filter { records[it]?.kept == true || !anyNonKept }
                .filter { it !in resultIds }
                .sortedBy { records[it]?.lastSeenMillis ?: 0L }
            fill(keptOldestFirst)
        }

        val items = resultIds.mapNotNull { byId[it] }

        // 更新被抽中项的记录（kept 保持不变）
        val newRecords = records.toMutableMap()
        for (id in resultIds) {
            val prevKept = newRecords[id]?.kept ?: false
            newRecords[id] = SeenRecord(lastDrawnCycle = cycle, lastSeenMillis = now, kept = prevKept)
        }
        return PickResult(items, PickState(cycle, newRecords))
    }

    /** 懒清理 + 封顶：超阈值才行动。先剔除已删除 id，仍超再丢最老。 */
    private fun reconcile(records: Map<String, SeenRecord>, liveIds: Set<String>): Map<String, SeenRecord> {
        val threshold = maxOf((liveIds.size * 1.2).toInt(), CAP_FLOOR)
        if (records.size <= threshold) return records
        var pruned: Map<String, SeenRecord> = records.filterKeys { it in liveIds }
        if (pruned.size > threshold) {
            pruned = pruned.entries
                .sortedByDescending { it.value.lastSeenMillis }
                .take(threshold)
                .associate { it.key to it.value }
        }
        return pruned
    }
}
