# Logo 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将新 Logo 设计（暖色鸭子 + 消散照片 + sparkle）实现到 Android App 图标和所有主题的闪屏页面中。

**Architecture:** 替换现有的紫色渐变+照片卡片图标为新的鸭子 Logo。App 图标通过 Android adaptive icon XML 实现（背景层改为暖渐变，前景层改为鸭子+照片+sparkle）。闪屏页面在共享 Compose 层新增 `LogoPainter` 组件，复用现有的 `SvgPathParser` 基础设施，各主题闪屏布局替换当前的 `IconPainter("photo")` 为新的 Logo 组件。

**Tech Stack:** Android Vector Drawable XML, Compose Multiplatform Canvas, 现有 `SvgPathParser`

**设计规范:** `docs/superpowers/specs/2026-04-05-logo-design.md`

---

### Task 1: 替换 App 图标背景层

**Files:**
- Modify: `androidApp/src/main/res/drawable/ic_launcher_background.xml`
- Modify: `androidApp/src/main/res/values/ic_launcher_background.xml`

- [ ] **Step 1: 更新背景渐变 XML**

将 `ic_launcher_background.xml` 的紫色渐变改为暖色渐变：

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
    CleanPic 图标背景：品牌暖色渐变
    使用 aapt:attr 实现真正的线性渐变（API 24+）
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <path android:pathData="M0,0 L108,0 L108,108 L0,108 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108"
                android:startColor="#FFF8E1"
                android:endColor="#FFECB3" />
        </aapt:attr>
    </path>

    <!-- 微妙的光晕（左上角暖色亮区） -->
    <path
        android:pathData="M0,0 C40,0 54,14 54,54 C54,14 0,0 0,0 Z"
        android:fillColor="#FFFFFF"
        android:fillAlpha="0.08" />

</vector>
```

- [ ] **Step 2: 更新背景色 fallback**

将 `ic_launcher_background.xml`（values 目录）的颜色改为暖色：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#FFECB3</color>
</resources>
```

