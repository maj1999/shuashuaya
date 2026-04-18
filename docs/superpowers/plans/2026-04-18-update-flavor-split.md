# 升级模块分发渠道隔离 — 实施计划

> **执行者须知：** 必须使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务执行本计划。步骤使用 `- [ ]` 语法跟踪进度。

**目标：** 将升级功能从 shared 模块抽取到独立 KMP 模块 `:update`，shared 通过 `AppHooks` slot 接口脱钩；androidApp 增加 `direct` / `store` productFlavor，store flavor APK 经字节码扫描确认完全不含升级相关代码、URL、权限。

**架构：** 三层隔离 ── (1) `:update` 独立 Gradle 模块持有所有升级代码、Composable、URL；(2) `shared` 通过不含 "update" 字样的 `AppHooks` 泛型 slot 接口接收外部注入；(3) `androidApp` 用 `direct`/`store` flavor 决定是否声明 `:update` 依赖，store flavor 的字节码里不会出现升级相关类与字符串。

**技术栈：** Kotlin 2.1.21、Compose Multiplatform 1.7.3、Gradle 8.5 + AGP 8.2.2、Android productFlavor、Ktor 3.1.1（仅 `:update`）

**设计文档：**
- US：`docs/product/user-stories/cleanpic.md` — US-CP-17
- 架构：`docs/architecture/cleanpic/auto-update.md` + `auto-update-distribution.md`
- 测试：`docs/testing/scenarios/ep6-auto-update.md`

**依赖：** 无
**阻塞：** 后续所有针对升级功能的修改、上架商店的发布动作

---

## 文件清单

### 新建

| 文件 | 职责 |
|------|------|
| `update/build.gradle.kts` | `:update` KMP 模块构建脚本（commonMain/androidMain/appleMain + Ktor + Compose） |
| `update/src/commonMain/kotlin/com/cleanpic/update/UpdateModels.kt` | 从 shared 迁移 |
| `update/src/commonMain/kotlin/com/cleanpic/update/UpdateChecker.kt` | 从 shared 迁移 |
| `update/src/commonMain/kotlin/com/cleanpic/update/UpdateInstaller.kt` | 从 shared 迁移 |
| `update/src/commonMain/kotlin/com/cleanpic/update/UpdateDialog.kt` | 从 shared 迁移（4 个 Composable） |
| `update/src/commonMain/kotlin/com/cleanpic/update/UpdateState.kt` | 升级会话级状态单例（取代 ServiceLocator.cachedUpdateResult） |
| `update/src/androidMain/AndroidManifest.xml` | `:update` 模块自有 Manifest（占位空 application） |
| `update/src/androidMain/kotlin/com/cleanpic/update/AndroidUpdateInstaller.kt` | 从 shared 迁移 |
| `update/src/appleMain/kotlin/com/cleanpic/update/IosUpdateInstaller.kt` | 从 shared 迁移 |
| `update/src/commonTest/kotlin/com/cleanpic/update/UpdateCheckerTest.kt` | 从 shared 迁移 |
| `update/src/androidUnitTest/kotlin/com/cleanpic/update/UpdateDialogOverlayTest.kt` | 从 shared 迁移 |
| `update/src/androidUnitTest/kotlin/com/cleanpic/update/DownloadOverlayRecompositionTest.kt` | 从 shared 迁移 |
| `shared/src/commonMain/kotlin/com/cleanpic/ui/AppHooks.kt` | 新增 slot 接口 |
| `shared/src/commonTest/kotlin/com/cleanpic/ui/AppHooksTest.kt` | U-UPD-07 单元测试 |
| `androidApp/src/direct/AndroidManifest.xml` | direct flavor 专属：声明 INTERNET、REQUEST_INSTALL_PACKAGES、FileProvider |
| `androidApp/src/direct/res/xml/file_paths.xml` | direct flavor 专属：FileProvider 路径配置 |
| `androidApp/src/direct/java/com/cleanpic/android/wiring/UpdateWiring.kt` | direct flavor：构造 UpdateAppHooks |
| `androidApp/src/store/java/com/cleanpic/android/wiring/UpdateWiring.kt` | store flavor：返回 AppHooks.Empty |
| `scripts/verify-store-apk.sh` | B-UPD-01/02/03：扫描 APK 字节码、权限、Manifest |
| `scripts/release-direct.sh` | direct flavor 发布脚本（取代旧 release.sh） |
| `scripts/build-store.sh` | store flavor 构建脚本 |
| `maestro/flows/store/us-cp-17-settings-no-update.yaml` | E-UPD-12 |
| `maestro/flows/store/us-cp-17-home-no-dialog.yaml` | E-UPD-13 |
| `maestro/flows/store/us-cp-17-startup-no-request.yaml` | E-UPD-14 |

### 修改

| 文件 | 改动 |
|------|------|
| `settings.gradle.kts` | 新增 `include(":update")` |
| `shared/build.gradle.kts` | 移除 commonMain 中的 Ktor 依赖；移除 androidMain 中的 ktor-client-okhttp；移除 appleMain 中的 ktor-client-darwin |
| `shared/src/commonMain/kotlin/com/cleanpic/ui/App.kt` | 接收 `hooks: AppHooks` 参数，移除所有 update 相关 imports 与逻辑 |
| `shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreen.kt` | 调用 `hooks.onAppStart()` 替代 UpdateChecker 调用 |
| `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreen.kt` | 移除所有 update 逻辑，改为调用 `hooks.SettingsExtras()` |
| `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt` | 移除 update 相关字段 |
| `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt` | 移除 UpdateSection，渲染 `state.settingsExtras()` slot |
| `shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt` | 移除 updateChecker、updateInstaller、cachedUpdateResult 字段 |
| `androidApp/build.gradle.kts` | 新增 `productFlavors` direct/store；将 `directImplementation(":update")` 加入 dependencies |
| `androidApp/src/main/AndroidManifest.xml` | 移除 INTERNET、REQUEST_INSTALL_PACKAGES、FileProvider 声明 |
| `androidApp/src/main/java/com/cleanpic/android/MainActivity.kt` | 移除 UpdateChecker/AndroidUpdateInstaller imports；调用 `UpdateWiring.provideHooks(this)` 注入 hooks |
| `buildSrc/src/main/kotlin/CleanPicBuildConfig.kt` | 移除 `ENABLE_UPDATE_CHECK` 常量（flavor 接管） |
| `scripts/build-android.sh` | 默认构建 direct flavor，新增 flavor 参数 |
| `scripts/test.sh` | 加入 `:update` 模块的测试任务 |
| `README.md` | 更新构建说明（提及 direct/store 二者） |
| `docs/TODO.md` | 新增 plan 跟踪条目 |

### 删除

| 文件 | 理由 |
|------|------|
| `shared/src/commonMain/kotlin/com/cleanpic/update/UpdateModels.kt` | 已迁移到 `:update` |
| `shared/src/commonMain/kotlin/com/cleanpic/update/UpdateChecker.kt` | 已迁移 |
| `shared/src/commonMain/kotlin/com/cleanpic/update/UpdateInstaller.kt` | 已迁移 |
| `shared/src/commonMain/kotlin/com/cleanpic/update/UpdateDialog.kt` | 已迁移 |
| `shared/src/androidMain/kotlin/com/cleanpic/update/AndroidUpdateInstaller.kt` | 已迁移 |
| `shared/src/appleMain/kotlin/com/cleanpic/update/IosUpdateInstaller.kt` | 已迁移 |
| `shared/src/commonTest/kotlin/com/cleanpic/update/UpdateCheckerTest.kt` | 已迁移 |
| `shared/src/androidUnitTest/kotlin/com/cleanpic/update/*.kt` | 已迁移 |
| `androidApp/src/main/res/xml/file_paths.xml` | 移到 `androidApp/src/direct/res/xml/` |
| `scripts/release.sh` | 由 `release-direct.sh` 取代 |

---

## 重要前置说明

### 关键约束

1. **shared 严禁出现 "update" 字符串**：任何 import、symbol、字符串字面量。store flavor 的 APK 一定打包 shared，shared 只要含 update 字样，扫描就过不了。
2. **AppHooks 接口不得含 "update" 字样**：方法名用 `onAppStart`、`HomeOverlay`、`SettingsExtras`，故意泛型化。
3. **Ktor 必须从 shared 完全移除**：当前 shared/commonMain 引用了 `ktor-client-core` 等，仅 UpdateChecker 用。迁移后这些 dependency 需挪到 `:update`，shared 完全无 Ktor。否则 store APK 会含 Ktor 类，间接引入 url 字符串。
4. **每个 task 完成后必须 commit**：保持小步迭代，便于回滚。
5. **每 phase 完成后必须验证整体编译**：避免 phase 内部局部成功但跨 phase 编译失败。

### 命名约定

- Kotlin 包：升级相关都在 `com.cleanpic.update.*`，不变
- BuildConfig flag：保留现有 `ENABLE_UPDATE_CHECK` 名称（避免无意义重命名），但**仅在 androidApp 的 flavor 中**定义，shared/`:update` 内部不读这个 flag
- Gradle module：`:update`
- Flavor：`direct`（含升级）、`store`（不含）
- 维度：`distribution`

### iOS 范围

本 plan **不动** iOS 工程配置。`:update/appleMain` 保留 IosUpdateInstaller 空壳，shared 的 cocoapods framework 不会包含 `:update`（因为 shared 不依赖 `:update`）。iOS 端未来真正接入升级时另起 plan，按 `auto-update-distribution.md` "iOS Flavor 方案" 章节落地。

### 测试纪律

按 CLAUDE.md：每个 task 末尾必须有"运行测试 → 通过"的步骤，每 phase 完成必须跑全量 `scripts/test.sh`。Maestro flow 改造在 Phase 5。

---

## Phase 0 — 基线与模块骨架

### 任务 0.1：基线测试与编译确认

- [ ] **步骤 1：确认工作区干净**

```bash
git -C /Users/mark/Projects/shuashuaya status --short
```

预期：无输出（干净）。

- [ ] **步骤 2：跑全量单元测试**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/test.sh
```

预期：全部通过。如有失败，先修复主干再开始本计划。

- [ ] **步骤 3：构建 debug APK 确认基线编译**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/build-android.sh
```

预期：`androidApp/build/outputs/apk/debug/刷刷鸭.apk` 生成成功。

### 任务 0.2：创建 `:update` 模块骨架

**文件：**
- 创建：`update/build.gradle.kts`
- 创建：`update/src/androidMain/AndroidManifest.xml`
- 修改：`settings.gradle.kts`

- [ ] **步骤 1：写 `update/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

group = AppConfig.GROUP
version = AppConfig.VERSION_NAME

kotlin {
    androidTarget {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    val commonMain by sourceSets.getting {
        dependencies {
            implementation(project(":shared"))
            // Compose MP（UpdateDialog 等 Composable）
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            // Ktor 网络
            implementation("io.ktor:ktor-client-core:${Versions.KTOR}")
            implementation("io.ktor:ktor-client-content-negotiation:${Versions.KTOR}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${Versions.KTOR}")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}")
        }
    }

    val commonTest by sourceSets.getting {
        dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            implementation("io.ktor:ktor-client-mock:${Versions.KTOR}")
        }
    }

    val androidUnitTest by sourceSets.getting {
        dependencies {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation("org.robolectric:robolectric:4.16.1")
        }
    }

    val androidMain by sourceSets.getting {
        dependsOn(commonMain)
        dependencies {
            implementation("androidx.core:core-ktx:1.12.0")
            implementation("io.ktor:ktor-client-okhttp:${Versions.KTOR}")
        }
    }

    sourceSets.appleMain {
        dependsOn(commonMain)
        dependencies {
            implementation("io.ktor:ktor-client-darwin:${Versions.KTOR}")
        }
    }

    targets.withType<KotlinNativeTarget> {
        val mainSourceSet = compilations.getByName("main").defaultSourceSet
        if (konanTarget.family.isAppleFamily) {
            mainSourceSet.dependsOn(sourceSets.getByName("appleMain"))
        }
    }
}

android {
    compileSdk = Versions.ANDROID_COMPILE_SDK
    namespace = "${AppConfig.GROUP}.update"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Versions.ANDROID_MIN_SDK
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}
```

