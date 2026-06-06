package com.cleanpic.media

import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RandomPickerTest {
    private fun items(n: Int, prefix: String = "id") = (1..n).map {
        MediaItem("${prefix}_$it", MediaType.PHOTO, "img_$it.jpg", 1000L, 0L, 100, 100)
    }

    private fun rec(cycle: Int = 0, seen: Long = 0L, kept: Boolean = false) =
        SeenRecord(lastDrawnCycle = cycle, lastSeenMillis = seen, kept = kept)

    private val seeded = Random(42)

    // U-RP-01 一轮内不重复
    @Test fun u01_single_round_no_duplicate() {
        val r = RandomPicker.pick(items(100), 10, PickState(), random = seeded)
        assertEquals(10, r.items.size)
        assertEquals(10, r.items.map { it.id }.toSet().size)
    }

    // U-RP-02 洗牌袋：整库抽满前不重复
    @Test fun u02_no_repeat_until_full_cycle() {
        val all = items(100)
        var state = PickState()
        val seen = mutableListOf<String>()
        repeat(10) {
            val r = RandomPicker.pick(all, 10, state, now = 0L, random = Random(it.toLong()))
            seen += r.items.map { m -> m.id }
            state = r.state
        }
        // 100 次抽取覆盖全库且互不重复（同一 cycle 内不重复）
        assertEquals(100, seen.size)
        assertEquals(100, seen.toSet().size)
    }

    // U-RP-03 保留过的沉底：非保留充足时结果不含 kept
    @Test fun u03_kept_excluded_when_nonkept_sufficient() {
        val all = items(20)
        val keptIds = all.take(5).map { it.id }.toSet()
        val state = PickState(records = all.associate { it.id to rec(kept = it.id in keptIds) })
        val r = RandomPicker.pick(all, 10, state, random = seeded)
        assertEquals(10, r.items.size)
        assertTrue(r.items.none { it.id in keptIds })
    }

    // U-RP-04 非保留不足时按最久优先动用保留集
    @Test fun u04_kept_fallback_oldest_first() {
        val all = items(12)
        // 4 非保留(无记录) + 8 保留，lastSeenMillis 递增
        val records = HashMap<String, SeenRecord>()
        all.drop(4).forEachIndexed { i, m -> records[m.id] = rec(seen = (i + 1) * 1000L, kept = true) }
        val r = RandomPicker.pick(all, 10, PickState(records = records), random = seeded)
        assertEquals(10, r.items.size)
        val nonKept = all.take(4).map { it.id }.toSet()
        assertTrue(r.items.map { it.id }.toSet().containsAll(nonKept))  // 4 非保留全在
        // 补位的 6 个保留应是 lastSeenMillis 最小的 6 个
        val pickedKept = r.items.map { it.id }.filter { it !in nonKept }.toSet()
        val expectedKept = all.drop(4).sortedBy { records[it.id]!!.lastSeenMillis }.take(6).map { it.id }.toSet()
        assertEquals(expectedKept, pickedKept)
    }

    // U-RP-05 新内容同等参与：本循环已抽过的被延后，新项可被抽中
    @Test fun u05_new_items_participate() {
        val old = items(10, "old")
        val fresh = items(10, "new")
        val all = old + fresh
        // old 全部本循环已抽过（drawnThisCycle, 非保留）
        val state = PickState(cycle = 0, records = old.associate { it.id to rec(cycle = 0) })
        val r = RandomPicker.pick(all, 10, state, random = seeded)
        // 新项应被全部选中（旧项本循环已抽过被延后）
        assertEquals(fresh.map { it.id }.toSet(), r.items.map { it.id }.toSet())
    }

    // U-RP-06 删除自愈：幽灵 id 不被抽中、不报错
    @Test fun u06_ghost_ids_ignored() {
        val all = items(5)
        val ghosts = (1..50).associate { "ghost_$it" to rec(cycle = 0, seen = it.toLong()) }
        val r = RandomPicker.pick(all, 5, PickState(records = ghosts), random = seeded)
        assertEquals(5, r.items.size)
        // 幽灵 id 不在当前相册 → 永不被抽中（残留于记忆中无害，仅占存储）
        assertTrue(r.items.all { it.id.startsWith("id_") })
    }

    // U-RP-07 整库都保留 → 大循环重置
    @Test fun u07_all_kept_triggers_big_cycle_reset() {
        val all = items(10)
        val state = PickState(cycle = 3, records = all.associate { it.id to rec(cycle = 3, kept = true) })
        val r = RandomPicker.pick(all, 5, state, random = seeded)
        assertEquals(5, r.items.size)
        assertEquals(4, r.state.cycle)  // cycle++
        assertTrue(r.state.records.values.none { it.kept })  // kept 全部清零
    }

    // U-RP-08 count ≥ 库大小：单轮仍不重复
    @Test fun u08_count_exceeds_size() {
        val r = RandomPicker.pick(items(8), 10, PickState(), random = seeded)
        assertEquals(8, r.items.size)
        assertEquals(8, r.items.map { it.id }.toSet().size)
    }

    // U-RP-09 天数新鲜度：有更优项时近 N 天看过的被延后
    @Test fun u09_day_freshness_defers_recent() {
        val now = 10 * 86_400_000L
        val all = items(4)
        // 前 2 个：今天看过（recent，非保留，非本循环抽过：lastDrawnCycle 与 state.cycle 不同）
        val records = mapOf(
            all[0].id to rec(cycle = 3, seen = now),
            all[1].id to rec(cycle = 3, seen = now),
        )
        val r = RandomPicker.pick(all, 2, PickState(cycle = 5, records = records), now = now, freshDays = 1, random = seeded)
        // 应选 2 个无记录的新鲜项，而非今天看过的
        assertEquals(setOf(all[2].id, all[3].id), r.items.map { it.id }.toSet())
    }

    // U-RP-10 空相册
    @Test fun u10_empty_live() {
        val state = PickState(cycle = 2)
        val r = RandomPicker.pick(emptyList(), 10, state, random = seeded)
        assertTrue(r.items.isEmpty())
        assertEquals(state, r.state)
    }

    // U-RP-11 存储封顶懒清理
    @Test fun u11_cap_prunes_ghosts() {
        val all = items(10)
        val ghosts = (1..2100).associate { "ghost_$it" to rec(seen = it.toLong()) }
        val r = RandomPicker.pick(all, 10, PickState(records = ghosts), random = seeded)
        assertEquals(10, r.items.size)
        // 幽灵被剔除，只剩 live 内的记录
        assertTrue(r.state.records.keys.all { it in all.map { m -> m.id } })
        assertEquals(10, r.state.records.size)
    }

    // 编解码往返
    @Test fun codec_round_trip() {
        val state = PickState(
            cycle = 7,
            records = mapOf(
                "photo_1" to SeenRecord(2, 1700000000000L, true),
                "photo_2" to SeenRecord(0, 0L, false),
            )
        )
        assertEquals(state, PickStateCodec.decode(PickStateCodec.encode(state)))
    }

    @Test fun codec_empty_and_null() {
        assertEquals(PickState(), PickStateCodec.decode(null))
        assertEquals(PickState(), PickStateCodec.decode(""))
        assertEquals(PickState(cycle = 5, records = emptyMap()),
            PickStateCodec.decode(PickStateCodec.encode(PickState(cycle = 5))))
    }

    @Test fun pick_zero_count_returns_empty() {
        val r = RandomPicker.pick(items(10), 0, PickState(), random = seeded)
        assertTrue(r.items.isEmpty())
        assertFalse(r.items.isNotEmpty())
    }
}
