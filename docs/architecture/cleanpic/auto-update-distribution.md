# CleanPic — 升级功能的分发渠道与编译期隔离

|文档状态| 初稿 | 2026-04-12 |

> 父文档: [auto-update.md](auto-update.md)

## 设计目标

刷刷鸭需同时面向两种分发渠道：
- **直装版（direct）** —— 通过 GitHub Release 分发，包含完整应用内升级能力
- **商店版（store）** —— 通过应用商店分发，**编译期完全移除**升级相关代码与网络请求

"编译期完全移除"不是运行时 flag，而是**字节码级隔离**：store 渠道 APK 中反编译/字符串搜索均无任何 `UpdateXxx` 类、升级 API URL、升级 UI 字符串。

## 架构形态

采用"独立 Gradle 模块 + shared slot 脱钩 + productFlavor 按渠道依赖"的组合：

```
shuashuaya/
├── shared/                      ── KMP 模块，完全不感知升级
│   ├── commonMain/
│   │   ├── ui/App.kt            ── 接收 AppHooks slot
│   │   ├── ui/splash/           ── 调用 hooks.onAppStart()
│   │   ├── ui/settings/         ── 渲染 hooks.SettingsExtras()
│   │   ├── ui/AppHooks.kt       ── 不含 "update" 字样的泛型钩子接口
│   │   └── di/ServiceLocator.kt ── 移除所有 updateChecker/updateInstaller 字段
│   └── (update/ 目录整体删除)
│
├── update/                      ── 新增 KMP 模块，升级功能代码的唯一归属
│   ├── commonMain/              ── UpdateChecker / UpdateModels / UpdateDialog 等 Composable
│   ├── androidMain/             ── AndroidUpdateInstaller
│   ├── appleMain/               ── IosUpdateInstaller（空壳，预留）
│   └── ohosArm64Main/           ── HarmonyUpdateInstaller（空壳）
│
└── androidApp/
    ├── src/main/                ── 公共代码（MainActivity 骨架）
    ├── src/direct/              ── direct flavor 专属
    │   ├── AndroidManifest.xml  ── 声明 REQUEST_INSTALL_PACKAGES + FileProvider
    │   └── java/.../UpdateWiring.kt ── 实例化 :update 实现，注入 AppHooks
    └── src/store/               ── store flavor 专属
        └── java/.../UpdateWiring.kt ── 空实现，返回 AppHooks.Empty
```

## Shared 脱钩：AppHooks slot 接口

`shared` 模块通过一个**不含"update"字样**的泛型钩子接口接收外部注入，自身不依赖 `:update`：

```kotlin
// shared/commonMain/.../ui/AppHooks.kt
interface AppHooks {
    /** Splash 启动时调用，宿主可用于发起后台任务 */
    fun onAppStart() = Unit

    /** 首页叠加层（供外部渲染对话框、下载 overlay 等） */
    @Composable
    fun HomeOverlay() = Unit

    /** 设置页额外区块（供外部插入升级相关 UI） */
    @Composable
    fun SettingsExtras() = Unit

    companion object {
        val Empty = object : AppHooks {}
    }
}

// shared/commonMain/.../ui/App.kt
@Composable
fun CleanPicApp(hooks: AppHooks = AppHooks.Empty)
```

`ServiceLocator` 同步清理：移除 `updateChecker` / `updateInstaller` / `cachedUpdateResult` 字段。这些状态改由 `:update` 模块内部自持。

## Android Flavor 配置

```kotlin
// androidApp/build.gradle.kts
android {
    flavorDimensions += "distribution"
    productFlavors {
        create("direct") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_ENABLED", "true")
            buildConfigField("String",  "UPDATE_API_URL",
                "\"${AppConfig.UPDATE_API_URL}\"")
        }
        create("store") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_ENABLED", "false")
            buildConfigField("String",  "UPDATE_API_URL", "\"\"")
        }
    }
}

dependencies {
    "directImplementation"(project(":update"))   // 仅 direct 链接 :update
    // store 不声明依赖 —— :update 的字节码不会进 store APK
    implementation(project(":shared"))
}
```

`UPDATE_ENABLED` 是代码里**唯一的**运行时判断点（仅在 `MainActivity` / `UpdateWiring` 使用），用于断言当前 flavor，不做业务分支。shared 和 `:update` 模块内部**不含**此 flag 的判断。

## iOS Flavor 方案（预留，本次不落地）

iOS 侧未来接入商店版/直装版区分时，走 Xcode Build Configuration 路线：

| 要素 | 方案 |
|---|---|
| 区分维度 | 新增 `ReleaseDirect` / `ReleaseStore` 两个 Xcode Configuration |
| 依赖注入 | `ReleaseDirect` 链接 `:update` 产出的 iOS framework；`ReleaseStore` 不链接 |
| flag 传递 | Kotlin 侧通过编译期常量（`kotlin.compilerArgs`）传入 `UPDATE_ENABLED` |
| App Store 合规 | 默认 `ReleaseStore` 用于 App Store 提交，无任何升级代码 |

本次 Android 侧落地时，iOS 端保持 `:update/appleMain` 为空壳 `IosUpdateInstaller` 即可，架构已预留未来填充位置。

