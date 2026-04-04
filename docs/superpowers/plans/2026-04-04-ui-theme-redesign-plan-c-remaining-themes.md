# UI 主题重设计 — Plan C：剩余 4 个主题差异化布局

> **执行者须知：** 必须使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务执行本计划。步骤使用 `- [ ]` 语法跟踪进度。

**目标：** 为剩余 4 个主题（克制极简、大胆几何、活泼精致、杂志排版）创建差异化的页面布局，更新所有 Screen 的 `when` 分发，让切换主题真正等于换一个 App。

**架构：** 每个主题需要 4 个布局文件（Home/Splash/Result/Settings）。Viewer 和公共组件已在 Plan B 中参数化，无需按主题拆分。每个 Screen 的 `when (theme.layoutId)` 从 `else -> WarmXxxLayout` 改为完整的 5 路分发。

**技术栈：** Kotlin 2.1.21、Compose Multiplatform 1.7.3、Compose Canvas

**依赖：** Plan A（基础设施）+ Plan B（warm 主题端到端 + 架构验证）

---

## 文件清单

| 操作 | 文件 | 职责 |
|------|------|------|
| 新建 | `shared/.../theme/MinimalTheme.kt` | 克制极简主题定义 |
| 新建 | `shared/.../theme/GeometricTheme.kt` | 大胆几何主题定义 |
| 新建 | `shared/.../theme/PlayfulTheme.kt` | 活泼精致主题定义 |
| 新建 | `shared/.../theme/EditorialTheme.kt` | 杂志排版主题定义 |
| 修改 | `shared/.../theme/ThemeManager.kt` | 注册全部 5 个主题 |
| 新建 | `shared/.../ui/home/MinimalHomeLayout.kt` | 极简首页：分段切换+单按钮 |
| 新建 | `shared/.../ui/home/GeometricHomeLayout.kt` | 几何首页：双卡片并排 |
| 新建 | `shared/.../ui/home/PlayfulHomeLayout.kt` | 活泼首页：毛玻璃卡片+图标网格 |
| 新建 | `shared/.../ui/home/EditorialHomeLayout.kt` | 杂志首页：报头+双栏分栏 |
| 新建 | `shared/.../ui/splash/MinimalSplashLayout.kt` | 极简闪屏：线条勾勒 |
| 新建 | `shared/.../ui/splash/GeometricSplashLayout.kt` | 几何闪屏：色块旋转 |
| 新建 | `shared/.../ui/splash/PlayfulSplashLayout.kt` | 活泼闪屏：弹性弹出 |
| 新建 | `shared/.../ui/splash/EditorialSplashLayout.kt` | 杂志闪屏：打字机效果 |
| 新建 | `shared/.../ui/result/MinimalResultLayout.kt` | 极简结果页 |
| 新建 | `shared/.../ui/result/GeometricResultLayout.kt` | 几何结果页 |
| 新建 | `shared/.../ui/result/PlayfulResultLayout.kt` | 活泼结果页 |
| 新建 | `shared/.../ui/result/EditorialResultLayout.kt` | 杂志结果页 |
| 新建 | `shared/.../ui/settings/MinimalSettingsLayout.kt` | 极简设置页 |
| 新建 | `shared/.../ui/settings/GeometricSettingsLayout.kt` | 几何设置页 |
| 新建 | `shared/.../ui/settings/PlayfulSettingsLayout.kt` | 活泼设置页 |
| 新建 | `shared/.../ui/settings/EditorialSettingsLayout.kt` | 杂志设置页 |
| 修改 | `shared/.../ui/home/HomeScreen.kt` | when 完整 5 路分发 |
| 修改 | `shared/.../ui/splash/SplashScreen.kt` | when 完整 5 路分发 |
| 修改 | `shared/.../ui/result/ResultScreen.kt` | when 完整 5 路分发 |
| 修改 | `shared/.../ui/settings/SettingsScreen.kt` | when 完整 5 路分发 |
| 修改 | `shared/.../theme/ThemeManagerTest.kt` | 更新测试为 5 个主题 |

---

### 任务 1：创建 4 个主题定义 + 更新 ThemeManager

**文件：**
- 新建：`MinimalTheme.kt`、`GeometricTheme.kt`、`PlayfulTheme.kt`、`EditorialTheme.kt`
- 修改：`ThemeManager.kt`、`ThemeManagerTest.kt`

