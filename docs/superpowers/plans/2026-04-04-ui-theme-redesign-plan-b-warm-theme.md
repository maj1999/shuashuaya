# UI 主题重设计 — Plan B：温暖手工感主题端到端

> **执行者须知：** 必须使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务执行本计划。步骤使用 `- [ ]` 语法跟踪进度。

**目标：** 用温暖手工感（warm）主题端到端实现所有页面的布局分发架构，替换所有 emoji 为矢量图标，验证架构可行性。

**架构：** 每个 Screen Composable 提取业务逻辑到 State 对象，然后 `when (theme.layoutId)` 分发到布局 Composable。当前只实现 WARM 布局，其余 4 个 layoutId 走相同的 WARM 布局（Plan C 实现差异化）。所有 emoji 替换为 AppIcons 矢量图标渲染。

**技术栈：** Kotlin 2.1.21、Compose Multiplatform 1.7.3、Compose Canvas

**设计 Spec：** `docs/superpowers/specs/2026-04-04-ui-theme-redesign-design.md`

**依赖：** Plan A（已完成：ThemeTokens 扩展、WarmTheme、AppIcons、共享 State 接口）
**阻塞：** Plan C（剩余 4 主题的差异化布局）

---

## 核心模式：IconPainter

所有任务共用的图标渲染 Composable，需要在任务 1 中首先创建：

```kotlin
// shared/src/commonMain/kotlin/com/cleanpic/icons/IconPainter.kt
package com.cleanpic.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.cleanpic.theme.IconStrokeCap
import com.cleanpic.theme.ThemeTokens

/**
 * 将 IconDef 渲染为 Compose Canvas。
 * 使用简化的 path 解析，支持 M/L/H/V/Z/A/C 等 SVG 命令。
 */
@Composable
fun IconPainter(
    name: String,
    theme: ThemeTokens,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    colorOverride: Long? = null
) {
    val icon = AppIcons.get(name, theme)
    val color = Color(colorOverride ?: icon.strokeColor)
    val cap = when (icon.strokeCap) {
        IconStrokeCap.BUTT -> StrokeCap.Butt
        IconStrokeCap.ROUND -> StrokeCap.Round
        IconStrokeCap.SQUARE -> StrokeCap.Square
    }

    Canvas(modifier = modifier.size(size)) {
        val path = parseSvgPath(icon.pathData)
        val scaleX = this.size.width / icon.viewportWidth
        val scaleY = this.size.height / icon.viewportHeight
        val matrix = Matrix().apply { scale(scaleX, scaleY) }
        path.transform(matrix)
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = icon.strokeWidth * scaleX,
                cap = cap,
                join = StrokeJoin.Round
            )
        )
    }
}
```

`parseSvgPath` 是一个辅助函数，将 SVG path data 字符串转为 Compose `Path` 对象。需要实现 M、L、H、V、C、A、Z 等基础命令（大写绝对 + 小写相对）。这是本 Plan 最复杂的部分。

---

## 文件清单

| 操作 | 文件 | 职责 |
|------|------|------|
| 新建 | `shared/.../icons/IconPainter.kt` | Canvas 图标渲染 Composable + SVG path 解析 |
| 新建 | `shared/.../icons/SvgPathParser.kt` | SVG path data → Compose Path 解析器 |
| 修改 | `shared/.../ui/home/HomeScreen.kt` | 提取业务逻辑到 State，添加 when 分发 |
| 新建 | `shared/.../ui/home/WarmHomeLayout.kt` | 温暖主题首页布局（列表卡片堆叠） |
| 修改 | `shared/.../ui/splash/SplashScreen.kt` | 提取逻辑到 State，添加 when 分发 |
| 新建 | `shared/.../ui/splash/WarmSplashLayout.kt` | 温暖主题闪屏（圆角卡片浮出动画） |
| 修改 | `shared/.../ui/result/ResultScreen.kt` | 提取业务逻辑到 State，添加 when 分发 |
| 新建 | `shared/.../ui/result/WarmResultLayout.kt` | 温暖主题结果页（白色阴影卡片） |
| 修改 | `shared/.../ui/settings/SettingsScreen.kt` | 提取业务逻辑到 State，添加 when 分发 |
| 新建 | `shared/.../ui/settings/WarmSettingsLayout.kt` | 温暖主题设置页（圆角卡片分区） |
| 修改 | `shared/.../ui/common/EmptyStateScreen.kt` | emoji → IconPainter |
| 修改 | `shared/.../ui/common/PermissionBanner.kt` | emoji → IconPainter |
| 修改 | `shared/.../ui/viewer/ViewerScreen.kt` | ProgressHeader 中 emoji → IconPainter |
| 修改 | `shared/.../ui/viewer/CarouselMode.kt` | 所有 emoji → IconPainter |
| 修改 | `shared/.../ui/viewer/SwipeCardMode.kt` | 所有 emoji → IconPainter |
| 修改 | `shared/.../ui/viewer/FullscreenMode.kt` | 所有 emoji → IconPainter |

