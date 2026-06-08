# 清理成果统计 — 按类型累计 + 统计页 + 首页入口

|文档状态| 阶段一~三已实现 | 2026-06-08 |

> 父文档: [overview.md](overview.md) ｜ 关联 US: US-CP-27、US-CP-28 ｜ 设计稿: [../../superpowers/specs/2026-06-07-cleanup-stats-design.md](../../superpowers/specs/2026-06-07-cleanup-stats-design.md) ｜ 实现计划: [../../superpowers/plans/2026-06-07-cleanup-stats.md](../../superpowers/plans/2026-06-07-cleanup-stats.md)

## 1. 目标

把用户真实清理掉的照片/视频（大小·数量·完成轮次，**照片/视频完全拆分**）持久化累计，新增「清理成果」统计页 + 首页入口，形成"清理 → 记录 → 查看成果"闭环。原则：数字**绝不注水**（只记真实删除成功的），**全程离线、记录仅存本机**，**零新依赖**（仿 `PickStateCodec` 紧凑编码，不引入 serialization-json 运行时）。

§1~11 覆盖**阶段一**（按类型累计 + 统计页 + 首页入口）。阶段二（里程碑徽章/连续天数/构成环形图）、阶段三（月度回顾）已实现，见 **§12**；剩余路线图项（分享/本地备份）仍待做。

## 2. 两个独立指标

| 指标 | 含义 | 全保留的一轮 |
|------|------|------|
| 完成轮次 | 用户审阅完一轮的次数（使用/习惯） | +1 |
| 清理量/数量 | 真实删除成功的字节与文件数（成果） | +0 |

「一轮」沿用 domain-model「一轮清理 = Round/Session」：**从进入清理到到达结果页即一轮，无论是否删除**。判定点 = 到达结果页，每轮一次性（标志位防重复计数）。

## 3. 数据模型（`model/CleanupStats.kt`，纯数据）

照片/视频各一份 `MediaTypeStats`，合计由相加得出。

```kotlin
data class MediaTypeStats(val bytes: Long = 0, val count: Int = 0, val rounds: Int = 0)

data class LifetimeStats(
    val photo: MediaTypeStats = MediaTypeStats(),
    val video: MediaTypeStats = MediaTypeStats(),
    val firstCleanupAt: Long = 0, val lastCleanupAt: Long = 0,
) {
    val totalBytes get() = photo.bytes + video.bytes
    val totalCount get() = photo.count + video.count
    val totalRounds get() = photo.rounds + video.rounds
}

data class DailyStat(val date: String, val photo: MediaTypeStats = MediaTypeStats(), val video: MediaTypeStats = MediaTypeStats())
data class StatsSnapshot(val lifetime: LifetimeStats = LifetimeStats(), val daily: List<DailyStat> = emptyList())
data class StorageInfo(val totalBytes: Long, val availableBytes: Long) { val usedBytes get() = (totalBytes - availableBytes).coerceAtLeast(0) }
```

`daily`（按天明细）阶段一只采集不展示，为阶段二/三趋势/连续天数/月度回顾**提前埋数据**（每条约百余字节，数年仅数十 KB）。

## 4. 持久化（`stats/` 新包）

```kotlin
interface StatsStore {
    fun load(): StatsSnapshot
    fun recordRoundReached(type: MediaType, nowMillis: Long, today: String)
    fun recordDeletion(type: MediaType, nowMillis: Long, today: String, bytes: Long, count: Int)
    fun reset()
}
```

- `InMemoryStatsStore`（commonMain，测试/默认）+ `AndroidStatsStore`（写现有 `cleanpic_prefs`，新 key `stats_snapshot_v1`，不动老数据）。
- `StatsCodec`：仿 `PickStateCodec` 的零依赖紧凑编解码（控制字符分隔），脏数据/空串安全回退默认。
- `StatsAggregator`：commonMain **纯函数**，把单次事件并入快照——`applyRound` 只 +rounds，`applyDeletion` 只 +bytes/count，二者解耦；`firstCleanupAt` 首次事件设、`lastCleanupAt` 每次更新；按 `today` 合并/新增 daily 行。
- 注入：`ServiceLocator.statsStore`（默认 InMemory，`MainActivity` 注入 Android 实现）。

