# CleanPic 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个 KuiklyUI 跨平台相册/视频随机清理 App，支持 Android/iOS/HarmonyOS 三端，含 5 套主题和 3 种交互模式。

**Architecture:** 基于 KuiklyUI (Kotlin Multiplatform) 的 Compose DSL，共享层包含全部 UI + 业务逻辑，各平台壳工程仅实现 Native Module（媒体访问、权限、视频播放）。采用 ViewModel + StateFlow 管理状态，NavHost 管理导航。

**Tech Stack:** KuiklyUI Compose DSL / Kotlin Multiplatform / Gradle KTS / kotlin.test

**Spec:** `docs/architecture/overview.md` + 子文档

---

## 文件结构

```
cleanpic/
├── shared/src/commonMain/kotlin/com/cleanpic/
│   ├── App.kt                         # NavHost 路由入口
│   ├── model/
│   │   ├── MediaItem.kt               # 媒体数据模型
│   │   ├── OperationState.kt          # 操作状态枚举
│   │   └── InteractionMode.kt         # 交互模式枚举
│   ├── theme/
│   │   ├── ThemeTokens.kt             # Token 接口定义
│   │   ├── ThemeManager.kt            # 主题状态管理
│   │   ├── DreamyGradient.kt          # 梦幻渐变(默认)
│   │   ├── SoftMinimal.kt             # 柔和极简
│   │   ├── CutePlayful.kt            # 可爱活泼
│   │   ├── ElegantDark.kt            # 优雅暗黑
│   │   └── NaturalWarm.kt            # 自然温暖
│   ├── media/
│   │   ├── MediaRepository.kt         # 媒体仓库接口
│   │   ├── RandomPicker.kt            # 随机选取逻辑
│   │   └── VideoPlayer.kt             # 视频播放器接口
│   ├── permission/
│   │   └── PermissionManager.kt       # 权限管理接口 + 状态枚举
│   ├── settings/
│   │   └── AppSettings.kt             # 偏好存储接口
│   ├── di/
│   │   └── ServiceLocator.kt          # 平台依赖注入定位器
│   ├── viewmodel/
│   │   ├── HomeViewModel.kt           # 首页 VM
│   │   ├── ViewerViewModel.kt         # 浏览页 VM
│   │   ├── ResultViewModel.kt         # 结果页 VM
│   │   └── SettingsViewModel.kt       # 设置页 VM
│   └── ui/
│       ├── splash/SplashScreen.kt     # 启动页
│       ├── home/HomeScreen.kt         # 首页
│       ├── viewer/
│       │   ├── ViewerScreen.kt        # 浏览页容器
│       │   ├── CarouselMode.kt        # 轮播相册式
│       │   ├── SwipeCardMode.kt       # 卡片左右滑
│       │   └── FullscreenMode.kt      # 全屏上下滑
│       ├── result/ResultScreen.kt     # 结果页
│       └── settings/SettingsScreen.kt # 设置页
├── shared/src/commonTest/kotlin/com/cleanpic/
│   ├── mock/
│   │   ├── MockMediaRepository.kt     # 内存假媒体列表
│   │   ├── MockAppSettings.kt         # 内存假偏好存储
│   │   └── TestMediaFactory.kt        # 测试数据工厂
│   ├── media/RandomPickerTest.kt
│   ├── theme/ThemeManagerTest.kt
│   ├── settings/AppSettingsTest.kt
│   └── viewmodel/ViewerViewModelTest.kt
├── shared/src/androidMain/kotlin/com/cleanpic/
│   ├── media/AndroidMediaRepository.kt
│   ├── media/AndroidVideoPlayer.kt
│   ├── settings/AndroidAppSettings.kt
│   ├── permission/AndroidPermission.kt
│   └── di/AndroidServiceLocator.kt
├── shared/src/appleMain/kotlin/com/cleanpic/
│   ├── media/IosMediaRepository.kt
│   ├── media/IosVideoPlayer.kt
│   ├── settings/IosAppSettings.kt
│   ├── permission/IosPermission.kt
│   └── di/IosServiceLocator.kt
├── shared/src/ohosArm64Main/kotlin/com/cleanpic/
│   ├── media/HarmonyMediaRepository.kt
│   ├── media/HarmonyVideoPlayer.kt
│   ├── settings/HarmonyAppSettings.kt
│   ├── permission/HarmonyPermission.kt
│   └── di/HarmonyServiceLocator.kt
├── androidApp/                         # Android 壳工程
├── iosApp/                             # iOS 壳工程
├── ohosApp/                            # HarmonyOS 壳工程
├── scripts/
│   ├── build-android.sh
│   ├── build-ios.sh
│   ├── build-harmony.sh
│   ├── test.sh
│   └── run-android.sh
└── docs/                               # 已有文档体系
```