## 编译产物差异

| 检查项 | `direct` flavor APK | `store` flavor APK |
|---|---|---|
| `UpdateChecker` 等类字节码 | ✅ 存在 | ❌ 不编译 |
| `workers.dev` URL 字符串 | ✅ 存在 | ❌ `UPDATE_API_URL=""` |
| `REQUEST_INSTALL_PACKAGES` 权限 | ✅ 声明 | ❌ `direct/AndroidManifest.xml` 未合并进 store |
| `FileProvider` 配置 | ✅ 声明 | ❌ 同上 |
| 设置页升级区块 UI | ✅ 显示 | ❌ `SettingsExtras()` 为空 Composable |
| 首页升级弹窗 | ✅ 可能显示 | ❌ `HomeOverlay()` 为空 |
| Splash 发起升级请求 | ✅ 通过 `onAppStart()` 触发 | ❌ `onAppStart()` 空实现 |

验证手段（详见测试方案）：
1. `./gradlew :androidApp:assembleStoreRelease`
2. `unzip -p 刷刷鸭-store.apk classes.dex | strings | grep -c "UpdateChecker\|workers.dev"` → 期望 `0`
3. Manifest 解析：`aapt dump permissions 刷刷鸭-store.apk` → 期望无 `REQUEST_INSTALL_PACKAGES`

## 构建与发布脚本

`scripts/release.sh` 拆为两个独立脚本（上架流程与 GitHub Release 流程解耦）：

| 脚本 | 产物 | 用途 |
|---|---|---|
| `scripts/release-direct.sh <version> "<changelog>"` | `direct` flavor APK | 上传 GitHub Release + 更新 Cloudflare Worker 版本信息 |
| `scripts/build-store.sh <version>` | `store` flavor APK/AAB | 本地产出 `dist/` 待手动上架各应用商店 |

理由：商店上架涉及各渠道独立签名、元数据、审核材料，不适合与 GitHub 自动发布合并成单一脚本。

## 文件变更清单（本次分发渠道重构）

| 位置 | 操作 |
|---|---|
| `update/` | 新增 Gradle 模块（KMP），接入 settings.gradle.kts |
| `update/build.gradle.kts` | 新增：声明 commonMain / androidMain / appleMain / ohosArm64Main target，引入 Ktor + Compose MP 依赖 |
| `update/src/commonMain/.../*` | 从 `shared/src/commonMain/.../update/*` 迁移 |
| `update/src/androidMain/.../*` | 从 `shared/src/androidMain/.../update/*` 迁移 |
| `update/src/appleMain/.../*` | 从 `shared/src/appleMain/.../update/*` 迁移 |
| `update/src/ohosArm64Main/.../*` | 从 `shared/src/ohosArm64Main/.../update/*` 迁移 |
| `shared/src/commonMain/.../ui/AppHooks.kt` | 新增 slot 接口 |
| `shared/src/commonMain/.../ui/App.kt` | 修改：接收 `hooks: AppHooks` 参数，移除所有 UpdateXxx 引用 |
| `shared/src/commonMain/.../ui/splash/SplashScreen.kt` | 修改：调用 `hooks.onAppStart()` 替代 `updateChecker.checkForUpdate()` |
| `shared/src/commonMain/.../ui/settings/SettingsScreen.kt` | 修改：渲染 `hooks.SettingsExtras()` 替代内置 UpdateSection |
| `shared/src/commonMain/.../di/ServiceLocator.kt` | 修改：移除 `updateChecker` / `updateInstaller` / `cachedUpdateResult` 字段 |
| `androidApp/build.gradle.kts` | 修改：新增 `flavorDimensions` + `direct`/`store` flavor + `directImplementation(":update")` |
| `androidApp/src/direct/AndroidManifest.xml` | 新增：声明 `REQUEST_INSTALL_PACKAGES` + FileProvider |
| `androidApp/src/direct/java/.../UpdateWiring.kt` | 新增：构造 `UpdateAppHooks`，注入 `CleanPicApp` |
| `androidApp/src/store/java/.../UpdateWiring.kt` | 新增：返回 `AppHooks.Empty` |
| `androidApp/src/main/java/.../MainActivity.kt` | 修改：根据 flavor 注入 hooks（通过 `UpdateWiring.provideHooks()`） |
| `androidApp/src/main/AndroidManifest.xml` | 修改：移除 `REQUEST_INSTALL_PACKAGES`（下放到 direct flavor） |
| `scripts/release.sh` | 删除 |
| `scripts/release-direct.sh` | 新增（原 release.sh 的 direct flavor 版本） |
| `scripts/build-store.sh` | 新增（产出 store flavor APK/AAB 到 `dist/`） |
| `docs/product/user-stories/cleanpic.md` | 新增 US-CP-17（已完成） |
| `docs/architecture/domain-model.md` | 新增直装版/商店版/分发渠道术语（已完成） |
| `docs/architecture/overview.md` | 修改架构图与组件职责表（将 Update 节点从 shared 移至独立模块） |
| `docs/architecture/cleanpic/overview.md` | 修改子文档导航注释 |
| `docs/testing/scenarios/ep6-auto-update.md` | 新增 store flavor 相关测试场景（Step 3 完成） |
