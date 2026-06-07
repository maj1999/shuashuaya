# 清理统计（阶段一）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把用户真实清理掉的照片/视频（大小·数量·轮次，按类型拆分）持久化累计，并新增「清理成果」统计页 + 首页入口，形成"清理→记录→查看成果"的闭环。

**Architecture:** commonMain 放纯数据 + 纯函数（编解码 / 聚合 / 语录选句），平台层（androidMain/appleMain）只提供 SharedPreferences 持久化、本地日期、设备存储三个薄 actual。埋点打在 `ViewerViewModel`（清理量）与 `ViewerScreen` 到达结果页处（轮次）。统计页是**单个 Composable 读 `ThemeTokens`** 适配 5 主题（同结构换 token）。

**Tech Stack:** Kotlin Multiplatform + Compose Multiplatform 1.7.3；持久化用现有 `cleanpic_prefs` SharedPreferences + 紧凑编码（仿 `PickStateCodec`，**零新依赖**，不引入 serialization-json 运行时）；测试 `kotlin.test`。

**范围切分：** 本计划交付核心闭环（数据采集 + 统计页 + 首页入口 + 测试）。spec §6.3「结果页升级（数字滚动动画 + 累计/语录）」需改 5 个 `*ResultLayout.kt`，作为**后续独立小计划**，不在本计划内。

**关键既有锚点（已核对）：**
- 持久化范式：`shared/src/androidMain/kotlin/com/cleanpic/settings/AndroidAppSettings.kt`（`cleanpic_prefs`）；编码范式：`shared/src/commonMain/kotlin/com/cleanpic/media/PickState.kt`（`PickStateCodec`）
- DI：`shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt`；平台初始化：`androidApp/src/main/java/com/cleanpic/android/MainActivity.kt:56`
- 埋点：`shared/src/commonMain/kotlin/com/cleanpic/viewmodel/ViewerViewModel.kt`（`confirmDelete()` 127-133，`currentType` 31，`loadMedia` 42）；到达结果页：`shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/ViewerScreen.kt:36`
- 平台 expect：`shared/src/commonMain/kotlin/com/cleanpic/Platform.kt`（`currentEpochMillis`）+ `Platform.android.kt` + `appleMain/.../Platform.apple.kt`
- 路由：`shared/src/commonMain/kotlin/com/cleanpic/ui/navigation/AppRouter.kt`（`Route` sealed class）；渲染分发：`shared/src/commonMain/kotlin/com/cleanpic/ui/App.kt:26`
- 图标：`shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt`（**单 path 字符串** map）+ `IconPainter.kt`（Canvas 绘制）
- 字节格式化：`shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/CarouselMode.kt:381` `internal fun formatBytes`
- 首页：`HomeScreen.kt` + `HomeScreenState.kt` + 5 个 `*HomeLayout.kt`
- 测试范式：`update/src/commonTest/.../UpdateCheckerTest.kt`；`shared/src/commonTest/.../viewmodel/ViewerViewModelTest.kt`
- 构建/测试脚本：`scripts/build-android.sh`、`scripts/test.sh`（CLAUDE.md 要求经脚本，不直接 gradle）

---

## 文件结构

**新建（commonMain）**
- `shared/src/commonMain/kotlin/com/cleanpic/model/CleanupStats.kt` — `MediaTypeStats` / `LifetimeStats` / `DailyStat` / `StatsSnapshot` / `StorageInfo`
- `shared/src/commonMain/kotlin/com/cleanpic/stats/StatsCodec.kt` — 紧凑编解码
- `shared/src/commonMain/kotlin/com/cleanpic/stats/StatsAggregator.kt` — 纯函数聚合
- `shared/src/commonMain/kotlin/com/cleanpic/stats/StatsStore.kt` — `StatsStore` 接口 + `InMemoryStatsStore`
- `shared/src/commonMain/kotlin/com/cleanpic/stats/CleanupQuotes.kt` — 语录池 + `pick`
- `shared/src/commonMain/kotlin/com/cleanpic/ui/stats/StatsScreen.kt` — 统计页（单 Composable）

**新建（androidMain）**
- `shared/src/androidMain/kotlin/com/cleanpic/stats/AndroidStatsStore.kt`

**新建（测试）**
- `shared/src/commonTest/kotlin/com/cleanpic/stats/StatsCodecTest.kt`
- `shared/src/commonTest/kotlin/com/cleanpic/stats/StatsAggregatorTest.kt`
- `shared/src/commonTest/kotlin/com/cleanpic/stats/CleanupQuotesTest.kt`
- `maestro/flows/cleanup-stats.yaml`

**修改**
- `shared/src/commonMain/kotlin/com/cleanpic/Platform.kt`（+`currentLocalDate`/`deviceStorage`）
- `shared/src/androidMain/kotlin/com/cleanpic/Platform.android.kt`（actual）
- `shared/src/appleMain/kotlin/com/cleanpic/Platform.apple.kt`（actual）
- `shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt`（+`statsStore`）
- `androidApp/src/main/java/com/cleanpic/android/MainActivity.kt`（注入 `AndroidStatsStore`）
- `shared/src/commonMain/kotlin/com/cleanpic/viewmodel/ViewerViewModel.kt`（埋点）
- `shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/ViewerScreen.kt`（轮次埋点调用）
- `shared/src/commonMain/kotlin/com/cleanpic/ui/navigation/AppRouter.kt`（+`Route.Stats`）
- `shared/src/commonMain/kotlin/com/cleanpic/ui/App.kt`（+ 分支）
- `shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt`（+`stats`/`storage`/`shield`）
- `shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt`（+`onOpenStats`）
- `shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreen.kt`（接线）
- 5 个 `*HomeLayout.kt`（设置图标旁加统计入口）