- [ ] **Step 3: 验证编译**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/res/drawable/ic_launcher_background.xml androidApp/src/main/res/values/ic_launcher_background.xml
git commit -m "feat: update app icon background to warm gradient"
```

---

### Task 2: 替换 App 图标前景层

**Files:**
- Modify: `androidApp/src/main/res/drawable/ic_launcher_foreground.xml`

鸭子 SVG path 来源：https://www.svgrepo.com/svg/117055/small-duck（CC0 公共领域）
原始 viewBox 为 `0 0 209.322 209.322`，需要缩放到 108dp 画布的安全区（中心 72dp 区域，即 x:18-90, y:18-90）。

缩放因子：72 / 209.322 ≈ 0.344，偏移 x:18, y:12（垂直方向微调让鸭子视觉居中）。

- [ ] **Step 1: 替换前景层 XML**

将 `ic_launcher_foreground.xml` 替换为鸭子 + 消散照片 + sparkle：

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
    CleanPic 自适应图标前景层
    设计：暖色鸭子 + 消散照片/视频 + sparkle
    素材：SVG Repo Small Duck（CC0 公共领域）
    画布 108dp，安全区中心 72dp
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- ==================== 消散中的照片/视频（鸭子右上方） ==================== -->

    <!-- 照片 1（较实） -->
    <group android:translateX="68" android:translateY="16" android:rotation="8"
        android:pivotX="8" android:pivotY="6">
        <path
            android:pathData="M0,0 L16,0 C17.1,0 18,0.9 18,2 L18,12 C18,13.1 17.1,14 16,14 L2,14 C0.9,14 0,13.1 0,12 L0,2 C0,0.9 0.9,0 2,0 Z"
            android:fillColor="#FFFFFF"
            android:fillAlpha="0.7" />
        <path
            android:pathData="M1,1 L17,1 L17,9 L1,9 Z"
            android:fillColor="#FFECD2"
            android:fillAlpha="0.5" />
    </group>

    <!-- 照片 2（半透明，含播放三角=视频） -->
    <group android:translateX="76" android:translateY="28" android:rotation="-5"
        android:pivotX="7" android:pivotY="5">
        <path
            android:pathData="M0,0 L14,0 C15.1,0 16,0.9 16,2 L16,10 C16,11.1 15.1,12 14,12 L2,12 C0.9,12 0,11.1 0,10 L0,2 C0,0.9 0.9,0 2,0 Z"
            android:fillColor="#FFFFFF"
            android:fillAlpha="0.4" />
        <!-- 播放三角 -->
        <path
            android:pathData="M5,3 L5,9 L11,6 Z"
            android:fillColor="#D4A574"
            android:fillAlpha="0.45" />
    </group>

    <!-- 照片 3（快消失） -->
    <group android:translateX="82" android:translateY="40" android:rotation="12"
        android:pivotX="5" android:pivotY="4">
        <path
            android:pathData="M0,0 L10,0 C11.1,0 12,0.9 12,2 L12,8 C12,9.1 11.1,10 10,10 L2,10 C0.9,10 0,9.1 0,8 L0,2 C0,0.9 0.9,0 2,0 Z"
            android:fillColor="#FFFFFF"
            android:fillAlpha="0.2" />
    </group>

    <!-- ==================== 鸭子主体 ==================== -->

    <!-- 鸭身（暖黄） -->
    <group android:scaleX="0.344" android:scaleY="0.344"
        android:translateX="18" android:translateY="12">
        <path
            android:pathData="M105.572,101.811c9.889,-6.368 27.417,-16.464 28.106,-42.166c0.536,-20.278 -9.971,-49.506 -49.155,-50.878C53.041,7.659 39.9,28.251 36.071,46.739l-0.928,-0.126c-1.932,0 -3.438,1.28 -5.34,2.889c-2.084,1.784 -4.683,3.979 -7.792,4.308c-3.573,0.361 -8.111,-1.206 -11.698,-2.449c-4.193,-1.431 -6.624,-2.047 -8.265,-0.759c-1.503,1.163 -2.178,3.262 -2.028,6.226c0.331,6.326 4.971,18.917 16.016,25.778c7.67,4.765 16.248,5.482 20.681,5.482c0.006,0 0.006,0 0.006,0c2.37,0 4.945,-0.239 7.388,-0.726c2.741,4.218 5.228,7.476 6.037,9.752c2.054,5.851 -27.848,25.087 -27.848,55.01c0,29.916 22.013,48.475 56.727,48.475h55.004c30.593,0 70.814,-29.908 75.291,-92.48C180.781,132.191 167.028,98.15 105.572,101.811z"
            android:fillColor="#FFD54F" />

        <!-- 嘴巴（橙色） -->
        <path
            android:pathData="M18.941,77.945C8.775,71.617 4.992,58.922 5.294,55.525c0.897,0.24 2.194,0.689 3.228,1.042c4.105,1.415 9.416,3.228 14.068,2.707c4.799,-0.499 8.253,-3.437 10.778,-5.574c0.607,-0.509 1.393,-1.176 1.872,-1.491c0.87,0.315 0.962,0.693 1.176,3.14c0.196,2.26 0.473,5.37 2.362,9.006c1.437,2.761 3.581,5.705 5.646,8.542c1.701,2.336 4.278,5.871 4.535,6.404c-0.445,1.184 -4.907,3.282 -12.229,3.282C30.177,82.591 23.69,80.904 18.941,77.945z"
            android:fillColor="#FF8F00" />

        <!-- 尾巴花纹（浅金） -->
        <path
            android:pathData="M149.159,155.398l-20.63,11.169l13.408,9.293c0,0 -49.854,15.813 -72.198,-6.885c-11.006,-11.16 -13.06,-28.533 4.124,-38.84c17.184,-10.312 84.609,3.943 84.609,3.943L134.295,147.8L149.159,155.398z"
            android:fillColor="#FFCA28" />

        <!-- 眼睛 -->
        <path
            android:pathData="M65.8,49.4 m-8.9,0 a8.9,8.9 0 1,1 17.8,0 a8.9,8.9 0 1,1 -17.8,0"
            android:fillColor="#3E2723" />

        <!-- 眼睛高光 -->
        <path
            android:pathData="M63,46.5 m-3.5,0 a3.5,3.5 0 1,1 7,0 a3.5,3.5 0 1,1 -7,0"
            android:fillColor="#FFFFFF"
            android:fillAlpha="0.85" />

        <!-- 腮红 -->
        <path
            android:pathData="M50,72 m-8,0 a8,5 0 1,1 16,0 a8,5 0 1,1 -16,0"
            android:fillColor="#FFAB91"
            android:fillAlpha="0.35" />
    </group>

    <!-- ==================== Sparkle 闪光星 ==================== -->

    <!-- 大 sparkle -->
    <path
        android:pathData="M92,18 L93.5,14 L95,18 L99,19.5 L95,21 L93.5,25 L92,21 L88,19.5 Z"
        android:fillColor="#FFB300"
        android:fillAlpha="0.65" />

    <!-- 中 sparkle -->
    <path
        android:pathData="M86,10 L87,8 L88,10 L90,11 L88,12 L87,14 L86,12 L84,11 Z"
        android:fillColor="#FFB300"
        android:fillAlpha="0.4" />

    <!-- 小 sparkle -->
    <path
        android:pathData="M97,28 L97.5,26.5 L98,28 L99.5,28.5 L98,29 L97.5,30.5 L97,29 L95.5,28.5 Z"
        android:fillColor="#FFB300"
        android:fillAlpha="0.3" />

</vector>
```

