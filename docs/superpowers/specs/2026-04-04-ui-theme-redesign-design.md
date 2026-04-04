---
title: UI 主题全面重设计
date: 2026-04-04
status: approved
---

# UI 主题全面重设计

## 背景

当前 app 的 UI 大量使用 emoji 字符作为图标（25+ 处），整体布局/配色/排版呈现"AI 模板化"感。需要全面重设计，让每个主题都有独立的视觉风格和布局结构，切换主题等于换一个 app。

## 设计决策摘要

| 决策项 | 结论 |
|--------|------|
| 主题数量 | 5 个，替换现有 5 个 |
| 改造范围 | 全部 6 个页面（闪屏、首页、Viewer、结果页、设置页、空状态页） |
| 布局差异 | 每个主题有独立布局（B 方案：切换主题 = 换 app） |
| Viewer 交互模式 | 保留 3 种模式（轮播/滑卡/全屏）用户自选，视觉风格跟随主题 |
| 闪屏 | 保留，每个主题独立设计闪屏动画 |
| 图标技术 | 混合方案：操作类用 ImageVector，装饰类用 Canvas API |

## 5 个主题定义

### 1. 克制极简（minimal）

- **视觉关键词：** 细线条、大留白、无圆角、低饱和度
- **参考：** Muji / Nothing Phone
- **配色：** 背景 #FAFAFA，主色 #333333，辅助 #999999，分割线 #E0E0E0
- **字体：** 系统无衬线，标题 letter-spacing 3-4px
- **圆角：** 0-2dp
- **动画：** 250ms，easeOutQuad，无按压动效

**各页面布局：**
- 闪屏：线条逐笔勾勒相机图标（stroke-dashoffset 动画），标题淡入
- 首页：分段切换（照片/视频 tab）+ 单"开始"按钮，标题左上角
- Viewer：方形描边按钮、1px 细线进度条、无装饰文件信息
- 结果页：列表式统计（非卡片），分割线分隔，方形缩略图
- 设置页：英文大写分区标签（THEME/MODE/COUNT），方形选择器

### 2. 大胆几何（geometric）

- **视觉关键词：** 深色底、粗线条、渐变色块、高对比
- **参考：** Spotify / Discord
- **配色：** 背景 #1A1A2E，主色 #E94560，辅助 #533483，渐变 #0F3460
- **字体：** 系统无衬线，font-weight 800-900，英文小标签
- **圆角：** 12-18dp
- **动画：** 300ms，easeInOutCubic，SCALE 按压

**各页面布局：**
- 闪屏：几何色块旋转拼合成相机轮廓，渐变线条扫过
- 首页：双卡片并排入口（照片/视频各一个渐变大卡片），标题左上角红色
- Viewer：渐变填充按钮、粗渐变进度条、高亮计数 badge
- 结果页：五边形完成图标、色块统计卡片（红/绿/紫）、渐变确认按钮
- 设置页：暗色卡片式选择器、渐变高亮选中态、SVG 模式指示器

### 3. 温暖手工感（warm）

- **视觉关键词：** 暖色调、大圆角、柔和阴影、衬线标题
- **参考：** Bear / Things
- **配色：** 背景 #FFF8F0，主色 #5D4037，辅助 #8D6E63/#A1887F，卡片 #FFFFFF
- **字体：** 标题用衬线（Georgia/serif），正文用系统无衬线
- **圆角：** 16-24dp
- **阴影：** 0 2-4dp 8-12dp rgba(93,64,55,0.08-0.12)
- **动画：** 280ms，easeOutCubic，SCALE 按压

**各页面布局：**
- 闪屏：圆角卡片从中心柔和浮出（scale 0.8→1），阴影渐展
- 首页：列表卡片堆叠（照片/视频各一行，带图标+描述+箭头），衬线标题+斜体副标题
- Viewer：白色圆形按钮带彩色阴影、大圆角卡片、柔和圆条进度
- 结果页：白色阴影统计卡片、圆形完成图标、全圆角按钮、鼓励语
- 设置页：白色圆角卡片分区、Chip 全圆角、暖色填充选中态

