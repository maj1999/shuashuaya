# 随机算法优化设计 — Shuffle Bag 洗牌袋方案

- 日期：2026-06-06
- 主题：消除「最近几轮 / 已保留过的照片视频反复出现」问题
- 状态：待评审（仅设计，未实现）

---

## 1. 背景与问题

App 每轮随机抽 N 张照片/视频让用户逐一决定去留。用户反馈：**经常抽到最近几轮看过、甚至已经"保留"过的内容**，体验像"假随机"。

### 现状实现

核心是 `RandomPicker.pick`（`shared/src/commonMain/.../media/RandomPicker.kt`）：

```kotlin
fun pick(items, count, exclude: Set<String>): List<MediaItem> {
    var available = items.filter { it.id !in exclude }
    if (available.isEmpty()) available = items   // 耗尽即重置为全量
    return available.shuffled().take(count)       // 纯均匀随机
}
```

去重靠 `ViewerViewModel._shownIds`（内存 `MutableSet<String>`）。

### 四个根因

1. **回首页即清空**：`ResultScreen.kt` 的 `onGoHome → clearSession()` 把 `_shownIds` 整个清空，刚看过的立刻可被重抽。
2. **纯内存、无持久化**：App 被杀/重启后历史归零，跨会话不记得。
3. **耗尽即硬重置**：`available` 为空时回退到全量纯随机，刚看过的马上回来。
4. **均匀随机、无"保留过"概念**：用户保留的照片一直留在库中，每轮等概率参与抽取，反复出现。

---

## 2. 设计目标

| 目标 | 说明 |
|------|------|
| 一圈内不重复 | 在抽完当前一轮"可抽集合"之前，不重复任何一项 |
| 跨重启记忆 | 历史持久化，App 重启后仍生效 |
| 保留过的沉底 | 用户"保留"过的内容强压制，几乎不再出现，直到整库都被决策过一遍 |
| 新增/删除自愈 | 新拍的自动参与、删除的自动失效，无需手工维护 |
| 新老一视同仁 | 新照片不插队，与未保留的旧照片同等概率 |
| 零魔法数 | 冷却强度随相册大小自适应，不依赖人工调参 |

---

## 3. 行业调研结论

| 方案 | 机制 | 适用 | 对我们 |
|------|------|------|--------|
| **Shuffle Bag**（游戏业标准，Apple `GKShuffledDistribution`） | 从"袋子"不放回地抽，抽空再装满 | 离散、要公平轮转 | ✅ 采纳为骨架 |
| **LRU / recency-weighted**（缓存淘汰） | 最近最少使用优先淘汰；深度按选项数调 | 时间衰减场景 | ✅ 用于补位排序与自适应 |
| **Spotify "Fewer Repeats"** | 生成多个随机序列按新鲜度打分择优 | 大列表、序列听感 | ❌ 为"播放顺序"设计，对"抽 N 张"过重、难测、难解释 |

**采纳**：Shuffle Bag 为骨架 + 保留集沉底 + 一点"天数新鲜度"微调（借鉴打分思想的最小化） + 删除懒自愈。理由：硬保证不重复、零魔法数、持久化状态小、可测试性与可解释性强、契合现有 KMP + SharedPreferences。

> Shuffle Bag 的"袋子 = 相册"天然自适应：大相册几百轮不重复，小相册一圈即循环，无需任何冷却参数 K。

---

## 4. 核心模型

### 4.1 持久化状态（按 MediaType 分别存储）

照片与视频是独立会话（`queryPhotos` / `queryVideos`），各自维护一份状态。

```kotlin
@Serializable
data class PickState(
    val cycle: Int = 0,                              // 当前循环号
    val records: Map<String, SeenRecord> = emptyMap()
)

@Serializable
data class SeenRecord(
    val lastDrawnCycle: Int,    // 上次被抽中时的循环号（实现"本循环不重复"的洗牌袋语义）
    val lastSeenMillis: Long,   // 上次出现时间（天数新鲜度用）
    val kept: Boolean           // 是否被用户保留过（沉底用）
)
```

- 序列化为 JSON，存入现有 `cleanpic_prefs`，键 `pick_state_photo` / `pick_state_video`。
- **"袋子"是派生概念**，不单独存：某 id 在袋中 ⟺ `records[id]` 不存在，或 `records[id].lastDrawnCycle < cycle`（本循环还没抽到它）。