## 5. 埋点（`ViewerViewModel` + `ViewerScreen`）

- **轮次**：`ViewerScreen` 到达结果页的 `LaunchedEffect`（`currentIndex >= items.size`）里调 `viewerViewModel.recordRoundReached()`；VM 内 `roundCounted` 标志保证每轮一次，`loadMedia` 重置。
- **清理量**：`ViewerViewModel.confirmDelete()` 删除成功后，按删除前算好的 `bytes/count` 调 `statsStore.recordDeletion(currentType, …)`。一轮只一种 `MediaType`，无需一次混记两类。

> 诚实性：Android 11+ 删除走系统回收站弹窗，用户取消则 `confirmDelete` 不成功、不计；空间不立即释放，故成果文案统一用「已清理」而非「已释放空间」。

## 6. 语录（`stats/CleanupQuotes.kt`）

治愈温柔文案池 + 纯函数 `pick(stats, isStreak, seed)` 按情境选句：`首次(totalRounds≤1) > 连续(isStreak) > 日常`。种子由调用方传（用累计轮次），保证可测、同种子稳定。里程碑语录随阶段二徽章一起做（需"刚跨阈值"判定，本期不做）。

## 7. UI

### 7.1 统计页 `StatsScreen`（`ui/stats/`，新增 `Route.Stats`）

**单个 Composable 读 `ThemeTokens` 适配 5 主题**（同结构换 token，无需像首页/结果页拆 5 个 layout 文件 —— mockup `stats-5themes-h.png` 已验证）。单屏不滚动，自上而下：

1. 返回栏
2. 成果总览大卡：累计已清理大数字 + 「共 N 文件 · 完成 N 轮」副标 + 淡分隔 + **语录**（情感注脚，归属此卡）
3. 分类构成：照片/视频 各 `图标 + 名称 + 占比% + (大小·数量·轮次) + 占比条`（占比按字节）
4. 设备存储：`StatFs` 真实已用/总量 + 进度条
5. 离线声明：盾牌图标 + 文案

### 7.2 首页入口（纯图标 · 与设置并排）

各主题首页**设置图标邻位**加一个统计图标（点击 `Route.Stats`），不露数字、不占版面，主功能保持主角。改 `HomeScreenState`(+`onOpenStats`) + `HomeScreen` + 5 个 `*HomeLayout.kt`。定稿见 `../../superpowers/specs/home-entry-clean.png`。

### 7.3 图标（`AppIcons`）

`AppIcons.paths` 为**单 path 字符串**（非多元素 SVG）。复用现有 `back`/`photo`/`video`；新增单 path：`stats`(柱状图，首页入口)、`storage`(硬盘)、`shield`(隐私)。描边随各主题 `iconStrokeWidth`，**不用 emoji**。

## 8. 平台 actual（`Platform.kt` expect/actual）

| expect | Android actual | Apple actual |
|--------|----------------|--------------|
| `currentLocalDate(): String` | `java.time.LocalDate.now().toString()`（minSdk 26） | `NSDateFormatter("yyyy-MM-dd")` |
| `epochToLocalDate(ms): String` | `Instant.ofEpochMilli(...).atZone(systemDefault()).toLocalDate()` | `NSDateFormatter` + `NSDate(timeIntervalSince1970)` |
| `deviceStorage(): StorageInfo` | `StatFs(Environment.getDataDirectory())` | `NSFileManager` attributesOfFileSystem |

## 9. 接线表

| 组件 | 改动 |
|------|------|
| `model/CleanupStats.kt`（新） | 数据模型 |
| `stats/StatsCodec.kt`（新） | 紧凑编解码 |
| `stats/StatsAggregator.kt`（新） | 纯函数聚合 |
| `stats/StatsStore.kt`（新） | 接口 + InMemory |
| `stats/AndroidStatsStore.kt`（新，androidMain） | SharedPreferences 实现 |
| `stats/CleanupQuotes.kt`（新） | 语录池 + 选句 |
| `ui/stats/StatsScreen.kt`（新） | 统计页（5 主题适配） |
| `Platform.kt` + android/apple actual | 日期 + 设备存储 |
| `di/ServiceLocator.kt` / `MainActivity.kt` | 注入 statsStore |
| `viewmodel/ViewerViewModel.kt` | 埋点（轮次 + 清理量） |
| `ui/viewer/ViewerScreen.kt` | 到达结果页触发轮次 |
| `ui/navigation/AppRouter.kt` / `ui/App.kt` | `Route.Stats` + 分发 |
| `icons/AppIcons.kt` | +stats/storage/shield |
| `ui/home/HomeScreenState.kt` + `HomeScreen.kt` + 5×`*HomeLayout.kt` | 首页入口 |

