# 图片展示与视频预览/播放 — 设计文档

> 日期: 2026-03-29
> 范围: 仅 Android 平台，iOS/HarmonyOS 保持现有占位符

## 问题

当前所有浏览模式（轮播、滑卡、全屏）及结果页面中，图片和视频均以 emoji 占位符（🖼️/🎬）展示，无法看到实际内容。视频播放功能为空壳 stub。

## 方案

### 技术选型

| 功能 | 技术 | 理由 |
|------|------|------|
| 图片/视频缩略图 | Coil 3 (`coil-compose-android` + `coil-video`) | 支持 content:// URI、内置视频帧解码、内存/磁盘缓存 |
| 视频播放 | Media3 ExoPlayer (`media3-exoplayer` + `media3-ui`) | Google 官方维护、Compose AndroidView 集成成熟 |

### 架构：expect/actual 隔离

```
commonMain (expect)          androidMain (actual)           appleMain (actual)
─────────────────           ────────────────────           ──────────────────
MediaImage()         →      Coil AsyncImage                emoji 占位符
VideoPlayerView()    →      ExoPlayer + PlayerView         占位文案
```

### 1. MediaImage 组件

**commonMain — expect 声明：**

```kotlin
@Composable
expect fun MediaImage(
    item: MediaItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
)
```

**androidMain — actual 实现：**

- 根据 `MediaType` 构造对应的 content:// URI：
  - PHOTO → `ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)`
  - VIDEO → `ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)`
- 使用 Coil `AsyncImage` 加载，`coil-video` 自动为视频 URI 解码首帧
- 加载中显示 surface 色占位，失败显示 emoji fallback

**appleMain — actual 占位：**

保持现有 emoji 文本展示。

### 2. VideoPlayerView 组件

**commonMain — expect 声明：**

```kotlin
@Composable
expect fun VideoPlayerView(
    item: MediaItem,
    isMuted: Boolean,
    modifier: Modifier = Modifier
)
```

**androidMain — actual 实现：**

- `remember` 创建 ExoPlayer 实例
- `DisposableEffect` 管理生命周期（onDispose 释放）
- `LaunchedEffect(isMuted)` 同步静音状态
- 自动 prepare + play
- `AndroidView { PlayerView }` 渲染视频画面
- 隐藏默认控制器（`useController = false`），由外层 UI 控制

**appleMain — actual 占位：**

显示占位文案。

### 3. UI 替换规则

| 模式 | 图片 | 视频 |
|------|------|------|
| CarouselMode | MediaImage | MediaImage（缩略图）+ 播放图标 → 点击后 VideoPlayerView + 静音按钮 |
| SwipeCardMode | MediaImage | MediaImage（缩略图）+ 播放图标 → 点击后 VideoPlayerView + 静音按钮 |
| FullscreenMode | MediaImage | VideoPlayerView（自动静音播放）+ 静音按钮 |
| ResultScreen | MediaImage | MediaImage（缩略图） |

所有浏览模式中视频均支持播放和静音控制：
- **轮播/卡片模式**：默认显示缩略图 + 播放图标覆层，点击后内联播放（默认静音），卡片右下角显示静音切换按钮
- **全屏模式**：自动静音播放，底部显示静音切换按钮

### 4. 文件变更

| 操作 | 文件路径 |
|------|----------|
| 改 | `buildSrc/.../CleanPicBuildConfig.kt` — 添加版本常量 |
| 改 | `shared/build.gradle.kts` — 添加 Coil 3 + Media3 依赖 |
| 新建 | `commonMain/.../ui/media/MediaImage.kt` |
| 新建 | `androidMain/.../ui/media/AndroidMediaImage.kt` |
| 新建 | `appleMain/.../ui/media/IosMediaImage.kt` |
| 新建 | `commonMain/.../ui/media/VideoPlayerView.kt` |
| 新建 | `androidMain/.../ui/media/AndroidVideoPlayerView.kt` |
| 新建 | `appleMain/.../ui/media/IosVideoPlayerView.kt` |
| 改 | `CarouselMode.kt` — emoji → MediaImage |
| 改 | `SwipeCardMode.kt` — emoji → MediaImage |
| 改 | `FullscreenMode.kt` — emoji → MediaImage / VideoPlayerView |
| 改 | `ResultScreen.kt` — emoji → MediaImage |

### 5. 不变更

- `VideoPlayer` 接口 / `AndroidVideoPlayer` — 保持现状
- `MediaRepository` 接口 — 缩略图加载全部交给 Coil
- iOS / HarmonyOS 实现 — 保持 stub
