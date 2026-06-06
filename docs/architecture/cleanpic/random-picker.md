# 随机选取算法 — Shuffle Bag 洗牌袋 + 持久化浏览记忆

|文档状态| 初稿 | 2026-06-06 |

> 父文档: [overview.md](overview.md) ｜ 关联 US: US-CP-22 / US-CP-23 ｜ 设计稿: [../../superpowers/specs/2026-06-06-shuffle-bag-random-design.md](../../superpowers/specs/2026-06-06-shuffle-bag-random-design.md)

## 1. 解决的问题

旧实现 `RandomPicker.pick(items, count, exclude=shownIds)` 用**内存 `shownIds` 集合**去重，存在四个缺陷，导致"最近/已保留过的反复出现"：

1. 回首页 `clearSession()` 即清空 `shownIds`，刚看过的立刻可重抽。
2. 纯内存、无持久化，App 重启后归零。
3. 候选耗尽即硬重置为全量纯随机。
4. 均匀随机、无"保留过"概念，已保留媒体反复出现。

## 2. 方案选型（行业调研结论）

| 方案 | 机制 | 对本场景 |
|------|------|---------|
| **Shuffle Bag**（游戏业标准，Apple `GKShuffledDistribution`） | 从"袋子"不放回抽取，抽空再装满 | ✅ 骨架：硬保证一圈内不重复、零调参、自适应相册大小 |
| **LRU / recency-weighted**（缓存淘汰） | 最近最少使用优先，深度按选项数调 | ✅ 补位排序"最久优先" |
| **Spotify Fewer Repeats** | 生成多序列按新鲜度打分择优 | ❌ 为播放"序列"设计，对"抽 N 张"过重、难测 |

**采纳**：Shuffle Bag 骨架 + 保留集沉底 + 天数新鲜度微调 + 删除懒自愈。

## 3. 核心模型

### 3.1 持久化状态（按 MediaType 各存一份）

```kotlin
@Serializable
data class PickState(
    val cycle: Int = 0,                          // 当前循环号
    val records: Map<String, SeenRecord> = emptyMap(),
)

@Serializable
data class SeenRecord(
    val lastDrawnCycle: Int,    // 上次被抽中时的循环号 → 实现"本循环不重复"
    val lastSeenMillis: Long,   // 上次出现时间 → 天数新鲜度
    val kept: Boolean,          // 是否被保留过 → 沉底
)
```

- **"袋子"是派生概念**，不单独存：id 在袋中 ⟺ `records[id]` 不存在，或 `records[id].lastDrawnCycle < cycle`。
- 序列化为 JSON 存入现有 `cleanpic_prefs`，键 `pick_state_photo` / `pick_state_video`。

### 3.2 决策口径

轮播流 `goNext` 默认把未决策项置 KEPT，被删项确认后从相册移除。因此展示过的项最终非 KEPT 即 DELETE：

- 保留（KEPT）→ `record.kept = true`，进入沉底集。
- 删除（确认）→ 相册消失，`record` 一并移除。

## 4. 抽取算法（纯函数）

`RandomPicker.pick(live, count, state, now, freshDays=1): PickResult(items, newState)`

对 `live` 中 id `x`（`rec = state.records[x]`）：
- `drawnThisCycle = rec != null && rec.lastDrawnCycle == state.cycle`
- `kept = rec != null && rec.kept`
- `recentlySeen = rec != null && now - rec.lastSeenMillis < freshDays*86_400_000`

**逐层填充**结果（结果内严格唯一，最多 count；层内随机，L4 按 `lastSeenMillis` 最久优先）：

| 层 | 条件 | 含义 |
|----|------|------|
| **L1 主池·新鲜** | `!kept && !drawnThisCycle && !recentlySeen` | 最该抽 |
| **L2 主池·含近期** | `!kept && !drawnThisCycle` | 补：含近 N 天看过的 |
| *进位* | L1∪L2 不足 count ⇒ 本循环非保留项抽空，`cycle++`，所有 `!kept` 项重新可抽 | |
| **L3 新循环主池** | `!kept`（排除本轮已选） | 新循环 |
| *兜底* | 仍不足 ⇒ 整库非保留项 < count ⇒ 动用保留集 | |
| **L4 保留兜底** | `kept`，最久优先（排除本轮已选） | 保留项放回 |
| *大循环重置* | 动用 L4 且 `live == kept` ⇒ 所有 `kept=false`、`cycle++` | "刷完一圈才再见" |

