# UI Theme Redesign — Plan A: Infrastructure

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the theme system with layout identifiers, typed enums, vector icon system, and shared page state interfaces — the foundation all 5 new themes build on.

**Architecture:** ThemeTokens gets new fields (layoutId, iconStroke*, progressStyle, buttonStyle, titleFontFamily). Each page defines a shared State class that encapsulates business logic, so the 5 layout variants only handle UI. AppIcons provides themed ImageVector icons replacing all emoji.

**Tech Stack:** Kotlin 2.1.21, Compose Multiplatform 1.7.3, kotlin.test

**Spec:** `docs/superpowers/specs/2026-04-04-ui-theme-redesign-design.md`

**Depends on:** Nothing (this is the first plan)
**Blocks:** Plan B (warm theme end-to-end), Plan C (remaining 4 themes)

---

## File Map

| Action | File | Responsibility |
|--------|------|---------------|
| Modify | `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeTokens.kt` | Add enums + new fields |
| Modify | `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeManager.kt` | Update default, add migration fallback |
| Modify | `shared/src/commonMain/kotlin/com/cleanpic/settings/AppSettings.kt` | Update default theme to "warm" |
| Create | `shared/src/commonMain/kotlin/com/cleanpic/theme/WarmTheme.kt` | First new theme definition |
| Create | `shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt` | Unified vector icon entry point |
| Create | `shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt` | Shared home page state |
| Create | `shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreenState.kt` | Shared result page state |
| Create | `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt` | Shared settings page state |
| Create | `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreenState.kt` | Shared splash page state |
| Modify | `shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeManagerTest.kt` | Update tests for new defaults + migration |
| Modify | `shared/src/commonTest/kotlin/com/cleanpic/settings/AppSettingsTest.kt` | Update default theme assertion |
| Create | `shared/src/commonTest/kotlin/com/cleanpic/icons/AppIconsTest.kt` | Test icon generation per theme |

---

### Task 1: Extend ThemeTokens with enums and new fields

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeTokens.kt`

- [ ] **Step 1: Write the failing test**

Create test that validates new enum types and fields exist:

File: `shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeTokensTest.kt`

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

- [ ] **Step 2: Run test to verify it fails**

Run: `scripts/test.sh`
Expected: FAIL — ThemeLayoutId, ProgressStyle, ButtonStyle not defined

- [ ] **Step 3: Implement ThemeTokens extension**

Replace `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeTokens.kt` with:

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
    // v2: 布局与图标
    val layoutId: ThemeLayoutId = ThemeLayoutId.WARM,
    val iconStrokeWidth: Float = 1.8f,
    val iconStrokeColor: Long = 0xFF333333,
    val iconStrokeCap: IconStrokeCap = IconStrokeCap.ROUND,
    val progressStyle: ProgressStyle = ProgressStyle.SOFT,
    val buttonStyle: ButtonStyle = ButtonStyle.SHADOW
)
```

Note: New fields have defaults so existing theme definitions still compile without modification.

- [ ] **Step 4: Run tests to verify they pass**

Run: `scripts/test.sh`
Expected: ALL PASS (new tests + existing tests, since defaults preserve backward compatibility)

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeTokens.kt \
       shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeTokensTest.kt
git commit -m "feat: extend ThemeTokens with layout/icon/style enums and fields"
```

---

### Task 2: Create WarmTheme definition

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/theme/WarmTheme.kt`

- [ ] **Step 1: Write the failing test**

Add to `shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeTokensTest.kt`:

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

- [ ] **Step 2: Run test to verify it fails**

Run: `scripts/test.sh`
Expected: FAIL — WarmTheme not defined

- [ ] **Step 3: Create WarmTheme**

File: `shared/src/commonMain/kotlin/com/cleanpic/theme/WarmTheme.kt`

```kotlin
package com.cleanpic.theme

/**
 * 温暖手工感主题 — 暖色调、大圆角、柔和阴影、衬线标题
 * 参考: Bear / Things
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

- [ ] **Step 4: Run tests to verify they pass**

Run: `scripts/test.sh`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/theme/WarmTheme.kt \
       shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeTokensTest.kt
git commit -m "feat: add WarmTheme as first v2 theme definition"
```