- [ ] **步骤 2：写空白 Android Manifest**

文件：`update/src/androidMain/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

（无内容声明：升级模块本身不需要权限或组件，权限由 `androidApp/src/direct/AndroidManifest.xml` 声明，避免 `:update` 被引用即注入权限）

- [ ] **步骤 3：在 settings.gradle.kts 注册模块**

修改 `settings.gradle.kts`，在 `include(":androidApp")` 上方加：

```kotlin
include(":update")
```

- [ ] **步骤 4：验证 `:update` 模块本身能编译**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:compileKotlinMetadata
```

预期：BUILD SUCCESSFUL。即使没有源代码，Gradle 也能完成空模块编译。

- [ ] **步骤 5：commit**

```bash
git add update/ settings.gradle.kts
git commit -m "feat(update): scaffold :update KMP module"
```

### 任务 0.3：phase 末尾验证

- [ ] **步骤 1：根项目同步检查**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew projects
```

预期输出包含：`+--- Project ':androidApp'`、`+--- Project ':shared'`、`\--- Project ':update'`。

- [ ] **步骤 2：编译全部模块**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew assemble
```

预期：所有模块 BUILD SUCCESSFUL。

---

## Phase 1 — 迁移升级代码到 `:update`

### 任务 1.1：迁移 UpdateModels.kt

**文件：**
- 创建：`update/src/commonMain/kotlin/com/cleanpic/update/UpdateModels.kt`
- 删除：`shared/src/commonMain/kotlin/com/cleanpic/update/UpdateModels.kt`

- [ ] **步骤 1：复制内容到新位置**

`update/src/commonMain/kotlin/com/cleanpic/update/UpdateModels.kt` 内容**完全等同于** `shared/src/commonMain/kotlin/com/cleanpic/update/UpdateModels.kt`（包名 `com.cleanpic.update` 不变）。

```kotlin
package com.cleanpic.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val version: String,
    val versionCode: Int? = null,
    val forceUpdate: Boolean = false,
    val minVersion: String = "0.0.0",
    val changelog: String = "",
    val downloadUrl: String = ""
)

enum class UpdateStatus {
    UP_TO_DATE,
    OPTIONAL_UPDATE,
    FORCE_UPDATE
}

data class UpdateCheckResult(
    val status: UpdateStatus,
    val updateInfo: UpdateInfo? = null
)

@Serializable
data class VersionResponse(
    val android: UpdateInfo? = null,
    val ios: UpdateInfo? = null,
    val harmonyos: UpdateInfo? = null
)
```

- [ ] **步骤 2：删除 shared 旧文件**

```bash
rm /Users/mark/Projects/shuashuaya/shared/src/commonMain/kotlin/com/cleanpic/update/UpdateModels.kt
```

- [ ] **步骤 3：编译 `:update` 验证**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:compileKotlinMetadata
```

预期：BUILD SUCCESSFUL。

（shared 此时会编译失败，因为 UpdateChecker 等还在 shared 里引用 UpdateInfo —— 不要急，下一任务一并处理）

- [ ] **步骤 4：commit**

```bash
git add update/src/commonMain/kotlin/com/cleanpic/update/UpdateModels.kt
git rm shared/src/commonMain/kotlin/com/cleanpic/update/UpdateModels.kt
git commit -m "refactor(update): move UpdateModels to :update module"
```

### 任务 1.2：迁移 UpdateChecker.kt + 单元测试

**文件：**
- 创建：`update/src/commonMain/kotlin/com/cleanpic/update/UpdateChecker.kt`
- 创建：`update/src/commonTest/kotlin/com/cleanpic/update/UpdateCheckerTest.kt`
- 删除：`shared/src/commonMain/kotlin/com/cleanpic/update/UpdateChecker.kt`
- 删除：`shared/src/commonTest/kotlin/com/cleanpic/update/UpdateCheckerTest.kt`

- [ ] **步骤 1：复制 UpdateChecker.kt 到 `:update`**

文件内容与原 `shared/src/commonMain/kotlin/com/cleanpic/update/UpdateChecker.kt` 完全一致（保持包名 `com.cleanpic.update`）。直接物理移动文件即可：

```bash
mkdir -p /Users/mark/Projects/shuashuaya/update/src/commonMain/kotlin/com/cleanpic/update
mv /Users/mark/Projects/shuashuaya/shared/src/commonMain/kotlin/com/cleanpic/update/UpdateChecker.kt \
   /Users/mark/Projects/shuashuaya/update/src/commonMain/kotlin/com/cleanpic/update/UpdateChecker.kt
```

- [ ] **步骤 2：移动单元测试**

```bash
mkdir -p /Users/mark/Projects/shuashuaya/update/src/commonTest/kotlin/com/cleanpic/update
mv /Users/mark/Projects/shuashuaya/shared/src/commonTest/kotlin/com/cleanpic/update/UpdateCheckerTest.kt \
   /Users/mark/Projects/shuashuaya/update/src/commonTest/kotlin/com/cleanpic/update/UpdateCheckerTest.kt
```

测试代码无需修改 import（包名相同）。

- [ ] **步骤 3：跑迁移后的测试**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:allTests
```

预期：UpdateCheckerTest 全部通过。

- [ ] **步骤 4：commit**

```bash
git add update/src/commonMain/kotlin/com/cleanpic/update/UpdateChecker.kt \
        update/src/commonTest/kotlin/com/cleanpic/update/UpdateCheckerTest.kt
git rm shared/src/commonMain/kotlin/com/cleanpic/update/UpdateChecker.kt \
       shared/src/commonTest/kotlin/com/cleanpic/update/UpdateCheckerTest.kt
git commit -m "refactor(update): move UpdateChecker to :update module"
```

### 任务 1.3：迁移 UpdateInstaller 接口

**文件：**
- 创建：`update/src/commonMain/kotlin/com/cleanpic/update/UpdateInstaller.kt`
- 删除：`shared/src/commonMain/kotlin/com/cleanpic/update/UpdateInstaller.kt`

- [ ] **步骤 1：物理移动接口文件**

```bash
mv /Users/mark/Projects/shuashuaya/shared/src/commonMain/kotlin/com/cleanpic/update/UpdateInstaller.kt \
   /Users/mark/Projects/shuashuaya/update/src/commonMain/kotlin/com/cleanpic/update/UpdateInstaller.kt
```

- [ ] **步骤 2：编译 `:update`**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:compileKotlinMetadata
```

预期：BUILD SUCCESSFUL。

- [ ] **步骤 3：commit**

```bash
git add update/src/commonMain/kotlin/com/cleanpic/update/UpdateInstaller.kt
git rm shared/src/commonMain/kotlin/com/cleanpic/update/UpdateInstaller.kt
git commit -m "refactor(update): move UpdateInstaller interface to :update module"
```

### 任务 1.4：迁移 UpdateDialog.kt

**文件：**
- 创建：`update/src/commonMain/kotlin/com/cleanpic/update/UpdateDialog.kt`
- 删除：`shared/src/commonMain/kotlin/com/cleanpic/update/UpdateDialog.kt`

UpdateDialog 引用 `com.cleanpic.theme.ThemeTokens`，因 `:update` 已经依赖 `:shared`（见 build.gradle），import 不需要改。

- [ ] **步骤 1：物理移动文件**

```bash
mv /Users/mark/Projects/shuashuaya/shared/src/commonMain/kotlin/com/cleanpic/update/UpdateDialog.kt \
   /Users/mark/Projects/shuashuaya/update/src/commonMain/kotlin/com/cleanpic/update/UpdateDialog.kt
```

- [ ] **步骤 2：编译 `:update`**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:compileDebugKotlinAndroid
```

预期：BUILD SUCCESSFUL。

- [ ] **步骤 3：commit**

```bash
git add update/src/commonMain/kotlin/com/cleanpic/update/UpdateDialog.kt
git rm shared/src/commonMain/kotlin/com/cleanpic/update/UpdateDialog.kt
git commit -m "refactor(update): move UpdateDialog Composables to :update module"
```

### 任务 1.5：迁移 AndroidUpdateInstaller

**文件：**
- 创建：`update/src/androidMain/kotlin/com/cleanpic/update/AndroidUpdateInstaller.kt`
- 删除：`shared/src/androidMain/kotlin/com/cleanpic/update/AndroidUpdateInstaller.kt`

- [ ] **步骤 1：物理移动文件**

```bash
mkdir -p /Users/mark/Projects/shuashuaya/update/src/androidMain/kotlin/com/cleanpic/update
mv /Users/mark/Projects/shuashuaya/shared/src/androidMain/kotlin/com/cleanpic/update/AndroidUpdateInstaller.kt \
   /Users/mark/Projects/shuashuaya/update/src/androidMain/kotlin/com/cleanpic/update/AndroidUpdateInstaller.kt
```

- [ ] **步骤 2：编译 `:update` Android target**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:compileDebugKotlinAndroid
```

预期：BUILD SUCCESSFUL。

- [ ] **步骤 3：commit**

```bash
git add update/src/androidMain/kotlin/com/cleanpic/update/AndroidUpdateInstaller.kt
git rm shared/src/androidMain/kotlin/com/cleanpic/update/AndroidUpdateInstaller.kt
git commit -m "refactor(update): move AndroidUpdateInstaller to :update module"
```

### 任务 1.6：迁移 IosUpdateInstaller

**文件：**
- 创建：`update/src/appleMain/kotlin/com/cleanpic/update/IosUpdateInstaller.kt`
- 删除：`shared/src/appleMain/kotlin/com/cleanpic/update/IosUpdateInstaller.kt`

- [ ] **步骤 1：物理移动文件**

```bash
mkdir -p /Users/mark/Projects/shuashuaya/update/src/appleMain/kotlin/com/cleanpic/update
mv /Users/mark/Projects/shuashuaya/shared/src/appleMain/kotlin/com/cleanpic/update/IosUpdateInstaller.kt \
   /Users/mark/Projects/shuashuaya/update/src/appleMain/kotlin/com/cleanpic/update/IosUpdateInstaller.kt
```

- [ ] **步骤 2：编译 iOS target**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:compileKotlinIosArm64
```

预期：BUILD SUCCESSFUL。

- [ ] **步骤 3：commit**

```bash
git add update/src/appleMain/kotlin/com/cleanpic/update/IosUpdateInstaller.kt
git rm shared/src/appleMain/kotlin/com/cleanpic/update/IosUpdateInstaller.kt
git commit -m "refactor(update): move IosUpdateInstaller to :update module"
```

### 任务 1.7：迁移 androidUnitTest 中的 UI 测试

