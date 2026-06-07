# 刷刷鸭 · 清理统计功能设计

- 日期：2026-06-07
- 状态：设计待评审
- 范围：**C 全规划，本次只实现「阶段一」**

## 1. 背景与目标

刷刷鸭现在只有「本轮」统计（`ViewerViewModel.releasedBytes` / `deletedCount`），关闭即丢，用户看不到自己长期清理的成果。

本功能给用户一个**清理成果中心**：把用户通过 App 真实清理掉的照片/视频的大小、数量、完成轮次累计下来并持续展示，制造正反馈、降低"删除焦虑"，同时把"全程离线、记录仅存本机"做成信任卖点。

设计目标：

- 数字**绝不注水**——只统计用户真实删除成功的文件。
- 沿用现有持久化范式（SharedPreferences + kotlinx.serialization），**零新依赖**。
- 阶段一就开始采集明细数据，为后续阶段（徽章/趋势/月度回顾）**提前埋数据**，避免历史不可回溯。

## 2. 核心概念定义

### 2.1 两个独立指标，互不污染

| 指标 | 含义 | 全保留的一轮 |
|---|---|---|
| **完成轮次** | 用户审阅完一轮的次数（使用/习惯指标） | +1 |
| **清理量 / 清理数量** | 真实删除成功的字节数与文件数（成果指标） | +0 |

两者分开存储、分开展示，谁也不注水。

### 2.2 "一轮"的定义

**从进入清理（开始浏览）走到结果页，即算完成一轮，无论是否删除。**

- 判定点：**到达结果页**（浏览阶段的自然终点）。
- 到达结果页时轮次 +1，每轮只记一次（用一次性标志位防止结果页重组重复计数）。
- 边界一致性：
  - 到结果页未点删除就退出 → 轮次 +1，清理量 +0。
  - 到结果页点删除但 Android 11+ 系统弹窗被取消 → 轮次 +1，清理量 +0。
  - 到结果页删成功 N 个 → 轮次 +1，清理量累加真实删除的 N 个。

> 选择"使用次数"口径而非"有效清理次数"：它对 streak / 月度回顾是更有用的留存指标，且不丢信息——"清理量>0 的次数"随时能再算出来，反之则会把"看了但全留"的使用行为永久丢失。

### 2.3 诚实性原则（信任红线）

- **只记真实删除成功的**文件（以 `confirmDelete` 实际删除结果为准），系统弹窗取消的不计。
- Android 11+ 删除是**移入系统回收站**（约保留 30 天），可用空间不会立刻释放。因此成果文案统一用 **"已清理"**，而非死磕"已释放空间"——避免用户拿 `StatFs` 一对发现"空间没变"而崩塌信任。
- `StatFs` 只用于展示"设备存储现状"（可用/已用），清理量以**文件尺寸累加**为准。
- 页面明确标注 **"全程离线 · 清理记录仅存本机，不上传"**，把纯本地变成卖点。
- 统计库**只存元数据**（时间、大小、类型、数量、轮次），**绝不缓存**被删文件的缩略图或内容。

## 3. 数据模型（`model/` 层，纯数据）

```kotlin
// 累计聚合
data class LifetimeStats(
    val totalBytes: Long,      // 累计清理字节
    val totalDeleted: Int,     // 累计清理文件数
    val totalPhotos: Int,      // 其中照片数
    val totalVideos: Int,      // 其中视频数
    val totalRounds: Int,      // 累计完成轮次
    val firstCleanupAt: Long,  // 首次清理时间戳（0 = 无）
    val lastCleanupAt: Long,   // 最近清理时间戳
)

// 按天明细（每天一条），为阶段二/三的趋势、连续天数、月度回顾提前埋数据
data class DailyStat(
    val date: String,          // "yyyy-MM-dd"（设备本地时区）
    val bytes: Long,
    val deleted: Int,
    val photos: Int,
    val videos: Int,
    val rounds: Int,
)

// 持久化的整体快照
data class StatsSnapshot(
    val lifetime: LifetimeStats,
    val daily: List<DailyStat>,
)
```

> `daily` 按天聚合，天然限制增长（每条约几十字节，数年也仅数十 KB）。阶段一只采集、不展示。

## 4. 持久化（`stats/` 新包）

照搬 `AppSettings` / `AndroidAppSettings` 范式：

```kotlin
// commonMain
interface StatsStore {
    fun load(): StatsSnapshot
    fun recordRoundReached(nowMillis: Long, today: String)
    fun recordDeletion(
        nowMillis: Long, today: String,
        deletedBytes: Long, photos: Int, videos: Int,
    )
    fun reset()   // 供设置页"清空统计"用（阶段一可选）
}
```

