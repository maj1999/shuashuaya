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

**核心：照片 / 视频完全拆分**（大小、数量、轮次都各记一份），合计由相加得出。App 的清理入口本就分"清理照片""清理视频"，一轮只属于一种类型。

```kotlin
// 单一媒体类型（照片 或 视频）的统计三元组
data class MediaTypeStats(
    val bytes: Long,   // 清理字节
    val count: Int,    // 清理数量
    val rounds: Int,   // 完成轮次
)

// 累计聚合：照片 + 视频 各一份，所有"合计"都由相加得出
data class LifetimeStats(
    val photo: MediaTypeStats,
    val video: MediaTypeStats,
    val firstCleanupAt: Long,  // 首次清理时间戳（0 = 无）
    val lastCleanupAt: Long,   // 最近清理时间戳
) {
    val totalBytes get() = photo.bytes + video.bytes
    val totalCount get() = photo.count + video.count
    val totalRounds get() = photo.rounds + video.rounds
}

// 按天明细（每天一条），照片/视频分别记；为阶段二/三的趋势、连续天数、月度回顾提前埋数据
data class DailyStat(
    val date: String,          // "yyyy-MM-dd"（设备本地时区）
    val photo: MediaTypeStats,
    val video: MediaTypeStats,
)

// 持久化的整体快照
data class StatsSnapshot(
    val lifetime: LifetimeStats,
    val daily: List<DailyStat>,
)
```

> `daily` 按天聚合，天然限制增长（每条约百余字节，数年也仅数十 KB）。阶段一只采集、不展示。

## 4. 持久化（`stats/` 新包）

照搬 `AppSettings` / `AndroidAppSettings` 范式：

```kotlin
// commonMain。一轮只属于一种 MediaType，故两个 record 都带 type
interface StatsStore {
    fun load(): StatsSnapshot
    fun recordRoundReached(type: MediaType, nowMillis: Long, today: String)
    fun recordDeletion(type: MediaType, nowMillis: Long, today: String, bytes: Long, count: Int)
    fun reset()   // 供设置页"清空统计"用（接口保留，阶段一不接入 UI）
}
```

- `AndroidStatsStore`：写入现有 `cleanpic_prefs`，**新 key**（如 `stats_snapshot_v1`），kotlinx.serialization JSON 序列化整个 `StatsSnapshot`，不动任何老数据。
- 聚合/合并逻辑（把一次事件并入 snapshot）抽成 **commonMain 纯函数** `StatsAggregator`，便于单测（参考现有 `RandomPicker` / `PickStateCodec` 的可测范式）。
- `today` / `nowMillis` 由调用方（平台层）传入，保持 commonMain 纯净、可测。
- 在 `ServiceLocator` 注入 `statsStore`。

## 5. 埋点（核心改动）

位置：`viewmodel/ViewerViewModel.kt`（当前清理类型即 Viewer 的 `MediaType`）

1. **轮次**：到达结果页时调用 `statsStore.recordRoundReached(currentType, ...)`，每轮一次性（用标志位 `roundCounted`，进入新一轮时重置）。
2. **清理量**：`confirmDelete()` 中 `repo.deleteMediaItems(items)` **返回成功后**，按实际删除结果调用 `statsStore.recordDeletion(currentType, ..., bytes, count)`，累加该类型的字节与数量。

> 现有 `confirmDelete()` 在 `ViewerViewModel.kt:127-133`；删除返回 `Result<Int>`（实际删除数）。需以实际删除的项为准累加，而非 `pendingDeletes` 的全集。一轮只处理一种类型，故无需在一次事件里同时累加照片与视频。

## 6. UI（阶段一）

技术栈：Compose Multiplatform + 自定义 `AppRouter`（沿用现有）。**5 个主题全部适配、跟随当前主题切换**（与其它页一致）；视觉细节（配色/圆角/阴影/字体/图标描边）一律读各主题 `ThemeTokens`。

### 6.1 清理成果页 `StatsScreen`（新增 `Route.Stats`）

单屏不滚动，自上而下五段（定稿见 `stats-mockup.html` 及截图 `stats-5themes-h.png`）：

1. **成果总览大卡（hero）**：
   - 大数字「累计已清理 12.6 GB」
   - 副标「共 N 个文件 · 完成 N 轮清理」
   - 卡片底部一道极淡分隔线 + **一句语录**（情感注脚，见 §7）。语录归属此卡，不独立漂浮。
2. **分类构成**：照片、视频各一组，每组 = `图标 + 名称 + 占比% + (大小·数量·轮次)三元组 + 占比条`。占比按字节。
3. **设备存储现状**：`StatFs` 读真实可用/已用 + 进度条。
4. **离线信任声明**：盾牌图标 +「全程离线 · 清理记录仅存本机，不上传」。
5. （阶段二/三的徽章/趋势/月度回顾等暂不渲染，留结构。）