**主题参数（来自设计 spec）：**

**MinimalTheme（克制极简）：**
```kotlin
id = "minimal", name = "克制极简"
colorPrimary = 0xFF333333, colorAccent = 0xFF999999
colorBackground = 0xFFFAFAFA, colorSurface = 0xFFFFFFFF
colorDanger = 0xFFE57373, colorSuccess = 0xFF81C784
colorText = 0xFF333333, colorTextSecondary = 0xFF999999
gradientMain = null, borderRadius = 2f
shadowStyle = ShadowDef(0f, 1f, 4f, 0x0A000000)
fontFamily = "System", titleFontFamily = "System"
animDuration = 250L, animEasing = "easeOutQuad", animButtonPress = NONE
layoutId = MINIMAL, iconStrokeWidth = 1.2f, iconStrokeColor = 0xFF333333
iconStrokeCap = BUTT, progressStyle = THIN, buttonStyle = OUTLINED
```

**GeometricTheme（大胆几何）：**
```kotlin
id = "geometric", name = "大胆几何"
colorPrimary = 0xFFE94560, colorAccent = 0xFF533483
colorBackground = 0xFF1A1A2E, colorSurface = 0xFF16213E
colorDanger = 0xFFE94560, colorSuccess = 0xFF4CAF50
colorText = 0xFFFFFFFF, colorTextSecondary = 0x66FFFFFF
gradientMain = GradientDef(180f, listOf(0xFFE94560, 0xFF0F3460))
borderRadius = 16f
shadowStyle = ShadowDef(0f, 4f, 16f, 0x60000000)
fontFamily = "System", titleFontFamily = "System"
animDuration = 300L, animEasing = "easeInOutCubic", animButtonPress = SCALE
layoutId = GEOMETRIC, iconStrokeWidth = 2.5f, iconStrokeColor = 0xFFFFFFFF
iconStrokeCap = BUTT, progressStyle = BOLD, buttonStyle = FILLED
```

**PlayfulTheme（活泼精致）：**
```kotlin
id = "playful", name = "活泼精致"
colorPrimary = 0xFF667EEA, colorAccent = 0xFF764BA2
colorBackground = 0xFF667EEA, colorSurface = 0x1FFFFFFF
colorDanger = 0x4DE57373, colorSuccess = 0x4D81C784
colorText = 0xFFFFFFFF, colorTextSecondary = 0x99FFFFFF
gradientMain = GradientDef(160f, listOf(0xFF667EEA, 0xFF764BA2))
borderRadius = 20f
shadowStyle = ShadowDef(0f, 4f, 20f, 0x40000000)
fontFamily = "System", titleFontFamily = "System"
animDuration = 350L, animEasing = "easeOutBounce", animButtonPress = BOUNCE
layoutId = PLAYFUL, iconStrokeWidth = 2.0f, iconStrokeColor = 0xFFFFFFFF
iconStrokeCap = ROUND, progressStyle = GLASS, buttonStyle = GLASS
```

**EditorialTheme（杂志排版）：**
```kotlin
id = "editorial", name = "杂志排版"
colorPrimary = 0xFF1A1A1A, colorAccent = 0xFF999999
colorBackground = 0xFFFFFFF5, colorSurface = 0xFFF0EDE6
colorDanger = 0xFFC62828, colorSuccess = 0xFF2E7D32
colorText = 0xFF1A1A1A, colorTextSecondary = 0xFF555555
gradientMain = null, borderRadius = 2f
shadowStyle = ShadowDef(0f, 0f, 0f, 0x00000000)
fontFamily = "Serif", titleFontFamily = "Serif"
animDuration = 300L, animEasing = "easeInOutCubic", animButtonPress = NONE
layoutId = EDITORIAL, iconStrokeWidth = 1.0f, iconStrokeColor = 0xFF999999
iconStrokeCap = BUTT, progressStyle = EDITORIAL, buttonStyle = TEXT
```

**ThemeManager 更新：**
```kotlin
val allThemes = listOf(WarmTheme, MinimalTheme, GeometricTheme, PlayfulTheme, EditorialTheme)
```

