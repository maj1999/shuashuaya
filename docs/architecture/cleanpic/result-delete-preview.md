# 待删除项全屏预览 — 复用缩放/播放内核 + HorizontalPager

|文档状态| 初稿 | 2026-06-06 |

> 父文档: [overview.md](overview.md) ｜ 关联 US: US-CP-24 ｜ 设计稿: [../../superpowers/specs/2026-06-06-result-delete-preview-design.md](../../superpowers/specs/2026-06-06-result-delete-preview-design.md)

## 1. 目标

结果页待确认态（`ResultPhase.CONFIRM`）的待删除缩略图列表，点击某项 → 全屏放大查看（照片缩放 / 视频播放）+ 左右滑切换 + 全屏内取消删除。仅待确认态生效。

## 2. 复用现有内核

`FullscreenViewer`（US-CP-18/20）内部 `FullscreenContent` 已实现照片缩放（`net.engawapg.lib:zoomable`，maxScale 5f）+ 视频播放（`VideoPlayerView` → Media3 ExoPlayer 自动播放/循环/静音）。本功能复用，不重造。

**抽取共用组件**：

```kotlin
@Composable
fun ZoomableMediaContent(media: MediaItem, isMuted: Boolean, modifier: Modifier = Modifier)
```

- 抽自 `FullscreenViewer.FullscreenContent`；`FullscreenViewer` 改为调用它（纯重构，浏览页行为不变）。

## 3. 新增 DeletePreviewOverlay（主题无关，一份）

```kotlin
@Composable
fun DeletePreviewOverlay(
    items: List<MediaItem>,
    startIndex: Int,
    theme: ThemeTokens,
    onCancelDelete: (MediaItem) -> Unit,
    onBack: () -> Unit,
)
```

- `HorizontalPager` 在 `items` 间左右滑；每页 `ZoomableMediaContent`。
- 顶部「返回」+ 进度 current/total；视频页静音切换；底部「取消删除」作用于当前页。
- 缩放 vs 翻页：照片放大后横向拖动为平移，1× 时横滑翻页（`zoomable` 库在 Pager 中协同）。

### 3.1 「取消删除」按钮视觉

全屏照片之上的次要操作，需兼顾「主题个性」与「任意背景下可读性」。最终方案：

- **图标 `undo`（撤销箭头）**，非 `keep`(✓)：避免删除语境下 ✓ 被误读为"确认删除"；语义为"将此项撤回、不再删除"，与浏览页「撤销上一步」图标一致。
- **主题色半透明圆底座**：`Color(theme.colorPrimary).copy(alpha = 0.65f)` 的 56dp 圆形基座。各主题底座色不同（warm 棕 / minimal 深灰 / geometric 霓虹红 / playful 蓝紫 / editorial 近黑），保留主题差异，又在任意照片上提供稳定可点区域。
- **图标取 `theme.colorSuccess` 强制不透明**（`or 0xFF000000L`）：避免 Playful 等半透明 success 色在底座上偏淡。
- **底部渐变遮罩**：`Transparent → Black α0.55` 垂直渐变，压暗背景，保证按钮与「取消删除」白字在亮照片上可读。

> 取舍：早期曾直接复用浏览页 `ThemedActionButton`，但其 TEXT(editorial)/OUTLINED(minimal) 风格无背景、浮在照片上几乎不可见；故改用「统一圆底座 + 主题色」，可见性与主题个性兼得。

## 4. 接线

| 组件 | 改动 |
|------|------|
| `ZoomableMediaContent.kt`（新增，`ui/viewer`） | 抽取缩放/播放内核 |
| `FullscreenViewer.kt` | 改调用 `ZoomableMediaContent`（重构） |
| `DeletePreviewOverlay.kt`（新增，`ui/result`） | Pager + 取消删除 + 静音 |
| `ResultScreenState.kt` | 新增 `onPreviewItem: (MediaItem) -> Unit` |
| `ResultScreen.kt` | 管理 `previewIndex` 状态、渲染 overlay、接线 |
| 5×`*ResultLayout.kt` 的 `*DeletePreviewItem` | 图片区加 `clickable { state.onPreviewItem(item) }` |

### 数据流

```
点击待删除缩略图 → state.onPreviewItem(item)
  → ResultScreen.previewIndex = pendingDeleteItems.indexOf(item)
  → DeletePreviewOverlay(items=pendingDeleteItems, startIndex=previewIndex)
       左右滑 → 切换页
       取消删除(当前) → state.onCancelItem(item)（移出列表）
            列表空 → onBack 关闭 overlay
       返回 → previewIndex = null
```

## 5. 边界

| 场景 | 行为 |
|------|------|
| 取消删除后列表减少 | pager 停相邻项；项数随 `pendingDeleteItems` 收缩 |
| 删空 | 自动关闭全屏，结果页回统计（无待删除项 → 趋向完成态） |
| 单项 | 不可横滑，仅查看 + 取消删除 |
| 视频切页 | 上一个 ExoPlayer 由 `VideoPlayerView` 的 `DisposableEffect(item.id)` 释放 |
| isDeleting / 完成态 | 不响应点击；完成态无待删除列表 |

## 6. 测试 / 版本

- 测试：[../../testing/scenarios/ep1-photo-cleanup.md](../../testing/scenarios/ep1-photo-cleanup.md) 追加 E-PRV-* 用例。
- 版本：新增用户可见功能 → MINOR，1.8.0 → 1.9.0。

## 7. 缺陷修复记录

- **Minimal 主题点缩略图误触发取消删除（已修复）**：根因为 `MinimalDeletePreviewItem` 把 `onCancel` 角标**嵌套在** `delete_thumb`(onPreview) 容器内部，`testTagsAsResourceId` 下点击 `delete_thumb` 被分派给内层 onCancel，项被移出列表而非进入预览。其余 4 主题均为「`onPreview` 在 `MediaImage` 叶子、`onCancel` 独立兄弟」的非嵌套结构，故正常。
  - 修复：Minimal 对齐其余主题——`testTag`+`onPreview` 移到 `MediaImage` 叶子，角标改为图片的平级兄弟（视觉不变）。
  - 回归测试：`maestro/flows/direct/delete-preview-minimal.yaml`（修复前 red、修复后 green）。