---

### 任务 1：创建 SVG Path 解析器和 IconPainter

**文件：**
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/icons/SvgPathParser.kt`
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/icons/IconPainter.kt`
- 新建：`shared/src/commonTest/kotlin/com/cleanpic/icons/SvgPathParserTest.kt`

**目标：** 实现一个可以将 AppIcons 中的 SVG path data 字符串转为 Compose `Path` 的解析器，以及一个 `IconPainter` Composable。

**SvgPathParser 需要支持的 SVG 命令：**
分析 AppIcons 中所有 13 个 path data，用到的命令有：
- `M`/`m` — moveTo
- `L`/`l` — lineTo
- `H`/`h` — 水平 lineTo
- `V`/`v` — 垂直 lineTo
- `C`/`c` — 三次贝塞尔曲线（cubicTo）
- `A`/`a` — 椭圆弧（arcTo）
- `Z`/`z` — 闭合路径

**测试用例：**
- 简单路径 "M0 0L10 10" 应生成非空 Path
- 带闭合的路径 "M0 0L10 0L10 10Z" 应正确闭合
- AppIcons 中所有 13 个 path 都能无异常解析

- [ ] 步骤 1：写 SvgPathParserTest 测试
- [ ] 步骤 2：运行测试确认失败
- [ ] 步骤 3：实现 SvgPathParser（`parseSvgPath(data: String): Path`）
- [ ] 步骤 4：实现 IconPainter Composable
- [ ] 步骤 5：运行测试确认通过
- [ ] 步骤 6：验证构建
- [ ] 步骤 7：提交

**注意：** SvgPathParser 是纯函数，可以完整单元测试。IconPainter 是 Composable 无法单元测试，通过构建验证即可。

---

### 任务 2：改造首页（HomeScreen → State + WarmHomeLayout）

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreen.kt`
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/ui/home/WarmHomeLayout.kt`

**目标：**

1. HomeScreen.kt 保留业务逻辑（权限检查、对话框管理、导航），构造 `HomeScreenState`，然后 `when (theme.layoutId)` 分发。当前所有 layoutId 都走 `WarmHomeLayout`（Plan C 添加其余 4 个）。

2. WarmHomeLayout 实现温暖手工感的首页布局：
   - 衬线标题 "刷刷鸭" + 斜体副标题
   - 两个列表卡片（白色圆角 + 阴影）：照片和视频，各带图标+描述+箭头
   - 底部设置齿轮图标
   - 所有图标使用 IconPainter，不用 emoji

3. PermissionBanner 和 PermissionDeniedDialog/PermissionPermanentDialog 逻辑保留在 HomeScreen 中，通过 State 传入布局。

