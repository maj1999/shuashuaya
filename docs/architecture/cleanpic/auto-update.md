    # CleanPic — 自动升级模块设计

|文档状态| 初稿 | 2026-04-05 |

> 父文档: [overview.md](overview.md)

## 设计目标

| 目标 | 描述 |
|------|------|
| 跨平台版本检查 | 通过 Cloudflare Workers API 获取各平台最新版本信息 |
| 分平台安装策略 | Android/HarmonyOS 应用内下载安装，iOS 跳转浏览器 |
| 强制/可选更新 | 后端可标记某版本为强制更新，App 根据标记决定弹窗行为 |
| 国内外可达 | Cloudflare 全球 CDN + Workers 代理 GitHub Release 下载 |
| 分渠道分发 | 直装版含应用内升级；商店版编译期移除升级代码 |

## 架构概览

```
┌─────────────────────────────────────────────────┐
│  :update（独立 KMP 模块，direct flavor 链接）    │
│  ┌───────────────┐  ┌────────────────────────┐  │
│  │ UpdateChecker  │  │ UpdateDialog (Compose) │  │
│  │ (版本检查)     │  │ (更新弹窗 UI)          │  │
│  └───────┬───────┘  └────────────────────────┘  │
│          │                                       │
│  ┌───────▼───────┐                               │
│  │ UpdateInstaller│ ← expect 声明                │
│  │ (下载安装抽象) │                               │
│  └───────┬───────┘                               │
├──────────┼──────────────────────────────────────┤
│          │  平台层 (actual 实现)                  │
│  ┌───────┴─────────────────────────────────┐    │
│  │ Android          │ iOS      │ HarmonyOS │    │
│  │ DownloadManager  │ 跳转     │ 下载+安装 │    │
│  │ + Intent 安装    │ Safari   │ HAP 安装  │    │
│  └──────────────────┴──────────┴───────────┘    │
└─────────────────────────────────────────────────┘
            │                    ▲
            │ 通过 AppHooks slot  │ direct flavor 注入
            ▼                    │
┌─────────────────────────────────────────────────┐
│  shared（KMP 模块，完全不感知升级）              │
│  UI 通过 hooks.onAppStart / HomeOverlay /       │
│  SettingsExtras 接收外部注入                     │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  Cloudflare Workers (后端)                       │
│  ┌──────────────┐  ┌──────────────────────────┐ │
│  │ /api/version  │  │ /download/:platform/:tag │ │
│  │ 版本信息 API  │  │ GitHub Release 代理      │ │
│  └──────────────┘  └──────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

分发渠道与 shared 脱钩的具体方案详见 [auto-update-distribution.md](auto-update-distribution.md)。

## 后端设计：Cloudflare Workers

### API 接口

**GET /api/version**

返回各平台最新版本信息：

```json
{
  "android": {
    "version": "1.2.0",
    "versionCode": 13,
    "forceUpdate": false,
    "minVersion": "1.0.0",
    "changelog": "- 新增自动升级功能\n- 修复若干问题",
    "downloadUrl": "https://{worker-domain}/download/android/v1.2.0"
  },
  "ios": {
    "version": "1.2.0",
    "forceUpdate": false,
    "minVersion": "1.0.0",
    "changelog": "...",
    "downloadUrl": "https://testflight.apple.com/join/xxx"
  },
  "harmonyos": {
    "version": "1.2.0",
    "forceUpdate": false,
    "minVersion": "1.0.0",
    "changelog": "...",
    "downloadUrl": "https://{worker-domain}/download/harmonyos/v1.2.0"
  }
}
```

`forceUpdate` 和 `minVersion` 双重控制：
- `forceUpdate: true` → 该版本为强制更新
- `minVersion` → 低于此版本的用户也必须强制更新

**GET /download/:platform/:tag**

代理 GitHub Release 资源下载，解决国内访问问题。Workers 从 GitHub Release 拉取对应 asset 并流式返回。

### 版本信息维护

版本信息以 JSON 文件存储在 Workers 的环境变量或 KV 中。发布新版本时手动更新（后续可通过 GitHub Actions 自动化）。

## App 端设计

### 模块归属

升级相关代码统一归属 `:update` 独立 Gradle 模块（KMP），不再放在 `shared` 模块中。`shared` 通过 `AppHooks` slot 接口接收外部注入，自身不依赖 `:update`。具体模块拆分方案详见 [auto-update-distribution.md](auto-update-distribution.md)。

### 依赖

在 `update/build.gradle.kts` 的 commonMain 中添加 Ktor Client（KMP HTTP 客户端）：

```kotlin
// commonMain
implementation("io.ktor:ktor-client-core:3.1.1")
implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1")

// androidMain
implementation("io.ktor:ktor-client-okhttp:3.1.1")

// appleMain
implementation("io.ktor:ktor-client-darwin:3.1.1")
```

### 数据模型

```kotlin
// :update — commonMain — UpdateInfo.kt
data class UpdateInfo(
    val version: String,         // 最新版本号
    val versionCode: Int? = null,// Android 版本号
    val forceUpdate: Boolean,    // 是否强制更新
    val minVersion: String,      // 最低兼容版本
    val changelog: String,       // 更新日志
    val downloadUrl: String      // 下载/跳转地址
)

enum class UpdateStatus {
    UP_TO_DATE,       // 已是最新
    OPTIONAL_UPDATE,  // 可选更新
    FORCE_UPDATE      // 强制更新
}

data class UpdateCheckResult(
    val status: UpdateStatus,
    val updateInfo: UpdateInfo? = null
)
```

### UpdateChecker（`:update` commonMain 共享逻辑）

```kotlin
// :update — commonMain — UpdateChecker.kt
class UpdateChecker(private val httpClient: HttpClient) {