---

## Task 1: 项目脚手架

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
- Create: `shared/build.gradle.kts`
- Create: `scripts/test.sh`, `scripts/build-android.sh`

- [ ] **Step 1: 通过 Kuikly 插件创建项目**

在 Android Studio 中：File → New → New Project → Kuikly Project Template → 选择 Compose DSL → 项目名 `cleanpic`，包名 `com.cleanpic`

- [ ] **Step 2: 验证项目结构**

```bash
ls -la shared/src/commonMain/kotlin/
```
Expected: 目录存在

- [ ] **Step 3: 创建构建/运行脚本**

`scripts/test.sh`:
```bash
#!/bin/bash
set -e
cd "$(dirname "$0")/.."
./gradlew :shared:allTests --info 2>&1 | tee logs/test.log
```

`scripts/build-android.sh`:
```bash
#!/bin/bash
set -e
cd "$(dirname "$0")/.."
./gradlew :androidApp:assembleDebug 2>&1 | tee logs/build-android.log
```

- [ ] **Step 4: 创建 logs 目录和 .gitignore**

```bash
mkdir -p logs
echo "logs/" >> .gitignore
echo ".superpowers/" >> .gitignore
```

- [ ] **Step 5: 运行 Gradle Sync 验证**

```bash
./gradlew :shared:compileKotlinMetadata
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git init && git add -A && git commit -m "chore: scaffold KuiklyUI project"
```

---

## Task 2: 核心数据模型

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/model/MediaItem.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/model/OperationState.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/model/InteractionMode.kt`

- [ ] **Step 1: 创建 MediaItem**

```kotlin
package com.cleanpic.model

data class MediaItem(
    val id: String,
    val type: MediaType,
    val name: String,
    val size: Long,
    val date: Long,
    val width: Int,
    val height: Int,
    val duration: Long? = null
)

enum class MediaType { PHOTO, VIDEO }
```

- [ ] **Step 2: 创建 OperationState**

```kotlin
package com.cleanpic.model

enum class OperationState { PENDING, KEPT, PENDING_DELETE }

data class ViewerItem(
    val media: MediaItem,
    val state: OperationState = OperationState.PENDING,
    val thumbnailLoaded: Boolean = false
)
```

- [ ] **Step 3: 创建 InteractionMode**

```kotlin
package com.cleanpic.model