- [ ] **Step 2: 验证编译**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/res/drawable/ic_launcher_foreground.xml
git commit -m "feat: update app icon foreground to duck logo with dispersing photos"
```

---

### Task 3: 更新图标生成脚本

**Files:**
- Modify: `scripts/generate-icon.sh`

图标生成脚本当前用 ffmpeg 生成纯渐变 PNG。由于我们现在使用 XML vector drawable 作为前景层，PNG fallback 只需要背景色渐变。更新脚本配色。

- [ ] **Step 1: 更新脚本中的颜色值**

将 `scripts/generate-icon.sh` 中的紫色渐变改为暖色渐变：

替换所有 `#6A3DE8` → `#FFECB3` 和 `#B06FED` → `#FFF8E1`

具体改动：
- 第 31 行: `"gradients=s=${size}x${size}:c0=#FFF8E1:c1=#FFECB3:duration=1:speed=0.01"`
- 第 39 行: `"gradients=s=${fg}x${fg}:c0=#FFF8E1:c1=#FFECB3:duration=1:speed=0.01"`
- 第 48 行: `"gradients=s=512x512:c0=#FFF8E1:c1=#FFECB3:duration=1:speed=0.01"`

- [ ] **Step 2: 运行脚本重新生成 PNG**

Run: `scripts/generate-icon.sh`
Expected: `=== 图标生成完成 ===`

- [ ] **Step 3: Commit**

```bash
git add scripts/generate-icon.sh androidApp/src/main/res/mipmap-*
git commit -m "feat: update icon generation script to warm color palette"
```

---

### Task 4: 新增 LogoPainter 组件（闪屏用）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/icons/LogoPainter.kt`
- Test: `shared/src/commonTest/kotlin/com/cleanpic/icons/LogoPainterTest.kt`

新增一个 Composable，在 Canvas 上绘制鸭子 Logo（复用现有 `parseSvgPath`）。闪屏页面将用这个组件替换当前的 `IconPainter("photo")`。

- [ ] **Step 1: 编写测试**

