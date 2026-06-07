# 刷刷鸭

> 随机一刷，相册清爽

一款精致的相册随机清理工具。在碎片时间打开 App，随机浏览几张照片或视频，轻松决定去留，释放手机存储空间。

**当前版本 v1.13.1** · Android 8.0+（iOS / HarmonyOS 规划中）· Kotlin Multiplatform + Compose

---

## 功能特性

- **随机清理** — 不用翻遍相册，随机挑选一小批，降低决策负担
- **照片 + 视频** — 支持照片和视频的随机浏览与批量清理
- **3 种交互模式** — 轮播相册式 / 卡片左右滑 / 全屏上下滑
- **5 套主题** — 温暖手工感 / 克制极简 / 大胆几何 / 活泼精致 / 杂志排版（每个主题独立布局，切换主题 = 换一个 App）
- **矢量图标** — 全部图标使用 SVG path + Canvas 绘制，风格随主题变化，告别 emoji
- **安全删除** — 浏览时仅标记，结果页统一确认，支持反悔
- **清理成果** — 统计页展示累计已清理数量、分类构成与设备存储情况，全程离线、记录仅存本机
- **自动更新** — 启动自动检测新版本；下载多源竞速、自动选最快可用线路（适配国内直连与境外梯子），支持强制/可选更新，可在设置页关闭
- **隐私优先** — 除版本检查外纯本地处理，不收集任何用户数据

## 截图

### 首页 / 设置 / 结果页

| 首页 | 设置页 | 结果页 |
|------|--------|--------|
| ![首页](docs/screenshots/home.png) | ![设置](docs/screenshots/settings.png) | ![结果](docs/screenshots/result.png) |

### 三种浏览模式

| 轮播相册式 | 卡片左右滑 | 全屏沉浸式 |
|-----------|-----------|-----------|
| ![轮播](docs/screenshots/viewer-carousel.png) | ![卡片](docs/screenshots/viewer-swipecard.png) | ![全屏](docs/screenshots/viewer-fullscreen.png) |

## 技术栈

| 项目 | 技术 |
|------|------|
| 语言 | Kotlin (Multiplatform) |
| UI 框架 | Compose Multiplatform |
| 目标平台 | Android 8.0+ / iOS 14.0+ / HarmonyOS NEXT 5.0+ |
| 图片 / 视频 | Coil 3 / Media3 ExoPlayer |
| 网络 / 更新 | Ktor Client（Android OkHttp / iOS Darwin），多源竞速下载 |
| 构建工具 | Gradle 8.5 + AGP 8.2.2（compileSdk 34） |
| 测试 | Kotlin Test（单元）+ Maestro（E2E） |

## 项目结构

```
cleanpic/
├── shared/                     # KMP 共享模块（UI / 图标 / 主题 / 媒体 / 设置 / 权限 / DI）
│   └── src/
│       ├── commonMain/         # 跨平台共享代码
│       │   └── kotlin/com/cleanpic/
│       │       ├── ui/         # Compose UI（5 页面 × 5 主题布局）
│       │       ├── icons/      # 矢量图标系统（SVG Path 解析 + Canvas 渲染）
│       │       ├── viewmodel/  # ViewModel 层
│       │       ├── model/      # 数据模型
│       │       ├── media/      # 媒体仓库 + 随机选取
│       │       ├── theme/      # 主题管理（5 套 ThemeTokens + 布局标识）
│       │       ├── settings/   # 偏好设置
│       │       ├── permission/ # 权限管理
│       │       └── di/         # 依赖注入
│       ├── androidMain/        # Android 平台实现
│       ├── appleMain/          # iOS 平台实现
│       └── commonTest/         # 单元测试
├── update/                     # 自动更新独立模块
│   └── src/
│       ├── commonMain/         # 版本检查 + 选源竞速 + 更新弹窗（UpdateChecker / DownloadSourceSelector / UpdateDialog）
│       ├── androidMain/        # Android 下载安装（ktor 流式下载 ApkDownloader + AndroidUpdateInstaller + 完整性校验）
│       ├── appleMain/          # iOS 安装（IosUpdateInstaller）
│       ├── commonTest/         # 选源 / 版本检查单测
│       └── androidUnitTest/    # 下载 / 完整性单测
├── androidApp/                 # Android 壳工程（direct / store 两个 flavor）
├── worker/                     # （遗留）旧 Cloudflare Worker；现已改为 Gitee 静态 version.json 分发，不再部署
├── maestro/                    # E2E 测试
│   ├── flows/direct/           # direct flavor 测试流（照片 25 个）
│   ├── flows/direct/video/     # 视频测试流（7 个，需 ffmpeg 生成资源）
│   ├── flows/store/            # store flavor 测试流（16 个）
│   └── screenshots/            # 测试截图输出（git 忽略）
├── scripts/                    # 构建 / 运行 / 测试 / 发布脚本
├── test-assets/                # 测试媒体（本地生成）
└── docs/                       # 产品 / 架构 / 测试 / 部署文档
```