### 4.2 决策口径

轮播流中 `goNext` 默认把未决策项标为 KEPT，被删的会真正从相册移除。因此每个展示过的项最终非 KEPT 即 DELETE：

- **保留（KEPT）** → `record.kept = true`，进入沉底集。
- **删除（确认后）** → 从相册消失，`record` 一并移除。

---

## 5. 抽取算法（每轮）

输入：`live`（当前相册全量）、`count`（roundCount）、`state`、`now`、`freshDays`（默认 1 天）。

对 live 中某 id `x`，记 `rec = state.records[x]`：

- `drawnThisCycle` = `rec != null && rec.lastDrawnCycle == state.cycle`
- `kept` = `rec != null && rec.kept`
- `recentlySeen` = `rec != null && now - rec.lastSeenMillis < freshDays * 86_400_000`

按优先级**逐层填充**结果（结果内**严格唯一**，最多 `count` 个）；层内随机，保留兜底层按 `lastSeenMillis` 最久优先：

| 层 | 条件 | 含义 |
|----|------|------|
| **L1 主池·新鲜** | `!kept && !drawnThisCycle && !recentlySeen` | 最该抽：本循环没抽过、非保留、近 N 天没见 |
| **L2 主池·含近期** | `!kept && !drawnThisCycle` | 补：把"近 N 天看过的"也算进来 |
| *进位* | 若 L1∪L2 仍不足 count ⇒ 本循环非保留项已抽空，`cycle++` 开新循环（所有 `!kept` 项重新变 `!drawnThisCycle`） | |
| **L3 新循环主池** | `!kept`（排除本轮已选） | 新循环重新洗牌 |
| *兜底* | 若仍不足 ⇒ 整库非保留项 < count（库太小或保留太多）⇒ 动用保留集 | |
| **L4 保留兜底** | `kept`，按 `lastSeenMillis` 最久优先（排除本轮已选） | 保留过的也得放回，但仍最久优先 |
| *大循环重置* | 若动用 L4 且 `live == kept`（整库都保留过）⇒ 把所有 `record.kept = false`，`cycle++`，开始全新一轮 | "刷完一圈才再见" |

填满 `count` 或所有层耗尽即停。

**抽取后更新**：对结果中每个 id，写 `record.lastDrawnCycle = state.cycle`（含进位后的值）、`lastSeenMillis = now`，`kept` 保持不变；持久化新 state。

> 关键不变式：**单轮结果内绝不重复**（逐层只追加未入选 id）；**洗牌袋语义**（同一 cycle 内不重复，cycle 进位才放回）。

---

## 6. 删除自愈与存储封顶

**核心洞察**：抽取是集合运算 `候选 ⊆ live`。已删除的 id 即使残留在 `records` 里，也不在 `live` 中，**天然被忽略，不影响任何行为**——残留只占存储，不影响正确性。

因此对账从"每轮必做"降级为"懒清理"：

- **本 App 删除**：`confirmDelete` 成功后顺手 `records -= 删除的 id`。
- **外部删除**：无需实时处理；靠下面的懒清理兜底。
- **懒清理 + 封顶**：每次抽取时，若 `records.size` 超过阈值（`max(live.size * 1.2, 2000)`），先剔除不在 `live` 中的 id，仍超则按 `lastSeenMillis` 丢弃最老的。被丢的项重新变回"从没抽过"，符合"最久优先"语义，不矛盾。

---

## 7. 行为变更

| 位置 | 现状 | 变更后 |
|------|------|--------|
| `ResultScreen.onGoHome` | `clearSession()` 清空内存历史 | **不再清空**，历史持久化保留 |
| 清空历史入口 | 仅回首页时隐式触发 | 新增**设置项「重置浏览记录」**，显式清空 `pick_state_photo` + `pick_state_video` |

---

## 8. 组件与文件

遵循现有 `expect/actual` + `ServiceLocator` 模式，**纯逻辑与持久化分离**，保持可测试。