**图标**：用 Lucide（lucide.dev，MIT）图标，**SVG path 内联**喂给现有 `icons/SvgPathParser`，不引网络依赖。阶段一用到：`chevron-left`(返回)、`image`(照片)、`film`(视频)、`hard-drive`(存储)、`shield-check`(离线)、`bar-chart-3`(首页入口)。描边粗细跟随各主题 `iconStrokeWidth` token（极简 1.4 / 几何 2.5 / 暖 1.8 / 活泼 2.2 / 杂志 1.1）。**不用任何 emoji**。语录前不加图标（避免 `sparkles` 类星形被误认为 emoji）。

### 6.2 首页入口（纯图标 · 与设置并排）

在首页**设置图标的邻位**新增一个统计图标（Lucide `bar-chart-3`），点击 `router.navigate(Route.Stats)`。纯图标、不露数字、不占首页版面，让主功能（清理照片/视频）保持主角。

各主题按自身首页布局把图标放在设置图标旁（定稿见 `home-entry-mockup.html` / `home-entry-clean.png`）：
- Minimal / Editorial：右上顶栏，设置图标左侧。
- Warm：底部图标区，设置图标左侧并排。
- Geometric / Playful：按各自首页的设置图标位置同侧放置。

描边随各主题 `iconStrokeWidth`；与设置图标同尺寸同色。改动落在 5 个 `*HomeLayout.kt` + `HomeScreenState`（加 `onOpenStats` 回调）+ `HomeScreen.kt`（接 `Route.Stats` 导航）。

### 6.3 结果页升级 `ResultScreen`

- 本轮完成后展示"**本次清理 X** + **累计已清理 Y**"。
- **数字滚动动画**（Compose `animate*AsState`），最低成本最高回报的爽点。
- 附一句语录（同 §7 体系）。

### 6.4 文案

沿用项目现状（硬编码中文，无 i18n）。统一用"已清理"口径。**中文不使用斜体**（伪斜体发虚、廉价）。

## 7. 语录系统（治愈温柔 · 动态轮换）

> 替代原"拟物化换算"——对照片清理 App，把 GB 换算成"≈N 张照片"是同义反复（用户清的就是照片），故去掉。改为情感语录。

- **调性**：治愈温柔。
- **机制**：内置文案池，commonMain **纯函数** `pickQuote(stats, today, seed): String` 按情境选句，优先级：
  1. **里程碑**（累计破 1G/10G/50G 等阈值）
  2. **首次清理**（`totalRounds` 极少）
  3. **连续清理**（`lastCleanupAt` 落在今天/昨天，体现坚持）
  4. **大额清理**（最近一次清理量较大）
  5. **日常随机池**
- **种子**：commonMain 不可用 `Math.random()`/`Date.now()`，随机由调用方传入种子（如 `lastCleanupAt` 或进入次数），保证纯函数可测。
- **情境数据全部来自已有统计**，不额外采集。
- 文案示例（治愈温柔）：
  - 首次：「第一次清理完成，相册轻盈了一点点。」
  - 里程碑：「已经清出 10 GB，给自己一个温柔的赞。」
  - 连续：「又见面了，坚持整理的样子真好。」
  - 日常：「干净的相册，像刚收拾好的房间。」「每一次清理，都是对自己温柔一点。」「少一点冗余，多一点清爽。」「整理好的不只是手机，还有心情。」

文案池可在实现阶段补充扩展，建议每个情境 ≥3 句。

## 8. 测试策略

- `StatsAggregator`（commonMain 纯函数）单测：累计合并、按天聚合、首次/最近时间、照片/视频拆分、合计=相加、轮次与清理量解耦（全保留轮次：轮次+1 量+0）。
- `pickQuote`（commonMain 纯函数）单测：各情境（里程碑/首次/连续/大额/日常）选句正确、优先级正确、同种子结果稳定。
- 参考现有 `update/src/commonTest/.../UpdateCheckerTest.kt` 范式。
- 按 CLAUDE.md 测试纪律：新增 Maestro E2E 流覆盖"进入统计页 / 清理后数字增长"。
- UI 以编译通过 + Maestro 冒烟为准（不强制 UI 单测）。

## 9. 分阶段路线图

- **阶段一（本次实现）**：数据模型（按类型拆分）+ `StatsStore`/`StatsAggregator` + 语录 `pickQuote` + 埋点（轮次 + 清理量，按类型）+ `StatsScreen`（成果总览/分类构成/语录/存储现状/离线声明，5 主题适配 + Lucide 内联图标）+ 首页入口 + 结果页升级（数字动画 + 语录）。`daily` 数据开始采集但不展示。
- **阶段二（B）**：里程碑徽章（1G/10G/100G、1000 张等）、连续清理天数(streak)、照片/视频构成图。
- **阶段三（C）**：月度回顾卡、可分享成果图、本地备份导出（解决纯本地换机归零）。

## 10. 明确不做（YAGNI，阶段一）

- 不引入数据库/Room（SharedPreferences + JSON 足够）。
- 不做云同步/账号。
- 不在阶段一渲染徽章/趋势/月度回顾/分享/备份（仅留结构与数据采集）。
- 不申请任何新权限。