## 10. 边界

| 场景 | 行为 |
|------|------|
| 从未清理 | 各项 0，页面正常 |
| 全保留一轮 | 完成轮次 +1，清理量 +0 |
| 系统弹窗取消删除 | 轮次已 +1（已到结果页），清理量 +0 |
| 结果页重组多次 | `roundCounted` 保证只记一次 |
| 脏/空持久化数据 | `StatsCodec.decode` 回退默认快照，不崩 |
| 设备存储读取失败/为 0 | 进度条按 0 处理，不崩 |

## 11. 测试 / 版本

- 测试：[../../testing/scenarios/ep8-cleanup-stats.md](../../testing/scenarios/ep8-cleanup-stats.md)（U-ST-* 单元 / I-ST-* 集成 / E-ST-* E2E）。
- 版本：新增用户可见功能 → MINOR（阶段一 1.x；阶段二/三随 1.14.0 发布）。

---

## 12. 阶段二 / 三实现（连续天数 · 里程碑 · 构成环形图 · 月度回顾 · 结果页累计）

延续阶段一原则：**零新依赖、全程离线、数字绝不注水**。逻辑全部落在 commonMain 纯函数（`stats/` 新增 4 个 object），数据底子复用阶段一就开始采集的 `daily` 明细，故历史可回溯、无需迁移。

### 12.1 纯日期运算 `stats/DateMath.kt`

commonMain 不可用 `java.time`，故自带零依赖日期运算（连续天数与月度聚合的共同底座）：

| 函数 | 作用 | 关键点 |
|------|------|--------|
| `epochDay("yyyy-MM-dd"): Int?` | 日期 → 自 1970-01-01 起的天序号（可负） | Howard Hinnant `days_from_civil` 算法，覆盖闰年/世纪规则；非法格式回 null |
| `yearMonth("yyyy-MM-dd"): String?` | → 「yyyy-MM」月份键 | 月份补零；非法回 null |

### 12.2 连续清理天数 `stats/StatsStreak.kt`

`current(daily, today): Int` —— 把 `daily` 日期映射成 `epochDay` 集合，从今天起向前数连续命中的天数。**口径**：今天有清理则从今天起算；今天未清理但昨天清理过则从昨天起算（避免"今天还没打开"就清零）；今天和昨天都没有则为 0。

### 12.3 里程碑徽章 `stats/Milestones.kt`

`evaluate(lifetime): List<Badge>` —— 对固定的 8 个阈值定义判定达成，**全部返回**（含未达成，UI 灰显），顺序固定便于稳定渲染与测试。`achievedCount()` 供「X / Y」概览。

| 维度 | 阈值 |
|------|------|
| 累计字节 `totalBytes` | 1 GB / 10 GB / 100 GB（GB 按 1e9） |
| 累计文件 `totalCount` | 100 / 1000 |
| 完成轮次 `totalRounds` | 10 / 50 / 100 |

`Badge(id, label, icon, achieved)`，icon 复用 `storage`/`photo`/`stats` 现有 path。

### 12.4 月度回顾 `stats/MonthlyReview.kt`

`byMonth(daily): List<MonthStat>` —— 把 `daily` 按 `yearMonth` 聚合（照片/视频各自累加），**按月份倒序**（最近月在前）。`forMonth(daily, ym)` 取指定月、无数据回 null。`MonthStat` 暴露 `totalBytes/Count/Rounds`。

### 12.5 构成环形图（统计页 `ui/stats/StatsScreen.kt`）

阶段一「分类构成」的占比条升级为 **Canvas 环形图**，按字节占比绘制照片/视频两段弧。单 Composable 读 `ThemeTokens` 适配 5 主题，沿用阶段一不滚动单屏；里程碑徽章网格、连续天数、月度回顾卡均接入本页。

