# CleanPic — 多平台模拟测试环境设计方案

| 文档状态 | 已确认 | 2026-03-28 |
|---------|--------|-----------|

## 一、背景与目标

CleanPic 是基于 KuiklyUI 的 Kotlin Multiplatform 项目，目标平台为 Android、iOS、HarmonyOS NEXT。当前开发机（macOS Apple Silicon）上未安装任何模拟器环境。

**目标：** 在开发机上搭建完整的三端模拟测试环境，使开发者无需真机即可完成日常开发和测试。

**核心决策：** 采用本地 + 云端混合方案——三端 IDE 全部本地安装，HarmonyOS 额外接入华为 AGC 云测试弥补模拟器的硬件功能缺陷。

**前置条件：** HarmonyOS 的 `ohosArm64` Gradle target 当前被注释（依赖 KuiklyUI Gradle 插件对 ohos 的支持就绪）。在该 target 解除注释之前，`test-harmony.sh` 和 `test-harmony-cloud.sh` 暂不可用，HarmonyOS 相关脚本将在 target 启用后生效。Android 和 iOS 的环境搭建不受此影响。

## 二、工具链架构

```
开发机 (macOS Apple Silicon)
├── Android Studio + Android Emulator
│   ├── API 34 系统镜像 (arm64)
│   └── Maestro CLI（自动化编排）
├── Xcode + iOS Simulator
│   ├── iOS 17/18 Runtime
│   └── Maestro CLI（共用同一个）
├── DevEco Studio + HarmonyOS Emulator
│   ├── HarmonyOS NEXT 系统镜像
│   └── hdc 命令行工具
└── 共享测试层
    ├── ./gradlew :shared:allTests（commonTest）
    └── scripts/test.sh（已有）
```

### 各平台角色定位

| 平台 | 本地模拟器职责 | 云端职责 |
|------|-------------|---------|
| Android | UI 调试 + 功能验证 + 自动化测试（Maestro） | 无需云端 |
| iOS | UI 调试 + 功能验证 + 自动化测试（Maestro） | 无需云端 |
| HarmonyOS | UI 布局调试 + 基础逻辑验证 | AGC 云调试：媒体/权限/视频真机验证；AGC 云测试：兼容性扫描 |

### 磁盘预算

| 工具 | 预估占用 |
|------|---------|
| Android Studio + SDK + Emulator 镜像 | ~15GB |
| Xcode + iOS Simulator Runtime | ~30GB |
| DevEco Studio + SDK + Emulator 镜像 | ~10GB |
| Maestro CLI | ~200MB |
| **合计** | **~55GB** |

## 三、测试分层与平台覆盖矩阵

### 与现有 L1-L4 策略的衔接

| 层级 | 运行方式 | Android | iOS | HarmonyOS |
|------|---------|---------|-----|-----------|
| L1 单元 | `scripts/test.sh` | 跨平台 commonTest | 跨平台 commonTest | 跨平台 commonTest |
| L2 组件 | `scripts/test.sh` | 跨平台 commonTest | 跨平台 commonTest | 跨平台 commonTest |
| L3 集成 | 各平台脚本 | Emulator | Simulator | 模拟器(UI) + AGC 云真机(媒体/权限) |
| L4 E2E | 各平台脚本 | Maestro + Emulator | Maestro + Simulator | ArkTS + 模拟器/AGC 云真机 |

### L3 集成测试——各平台能力矩阵

| 测试项 | Android Emulator | iOS Simulator | HarmonyOS 模拟器 | AGC 云真机 |
|-------|-----------------|--------------|-----------------|-----------|
| 相册读取 (MediaRepository) | MediaStore 可用 | PHAsset 可用 | stub，无真实相册 | 真机相册 |
| 权限申请 (PermissionManager) | 完整权限弹窗 | 完整权限弹窗 | 基础权限可模拟 | 完整权限流程 |
| 视频播放 (VideoPlayer) | 硬件解码 | 硬件解码 | 仅软解，易卡顿 | 真机硬解 |
| 偏好存储 (AppSettings) | SharedPreferences | NSUserDefaults | 模拟器可验证 | 可验证 |
| 批量删除确认 | 可验证 | 可验证 | 可测 UI 流程 | 真实删除 |

### L4 E2E 自动化工具选型

| 平台 | 工具 | 脚本格式 | 运行目标 |
|------|------|---------|---------|
| Android | Maestro | YAML | 本地 Emulator |
| iOS | Maestro | YAML（可复用大部分 Android 脚本） | 本地 Simulator |
| HarmonyOS | hdc + ArkTS 测试框架 | ArkTS | 本地模拟器 + AGC 云真机 |

## 四、脚本体系与日常工作流

### scripts/ 目录规划

```
scripts/
├── test.sh                    # 已有，运行 commonTest
├── check-env.sh               # 环境检查，一键验证所有工具就绪
├── emulator-android.sh        # 启动/停止 Android Emulator
├── emulator-ios.sh            # 启动/停止 iOS Simulator
├── emulator-harmony.sh        # 启动/停止 HarmonyOS Emulator
├── test-android.sh            # 构建并部署到 Android Emulator + 跑 Maestro
├── test-ios.sh                # 构建并部署到 iOS Simulator + 跑 Maestro
├── test-harmony.sh            # 构建并部署到 HarmonyOS Emulator
├── test-harmony-cloud.sh      # 触发 AGC 云测试任务
└── test-all-platforms.sh      # 一键顺序执行三端测试
```

### 日常开发工作流