```kotlin
package com.cleanpic.icons

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LogoPainterTest {

    @Test
    fun duckPathDataIsNotEmpty() {
        assertNotNull(LogoPaths.DUCK_BODY)
        assertTrue(LogoPaths.DUCK_BODY.isNotBlank())
    }

    @Test
    fun allPathsAreParseable() {
        // 验证所有 path data 都能被 SvgPathParser 正确解析（不抛异常）
        val paths = listOf(
            LogoPaths.DUCK_BODY,
            LogoPaths.DUCK_MOUTH,
            LogoPaths.DUCK_TAIL
        )
        for (pathData in paths) {
            val result = parseSvgPath(pathData)
            assertNotNull(result)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `scripts/test.sh`
Expected: FAIL — `LogoPaths` 未定义

- [ ] **Step 3: 实现 LogoPainter**

```kotlin
package com.cleanpic.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 鸭子 Logo 的 SVG path data。
 * 素材来源：https://www.svgrepo.com/svg/117055/small-duck（CC0 公共领域）
 * 原始 viewBox: 0 0 209.322 209.322
 */
object LogoPaths {
    /** 原始 viewBox 尺寸 */
    const val VIEWPORT = 209.322f

    /** 鸭身主路径 */
    const val DUCK_BODY = "M105.572,101.811c9.889,-6.368 27.417,-16.464 28.106,-42.166c0.536,-20.278 -9.971,-49.506 -49.155,-50.878C53.041,7.659 39.9,28.251 36.071,46.739l-0.928,-0.126c-1.932,0 -3.438,1.28 -5.34,2.889c-2.084,1.784 -4.683,3.979 -7.792,4.308c-3.573,0.361 -8.111,-1.206 -11.698,-2.449c-4.193,-1.431 -6.624,-2.047 -8.265,-0.759c-1.503,1.163 -2.178,3.262 -2.028,6.226c0.331,6.326 4.971,18.917 16.016,25.778c7.67,4.765 16.248,5.482 20.681,5.482c0.006,0 0.006,0 0.006,0c2.37,0 4.945,-0.239 7.388,-0.726c2.741,4.218 5.228,7.476 6.037,9.752c2.054,5.851 -27.848,25.087 -27.848,55.01c0,29.916 22.013,48.475 56.727,48.475h55.004c30.593,0 70.814,-29.908 75.291,-92.48C180.781,132.191 167.028,98.15 105.572,101.811z"

    /** 嘴巴路径 */
    const val DUCK_MOUTH = "M18.941,77.945C8.775,71.617 4.992,58.922 5.294,55.525c0.897,0.24 2.194,0.689 3.228,1.042c4.105,1.415 9.416,3.228 14.068,2.707c4.799,-0.499 8.253,-3.437 10.778,-5.574c0.607,-0.509 1.393,-1.176 1.872,-1.491c0.87,0.315 0.962,0.693 1.176,3.14c0.196,2.26 0.473,5.37 2.362,9.006c1.437,2.761 3.581,5.705 5.646,8.542c1.701,2.336 4.278,5.871 4.535,6.404c-0.445,1.184 -4.907,3.282 -12.229,3.282C30.177,82.591 23.69,80.904 18.941,77.945z"

    /** 尾巴花纹路径 */
    const val DUCK_TAIL = "M149.159,155.398l-20.63,11.169l13.408,9.293c0,0 -49.854,15.813 -72.198,-6.885c-11.006,-11.16 -13.06,-28.533 4.124,-38.84c17.184,-10.312 84.609,3.943 84.609,3.943L134.295,147.8L149.159,155.398z"
}

/**
 * 绘制鸭子 Logo 的 Composable。
 * 在闪屏页面和其他品牌展示场景中使用。
 */
