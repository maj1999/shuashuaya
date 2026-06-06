# 全屏沉浸模式 — 单击隐藏/显示顶栏与底栏

|文档状态| 初稿 | 2026-06-07 |

> 父文档: [overview.md](overview.md) ｜ 关联 US: US-CP-26

## 1. 目标

全屏查看照片/视频时，单击画面隐藏顶栏（返回+进度）与底栏（信息、删除/保留/撤销、视频进度条），淡入淡出，再次单击恢复。让用户专注看内容。单击切沉浸与双击放大互不冲突。

## 2. 手势分离（单击 vs 双击）

`ZoomableMediaContent` 的 `zoomable`（`net.engawapg.lib:zoomable` 2.7.0）原生区分单击/双击：

- 增 `onTap: (() -> Unit)?` 入参，接入 `zoomable(onTap = ...)` —— 单击（双击超时后）回调，用于切沉浸。
- 双击仍由库默认处理为放大/复位（见 [viewer-zoom.md](viewer-zoom.md)，US-CP-20），不受影响。

故"单击切沉浸"与"双击放大"由库的 tap/doubleTap 判定天然分离，无需自管延时。

## 3. 显隐状态

`FullscreenViewer` 持有 `chromeVisible`（默认 true）：

```kotlin
var chromeVisible by remember(item.media.id) { mutableStateOf(true) }
// onTap = { chromeVisible = !chromeVisible }
```

- `remember(item.media.id)`：切换媒体时复位为显示态（沉浸态不跨媒体保留，对应 US-CP-26 AC）。
- 顶栏 `TopBar` 与底栏 `BottomBar` 各自包进 `AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut())`，淡入淡出。
- 默认显示态不变，既有 E2E（`exit_button`/`keep_button`/`video_scrubber` 进全屏即可见）不受影响。

## 4. 接线

| 组件 | 改动 |
|------|------|
| `ZoomableMediaContent.kt` | 增 `onTap` 入参，接入 `zoomable(onTap=...)` |
| `FullscreenViewer.kt` | 增 `chromeVisible` 状态；`onTap` 取反；顶/底栏用 `AnimatedVisibility` 包裹 |

## 5. 边界

| 场景 | 行为 |
|------|------|
| 沉浸态下单击按钮区 | 按钮已隐藏，单击落在画面 → 恢复显示 |
| 双击 | 放大/还原，不切沉浸 |
| 切换上一/下一媒体 | `chromeVisible` 随 `media.id` 复位为显示 |
| 待删除预览 DeletePreviewOverlay | 未接 `onTap`，本功能暂不覆盖（可后续扩展） |

## 6. 测试 / 版本

- 测试：[../../testing/scenarios/ep5-browsing-enhancement.md](../../testing/scenarios/ep5-browsing-enhancement.md)；E2E `maestro/flows/direct/fullscreen-immersive-toggle.yaml`（显示→单击隐藏→单击恢复）。
- 版本：新增交互功能 → MINOR，1.10.0 → 1.11.0。