    suspend fun checkForUpdate(
        currentVersion: String,
        platform: String
    ): UpdateCheckResult

    // 语义化版本比较
    internal fun isNewerVersion(current: String, remote: String): Boolean
    internal fun isForceRequired(current: String, minVersion: String, forceUpdate: Boolean): Boolean
}
```

- 调用 `/api/version` 获取 JSON
- 根据 `platform` 参数（来自 `getPlatformName()`）取对应平台信息
- 比较版本号，返回 `UpdateCheckResult`

### UpdateInstaller（expect/actual 跨平台抽象）

```kotlin
// :update — commonMain — UpdateInstaller.kt
expect class UpdateInstaller {
    // 开始下载并安装更新
    fun startUpdate(updateInfo: UpdateInfo)

    // 下载进度回调（0.0 ~ 1.0，iOS 不适用）
    val downloadProgress: StateFlow<Float>

    // 下载状态
    val downloadState: StateFlow<DownloadState>
}

enum class DownloadState {
    IDLE, DOWNLOADING, DOWNLOADED, INSTALLING, FAILED
}
```

| 平台 | actual 实现 |
|------|------------|
| Android | `DownloadManager` 下载 APK → `FileProvider` + `ACTION_INSTALL_PACKAGE` 安装 |
| iOS | `UIApplication.shared.open(url)` 跳转 Safari |
| HarmonyOS | 下载 HAP → 系统安装接口 |

### 依赖注入（AppHooks slot 模式）

`shared` 不再持有 `UpdateChecker` / `UpdateInstaller`。`:update` 模块提供一个实现了 `AppHooks` 的对象，由 `direct` flavor 的 `MainActivity` 在启动时注入 `CleanPicApp(hooks = updateHooks)`。`store` flavor 注入 `AppHooks.Empty`，整个升级模块不被链接进 APK。

具体 slot 接口与注入方式详见 [auto-update-distribution.md](auto-update-distribution.md)。

### UI 层

#### UpdateDialog（`:update` commonMain 共享 Compose）

两种弹窗样式：
- **可选更新弹窗**：显示版本号 + 更新日志 + "立即更新" / "稍后提醒"
- **强制更新弹窗**：全屏显示，仅"立即更新"按钮，不可关闭

弹窗样式跟随当前主题 Token。

#### SettingsScreen 插入升级区块

`shared` 的设置页通过 `hooks.SettingsExtras()` slot 渲染外部内容：
- direct flavor 下，`:update` 提供的 hooks 渲染出"自动检查更新"开关、"检查更新"按钮、"有新版本"提示等
- store flavor 下，slot 为空，设置页无任何升级相关 UI

### 触发时机

1. **启动检查**：`SplashScreen` 通过 `hooks.onAppStart()` 触发。direct flavor 下钩子内部调用 `UpdateChecker.checkForUpdate()` 并缓存结果；store flavor 下为空操作。
2. **手动检查**：设置页 `hooks.SettingsExtras()` 渲染的"检查更新"按钮触发（仅 direct flavor 存在）。

### 状态管理

| 状态 | 生命周期 | 存储位置 |
|------|---------|---------|
| UpdateCheckResult | 会话级 | `:update` 模块内部（非 `shared/ServiceLocator`） |
| DownloadProgress | 下载期间 | UpdateInstaller.downloadProgress |
| hasNewVersion | 会话级 | `:update` 模块内部，供设置页红点展示 |
| autoCheckUpdate | 持久化 | AppSettings（默认 true，仅 direct flavor 有 UI 控制） |

### 开源项目适配

作为开源项目，自动升级功能需要对自编译用户友好：

1. **用户可关闭**：direct flavor 设置页提供"自动检查更新"开关，关闭后启动时不请求网络，手动检查仍可用
2. **API 地址可配置**：版本检查 URL 定义在 `BuildConfig.UPDATE_API_URL` 中，Fork 项目可指向自己的后端
3. **构建时可禁用**：使用 `store` flavor 构建，编译期完全移除升级相关 UI 和网络代码（详见 [auto-update-distribution.md](auto-update-distribution.md)）
4. **无网络降级**：版本检查失败不影响任何核心功能

## 安全与隐私影响

本模块是 App 中 **唯一的网络请求模块**（且仅在 `direct` flavor 存在），需更新架构总览中"纯本地"的描述：

- 仅 direct flavor 请求版本检查 API，不传输任何用户数据
- store flavor 完全无网络请求
- 请求内容：当前版本号 + 平台标识（无设备 ID、无用户信息）
- 下载通过 HTTPS，Cloudflare Workers 代理 GitHub Release
- 无网络时静默失败，不影响核心功能

## 分发渠道与编译期隔离

升级功能支持双分发渠道（GitHub 直装 / 应用商店），商店版在编译期完全移除升级代码以符合商店审核要求。

详见 [auto-update-distribution.md](auto-update-distribution.md)，包含：
- `:update` 独立 Gradle 模块的拆分与 `shared` 脱钩（AppHooks slot 接口）
- Android `direct` / `store` productFlavor 配置
- iOS 未来 Xcode Build Configuration 方案（本次仅预留，不落地）
- 编译产物差异对照表与字节码扫描验证手段
- `scripts/release-direct.sh` / `scripts/build-store.sh` 脚本拆分方案
- 本次分发渠道重构的文件变更清单

## 部署与配置

详见 [自动更新部署与配置指南](../../deployment/auto-update-setup.md)，包含：
- Cloudflare Workers 首次部署步骤
- 项目环境变量配置（`.env`）
- 发布新版本的一键脚本使用说明
- Fork 项目用户指南
- 故障排查