---

## Task 1: 数据模型 CleanupStats

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/model/CleanupStats.kt`

- [ ] **Step 1: 创建数据模型文件**

```kotlin
package com.cleanpic.model

/** 单一媒体类型（照片 或 视频）的统计三元组。 */
data class MediaTypeStats(
    val bytes: Long = 0L,
    val count: Int = 0,
    val rounds: Int = 0,
)

/** 累计聚合：照片 + 视频 各一份，所有"合计"由相加得出。 */
data class LifetimeStats(
    val photo: MediaTypeStats = MediaTypeStats(),
    val video: MediaTypeStats = MediaTypeStats(),
    val firstCleanupAt: Long = 0L,
    val lastCleanupAt: Long = 0L,
) {
    val totalBytes: Long get() = photo.bytes + video.bytes
    val totalCount: Int get() = photo.count + video.count
    val totalRounds: Int get() = photo.rounds + video.rounds
}

/** 按天明细（每天一条），照片/视频分别记。阶段一只采集，不展示。 */
data class DailyStat(
    val date: String,                 // "yyyy-MM-dd"（设备本地时区）
    val photo: MediaTypeStats = MediaTypeStats(),
    val video: MediaTypeStats = MediaTypeStats(),
)

/** 持久化的整体快照。 */
data class StatsSnapshot(
    val lifetime: LifetimeStats = LifetimeStats(),
    val daily: List<DailyStat> = emptyList(),
)

/** 设备存储现状（字节）。 */
data class StorageInfo(
    val totalBytes: Long,
    val availableBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
}
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/mark/Projects/feishu_bot_workspace/shuashuaya && ./scripts/build-android.sh`
Expected: BUILD SUCCESSFUL（仅新增 data class，不影响现有代码）

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/model/CleanupStats.kt
git commit -m "feat(stats): 清理统计数据模型（按类型拆分）"
```

---

## Task 2: StatsCodec 紧凑编解码（TDD）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/stats/StatsCodec.kt`
- Test: `shared/src/commonTest/kotlin/com/cleanpic/stats/StatsCodecTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
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
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd /Users/mark/Projects/feishu_bot_workspace/shuashuaya && ./scripts/test.sh`
Expected: FAIL，`StatsCodec` 未定义（unresolved reference）

- [ ] **Step 3: 实现 StatsCodec**

```kotlin
package com.cleanpic.stats

import com.cleanpic.model.*

/**
 * StatsSnapshot 的零依赖紧凑编解码（仿 PickStateCodec，不引入 kotlinx-serialization）。
 *
 * 格式：`<lifetime> §H§ <daily> §R§ <daily> ...`
 *   lifetime: pBytes§F§pCount§F§pRounds§F§vBytes§F§vCount§F§vRounds§F§firstAt§F§lastAt
 *   daily:    date§F§pBytes§F§pCount§F§pRounds§F§vBytes§F§vCount§F§vRounds
 * 控制字符 U+0001/U+0002/U+0003 作分隔符，date 不含这些字符。
 */
object StatsCodec {
    private const val F = ''   // 字段
    private const val R = ''   // daily 记录
    private const val H = ''   // lifetime / daily 段分隔

    fun encode(s: StatsSnapshot): String {
        val l = s.lifetime
        val head = listOf(
            l.photo.bytes, l.photo.count, l.photo.rounds,
            l.video.bytes, l.video.count, l.video.rounds,
            l.firstCleanupAt, l.lastCleanupAt,
        ).joinToString(F.toString())
        val body = s.daily.joinToString(R.toString()) { d ->
            listOf(
                d.date, d.photo.bytes, d.photo.count, d.photo.rounds,
                d.video.bytes, d.video.count, d.video.rounds,
            ).joinToString(F.toString())
        }
        return "$head$H$body"
    }

    fun decode(raw: String?): StatsSnapshot {
        if (raw.isNullOrEmpty()) return StatsSnapshot()
        val h = raw.indexOf(H)
        if (h < 0) return StatsSnapshot()
        val head = raw.substring(0, h).split(F)
        if (head.size != 8) return StatsSnapshot()
        val lifetime = LifetimeStats(
            photo = MediaTypeStats(
                head[0].toLongOrNull() ?: return StatsSnapshot(),
                head[1].toIntOrNull() ?: 0,
                head[2].toIntOrNull() ?: 0,
            ),
            video = MediaTypeStats(
                head[3].toLongOrNull() ?: 0L,
                head[4].toIntOrNull() ?: 0,
                head[5].toIntOrNull() ?: 0,
            ),
            firstCleanupAt = head[6].toLongOrNull() ?: 0L,
            lastCleanupAt = head[7].toLongOrNull() ?: 0L,
        )
        val body = raw.substring(h + 1)
        val daily = if (body.isEmpty()) emptyList() else body.split(R).mapNotNull { chunk ->
            if (chunk.isEmpty()) return@mapNotNull null
            val p = chunk.split(F)
            if (p.size != 7) return@mapNotNull null
            DailyStat(
                date = p[0],
                photo = MediaTypeStats(p[1].toLongOrNull() ?: 0L, p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0),
                video = MediaTypeStats(p[4].toLongOrNull() ?: 0L, p[5].toIntOrNull() ?: 0, p[6].toIntOrNull() ?: 0),
            )
        }
        return StatsSnapshot(lifetime, daily)
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./scripts/test.sh`
Expected: PASS（StatsCodecTest 4 个用例全绿）

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/stats/StatsCodec.kt shared/src/commonTest/kotlin/com/cleanpic/stats/StatsCodecTest.kt
git commit -m "feat(stats): StatsSnapshot 紧凑编解码 + 测试"
```

---

## Task 3: StatsAggregator 纯函数聚合（TDD）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/stats/StatsAggregator.kt`
- Test: `shared/src/commonTest/kotlin/com/cleanpic/stats/StatsAggregatorTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.cleanpic.stats

import com.cleanpic.model.MediaType
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
        // daily 同步
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
        assertEquals(1000L, s.lifetime.firstCleanupAt)  // 首次事件
        assertEquals(2000L, s.lifetime.lastCleanupAt)   // 最近事件
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
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./scripts/test.sh`
Expected: FAIL，`StatsAggregator` 未定义