### 4. 活泼精致（playful）

- **视觉关键词：** 渐变背景、毛玻璃效果、浮动感、半透明
- **参考：** Arc Browser / Linear
- **配色：** 背景渐变 #667EEA→#764BA2，元素 rgba(255,255,255,0.1-0.2)，边框 rgba(255,255,255,0.15-0.25)
- **字体：** 系统无衬线，font-weight 600-700，白色
- **圆角：** 16-24dp
- **毛玻璃：** API 31+ 使用 `RenderEffect.createBlurEffect` 实现真实模糊；API 26-30 降级为半透明纯色（rgba(255,255,255,0.15)）+ 1px 白色边框模拟，视觉上可接受
- **动画：** 350ms，easeOutBounce/overshoot，BOUNCE 按压

**各页面布局：**
- 闪屏：毛玻璃卡片弹性弹出（overshoot easing），背景渐变微流动，光点依次亮起
- 首页：毛玻璃大卡片居中，内含照片/视频两个图标按钮，标题左上角
- Viewer：毛玻璃半透明按钮和卡片、半透明进度条、pill 形计数器
- 结果页：毛玻璃统计卡片、半透明删除按钮、渐变背景贯穿
- 设置页：毛玻璃卡片分区、白色半透明高亮选中态

### 5. 杂志排版（editorial）

- **视觉关键词：** 衬线字体、细分割线、米白底、字体层次感
- **参考：** The New Yorker / Monocle 杂志
- **配色：** 背景 #FFFFF5，主色 #1A1A1A，辅助 #999999，分割线 #E0DDD6，色块 #F0EDE6
- **字体：** 全局衬线（Georgia/Times New Roman），英文标签 letter-spacing 2-5px
- **圆角：** 0-4dp
- **动画：** 300ms，easeInOutCubic，无按压动效

**各页面布局：**
- 闪屏：打字机逐字效果，分割线从中心展开，小图标最后淡入，"EST. 2025" 报头
- 首页：报头（居中标题+分割线+副标题）+ 双栏分栏（照片|视频），文字链接"开始 →"
- Viewer：照片加细边框如画框、图注式文件信息（斜体）、文字按钮配英文标签（DISCARD/KEEP）、无侧边预览卡聚焦单张、分割线代替进度条显示"NO. 3 OF 10"
- 结果页：标题"清理报告"、表格式统计（带边框线+英文标签）、文字链接导航
- 设置页：细线+大写英文分区标签、杂志色卡条主题预览、下划线选中态、脚注版本信息

## 图标系统

### 操作类图标（ImageVector）

替换所有 emoji 为 SVG path 定义的 ImageVector：

| 当前 emoji | 用途 | ImageVector 描述 |
|-----------|------|-----------------|
| ← | 返回 | 左箭头线条 |
| ✓ | 保留/确认 | 勾选标记 |
| 🗑️ | 删除 | 垃圾桶轮廓 |
| ⚙️ | 设置 | 齿轮轮廓 |
| ▶ | 播放 | 三角形播放 |
| 🔇/🔊 | 静音/音量 | 喇叭+声波线条 |
| ✕ | 取消 | X 叉号 |
| 📷 | 照片 | 相机轮廓/图片山水 |
| 🎬 | 视频 | 摄像机轮廓 |
| 🔄 | 再来一轮 | 循环箭头 |
| 🏠 | 返回首页 | 房屋轮廓 |
| ⚠️ | 警告 | 三角感叹号 |

每个主题的图标参数不同：
- 极简：stroke-width 1-1.2，颜色单色
- 几何：stroke-width 2-2.5，白色
- 温暖：stroke-width 1.8，#8D6E63，stroke-linecap round
- 活泼：stroke-width 1.8-2，白色
- 杂志：stroke-width 0.8-1，#999/#CCC

### 装饰类元素（Canvas API）

- 闪屏动画：各主题的相机图标绘制动画
- 主题特色装饰：几何主题的渐变色块、活泼主题的背景渐变流动、温暖主题的柔和阴影
- 主题预览色卡：设置页的主题颜色预览条

