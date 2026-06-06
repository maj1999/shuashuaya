# 待删除项全屏预览设计 — 点击放大 + 左右滑 + 视频播放

- 日期：2026-06-06
- 主题：在结果页"即将删除（待确认态）"列表里，点击待删除照片/视频放大全屏查看，支持左右滑切换与视频播放，并可在全屏内取消删除
- 状态：待评审（仅设计，未实现）

---

## 1. 需求

结果页待确认态（`ResultPhase.CONFIRM`）展示待删除缩略图列表。用户在确认删除前，常需看清细节（照片是否糊/闭眼、视频实际内容）再决定去留。当前缩略图本身不可点，只有「取消」小按钮。

**目标**：
- 点击待删除缩略图 → 全屏放大查看；
- 照片支持双击 / 双指捏合缩放；视频自动播放（可静音切换）；
- 全屏内**左右滑**在待删除项之间切换；
- 全屏内提供「取消删除」按钮，把当前项移出删除列表；
- 仅在待确认态生效（完成态无待删除列表）。

---

## 2. 方案：复用现有缩放/播放内核 + 轻量全屏预览层

项目已有 `FullscreenViewer`（US-CP-18/20），内部 `FullscreenContent` 实现了照片缩放（`net.engawapg.lib:zoomable`）+ 视频播放（Media3 ExoPlayer，`VideoPlayerView`）。本功能**复用该内核**，不重造。

### 2.1 抽取共用内核

把 `FullscreenViewer` 内 private 的 `FullscreenContent` 抽成公共组件：

```kotlin
@Composable
fun ZoomableMediaContent(media: MediaItem, isMuted: Boolean, modifier: Modifier = Modifier)
```

- 照片：`MediaImage` + `rememberZoomState` + `zoomable`（双击/捏合，maxScale 5f）。
- 视频：`VideoPlayerView(item = media, isMuted)`。
- `FullscreenViewer` 改为调用此组件，**浏览页行为不变**（纯重构，去重）。

### 2.2 新增结果页预览层 `DeletePreviewOverlay`（主题无关，一份）

```kotlin
@Composable
fun DeletePreviewOverlay(
    items: List<MediaItem>,        // 整个待删除列表
    startIndex: Int,               // 点中的那张
    theme: ThemeTokens,
    onCancelDelete: (MediaItem) -> Unit,
    onBack: () -> Unit,
)
```

- 用 **`HorizontalPager`** 在 `items` 间左右滑；每页渲染 `ZoomableMediaContent`。
- 顶部「返回」+ 进度（current/total）；视频页带静音切换按钮。
- 底部「取消删除」作用于**当前页**：调用 `onCancelDelete(当前 item)`。

### 2.3 接线

- `ResultScreenState` 新增回调 `onPreviewItem: (MediaItem) -> Unit`。
- 5 套主题的 `*DeletePreviewItem`（Warm/Minimal/Geometric/Playful/Editorial）：图片区域加 `.clickable { state.onPreviewItem(item) }`（「取消」小按钮保留）。
- `ResultScreen` 管理 `previewItem`/`previewIndex` 状态（仿 `ViewerScreen.showFullscreen`），非空时渲染 `DeletePreviewOverlay`，`startIndex` = 该项在 `pendingDeleteItems` 中的下标。

---

## 3. 交互细节与边界

| 点 | 处理 |
|----|------|
| **缩放 vs 翻页冲突** | 照片放大后横向拖动 = 平移；1× 时横滑 = 翻页。依赖 `zoomable` 库在 `HorizontalPager` 中的协同（缩放态拦截横向手势）。 |
| **取消删除后列表变化** | 移出当前项后 pager 停在相邻项；列表项数随之减少。 |
| **删空自动退出** | 列表为空 → 关闭全屏（结果页此时也无待删除项，自动回到统计/完成态）。 |
| **单项** | 列表只剩 1 项时不可横滑，仅查看 + 取消删除。 |
| **视频** | 进入即自动播放、循环、默认静音；切到非当前页的视频由 pager/Compose 释放上一个 ExoPlayer（沿用 `VideoPlayerView` 的 `DisposableEffect(item.id)`）。 |
| **作用域** | 仅 `ResultPhase.CONFIRM`；`isDeleting` 期间不响应点击。 |

---

## 4. 组件与文件

| 组件 | 位置 | 改动 |
|------|------|------|
| `ZoomableMediaContent`（新增） | `commonMain/ui/viewer/ZoomableMediaContent.kt` | 抽取自 `FullscreenViewer.FullscreenContent` |
| `FullscreenViewer` | `commonMain/ui/viewer/FullscreenViewer.kt` | 改为调用 `ZoomableMediaContent`（纯重构） |
| `DeletePreviewOverlay`（新增） | `commonMain/ui/result/DeletePreviewOverlay.kt` | HorizontalPager + 取消删除 + 静音 |
| `ResultScreenState` | `commonMain/ui/result/ResultScreenState.kt` | 新增 `onPreviewItem: (MediaItem)->Unit` |
| `ResultScreen` | `commonMain/ui/result/ResultScreen.kt` | 管理 previewIndex 状态 + 渲染 overlay + 接线 onPreviewItem |
| 5×`*DeletePreviewItem` | 5 套 `*ResultLayout.kt` | 图片区加 `clickable { onPreviewItem(item) }` |

---

## 5. 测试

### 单元
- `ResultScreenState` 新回调可被构造/调用（轻量；本功能逻辑主要在 UI 层）。
- 若引入"取消删除后 pager 索引/空列表退出"的纯逻辑助手，则为其补单测。

### E2E（Maestro）
- 标记删除多张 → 结果页点缩略图 → 断言全屏出现（返回按钮可见）。
- 全屏内左右滑切换（断言进度 current/total 变化或截图）。
- 视频项：断言可进入并存在静音按钮。
- 点「取消删除」→ 返回结果页，待删除列表项数减少。
- 删到空 → 全屏自动关闭，结果页回到统计。
- 补进 `testing/scenarios/ep1-photo-cleanup.md`（或新增 ep 段）。

---

## 6. 版本

新增用户可见功能 → MINOR：1.8.0 → **1.9.0**。实现合入后询问用户是否发布。

---

## 7. 关联 US（dev-workflow Step 1 落地）

新增 US-CP-24「待删除项全屏预览」，AC 覆盖：点击放大、缩放、视频播放、左右滑切换、取消删除、删空退出、仅待确认态生效。