**ThemeManagerTest 更新：**
- `all_themes_available` 断言 `assertEquals(5, ...)`
- `switch_theme_updates_current` 测试切换到 "geometric" 等新 ID

- [ ] 步骤 1：创建 4 个主题定义文件
- [ ] 步骤 2：更新 ThemeManager 注册全部 5 个主题
- [ ] 步骤 3：更新 ThemeManagerTest
- [ ] 步骤 4：运行测试确认通过
- [ ] 步骤 5：提交

---

### 任务 2：克制极简主题 — 4 个布局

**新建 4 个文件：**
- `shared/.../ui/home/MinimalHomeLayout.kt`
- `shared/.../ui/splash/MinimalSplashLayout.kt`
- `shared/.../ui/result/MinimalResultLayout.kt`
- `shared/.../ui/settings/MinimalSettingsLayout.kt`

**设计规格：**

**首页（分段切换 + 单按钮）：**
- 左上角：小字标题 "刷刷鸭"（letter-spacing 3dp，灰色，大写风格）
- 右上角：IconPainter("settings")
- 细分割线
- 分段标签：照片（选中=下划线加粗）/ 视频（未选中=灰色）——用 `var selectedTab` 状态切换，点照片 tab 后按"开始"调 onStartPhoto，点视频 tab 调 onStartVideo
- 中心：大尺寸淡色图标（photo 或 video）
- 底部："开始"按钮——方形描边（border 1dp #333，无圆角，无填充）

**闪屏（线条勾勒）：**
- 浅灰背景 #FAFAFA
- Canvas 绘制相机图标轮廓（使用 parseSvgPath + 虚线 dashPathEffect）
- 标题 "刷刷鸭"（letter-spacing 4dp，300 weight）
- 细分割线
- 动画：strokePhase 从 1f→0f（线条逐渐显现）

**结果页（列表式统计）：**
- 无图标，纯文字标题 "COMPLETE" + "本轮清理完成"
- 细分割线
- 列表式统计（每项一行：标签 + 数字，底部分割线）
- 方形缩略图（无圆角）
- 描边按钮（确认删除/再来一轮/返回首页）

**设置页（英文大写标签）：**
- 返回按钮 IconPainter("back") + 居中 "设置"（letter-spacing 2dp）
- 分割线
- 分区标签大写英文：THEME / MODE / COUNT
- 主题选择：窄方形预览卡（无圆角），选中态=2dp 黑色边框
- 模式选择：方形描边按钮，文字标签
- 数量选择：方形描边方块
- 底部：版本信息（极细文字）

**所有按钮/卡片特征：无圆角（0-2dp）、1dp 描边、无阴影、大量留白。**

- [ ] 步骤 1：创建 MinimalHomeLayout.kt
- [ ] 步骤 2：创建 MinimalSplashLayout.kt
- [ ] 步骤 3：创建 MinimalResultLayout.kt
- [ ] 步骤 4：创建 MinimalSettingsLayout.kt
- [ ] 步骤 5：验证构建
- [ ] 步骤 6：提交

---

### 任务 3：大胆几何主题 — 4 个布局

**新建 4 个文件：**
- `shared/.../ui/home/GeometricHomeLayout.kt`
- `shared/.../ui/splash/GeometricSplashLayout.kt`
- `shared/.../ui/result/GeometricResultLayout.kt`
- `shared/.../ui/settings/GeometricSettingsLayout.kt`

**设计规格：**

**首页（双卡片并排）：**
- 深色背景 #1A1A2E
- 左上角：红色粗体 "刷刷鸭"（#E94560，weight 800）
- 右上角：三点菜单图标（IconPainter("settings")，白色半透明）
- 两个并排大卡片（flex 1:1，渐变填充，16dp 圆角）：
  - 照片卡：渐变 #E94560→#0F3460，顶部 IconPainter("photo", 粗白色)，底部 "照片" + "PHOTO"
  - 视频卡：渐变 #533483→#0F3460，顶部 IconPainter("video", 粗白色)，底部 "视频" + "VIDEO"
- 底部：渐变线条（3dp 高，#E94560→#533483）

**闪屏（几何色块旋转）：**
- 深色背景 #1A1A2E
- Canvas 绘制：旋转的方形边框（红色）+ 渐变填充方块 + 白色圆形
- 红色粗体 "刷刷鸭" + 渐变线条
- 动画：rotation 0→360°（300ms）