## 架构变更

### ThemeTokens 扩展

现有 ThemeTokens 需要新增字段以支持布局差异和图标参数。所有枚举类型的字段使用 enum 而非 String，与现有 `ButtonPressAnim` 模式保持一致：

```kotlin
// 新增枚举
enum class ThemeLayoutId { MINIMAL, GEOMETRIC, WARM, PLAYFUL, EDITORIAL }
enum class ProgressStyle { THIN, BOLD, SOFT, GLASS, EDITORIAL }
enum class ButtonStyle { OUTLINED, FILLED, SHADOW, GLASS, TEXT }

// ThemeTokens 新增字段
layoutId: ThemeLayoutId                // 布局标识，用于 when 分发（编译期穷举检查）
iconStrokeWidth: Float                 // 图标线宽
iconStrokeColor: Long                  // 图标默认颜色
iconStrokeCap: StrokeCap               // androidx.compose.ui.graphics.StrokeCap
titleFontFamily: FontFamily            // androidx.compose.ui.text.font.FontFamily（Default/Serif）
progressStyle: ProgressStyle           // 进度条样式
buttonStyle: ButtonStyle               // 按钮样式
```

### 共享业务逻辑接口

每个页面的 5 个布局变体共享同一套业务逻辑。逻辑在原始 Screen Composable 中管理，通过 State 对象传入布局：

```kotlin
// 首页共享状态
class HomeScreenState(
    val theme: ThemeTokens,
    val isLimitedAccess: Boolean,
    val showPermissionDeniedDialog: Boolean,
    val showPermissionPermanentDialog: Boolean,
    val onStartPhoto: () -> Unit,
    val onStartVideo: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onDismissDialog: () -> Unit,
)

// 结果页共享状态
class ResultScreenState(
    val theme: ThemeTokens,
    val deletedCount: Int,
    val keptCount: Int,
    val freedSpace: String,
    val pendingDeleteItems: List<MediaItem>,
    val isDeleting: Boolean,
    val deleteResult: String?,
    val onConfirmDelete: () -> Unit,
    val onCancelItem: (MediaItem) -> Unit,
    val onNextRound: () -> Unit,
    val onGoHome: () -> Unit,
)

// 设置页共享状态
class SettingsScreenState(
    val theme: ThemeTokens,
    val allThemes: List<ThemeTokens>,
    val currentMode: InteractionMode,
    val currentCount: Int,
    val onThemeChange: (String) -> Unit,
    val onModeChange: (InteractionMode) -> Unit,
    val onCountChange: (Int) -> Unit,
    val onBack: () -> Unit,
)

// 闪屏共享状态
class SplashScreenState(
    val theme: ThemeTokens,
    val onSplashComplete: () -> Unit,
)
```

### 导航与路由

AppRouter、Route 定义和 ViewModel 注入保持不变。每个 Screen Composable（HomeScreen、ResultScreen 等）的函数签名不变，内部先构造 State 对象，再根据 `theme.layoutId` 分发：

```kotlin
@Composable
fun HomeScreen(router: AppRouter, theme: ThemeTokens, viewerViewModel: ViewerViewModel) {
    // 业务逻辑保持在这里（权限检查、对话框管理等）
    val state = HomeScreenState(
        theme = theme,
        isLimitedAccess = ...,
        // ... 其余字段
    )
    when (theme.layoutId) {
        ThemeLayoutId.MINIMAL   -> MinimalHomeLayout(state)
        ThemeLayoutId.GEOMETRIC -> GeometricHomeLayout(state)
        ThemeLayoutId.WARM      -> WarmHomeLayout(state)
        ThemeLayoutId.PLAYFUL   -> PlayfulHomeLayout(state)
        ThemeLayoutId.EDITORIAL -> EditorialHomeLayout(state)
    }
}
```

使用 sealed enum `ThemeLayoutId` 确保 `when` 表达式编译期穷举检查，不会遗漏新增主题。

### 图标封装

创建统一的图标入口，根据主题返回对应参数的 ImageVector：