**改了 commonMain 共享代码：**
```bash
scripts/test.sh                  # 先跑 L1/L2 共享测试（<10s）
scripts/test-android.sh          # 部署到 Android 模拟器验证
```

**改了某个平台的原生实现：**
```bash
scripts/emulator-android.sh start
scripts/test-android.sh          # 只跑该平台
```

**提交 MR 前全平台验证：**
```bash
scripts/test-all-platforms.sh    # 顺序跑三端
```

**鸿蒙端需要真机功能验证：**
```bash
scripts/test-harmony-cloud.sh    # 触发 AGC 云调试
```

### 日志输出

统一输出到 `logs/`：

```
logs/
├── test-common.log
├── test-android.log
├── test-ios.log
├── test-harmony.log
└── test-harmony-cloud.log
```

## 五、环境搭建步骤

### 安装顺序

```
Step 1: Xcode            — App Store 安装，下载 iOS 17/18 Simulator Runtime
Step 2: Android Studio    — 官网 ARM 版，安装 SDK 34 + arm64 镜像，创建 AVD
Step 3: DevEco Studio     — 官网最新版(6.0.1)，安装 SDK + 系统镜像，创建模拟器
Step 4: Maestro CLI       — curl -Ls "https://get.maestro.mobile.dev" | bash
Step 5: AGC 云测试        — 注册华为开发者账号，实名认证，开通服务
```

### 环境检查

`scripts/check-env.sh` 一键检查所有工具是否就绪：

```
[✓] Xcode 16.2           — iOS Simulator: 3 devices available
[✓] Android Studio 2025.1 — AVD: Pixel_7_API_34 (arm64)
[✓] DevEco Studio 6.0.1   — Emulator: Phone_API_12
[✓] Maestro 1.39
[✓] hdc 3.1.0
[✗] AGC 云测试             — 未配置凭证（非阻塞，仅鸿蒙真机需要）
```

## 六、已知限制与应对

### Android Emulator

| 已知问题 | 应对策略 |
|---------|---------|
| 首次启动冷启动慢（~60s） | 首次启动后保存快照，后续快照启动（~5s） |
| MediaStore 中无测试媒体 | `emulator-android.sh` 启动时通过 `adb push` 注入 test-assets/ |
| API 33+ 权限模型变化 | Maestro 脚本中用 `adb shell pm grant` 预授权 |

### iOS Simulator

| 已知问题 | 应对策略 |
|---------|---------|
| 相册为空 | `emulator-ios.sh` 启动时通过 `xcrun simctl addmedia` 注入 test-assets/ |
| 不支持摄像头 | CleanPic 不需要拍照，不影响 |

### HarmonyOS Emulator

| 已知问题 | 应对策略 |
|---------|---------|
| 视频仅软解，容易卡顿 | 本地只验证 UI 流程，视频播放走 AGC 云真机 |
| 无真实相册数据 | 本地只测 UI + stub 逻辑，相册读取走 AGC 云真机 |
| 物理键盘无法输入中文 | 使用模拟器内软键盘 |
| 网络可能异常（VirtWifi） | `emulator-harmony.sh` 中配置代理参数 |

### AGC 云测试/云调试

| 已知问题 | 应对策略 |
|---------|---------|
| 每日 300 分钟限制 | 日常开发用本地模拟器，AGC 只用于真机验证环节 |
| 每种测试类型每天最多 10 次 | 合理规划测试批次 |
| 热门设备可能排队 | 非高峰时段执行，或选择冷门但同配置的设备 |
| IDE 插件连接需在中国大陆 | 通过浏览器访问 AGC 控制台 |

### Maestro + KuiklyUI 兼容性（待验证）

Maestro 的 UI 元素定位依赖 Accessibility 节点树。KuiklyUI 非标准 Jetpack Compose / SwiftUI，其渲染层可能不会自动暴露标准的 accessibility 标签。需要在环境搭建完成后尽早验证 Maestro 能否正确识别 KuiklyUI 渲染的 UI 元素。若不兼容，L4 E2E 自动化方案需切换为截图对比或其他方案。

## 七、测试媒体资源

三端共用统一的测试媒体，由各平台启动脚本自动注入：

```
test-assets/
├── photos/
│   ├── test_01.jpg  (1MB, 1080x1920)
│   ├── test_02.jpg  (3MB, 2160x3840)
│   └── test_03.png  (500KB, 720x1280)
└── videos/
    ├── test_01.mp4  (5MB, 10s, 1080p)
    └── test_02.mp4  (15MB, 30s, 4K)
```

test-assets/ 中包含大体积二进制文件（视频最大 15MB），应使用 Git LFS 管理以避免仓库膨胀。需在项目中配置 `.gitattributes`：

```
test-assets/**/*.mp4 filter=lfs diff=lfs merge=lfs -text
test-assets/**/*.jpg filter=lfs diff=lfs merge=lfs -text
test-assets/**/*.png filter=lfs diff=lfs merge=lfs -text
```

## 八、关键设计决策

| 决策 | 理由 |
|------|------|
| 三端 IDE 全部本地安装 | KMP 项目需要三端编译，IDE 是刚需 |
| Maestro 统一 Android/iOS E2E | YAML 脚本可跨平台复用，降低维护成本 |
| HarmonyOS 本地 + AGC 云端混合 | 模拟器媒体功能受限，云真机补位且免费 |
| 统一 test-assets/ 管理测试媒体 | 确保三端测试数据一致性 |
| 统一 scripts/ 入口 | 遵循项目规范，降低操作复杂度 |