**结果页（色块卡片）：**
- 深色背景
- 几何图形完成图标（Canvas 绘制五边形 + 勾选）
- "清理完成"（白色粗体）
- 3 个色块统计卡片（红色/绿色/紫色半透明背景，大数字 + 英文小标签 DELETE/KEEP/MB FREE）
- 渐变确认删除按钮
- 半透明背景的再来一轮/返回首页按钮

**设置页（暗色卡片）：**
- 深色背景，白色粗体"设置"
- 主题预览：圆角卡片（选中=红色边框）
- 模式选择：暗色卡片 + 图标 + 文字，选中=红色半透明背景
- 数量选择：渐变填充选中态
- 底部：渐变分割线 + 版本信息

**所有元素特征：深色底、渐变色块、粗线条图标（2.5px）、高对比。**

- [ ] 步骤 1：创建 GeometricHomeLayout.kt
- [ ] 步骤 2：创建 GeometricSplashLayout.kt
- [ ] 步骤 3：创建 GeometricResultLayout.kt
- [ ] 步骤 4：创建 GeometricSettingsLayout.kt
- [ ] 步骤 5：验证构建
- [ ] 步骤 6：提交

---

### 任务 4：活泼精致主题 — 4 个布局

**新建 4 个文件：**
- `shared/.../ui/home/PlayfulHomeLayout.kt`
- `shared/.../ui/splash/PlayfulSplashLayout.kt`
- `shared/.../ui/result/PlayfulResultLayout.kt`
- `shared/.../ui/settings/PlayfulSettingsLayout.kt`

**设计规格：**

**首页（毛玻璃卡片 + 图标网格）：**
- 渐变背景（#667EEA→#764BA2）
- 左上角：白色粗体 "刷刷鸭"
- 右上角：半透明圆角方块内 IconPainter("settings")
- 中心：大毛玻璃卡片（半透明白色 12% + 白色边框 20%，24dp 圆角）
  - 卡片内：提示文字 "选择要清理的内容"
  - 两个图标方块（72dp，半透明背景 + 边框）：照片 / 视频
- 底部：小圆点指示器

**毛玻璃效果实现：** 半透明白色背景（0x1FFFFFFF）+ 1dp 白色半透明边框（0x33FFFFFF）。不使用 `RenderEffect.createBlurEffect`（需要 API 31），用纯色模拟即可。

**闪屏（弹性弹出）：**
- 渐变背景
- 毛玻璃圆角方块（88dp，28dp 圆角）内含相机图标
- 白色粗体 "刷刷鸭"
- 3 个小圆点（依次亮起）
- 动画：spring(dampingRatio = 0.6f) 弹性 scale + 淡入

**结果页（毛玻璃卡片）：**
- 渐变背景贯穿
- 毛玻璃完成图标
- "清理完成！"白色粗体
- 3 个毛玻璃统计卡片
- 毛玻璃半透明删除按钮
- 毛玻璃再来一轮/返回首页

**设置页（毛玻璃分区）：**
- 渐变背景
- 毛玻璃返回按钮 + 白色"设置"
- 毛玻璃卡片分区（主题/模式/数量）
- 选中态：白色半透明高亮

**所有元素特征：渐变背景、半透明白色+边框模拟毛玻璃、浮动感、圆角。**

- [ ] 步骤 1：创建 PlayfulHomeLayout.kt
- [ ] 步骤 2：创建 PlayfulSplashLayout.kt
- [ ] 步骤 3：创建 PlayfulResultLayout.kt
- [ ] 步骤 4：创建 PlayfulSettingsLayout.kt
- [ ] 步骤 5：验证构建
- [ ] 步骤 6：提交

---

### 任务 5：杂志排版主题 — 4 个布局

**新建 4 个文件：**
- `shared/.../ui/home/EditorialHomeLayout.kt`
- `shared/.../ui/splash/EditorialSplashLayout.kt`
- `shared/.../ui/result/EditorialResultLayout.kt`
- `shared/.../ui/settings/EditorialSettingsLayout.kt`

**设计规格：**

