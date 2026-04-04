# UI 主题重设计 — Plan A：基础设施

> **执行者须知：** 必须使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务执行本计划。步骤使用 `- [ ]` 语法跟踪进度。

**目标：** 扩展主题系统，添加布局标识、类型安全枚举、矢量图标系统和共享页面 State 接口——这是 5 个新主题的基础。

**架构：** ThemeTokens 新增字段（layoutId、iconStroke*、progressStyle、buttonStyle、titleFontFamily）。每个页面定义共享 State 类封装业务逻辑，5 个布局变体只负责 UI 呈现。AppIcons 提供主题化的 ImageVector 图标替换所有 emoji。

**技术栈：** Kotlin 2.1.21、Compose Multiplatform 1.7.3、kotlin.test

**设计 Spec：** `docs/superpowers/specs/2026-04-04-ui-theme-redesign-design.md`

**依赖：** 无（这是第一个计划）
**阻塞：** Plan B（warm 主题端到端）、Plan C（剩余 4 个主题）

---

## 文件清单

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeTokens.kt` | 添加枚举 + 新字段 |
| 修改 | `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeManager.kt` | 更新默认值，添加迁移回退 |
| 修改 | `shared/src/commonMain/kotlin/com/cleanpic/settings/AppSettings.kt` | 默认主题改为 "warm" |
| 新建 | `shared/src/commonMain/kotlin/com/cleanpic/theme/WarmTheme.kt` | 第一个新主题定义 |
| 新建 | `shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt` | 统一矢量图标入口 |
| 新建 | `shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt` | 首页共享状态 |
| 新建 | `shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreenState.kt` | 结果页共享状态 |
| 新建 | `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt` | 设置页共享状态 |
| 新建 | `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreenState.kt` | 闪屏共享状态 |
| 修改 | `shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeManagerTest.kt` | 更新测试适配新默认值 + 迁移 |
| 修改 | `shared/src/commonTest/kotlin/com/cleanpic/settings/AppSettingsTest.kt` | 更新默认主题断言 |
| 新建 | `shared/src/commonTest/kotlin/com/cleanpic/icons/AppIconsTest.kt` | 测试图标生成 |

---

### 任务 1：扩展 ThemeTokens 枚举和新字段

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeTokens.kt`

- [ ] **步骤 1：写失败的测试**

新建测试文件验证枚举类型和字段：

文件：`shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeTokensTest.kt`

```kotlin
package com.cleanpic.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeTokensTest {
    @Test fun theme_layout_id_enum_has_five_values() {
        val values = ThemeLayoutId.entries
        assertEquals(5, values.size)
        assertEquals(
            setOf("MINIMAL", "GEOMETRIC", "WARM", "PLAYFUL", "EDITORIAL"),
            values.map { it.name }.toSet()
        )
    }

    @Test fun progress_style_enum_has_five_values() {
        val values = ProgressStyle.entries
        assertEquals(5, values.size)
        assertEquals(
            setOf("THIN", "BOLD", "SOFT", "GLASS", "EDITORIAL"),
            values.map { it.name }.toSet()
        )
    }

    @Test fun button_style_enum_has_five_values() {
        val values = ButtonStyle.entries
        assertEquals(5, values.size)
        assertEquals(
            setOf("OUTLINED", "FILLED", "SHADOW", "GLASS", "TEXT"),
            values.map { it.name }.toSet()
        )
    }
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：`scripts/test.sh`
预期：失败 — ThemeLayoutId、ProgressStyle、ButtonStyle 未定义

- [ ] **步骤 3：实现 ThemeTokens 扩展**

替换 `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeTokens.kt` 为：

```kotlin
package com.cleanpic.theme

/**
 * 主题渐变定义
 */
data class GradientDef(
    val angle: Float,
    val colors: List<Long>
)

/**
 * 阴影样式定义
 */
data class ShadowDef(
    val offsetX: Float,
    val offsetY: Float,
    val blur: Float,
    val color: Long
)

/**
 * 按钮按下动画类型
 */