**关键改动模式：**
```kotlin
// HomeScreen.kt 改造后的核心结构
@Composable
fun HomeScreen(router: AppRouter, theme: ThemeTokens, viewerViewModel: ViewerViewModel) {
    // ... 保留所有业务逻辑 ...
    val state = HomeScreenState(
        theme = theme,
        isLimitedAccess = homeViewModel.isLimitedAccess,
        onStartPhoto = { launchViewer(MediaType.PHOTO) },
        onStartVideo = { launchViewer(MediaType.VIDEO) },
        onOpenSettings = { router.navigate(Route.Settings) },
        onRequestPermission = { scope.launch { homeViewModel.requestPermission() } },
        onShowDeniedDialog = { showDeniedDialog = true },
        onShowPermanentDialog = { showPermanentDialog = true }
    )

    when (theme.layoutId) {
        // 当前所有 layoutId 都用 Warm 布局，Plan C 差异化
        else -> WarmHomeLayout(state)
    }

    // 对话框保留在这里（不传入布局）
    if (showDeniedDialog) { ... }
    if (showPermanentDialog) { ... }
}
```

- [ ] 步骤 1：创建 WarmHomeLayout.kt（温暖风格布局，使用 IconPainter）
- [ ] 步骤 2：改造 HomeScreen.kt（提取 State，添加 when 分发）
- [ ] 步骤 3：验证构建
- [ ] 步骤 4：提交

---

### 任务 3：改造闪屏（SplashScreen → State + WarmSplashLayout）

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreen.kt`
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/ui/splash/WarmSplashLayout.kt`

**目标：**

1. SplashScreen.kt 保留 `LaunchedEffect` 延迟逻辑，构造 `SplashScreenState`，分发到布局。
2. WarmSplashLayout 实现温暖主题闪屏：
   - 背景色 #FFF8F0
   - 中心：圆角白色卡片（阴影），内含相机图标（Canvas 绘制，非 emoji）
   - 衬线字体 "刷刷鸭" + 斜体副标题
   - 入场动画：scale 0.8→1 + alpha 0→1

- [ ] 步骤 1：创建 WarmSplashLayout.kt
- [ ] 步骤 2：改造 SplashScreen.kt
- [ ] 步骤 3：验证构建
- [ ] 步骤 4：提交

---

### 任务 4：改造公共组件（EmptyStateScreen + PermissionBanner）

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/common/EmptyStateScreen.kt`
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/common/PermissionBanner.kt`

**目标：** 将 emoji 替换为 IconPainter。这些组件不按主题拆分布局，而是通过 theme 参数化样式。

EmptyStateScreen 改动：
- `"📷"` → `IconPainter("photo", theme, size = 72.dp)`
- `"🎬"` → `IconPainter("video", theme, size = 72.dp)`

PermissionBanner 改动：
- `"⚠️"` → `IconPainter("warning", theme, size = 16.dp)`

- [ ] 步骤 1：修改 EmptyStateScreen.kt
- [ ] 步骤 2：修改 PermissionBanner.kt
- [ ] 步骤 3：验证构建
- [ ] 步骤 4：提交

---

### 任务 5：改造 Viewer 页面（emoji → IconPainter）

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/ViewerScreen.kt`
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/CarouselMode.kt`
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/SwipeCardMode.kt`
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/FullscreenMode.kt`

**目标：** Viewer 不按主题拆分布局文件，而是在现有文件内替换所有 emoji 为 IconPainter。

**替换清单（全部文件）：**

| 当前 emoji | 位置 | 替换为 |
|-----------|------|--------|
| `"← 退出"` | ViewerScreen ProgressHeader | `IconPainter("back", theme, size = 14.dp)` + `Text("退出")` |
| `"🗑️"` | CarouselMode 删除按钮 | `IconPainter("delete", theme, size = 24.dp, colorOverride = theme.colorDanger)` |
| `"✓"` | CarouselMode 保留按钮 | `IconPainter("keep", theme, size = 28.dp, colorOverride = theme.colorSuccess)` |
| `"▶"` | CarouselMode 播放按钮 | `IconPainter("play", theme, size = 24.dp)` |
| `"🔇"`/`"🔊"` | CarouselMode 静音切换 | `IconPainter(if (muted) "mute" else "unmute", theme, size = 18.dp)` |
| `"🗑️"` | FullscreenMode 删除按钮 | 同上 |
| `"✓"` | FullscreenMode 保留按钮 | 同上 |
| `"🔇"`/`"🔊"` | FullscreenMode 静音切换 | 同上 |
| `"←"` | FullscreenMode 返回 | `IconPainter("back", theme, size = 15.dp)` |
| SwipeCardMode 中的类似 emoji | | 同样替换 |