- `AndroidStatsStore`：写入现有 `cleanpic_prefs`，**新 key**（如 `stats_snapshot_v1`），kotlinx.serialization JSON 序列化整个 `StatsSnapshot`，不动任何老数据。
- 聚合/合并逻辑（把一次事件并入 snapshot）抽成 **commonMain 纯函数** `StatsAggregator`，便于单测（参考现有 `RandomPicker` / `PickStateCodec` 的可测范式）。
- `today` / `nowMillis` 由调用方（平台层）传入，保持 commonMain 纯净、可测。
- 在 `ServiceLocator` 注入 `statsStore`。

## 5. 埋点（核心改动）

位置：`viewmodel/ViewerViewModel.kt`

1. **轮次**：到达结果页时调用 `statsStore.recordRoundReached(...)`，每轮一次性（用标志位 `roundCounted`，进入新一轮时重置）。
2. **清理量**：`confirmDelete()` 中 `repo.deleteMediaItems(items)` **返回成功后**，按实际删除结果调用 `statsStore.recordDeletion(...)`，分别统计照片/视频数与字节。

> 现有 `confirmDelete()` 在 `ViewerViewModel.kt:127-133`；删除返回 `Result<Int>`（实际删除数）。需以实际删除的项为准累加，而非 `pendingDeletes` 的全集。

## 6. UI（阶段一）

技术栈：Compose Multiplatform + 自定义 `AppRouter`（沿用现有）。

### 6.1 清理成果页 `StatsScreen`（新增 `Route.Stats`）

1. **累计大卡**：已清理总量（字节格式化）、清理数量、完成轮次。
2. **拟物化换算**：根据累计量自动选最贴切表达（见 §7）。
3. **设备存储现状**：`StatFs` 读真实可用/已用，前后对比条。
4. **离线信任声明**："全程离线 · 清理记录仅存本机，不上传"。
5. **预留占位区**：为阶段二（徽章/连续天数/构成）、阶段三（月度回顾/分享/备份）留结构，阶段一不渲染实际内容。

### 6.2 首页入口

`HomeScreen` 增加"清理成果"卡片/入口，导航到 `Route.Stats`。正反馈要显眼，不藏进设置。

### 6.3 结果页升级 `ResultScreen`

- 本轮完成后展示"**本次清理 X** + **累计已清理 Y**"。
- **数字滚动动画**（Compose `animate*AsState`），最低成本最高回报的爽点。
- 附拟物化换算文案。

### 6.4 文案

沿用项目现状（硬编码中文，无 i18n）。统一用"已清理"口径。

## 7. 拟物化换算基准（写死取整，便于记忆）

| 基准 | 取值 |
|---|---|
| 1 张照片 | 4 MB（1GB ≈ 250 张） |
| 1 分钟 1080p 视频 | 100 MB（1GB ≈ 10 分钟） |
| 1 部电影（1080p） | ≈ 2 GB |

文案优先用"照片/视频"换算（与本品类直接相关、最可信），大数字时补"电影"。避免"绕地球 X 圈"这类不可信类比。

示例：释放 2GB → "≈ 500 张照片 / ≈ 1 部电影"。

## 8. 测试策略

- `StatsAggregator`（commonMain 纯函数）单测：累计合并、按天聚合、首次/最近时间、照片/视频拆分、轮次与清理量解耦（全保留轮次：轮次+1 量+0）。
- 换算函数单测：各量级选词正确。
- 参考现有 `update/src/commonTest/.../UpdateCheckerTest.kt` 范式。
- UI 以编译通过 + 手动/Maestro 冒烟为准（不强制 UI 单测）。

## 9. 分阶段路线图

- **阶段一（本次实现）**：数据模型 + `StatsStore`/`StatsAggregator` + 埋点（轮次 + 清理量）+ `StatsScreen`（累计/换算/存储现状/离线声明）+ 首页入口 + 结果页升级（数字动画）。`daily` 数据开始采集但不展示。
- **阶段二（B）**：里程碑徽章（1G/10G/100G、1000 张等）、连续清理天数(streak)、照片/视频构成图。
- **阶段三（C）**：月度回顾卡、可分享成果图、本地备份导出（解决纯本地换机归零）。

## 10. 明确不做（YAGNI，阶段一）

- 不引入数据库/Room（SharedPreferences + JSON 足够）。
- 不做云同步/账号。
- 不在阶段一渲染徽章/趋势/月度回顾/分享/备份（仅留结构与数据采集）。
- 不申请任何新权限。