- [ ] **Step 3: 实现 StatsAggregator**

```kotlin
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
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./scripts/test.sh`
Expected: PASS（StatsAggregatorTest 6 个用例全绿）

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/stats/StatsAggregator.kt shared/src/commonTest/kotlin/com/cleanpic/stats/StatsAggregatorTest.kt
git commit -m "feat(stats): 聚合纯函数（轮次/清理量解耦 + 按天）+ 测试"
```

---

## Task 4: CleanupQuotes 语录选句（TDD）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/stats/CleanupQuotes.kt`
- Test: `shared/src/commonTest/kotlin/com/cleanpic/stats/CleanupQuotesTest.kt`

> 阶段一情境：首次 / 连续 / 日常。里程碑随阶段二徽章一起做（需"刚跨阈值"判定，本期不做）。

- [ ] **Step 1: 写失败测试**

```kotlin
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
        // 不同 seed 都落在池内（无越界/崩溃）
        repeat(20) { i ->
            assertTrue(CleanupQuotes.pick(stats(rounds = 5), isStreak = false, seed = i) in CleanupQuotes.DAILY)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./scripts/test.sh`
Expected: FAIL，`CleanupQuotes` 未定义

- [ ] **Step 3: 实现 CleanupQuotes**

```kotlin
package com.cleanpic.stats

import com.cleanpic.model.LifetimeStats
import kotlin.math.abs

/** 治愈温柔语录池 + 情境化选句（纯函数，可测）。 */
object CleanupQuotes {

    val FIRST = listOf(
        "第一次清理完成，相册轻盈了一点点。",
        "万事开头难，你已经迈出第一步。",
        "清爽，从这一次开始。",
    )

    val STREAK = listOf(
        "又见面了，坚持整理的样子真好。",
        "保持节奏，相册会一直清清爽爽。",
        "今天也来收拾啦，给你点个赞。",
    )

    val DAILY = listOf(
        "干净的相册，像刚收拾好的房间。",
        "每一次清理，都是对自己温柔一点。",
        "少一点冗余，多一点清爽。",
        "整理好的不只是手机，还有心情。",
        "腾出的不只是空间，是翻相册的好心情。",
    )

    /**
     * @param isStreak 最近一次清理是否在"今天"（连续/活跃）。
     * @param seed 选句种子（调用方传，如累计轮次），保证纯函数可测、同种子稳定。
     */
    fun pick(stats: LifetimeStats, isStreak: Boolean, seed: Int): String {
        val pool = when {
            stats.totalRounds <= 1 -> FIRST
            isStreak -> STREAK
            else -> DAILY
        }
        return pool[abs(seed) % pool.size]
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./scripts/test.sh`
Expected: PASS（CleanupQuotesTest 5 个用例全绿）

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/stats/CleanupQuotes.kt shared/src/commonTest/kotlin/com/cleanpic/stats/CleanupQuotesTest.kt
git commit -m "feat(stats): 治愈温柔语录池 + 情境化选句 + 测试"
```

---

## Task 5: StatsStore 接口 + InMemory 实现

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/stats/StatsStore.kt`

- [ ] **Step 1: 创建接口 + 内存实现**

```kotlin
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
```

- [ ] **Step 2: 编译验证**

Run: `./scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/stats/StatsStore.kt
git commit -m "feat(stats): StatsStore 接口 + 内存实现"
```

---

## Task 6: 平台 actual — currentLocalDate + deviceStorage

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/Platform.kt`
- Modify: `shared/src/androidMain/kotlin/com/cleanpic/Platform.android.kt`
- Modify: `shared/src/appleMain/kotlin/com/cleanpic/Platform.apple.kt`

- [ ] **Step 1: commonMain 加 expect**

在 `Platform.kt` 末尾追加（与现有 `expect fun currentEpochMillis(): Long` 同文件）：

```kotlin
import com.cleanpic.model.StorageInfo