enum class InteractionMode(val id: String, val label: String) {
    CAROUSEL("carousel", "轮播相册式"),
    SWIPE_CARD("swipe-card", "卡片左右滑"),
    FULLSCREEN("fullscreen", "全屏上下滑");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: CAROUSEL
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/model/
git commit -m "feat: add core data models (MediaItem, OperationState, InteractionMode)"
```

---

## Task 3: RandomPicker（TDD）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/media/RandomPicker.kt`
- Test: `shared/src/commonTest/kotlin/com/cleanpic/media/RandomPickerTest.kt`

- [ ] **Step 1: 写全部失败测试**

```kotlin
package com.cleanpic.media

import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomPickerTest {
    private fun makeItems(n: Int) = (1..n).map {
        MediaItem("id_$it", MediaType.PHOTO, "img_$it.jpg", 1000L, 0L, 100, 100)
    }

    @Test fun pick_normal_returns_correct_count() {
        val result = RandomPicker.pick(makeItems(100), 10, emptySet())
        assertEquals(10, result.size)
    }
    @Test fun pick_normal_no_duplicates() {
        val result = RandomPicker.pick(makeItems(100), 10, emptySet())
        assertEquals(result.size, result.map { it.id }.toSet().size)
    }
    @Test fun pick_insufficient_returns_all() {
        val result = RandomPicker.pick(makeItems(3), 10, emptySet())
        assertEquals(3, result.size)
    }
    @Test fun pick_empty_returns_empty() {
        val result = RandomPicker.pick(emptyList(), 10, emptySet())
        assertTrue(result.isEmpty())
    }
    @Test fun pick_excludes_shown_ids() {
        val items = makeItems(15)
        val exclude = items.take(10).map { it.id }.toSet()
        val result = RandomPicker.pick(items, 10, exclude)
        assertEquals(5, result.size)
        assertTrue(result.none { it.id in exclude })
    }
    @Test fun pick_all_excluded_resets() {
        val items = makeItems(10)
        val exclude = items.map { it.id }.toSet()
        val result = RandomPicker.pick(items, 10, exclude)
        assertEquals(10, result.size)
    }
    @Test fun pick_boundary_counts() {
        for (count in listOf(5, 10, 15, 20)) {
            val result = RandomPicker.pick(makeItems(100), count, emptySet())
            assertEquals(count, result.size)
        }
    }
}
```

- [ ] **Step 2: 运行测试验证全部失败**

Run: `scripts/test.sh`
Expected: 7 个 FAIL

- [ ] **Step 3: 实现 RandomPicker**

```kotlin
package com.cleanpic.media

import com.cleanpic.model.MediaItem

object RandomPicker {
    fun pick(
        items: List<MediaItem>,
        count: Int,
        exclude: Set<String>
    ): List<MediaItem> {
        if (items.isEmpty()) return emptyList()
        var available = items.filter { it.id !in exclude }
        if (available.isEmpty()) {
            available = items
        }
        return available.shuffled().take(count)
    }
}
```

- [ ] **Step 4: 运行测试验证全部通过**

Run: `scripts/test.sh`
Expected: 7 个 PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/media/RandomPicker.kt \
       shared/src/commonTest/kotlin/com/cleanpic/media/RandomPickerTest.kt
git commit -m "feat: add RandomPicker with TDD (7 test cases)"
```

---

## Task 4: ThemeTokens + ThemeManager（TDD）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeTokens.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/theme/ThemeManager.kt`
- Create: 5 个主题文件
- Test: `shared/src/commonTest/kotlin/com/cleanpic/theme/ThemeManagerTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.cleanpic.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThemeManagerTest {
    @Test fun default_theme_is_dreamy_gradient() {
        val manager = ThemeManager()
        assertEquals("dreamy-gradient", manager.currentTheme.value.id)
    }
    @Test fun switch_theme_updates_current() {
        val manager = ThemeManager()
        manager.switchTheme("elegant-dark")
        assertEquals("elegant-dark", manager.currentTheme.value.id)
    }
    @Test fun all_themes_have_complete_tokens() {
        val manager = ThemeManager()
        manager.allThemes.forEach { theme ->
            assertNotNull(theme.colorPrimary, "${theme.id} missing colorPrimary")
            assertNotNull(theme.colorBackground, "${theme.id} missing colorBackground")
            assertNotNull(theme.colorDanger, "${theme.id} missing colorDanger")
            assertNotNull(theme.colorSuccess, "${theme.id} missing colorSuccess")
            assertTrue(theme.borderRadius > 0, "${theme.id} invalid borderRadius")
        }
    }
    @Test fun all_five_themes_available() {
        val manager = ThemeManager()
        assertEquals(5, manager.allThemes.size)
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `scripts/test.sh`
Expected: 4 个 FAIL

- [ ] **Step 3: 实现 ThemeTokens 接口**

`ThemeTokens.kt`:
```kotlin
package com.cleanpic.theme

data class ThemeTokens(
    val id: String,
    val name: String,
    val colorPrimary: Long,
    val colorAccent: Long,
    val colorBackground: Long,
    val colorSurface: Long,
    val colorDanger: Long,
    val colorSuccess: Long,
    val colorText: Long,
    val colorTextSecondary: Long,
    val gradientMain: GradientDef?,
    val borderRadius: Float,
    val shadowStyle: ShadowDef,
    val fontFamily: String,
    val animDuration: Long,
    val animEasing: String,
    val animButtonPress: ButtonPressAnim
)

data class GradientDef(val angle: Float, val colors: List<Long>)
data class ShadowDef(val offsetX: Float, val offsetY: Float, val blur: Float, val color: Long)
enum class ButtonPressAnim { SCALE, BOUNCE, NONE }
```

- [ ] **Step 4: 实现 5 套主题**

`DreamyGradient.kt`:
```kotlin
package com.cleanpic.theme

val DreamyGradientTheme = ThemeTokens(
    id = "dreamy-gradient", name = "梦幻渐变",
    colorPrimary = 0xFFC4B5FD, colorAccent = 0xFFF9A8D4,
    colorBackground = 0xFFEDE9FE, colorSurface = 0x99FFFFFF,
    colorDanger = 0xFFEF4444, colorSuccess = 0xFF22C55E,
    colorText = 0xFF4C1D95, colorTextSecondary = 0xFF7C3AED,
    borderRadius = 16f, animButtonPress = ButtonPressAnim.SCALE
)
```

（其他 4 套主题同理，每个文件仅含一个 val 定义）

- [ ] **Step 5: 实现 ThemeManager**

```kotlin
package com.cleanpic.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeManager {
    val allThemes = listOf(
        DreamyGradientTheme, SoftMinimalTheme, CutePlayfulTheme,
        ElegantDarkTheme, NaturalWarmTheme
    )
    private val _currentTheme = MutableStateFlow(DreamyGradientTheme)
    val currentTheme: StateFlow<ThemeTokens> = _currentTheme

    fun switchTheme(id: String) {
        allThemes.find { it.id == id }?.let { _currentTheme.value = it }
    }
}
```

- [ ] **Step 6: 运行测试验证通过**

Run: `scripts/test.sh`
Expected: 4 个 PASS

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/theme/ \
       shared/src/commonTest/kotlin/com/cleanpic/theme/
git commit -m "feat: add theme system with 5 themes (TDD, 4 tests)"
```

---

## Task 5: AppSettings 接口 + Mock（TDD）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/settings/AppSettings.kt`
- Test: `shared/src/commonTest/kotlin/com/cleanpic/settings/AppSettingsTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.cleanpic.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsTest {
    @Test fun defaults_are_correct() {
        val settings = InMemoryAppSettings()
        assertEquals("dreamy-gradient", settings.theme)
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

- [ ] **Step 2: 实现 AppSettings 接口 + InMemoryAppSettings**

```kotlin
package com.cleanpic.settings

interface AppSettings {
    var theme: String
    var interactionMode: String
    var roundCount: Int
}

private val VALID_COUNTS = setOf(5, 10, 15, 20)

class InMemoryAppSettings : AppSettings {
    override var theme: String = "dreamy-gradient"
    override var interactionMode: String = "carousel"
    private var _roundCount: Int = 10
    override var roundCount: Int
        get() = _roundCount
        set(value) { _roundCount = if (value in VALID_COUNTS) value else 10 }
}
```

- [ ] **Step 3: 运行测试验证通过**

Run: `scripts/test.sh`
Expected: 3 个 PASS

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/settings/ \
       shared/src/commonTest/kotlin/com/cleanpic/settings/
git commit -m "feat: add AppSettings interface with validation (TDD, 3 tests)"
```

---

## Task 6: DI + PermissionManager + VideoPlayer 接口

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/permission/PermissionManager.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/media/VideoPlayer.kt`

- [ ] **Step 1: 定义 ServiceLocator（平台依赖注入）**

```kotlin
package com.cleanpic.di

import com.cleanpic.media.MediaRepository
import com.cleanpic.media.VideoPlayer
import com.cleanpic.permission.PermissionManager
import com.cleanpic.settings.AppSettings

expect object ServiceLocator {
    val mediaRepository: MediaRepository
    val appSettings: AppSettings
    val permissionManager: PermissionManager
    val videoPlayer: VideoPlayer
}
```

- [ ] **Step 2: 定义 PermissionManager 接口**

```kotlin
package com.cleanpic.permission

enum class PermissionStatus { GRANTED, LIMITED, DENIED, PERMANENTLY_DENIED }

interface PermissionManager {
    suspend fun requestPhotoPermission(): PermissionStatus
    fun checkPermissionStatus(): PermissionStatus
    fun openAppSettings()
}
```

- [ ] **Step 3: 定义 VideoPlayer 接口**

```kotlin
package com.cleanpic.media

interface VideoPlayer {
    fun prepare(mediaId: String)
    fun play()
    fun pause()
    fun release()
    fun setMuted(muted: Boolean)
    val isPlaying: Boolean
    val currentPosition: Long
    val duration: Long
}
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/di/ \
       shared/src/commonMain/kotlin/com/cleanpic/permission/ \
       shared/src/commonMain/kotlin/com/cleanpic/media/VideoPlayer.kt
git commit -m "feat: add ServiceLocator, PermissionManager, VideoPlayer interfaces"
```

---

## Task 7: MediaRepository 接口 + ViewerViewModel

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/media/MediaRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/viewmodel/ViewerViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/viewmodel/HomeViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/viewmodel/ResultViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/viewmodel/SettingsViewModel.kt`

- [ ] **Step 1: 定义 MediaRepository 接口**

```kotlin
package com.cleanpic.media

import com.cleanpic.model.MediaItem

interface MediaRepository {
    suspend fun queryPhotos(): List<MediaItem>
    suspend fun queryVideos(): List<MediaItem>
    suspend fun getThumbnail(id: String): ByteArray?
    suspend fun getFullImage(id: String): ByteArray?
    suspend fun deleteMedia(ids: List<String>): Result<Int>
}
```

- [ ] **Step 2: 实现 ViewerViewModel**

```kotlin
package com.cleanpic.viewmodel

import com.cleanpic.media.MediaRepository
import com.cleanpic.media.RandomPicker
import com.cleanpic.model.*
import com.cleanpic.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ViewerViewModel(
    private val repo: MediaRepository,
    private val settings: AppSettings
) {
    private val _items = MutableStateFlow<List<ViewerItem>>(emptyList())
    val items: StateFlow<List<ViewerItem>> = _items
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex
    private val _shownIds = mutableSetOf<String>()

    suspend fun loadMedia(type: MediaType) {
        val all = when (type) {
            MediaType.PHOTO -> repo.queryPhotos()
            MediaType.VIDEO -> repo.queryVideos()
        }
        val picked = RandomPicker.pick(all, settings.roundCount, _shownIds)
        _shownIds.addAll(picked.map { it.id })
        _items.value = picked.map { ViewerItem(it) }
        _currentIndex.value = 0
    }

    fun markKept() { updateCurrent(OperationState.KEPT); advance() }
    fun markDelete() { updateCurrent(OperationState.PENDING_DELETE); advance() }

    val isComplete: Boolean get() = _currentIndex.value >= _items.value.size
    val pendingDeletes: List<MediaItem>
        get() = _items.value.filter { it.state == OperationState.PENDING_DELETE }.map { it.media }
    val keptCount: Int get() = _items.value.count { it.state == OperationState.KEPT }
    val deletedCount: Int get() = pendingDeletes.size
    val releasedBytes: Long get() = pendingDeletes.sumOf { it.size }

    fun cancelDelete(id: String) {
        _items.value = _items.value.map {
            if (it.media.id == id) it.copy(state = OperationState.KEPT) else it
        }
    }

    suspend fun confirmDelete(): Result<Int> {
        val ids = pendingDeletes.map { it.id }
        if (ids.isEmpty()) return Result.success(0)
        return repo.deleteMedia(ids)
    }

    fun resetForNextRound() { _currentIndex.value = 0 }
    fun clearSession() { _shownIds.clear() }

    private fun updateCurrent(state: OperationState) {
        val idx = _currentIndex.value
        val list = _items.value.toMutableList()
        if (idx < list.size) { list[idx] = list[idx].copy(state = state); _items.value = list }
    }
    private fun advance() {
        if (_currentIndex.value < _items.value.size) _currentIndex.value++
    }
}
```

- [ ] **Step 3: 实现其他 ViewModel（骨架）**

HomeViewModel、ResultViewModel、SettingsViewModel 以最小骨架创建，后续 Task 中填充 UI 时完善。

- [ ] **Step 4: 编译验证**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/media/MediaRepository.kt \
       shared/src/commonMain/kotlin/com/cleanpic/viewmodel/
git commit -m "feat: add MediaRepository interface and ViewerViewModel"
```

---

## Task 7: UI — 启动页 + 首页

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/home/HomeScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/App.kt`

- [ ] **Step 1: 实现 SplashScreen**

带 Logo 和主题渐变背景，1.5 秒后自动跳转。

- [ ] **Step 2: 实现 HomeScreen**

极简布局：Logo + 应用名 + 两个大圆角按钮（清理照片/清理视频）+ 设置入口。按钮使用当前主题 Token 渲染。

- [ ] **Step 3: 实现 App.kt NavHost 路由**

```kotlin
@Composable
fun CleanPicApp(themeManager: ThemeManager) {
    val theme by themeManager.currentTheme.collectAsState()
    val navController = rememberNavController()
    NavHost(navController, startDestination = "splash") {
        composable("splash") { SplashScreen(onTimeout = { navController.navigate("home") }) }
        composable("home") { HomeScreen(navController, theme) }
        composable("viewer/{type}") { entry ->
            val type = MediaType.valueOf(entry.arguments?.getString("type") ?: "PHOTO")
            ViewerScreen(navController, theme, type)
        }
        composable("result") { ResultScreen(navController, theme) }
        composable("settings") { SettingsScreen(navController, themeManager) }
    }
}
```

- [ ] **Step 4: Android 壳工程集成验证**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/App.kt \
       shared/src/commonMain/kotlin/com/cleanpic/ui/splash/ \
       shared/src/commonMain/kotlin/com/cleanpic/ui/home/
git commit -m "feat: add SplashScreen, HomeScreen, and NavHost routing"
```

---

## Task 9: UI — ViewerScreen 容器 + 空状态页

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/ViewerScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/common/EmptyStateScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/common/PermissionBanner.kt`

- [ ] **Step 1: 实现 EmptyStateScreen（空状态页）**

接收主题 Token，展示占位插画 + 提示文案 + 返回按钮。用于相册为空或视频为空场景。

- [ ] **Step 2: 实现 PermissionBanner（部分权限提示条）**

当 PermissionStatus == LIMITED 时，在首页顶部展示"当前仅能访问部分照片，点击授权全部"提示条。

- [ ] **Step 3: 实现 ViewerScreen 容器**

根据 AppSettings.interactionMode 分发到 CarouselMode/SwipeCardMode/FullscreenMode。顶部进度条 + 计数。处理加载状态、空状态分支。响应系统"减少动态效果"偏好关闭装饰性动画。

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/ViewerScreen.kt \
       shared/src/commonMain/kotlin/com/cleanpic/ui/common/
git commit -m "feat: add ViewerScreen container, EmptyState, PermissionBanner"
```

---

## Task 10: UI — 轮播相册式（CarouselMode）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/CarouselMode.kt`

- [ ] **Step 1: 实现 CarouselMode**

- 当前照片/视频居中放大展示
- 左右两侧预览前后张（缩小 + 模糊）
- 左右滑动仅切换浏览（不触发操作）
- 底部两个操作按钮：删除(红) / 保留(绿)
- 按钮操作后自动跳到下一张
- 视频项：显示缩略图 + 时长标签，点击后通过 VideoPlayer 内联播放
- 文件信息悬浮层：文件名、大小、日期、分辨率

- [ ] **Step 2: 编译验证**

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/CarouselMode.kt
git commit -m "feat: add CarouselMode (default interaction)"
```

---

## Task 11: UI — 卡片左右滑（SwipeCardMode）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/SwipeCardMode.kt`

- [ ] **Step 1: 实现 SwipeCardMode**

- 照片/视频作为卡片堆叠展示
- 左滑 = 标记删除（红色指示器），右滑 = 保留（绿色指示器）
- 滑动过程中卡片跟随手指旋转 + 透明度变化
- 底层卡片缩小半透明可见
- 视频项同 CarouselMode

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/SwipeCardMode.kt
git commit -m "feat: add SwipeCardMode (Tinder-style swipe)"
```

---

## Task 12: UI — 全屏上下滑（FullscreenMode）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/FullscreenMode.kt`

- [ ] **Step 1: 实现 FullscreenMode**

- 照片/视频全屏沉浸展示
- 上下滑切换到下/上一张
- 右侧悬浮操作按钮（删除/保留）
- 底部悬浮文件信息
- 顶部悬浮退出按钮 + 进度
- 视频项：自动静音播放，点击切换声音，底部播放进度条

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/viewer/FullscreenMode.kt
git commit -m "feat: add FullscreenMode (TikTok-style)"
```

---

## Task 13: UI — 结果页 + 设置页

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/result/ResultScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: 实现 ResultScreen**

完成动画 + 统计卡片（删除/保留/释放）+ 待删除缩略图预览列表（可取消）+ "确认删除"按钮 + "再来一轮" / "返回首页"。

- [ ] **Step 2: 实现 SettingsScreen**

主题切换（横向预览卡片）+ 交互模式选择（3 个图标）+ 每轮数量滑块（5/10/15/20）+ 关于信息。

- [ ] **Step 3: 编译验证**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/result/ \
       shared/src/commonMain/kotlin/com/cleanpic/ui/settings/
git commit -m "feat: add ResultScreen and SettingsScreen"
```

---

## Task 14: L2 组件测试 + Mock 基础设施

**Files:**
- Create: `shared/src/commonTest/kotlin/com/cleanpic/mock/MockMediaRepository.kt`
- Create: `shared/src/commonTest/kotlin/com/cleanpic/mock/MockAppSettings.kt`
- Create: `shared/src/commonTest/kotlin/com/cleanpic/mock/TestMediaFactory.kt`
- Create: `shared/src/commonTest/kotlin/com/cleanpic/viewmodel/ViewerViewModelTest.kt`

- [ ] **Step 1: 创建 TestMediaFactory**

```kotlin
package com.cleanpic.mock

import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType

object TestMediaFactory {
    fun photos(n: Int) = (1..n).map {
        MediaItem("photo_$it", MediaType.PHOTO, "IMG_$it.jpg", (it * 1024).toLong(), System.currentTimeMillis(), 4032, 3024)
    }
    fun videos(n: Int) = (1..n).map {
        MediaItem("video_$it", MediaType.VIDEO, "VID_$it.mp4", (it * 10240).toLong(), System.currentTimeMillis(), 1920, 1080, duration = (it * 5000).toLong())
    }
}
```

- [ ] **Step 2: 创建 MockMediaRepository + MockAppSettings**

MockMediaRepository 使用内存列表模拟查询/删除。MockAppSettings 复用 InMemoryAppSettings。

- [ ] **Step 3: 编写 ViewerViewModel 测试**

测试覆盖：加载媒体、标记保留/删除、结果统计、反悔取消、去重逻辑。

- [ ] **Step 4: 运行测试验证通过**

Run: `scripts/test.sh`
Expected: 全部 PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonTest/
git commit -m "test: add L2 mock infrastructure and ViewerViewModel tests"
```

---

## Task 15: Android 平台实现

**Files:**
- Create: `shared/src/androidMain/kotlin/com/cleanpic/media/AndroidMediaRepository.kt`
- Create: `shared/src/androidMain/kotlin/com/cleanpic/settings/AndroidAppSettings.kt`
- Create: `shared/src/androidMain/kotlin/com/cleanpic/permission/AndroidPermission.kt`

- [ ] **Step 1: 实现 AndroidMediaRepository**

使用 MediaStore API 查询照片/视频（分页查询，每页 500 条），ContentResolver 获取缩略图/原图，`createDeleteRequest()` 批量删除。

- [ ] **Step 2: 实现 AndroidVideoPlayer**

使用 ExoPlayer (Media3) 实现视频播放接口。支持 MP4/MOV/HEVC/3GP/WebM。

- [ ] **Step 3: 实现 AndroidAppSettings**

使用 EncryptedSharedPreferences 存储 theme/interactionMode/roundCount。

- [ ] **Step 4: 实现 AndroidPermission**

处理 API 26-32 (READ_EXTERNAL_STORAGE)、API 33+ (READ_MEDIA_IMAGES/VIDEO)、API 34+ (部分权限 LIMITED)。永久拒绝时跳转系统设置。

- [ ] **Step 5: 实现 AndroidServiceLocator**

将所有 Android 实现注入 ServiceLocator。

- [ ] **Step 6: 配置 androidApp 壳工程入口 Activity**

在 MainActivity 中初始化 KuiklyUI + ServiceLocator，注册 CleanPicApp 为根组件。

- [ ] **Step 7: 真机验证照片查询**

连接 Android 设备，运行 App，点击"清理照片"，验证照片列表加载成功。

- [ ] **Step 8: Commit**

```bash
git add shared/src/androidMain/ androidApp/
git commit -m "feat: add Android platform (MediaStore, ExoPlayer, permissions, DI)"
```

---

## Task 16: iOS 平台实现

**Files:**
- Create: `shared/src/appleMain/kotlin/com/cleanpic/media/IosMediaRepository.kt`
- Create: `shared/src/appleMain/kotlin/com/cleanpic/media/IosVideoPlayer.kt`
- Create: `shared/src/appleMain/kotlin/com/cleanpic/settings/IosAppSettings.kt`
- Create: `shared/src/appleMain/kotlin/com/cleanpic/permission/IosPermission.kt`
- Create: `shared/src/appleMain/kotlin/com/cleanpic/di/IosServiceLocator.kt`

- [ ] **Step 1: 实现 IosMediaRepository**

使用 PHAsset/PHFetchResult 查询照片视频（分页），PHImageManager 获取缩略图/原图，PHAssetChangeRequest.deleteAssets() 批量删除。

- [ ] **Step 2: 实现 IosVideoPlayer**

使用 AVPlayer 实现视频播放。支持 MP4/MOV/HEVC/M4V。

- [ ] **Step 3: 实现 IosAppSettings**

使用 NSUserDefaults 存储偏好。

- [ ] **Step 4: 实现 IosPermission**

处理 PHAuthorizationStatus（.authorized / .limited / .denied），iOS 14+ Limited Photo Access 返回 LIMITED 状态。

- [ ] **Step 5: 实现 IosServiceLocator + 壳工程入口**

配置 iosApp 壳工程 ViewController 初始化 KuiklyUI。

- [ ] **Step 6: 真机验证**

- [ ] **Step 7: Commit**

```bash
git add shared/src/appleMain/ iosApp/
git commit -m "feat: add iOS platform (PHAsset, AVPlayer, permissions, DI)"
```

---

## Task 17: HarmonyOS 平台实现

**Files:**
- Create: `shared/src/ohosArm64Main/kotlin/com/cleanpic/media/HarmonyMediaRepository.kt`
- Create: `shared/src/ohosArm64Main/kotlin/com/cleanpic/media/HarmonyVideoPlayer.kt`
- Create: `shared/src/ohosArm64Main/kotlin/com/cleanpic/settings/HarmonyAppSettings.kt`
- Create: `shared/src/ohosArm64Main/kotlin/com/cleanpic/permission/HarmonyPermission.kt`
- Create: `shared/src/ohosArm64Main/kotlin/com/cleanpic/di/HarmonyServiceLocator.kt`

- [ ] **Step 1: 实现 HarmonyMediaRepository**

使用 photoAccessHelper 查询照片视频（分页），getThumbnail 获取缩略图，deleteAssets 批量删除。

- [ ] **Step 2: 实现 HarmonyVideoPlayer**

使用 AVPlayer (ArkUI) 实现视频播放。支持 MP4/MOV/HEVC/MKV。

- [ ] **Step 3: 实现 HarmonyAppSettings**

使用 Preferences (data/preferences) 存储偏好。

- [ ] **Step 4: 实现 HarmonyPermission**

处理 ohos.permission.READ_IMAGEVIDEO 权限。

- [ ] **Step 5: 实现 HarmonyServiceLocator + 壳工程入口**

配置 ohosApp 壳工程 AbilityStage 初始化 KuiklyUI。

- [ ] **Step 6: 真机验证**

- [ ] **Step 7: Commit**

```bash
git add shared/src/ohosArm64Main/ ohosApp/
git commit -m "feat: add HarmonyOS platform (photoAccessHelper, AVPlayer, permissions, DI)"
```

---

## Task 18: 集成测试 + E2E 验证

**Files:**
- 参考: `docs/testing/scenarios/ep1-photo-cleanup.md`
- 参考: `docs/testing/scenarios/tech-nfr.md`

- [ ] **Step 1: 在 Android 真机上跑完整流程**

E01: 首页→清理照片→浏览 10 张→标记删除 4 张→结果页确认→验证系统弹窗仅 1 次

- [ ] **Step 2: 验证 5 套主题全量切换**

E16: 每套主题走一遍完整流程，验证配色正确

- [ ] **Step 3: 验证 3 种交互模式**

E10-E12: 分别验证轮播/卡片/全屏的手势和操作

- [ ] **Step 4: 验证边界场景**

B01(空相册), B03(不足N张), B07(来电中断), B08(前后台切换)

- [ ] **Step 5: 性能验证**

F01(冷启动<2s), F02(帧率>=55fps), F05(内存<150MB)

- [ ] **Step 6: 最终 Commit**

```bash
git add -A && git commit -m "test: complete integration and E2E verification"
```
