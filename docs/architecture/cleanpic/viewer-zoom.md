# 全屏查看缩放（双击 + 双指捏合）技术设计

> 对应 User Story：US-CP-20 放大查看照片与视频
> 状态：设计已确认，待实现

## 目标

在全屏查看时，让**照片和视频**支持：
- 双击放大 / 再双击还原
- 双指捏合自由缩放（1×–5×）
- 放大后单指拖动平移（带边界约束 + 惯性）
- 切换媒体或关闭全屏时自动复位

## 选型结论

采用成熟、内容无关的 Compose Multiplatform 缩放库 **usuiat/Zoomable**，
而非自研，理由：
- 它的 `Modifier.zoomable` 作用于**任意 Composable**，照片与视频可共用同一套手势/变换 → 行为天然一致
- 是 **Compose Multiplatform** 库（Android/iOS/Desktop），契合本项目 KMP 架构
- 自带双击、捏合、拖动平移、惯性、边界约束，手感对齐主流图库
- 体积极小（纯 Kotlin、无 native 库，仅依赖已打包的 Compose），release 包增量约几十 KB

### 版本

`net.engawapg.lib:zoomable:2.7.0` —— 该版本编译于 **Compose 1.7.3**，
与本项目（Compose Multiplatform 1.7.3 / Kotlin 2.1.21）精确匹配，零版本冲突。

> 注意：2.8.x 起基于 Compose 1.9，2.12 基于 1.11，**不可**直接用于 1.7.3。

## 架构与接入点

缩放逻辑集中在**唯一**组件 `ui/viewer/FullscreenViewer.kt`：
它是"轮播/卡片点击进全屏"叠层与"全屏上下滑"模式**共用**的无状态全屏组件，
因此在此一处接入即可让**三种交互模式的全屏视图**全部获得缩放能力。

缩略图（轮播主卡片 / 卡片堆叠）**不**加缩放，符合主流图库习惯。

### 关键改动

1. **FullscreenContent 包裹 zoomable**（`FullscreenViewer.kt`）
   - 在展示媒体的 `Box` 上加 `Modifier.zoomable(zoomState)`
   - 照片 `MediaImage` 与视频 `VideoPlayerView` 都放在该 Box 内 → 共用一套缩放
   - `val zoomState = rememberZoomState(maxScale = 5f)`，并以**当前 item 为 key** 重建，
     使切换上一个/下一个、关闭全屏时自动复位到 1×
   - 双击行为用库默认（点击点 1× ↔ ~2.5× 切换）；关闭单指缩放（`enableOneFingerZoom = false`）

2. **视频渲染改用 TextureView**（`androidMain/ui/media/AndroidVideoPlayerView.kt`）
   - Media3 `PlayerView` 默认用 SurfaceView，是独立窗口层，
     `graphicsLayer` 变换无法平滑作用其上（裁切/不跟手）
   - 改为通过带 `app:surface_type="texture_view"` 的 XML 布局 inflate `PlayerView`，
     使视频画面随缩放/平移平滑变换
   - 仅 Android 侧改动，零新增依赖

## 手势冲突分析

经核查，全屏路径（`FullscreenViewer` / `FullscreenMode`）**无任何滑动/分页手势**，
媒体切换由删除/保留/撤销**按钮**驱动，内容区也无"点击关闭"手势。因此：
- 库的"双击"由其接管，与既有单击无冲突
- 库的"放大后单指拖动平移"与切换无冲突（切换是按钮）
- 顶部返回/退出、右侧删/留/撤销按钮在叠层之上，行为不变

## 跨平台

- `Modifier.zoomable` 在 commonMain 使用即可，多平台编译通过
- 视频 TextureView 适配仅 Android（iOS/HarmonyOS 视频视图各自独立，本期不展开；
  当前仅 Android 完整实现）

## 测试策略

- **单元测试**：缩放为纯 UI 手势、不进入 ViewModel，无新增 ViewModel 单测
- **E2E（Maestro）**：新增 `fullscreen-zoom` 流——进全屏 → `doubleTapOn` 放大 → 截图
  → 再 `doubleTapOn` 还原 → 截图。
  **限制（诚实记录）**：Maestro 不支持真双指捏合手势，**捏合缩放只能真机手动验证**（截图留证）
- **手动验证**：真机分别对照片、视频做双击与捏合，确认缩放、平移、播放、复位均正常

## YAGNI / 不做

- 不做旋转手势
- 不做缩略图缩放
- 不做单击隐藏/显示信息栏（如需，后续单独迭代）