enum class ButtonPressAnim { NONE, SCALE, BOUNCE }

/**
 * 主题布局标识 — 决定每个页面使用哪种布局
 */
enum class ThemeLayoutId { MINIMAL, GEOMETRIC, WARM, PLAYFUL, EDITORIAL }

/**
 * 进度条视觉样式
 */
enum class ProgressStyle { THIN, BOLD, SOFT, GLASS, EDITORIAL }

/**
 * 按钮视觉样式
 */
enum class ButtonStyle { OUTLINED, FILLED, SHADOW, GLASS, TEXT }

/**
 * 图标线端样式
 */
enum class IconStrokeCap { BUTT, ROUND, SQUARE }

/**
 * 主题设计令牌 — 承载完整的主题视觉定义
 */
data class ThemeTokens(
    val id: String,
    val name: String,
    // 颜色
    val colorPrimary: Long,
    val colorAccent: Long,
    val colorBackground: Long,
    val colorSurface: Long,
    val colorDanger: Long,
    val colorSuccess: Long,
    val colorText: Long,
    val colorTextSecondary: Long,
    // 渐变与形状
    val gradientMain: GradientDef?,
    val borderRadius: Float,
    val shadowStyle: ShadowDef,
    // 字体
    val fontFamily: String,
    val titleFontFamily: String = "System",
    // 动画
    val animDuration: Long,
    val animEasing: String,
    val animButtonPress: ButtonPressAnim,
    // v2：布局与图标
    val layoutId: ThemeLayoutId = ThemeLayoutId.WARM,
    val iconStrokeWidth: Float = 1.8f,
    val iconStrokeColor: Long = 0xFF333333,
    val iconStrokeCap: IconStrokeCap = IconStrokeCap.ROUND,
    val progressStyle: ProgressStyle = ProgressStyle.SOFT,
    val buttonStyle: ButtonStyle = ButtonStyle.SHADOW
)
```

注意：新字段都有默认值，确保现有主题定义无需修改即可编译。

- [ ] **步骤 4：运行测试确认通过**

运行：`scripts/test.sh`
预期：全部通过（新测试 + 现有测试，因为默认值保持向后兼容）

- [ ] **步骤 5：提交**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeTokens.kt \
       shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeTokensTest.kt
git commit -m "feat: extend ThemeTokens with layout/icon/style enums and fields"
```

---

### 任务 2：创建 WarmTheme 定义

**文件：**
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/theme/WarmTheme.kt`

- [ ] **步骤 1：写失败的测试**

在 `shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeTokensTest.kt` 中添加：

```kotlin
@Test fun warm_theme_has_correct_layout_id() {
    assertEquals(ThemeLayoutId.WARM, WarmTheme.layoutId)
    assertEquals("warm", WarmTheme.id)
    assertEquals("温暖手工感", WarmTheme.name)
}