| 组件 | 位置 | 职责 |
|------|------|------|
| `RandomPicker`（改为纯函数） | `commonMain/.../media/RandomPicker.kt` | `pick(live, count, state, now, freshDays): PickResult(items, newState)`，无副作用 |
| `PickState` / `SeenRecord`（新增） | `commonMain/.../media/PickState.kt` | `@Serializable` 状态模型 |
| `PickStateStore`（新增 interface） | `commonMain/.../media/PickStateStore.kt` | `load(type)` / `save(type, state)` / `clearAll()` |
| `AndroidPickStateStore`（新增） | `androidMain/.../media/AndroidPickStateStore.kt` | SharedPreferences(`cleanpic_prefs`) + `Json` 实现 |
| `ServiceLocator` | `commonMain/.../di/ServiceLocator.kt` | 新增 `pickStateStore` 注入 |
| `ViewerViewModel` | `commonMain/.../viewmodel/ViewerViewModel.kt` | 移除 `_shownIds`；`loadMedia` 改为 load→pick→save；保留时更新 `kept`；删除后移除 record；`clearSession` 改为调用 `store.clearAll()` |
| 设置页 | `commonMain/.../ui/settings/*` | 新增「重置浏览记录」入口 |

> iOS / HarmonyOS：当前仅 Android 完整实现。`PickStateStore` 在其余平台先用内存实现（`InMemoryPickStateStore`）占位，与项目现状一致。

### 数据流

```
loadMedia(type)
  ├─ all   = repo.query{Photos|Videos}()
  ├─ state = pickStateStore.load(type)
  ├─ result= RandomPicker.pick(all, roundCount, state, now)
  ├─ pickStateStore.save(type, result.newState)
  └─ _items = result.items

markKept / goNext(默认保留)  → state.records[id].kept = true → save
confirmDelete                → state.records -= 删除 id        → save
设置·重置浏览记录            → pickStateStore.clearAll()
```

---

## 9. 边界情况

| 场景 | 行为 |
|------|------|
| 空相册 | 返回空，置 `isEmpty`（同现状） |
| `count ≥ 非保留项数` | 本轮返回所有可用项（可能短于 count）；不足才纳入保留项填满，**单轮内仍不重复** |
| 整库都保留过 | 触发大循环重置（`kept` 清零、`cycle++`），保留过的重新出现——符合"刷完一圈才再见" |
| App 中途被杀 | 已抽未决策项已记 `lastDrawnCycle`，本循环不再出现；未标 `kept`，下个循环正常回归。可接受 |
| 外部新增照片 | 无 record，自动落入 L1/L2 主池，一视同仁 |
| 外部删除照片 | record 残留无害，懒清理回收 |
| MediaStore `_ID` 因重扫描变化 | 该项被当作新项出现一次。**已知假设**，不额外处理 |
| `roundCount` 变更 | 无影响，下轮按新值抽 |

---

## 10. 测试方案

### 单元测试（`RandomPicker` 纯函数，扩展现有 `RandomPickerTest`）

- 单轮结果内无重复（含 `count > 库大小` 时）。
- 洗牌袋不变式：连续多轮，在"非保留项耗尽"前不出现重复。
- 保留沉底：被标 `kept` 的项在非保留项耗尽前不出现。
- 天数新鲜度：有更优项时，"近 N 天看过的"排在后面。
- 大循环重置：整库保留后触发 `kept` 清零并可重现。
- 删除：从 record 移除后不影响后续；残留不在 live 的 id 被忽略与懒清理。
- 新增：新 id 当轮即可被抽，与旧项概率相当。

### 集成测试（`ViewerViewModelTest`）

- 跨"模拟重启"（重建 VM，共用同一 `PickStateStore`）历史保持，第二段不重复第一段。
- 回首页不再清空历史（替换原 `next_round_excludes_shown` / `clearSession_resets` 用例）。
- 「重置浏览记录」后历史清空、可重现。

### E2E（Maestro，按 `CLAUDE.md` 测试纪律必做）

- 刷一轮 → 回首页 → 再刷，断言不出现刚才的内容。
- 设置·重置浏览记录后，可再次出现。

---

## 11. 版本与发布

按 `CLAUDE.md`：属"显著增强现有功能" → **MINOR** 版本号 +1。实现合入后询问用户是否发布 Release。

---

## 12. 待实现时确认的小项（不阻塞设计）

- `freshDays` 默认值（暂定 1 天），是否暴露为设置项（建议否，YAGNI）。
- 「重置浏览记录」入口的文案与二次确认弹窗。