**注意：** 这些文件较大（300/216/235 行），修改时只替换 emoji 相关的 `Text` composable，不改动交互逻辑（拖拽、滑动、手势）。

- [ ] 步骤 1：修改 ViewerScreen.kt（ProgressHeader 中的 emoji）
- [ ] 步骤 2：修改 CarouselMode.kt（所有 emoji）
- [ ] 步骤 3：修改 SwipeCardMode.kt（所有 emoji）
- [ ] 步骤 4：修改 FullscreenMode.kt（所有 emoji）
- [ ] 步骤 5：验证构建
- [ ] 步骤 6：提交

---

### 任务 6：改造结果页（ResultScreen → State + WarmResultLayout）

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreen.kt`
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/ui/result/WarmResultLayout.kt`

**目标：**

1. ResultScreen.kt 保留业务逻辑（删除确认、状态管理），构造 `ResultScreenState`，分发到布局。
2. WarmResultLayout 实现温暖主题结果页：
   - 圆形完成图标（IconPainter "keep"）替换 🎉
   - 白色阴影统计卡片（删除/保留/释放）
   - 待删除预览（圆角缩略图）
   - 全圆角按钮（确认删除/再来一轮/返回首页）
   - 🔄 → IconPainter "refresh"，🏠 → IconPainter "home"

- [ ] 步骤 1：创建 WarmResultLayout.kt
- [ ] 步骤 2：改造 ResultScreen.kt
- [ ] 步骤 3：验证构建
- [ ] 步骤 4：提交

---

### 任务 7：改造设置页（SettingsScreen → State + WarmSettingsLayout）

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreen.kt`
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/ui/settings/WarmSettingsLayout.kt`

**目标：**

1. SettingsScreen.kt 保留 ViewModel 交互，构造 `SettingsScreenState`，分发到布局。
2. WarmSettingsLayout 实现温暖主题设置页：
   - 返回按钮用 IconPainter "back" 替换 "←"
   - 主题选择卡片（颜色预览 + 名称），选中态用主题色边框
   - 交互模式用 IconPainter 替换 🖼️🃏📱
   - 每轮数量用圆角 Chip
   - 所有卡片白色圆角 + 柔和阴影

- [ ] 步骤 1：创建 WarmSettingsLayout.kt
- [ ] 步骤 2：改造 SettingsScreen.kt
- [ ] 步骤 3：验证构建
- [ ] 步骤 4：提交

---

### 任务 8：全量验证 + emoji 扫描

- [ ] 步骤 1：运行 `./gradlew :shared:testDebugUnitTest`（全部通过）
- [ ] 步骤 2：运行 `scripts/build-android.sh`（构建成功）
- [ ] 步骤 3：在代码中搜索残留 emoji（`grep -rn '[📷🎬⚙️🗑️🎉✨🖼️🃏📱🔄🏠⚠️🔇🔊▶✓✕←]' shared/src/`），确认无残留
- [ ] 步骤 4：如有残留，修复并提交

---

## Plan B 完成检查清单

- [ ] IconPainter + SvgPathParser 可用，所有 13 个图标可渲染
- [ ] HomeScreen 使用 State + when 分发，WarmHomeLayout 实现
- [ ] SplashScreen 使用 State + when 分发，WarmSplashLayout 实现
- [ ] ResultScreen 使用 State + when 分发，WarmResultLayout 实现
- [ ] SettingsScreen 使用 State + when 分发，WarmSettingsLayout 实现
- [ ] EmptyStateScreen、PermissionBanner 中 emoji 已替换
- [ ] Viewer 所有模式中 emoji 已替换
- [ ] 代码中无 emoji 残留
- [ ] 所有单元测试通过
- [ ] 构建成功