## 快速开始

### 环境要求

- JDK 17
- Android Studio（含 Android SDK 34）
- ffmpeg（生成测试媒体，可选）
- Maestro（E2E 测试，可选）

```bash
scripts/check-env.sh        # 一键检查环境是否就绪
```

### 1. 启动模拟器

**方式一：使用脚本**（推荐，需已创建 AVD）

```bash
scripts/emulators/android.sh start          # 启动并自动等待就绪 + 注入测试媒体
scripts/emulators/android.sh start --cold    # 冷启动（忽略快照）
scripts/emulators/android.sh status          # 查看状态
```

**方式二：纯命令行创建 AVD**（无需 Android Studio）

```bash
brew install --cask android-commandlinetools          # 1. 安装命令行工具（macOS）
yes | sdkmanager --licenses                            # 2. 接受许可
sdkmanager "platform-tools" "platforms;android-34" "emulator" "system-images;android-34;google_apis;arm64-v8a"  # 3. 安装组件
avdmanager create avd -n CleanPic_API34 -k "system-images;android-34;google_apis;arm64-v8a" -d pixel_6           # 4. 创建 AVD
emulator -avd CleanPic_API34 &                         # 5. 启动
adb wait-for-device && adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'  # 6. 等待就绪
```

> Intel Mac 用 `x86_64` 替换 `arm64-v8a`；`emulator` 不在 PATH 时完整路径通常为 `$ANDROID_HOME/emulator/emulator`。
> 已有模拟器运行或连接了实机可跳过此步，`adb devices` 确认设备在线即可。

### 2. 构建并安装

```bash
scripts/build-android.sh        # 构建 direct flavor（store flavor：scripts/build-android.sh store）
scripts/test-android.sh deploy  # 安装到已连接的设备 / 模拟器
```

构建产物：`androidApp/build/outputs/apk/direct/debug/刷刷鸭-direct.apk`。手动安装亦可：

```bash
adb install -r androidApp/build/outputs/apk/direct/debug/刷刷鸭-direct.apk
adb shell am start -n com.cleanpic.android/.MainActivity
```

App 启动后授予相册权限即可使用。

### 3. 运行测试

```bash
scripts/test.sh                                      # 全部单元测试（shared + update 模块）
scripts/generate-test-assets.sh                      # 生成测试媒体（12 张照片 + 2 个视频，E2E 前需要）
~/.maestro/bin/maestro test maestro/flows/direct/    # E2E 照片测试（25 个，需 Maestro + 模拟器 + 已部署 App）
~/.maestro/bin/maestro test maestro/flows/direct/video/  # E2E 视频测试（7 个，额外需 ffmpeg 视频资源）

scripts/test-android.sh         # 一键：构建 + 部署 + 跑全量 E2E
```

## 构建变体与发布

项目有 `direct` / `store` 两个 productFlavor：

- **direct** — 含应用内升级，走 GitHub / Gitee Release 渠道分发
- **store** — 编译期完全剥离应用内升级代码、URL、权限，用于上架各应用商店

```bash
# direct 渠道发布（含自动更新）：更新版本号 → 构建 Release APK
#   → 上传 Gitee（国内主源）+ GitHub（境外备用源）→ 更新 version.json（含匿名下载 sha256 门禁校验）
./scripts/release-direct.sh 1.13.1 "更新说明"

# store 渠道构建：编译期移除升级代码（经字节码扫描确认），需手动签名上架
./scripts/build-store.sh 1.13.1
# 输出: dist/刷刷鸭-store-v1.13.1.apk
```

## 自动更新机制

更新分发为**纯静态**，不依赖任何服务端（已弃用早期的 Cloudflare Worker）：

1. **检测** — 客户端读取 Gitee 上的静态 `update/version.json`，与本地 versionCode 比较，判定无更新 / 可选更新 / 强制更新；网络异常安全降级为"已是最新"。
2. **选源** — 下载前**并发探测** Gitee（国内主源）与 GitHub（境外备用源），只读响应头判定连通，自动选**当前网络最快可达**的线路——零地域判断，国内直连与境外梯子都适配。
3. **下载** — 用 App 内 ktor（与检测同一网络栈，"能检查就能下"）流式下载到外部私有目录，实时回调进度；单源卡死 / 失败自动切换下一源。
4. **校验与安装** — 对下载包做 `sha256` + ZIP 魔数完整性校验（挡住防盗链 / 风控返回的 HTML 伪装页），通过后经 FileProvider 拉起系统安装器。

> 设计详见 [自动更新设计文档](docs/architecture/cleanpic/auto-update.md)。

## 测试覆盖

### 单元测试