/** 设备本地日期，格式 "yyyy-MM-dd"。 */
expect fun currentLocalDate(): String

/** 设备存储现状（主分区）。 */
expect fun deviceStorage(): StorageInfo
```

- [ ] **Step 2: androidMain actual**

在 `Platform.android.kt` 末尾追加：

```kotlin
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.cleanpic.model.StorageInfo
import java.time.LocalDate   // minSdk=26，可用

actual fun currentLocalDate(): String = LocalDate.now().toString()  // ISO "yyyy-MM-dd"

actual fun deviceStorage(): StorageInfo {
    val stat = StatFs(Environment.getDataDirectory().path)
    val total = stat.blockCountLong * stat.blockSizeLong
    val available = stat.availableBlocksLong * stat.blockSizeLong
    return StorageInfo(totalBytes = total, availableBytes = available)
}
```

- [ ] **Step 3: appleMain actual（保证 iOS 编译；iOS 当前未完整发布）**

在 `Platform.apple.kt` 末尾追加：

```kotlin
import com.cleanpic.model.StorageInfo
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSFileSystemFreeSize

actual fun currentLocalDate(): String {
    val f = NSDateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    return f.stringFromDate(NSDate())
}

actual fun deviceStorage(): StorageInfo {
    val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath("/", null)
    val total = (attrs?.get(NSFileSystemSize) as? Long) ?: 0L
    val free = (attrs?.get(NSFileSystemFreeSize) as? Long) ?: 0L
    return StorageInfo(totalBytes = total, availableBytes = free)
}
```

- [ ] **Step 4: 编译验证（Android）**

Run: `./scripts/build-android.sh`
Expected: BUILD SUCCESSFUL（按 shuashuaya 记忆：iOS target 验证可跳过，只跑 Android）

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/Platform.kt shared/src/androidMain/kotlin/com/cleanpic/Platform.android.kt shared/src/appleMain/kotlin/com/cleanpic/Platform.apple.kt
git commit -m "feat(stats): 平台 actual — 本地日期 + 设备存储"
```

---

## Task 7: AndroidStatsStore（SharedPreferences）

**Files:**
- Create: `shared/src/androidMain/kotlin/com/cleanpic/stats/AndroidStatsStore.kt`

- [ ] **Step 1: 创建实现**

```kotlin
package com.cleanpic.stats

import android.content.Context
import com.cleanpic.model.MediaType
import com.cleanpic.model.StatsSnapshot

/** 写入现有 cleanpic_prefs，新 key，紧凑编码；不动任何老数据。 */
class AndroidStatsStore(context: Context) : StatsStore {
    private val prefs = context.getSharedPreferences("cleanpic_prefs", Context.MODE_PRIVATE)
    private val key = "stats_snapshot_v1"

    override fun load(): StatsSnapshot = StatsCodec.decode(prefs.getString(key, null))

    override fun recordRoundReached(type: MediaType, nowMillis: Long, today: String) =
        save(StatsAggregator.applyRound(load(), type, nowMillis, today))

    override fun recordDeletion(type: MediaType, nowMillis: Long, today: String, bytes: Long, count: Int) =
        save(StatsAggregator.applyDeletion(load(), type, nowMillis, today, bytes, count))

    override fun reset() { prefs.edit().remove(key).apply() }

    private fun save(s: StatsSnapshot) {
        prefs.edit().putString(key, StatsCodec.encode(s)).apply()
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `./scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/androidMain/kotlin/com/cleanpic/stats/AndroidStatsStore.kt
git commit -m "feat(stats): AndroidStatsStore（SharedPreferences + 紧凑编码）"
```

---

## Task 8: ServiceLocator 注入 + MainActivity 接线

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt`
- Modify: `androidApp/src/main/java/com/cleanpic/android/MainActivity.kt:56`

- [ ] **Step 1: ServiceLocator 加 statsStore**

在 `ServiceLocator` 对象内，`pickStateStore` 声明之后加：

```kotlin
    /** 清理统计持久化。平台侧可在 initialize 时注入；默认内存实现。 */
    var statsStore: com.cleanpic.stats.StatsStore = com.cleanpic.stats.InMemoryStatsStore()
```

并在 `initialize(...)` 的参数列表末尾加形参、函数体内赋值：

```kotlin
    fun initialize(
        mediaRepo: MediaRepository,
        settings: AppSettings,
        permission: PermissionManager,
        player: VideoPlayer,
        pickStateStore: PickStateStore = InMemoryPickStateStore(),
        statsStore: com.cleanpic.stats.StatsStore = com.cleanpic.stats.InMemoryStatsStore()
    ) {
        mediaRepository = mediaRepo
        appSettings = settings
        permissionManager = permission
        videoPlayer = player
        this.pickStateStore = pickStateStore
        this.statsStore = statsStore
        themeManager.switchTheme(settings.theme)
    }
```

- [ ] **Step 2: MainActivity 注入 AndroidStatsStore**

`MainActivity.kt:56` 的 `ServiceLocator.initialize(...)` 调用，在 `pickStateStore = ...` 后加一行（注意补逗号）：