@Test fun warm_theme_has_correct_icon_params() {
    assertEquals(1.8f, WarmTheme.iconStrokeWidth)
    assertEquals(IconStrokeCap.ROUND, WarmTheme.iconStrokeCap)
    assertEquals(ProgressStyle.SOFT, WarmTheme.progressStyle)
    assertEquals(ButtonStyle.SHADOW, WarmTheme.buttonStyle)
    assertEquals("Serif", WarmTheme.titleFontFamily)
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：`scripts/test.sh`
预期：失败 — WarmTheme 未定义

- [ ] **步骤 3：创建 WarmTheme**

文件：`shared/src/commonMain/kotlin/com/cleanpic/theme/WarmTheme.kt`

```kotlin
package com.cleanpic.theme

/**
 * 温暖手工感主题 — 暖色调、大圆角、柔和阴影、衬线标题
 * 参考：Bear / Things
 */
val WarmTheme = ThemeTokens(
    id = "warm",
    name = "温暖手工感",
    colorPrimary = 0xFF5D4037,
    colorAccent = 0xFF8D6E63,
    colorBackground = 0xFFFFF8F0,
    colorSurface = 0xFFFFFFFF,
    colorDanger = 0xFFE57373,
    colorSuccess = 0xFF81C784,
    colorText = 0xFF5D4037,
    colorTextSecondary = 0xFFA1887F,
    gradientMain = null,
    borderRadius = 20f,
    shadowStyle = ShadowDef(
        offsetX = 0f,
        offsetY = 2f,
        blur = 12f,
        color = 0x1F5D4037
    ),
    fontFamily = "System",
    titleFontFamily = "Serif",
    animDuration = 280L,
    animEasing = "easeOutCubic",
    animButtonPress = ButtonPressAnim.SCALE,
    layoutId = ThemeLayoutId.WARM,
    iconStrokeWidth = 1.8f,
    iconStrokeColor = 0xFF8D6E63,
    iconStrokeCap = IconStrokeCap.ROUND,
    progressStyle = ProgressStyle.SOFT,
    buttonStyle = ButtonStyle.SHADOW
)
```

- [ ] **步骤 4：运行测试确认通过**

运行：`scripts/test.sh`
预期：全部通过

- [ ] **步骤 5：提交**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/theme/WarmTheme.kt \
       shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeTokensTest.kt
git commit -m "feat: add WarmTheme as first v2 theme definition"
```

---

### 任务 3：更新 ThemeManager 默认值和迁移回退

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeManager.kt`
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/settings/AppSettings.kt`
- 修改：`shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeManagerTest.kt`
- 修改：`shared/src/commonTest/kotlin/com/cleanpic/settings/AppSettingsTest.kt`

- [ ] **步骤 1：更新 ThemeManagerTest**

替换 `shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeManagerTest.kt`：

```kotlin
package com.cleanpic.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeManagerTest {
    @Test fun default_theme_is_warm() {
        val manager = ThemeManager()
        assertEquals("warm", manager.currentTheme.value.id)
        assertEquals(ThemeLayoutId.WARM, manager.currentTheme.value.layoutId)
    }

    @Test fun switch_theme_updates_current() {
        val manager = ThemeManager()
        manager.switchTheme("warm")
        assertEquals("warm", manager.currentTheme.value.id)
    }

    @Test fun unknown_theme_id_falls_back_to_default() {
        val manager = ThemeManager()
        manager.switchTheme("dreamy-gradient")
        assertEquals("warm", manager.currentTheme.value.id)
    }

    @Test fun old_theme_ids_fall_back_to_default() {
        val manager = ThemeManager()
        val oldIds = listOf("dreamy-gradient", "soft-minimal", "cute-playful", "elegant-dark", "natural-warm")
        for (oldId in oldIds) {
            manager.switchTheme(oldId)
            assertEquals("warm", manager.currentTheme.value.id, "旧 ID '$oldId' 应回退到 warm")
        }
    }

    @Test fun all_themes_have_complete_tokens() {
        val manager = ThemeManager()
        manager.allThemes.forEach { theme ->
            assertTrue(theme.colorPrimary != 0L, "${theme.id} 缺少 colorPrimary")
            assertTrue(theme.colorBackground != 0L, "${theme.id} 缺少 colorBackground")
            assertTrue(theme.colorDanger != 0L, "${theme.id} 缺少 colorDanger")
            assertTrue(theme.colorSuccess != 0L, "${theme.id} 缺少 colorSuccess")
            assertTrue(theme.borderRadius >= 0, "${theme.id} borderRadius 无效")
            assertTrue(theme.iconStrokeWidth > 0, "${theme.id} iconStrokeWidth 无效")
        }
    }

    @Test fun all_themes_available() {
        val manager = ThemeManager()
        assertEquals(1, manager.allThemes.size) // 当前仅 WarmTheme；Plan C 添加剩余 4 个
    }
}
```

- [ ] **步骤 2：更新 AppSettingsTest**

替换 `shared/src/commonTest/kotlin/com/cleanpic/settings/AppSettingsTest.kt`：

```kotlin
package com.cleanpic.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsTest {
    @Test fun defaults_are_correct() {
        val settings = InMemoryAppSettings()
        assertEquals("warm", settings.theme)
        assertEquals("carousel", settings.interactionMode)
        assertEquals(10, settings.roundCount)
    }

    @Test fun write_and_read() {
        val settings = InMemoryAppSettings()
        settings.roundCount = 20
        assertEquals(20, settings.roundCount)
    }

    @Test fun invalid_round_count_falls_back() {
        val settings = InMemoryAppSettings()
        settings.roundCount = 99
        assertEquals(10, settings.roundCount)
    }
}
```

- [ ] **步骤 3：运行测试确认失败**

运行：`scripts/test.sh`
预期：失败 — ThemeManager 仍使用旧主题，AppSettings 默认值仍为 "dreamy-gradient"

- [ ] **步骤 4：更新 ThemeManager**

替换 `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeManager.kt`：

```kotlin
package com.cleanpic.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeManager {
    val allThemes = listOf(
        WarmTheme
        // Plan C 将添加：MinimalTheme、GeometricTheme、PlayfulTheme、EditorialTheme
    )

    private val _currentTheme = MutableStateFlow(WarmTheme)
    val currentTheme: StateFlow<ThemeTokens> = _currentTheme

    fun switchTheme(id: String) {
        val target = allThemes.find { it.id == id }
        _currentTheme.value = target ?: WarmTheme
    }
}
```

- [ ] **步骤 5：更新 AppSettings 默认值**

在 `shared/src/commonMain/kotlin/com/cleanpic/settings/AppSettings.kt` 中，修改第 12 行：

```kotlin
override var theme: String = "warm"
```

- [ ] **步骤 6：运行测试确认通过**

运行：`scripts/test.sh`
预期：全部通过

- [ ] **步骤 7：提交**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeManager.kt \
       shared/src/commonMain/kotlin/com/cleanpic/settings/AppSettings.kt \
       shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeManagerTest.kt \
       shared/src/commonTest/kotlin/com/cleanpic/settings/AppSettingsTest.kt
git commit -m "feat: update ThemeManager to use WarmTheme as default with migration fallback"
```

---

### 任务 4：创建 AppIcons 矢量图标系统

**文件：**
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt`
- 新建：`shared/src/commonTest/kotlin/com/cleanpic/icons/AppIconsTest.kt`

- [ ] **步骤 1：写失败的测试**

文件：`shared/src/commonTest/kotlin/com/cleanpic/icons/AppIconsTest.kt`

```kotlin
package com.cleanpic.icons

import com.cleanpic.theme.IconStrokeCap
import com.cleanpic.theme.WarmTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppIconsTest {
    @Test fun all_icons_defined() {
        val iconNames = listOf(
            "back", "delete", "keep", "settings", "play",
            "mute", "unmute", "photo", "video", "refresh",
            "home", "warning", "close"
        )
        for (name in iconNames) {
            val icon = AppIcons.get(name, WarmTheme)
            assertTrue(icon.pathData.isNotEmpty(), "图标 '$name' 没有 path 数据")
        }
    }

    @Test fun icon_params_follow_theme() {
        val icon = AppIcons.get("delete", WarmTheme)
        assertEquals(WarmTheme.iconStrokeWidth, icon.strokeWidth)
        assertEquals(WarmTheme.iconStrokeColor, icon.strokeColor)
        assertEquals(WarmTheme.iconStrokeCap, icon.strokeCap)
    }

    @Test fun unknown_icon_name_throws() {
        try {
            AppIcons.get("nonexistent", WarmTheme)
            assertTrue(false, "应当抛出异常")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("nonexistent"))
        }
    }
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：`scripts/test.sh`
预期：失败 — AppIcons 未定义

- [ ] **步骤 3：实现 AppIcons**

文件：`shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt`

```kotlin
package com.cleanpic.icons

import com.cleanpic.theme.IconStrokeCap
import com.cleanpic.theme.ThemeTokens

/**
 * 主题化矢量图标描述（平台无关数据）。
 * Compose 层通过 toImageVector() 扩展转为 ImageVector 或直接用 Canvas 绘制。
 */
data class IconDef(
    val name: String,
    val pathData: String,
    val viewportWidth: Float = 24f,
    val viewportHeight: Float = 24f,
    val strokeWidth: Float,
    val strokeColor: Long,
    val strokeCap: IconStrokeCap
)

/**
 * 统一图标入口 — 根据名称和主题返回 IconDef。
 */
object AppIcons {

    private val paths = mapOf(
        "back" to "M19 12H5M12 19l-7-7 7-7",
        "delete" to "M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2",
        "keep" to "M20 6L9 17l-5-5",
        "settings" to "M12 15a3 3 0 100-6 3 3 0 000 6zM19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09a1.65 1.65 0 00-1-1.51 1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09a1.65 1.65 0 001.51-1 1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z",
        "play" to "M5 3l14 9-14 9V3z",
        "mute" to "M11 5L6 9H2v6h4l5 4V5zM23 9l-6 6M17 9l6 6",
        "unmute" to "M11 5L6 9H2v6h4l5 4V5zM19.07 4.93a10 10 0 010 14.14M15.54 8.46a5 5 0 010 7.07",
        "photo" to "M3 3h18v18H3V3zM8.5 8.5a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM21 15l-5-5L5 21",
        "video" to "M23 7l-7 5 7 5V7zM1 5h15v14H1V5z",
        "refresh" to "M23 4v6h-6M1 20v-6h6M20.49 9A9 9 0 005.64 5.64L1 10M22.99 14l-4.64 4.36A9 9 0 013.51 15",
        "home" to "M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z",
        "warning" to "M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0zM12 9v4M12 17h.01",
        "close" to "M18 6L6 18M6 6l12 12"
    )

    fun get(name: String, theme: ThemeTokens): IconDef {
        val pathData = paths[name]
            ?: throw IllegalArgumentException("未知图标：$name")
        return IconDef(
            name = name,
            pathData = pathData,
            strokeWidth = theme.iconStrokeWidth,
            strokeColor = theme.iconStrokeColor,
            strokeCap = theme.iconStrokeCap
        )
    }

    /** 所有可用的图标名称 */
    val allNames: Set<String> get() = paths.keys
}
```

- [ ] **步骤 4：运行测试确认通过**

运行：`scripts/test.sh`
预期：全部通过

- [ ] **步骤 5：提交**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt \
       shared/src/commonTest/kotlin/com/cleanpic/icons/AppIconsTest.kt
git commit -m "feat: add AppIcons vector icon system with 13 themed icons"
```

---

### 任务 5：创建共享页面 State 接口

**文件：**
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt`
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreenState.kt`
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt`
- 新建：`shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreenState.kt`

- [ ] **步骤 1：创建 HomeScreenState**

文件：`shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt`

```kotlin
package com.cleanpic.ui.home

import com.cleanpic.theme.ThemeTokens

/**
 * 首页的共享状态 — 5 个布局变体通过此接口接收数据和回调。
 * 业务逻辑保留在 HomeScreen composable 中。
 */
data class HomeScreenState(
    val theme: ThemeTokens,
    val isLimitedAccess: Boolean,
    val onStartPhoto: () -> Unit,
    val onStartVideo: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onShowDeniedDialog: () -> Unit,
    val onShowPermanentDialog: () -> Unit
)
```

- [ ] **步骤 2：创建 ResultScreenState**

文件：`shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreenState.kt`

```kotlin
package com.cleanpic.ui.result

import com.cleanpic.model.MediaItem
import com.cleanpic.theme.ThemeTokens

/**
 * 结果页的共享状态 — 5 个布局变体通过此接口接收数据和回调。
 */
data class ResultScreenState(
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
    val onGoHome: () -> Unit
)
```

- [ ] **步骤 3：创建 SettingsScreenState**

文件：`shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt`

```kotlin
package com.cleanpic.ui.settings

import com.cleanpic.theme.ThemeTokens

/**
 * 设置页的共享状态 — 5 个布局变体通过此接口接收数据和回调。
 */
data class SettingsScreenState(
    val theme: ThemeTokens,
    val allThemes: List<ThemeTokens>,
    val currentMode: String,
    val currentCount: Int,
    val onThemeChange: (String) -> Unit,
    val onModeChange: (String) -> Unit,
    val onCountChange: (Int) -> Unit,
    val onBack: () -> Unit
)
```

- [ ] **步骤 4：创建 SplashScreenState**

文件：`shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreenState.kt`

```kotlin
package com.cleanpic.ui.splash

import com.cleanpic.theme.ThemeTokens

/**
 * 闪屏的共享状态 — 5 个布局变体通过此接口接收主题和完成回调。
 */
data class SplashScreenState(
    val theme: ThemeTokens,
    val onSplashComplete: () -> Unit
)
```

- [ ] **步骤 5：验证编译**

运行：`scripts/build-android.sh`
预期：构建成功（State 类是 data class，仅依赖已有的 model 类型）

- [ ] **步骤 6：提交**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt \
       shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreenState.kt \
       shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt \
       shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreenState.kt
git commit -m "feat: add shared page state interfaces for theme layout dispatch"
```

---

### 任务 6：删除旧主题文件

**文件：**
- 删除：`shared/src/commonMain/kotlin/com/cleanpic/theme/DreamyGradient.kt`
- 删除：`shared/src/commonMain/kotlin/com/cleanpic/theme/SoftMinimal.kt`
- 删除：`shared/src/commonMain/kotlin/com/cleanpic/theme/CutePlayful.kt`
- 删除：`shared/src/commonMain/kotlin/com/cleanpic/theme/ElegantDark.kt`
- 删除：`shared/src/commonMain/kotlin/com/cleanpic/theme/NaturalWarm.kt`

- [ ] **步骤 1：删除旧主题文件**

```bash
rm shared/src/commonMain/kotlin/com/cleanpic/theme/DreamyGradient.kt
rm shared/src/commonMain/kotlin/com/cleanpic/theme/SoftMinimal.kt
rm shared/src/commonMain/kotlin/com/cleanpic/theme/CutePlayful.kt
rm shared/src/commonMain/kotlin/com/cleanpic/theme/ElegantDark.kt
rm shared/src/commonMain/kotlin/com/cleanpic/theme/NaturalWarm.kt
```

- [ ] **步骤 2：运行测试确认不受影响**

运行：`scripts/test.sh`
预期：全部通过（ThemeManager 现在只引用 WarmTheme）

- [ ] **步骤 3：验证构建**

运行：`scripts/build-android.sh`
预期：构建成功

- [ ] **步骤 4：提交**

```bash
git add -u shared/src/commonMain/kotlin/com/cleanpic/theme/
git commit -m "refactor: remove 5 old theme definitions (replaced by v2 themes)"
```

---

### 任务 7：全量验证测试和构建

- [ ] **步骤 1：运行完整单元测试**

运行：`scripts/test.sh`
预期：全部通过

- [ ] **步骤 2：运行完整构建**

运行：`scripts/build-android.sh`
预期：构建成功

如果出现因代码中其他地方引用旧主题名导致的编译错误，记录下来——将在 Plan B 重写页面 composable 时解决。

- [ ] **步骤 3：如有修复则提交**

```bash
git add -A && git commit -m "fix: resolve any remaining old theme references"
```

---

## Plan A 完成检查清单

所有任务完成后确认：
- [ ] ThemeTokens 包含 ThemeLayoutId、ProgressStyle、ButtonStyle、IconStrokeCap 枚举
- [ ] WarmTheme 使用所有 v2 字段完整定义
- [ ] ThemeManager 默认使用 WarmTheme，未知 ID 回退
- [ ] AppSettings 默认主题为 "warm"
- [ ] AppIcons 包含 13 个主题化矢量图标
- [ ] 4 个共享页面 State 类已创建（Home/Result/Settings/Splash）
- [ ] 旧 5 个主题文件已删除
- [ ] 所有单元测试通过
- [ ] 构建成功