| 测试类 | 用例数 | 覆盖 |
|--------|--------|------|
| RandomPickerTest | 7 | 随机选取、去重、边界 |
| ThemeManagerTest | 6 | 5 主题切换、旧 ID 迁移回退、Token 完整性 |
| ThemeTokensTest | 5 | 枚举值验证、WarmTheme 参数校验 |
| AppSettingsTest | 5 | 读写、默认值、非法值防御、autoCheckUpdate |
| UpdateCheckerTest | 32 | 版本比较、强制更新、平台提取、完整评估、双端点 fallback |
| DownloadSourceSelectorTest | 4 | 选源竞速：跳过失败源、最快优先、全失败、忽略 Range 仍成功 |
| ApkDownloaderTest | 4 | ktor 流式下载、失败切源、完整性挡 HTML 伪装包、进度回调 |
| ApkIntegrityTest | 6 | ZIP 魔数 / size / sha256 校验、缺字段向后兼容 |
| AppIconsTest | 3 | 图标定义、主题参数跟随、未知名异常 |
| SvgPathParserTest | 13 | SVG 命令解析（M/L/H/V/C/S/A/Z）、图标 path 全量验证 |
| ViewerViewModelTest | 13 | 加载、标记、删除、统计 |
| PlatformTest | 1 | 平台标识 |

### E2E 测试（Maestro）

- **照片**（`maestro/flows/direct/`，25 个）— 完整清理流程、删除确认、全部保留、再来一轮、5 主题切换、三种交互模式、缩略图、结果页统计、设置页更新区、自动检查更新开关、清理成果页等
- **视频**（`maestro/flows/direct/video/`，7 个）— 视频清理、缩略图 + 时长、三种模式下的视频播放与静音
- **store flavor**（`maestro/flows/store/`，16 个）— 验证商店版已剥离更新相关入口与请求

## 开发路线图

### V1.0 — 核心清理

- [x] 随机照片 / 视频清理（浏览 + 标记 + 确认删除）
- [x] 5 套主题切换 · 3 种交互模式（轮播 / 卡片滑动 / 全屏）
- [x] 每轮数量可配置（5/10/15/20）· 批量延迟删除 + 反悔 · 再来一轮（自动去重）
- [x] Android 权限请求流程 · 真实缩略图加载（Coil 3）· 视频播放（ExoPlayer，三种模式）

### V1.1 — 主题与图标

- [x] **UI 主题全面重设计** — 5 个主题各有独立布局，切换主题等于换一个 App
- [x] **矢量图标系统** — SVG Path 解析器 + Canvas 渲染，主题化图标替换全部 emoji
- [x] **State 分发架构** — 业务逻辑与 UI 布局分离，每页 5 个布局变体

### V1.2 — 自动更新

- [x] **自动更新系统** — 版本检查 + 更新弹窗，支持强制 / 可选更新
- [x] **设置页更新入口** — 自动检查开关、手动检查按钮、红点提示新版本
- [x] **一键发布脚本** — `scripts/release-direct.sh` 自动完成版本号更新 → 构建 → Release

### V1.3（当前，v1.13.x）

- [x] **清理成果统计页** — 累计已清理数量、分类构成、设备存储情况（全程离线，仅存本机）
- [x] **分发改纯静态** — Gitee 静态 version.json（国内主源）+ GitHub Release（境外备用），替代 Cloudflare Worker
- [x] **多源自适应下载** — App 内 ktor 流式下载 + 选源竞速，自动选最快可达线路，修复梯子环境下载卡在 0% 的问题
- [ ] HarmonyOS NEXT 适配
- [ ] 清理历史记录
- [ ] Widget 桌面小组件（"今日清理"快捷入口）
- [ ] 减少动效模式（响应系统无障碍偏好）
- [ ] iOS 平台适配

### V2.0（远期）

- [ ] AI 智能分类（模糊照片、截图、重复照片自动标记）
- [ ] 云端备份提醒（检测到未备份照片时提示）
- [ ] 社交分享（清理成就卡片）

## 文档索引

| 文档 | 路径 |
|------|------|
| 产品需求 | [docs/product/prd.md](docs/product/prd.md) |
| 用户故事 | [docs/product/user-stories/](docs/product/user-stories/) |
| 系统架构 | [docs/architecture/overview.md](docs/architecture/overview.md) |
| 技术栈选型 | [docs/architecture/tech-stack.md](docs/architecture/tech-stack.md) |
| 主题系统（v2） | [docs/architecture/cleanpic/theme-system.md](docs/architecture/cleanpic/theme-system.md) |
| UI 重设计 Spec | [docs/superpowers/specs/2026-04-04-ui-theme-redesign-design.md](docs/superpowers/specs/2026-04-04-ui-theme-redesign-design.md) |
| 自动更新设计 | [docs/architecture/cleanpic/auto-update.md](docs/architecture/cleanpic/auto-update.md) |
| 测试策略 | [docs/testing/strategy.md](docs/testing/strategy.md) |
| 测试场景 | [docs/testing/scenarios/](docs/testing/scenarios/) |

## 许可证

私有项目，保留所有权利。