@Composable
fun LogoPainter(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val bodyPath = remember { parseSvgPath(LogoPaths.DUCK_BODY) }
    val mouthPath = remember { parseSvgPath(LogoPaths.DUCK_MOUTH) }
    val tailPath = remember { parseSvgPath(LogoPaths.DUCK_TAIL) }

    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / LogoPaths.VIEWPORT
        val matrix = Matrix().apply { scale(scale, scale) }

        // 鸭身（暖黄）
        val body = Path().apply { addPath(bodyPath); transform(matrix) }
        drawPath(body, color = Color(0xFFFFD54F), style = Fill)

        // 嘴巴（橙色）
        val mouth = Path().apply { addPath(mouthPath); transform(matrix) }
        drawPath(mouth, color = Color(0xFFFF8F00), style = Fill)

        // 尾巴花纹（浅金）
        val tail = Path().apply { addPath(tailPath); transform(matrix) }
        drawPath(tail, color = Color(0xFFFFCA28), style = Fill)

        // 眼睛（深棕圆）
        val eyeX = 65.8f * scale
        val eyeY = 49.4f * scale
        val eyeR = 8.9f * scale
        drawCircle(color = Color(0xFF3E2723), radius = eyeR, center = Offset(eyeX, eyeY))

        // 眼睛高光
        val hlX = 63f * scale
        val hlY = 46.5f * scale
        val hlR = 3.5f * scale
        drawCircle(color = Color(0xD9FFFFFF), radius = hlR, center = Offset(hlX, hlY))

        // 腮红
        val blushX = 50f * scale
        val blushY = 72f * scale
        drawOval(
            color = Color(0x59FFAB91),
            topLeft = Offset(blushX - 8f * scale, blushY - 5f * scale),
            size = Size(16f * scale, 10f * scale)
        )
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `scripts/test.sh`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/icons/LogoPainter.kt shared/src/commonTest/kotlin/com/cleanpic/icons/LogoPainterTest.kt
git commit -m "feat: add LogoPainter composable for duck logo rendering"
```

---

### Task 5: 更新闪屏页面使用 LogoPainter

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/WarmSplashLayout.kt`
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/MinimalSplashLayout.kt`
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/GeometricSplashLayout.kt`
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/PlayfulSplashLayout.kt`
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/EditorialSplashLayout.kt`

每个闪屏布局中，将 `IconPainter("photo", theme, size = 40.dp)` 替换为 `LogoPainter(size = 56.dp)`。

- [ ] **Step 1: 更新 WarmSplashLayout**

在 `WarmSplashLayout.kt` 中：

1. 添加 import: `import com.cleanpic.icons.LogoPainter`
2. 将第 64 行的 `IconPainter("photo", theme, size = 40.dp)` 替换为 `LogoPainter(size = 56.dp)`

- [ ] **Step 2: 更新其他 4 个闪屏布局**

对 `MinimalSplashLayout.kt`、`GeometricSplashLayout.kt`、`PlayfulSplashLayout.kt`、`EditorialSplashLayout.kt` 执行同样的替换：

1. 添加 import: `import com.cleanpic.icons.LogoPainter`
2. 找到 `IconPainter("photo"` 调用，替换为 `LogoPainter(size = 56.dp)`

注意：每个布局的 Logo 尺寸可能需要根据布局空间微调，但统一用 56.dp 作为起点。

- [ ] **Step 3: 验证编译**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 运行单元测试**

Run: `scripts/test.sh`
Expected: ALL TESTS PASSED

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/splash/
git commit -m "feat: replace photo icon with duck logo in all splash layouts"
```

---

### Task 6: E2E 验证

**Files:** 无新文件

手动验证 App 图标和闪屏的视觉效果。

- [ ] **Step 1: 安装 APK 到模拟器/设备**

Run: `scripts/build-android.sh`
然后: `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`

- [ ] **Step 2: 验证 App 图标**

在设备桌面确认：
- 图标背景为暖色渐变（米黄→暖杏）
- 前景为暖黄色鸭子 + 右上方消散照片 + sparkle
- 图标在圆形和方形裁切下都看起来正常

- [ ] **Step 3: 验证闪屏**

打开 App 确认：
- 闪屏页面展示鸭子 Logo（暖黄鸭子，有眼睛、嘴巴、腮红）
- 下方有"刷刷鸭"品牌名
- 动画正常播放

- [ ] **Step 4: 运行闪屏相关 Maestro 测试**

Run: `~/.maestro/bin/maestro test maestro/flows/`
Expected: ALL FLOWS PASSED

- [ ] **Step 5: 最终 Commit（如有微调）**

如果在视觉验证中发现需要微调尺寸/位置，修改后提交：

```bash
git add -A
git commit -m "fix: adjust logo sizing in splash layouts after visual verification"
```