**文件：**
- 创建：`update/src/androidUnitTest/kotlin/com/cleanpic/update/UpdateDialogOverlayTest.kt`
- 创建：`update/src/androidUnitTest/kotlin/com/cleanpic/update/DownloadOverlayRecompositionTest.kt`
- 删除：`shared/src/androidUnitTest/kotlin/com/cleanpic/update/UpdateDialogOverlayTest.kt`
- 删除：`shared/src/androidUnitTest/kotlin/com/cleanpic/update/DownloadOverlayRecompositionTest.kt`

- [ ] **步骤 1：物理移动测试**

```bash
mkdir -p /Users/mark/Projects/shuashuaya/update/src/androidUnitTest/kotlin/com/cleanpic/update
mv /Users/mark/Projects/shuashuaya/shared/src/androidUnitTest/kotlin/com/cleanpic/update/UpdateDialogOverlayTest.kt \
   /Users/mark/Projects/shuashuaya/update/src/androidUnitTest/kotlin/com/cleanpic/update/UpdateDialogOverlayTest.kt
mv /Users/mark/Projects/shuashuaya/shared/src/androidUnitTest/kotlin/com/cleanpic/update/DownloadOverlayRecompositionTest.kt \
   /Users/mark/Projects/shuashuaya/update/src/androidUnitTest/kotlin/com/cleanpic/update/DownloadOverlayRecompositionTest.kt
```

- [ ] **步骤 2：跑 `:update` androidUnitTest**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:testDebugUnitTest
```

预期：UpdateDialogOverlayTest、DownloadOverlayRecompositionTest 全部通过。

- [ ] **步骤 3：commit**

```bash
git add update/src/androidUnitTest/kotlin/com/cleanpic/update/
git rm shared/src/androidUnitTest/kotlin/com/cleanpic/update/UpdateDialogOverlayTest.kt \
       shared/src/androidUnitTest/kotlin/com/cleanpic/update/DownloadOverlayRecompositionTest.kt
git commit -m "refactor(update): move update Compose UI tests to :update module"
```

### 任务 1.8：抽取 UpdateState 单例（取代 ServiceLocator.cachedUpdateResult）

**文件：**
- 创建：`update/src/commonMain/kotlin/com/cleanpic/update/UpdateState.kt`

- [ ] **步骤 1：写 UpdateState 单例**

```kotlin
package com.cleanpic.update

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 升级模块会话级状态。
 * 取代原 ServiceLocator.cachedUpdateResult，使 shared 不再持有升级相关状态。
 */
object UpdateState {
    val cachedResult = MutableStateFlow(UpdateCheckResult(UpdateStatus.UP_TO_DATE))
}
```

- [ ] **步骤 2：编译 `:update`**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:compileKotlinMetadata
```

预期：BUILD SUCCESSFUL。

- [ ] **步骤 3：commit**

```bash
git add update/src/commonMain/kotlin/com/cleanpic/update/UpdateState.kt
git commit -m "feat(update): add UpdateState singleton for session-level cache"
```

### 任务 1.9：清理 shared 的 Ktor 依赖

**文件：**
- 修改：`shared/build.gradle.kts`

shared 不再有 UpdateChecker，应移除所有 Ktor 依赖。否则 store APK 通过 shared 引入 Ktor 类，间接增加字节码体积，且可能含 Ktor 内置 url 字符串（虽然不是 workers.dev，但属于无意义膨胀）。

- [ ] **步骤 1：编辑 shared/build.gradle.kts**

删除以下行（在 commonMain dependencies block 中）：

```kotlin
            implementation("io.ktor:ktor-client-core:${Versions.KTOR}")
            implementation("io.ktor:ktor-client-content-negotiation:${Versions.KTOR}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${Versions.KTOR}")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}")
```

删除 commonTest 中：

```kotlin
            implementation("io.ktor:ktor-client-mock:${Versions.KTOR}")
```

删除 androidMain 中：

```kotlin
            implementation("io.ktor:ktor-client-okhttp:${Versions.KTOR}")
```

删除 appleMain 中：

```kotlin
            implementation("io.ktor:ktor-client-darwin:${Versions.KTOR}")
```

注意：shared 的 `kotlinx-serialization-json` 也只被 UpdateModels 用，可以一并删除。但若其它代码后续会用到（例如 AppSettings 持久化），可保留。本次保守删除（已确认目前无其他用户）。

- [ ] **步骤 2：编译 shared，确认 Ktor 移除后仍通过**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :shared:compileKotlinMetadata
```

预期：失败（因为 shared/.../ui/ 下还有 update 相关引用）—— Phase 2 来处理。本步暂时记录失败，是预期。

- [ ] **步骤 3：commit**

```bash
git add shared/build.gradle.kts
git commit -m "refactor(shared): remove Ktor dependencies (moved to :update)"
```

### 任务 1.10：Phase 1 末尾 Phase Checkpoint

- [ ] **步骤 1：运行 `:update` 全量测试**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :update:allTests
```

预期：BUILD SUCCESSFUL，全部通过。

- [ ] **步骤 2：确认 shared 中已无 update 文件**

```bash
find /Users/mark/Projects/shuashuaya/shared/src -path '*/update/*' -type f
```

预期：无输出。

---

## Phase 2 — shared 脱钩与 AppHooks slot

> **本阶段结束后，shared 模块编译时不再需要 `:update`，且 grep "update" 仅命中 AppSettings.autoCheckUpdate 这一个无关字段。**

### 任务 2.1：定义 AppHooks 接口

**文件：**
- 创建：`shared/src/commonMain/kotlin/com/cleanpic/ui/AppHooks.kt`

- [ ] **步骤 1：写 AppHooks 接口**

```kotlin
package com.cleanpic.ui

import androidx.compose.runtime.Composable

/**
 * 宿主可注入的钩子。shared 模块不感知具体功能，由各 flavor 注入实现。
 *
 * 设计原则：方法名严格泛型，不出现 "update" "升级" 等具体功能字样，
 * 避免商店渠道 APK 通过 shared 字节码暴露功能名称。
 */
interface AppHooks {
    /** Splash 启动时调用，宿主可用于发起后台任务 */
    fun onAppStart() = Unit

    /** 首页叠加层（弹窗、下载 overlay 等） */
    @Composable
    fun HomeOverlay() = Unit

    /** 设置页额外区块 */
    @Composable
    fun SettingsExtras() = Unit

    companion object {
        val Empty: AppHooks = object : AppHooks {}
    }
}
```

- [ ] **步骤 2：编译 shared 验证（仅这一文件，旧 update 引用还会失败）**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :shared:compileKotlinMetadata 2>&1 | head -30
```

预期：报错于 App.kt/SplashScreen.kt/SettingsScreen.kt 中残留的 update import；但 AppHooks.kt 自身编译通过。

- [ ] **步骤 3：commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/AppHooks.kt
git commit -m "feat(shared): introduce AppHooks slot interface"
```

### 任务 2.2：写 AppHooks 单元测试（U-UPD-07）

**文件：**
- 创建：`shared/src/commonTest/kotlin/com/cleanpic/ui/AppHooksTest.kt`

- [ ] **步骤 1：写测试**

```kotlin
package com.cleanpic.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppHooksTest {

    @Test
    fun empty_onAppStart_does_not_throw() {
        AppHooks.Empty.onAppStart()
        // 不抛异常即通过
    }

    @Test
    fun empty_is_singleton() {
        assertSame(AppHooks.Empty, AppHooks.Empty)
    }

    @Test
    fun custom_implementation_can_override_onAppStart() {
        var triggered = 0
        val hooks = object : AppHooks {
            override fun onAppStart() { triggered++ }
        }
        hooks.onAppStart()
        hooks.onAppStart()
        assertEquals(2, triggered)
    }
}
```

注意：`HomeOverlay` 和 `SettingsExtras` 是 `@Composable` 方法，无法在普通单元测试调用（需 Compose runtime）。验证空实现行为放在 androidUnitTest 用 Robolectric + Compose UI Test，但此处只覆盖纯逻辑层；Composable 默认空行为已由"接口默认实现 = `Unit`"的语言保证，无需运行时验证。

- [ ] **步骤 2：跑测试**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :shared:commonTest --tests "com.cleanpic.ui.AppHooksTest"
```

预期：全部通过（注意 Phase 2 之前 shared 整体可能仍编译失败，可以暂时跳过整体编译只跑此 testClass；如失败因为整体编译破，不要修测试，而是接续后续任务把整体修绿）。

- [ ] **步骤 3：commit**

```bash
git add shared/src/commonTest/kotlin/com/cleanpic/ui/AppHooksTest.kt
git commit -m "test(shared): U-UPD-07 AppHooks default implementation contract"
```

### 任务 2.3：脱钩 App.kt — 接受 hooks 参数，移除 update 引用

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/App.kt`

- [ ] **步骤 1：重写 App.kt**

```kotlin
package com.cleanpic.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.cleanpic.di.ServiceLocator
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.ui.navigation.Route
import com.cleanpic.ui.navigation.rememberAppRouter
import com.cleanpic.ui.splash.SplashScreen
import com.cleanpic.ui.home.HomeScreen
import com.cleanpic.ui.viewer.ViewerScreen
import com.cleanpic.ui.result.ResultScreen
import com.cleanpic.ui.settings.SettingsScreen
import com.cleanpic.viewmodel.ViewerViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CleanPicApp(hooks: AppHooks = AppHooks.Empty) {
    val themeManager = ServiceLocator.themeManager
    val theme by themeManager.currentTheme.collectAsState()
    val router = rememberAppRouter()
    val viewerViewModel = remember { ViewerViewModel() }

    Box(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        when (val route = router.currentRoute) {
            is Route.Splash -> SplashScreen(theme, hooks) {
                router.navigate(Route.Home, clearBackStackUpTo = Route.Splash, inclusive = true)
            }
            is Route.Home -> {
                HomeScreen(router, theme, viewerViewModel)
                hooks.HomeOverlay()
            }
            is Route.Viewer -> ViewerScreen(router, theme, viewerViewModel, route.type)
            is Route.Result -> ResultScreen(router, theme, viewerViewModel)
            is Route.Settings -> SettingsScreen(router, theme, hooks)
        }
    }
}
```

注意：
- `SplashScreen` 和 `SettingsScreen` 现在接受 `hooks` 参数（任务 2.4/2.5 修改它们）
- `HomeOverlay()` 在 Home 路由下渲染，由 hooks 决定内容（direct flavor 注入升级弹窗，store flavor 渲染空）

- [ ] **步骤 2：尝试编译 shared**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :shared:compileKotlinMetadata 2>&1 | head -30
```

预期：仍失败于 SplashScreen/SettingsScreen（下两个任务修复）。

- [ ] **步骤 3：commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/App.kt
git commit -m "refactor(shared): App.kt accepts AppHooks slot, drop update imports"
```

### 任务 2.4：脱钩 SplashScreen.kt

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreen.kt`

- [ ] **步骤 1：重写 SplashScreen.kt**

```kotlin
package com.cleanpic.ui.splash