```kotlin
object AppIcons {
    @Composable
    fun Delete(theme: ThemeTokens): ImageVector { ... }
    @Composable
    fun Keep(theme: ThemeTokens): ImageVector { ... }
    @Composable
    fun Back(theme: ThemeTokens): ImageVector { ... }
    // ...
}
```

注意：图标是纯 Composable 函数，不需要依赖注入。

### 公共组件处理

以下组件保持单一实现，通过 ThemeTokens 参数化样式（不按主题拆分）：
- `PermissionBanner` — 根据主题调整背景色、文字色、图标
- `SimpleDialog` — 根据主题调整圆角、按钮样式、字体
- `LoadingView`（ViewerScreen 内）— 根据主题调整加载指示器颜色和样式
- `ProgressHeader`（ViewerScreen 内）— 根据 `theme.progressStyle` 切换进度条样式（细线/粗条/柔和/毛玻璃/分割线+文本计数）

### 文件组织

```
shared/src/commonMain/kotlin/com/cleanpic/
├── icons/
│   └── AppIcons.kt              // 统一图标入口
├── theme/
│   ├── ThemeTokens.kt           // 扩展数据类
│   ├── ThemeManager.kt          // 5 个新主题注册
│   ├── MinimalTheme.kt
│   ├── GeometricTheme.kt
│   ├── WarmTheme.kt
│   ├── PlayfulTheme.kt
│   └── EditorialTheme.kt
└── ui/
    ├── splash/
    │   ├── SplashScreen.kt      // when 分发
    │   ├── MinimalSplash.kt
    │   ├── GeometricSplash.kt
    │   ├── WarmSplash.kt
    │   ├── PlayfulSplash.kt
    │   └── EditorialSplash.kt
    ├── home/
    │   ├── HomeScreen.kt        // when 分发
    │   ├── MinimalHome.kt
    │   ├── GeometricHome.kt
    │   ├── WarmHome.kt
    │   ├── PlayfulHome.kt
    │   └── EditorialHome.kt
    ├── viewer/
    │   ├── ViewerScreen.kt
    │   ├── CarouselMode.kt      // 内部根据主题调整视觉参数
    │   ├── SwipeCardMode.kt
    │   └── FullscreenMode.kt
    ├── result/
    │   ├── ResultScreen.kt      // when 分发
    │   ├── MinimalResult.kt
    │   ├── GeometricResult.kt
    │   ├── WarmResult.kt
    │   ├── PlayfulResult.kt
    │   └── EditorialResult.kt
    ├── settings/
    │   ├── SettingsScreen.kt    // when 分发
    │   ├── MinimalSettings.kt
    │   ├── GeometricSettings.kt
    │   ├── WarmSettings.kt
    │   ├── PlayfulSettings.kt
    │   └── EditorialSettings.kt
    └── common/
        ├── EmptyStateScreen.kt  // 根据主题分发
        ├── PermissionBanner.kt  // 根据主题调整样式
        └── SimpleDialog.kt     // 根据主题调整样式
```

### Viewer 处理方式

Viewer 的 3 种交互模式（CarouselMode/SwipeCardMode/FullscreenMode）不按主题拆分独立文件，而是在现有文件内通过 theme 参数调整：
- 按钮形状和颜色
- 进度条样式
- 卡片圆角和阴影
- 文件信息显示样式
- 动画参数

这是因为交互逻辑（拖拽、滑动、手势）是共享的，只有视觉呈现不同。

## 系统深色模式

App 不跟随系统深色模式。主题选择完全由用户在设置页控制。"大胆几何"本身是深色主题，其余为浅色。状态栏颜色跟随当前主题的背景色自动适配（深色背景用浅色状态栏图标，反之亦然）。

## 装饰性 emoji 处理

除操作类图标外，以下装饰性 emoji 也需替换：

