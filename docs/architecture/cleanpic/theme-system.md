# CleanPic — 主题系统设计

|文档状态| v2.0 重写 | 2026-04-04 |

> 父文档: [overview.md](overview.md)
> 设计 Spec: [ui-theme-redesign-design.md](../../superpowers/specs/2026-04-04-ui-theme-redesign-design.md)

## 设计原则

1. **主题 = 布局 + 视觉**：每个主题不仅定义颜色/字体等视觉 Token，还绑定独立的页面布局。切换主题 = 换一个 App。
2. **语义化 Token**：所有 UI 组件引用 Token 而非硬编码值。
3. **矢量图标**：所有图标使用 ImageVector（操作类）或 Canvas（装饰类），不使用 emoji。
4. **业务逻辑不重复**：页面业务逻辑提取为共享 State 接口，5 个布局变体只负责 UI 呈现。

## Token 定义

### 基础 Token（继承自 v1）

```
colorBackground       — 页面底色
colorSurface          — 卡片/容器底色
colorPrimary          — 主操作色/按钮色
colorAccent           — 强调色/高亮色
colorDanger           — 删除操作色（红系）
colorSuccess          — 保留操作色（绿系）
colorText             — 主文本色
colorTextSecondary    — 次文本色
gradientMain          — 主渐变（方向 + 色值数组）
borderRadius          — 圆角大小 (dp)
shadowStyle           — 阴影风格（偏移/模糊/颜色）
fontFamily            — 字体风格
animDuration          — 基础动画时长 (ms)
animEasing            — 缓动曲线
animButtonPress       — 按钮按压动效（NONE / SCALE / BOUNCE）
```

### 新增 Token（v2）

```kotlin
// 枚举类型
enum class ThemeLayoutId { MINIMAL, GEOMETRIC, WARM, PLAYFUL, EDITORIAL }
enum class ProgressStyle { THIN, BOLD, SOFT, GLASS, EDITORIAL }
enum class ButtonStyle { OUTLINED, FILLED, SHADOW, GLASS, TEXT }

// ThemeTokens 新增字段
layoutId: ThemeLayoutId        — 布局标识，用于 when 分发（编译期穷举检查）
iconStrokeWidth: Float         — 图标默认线宽
iconStrokeColor: Long          — 图标默认颜色
iconStrokeCap: StrokeCap       — 图标线端样式（Butt/Round/Square）
titleFontFamily: FontFamily    — 标题字体（FontFamily.Default / FontFamily.Serif）
progressStyle: ProgressStyle   — 进度条样式
buttonStyle: ButtonStyle       — 按钮样式
```

## 5 套主题定义

### 1. 克制极简（minimal）

| Token | 值 |
|-------|----|
| layoutId | MINIMAL |
| colorPrimary | #333333 |
| colorAccent | #999999 |
| colorBackground | #FAFAFA |
| colorSurface | #FFFFFF |
| colorDanger | #E57373 |
| colorSuccess | #81C784 |
| gradientMain | 无（纯色） |
| borderRadius | 0-2dp |
| titleFontFamily | FontFamily.Default |
| iconStrokeWidth | 1.0-1.2 |
| iconStrokeCap | Butt |
| progressStyle | THIN |
| buttonStyle | OUTLINED |
| animDuration | 250ms |
| animEasing | easeOutQuad |
| animButtonPress | NONE |

**首页布局**：分段切换（照片/视频 tab）+ 单"开始"按钮
**视觉特色**：细线条、大留白、无圆角、低饱和度

### 2. 大胆几何（geometric）

| Token | 值 |
|-------|----|
| layoutId | GEOMETRIC |
| colorPrimary | #E94560 |
| colorAccent | #533483 |
| colorBackground | #1A1A2E |
| colorSurface | rgba(255,255,255,0.08) |
| colorDanger | 渐变 #E94560→#C62828 |
| colorSuccess | 渐变 #4CAF50→#2E7D32 |
| gradientMain | 180deg, #E94560→#0F3460 |
| borderRadius | 12-18dp |
| titleFontFamily | FontFamily.Default |
| iconStrokeWidth | 2.0-2.5 |
| iconStrokeCap | Butt |
| progressStyle | BOLD |
| buttonStyle | FILLED |
| animDuration | 300ms |
| animEasing | easeInOutCubic |
| animButtonPress | SCALE |

**首页布局**：双卡片并排入口（照片/视频各一个渐变大卡片）
**视觉特色**：深色底、粗线条、渐变色块、高对比

### 3. 温暖手工感（warm）— 默认主题

| Token | 值 |
|-------|----|
| layoutId | WARM |
| colorPrimary | #5D4037 |
| colorAccent | #8D6E63 |
| colorBackground | #FFF8F0 |
| colorSurface | #FFFFFF |
| colorDanger | #E57373 |
| colorSuccess | #81C784 |
| gradientMain | 无（纯色） |
| borderRadius | 16-24dp |
| titleFontFamily | FontFamily.Serif |
| iconStrokeWidth | 1.8 |
| iconStrokeCap | Round |
| progressStyle | SOFT |
| buttonStyle | SHADOW |
| animDuration | 280ms |
| animEasing | easeOutCubic |
| animButtonPress | SCALE |