```kotlin
        ServiceLocator.initialize(
            mediaRepo = AndroidMediaRepository(applicationContext),
            settings = AndroidAppSettings(applicationContext),
            permission = androidPermission,
            player = AndroidVideoPlayer(),
            pickStateStore = AndroidPickStateStore(applicationContext),
            statsStore = com.cleanpic.stats.AndroidStatsStore(applicationContext)
        )
```

- [ ] **Step 3: 编译验证**

Run: `./scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt androidApp/src/main/java/com/cleanpic/android/MainActivity.kt
git commit -m "feat(stats): ServiceLocator 注入 statsStore + 平台接线"
```

---

## Task 9: ViewerViewModel 埋点（TDD）

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/viewmodel/ViewerViewModel.kt`
- Test: `shared/src/commonTest/kotlin/com/cleanpic/viewmodel/ViewerViewModelTest.kt`（在现有文件追加用例）

> 现有 `ViewerViewModelTest` 已在 `setup` 调 `ServiceLocator.initialize(...)`。本任务新增用例需注入 `InMemoryStatsStore` 并断言其 `load()`。先读该测试文件确认现有 `initialize` 调用，按相同风格在用例内重新 `initialize` 并传 `statsStore`。

- [ ] **Step 1: 写失败测试（追加到 ViewerViewModelTest）**

```kotlin
    @Test fun confirm_delete_records_amount_into_stats() = runTest {
        val stats = com.cleanpic.stats.InMemoryStatsStore()
        ServiceLocator.initialize(
            mediaRepo = FakeMediaRepository(photos = listOf(
                MediaItem("p1", MediaType.PHOTO, "a", size = 100L, date = 0L, width = 1, height = 1),
                MediaItem("p2", MediaType.PHOTO, "b", size = 200L, date = 0L, width = 1, height = 1),
            )),
            settings = InMemoryAppSettings().apply { roundCount = 5 },
            permission = FakePermissionManager(),
            player = FakeVideoPlayer(),
            statsStore = stats
        )
        val vm = ViewerViewModel()
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()   // p1 标删
        vm.markDelete()   // p2 标删
        vm.confirmDelete()
        val snap = stats.load()
        assertEquals(2, snap.lifetime.photo.count)
        assertEquals(300L, snap.lifetime.photo.bytes)
    }

    @Test fun record_round_reached_is_idempotent_per_round() = runTest {
        val stats = com.cleanpic.stats.InMemoryStatsStore()
        ServiceLocator.initialize(
            mediaRepo = FakeMediaRepository(photos = listOf(
                MediaItem("p1", MediaType.PHOTO, "a", size = 100L, date = 0L, width = 1, height = 1),
            )),
            settings = InMemoryAppSettings().apply { roundCount = 5 },
            permission = FakePermissionManager(),
            player = FakeVideoPlayer(),
            statsStore = stats
        )
        val vm = ViewerViewModel()
        vm.loadMedia(MediaType.PHOTO)
        vm.recordRoundReached()
        vm.recordRoundReached()  // 同一轮重复调用
        assertEquals(1, stats.load().lifetime.photo.rounds)  // 只记一次
    }
```

> 注：`FakeMediaRepository` / `FakePermissionManager` / `FakeVideoPlayer` 用现有 `ViewerViewModelTest` 里已有的测试替身（读文件确认其构造签名后对齐参数；若现有替身字段名不同，按其实际签名调整 `photos =`）。

- [ ] **Step 2: 运行测试确认失败**

Run: `./scripts/test.sh`
Expected: FAIL，`recordRoundReached` 未定义 / stats 未记录

- [ ] **Step 3: 实现埋点**

在 `ViewerViewModel.kt` 顶部加 import：

```kotlin
import com.cleanpic.currentLocalDate
import com.cleanpic.di.ServiceLocator
```
（`ServiceLocator` 已 import；新增 `currentLocalDate`。`currentEpochMillis` 已 import。）

加 store 访问器 + 轮次标志（放在 `private val pickStore` 附近）：

```kotlin
    private val statsStore get() = ServiceLocator.statsStore
    private var roundCounted = false
```

在 `loadMedia(...)` 内（设置 `currentType = type` 之后）重置标志：

```kotlin
        currentType = type
        roundCounted = false
```

新增轮次埋点方法（放在 `confirmDelete` 之前）：

```kotlin
    /** 到达结果页时调用：本轮只记一次（spec：进入清理→到结果页即一轮，无论删留）。 */
    fun recordRoundReached() {
        if (roundCounted) return
        val type = currentType ?: return
        roundCounted = true
        statsStore.recordRoundReached(type, currentEpochMillis(), currentLocalDate())
    }