### 12.6 结果页即时成果反馈 `ui/result/ResultCumulativeBlock.kt`（US-CP-28）

结果页「完成」态注脚：`累计已清理 Y` + 语录。`Animatable` 单插值 0→1，900ms `FastOutSlowInEasing`：累计从 `prevLifetime = (lifetimeBytes − roundBytes)` 滚到 `lifetimeBytes`，**强调本轮增量**。5 个结果布局视觉各异，故配色由调用方传入，本块只统一结构 + 动画；接线见各 `*ResultLayout.kt` + `ResultScreen(State)`。

> 去重：本轮单次清理字节（`freedBytes`，与顶部三宫格「已释放」`freedSpace` 同源）原在注脚再展示一行「本次清理」，与顶部重复，故移除；注脚专讲长期累计 + 情绪价值。`roundBytes` 入参保留，仅用于算累计动画起点 `prevLifetime`。

### 12.7 诚实性红线（回归补强）

`ViewerViewModelTest.cancelled_or_failed_delete_does_not_record_amount`：模拟系统回收站弹窗取消 / 删除失败（`confirmDelete` 不成功），断言**清理量绝不入账**——结果页累计与统计页同口径，不注水。

### 12.8 接线表（阶段二/三新增）

| 组件 | 改动 |
|------|------|
| `stats/DateMath.kt`（新） | 零依赖日期运算 |
| `stats/StatsStreak.kt`（新） | 连续清理天数纯函数 |
| `stats/Milestones.kt`（新） | 8 徽章评估纯函数 |
| `stats/MonthlyReview.kt`（新） | daily → 按月聚合纯函数 |
| `ui/stats/StatsScreen.kt` | 环形图 + 徽章网格 + 连续天数 + 月度回顾卡 |
| `ui/result/ResultCumulativeBlock.kt`（新） | 结果页本次/累计滚动注脚 |
| `ui/result/ResultScreen(State).kt` + 5×`*ResultLayout.kt` | 接入注脚 + 传各主题配色 |

### 12.9 统计页入场动画

进页面时单个 `Animatable(0f)` 在 `LaunchedEffect(Unit)` 里 900ms `FastOutSlowInEasing` 动画到 1f（与结果页注脚同款），其值 `p` 驱动整页：累计大数字 / 文件数·轮次 / 环形图 sweep / 中心数字 / 各 `TypeRow` 的大小·占比%·占比条 / 设备存储条全部乘 `p`，从 0 增长到最终值；里程碑徽章、月度回顾保持静态避免眼花。`when(route)` 条件渲染保证每次进入都是全新 composition、动画必重触发。

> 验证记录：0.9s 动画在本地 maestro+模拟器环境无法用截图/断言观测（图形抓取在动画期间卡帧、maestro 命令等动画 settle 后才读），最终靠 `screenrecord` 抓到进页面首帧为「0 B / 空环形」确认动画从 0 起步、确实生效。

### 12.10 深色/同色系主题的图表配色（colorStatsAccent）

Geometric（深蓝霓虹）、Playful（蓝紫玻璃）的 `colorAccent` 都是偏暗的紫，在统计页背景上对比不足、发闷：深紫 on 深蓝 → 闷；深紫 on 蓝紫 → 同色系糊一起。`ThemeTokens` 新增可选 `colorStatsAccent`（默认 null → computed `statsAccent` 回退 `colorAccent`），StatsScreen 的数据可视化（环形图 / 里程碑徽章 / 连续天数 / 占比条 / 分类图标）统一读 `theme.statsAccent`：

| 主题 | colorStatsAccent | 说明 |
|------|------------------|------|
| Geometric | `0xFFFF5277` 亮玫红 | 深蓝霓虹背景上大胆醒目 |
| Playful | `0xFFFF9ECF` 亮粉 | 蓝紫背景上活泼明快 |
| 其余 3 主题 | 未设（null） | 沿用各自 colorAccent |

仅作用于统计页数据色，不改主题在其他页面的强调色（按钮等）。色值定稿经实机预览迭代（首版 #E94560/#FF7EB3 偏深 → 提亮为 #FF5277/#FF9ECF）。