import androidx.compose.runtime.*
import com.cleanpic.theme.ThemeLayoutId
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.AppHooks
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    theme: ThemeTokens,
    hooks: AppHooks = AppHooks.Empty,
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        hooks.onAppStart()
        delay(1500L)
        onFinished()
    }
    val state = SplashScreenState(theme = theme, onSplashComplete = onFinished)
    when (theme.layoutId) {
        ThemeLayoutId.MINIMAL   -> MinimalSplashLayout(state)
        ThemeLayoutId.GEOMETRIC -> GeometricSplashLayout(state)
        ThemeLayoutId.WARM      -> WarmSplashLayout(state)
        ThemeLayoutId.PLAYFUL   -> PlayfulSplashLayout(state)
        ThemeLayoutId.EDITORIAL -> EditorialSplashLayout(state)
    }
}
```

注意：原 SplashScreen 有 `updateScope` 协程作用域处理 `UpdateChecker` 调用，整段移除。`hooks.onAppStart()` 内部由 direct flavor 实现自启动协程做版本检查。

- [ ] **步骤 2：编译 shared 单文件**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :shared:compileKotlinMetadata 2>&1 | head -20
```

预期：仍失败于 SettingsScreen（下个任务修复）。

- [ ] **步骤 3：commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/splash/SplashScreen.kt
git commit -m "refactor(shared): SplashScreen calls hooks.onAppStart() instead of UpdateChecker"
```

### 任务 2.5：脱钩 SettingsScreen.kt 与 SettingsScreenState

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreen.kt`
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt`

- [ ] **步骤 1：重写 SettingsScreenState.kt**

```kotlin
package com.cleanpic.ui.settings

import androidx.compose.runtime.Composable
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
    val onBack: () -> Unit,
    /** 由宿主 flavor 注入的额外区块（如升级 UI），默认空 */
    val extras: @Composable () -> Unit = {}
)
```

- [ ] **步骤 2：重写 SettingsScreen.kt**

```kotlin
package com.cleanpic.ui.settings

import androidx.compose.runtime.*
import com.cleanpic.theme.ThemeTokens
import com.cleanpic.ui.AppHooks
import com.cleanpic.ui.navigation.AppRouter
import com.cleanpic.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    router: AppRouter,
    theme: ThemeTokens,
    hooks: AppHooks = AppHooks.Empty
) {
    val viewModel = remember { SettingsViewModel() }
    var selectedTheme by remember { mutableStateOf(viewModel.currentThemeId) }
    var selectedMode by remember { mutableStateOf(viewModel.currentMode) }
    var selectedCount by remember { mutableStateOf(viewModel.currentRoundCount) }

    val state = SettingsScreenState(
        theme = theme,
        allThemes = viewModel.allThemes,
        currentMode = selectedMode.id,
        currentCount = selectedCount,
        onThemeChange = { id ->
            selectedTheme = id
            viewModel.switchTheme(id)
        },
        onModeChange = { modeId ->
            val mode = com.cleanpic.model.InteractionMode.fromId(modeId)
            selectedMode = mode
            viewModel.switchInteractionMode(mode)
        },
        onCountChange = { count ->
            selectedCount = count
            viewModel.setRoundCount(count)
        },
        onBack = { router.popBackStack() },
        extras = { hooks.SettingsExtras() }
    )

    SharedSettingsLayout(state)
}
```

注意：原 SettingsScreen 维护的 `autoCheckUpdate`/`isCheckingUpdate`/`manualCheckResult`/`onCheckUpdate`/`onStartUpdate`/`onSimulateDownload` 等状态全部移除，由 direct flavor 的 UpdateAppHooks 内部维护。

- [ ] **步骤 3：commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreen.kt \
        shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt
git commit -m "refactor(shared): SettingsScreen uses extras slot instead of UpdateSection"
```

