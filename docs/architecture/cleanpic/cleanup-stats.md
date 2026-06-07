# 清理成果统计 — 按类型累计 + 统计页 + 首页入口

|文档状态| 初稿 | 2026-06-07 |

> 父文档: [overview.md](overview.md) ｜ 关联 US: US-CP-27 ｜ 设计稿: [../../superpowers/specs/2026-06-07-cleanup-stats-design.md](../../superpowers/specs/2026-06-07-cleanup-stats-design.md) ｜ 实现计划: [../../superpowers/plans/2026-06-07-cleanup-stats.md](../../superpowers/plans/2026-06-07-cleanup-stats.md)

## 1. 目标

把用户真实清理掉的照片/视频（大小·数量·完成轮次，**照片/视频完全拆分**）持久化累计，新增「清理成果」统计页 + 首页入口，形成"清理 → 记录 → 查看成果"闭环。原则：数字**绝不注水**（只记真实删除成功的），**全程离线、记录仅存本机**，**零新依赖**（仿 `PickStateCodec` 紧凑编码，不引入 serialization-json 运行时）。

本文覆盖**阶段一**。阶段二（里程碑徽章/连续天数/构成饼图）、阶段三（月度回顾/分享/本地备份）见设计稿路线图，后续单独立文档。

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
- 版本：新增用户可见功能 → MINOR（下一发布版本号按当时基线 +MINOR）。