---

### Task 3: Update ThemeManager defaults and add migration fallback

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeManager.kt`
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/settings/AppSettings.kt`
- Modify: `shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeManagerTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/cleanpic/settings/AppSettingsTest.kt`

- [ ] **Step 1: Update ThemeManagerTest**

Replace `shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeManagerTest.kt`:

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
            assertEquals("warm", manager.currentTheme.value.id, "Old ID '$oldId' should fall back to warm")
        }
    }

    @Test fun all_themes_have_complete_tokens() {
        val manager = ThemeManager()
        manager.allThemes.forEach { theme ->
            assertTrue(theme.colorPrimary != 0L, "${theme.id} missing colorPrimary")
            assertTrue(theme.colorBackground != 0L, "${theme.id} missing colorBackground")
            assertTrue(theme.colorDanger != 0L, "${theme.id} missing colorDanger")
            assertTrue(theme.colorSuccess != 0L, "${theme.id} missing colorSuccess")
            assertTrue(theme.borderRadius >= 0, "${theme.id} invalid borderRadius")
            assertTrue(theme.iconStrokeWidth > 0, "${theme.id} invalid iconStrokeWidth")
        }
    }

    @Test fun all_themes_available() {
        val manager = ThemeManager()
        assertEquals(1, manager.allThemes.size) // Only WarmTheme for now; Plan C adds remaining 4
    }
}
```

- [ ] **Step 2: Update AppSettingsTest**

Replace `shared/src/commonTest/kotlin/com/cleanpic/settings/AppSettingsTest.kt`:

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

- [ ] **Step 3: Run tests to verify they fail**

Run: `scripts/test.sh`
Expected: FAIL — ThemeManager still uses old themes, AppSettings default is still "dreamy-gradient"

- [ ] **Step 4: Update ThemeManager**

Replace `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeManager.kt`:

```kotlin
package com.cleanpic.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeManager {
    val allThemes = listOf(
        WarmTheme
        // Plan C will add: MinimalTheme, GeometricTheme, PlayfulTheme, EditorialTheme
    )

    private val _currentTheme = MutableStateFlow(WarmTheme)
    val currentTheme: StateFlow<ThemeTokens> = _currentTheme

    fun switchTheme(id: String) {
        val target = allThemes.find { it.id == id }
        _currentTheme.value = target ?: WarmTheme
    }
}
```

- [ ] **Step 5: Update AppSettings default**

In `shared/src/commonMain/kotlin/com/cleanpic/settings/AppSettings.kt`, change line 12:

```kotlin
override var theme: String = "warm"
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `scripts/test.sh`
Expected: ALL PASS

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeManager.kt \
       shared/src/commonMain/kotlin/com/cleanpic/settings/AppSettings.kt \
       shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeManagerTest.kt \
       shared/src/commonTest/kotlin/com/cleanpic/settings/AppSettingsTest.kt
git commit -m "feat: update ThemeManager to use WarmTheme as default with migration fallback"
```

---

### Task 4: Create AppIcons vector icon system

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt`
- Create: `shared/src/commonTest/kotlin/com/cleanpic/icons/AppIconsTest.kt`

- [ ] **Step 1: Write the failing test**