| 当前 emoji | 位置 | 替换方案 |
|-----------|------|---------|
| ✨ | 首页标题装饰、闪屏 | 删除，各主题闪屏用 Canvas 绘制主题特色图标 |
| 🎉 | 结果页完成庆祝 | 各主题独立的完成图标（极简=无，几何=五边形勾选，温暖=圆形勾选，活泼=毛玻璃勾选，杂志=文字标题） |
| 🖼️🃏📱 | 设置页交互模式指示 | 替换为 SVG ImageVector（轮播=横向卡片组，滑卡=倾斜卡片，全屏=全屏矩形） |

## 闪屏动画技术说明

各主题闪屏动画均使用 Compose `Canvas` API + `Animatable` 实现：
- 极简（线条勾勒）：`PathMeasure` + `pathLength` 动画实现 path trimming 效果
- 几何（色块旋转）：`graphicsLayer { rotationZ }` + `drawRect` 渐变填充
- 温暖（卡片浮出）：`scale` + `alpha` 组合动画，`drawRoundRect` 绘制阴影
- 活泼（弹性弹出）：`spring(dampingRatio = 0.6f)` 弹性动画 + `drawCircle` 光点
- 杂志（打字机）：逐字 `drawText` + `drawLine` 从中心扩展动画

不使用 Lottie 等第三方库，保持 APK 体积最小。

## 字体策略

- 衬线字体（温暖/杂志主题）：使用 `FontFamily.Serif`，Android 上映射为 Noto Serif
- 无衬线字体（其余主题）：使用 `FontFamily.Default`
- 不捆绑自定义字体文件，依赖系统字体，未来跨平台时通过 `expect/actual` 扩展

## 迁移策略

### 步骤

1. 删除现有 5 个主题定义文件（DreamyGradient/SoftMinimal/CutePlayful/ElegantDark/NaturalWarm）
2. 默认主题从 DreamyGradient 改为 warm（温暖手工感），作为最友好的默认体验
3. 用户已保存的主题偏好如果是旧 ID，回退到默认主题

### 需修改的具体文件

| 文件 | 修改内容 |
|------|---------|
| `shared/.../theme/ThemeTokens.kt` | 新增字段和枚举类型 |
| `shared/.../theme/ThemeManager.kt` | 替换 5 个主题注册，更新默认主题 |
| `shared/.../settings/AppSettings.kt` | 默认 theme 从 `"dreamy-gradient"` 改为 `"warm"` |
| `shared/.../theme/DreamyGradient.kt` 等 5 个旧主题文件 | 删除 |
| `shared/.../ui/home/HomeScreen.kt` | 提取业务逻辑到 State，添加 when 分发 |
| `shared/.../ui/result/ResultScreen.kt` | 同上 |
| `shared/.../ui/settings/SettingsScreen.kt` | 同上 |
| `shared/.../ui/splash/SplashScreen.kt` | 同上 |
| `shared/.../ui/common/EmptyStateScreen.kt` | 同上 |
| `shared/.../ui/viewer/CarouselMode.kt` | 内部参数化，不拆分 |
| `shared/.../ui/viewer/SwipeCardMode.kt` | 同上 |
| `shared/.../ui/viewer/FullscreenMode.kt` | 同上 |
| `shared/.../ui/viewer/ViewerScreen.kt` | LoadingView + ProgressHeader 参数化 |
| `shared/.../ui/common/PermissionBanner.kt` | 参数化样式 |
| `shared/.../ui/common/SimpleDialog.kt` | 参数化样式 |
| `shared/src/commonTest/.../ThemeManagerTest.kt` | 更新测试用例适配新主题 |

### 需同步更新的文档

| 文档 | 修改内容 |
|------|---------|
| `docs/architecture/cleanpic/theme-system.md` | 完整重写，描述新的 5 主题体系 |
| `docs/architecture/overview.md` | 更新主题系统相关章节 |
| `docs/architecture/domain-model.md` | 新增术语：ThemeLayoutId、ProgressStyle、ButtonStyle、HomeScreenState 等 |

## 建议实现顺序

1. ThemeTokens 扩展 + 枚举定义 + 共享 State 接口
2. AppIcons 图标系统
3. 一个主题（warm）端到端完整实现（验证架构可行性）
4. 剩余 4 个主题逐一实现
5. 迁移策略 + 测试更新 + 文档同步