```

改 `confirmDelete()` —— 删除成功后记清理量（用删除前算好的 bytes/count）：

```kotlin
    suspend fun confirmDelete(): Result<Int> {
        val items = pendingDeletes.map { it.media }
        if (items.isEmpty()) return Result.success(0)
        val type = currentType
        val bytes = items.sumOf { it.size }
        val count = items.size
        val result = repo.deleteMediaItems(items)
        if (result.isSuccess) {
            forgetRecords(items.map { it.id })
            if (type != null) {
                statsStore.recordDeletion(type, currentEpochMillis(), currentLocalDate(), bytes, count)
            }
        }
        return result
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./scripts/test.sh`
Expected: PASS（新增 2 用例 + 原有用例全绿）

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/viewmodel/ViewerViewModel.kt shared/src/commonTest/kotlin/com/cleanpic/viewmodel/ViewerViewModelTest.kt
git commit -m "feat(stats): ViewerViewModel 埋点（轮次幂等 + 清理量按类型）+ 测试"
```

---

## Task 10: ViewerScreen 触发轮次埋点

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/ViewerScreen.kt:36`

- [ ] **Step 1: 在到达结果页的 LaunchedEffect 里调用埋点**

把 36-44 行的 `LaunchedEffect` 改为（导航前先记轮次）：

```kotlin
    LaunchedEffect(viewerViewModel.isComplete, currentIndex) {
        if (items.isNotEmpty() && currentIndex >= items.size) {
            viewerViewModel.recordRoundReached()
            router.navigate(
                Route.Result,
                clearBackStackUpTo = Route.Viewer(type),
                inclusive = true
            )
        }
    }
```

- [ ] **Step 2: 编译验证**

Run: `./scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/ViewerScreen.kt
git commit -m "feat(stats): 到达结果页时记一轮"
```

---

## Task 11: 新增图标（AppIcons）

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt`

> `AppIcons.paths` 是**单 path 字符串** map（可含多段 `M` 子路径）。新增 3 个线性图标（描边由主题 token 控制），复用现有 `back`/`photo`/`video`。

- [ ] **Step 1: 在 `paths` map 末尾追加三个图标**

在 `"fullscreen" to "..."` 后加逗号并追加：

```kotlin
        "stats" to "M3 3v18h18M8 17v-3M13 17V5M18 17V9",
        "storage" to "M22 12H2M5.45 5.11L2 12v6a2 2 0 002 2h16a2 2 0 002-2v-6l-3.45-6.89A2 2 0 0016.76 4H7.24a2 2 0 00-1.79 1.11z",
        "shield" to "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10zM9 12l2 2 4-4"
```

- [ ] **Step 2: 编译验证**

Run: `./scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt
git commit -m "feat(stats): 新增 stats/storage/shield 图标"
```

---

## Task 12: Route.Stats + App.kt 分发

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/navigation/AppRouter.kt`
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/App.kt:32`

- [ ] **Step 1: Route 加 Stats**

在 `Route` sealed class 内（`Settings` 之后）加：

```kotlin
    data object Stats : Route()
```

- [ ] **Step 2: App.kt when 加分支**

在 `is Route.Settings -> ...` 之后加（与现有分支同缩进）：

```kotlin
            is Route.Stats -> StatsScreen(router, theme)
```

并在文件顶部 import 区加：

```kotlin
import com.cleanpic.ui.stats.StatsScreen
```

> 注：此步引用的 `StatsScreen` 在 Task 13 创建。可与 Task 13 连续完成后再编译；本步只改路由，编译留到 Task 13 末尾。

- [ ] **Step 3: Commit（与 Task 13 合并提交也可）**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/navigation/AppRouter.kt shared/src/commonMain/kotlin/com/cleanpic/ui/App.kt
git commit -m "feat(stats): 新增 Route.Stats 与路由分发"
```

---

## Task 13: StatsScreen 统计页（单 Composable，读 ThemeTokens）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/stats/StatsScreen.kt`

> 布局定稿见 `docs/superpowers/specs/stats-mockup.html` / `stats-5themes-h.png`。结构：返回栏 → 成果总览大卡(大数字+副标+分隔+语录) → 分类构成(照片/视频 各三元组+占比条) → 设备存储 → 离线声明。颜色/圆角/阴影/字体全部读 `theme`。复用 `formatBytes`、`IconPainter`。

- [ ] **Step 1: 创建 StatsScreen**

```kotlin
package com.cleanpic.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpic.currentLocalDate
import com.cleanpic.deviceStorage
import com.cleanpic.di.ServiceLocator
import com.cleanpic.icons.IconPainter
import com.cleanpic.stats.CleanupQuotes
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.ui.viewer.formatBytes

@Composable
fun StatsScreen(router: AppRouter, theme: ThemeTokens) {
    val snapshot = remember { ServiceLocator.statsStore.load() }
    val storage = remember { deviceStorage() }
    val l = snapshot.lifetime
    val isStreak = remember {
        l.lastCleanupAt > 0L && epochToLocalDate(l.lastCleanupAt) == currentLocalDate()
    }
    val quote = remember { CleanupQuotes.pick(l, isStreak, seed = l.totalRounds) }

    val bg = Color(theme.colorBackground)
    val surface = Color(theme.colorSurface)
    val text = Color(theme.colorText)
    val sub = Color(theme.colorTextSecondary)
    val accent = Color(theme.colorAccent)
    val radius = theme.borderRadius.dp

    Column(
        modifier = Modifier.fillMaxSize().background(bg).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(50.dp))
        // 返回栏
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPainter("back", theme, size = 22.dp, colorOverride = theme.colorText,
                modifier = Modifier.clickable { router.popBackStack() })
            Spacer(Modifier.width(12.dp))
            Text("清理成果", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = text)
        }
        Spacer(Modifier.height(14.dp))

        // 成果总览大卡
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(surface).padding(18.dp)
        ) {
            Text("累计已清理", fontSize = 13.sp, color = sub)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatBytes(l.totalBytes), fontSize = 44.sp, fontWeight = FontWeight.Black, color = text)
            }
            Spacer(Modifier.height(7.dp))
            Text("共 ${l.totalCount} 个文件 · 完成 ${l.totalRounds} 轮清理", fontSize = 12.5.sp, color = sub)
            Spacer(Modifier.height(13.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(sub.copy(alpha = 0.18f)))
            Spacer(Modifier.height(13.dp))
            Text(quote, fontSize = 13.sp, color = sub)
        }
        Spacer(Modifier.height(12.dp))

        // 分类构成
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(surface).padding(18.dp)) {
            Text("分类构成", fontSize = 11.sp, color = sub, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            TypeRow("photo", "照片", l.photo, l.totalBytes, "张", theme)
            Spacer(Modifier.height(14.dp))
            TypeRow("video", "视频", l.video, l.totalBytes, "个", theme)
        }
        Spacer(Modifier.height(12.dp))

        // 设备存储
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(surface).padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconPainter("storage", theme, size = 14.dp, colorOverride = theme.colorTextSecondary)
                    Spacer(Modifier.width(7.dp))
                    Text("设备存储", fontSize = 12.5.sp, color = sub)
                }
                Text("已用 ${formatBytes(storage.usedBytes)} / ${formatBytes(storage.totalBytes)}", fontSize = 12.5.sp, color = sub)
            }
            Spacer(Modifier.height(8.dp))
            val frac = if (storage.totalBytes > 0) (storage.usedBytes.toFloat() / storage.totalBytes).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(99.dp)).background(sub.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(frac).height(9.dp).clip(RoundedCornerShape(99.dp)).background(accent))
            }
        }
        Spacer(Modifier.height(12.dp))

        // 离线声明
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPainter("shield", theme, size = 14.dp, colorOverride = theme.colorTextSecondary)
            Spacer(Modifier.width(8.dp))
            Text("全程离线 · 清理记录仅存本机，不上传", fontSize = 11.5.sp, color = sub)
        }
    }
}

@Composable
private fun TypeRow(icon: String, name: String, s: com.cleanpic.model.MediaTypeStats, totalBytes: Long, unit: String, theme: ThemeTokens) {
    val text = Color(theme.colorText)
    val sub = Color(theme.colorTextSecondary)
    val accent = Color(theme.colorAccent)
    val pct = if (totalBytes > 0) (s.bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPainter(icon, theme, size = 16.dp, colorOverride = theme.colorAccent)
            Spacer(Modifier.width(7.dp))
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = text)
        }
        Text("占 ${(pct * 100).toInt()}%", fontSize = 12.sp, color = sub)
    }
    Spacer(Modifier.height(8.dp))
    Row {
        Metric(formatBytes(s.bytes), "大小", theme); Spacer(Modifier.width(20.dp))
        Metric("${s.count}", unit, theme); Spacer(Modifier.width(20.dp))
        Metric("${s.rounds}", "轮", theme)
    }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(sub.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxWidth(pct).height(8.dp).clip(RoundedCornerShape(99.dp)).background(accent))
    }
}

@Composable
private fun Metric(value: String, label: String, theme: ThemeTokens) {
    Column {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(theme.colorText))
        Text(label, fontSize = 11.sp, color = Color(theme.colorTextSecondary))
    }
}

/** epoch 毫秒 → 本地 "yyyy-MM-dd"（与 currentLocalDate 同口径，仅用于 streak 判断）。 */
private fun epochToLocalDate(millis: Long): String = com.cleanpic.epochToLocalDate(millis)
```

> 注：`epochToLocalDate` 需作为平台 expect（android `java.time`，apple `NSDateFormatter`）。在 Task 6 的 `Platform.kt` 一并加 `expect fun epochToLocalDate(millis: Long): String`，android actual `Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()`，apple actual 用 `NSDateFormatter` + `NSDate(timeIntervalSince1970 = millis/1000.0)`。**执行 Task 6 时补上这第三个 expect/actual。**（见下方 Self-Review 修正）

- [ ] **Step 2: 编译验证（含 Task 12 的路由改动）**

Run: `./scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/stats/StatsScreen.kt
git commit -m "feat(stats): 清理成果统计页（单 Composable 适配 5 主题）"
```

---

## Task 14: 首页入口（HomeScreenState + HomeScreen + 5 个 Layout）

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt`
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreen.kt`
- Modify: 5 个 `*HomeLayout.kt`

> 入口 = 设置图标邻位加一个 `stats` 图标，点击 `onOpenStats`。各 layout 在现有设置图标旁追加。

- [ ] **Step 1: HomeScreenState 加回调**

在 `HomeScreenState` 的 `onOpenSettings` 之后加：

```kotlin
    val onOpenStats: () -> Unit,
```

- [ ] **Step 2: HomeScreen 接线**

在 `HomeScreen.kt` 的 `HomeScreenState(...)` 构造里，`onOpenSettings = { router.navigate(Route.Settings) },` 之后加：

```kotlin
        onOpenStats = { router.navigate(Route.Stats) },
```

- [ ] **Step 3: MinimalHomeLayout — 顶栏设置图标左侧加 stats**

`MinimalHomeLayout.kt` 顶栏 `Row` 内，把右侧单个设置 `IconPainter` 替换为一个 `Row { stats; spacer; settings }`：

```kotlin
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconPainter(
                    name = "stats", theme = theme, size = 20.dp, colorOverride = 0xFF999999,
                    modifier = Modifier.testTag("stats_button").clickable { state.onOpenStats() }
                )
                Spacer(modifier = Modifier.width(18.dp))
                IconPainter(
                    name = "settings", theme = theme, size = 20.dp, colorOverride = 0xFF999999,
                    modifier = Modifier.testTag("settings_button").clickable { state.onOpenSettings() }
                )
            }
```

- [ ] **Step 4: WarmHomeLayout — 底部设置图标左侧加 stats**

`WarmHomeLayout.kt` 底部单个设置 `IconPainter` 替换为：

```kotlin
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPainter(
                name = "stats", theme = theme, size = 28.dp,
                modifier = Modifier.testTag("stats_button").clickable { state.onOpenStats() }
            )
            Spacer(modifier = Modifier.width(30.dp))
            IconPainter(
                name = "settings", theme = theme, size = 28.dp,
                modifier = Modifier.testTag("settings_button").clickable { state.onOpenSettings() }
            )
        }
```

- [ ] **Step 5: 其余 3 个 Layout（Geometric / Playful / Editorial）同款追加**

读各文件，定位其 `name = "settings"` 的 `IconPainter`，在其紧邻位置（同一 Row/容器内，设置图标之前）加一个相同尺寸/同色的 `stats` 图标，`testTag("stats_button")`，`clickable { state.onOpenStats() }`。保持各主题原有图标尺寸与颜色参数一致（照抄该文件 settings 图标的 `size`/`colorOverride`，仅把 `name` 换 `"stats"`、回调换 `onOpenStats`、testTag 换 `"stats_button"`）。

- [ ] **Step 6: 编译验证**

Run: `./scripts/build-android.sh`
Expected: BUILD SUCCESSFUL（5 个 layout 都已加 `onOpenStats`，且 `HomeScreenState` 新字段已被构造）

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/home/
git commit -m "feat(stats): 首页设置图标旁新增清理成果入口（5 主题）"
```

---

## Task 15: Maestro E2E + 全量验证

**Files:**
- Create: `maestro/flows/cleanup-stats.yaml`

- [ ] **Step 1: 写 E2E flow**

```yaml
# 进入清理成果页并确认核心元素可见
appId: com.cleanpic.android
---
- launchApp:
    clearState: false
- tapOn:
    id: "stats_button"
- assertVisible: "清理成果"
- assertVisible: "累计已清理"
- assertVisible: "分类构成"
- assertVisible: "设备存储"
- assertVisible: "全程离线 · 清理记录仅存本机，不上传"
```

- [ ] **Step 2: 单元测试全绿**

Run: `./scripts/test.sh`
Expected: PASS（StatsCodec / StatsAggregator / CleanupQuotes / ViewerViewModel 新增用例全绿）

- [ ] **Step 3: 构建 + 安装 + E2E**

Run:
```bash
./scripts/build-android.sh
adb install -r androidApp/build/outputs/apk/direct/debug/*.apk
~/.maestro/bin/maestro test maestro/flows/cleanup-stats.yaml
```
Expected: Flow PASS（能从首页 stats_button 进入统计页，5 个元素可见）

> 按 shuashuaya 记忆：发布相关脚本前须 `export LANG/LC_ALL=en_US.UTF-8`；本任务为本地验证不发布，无需。

- [ ] **Step 4: Commit**

```bash
git add maestro/flows/cleanup-stats.yaml
git commit -m "test(stats): 清理成果页 Maestro E2E 流"
```

---

## Self-Review 修正（执行时务必注意）

1. **`epochToLocalDate` 第三个 expect/actual：** Task 13 的 streak 判断需要 `expect fun epochToLocalDate(millis: Long): String`。**在 Task 6 同时加这第三个 expect/actual**（android：`java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()`；apple：`NSDateFormatter("yyyy-MM-dd").stringFromDate(NSDate(timeIntervalSince1970 = millis / 1000.0))`）。Task 13 里 `private fun epochToLocalDate` 改为直接 `import com.cleanpic.epochToLocalDate` 使用，删掉该私有转发函数。

2. **测试替身签名：** Task 9 的 `FakeMediaRepository`/`FakePermissionManager`/`FakeVideoPlayer` 以现有 `ViewerViewModelTest.kt` 中的定义为准——执行前先读该文件，按其真实构造参数对齐（本计划示例参数名可能与实际不同）。

3. **`build-android.sh` 输出 APK 路径：** Task 15 的 `adb install` 路径以脚本实际产物为准（若不是 `direct/debug`，按 `find androidApp/build/outputs/apk -name '*.apk'` 结果调整）。

4. **类型/命名一致性核对：** `StatsStore`/`InMemoryStatsStore`/`AndroidStatsStore` 三处方法签名一致；`StatsAggregator.applyRound/applyDeletion` 与 store 调用一致；`Route.Stats` 在 AppRouter 与 App.kt 一致；图标名 `stats`/`storage`/`shield` 与 StatsScreen/HomeLayout 引用一致；`onOpenStats` 在 State/HomeScreen/5 Layout 一致。