File: `shared/src/commonTest/kotlin/com/cleanpic/icons/AppIconsTest.kt`

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
            assertTrue(icon.pathData.isNotEmpty(), "Icon '$name' has no path data")
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
            assertTrue(false, "Should have thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("nonexistent"))
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `scripts/test.sh`
Expected: FAIL — AppIcons not defined

- [ ] **Step 3: Implement AppIcons**

File: `shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt`

```kotlin
package com.cleanpic.icons

import com.cleanpic.theme.IconStrokeCap
import com.cleanpic.theme.ThemeTokens

/**
 * 描述一个主题化的矢量图标（平台无关数据）。
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
            ?: throw IllegalArgumentException("Unknown icon: $name")
        return IconDef(
            name = name,
            pathData = pathData,
            strokeWidth = theme.iconStrokeWidth,
            strokeColor = theme.iconStrokeColor,
            strokeCap = theme.iconStrokeCap
        )
    }

    /** All available icon names */
    val allNames: Set<String> get() = paths.keys
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `scripts/test.sh`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/icons/AppIcons.kt \
       shared/src/commonTest/kotlin/com/cleanpic/icons/AppIconsTest.kt
git commit -m "feat: add AppIcons vector icon system with 13 themed icons"
```

---

### Task 5: Create shared page State interfaces

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreenState.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreenState.kt`

- [ ] **Step 1: Create HomeScreenState**

File: `shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt`

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

- [ ] **Step 2: Create ResultScreenState**

File: `shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreenState.kt`

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

- [ ] **Step 3: Create SettingsScreenState**

File: `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt`

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

- [ ] **Step 4: Create SplashScreenState**

File: `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreenState.kt`

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

- [ ] **Step 5: Verify compilation**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL (state classes are data classes, no external dependencies beyond existing model types)

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreenState.kt \
       shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreenState.kt \
       shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt \
       shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreenState.kt
git commit -m "feat: add shared page state interfaces for theme layout dispatch"
```

---

### Task 6: Delete old theme files

**Files:**
- Delete: `shared/src/commonMain/kotlin/com/cleanpic/theme/DreamyGradient.kt`
- Delete: `shared/src/commonMain/kotlin/com/cleanpic/theme/SoftMinimal.kt`
- Delete: `shared/src/commonMain/kotlin/com/cleanpic/theme/CutePlayful.kt`
- Delete: `shared/src/commonMain/kotlin/com/cleanpic/theme/ElegantDark.kt`
- Delete: `shared/src/commonMain/kotlin/com/cleanpic/theme/NaturalWarm.kt`

- [ ] **Step 1: Delete old theme files**

```bash
rm shared/src/commonMain/kotlin/com/cleanpic/theme/DreamyGradient.kt
rm shared/src/commonMain/kotlin/com/cleanpic/theme/SoftMinimal.kt
rm shared/src/commonMain/kotlin/com/cleanpic/theme/CutePlayful.kt
rm shared/src/commonMain/kotlin/com/cleanpic/theme/ElegantDark.kt
rm shared/src/commonMain/kotlin/com/cleanpic/theme/NaturalWarm.kt
```

- [ ] **Step 2: Run tests to verify nothing breaks**

Run: `scripts/test.sh`
Expected: ALL PASS (ThemeManager now only references WarmTheme)

- [ ] **Step 3: Verify build**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -u shared/src/commonMain/kotlin/com/cleanpic/theme/
git commit -m "refactor: remove 5 old theme definitions (replaced by v2 themes)"
```

---

### Task 7: Verify full test suite and build

- [ ] **Step 1: Run full unit tests**

Run: `scripts/test.sh`
Expected: ALL PASS

- [ ] **Step 2: Run full build**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

If any compilation errors occur due to old theme references elsewhere in the codebase (e.g., settings UI referencing old theme names), note them — they will be resolved in Plan B when we rewrite the screen composables.

- [ ] **Step 3: Final commit if any fixups needed**

```bash
git add -A && git commit -m "fix: resolve any remaining old theme references"
```

---

## Plan A Completion Checklist

After all tasks:
- [ ] ThemeTokens has ThemeLayoutId, ProgressStyle, ButtonStyle, IconStrokeCap enums
- [ ] WarmTheme defined with all v2 fields
- [ ] ThemeManager defaults to WarmTheme, unknown IDs fall back
- [ ] AppSettings default theme is "warm"
- [ ] AppIcons has 13 vector icons with theme-aware parameters
- [ ] 4 shared page State classes created (Home/Result/Settings/Splash)
- [ ] Old 5 theme files deleted
- [ ] All unit tests pass
- [ ] Build succeeds
