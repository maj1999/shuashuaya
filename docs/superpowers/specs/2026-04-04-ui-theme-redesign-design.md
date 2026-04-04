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
- **毛玻璃：** backdrop-filter blur(8-12dp)，白色 10-15% 透明度
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

现有 ThemeTokens 需要新增字段以支持布局差异和图标参数：

```
// 新增字段
layoutId: String           // 布局标识：minimal/geometric/warm/playful/editorial
iconStrokeWidth: Float     // 图标线宽
iconStrokeColor: Long      // 图标默认颜色
iconStrokeCap: StrokeCap   // 线端样式：Butt/Round/Square
titleFontFamily: String    // 标题字体族：System/Serif
progressStyle: String      // 进度条样式：thin/bold/soft/glass/editorial
buttonStyle: String        // 按钮样式：outlined/filled/shadow/glass/text
```

### 页面组件结构

每个页面根据 `layoutId` 分发到不同的布局 Composable：

```
// 示例：首页
@Composable
fun HomeScreen(theme: ThemeTokens) {
    when (theme.layoutId) {
        "minimal"   -> MinimalHomeLayout(theme)
        "geometric" -> GeometricHomeLayout(theme)
        "warm"      -> WarmHomeLayout(theme)
        "playful"   -> PlayfulHomeLayout(theme)
        "editorial" -> EditorialHomeLayout(theme)
    }
}
```

每个 Layout Composable 是独立的，包含自己的布局逻辑、装饰元素和动画。共享业务逻辑（权限请求、媒体加载、导航等）通过参数传入。

### 图标封装

创建统一的图标入口，根据主题返回对应参数的 ImageVector：

```
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

## 迁移策略

1. 删除现有 5 个主题定义文件（DreamyGradient/SoftMinimal/CutePlayful/ElegantDark/NaturalWarm）
2. 默认主题从 DreamyGradient 改为 warm（温暖手工感），作为最友好的默认体验
3. 用户已保存的主题偏好如果是旧 ID，回退到默认主题