**首页布局**：列表卡片堆叠（照片/视频各一行，带图标+描述+箭头）
**视觉特色**：暖色调、大圆角、柔和阴影、衬线标题

### 4. 活泼精致（playful）

| Token | 值 |
|-------|----|
| layoutId | PLAYFUL |
| colorPrimary | #667EEA |
| colorAccent | #764BA2 |
| colorBackground | 渐变 #667EEA→#764BA2 |
| colorSurface | rgba(255,255,255,0.12) + blur |
| colorDanger | rgba(229,115,115,0.3) |
| colorSuccess | rgba(129,199,132,0.3) |
| gradientMain | 160deg, #667EEA→#764BA2 |
| borderRadius | 16-24dp |
| titleFontFamily | FontFamily.Default |
| iconStrokeWidth | 1.8-2.0 |
| iconStrokeCap | Round |
| progressStyle | GLASS |
| buttonStyle | GLASS |
| animDuration | 350ms |
| animEasing | easeOutBounce |
| animButtonPress | BOUNCE |

**首页布局**：毛玻璃大卡片居中，内含照片/视频图标按钮
**视觉特色**：渐变背景、毛玻璃效果、浮动感
**API 降级**：blur 效果需 API 31+；API 26-30 降级为半透明纯色 + 白色边框

### 5. 杂志排版（editorial）

| Token | 值 |
|-------|----|
| layoutId | EDITORIAL |
| colorPrimary | #1A1A1A |
| colorAccent | #999999 |
| colorBackground | #FFFFF5 |
| colorSurface | #F0EDE6 |
| colorDanger | #C62828 |
| colorSuccess | #2E7D32 |
| gradientMain | 无（纯色） |
| borderRadius | 0-4dp |
| titleFontFamily | FontFamily.Serif |
| iconStrokeWidth | 0.8-1.0 |
| iconStrokeCap | Butt |
| progressStyle | EDITORIAL |
| buttonStyle | TEXT |
| animDuration | 300ms |
| animEasing | easeInOutCubic |
| animButtonPress | NONE |

**首页布局**：报头 + 双栏分栏（照片|视频）
**视觉特色**：衬线字体、细分割线、米白底、字体层次感

## 主题总览表

| 主题 | layoutId | 主色调 | 圆角 | 图标线宽 | 按钮风格 | 动效 |
|------|----------|--------|------|---------|---------|------|
| 克制极简 | MINIMAL | 灰黑 | 0-2dp | 1.0 | 描边 | 无 |
| 大胆几何 | GEOMETRIC | 红紫深 | 12-18dp | 2.0+ | 渐变填充 | SCALE |
| 温暖手工感（默认）| WARM | 棕暖 | 16-24dp | 1.8 | 阴影 | SCALE |
| 活泼精致 | PLAYFUL | 蓝紫渐变 | 16-24dp | 1.8-2.0 | 毛玻璃 | BOUNCE |
| 杂志排版 | EDITORIAL | 黑米 | 0-4dp | 0.8 | 文字 | 无 |

## 图标系统

所有 emoji 替换为 ImageVector，通过 `AppIcons` 统一入口：

| 图标 | path 描述 | 各主题差异 |
|------|----------|-----------|
| Back | 左箭头 M19 12H5 M12 19l-7-7 7-7 | 线宽/颜色跟随主题 Token |
| Delete | 垃圾桶轮廓 | 极简=描边，几何=白色粗线，杂志=纤细+文字标签 |
| Keep | 勾选 M20 6 9 17 4 12 | 同上 |
| Settings | 齿轮 | 同上 |
| Play | 三角形 | 同上 |
| Mute/Unmute | 喇叭±声波 | 同上 |
| Photo | 图片山水 | 同上 |
| Video | 摄像机 | 同上 |
| Refresh | 循环箭头 | 同上 |
| Home | 房屋 | 同上 |
| Warning | 三角感叹号 | 同上 |
| Close | X 叉号 | 同上 |

装饰类元素（闪屏动画、背景图案）使用 Canvas API 绘制。

## 页面分发架构

```kotlin
// 每个 Screen 提取业务逻辑为 State，再按 layoutId 分发布局
@Composable
fun HomeScreen(router: AppRouter, theme: ThemeTokens, viewModel: ViewerViewModel) {
    val state = HomeScreenState(theme, ...) // 业务逻辑在这里
    when (theme.layoutId) {
        ThemeLayoutId.MINIMAL   -> MinimalHomeLayout(state)
        ThemeLayoutId.GEOMETRIC -> GeometricHomeLayout(state)
        ThemeLayoutId.WARM      -> WarmHomeLayout(state)
        ThemeLayoutId.PLAYFUL   -> PlayfulHomeLayout(state)
        ThemeLayoutId.EDITORIAL -> EditorialHomeLayout(state)
    }
}
```

Viewer 的 3 种交互模式不按主题拆分文件，在内部通过 theme 参数调整视觉。

## 系统深色模式

App 不跟随系统深色模式。主题选择完全由用户控制。"大胆几何"本身是深色主题。

## 迁移：v1 → v2

- 删除旧 5 主题（DreamyGradient/SoftMinimal/CutePlayful/ElegantDark/NaturalWarm）
- 默认主题改为 warm
- 旧主题 ID 自动回退到默认主题