**抽取后**：结果中每项写 `lastDrawnCycle=cycle`、`lastSeenMillis=now`，`kept` 不变；持久化新 state。

**不变式**：① 单轮结果内绝不重复；② 同 cycle 内不重复（洗牌袋）；③ 保留项仅在非保留项耗尽时出现。

## 5. 删除自愈与存储封顶

**核心洞察**：抽取是集合运算 `候选 ⊆ live`，已删除 id 即使残留于 `records` 也不在 `live` 中，**天然被忽略，不影响正确性**——残留仅占存储。

- **本 App 删除**：`confirmDelete` 成功后 `records -= 删除 id`。
- **外部删除**：无需实时处理，靠懒清理兜底。
- **懒清理 + 封顶**：每次抽取若 `records.size > max(live.size*1.2, 2000)`，先剔除不在 `live` 的 id，仍超则丢弃 `lastSeenMillis` 最老的。被丢项重新变"从没抽过"，符合最久优先语义。

## 6. 行为变更

| 位置 | 旧 | 新 |
|------|----|----|
| `ResultScreen.onGoHome` | `clearSession()` 清空 | 不再清空，持久化保留 |
| 清空历史 | 仅回首页隐式触发 | 新增设置项「重置浏览记录」+ 二次确认，显式 `PickStateStore.clearAll()` |

## 7. 组件与文件（纯逻辑与持久化分离）

| 组件 | 位置 | 职责 |
|------|------|------|
| `RandomPicker`（改纯函数） | `commonMain/media/RandomPicker.kt` | `pick(live,count,state,now,freshDays): PickResult`，无副作用 |
| `PickState` / `SeenRecord` / `PickResult`（新增） | `commonMain/media/PickState.kt` | `@Serializable` 状态模型 |
| `PickStateStore`（新增 interface） | `commonMain/media/PickStateStore.kt` | `load(type)`/`save(type,state)`/`clearAll()` |
| `AndroidPickStateStore`（新增） | `androidMain/media/AndroidPickStateStore.kt` | SharedPreferences(`cleanpic_prefs`) + `Json` |
| `InMemoryPickStateStore`（新增，占位） | `commonMain` | iOS/HarmonyOS 暂用内存实现 |
| `ServiceLocator` | `commonMain/di/ServiceLocator.kt` | 注入 `pickStateStore` |
| `ViewerViewModel` | `commonMain/viewmodel/ViewerViewModel.kt` | 移除 `_shownIds`；loadMedia=load→pick→save；保留更新 `kept`；删除后移除 record；`clearSession`→`store.clearAll()` |
| 设置页 | `commonMain/ui/settings/*` | 新增「重置浏览记录」入口 |

### 数据流（替换旧 shownIds 流）

```
loadMedia(type)
  ├─ all   = repo.query{Photos|Videos}()
  ├─ state = pickStateStore.load(type)
  ├─ result= RandomPicker.pick(all, roundCount, state, now)
  ├─ pickStateStore.save(type, result.newState)
  └─ _items = result.items
markKept / goNext(默认保留)  → records[id].kept=true → save
confirmDelete                → records -= 删除 id     → save
设置·重置浏览记录            → pickStateStore.clearAll()
```

## 8. 边界场景

| 场景 | 行为 |
|------|------|
| 空相册 | 返回空、置 isEmpty（同现状） |
| `count ≥ 非保留项数` | 返回所有可用项（可能短于 count），不足才纳入保留项填满，单轮仍不重复 |
| 整库都保留过 | 大循环重置（kept 清零、cycle++），符合"刷完一圈才再见" |
| App 中途被杀 | 已抽未决策项已记 lastDrawnCycle，本循环不再出现；未标 kept，下循环正常回归 |
| 外部新增 | 无 record，自动入 L1/L2，一视同仁 |
| 外部删除 | record 残留无害，懒清理回收 |
| MediaStore `_ID` 重扫描变化 | 该项当新项出现一次，已知假设，不额外处理 |
| roundCount 变更 | 无影响，下轮按新值抽 |

## 9. 关联

- US: US-CP-22（更聪明的随机）、US-CP-23（重置浏览记录），US-CP-03 末条 AC 由此规则承接。
- 测试: [../../testing/scenarios/ep7-random-enhancement.md](../../testing/scenarios/ep7-random-enhancement.md)
- 版本: 显著增强现有功能 → MINOR。