### 任务 2.6：移除 SharedSettingsLayout 中的 UpdateSection

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt`

- [ ] **步骤 1：定位 UpdateSection 调用与定义**

```bash
grep -n "UpdateSection\|UpdateStatus\|update" /Users/mark/Projects/shuashuaya/shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt
```

记录所有命中行号。

- [ ] **步骤 2：编辑 SharedSettingsLayout.kt**

需要做的修改：
1. 移除 `import com.cleanpic.update.UpdateStatus`
2. 移除整个 `UpdateSection` Composable 函数定义（约 100 行）
3. 在原 UpdateSection 调用处，替换为 `state.extras()`：

调用处类似（搜索 `UpdateSection(state` 找到调用点）：

```kotlin
// 改前
UpdateSection(state, theme, radius)

// 改后
state.extras()
```

- [ ] **步骤 3：检查残留 update 引用**

```bash
grep -n "update\|Update" /Users/mark/Projects/shuashuaya/shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt
```

预期：无任何 update 字样命中（除非业务文案中含"更新"中文，那是 UI 字符串属于业务，不是技术名称）。检查输出，确保仅业务文字而非 import/symbol。

- [ ] **步骤 4：commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt
git commit -m "refactor(shared): remove UpdateSection from SharedSettingsLayout, use extras slot"
```

### 任务 2.7：清理 ServiceLocator

**文件：**
- 修改：`shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt`

- [ ] **步骤 1：重写 ServiceLocator**

```kotlin
package com.cleanpic.di

import com.cleanpic.media.MediaRepository
import com.cleanpic.media.VideoPlayer
import com.cleanpic.permission.PermissionManager
import com.cleanpic.settings.AppSettings
import com.cleanpic.theme.ThemeManager

object ServiceLocator {
    lateinit var mediaRepository: MediaRepository
    lateinit var appSettings: AppSettings
    lateinit var permissionManager: PermissionManager
    lateinit var videoPlayer: VideoPlayer
    val themeManager: ThemeManager = ThemeManager()

    // 调试标记（平台侧设置）
    var isDebugBuild: Boolean = false

    fun initialize(
        mediaRepo: MediaRepository,
        settings: AppSettings,
        permission: PermissionManager,
        player: VideoPlayer
    ) {
        mediaRepository = mediaRepo
        appSettings = settings
        permissionManager = permission
        videoPlayer = player
        themeManager.switchTheme(settings.theme)
    }
}
```

注意：删掉 `updateChecker`、`updateInstaller`、`cachedUpdateResult` 三个字段及对应的 import；`initialize` 方法签名同步删除 `updater`/`installer` 参数。

- [ ] **步骤 2：编译 shared，确认全绿**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :shared:compileKotlinMetadata
```

预期：BUILD SUCCESSFUL（shared 此时已彻底脱钩）。

如有失败，按报错追踪剩余 update 引用并清理（应该已无遗留）。

- [ ] **步骤 3：跑全部 shared 单元测试**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :shared:allTests
```

预期：BUILD SUCCESSFUL。注意 androidUnitTest 中已无 update 测试（任务 1.7 移除）。

- [ ] **步骤 4：commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt
git commit -m "refactor(shared): strip update fields from ServiceLocator"
```

### 任务 2.8：检查 shared 中残留的 update 引用

- [ ] **步骤 1：全量 grep**

```bash
grep -rn "com.cleanpic.update" /Users/mark/Projects/shuashuaya/shared/src/
```

预期：无输出。

```bash
grep -rn "Update[A-Z]" /Users/mark/Projects/shuashuaya/shared/src/
```

预期：无 UpdateXxx 类名命中（应只可能命中"自动检查更新"等中文字面量在 layout 文件中——本任务前面已删除）。

```bash
grep -rn "autoCheckUpdate" /Users/mark/Projects/shuashuaya/shared/src/
```

预期：仅 AppSettings 文件命中（保留——这是布尔字段，名称里含 update 但属于设置，由 direct flavor 读取）。

- [ ] **步骤 2：commit（若 grep 结果合格则跳过此步）**

如果发现遗漏，逐一修复并合并到一个 commit：

```bash
git commit -m "refactor(shared): remove residual update references"
```

### 任务 2.9：Phase 2 末尾 Phase Checkpoint

- [ ] **步骤 1：运行全量单元测试（含 :update + :shared）**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/test.sh
```

预期：BUILD SUCCESSFUL。

- [ ] **步骤 2：尝试构建 androidApp（此时会失败，因 MainActivity 还引用旧 ServiceLocator 签名）**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/build-android.sh 2>&1 | tail -20
```

预期：失败，错误指向 MainActivity 的 `ServiceLocator.initialize(... updater=..., installer=...)`。Phase 3 修复。

---

## Phase 3 — androidApp Flavor 配置

### 任务 3.1：移除 BuildConfig 中的 ENABLE_UPDATE_CHECK 全局常量

**文件：**
- 修改：`buildSrc/src/main/kotlin/CleanPicBuildConfig.kt`
- 修改：`androidApp/build.gradle.kts`

- [ ] **步骤 1：编辑 CleanPicBuildConfig.kt**

将 `AppConfig` 中的 `ENABLE_UPDATE_CHECK` 删除：

```kotlin
object AppConfig {
    const val APPLICATION_ID = "com.cleanpic.android"
    const val GROUP = "com.cleanpic"
    const val VERSION_NAME = "1.2.6"
    const val VERSION_CODE = 22
    const val UPDATE_API_URL = "https://cleanpic-update.maj1946027533.workers.dev"
    // 移除：const val ENABLE_UPDATE_CHECK = true
}
```

`UPDATE_API_URL` 保留——只有 direct flavor 会引用。

- [ ] **步骤 2：编辑 androidApp/build.gradle.kts，移除 defaultConfig 的两个字段**

```kotlin
android {
    // ... 其他不变
    defaultConfig {
        applicationId = AppConfig.APPLICATION_ID
        minSdk = Versions.ANDROID_MIN_SDK
        targetSdk = Versions.ANDROID_TARGET_SDK
        versionCode = AppConfig.VERSION_CODE
        versionName = AppConfig.VERSION_NAME
        // 移除：buildConfigField("boolean", "ENABLE_UPDATE_CHECK", ...)
        // 移除：buildConfigField("String", "UPDATE_API_URL", ...)
    }
    // ... 后续步骤添加 productFlavors
}
```

- [ ] **步骤 3：编译验证（会失败于 MainActivity，预期）**

不必跑，下任务连同处理。

- [ ] **步骤 4：commit**

```bash
git add buildSrc/src/main/kotlin/CleanPicBuildConfig.kt androidApp/build.gradle.kts
git commit -m "refactor(build): remove ENABLE_UPDATE_CHECK global constant (flavor will set)"
```

### 任务 3.2：添加 productFlavors

**文件：**
- 修改：`androidApp/build.gradle.kts`

- [ ] **步骤 1：在 android {} 块内添加 flavorDimensions 与 productFlavors**

完整改后的 `androidApp/build.gradle.kts`：

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    compileSdk = Versions.ANDROID_COMPILE_SDK
    namespace = AppConfig.APPLICATION_ID

    defaultConfig {
        applicationId = AppConfig.APPLICATION_ID
        minSdk = Versions.ANDROID_MIN_SDK
        targetSdk = Versions.ANDROID_TARGET_SDK
        versionCode = AppConfig.VERSION_CODE
        versionName = AppConfig.VERSION_NAME
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("direct") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_ENABLED", "true")
            buildConfigField("String", "UPDATE_API_URL", "\"${AppConfig.UPDATE_API_URL}\"")
        }
        create("store") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_ENABLED", "false")
            buildConfigField("String", "UPDATE_API_URL", "\"\"")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val flavorName = productFlavors.first().name
            output.outputFileName = "刷刷鸭-${flavorName}.apk"
        }
    }
}

dependencies {
    implementation(project(":shared"))
    "directImplementation"(project(":update"))
    implementation(Deps.ANDROIDX_APPCOMPAT)
    implementation(Deps.ANDROIDX_CORE_KTX)
    implementation(Deps.MATERIAL)
    implementation("androidx.activity:activity-compose:1.8.2")
}
```

注意 outputFileName 改为 `刷刷鸭-direct.apk` / `刷刷鸭-store.apk`。

- [ ] **步骤 2：尝试同步（不构建 APK，只验证 Gradle 配置无误）**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :androidApp:tasks --all 2>&1 | grep -E "assembleDirect|assembleStore" | head
```

预期：能看到 `assembleDirectDebug`、`assembleDirectRelease`、`assembleStoreDebug`、`assembleStoreRelease` 等任务。

- [ ] **步骤 3：commit**

```bash
git add androidApp/build.gradle.kts
git commit -m "feat(androidApp): add direct/store productFlavors"
```

### 任务 3.3：拆 Manifest — 升级权限/Provider 下放到 direct flavor

**文件：**
- 修改：`androidApp/src/main/AndroidManifest.xml`
- 创建：`androidApp/src/direct/AndroidManifest.xml`
- 创建：`androidApp/src/direct/res/xml/file_paths.xml`
- 删除：`androidApp/src/main/res/xml/file_paths.xml`

- [ ] **步骤 1：清理 main Manifest**

将 `androidApp/src/main/AndroidManifest.xml` 改为不含 INTERNET、REQUEST_INSTALL_PACKAGES、FileProvider：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- API 33+ 细粒度媒体权限 -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <!-- API 32 及以下 -->
    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="刷刷鸭"
        android:supportsRtl="true"
        android:theme="@style/Theme.CleanPic">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

- [ ] **步骤 2：创建 direct flavor Manifest**

文件：`androidApp/src/direct/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 版本检查网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- APK 安装权限 -->
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

    <application>
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>

</manifest>
```

- [ ] **步骤 3：迁移 file_paths.xml**

```bash
mkdir -p /Users/mark/Projects/shuashuaya/androidApp/src/direct/res/xml
mv /Users/mark/Projects/shuashuaya/androidApp/src/main/res/xml/file_paths.xml \
   /Users/mark/Projects/shuashuaya/androidApp/src/direct/res/xml/file_paths.xml
```

- [ ] **步骤 4：commit**

```bash
git add androidApp/src/main/AndroidManifest.xml \
        androidApp/src/direct/AndroidManifest.xml \
        androidApp/src/direct/res/xml/file_paths.xml
git rm androidApp/src/main/res/xml/file_paths.xml
git commit -m "feat(androidApp): split update permissions and FileProvider into direct flavor"
```

### 任务 3.4：写 direct flavor 的 UpdateWiring（完整实现）

**文件：**
- 创建：`androidApp/src/direct/java/com/cleanpic/android/wiring/UpdateWiring.kt`

- [ ] **步骤 1：写 UpdateWiring**

```kotlin
package com.cleanpic.android.wiring

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.cleanpic.android.BuildConfig
import com.cleanpic.di.ServiceLocator
import com.cleanpic.ui.AppHooks
import com.cleanpic.update.AndroidUpdateInstaller
import com.cleanpic.update.DownloadProgressDialog
import com.cleanpic.update.DownloadState
import com.cleanpic.update.InstallingDialog
import com.cleanpic.update.UpdateChecker
import com.cleanpic.update.UpdateCheckResult
import com.cleanpic.update.UpdateDialog
import com.cleanpic.update.UpdateFailedDialog
import com.cleanpic.update.UpdateState
import com.cleanpic.update.UpdateStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * direct flavor 的升级钩子接线。
 * 在 store flavor 不存在此实现，由 store flavor 的 UpdateWiring 返回 AppHooks.Empty。
 */
object UpdateWiring {

    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var checker: UpdateChecker? = null
    private var installer: AndroidUpdateInstaller? = null

    fun provideHooks(context: Context): AppHooks {
        // BuildConfig.UPDATE_ENABLED 在 direct flavor 始终为 true，做最终保险
        if (!BuildConfig.UPDATE_ENABLED) return AppHooks.Empty

        checker = UpdateChecker(BuildConfig.UPDATE_API_URL)
        installer = AndroidUpdateInstaller(context.applicationContext)

        return DirectAppHooks(checker!!, installer!!)
    }

    private class DirectAppHooks(
        private val checker: UpdateChecker,
        private val installer: AndroidUpdateInstaller
    ) : AppHooks {

        override fun onAppStart() {
            if (!ServiceLocator.appSettings.autoCheckUpdate) return
            updateScope.launch {
                runCatching {
                    UpdateState.cachedResult.value = checker.checkForUpdate()
                }
            }
        }

        @Composable
        override fun HomeOverlay() {
            HomeUpdateOverlay(checker, installer)
        }

        @Composable
        override fun SettingsExtras() {
            SettingsUpdateSection(checker, installer)
        }
    }
}

@Composable
private fun HomeUpdateOverlay(
    checker: UpdateChecker,
    installer: AndroidUpdateInstaller
) {
    val theme by ServiceLocator.themeManager.currentTheme.collectAsState()
    val updateResult by UpdateState.cachedResult.collectAsState()
    var dialogShown by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    if (!dialogShown && updateResult.status != UpdateStatus.UP_TO_DATE && updateResult.updateInfo != null) {
        showDialog = true
        dialogShown = true
    }

    if (showDialog) {
        val info = updateResult.updateInfo
        if (info != null) {
            UpdateDialog(
                theme = theme,
                updateInfo = info,
                isForceUpdate = updateResult.status == UpdateStatus.FORCE_UPDATE,
                onUpdate = {
                    showDialog = false
                    installer.startUpdate(info)
                },
                onDismiss = { showDialog = false }
            )
        }
    }

    val downloadState by installer.downloadState.collectAsState()
    val downloadProgress by installer.downloadProgress.collectAsState()

    when (downloadState) {
        DownloadState.DOWNLOADING -> {
            DownloadProgressDialog(theme = theme, progress = downloadProgress)
        }
        DownloadState.INSTALLING -> InstallingDialog(theme = theme)
        DownloadState.FAILED -> {
            val info = updateResult.updateInfo
            UpdateFailedDialog(
                theme = theme,
                onRetry = {
                    installer.resetState()
                    if (info != null) installer.startUpdate(info)
                },
                onDismiss = { installer.resetState() }
            )
        }
        else -> {}
    }
}

@Composable
private fun SettingsUpdateSection(
    checker: UpdateChecker,
    installer: AndroidUpdateInstaller
) {
    // 该 Composable 渲染设置页中"自动检查更新"开关、"检查更新"按钮、"立即更新"等。
    // 其内容大量复用原 shared/src/.../settings/SharedSettingsLayout.kt 中的 UpdateSection 函数体。
    // TODO: 在任务 3.5 进一步从原 UpdateSection 复制完整实现到此处。
    // 本任务先放置占位 Box 让编译通过，下一任务粘贴完整实现。
    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier)
}
```

注意此处 `SettingsUpdateSection` 占位实现，下一任务从原 `SharedSettingsLayout.kt`（git 历史里）复制 UpdateSection 函数体粘贴进来。

- [ ] **步骤 2：commit（占位版）**

```bash
mkdir -p androidApp/src/direct/java/com/cleanpic/android/wiring
git add androidApp/src/direct/java/com/cleanpic/android/wiring/UpdateWiring.kt
git commit -m "feat(direct): scaffold UpdateWiring for direct flavor"
```

### 任务 3.5：复刻原 UpdateSection 到 direct flavor

**文件：**
- 修改：`androidApp/src/direct/java/com/cleanpic/android/wiring/UpdateWiring.kt`

- [ ] **步骤 1：从 git 历史取回原 UpdateSection 实现**

```bash
git -C /Users/mark/Projects/shuashuaya show HEAD~10:shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt > /tmp/original-shared-layout.kt
```

（HEAD~10 是个估算；如果失败，用 `git log --oneline shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt` 找到包含 UpdateSection 的最近 commit，再 git show 那个 commit）

- [ ] **步骤 2：从 /tmp/original-shared-layout.kt 找到 UpdateSection 函数体**

定位 `private fun UpdateSection(state: SettingsScreenState, theme: ThemeTokens, radius: Float)` 整段代码块，连同它依赖的子 helper（如 `cardBackground` 等若被删除则也需复制）。

- [ ] **步骤 3：在 UpdateWiring.kt 中替换占位 SettingsUpdateSection**

把占位的 SettingsUpdateSection 函数体改写为：内部维护 `autoCheckUpdate`、`isCheckingUpdate`、`checkResultMessage`、`manualCheckResult` 等 state（原 SettingsScreen.kt 中的逻辑），调用 `checker.checkForUpdate()`，复用从 git 历史取出的 UpdateSection UI 代码。

完整骨架：

```kotlin
@Composable
private fun SettingsUpdateSection(
    checker: UpdateChecker,
    installer: AndroidUpdateInstaller
) {
    val theme by ServiceLocator.themeManager.currentTheme.collectAsState()
    var autoCheckUpdate by remember { mutableStateOf(ServiceLocator.appSettings.autoCheckUpdate) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var checkResultMessage by remember { mutableStateOf<String?>(null) }
    var manualCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    val scope = rememberCoroutineScope()

    val updateResult: UpdateCheckResult? = manualCheckResult ?: UpdateState.cachedResult.value.let {
        if (it.status != UpdateStatus.UP_TO_DATE) it else null
    }

    UpdateSectionUi(
        theme = theme,
        autoCheckUpdate = autoCheckUpdate,
        onAutoCheckUpdateChange = { enabled ->
            autoCheckUpdate = enabled
            ServiceLocator.appSettings.autoCheckUpdate = enabled
        },
        isCheckingUpdate = isCheckingUpdate,
        onCheckUpdate = {
            scope.launch {
                isCheckingUpdate = true
                checkResultMessage = null
                runCatching {
                    val result = checker.checkForUpdate()
                    UpdateState.cachedResult.value = result
                    manualCheckResult = result
                    checkResultMessage = when (result.status) {
                        UpdateStatus.UP_TO_DATE -> "已是最新版本"
                        UpdateStatus.OPTIONAL_UPDATE -> "发现新版本 v${result.updateInfo?.version}"
                        UpdateStatus.FORCE_UPDATE -> "发现新版本 v${result.updateInfo?.version}（需要更新）"
                    }
                }.onFailure {
                    checkResultMessage = "网络不可用，请稍后再试"
                }
                isCheckingUpdate = false
            }
        },
        updateResult = updateResult,
        checkResultMessage = checkResultMessage,
        onStartUpdate = {
            val info = updateResult?.updateInfo ?: return@UpdateSectionUi
            installer.startUpdate(info)
        },
        isDebugBuild = ServiceLocator.isDebugBuild,
        onSimulateDownload = { installer.simulateDownload() }
    )
}

@Composable
private fun UpdateSectionUi(
    theme: ThemeTokens,
    autoCheckUpdate: Boolean,
    onAutoCheckUpdateChange: (Boolean) -> Unit,
    isCheckingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    updateResult: UpdateCheckResult?,
    checkResultMessage: String?,
    onStartUpdate: () -> Unit,
    isDebugBuild: Boolean,
    onSimulateDownload: () -> Unit
) {
    // 此处粘贴从 git 历史取出的 UpdateSection UI 代码主体（约 100 行）。
    // 关键 testTag：auto_check_update_toggle、check_update_button、check_result_message、start_update_button、simulate_download_button。
    // 实施时按 /tmp/original-shared-layout.kt 中的 UpdateSection 函数体粘贴。
}
```

注意需要 `import com.cleanpic.theme.ThemeTokens` 等。

- [ ] **步骤 4：编译验证**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :androidApp:compileDirectDebugKotlin
```

预期：BUILD SUCCESSFUL（先编译 direct flavor，store 还没有 wiring）。

- [ ] **步骤 5：commit**

```bash
git add androidApp/src/direct/java/com/cleanpic/android/wiring/UpdateWiring.kt
git commit -m "feat(direct): port UpdateSection UI from shared into direct UpdateWiring"
```

### 任务 3.6：写 store flavor 的 UpdateWiring（空实现）

**文件：**
- 创建：`androidApp/src/store/java/com/cleanpic/android/wiring/UpdateWiring.kt`

- [ ] **步骤 1：写空实现**

```kotlin
package com.cleanpic.android.wiring

import android.content.Context
import com.cleanpic.ui.AppHooks

/**
 * store flavor 的升级钩子接线 — 空实现。
 * 此文件不引用 com.cleanpic.update 任何符号，确保 store APK 不含升级类。
 */
object UpdateWiring {
    fun provideHooks(context: Context): AppHooks = AppHooks.Empty
}
```

- [ ] **步骤 2：commit**

```bash
mkdir -p androidApp/src/store/java/com/cleanpic/android/wiring
git add androidApp/src/store/java/com/cleanpic/android/wiring/UpdateWiring.kt
git commit -m "feat(store): empty UpdateWiring for store flavor"
```

### 任务 3.7：改写 MainActivity

**文件：**
- 修改：`androidApp/src/main/java/com/cleanpic/android/MainActivity.kt`

- [ ] **步骤 1：重写 MainActivity**

```kotlin
package com.cleanpic.android

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.cleanpic.android.wiring.UpdateWiring
import com.cleanpic.di.ServiceLocator
import com.cleanpic.media.AndroidMediaRepository
import com.cleanpic.media.AndroidVideoPlayer
import com.cleanpic.permission.AndroidPermission
import com.cleanpic.settings.AndroidAppSettings
import com.cleanpic.theme.isLightColor
import com.cleanpic.ui.CleanPicApp

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        (ServiceLocator.permissionManager as? AndroidPermission)
            ?.onPermissionResult(granted)
    }

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val granted = result.resultCode == Activity.RESULT_OK
        (ServiceLocator.mediaRepository as? AndroidMediaRepository)
            ?.onDeleteResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val androidPermission = AndroidPermission(applicationContext)
        AndroidPermission.permissionLauncher = { permissions ->
            permissionLauncher.launch(permissions)
        }

        AndroidMediaRepository.deleteLauncher = { intentSender ->
            val request = IntentSenderRequest.Builder(intentSender).build()
            deleteLauncher.launch(request)
        }

        ServiceLocator.isDebugBuild = BuildConfig.DEBUG

        ServiceLocator.initialize(
            mediaRepo = AndroidMediaRepository(applicationContext),
            settings = AndroidAppSettings(applicationContext),
            permission = androidPermission,
            player = AndroidVideoPlayer()
        )

        val hooks = UpdateWiring.provideHooks(this)

        setContent {
            val theme by ServiceLocator.themeManager.currentTheme.collectAsState()
            val statusBarColor = theme.colorBackground.toInt()
            val lightStatusBar = !isLightColor(theme.colorText)

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    @Suppress("DEPRECATION")
                    window.statusBarColor = statusBarColor
                    WindowCompat.getInsetsController(window, view)
                        .isAppearanceLightStatusBars = lightStatusBar
                }
            }

            CleanPicApp(hooks = hooks)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidPermission.permissionLauncher = null
        AndroidMediaRepository.deleteLauncher = null
    }
}
```

注意：
- 移除 `com.cleanpic.update.AndroidUpdateInstaller` / `com.cleanpic.update.UpdateChecker` import
- `ServiceLocator.initialize` 不再传 `updater` / `installer`
- 新增 `val hooks = UpdateWiring.provideHooks(this)` + `CleanPicApp(hooks = hooks)`

- [ ] **步骤 2：分别编译两个 flavor**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :androidApp:assembleDirectDebug && ./gradlew :androidApp:assembleStoreDebug
```

预期：两次都 BUILD SUCCESSFUL。两个 APK：
- `androidApp/build/outputs/apk/direct/debug/刷刷鸭-direct.apk`
- `androidApp/build/outputs/apk/store/debug/刷刷鸭-store.apk`

- [ ] **步骤 3：commit**

```bash
git add androidApp/src/main/java/com/cleanpic/android/MainActivity.kt
git commit -m "refactor(androidApp): MainActivity injects AppHooks via UpdateWiring"
```

### 任务 3.8：Phase 3 末尾 Phase Checkpoint

- [ ] **步骤 1：跑全量单元测试**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/test.sh
```

预期：全部通过（含 `:update` 与 `:shared`）。

- [ ] **步骤 2：分别构建 release flavor**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :androidApp:assembleDirectRelease :androidApp:assembleStoreRelease
```

预期：两次 BUILD SUCCESSFUL。

---

## Phase 4 — store flavor 字节码扫描验证

### 任务 4.1：写 verify-store-apk.sh

**文件：**
- 创建：`scripts/verify-store-apk.sh`

- [ ] **步骤 1：写脚本**

```bash
#!/usr/bin/env bash
#
# 验证 store flavor APK 不含升级相关代码、URL、权限。
#
# 用法:
#   scripts/verify-store-apk.sh <apk-path>
#
# 退出码:
#   0 = 通过（APK 干净）
#   1 = 失败（APK 含升级痕迹）
#
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "用法: $0 <apk-path>"
    exit 1
fi

APK="$1"
if [ ! -f "$APK" ]; then
    echo "❌ APK 不存在: $APK"
    exit 1
fi

# 必须存在 aapt（Android SDK build-tools）
AAPT="$(command -v aapt || true)"
if [ -z "$AAPT" ]; then
    BUILD_TOOLS=$(ls -d "${ANDROID_HOME:-$HOME/Library/Android/sdk}"/build-tools/*/ 2>/dev/null | sort -V | tail -1 || true)
    AAPT="${BUILD_TOOLS}aapt"
    if [ ! -x "$AAPT" ]; then
        echo "❌ 找不到 aapt，请检查 ANDROID_HOME"
        exit 1
    fi
fi

echo "═══════════════════════════════════════"
echo "  扫描 APK: $APK"
echo "═══════════════════════════════════════"

FAIL=0

# B-UPD-01: 字节码字符串扫描
echo ""
echo "【1】字节码字符串扫描..."
TMPDIR=$(mktemp -d)
unzip -p "$APK" 'classes*.dex' > "$TMPDIR/dexstrings.bin" 2>/dev/null || true

check_string() {
    local pattern="$1"
    local desc="$2"
    local count=$(strings "$TMPDIR/dexstrings.bin" | grep -c "$pattern" || true)
    if [ "$count" -gt 0 ]; then
        echo "  ❌ 发现 $count 处 \"$pattern\" ($desc)"
        FAIL=1
    else
        echo "  ✅ 无 \"$pattern\" ($desc)"
    fi
}

check_string "UpdateChecker" "升级检查类名"
check_string "UpdateInstaller" "升级安装类名"
check_string "UpdateDialog" "升级弹窗类名"
check_string "workers.dev" "升级 API 域名"
check_string "releases/download" "GitHub Release 下载 URL"
check_string "com/cleanpic/update" "升级模块包名"

rm -rf "$TMPDIR"

# B-UPD-02: 权限扫描
echo ""
echo "【2】权限扫描..."
PERMS=$("$AAPT" dump permissions "$APK" 2>/dev/null || true)

check_perm() {
    local perm="$1"
    if echo "$PERMS" | grep -q "$perm"; then
        echo "  ❌ 发现权限 $perm"
        FAIL=1
    else
        echo "  ✅ 无权限 $perm"
    fi
}

check_perm "REQUEST_INSTALL_PACKAGES"

# B-UPD-03: Manifest 扫描
echo ""
echo "【3】Manifest 资源扫描..."
MANIFEST_DUMP=$("$AAPT" dump xmltree "$APK" AndroidManifest.xml 2>/dev/null || true)

if echo "$MANIFEST_DUMP" | grep -q "FileProvider"; then
    echo "  ❌ Manifest 中发现 FileProvider 声明"
    FAIL=1
else
    echo "  ✅ Manifest 无 FileProvider"
fi

if echo "$MANIFEST_DUMP" | grep -q "fileprovider"; then
    echo "  ❌ Manifest 中发现 fileprovider authority"
    FAIL=1
else
    echo "  ✅ Manifest 无 fileprovider authority"
fi

# 总结
echo ""
echo "═══════════════════════════════════════"
if [ "$FAIL" -eq 0 ]; then
    echo "  ✅ APK 扫描通过：无升级相关代码与配置"
    exit 0
else
    echo "  ❌ APK 扫描失败：含有升级相关痕迹（详见上方）"
    exit 1
fi
```

- [ ] **步骤 2：赋可执行权限**

```bash
chmod +x /Users/mark/Projects/shuashuaya/scripts/verify-store-apk.sh
```

- [ ] **步骤 3：commit**

```bash
git add scripts/verify-store-apk.sh
git commit -m "feat(scripts): verify-store-apk.sh — bytecode/permission/manifest scan"
```

### 任务 4.2：用 store APK 验证脚本

- [ ] **步骤 1：构建 store release APK**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :androidApp:assembleStoreRelease
```

预期：APK 路径 `androidApp/build/outputs/apk/store/release/刷刷鸭-store.apk`

- [ ] **步骤 2：跑扫描脚本**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/verify-store-apk.sh androidApp/build/outputs/apk/store/release/刷刷鸭-store.apk
```

预期：退出码 0，输出 "APK 扫描通过：无升级相关代码与配置"。

如果失败：根据脚本输出的 "发现 N 处 xxx" 反查残留来源（最常见原因：shared 中 import 残留，或 ServiceLocator 未删干净）。

- [ ] **步骤 3：commit（若发现残留并修复）**

如本任务发现问题，记录修复 commit。如直接通过则跳过。

### 任务 4.3：用 direct APK 反向验证（应该 FAIL）

确认脚本没有"假阳性通过"——direct APK 应当被脚本判定为含升级代码。

- [ ] **步骤 1：构建 direct release APK**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :androidApp:assembleDirectRelease
```

- [ ] **步骤 2：跑脚本，期望失败**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/verify-store-apk.sh androidApp/build/outputs/apk/direct/release/刷刷鸭-direct.apk
echo "退出码: $?"
```

预期：退出码 1，输出 "APK 扫描失败：含有升级相关痕迹"。

如果 direct 也通过了，说明 direct flavor 的 `:update` 没有被正确链接 —— 检查 `androidApp/build.gradle.kts` 的 `directImplementation`。

### 任务 4.4：Phase 4 末尾 Phase Checkpoint

- [ ] **步骤 1：再次跑全量单元测试**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/test.sh
```

预期：BUILD SUCCESSFUL。

---

## Phase 5 — Maestro Flow 重组

### 任务 5.1：创建 flavor 目录结构

- [ ] **步骤 1：建目录**

```bash
mkdir -p /Users/mark/Projects/shuashuaya/maestro/flows/direct
mkdir -p /Users/mark/Projects/shuashuaya/maestro/flows/store
```

- [ ] **步骤 2：commit（空目录用 .gitkeep 保留）**

```bash
touch /Users/mark/Projects/shuashuaya/maestro/flows/direct/.gitkeep
touch /Users/mark/Projects/shuashuaya/maestro/flows/store/.gitkeep
git add maestro/flows/direct/.gitkeep maestro/flows/store/.gitkeep
git commit -m "feat(maestro): create direct/store flavor directories"
```

### 任务 5.2：迁移现有 flow 到 direct 目录

现有 14 个 flow 文件。按职责分类：
- **升级专属**（仅 direct flavor 适用）：`auto-check-update-toggle.yaml`、`settings-update-section.yaml`、`simulate-download.yaml`
- **通用功能**（direct + store 都应通过）：browse-photos、delete-confirm、exit-button、keep-all、next-round、photo-thumbnail、result-stats-layout、round-count、swipe-card-mode、swipe-card-thumbnail、switch-theme、`video/`

策略：通用 flow 复制（不是移动）到 store 目录，让 store 也能跑通用回归。升级专属只放 direct。

- [ ] **步骤 1：升级专属移到 direct**

```bash
cd /Users/mark/Projects/shuashuaya
git mv maestro/flows/auto-check-update-toggle.yaml maestro/flows/direct/
git mv maestro/flows/settings-update-section.yaml maestro/flows/direct/
git mv maestro/flows/simulate-download.yaml maestro/flows/direct/
```

- [ ] **步骤 2：通用 flow 移到 direct（store 复用同一份避免重复维护，5.3 会用 symlink 或复制策略）**

```bash
cd /Users/mark/Projects/shuashuaya
git mv maestro/flows/browse-photos.yaml maestro/flows/direct/
git mv maestro/flows/delete-confirm.yaml maestro/flows/direct/
git mv maestro/flows/exit-button.yaml maestro/flows/direct/
git mv maestro/flows/keep-all.yaml maestro/flows/direct/
git mv maestro/flows/next-round.yaml maestro/flows/direct/
git mv maestro/flows/photo-thumbnail.yaml maestro/flows/direct/
git mv maestro/flows/result-stats-layout.yaml maestro/flows/direct/
git mv maestro/flows/round-count.yaml maestro/flows/direct/
git mv maestro/flows/swipe-card-mode.yaml maestro/flows/direct/
git mv maestro/flows/swipe-card-thumbnail.yaml maestro/flows/direct/
git mv maestro/flows/switch-theme.yaml maestro/flows/direct/
git mv maestro/flows/video maestro/flows/direct/
```

- [ ] **步骤 3：commit**

```bash
git commit -m "refactor(maestro): move existing flows to direct/ directory"
```

### 任务 5.3：复制通用 flow 到 store 目录

store flavor 也需要跑通用功能回归。复制（而非 symlink，避免跨平台 git 问题）。

- [ ] **步骤 1：复制通用 flow**

```bash
cd /Users/mark/Projects/shuashuaya/maestro/flows
cp direct/browse-photos.yaml store/
cp direct/delete-confirm.yaml store/
cp direct/exit-button.yaml store/
cp direct/keep-all.yaml store/
cp direct/next-round.yaml store/
cp direct/photo-thumbnail.yaml store/
cp direct/result-stats-layout.yaml store/
cp direct/round-count.yaml store/
cp direct/swipe-card-mode.yaml store/
cp direct/swipe-card-thumbnail.yaml store/
cp direct/switch-theme.yaml store/
cp -r direct/video store/
```

- [ ] **步骤 2：commit**

```bash
git add maestro/flows/store/
git commit -m "feat(maestro): replicate common flows for store flavor regression"
```

### 任务 5.4：写 E-UPD-12 store 设置页无升级区块

**文件：**
- 创建：`maestro/flows/store/us-cp-17-settings-no-update.yaml`

- [ ] **步骤 1：写 flow**

```yaml
# maestro/flows/store/us-cp-17-settings-no-update.yaml
#
# E2E (E-UPD-12): store flavor 设置页不显示任何升级相关 UI
# 对应: US-CP-17 AC1
#
appId: com.cleanpic.android
---
- stopApp
- launchApp
- extendedWaitUntil:
    visible:
      id: "settings_button"
    timeout: 30000
- waitForAnimationToEnd

# 进入设置页
- tapOn:
    id: "settings_button"
- waitForAnimationToEnd

# 断言：升级相关元素均不存在
- assertNotVisible:
    id: "auto_check_update_toggle"
- assertNotVisible:
    id: "check_update_button"
- assertNotVisible:
    id: "start_update_button"
- assertNotVisible:
    id: "check_result_message"

# 截图作为证据
- takeScreenshot: maestro/screenshots/store-settings-no-update

# 返回首页
- tapOn:
    id: "back_button"
- waitForAnimationToEnd
```

### 任务 5.5：写 E-UPD-13 store 首页无升级弹窗

**文件：**
- 创建：`maestro/flows/store/us-cp-17-home-no-dialog.yaml`

- [ ] **步骤 1：写 flow**

```yaml
# maestro/flows/store/us-cp-17-home-no-dialog.yaml
#
# E2E (E-UPD-13): store flavor 启动后不弹任何升级弹窗
# 对应: US-CP-17 AC3
#
appId: com.cleanpic.android
---
- stopApp
- launchApp
- extendedWaitUntil:
    visible:
      id: "settings_button"
    timeout: 30000
- waitForAnimationToEnd

# 进入首页后立即检查
- assertNotVisible:
    id: "update_dialog"
- assertNotVisible:
    id: "download_progress_dialog"
- assertNotVisible:
    id: "installing_dialog"
- assertNotVisible:
    id: "update_failed_dialog"

# 等待 3 秒后再次检查（防止异步弹出）
- runScript:
    file: ../sleep-3s.js
- assertNotVisible:
    id: "update_dialog"
```

`sleep-3s.js` 文件如不存在则需创建：

文件：`maestro/sleep-3s.js`

```javascript
output.sleep = 'done';
```

实际 Maestro 等待用 `extendedWaitUntil` 的 timeout，可改写为：

更简单：删除 runScript，改用 extendedWaitUntil 配合一个不存在的元素 + 短 timeout 制造延迟。但更直接的写法是：

```yaml
- assertNotVisible:
    id: "update_dialog"
- waitForAnimationToEnd
- assertNotVisible:
    id: "update_dialog"
```

替换上面的 `runScript` 段为：

```yaml
- waitForAnimationToEnd
- assertNotVisible:
    id: "update_dialog"
```

### 任务 5.6：写 E-UPD-14 store 启动无升级代码

**文件：**
- 创建：`maestro/flows/store/us-cp-17-startup-no-request.yaml`

- [ ] **步骤 1：写 flow**

```yaml
# maestro/flows/store/us-cp-17-startup-no-request.yaml
#
# E2E (E-UPD-14): store flavor 启动正常，无升级行为副作用
# 对应: US-CP-17 AC2（行为层；流量层由 B-UPD-01 字节码扫描承担）
#
appId: com.cleanpic.android
---
- stopApp
- launchApp
- extendedWaitUntil:
    visible:
      id: "settings_button"
    timeout: 30000
- waitForAnimationToEnd

# 首页正常渲染
- assertVisible:
    id: "settings_button"

# 无升级弹窗
- assertNotVisible:
    id: "update_dialog"
- assertNotVisible:
    id: "download_progress_dialog"

# 进入设置页确认无升级元素
- tapOn:
    id: "settings_button"
- waitForAnimationToEnd
- assertNotVisible:
    id: "auto_check_update_toggle"

# 返回首页
- tapOn:
    id: "back_button"
- waitForAnimationToEnd
```

- [ ] **步骤 2：commit 三个 store flow**

```bash
git add maestro/flows/store/us-cp-17-settings-no-update.yaml \
        maestro/flows/store/us-cp-17-home-no-dialog.yaml \
        maestro/flows/store/us-cp-17-startup-no-request.yaml
git commit -m "test(maestro): add US-CP-17 store flavor E2E flows (E-UPD-12/13/14)"
```

### 任务 5.7：跑 direct flavor 全量回归（E-UPD-15）

- [ ] **步骤 1：构建 direct debug APK**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :androidApp:assembleDirectDebug
```

- [ ] **步骤 2：安装并跑 direct flow**

```bash
adb install -r androidApp/build/outputs/apk/direct/debug/刷刷鸭-direct.apk
~/.maestro/bin/maestro test maestro/flows/direct/
```

预期：全部通过（含 ep6 全量 11 个升级 flow + 通用功能 flow）。

如有失败，按 systematic-debugging 排查后修复。

### 任务 5.8：跑 store flavor 全量

- [ ] **步骤 1：安装 store debug APK**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :androidApp:assembleStoreDebug
adb install -r androidApp/build/outputs/apk/store/debug/刷刷鸭-store.apk
```

- [ ] **步骤 2：跑 store flow**

```bash
~/.maestro/bin/maestro test maestro/flows/store/
```

预期：全部通过（通用功能 + 3 个 US-CP-17 flow）。

---

## Phase 6 — 脚本拆分

### 任务 6.1：写 release-direct.sh（取代 release.sh）

**文件：**
- 创建：`scripts/release-direct.sh`
- 删除：`scripts/release.sh`

- [ ] **步骤 1：复制 release.sh 内容到 release-direct.sh，并改造**

```bash
cp /Users/mark/Projects/shuashuaya/scripts/release.sh /Users/mark/Projects/shuashuaya/scripts/release-direct.sh
```

- [ ] **步骤 2：编辑 release-direct.sh**

需要修改：
1. 文件头注释改为 "发布 direct flavor 版本"
2. APK 路径从 `androidApp/build/outputs/apk/release/刷刷鸭.apk` 改为 `androidApp/build/outputs/apk/direct/release/刷刷鸭-direct.apk`
3. Gradle 任务从 `assembleRelease` 改为 `assembleDirectRelease`
4. UPLOAD_APK 文件名 `/tmp/shuashuaya.apk` → `/tmp/shuashuaya-direct.apk`

修改示例（仅展示要改的关键行）：

```bash
APK_PATH="$PROJECT_ROOT/androidApp/build/outputs/apk/direct/release/刷刷鸭-direct.apk"
# ...
./gradlew :androidApp:assembleDirectRelease --quiet
# ...
UPLOAD_APK="/tmp/shuashuaya-direct.apk"
```

- [ ] **步骤 3：删除旧 release.sh**

```bash
rm /Users/mark/Projects/shuashuaya/scripts/release.sh
chmod +x /Users/mark/Projects/shuashuaya/scripts/release-direct.sh
```

- [ ] **步骤 4：commit**

```bash
git add scripts/release-direct.sh
git rm scripts/release.sh
git commit -m "refactor(scripts): rename release.sh -> release-direct.sh (direct flavor only)"
```

### 任务 6.2：写 build-store.sh

**文件：**
- 创建：`scripts/build-store.sh`

- [ ] **步骤 1：写脚本**

```bash
#!/usr/bin/env bash
#
# 构建 store flavor APK，产出到 dist/ 待手动上架应用商店。
#
# 用法:
#   scripts/build-store.sh <版本号>
#
# 示例:
#   scripts/build-store.sh 1.2.6
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ $# -lt 1 ]; then
    echo "用法: $0 <版本号>"
    echo "示例: $0 1.2.6"
    exit 1
fi

VERSION="$1"
APK_PATH="$PROJECT_ROOT/androidApp/build/outputs/apk/store/release/刷刷鸭-store.apk"
DIST_DIR="$PROJECT_ROOT/dist"
DIST_APK="$DIST_DIR/刷刷鸭-store-v${VERSION}.apk"

echo "═══════════════════════════════════════"
echo "  构建 store flavor v${VERSION}"
echo "═══════════════════════════════════════"

cd "$PROJECT_ROOT"

# 构建
echo ""
echo "【1/3】构建 store flavor APK..."
./gradlew :androidApp:assembleStoreRelease --quiet
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK 未生成: $APK_PATH"
    exit 1
fi
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "  ✅ APK 构建成功 ($APK_SIZE)"

# 字节码扫描
echo ""
echo "【2/3】字节码扫描验证..."
"$SCRIPT_DIR/verify-store-apk.sh" "$APK_PATH"

# 复制到 dist/
echo ""
echo "【3/3】产出到 dist/..."
mkdir -p "$DIST_DIR"
cp "$APK_PATH" "$DIST_APK"
echo "  ✅ APK 已复制到: $DIST_APK"

echo ""
echo "═══════════════════════════════════════"
echo "  ✅ 完成。可上架到各应用商店。"
echo "═══════════════════════════════════════"
echo ""
echo "  APK: $DIST_APK"
echo "  注意: 商店上架需各渠道独立签名与元数据，请手动处理。"
```

- [ ] **步骤 2：赋可执行权限并 commit**

```bash
chmod +x /Users/mark/Projects/shuashuaya/scripts/build-store.sh
git add scripts/build-store.sh
git commit -m "feat(scripts): build-store.sh — store flavor build + bytecode verification"
```

### 任务 6.3：更新 build-android.sh 支持 flavor 参数

**文件：**
- 修改：`scripts/build-android.sh`

- [ ] **步骤 1：读取当前脚本**

```bash
cat /Users/mark/Projects/shuashuaya/scripts/build-android.sh
```

- [ ] **步骤 2：改写 build-android.sh**

支持可选 flavor 参数（默认 direct），示例：

```bash
#!/usr/bin/env bash
#
# 构建 Android Debug APK
#
# 用法:
#   scripts/build-android.sh                # 构建 direct flavor（默认）
#   scripts/build-android.sh direct
#   scripts/build-android.sh store
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

FLAVOR="${1:-direct}"
case "$FLAVOR" in
    direct|store) ;;
    *) echo "❌ 未知 flavor: $FLAVOR （支持: direct, store）"; exit 1 ;;
esac

# capitalize first letter for gradle task
FLAVOR_CAP="$(echo "$FLAVOR" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')"
TASK="assemble${FLAVOR_CAP}Debug"

cd "$PROJECT_ROOT"
echo "构建 ${FLAVOR} flavor debug APK..."
./gradlew ":androidApp:${TASK}"

APK="$PROJECT_ROOT/androidApp/build/outputs/apk/${FLAVOR}/debug/刷刷鸭-${FLAVOR}.apk"
if [ -f "$APK" ]; then
    SIZE=$(du -h "$APK" | cut -f1)
    echo "✅ APK 已生成: $APK ($SIZE)"
else
    echo "❌ APK 未找到: $APK"
    exit 1
fi
```

- [ ] **步骤 3：commit**

```bash
git add scripts/build-android.sh
git commit -m "refactor(scripts): build-android.sh accepts flavor argument (default direct)"
```

### 任务 6.4：更新 scripts/test.sh 涵盖 :update 模块

**文件：**
- 修改：`scripts/test.sh`

- [ ] **步骤 1：检查现有内容**

```bash
cat /Users/mark/Projects/shuashuaya/scripts/test.sh
```

- [ ] **步骤 2：确保运行 `:update:allTests`**

如脚本是 `./gradlew test` 这类全局调用，则不必改（Gradle 自动包含新模块）。如指定了 `:shared:allTests`，需补充 `:update:allTests`。

修改示例（如指定模块的话）：

```bash
./gradlew :shared:allTests :update:allTests
```

- [ ] **步骤 3：commit（若有改动）**

```bash
git add scripts/test.sh
git commit -m "refactor(scripts): test.sh runs :update tests too"
```

### 任务 6.5：更新 README.md

**文件：**
- 修改：`README.md`

- [ ] **步骤 1：搜索 README 中 release.sh / build APK 相关说明**

```bash
grep -n "release.sh\|build-android\|assembleRelease\|刷刷鸭.apk" /Users/mark/Projects/shuashuaya/README.md
```

- [ ] **步骤 2：更新提及**

将旧的 `scripts/release.sh` 引用替换为 `scripts/release-direct.sh`，新增 store flavor 构建说明 `scripts/build-store.sh`。

- [ ] **步骤 3：commit**

```bash
git add README.md
git commit -m "docs(readme): update build/release commands for direct/store flavors"
```

### 任务 6.6：Phase 6 末尾 Phase Checkpoint

- [ ] **步骤 1：演练 build-store.sh**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/build-store.sh 1.2.6
```

预期：BUILD SUCCESSFUL + 字节码扫描通过 + APK 复制到 `dist/刷刷鸭-store-v1.2.6.apk`。

- [ ] **步骤 2：跑 build-android.sh 不带参数（默认 direct）**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/build-android.sh
```

预期：生成 `androidApp/build/outputs/apk/direct/debug/刷刷鸭-direct.apk`。

- [ ] **步骤 3：跑 build-android.sh store**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/build-android.sh store
```

预期：生成 `androidApp/build/outputs/apk/store/debug/刷刷鸭-store.apk`。

---

## Phase 7 — 最终验证与文档收尾

### 任务 7.1：全量单元测试

- [ ] **步骤 1：跑 scripts/test.sh**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/test.sh
```

预期：BUILD SUCCESSFUL，全部通过。

### 任务 7.2：direct flavor 全量 Maestro

- [ ] **步骤 1：构建 + 安装**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/build-android.sh direct
adb install -r androidApp/build/outputs/apk/direct/debug/刷刷鸭-direct.apk
```

- [ ] **步骤 2：跑全量 direct flow**

```bash
~/.maestro/bin/maestro test maestro/flows/direct/
```

预期：全部通过。

### 任务 7.3：store flavor 全量 Maestro

- [ ] **步骤 1：构建 + 安装**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/build-android.sh store
adb install -r androidApp/build/outputs/apk/store/debug/刷刷鸭-store.apk
```

- [ ] **步骤 2：跑全量 store flow**

```bash
~/.maestro/bin/maestro test maestro/flows/store/
```

预期：全部通过。

### 任务 7.4：store APK 字节码最终扫描

- [ ] **步骤 1：构建 store release**

```bash
cd /Users/mark/Projects/shuashuaya && ./gradlew :androidApp:assembleStoreRelease
```

- [ ] **步骤 2：扫描**

```bash
cd /Users/mark/Projects/shuashuaya && scripts/verify-store-apk.sh androidApp/build/outputs/apk/store/release/刷刷鸭-store.apk
echo "退出码: $?"
```

预期：退出码 0，"APK 扫描通过"。

### 任务 7.5：更新 docs/TODO.md

**文件：**
- 修改：`docs/TODO.md`

- [ ] **步骤 1：在"实现计划"区块新增本 plan**

在 `docs/TODO.md` 的"## 实现计划"段最后新增：

```markdown
- [x] `superpowers/plans/2026-04-18-update-flavor-split.md` — 升级模块分发渠道隔离
```

- [ ] **步骤 2：commit**

```bash
git add docs/TODO.md
git commit -m "docs(todo): record update flavor split plan completion"
```

### 任务 7.6：版本号与发布询问

按 CLAUDE.md "完整提交检查清单"：完成代码改动后需考虑版本号升级。本次重构虽属于"内部架构"，但 Manifest 拆分 / flavor 切换可能影响最终用户的安装路径（用户从 GitHub 下载的还是 direct flavor APK，体验等价），所以：

- [ ] **步骤 1：升级 PATCH 版本号**

修改 `buildSrc/src/main/kotlin/CleanPicBuildConfig.kt`：

```kotlin
const val VERSION_NAME = "1.2.7"
const val VERSION_CODE = 23
```

修改 `shared/src/commonMain/kotlin/com/cleanpic/AppInfo.kt`：

```kotlin
const val VERSION = "1.2.7"
```

- [ ] **步骤 2：commit 版本号**

```bash
git add buildSrc/src/main/kotlin/CleanPicBuildConfig.kt shared/src/commonMain/kotlin/com/cleanpic/AppInfo.kt
git commit -m "chore: bump version to 1.2.7"
```

- [ ] **步骤 3：询问用户是否发布 Release**

向用户提问：
> "版本号已升级到 1.2.7，本次包含分发渠道分层架构（direct/store flavor）。是否运行 `scripts/release-direct.sh 1.2.7 \"重构升级模块为独立 :update 模块，支持 direct/store 双渠道分发\"` 发布 direct flavor Release？"

等用户确认后再执行发布脚本。

---

## 自审清单

### 1. Spec 覆盖
- [x] US-CP-17 AC1（设置页无升级区块）→ 任务 2.5/2.6 + 5.4 (E-UPD-12)
- [x] US-CP-17 AC2（启动无升级 API 流量）→ 任务 4.1/4.2 (B-UPD-01) + 5.6 (E-UPD-14)
- [x] US-CP-17 AC3（首页无升级弹窗）→ 任务 2.3 + 3.6 + 5.5 (E-UPD-13)
- [x] US-CP-17 AC4（direct 完整保留）→ 任务 3.4/3.5 + 5.7 (E-UPD-15)
- [x] U-UPD-07 AppHooks 默认空实现 → 任务 2.2
- [x] B-UPD-01/02/03 字节码/权限/Manifest 扫描 → 任务 4.1
- [x] :update 独立 KMP 模块 → 任务 0.2 + Phase 1 全部
- [x] shared 通过 AppHooks 脱钩 → 任务 2.1/2.3/2.4/2.5/2.7
- [x] direct/store productFlavor → 任务 3.2
- [x] 升级权限/Provider 下放到 direct flavor → 任务 3.3
- [x] release-direct.sh + build-store.sh + verify-store-apk.sh → 任务 4.1, 6.1, 6.2

### 2. Placeholder 检查
- [x] 任务 3.4 提到"占位 Box"——已在任务 3.5 明确从 git 历史取出 UpdateSection 完整代码替换
- [x] 任务 3.5 写明 `git show HEAD~10:...` 是估算，含 fallback `git log --oneline ... 找包含 UpdateSection 的最近 commit` 兜底
- [x] 任务 5.5 中 `runScript: ../sleep-3s.js` 已替换为更简单的 `waitForAnimationToEnd`
- [x] 所有 step 提供具体命令或代码

### 3. 类型一致性
- [x] AppHooks 接口名与 AppHooks.kt 文件名一致
- [x] AppHooks.Empty 在所有引用处一致
- [x] `BuildConfig.UPDATE_ENABLED` 与 `BuildConfig.UPDATE_API_URL` 在 androidApp/build.gradle.kts 与 UpdateWiring.kt 一致
- [x] `productFlavors { create("direct") }` 与 `directImplementation(...)` flavor 名一致
- [x] `UpdateState.cachedResult` 在 UpdateState.kt 定义、UpdateWiring.kt 使用一致

---

## 执行交接

Plan 完整且已保存到 `docs/superpowers/plans/2026-04-18-update-flavor-split.md`。两个执行选择：

**1. Subagent-Driven（推荐）** — 每个任务派一个全新 subagent 执行，任务间快速 review

**2. Inline Execution** — 在当前 session 顺序执行，每 phase 末尾 review checkpoint

按设计：每个 task 是 2-5 分钟单元，全部 7 phases 约 45 个 task，估算总执行时间 4-6 小时（含编译等待）。

哪种执行方式？