**首页（报头 + 双栏分栏）：**
- 米白背景 #FFFFF5
- 居中报头：小字 "EST. 2025"（letter-spacing 4dp）+ 衬线 "刷刷鸭"（26sp）+ 细分割线 + "随机一刷 · 相册清爽"
- 双栏并排（中间 1px 分割线）：
  - 左栏：小字 "PHOTOS" + 色块区域（内含淡色图标）+ 描述文字 + "开始 →" 链接
  - 右栏：小字 "VIDEOS" + 同样结构
- 底部分割线 + 居中设置齿轮图标

**闪屏（打字机效果）：**
- 米白背景
- "— EST. 2025 —"（小字）
- 衬线 "刷刷鸭"（32sp）
- 细分割线
- "随机一刷 · 相册清爽"
- 小尺寸淡色相机图标
- 动画：标题逐字显现（clipToBounds + animatedWidth）+ 分割线从中心展开

**结果页（清理报告）：**
- 标题 "SUMMARY" + 衬线 "清理报告" + 分割线
- 表格式统计（3 列，细边框分隔）：DISCARD / KEEP / MB FREED
- 待删除缩略图（细边框方形）
- 描边确认按钮（"CONFIRM DELETE"，letter-spacing）
- 分割线
- 文字链接导航（"NEXT ROUND →" / "HOME →"）

**设置页（杂志色卡）：**
- 返回："BACK" 文字链接
- 分区标签大写英文（衬线）：THEME / MODE / COUNT
- 主题选择：色条预览（窄长条），选中态=下方细线标记
- 模式选择：文字按钮，选中=下划线
- 数量选择：简单文字，选中=粗体
- 底部脚注式版本信息

**所有元素特征：衬线字体、细分割线、米白底、无阴影、字体层次感、英文标签。**

- [ ] 步骤 1：创建 EditorialHomeLayout.kt
- [ ] 步骤 2：创建 EditorialSplashLayout.kt
- [ ] 步骤 3：创建 EditorialResultLayout.kt
- [ ] 步骤 4：创建 EditorialSettingsLayout.kt
- [ ] 步骤 5：验证构建
- [ ] 步骤 6：提交

---

### 任务 6：更新所有 Screen 的 when 分发

**修改 4 个文件：**
- `shared/.../ui/home/HomeScreen.kt`
- `shared/.../ui/splash/SplashScreen.kt`
- `shared/.../ui/result/ResultScreen.kt`
- `shared/.../ui/settings/SettingsScreen.kt`

将每个文件中的 `when (theme.layoutId) { else -> WarmXxxLayout(state) }` 替换为完整 5 路分发：

```kotlin
when (theme.layoutId) {
    ThemeLayoutId.MINIMAL   -> MinimalHomeLayout(state)
    ThemeLayoutId.GEOMETRIC -> GeometricHomeLayout(state)
    ThemeLayoutId.WARM      -> WarmHomeLayout(state)
    ThemeLayoutId.PLAYFUL   -> PlayfulHomeLayout(state)
    ThemeLayoutId.EDITORIAL -> EditorialHomeLayout(state)
}
```

4 个 Screen 都按这个模式更新（替换 Home/Splash/Result/Settings 对应的 Layout 名称）。

- [ ] 步骤 1：更新 HomeScreen.kt
- [ ] 步骤 2：更新 SplashScreen.kt
- [ ] 步骤 3：更新 ResultScreen.kt
- [ ] 步骤 4：更新 SettingsScreen.kt
- [ ] 步骤 5：验证构建
- [ ] 步骤 6：提交

---

### 任务 7：全量验证

- [ ] 步骤 1：运行 `./gradlew :shared:testDebugUnitTest`（全部通过）
- [ ] 步骤 2：运行 `scripts/build-android.sh`（构建成功）
- [ ] 步骤 3：确认 ThemeManager.allThemes.size == 5
- [ ] 步骤 4：确认每个 Screen 的 when 分发覆盖 5 个 layoutId（无 else 分支）

---

## Plan C 完成检查清单

- [ ] 4 个新主题定义完整（MinimalTheme/GeometricTheme/PlayfulTheme/EditorialTheme）
- [ ] ThemeManager 注册 5 个主题，测试通过
- [ ] 16 个新布局文件创建（4 主题 × 4 页面）
- [ ] 4 个 Screen 的 when 分发完整覆盖 5 个 ThemeLayoutId
- [ ] 所有单元测试通过
- [ ] 构建成功
