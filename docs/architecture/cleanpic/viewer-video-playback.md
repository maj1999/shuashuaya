# 全屏视频播放 — 静音统一 + 单播放器 + 拖拽进度条

|文档状态| 初稿 | 2026-06-07 |

> 父文档: [overview.md](overview.md) ｜ 关联 US: US-CP-18（全屏视频播放/声音）、US-CP-25（拖拽进度条）

## 1. 目标

全屏播放视频时：① 静音状态在浏览页与全屏间统一，放大/进全屏声音连续不中断；② 同一时刻仅一个播放器发声，杜绝"图标静音却仍有声音"的重复发声；③ 底部提供可拖拽进度条 seek 到任意位置。

## 2. 播放器与静音

### 2.1 单播放器（消除声音泄漏）

浏览页（`CarouselMode`/`SwipeCardMode`）与点击弹出的全屏叠层（`FullscreenViewer`）由 `ViewerScreen` 同框承载。早期 `showFullscreen=true` 打开叠层后，底层交互模式仍留在组合树继续播放——其 ExoPlayer 与叠层新建的 ExoPlayer 并存，底层未静音则持续发声、叠层默认静音，表现为"全屏图标静音但仍有声音"。

修复：`ViewerScreen` 在 `showFullscreen` 为真时**不组合底层交互模式**（`if (!showFullscreen) { when(mode) {...} }`），触发底层 `VideoPlayerView` 的 `DisposableEffect.onDispose` 释放其 ExoPlayer，保证同一时刻仅一个播放器存活。

### 2.2 静音状态上提共享（声音连续）

`isMuted` 由各页面局部 `remember` 上提到 `ViewerScreen` 统一持有，分发给 `CarouselMode`/`SwipeCardMode`/`FullscreenMode` 与全屏叠层 `FullscreenViewer`（均改为接收 `isMuted`/`onToggleMute` 入参）。用户在浏览页取消静音后进入全屏，新播放器继承"未静音"状态、声音连续；任一处切换静音同步另一处。配合 2.1 的单播放器，不会双声叠加。

## 3. 拖拽进度条

### 3.1 进度桥 VideoControl

进度条放在全屏**底部面板**（Compose 层、信息条上方），不放在视频画面上——画面外层包了 `zoomable`（缩放/翻页手势），进度条若叠在画面上拖拽会被手势抢走。底层仍用 ExoPlayer 的 `seekTo` 做真实跳转，保证精度/缓冲正确。

平台播放器（Android ExoPlayer）与 Compose UI 通过 `VideoControl`（`commonMain`）桥接：

```kotlin
class VideoControl {
    var positionMs by mutableStateOf(0L) internal set  // 播放器轮询上报
    var durationMs by mutableStateOf(0L) internal set
    var seek: (Long) -> Unit internal set               // 播放器接管
}
```

- `VideoPlayerView` 增 `control: VideoControl?` 入参；`AndroidVideoPlayerView` 在 `control != null` 时接管 `seek` 并每 250ms 轮询上报 `currentPosition`/`duration`；为 null（轮播小卡等无需进度条）时不轮询、零开销。
- iOS 占位实现忽略 `control`（视频播放本身亦待实现）。

### 3.2 UI VideoScrubber

`FullscreenViewer` 为视频 `remember` 一个 `VideoControl`，经 `ZoomableMediaContent` 传给播放器，并在底部栏渲染 `VideoScrubber`（Material3 `Slider`，主题色 `colorPrimary`）：拖拽中显示本地 scrub 值与时间，松手调用 `control.seek`。仅视频、仅全屏显示。

## 4. 接线

| 组件 | 改动 |
|------|------|
| `ViewerScreen.kt` | `showFullscreen` 时不组合底层模式；上提共享 `isMuted` 分发四处 |
| `CarouselMode/SwipeCardMode/FullscreenMode/FullscreenViewer` | 改为接收 `isMuted`/`onToggleMute` 入参，移除局部 state |
| `VideoControl.kt`（新增，`media`） | 进度桥（position/duration + seek） |
| `VideoPlayerView`（expect/actual） | 增 `control` 入参；Android 轮询上报 + 接管 seek |
| `ZoomableMediaContent.kt` | 透传 `control` |
| `FullscreenViewer.kt` | 创建 `VideoControl`、底部栏渲染 `VideoScrubber` |

## 5. 边界

| 场景 | 行为 |
|------|------|
| 照片 | 不创建 VideoControl、不显示进度条 |
| 轮播小卡视频 | 不传 control → 无进度条、无轮询 |
| duration 未就绪 | ExoPlayer 返回 TIME_UNSET，归一为 0；Slider 禁用直至就绪 |
| 进全屏瞬间 | 底层播放器在叠层播放器 Init 后随即 Release（logcat 实证），单播放器存活 |

## 6. 测试 / 版本

- 测试：[../../testing/scenarios/ep2-video-cleanup.md](../../testing/scenarios/ep2-video-cleanup.md)（进度条 seek + 单播放器/静音一致）；E2E `maestro/flows/direct/video/fullscreen-video-scrubber.yaml`。
- 版本：声音一致/连续修复 1.9.0→1.9.1→1.9.2；进度条 1.9.2→1.10.0（MINOR）。

## 7. 缺陷修复记录

- **图标静音却仍有声音（已修复，1.9.1）**：根因为打开全屏叠层后底层播放器未释放、与叠层播放器并存发声。修复见 §2.1；logcat 实证进全屏时底层 ExoPlayer 在新播放器 Init 后随即 Release。
- **放大/进全屏后声音中断、静音状态各自为政（已修复，1.9.2）**：根因为 `isMuted` 各页面局部持有。修复见 §2.2（状态上提共享）。
- **全屏操作按钮竖排锚右侧、压住超宽媒体（已修复，1.9.1）**：按钮组由 `Alignment.CenterEnd` 竖排改为底部一行（对齐 `CarouselMode.ActionButtons`），见 [viewer-fullscreen-undo.md](viewer-fullscreen-undo.md) 同步。
