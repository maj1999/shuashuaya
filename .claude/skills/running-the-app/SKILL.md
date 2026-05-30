---
name: running-the-app
description: Use when running, launching, installing, screenshotting, or smoke-testing the 刷刷鸭 (CleanPic) Android app itself on an emulator or device — including first-time dev-environment setup (adb/emulator not on PATH, ANDROID_HOME unset, no devices/emulators found, AVD not started) and when the launched app shows a black screen, crashes, or stays stuck on the home launcher. Not for running unit tests or Maestro E2E flows, and not for fixing the emulator's own performance or system-image problems.
---

# 运行刷刷鸭 (CleanPic) Android App

## 概述

把刷刷鸭从「干净环境」跑到「App 在模拟器前台运行」的验证路径。
核心链路：**配置环境变量 → 启动模拟器（若无设备） → 构建 APK → 安装 → 启动 → 截图确认**。

本项目目前只有 Android 完整实现，"运行项目" = 在 Android 模拟器上跑起来。
路径、SDK 位置、AVD 名都按本机动态推导，不写死任何人的目录。

## 最易踩的坑（先读这条）

**每次 Bash 调用都是一个全新 shell，环境变量不会保留。**
`emulator` 命令通常不在 PATH，`ANDROID_HOME` 可能未设。所以**每一条**用到 `emulator` / `adb` / gradle 的命令，都必须在同一条命令块开头先跑下面这段 **ENV 片段**，否则会报 `emulator: command not found` 或 `ANDROID_HOME unset`。

```bash
# === ENV：在仓库根目录执行；自动推导 Android SDK 并补全 PATH ===
cd "$(git rev-parse --show-toplevel)"
ANDROID_HOME="$(grep -s '^sdk.dir=' local.properties | cut -d= -f2-)"
ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
export ANDROID_HOME ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
```

> SDK 路径优先从项目 `local.properties` 的 `sdk.dir` 读（构建必填，最可靠），其次用已有的 `ANDROID_SDK_ROOT`，最后兜底常见默认位置。`adb` 和 `emulator` 都从 `$ANDROID_HOME` 下的 `platform-tools` / `emulator` 取——不依赖 Homebrew 或任何包管理器路径。
> **非 macOS**：默认 SDK 位置不同（Linux 通常 `$HOME/Android/Sdk`）。只要 `local.properties` 里有 `sdk.dir` 就无需关心兜底。

## 关键常量

| 项 | 值 |
|----|----|
| 仓库根 | `git rev-parse --show-toplevel` 动态获取 |
| SDK | 取自 `local.properties` 的 `sdk.dir`（构建必填项） |
| AVD | `emulator -list-avds` 的第一个（启动脚本自动选，无需指定具体名字） |
| 应用 ID / 启动 Activity | `com.cleanpic.android/.MainActivity` |
| Debug APK | `androidApp/build/outputs/apk/direct/debug/刷刷鸭-direct.apk`（注意中文名） |
| JDK | 17+（项目用 21；`scripts/build-android.sh` 走 gradle wrapper） |

## 运行步骤

所有命令在仓库根目录执行（ENV 片段已包含 `cd`）。优先用 `scripts/` 脚本（项目约定）。

### 0. 判断当前状态

```bash
cd "$(git rev-parse --show-toplevel)"
export PATH="$(grep -s '^sdk.dir=' local.properties | cut -d= -f2-)/platform-tools:$PATH"
adb devices
```

输出里有 `emulator-xxxx  device` → 设备已连接。再确认它**真的开完机**了（刚启动的模拟器也显示 `device`，但此时装 App 会失败）：

```bash
adb shell getprop sys.boot_completed
```

返回 `1` → 已就绪，**跳到第 2 步**。返回空/`0` → 还在开机，等几秒重试。
`adb devices` 是空列表 → 没有设备，先做第 1 步启动模拟器。

### 1. 启动模拟器（仅当无设备时）

脚本会自动选第一个 AVD、启动、等待开机完成、并注入测试照片/视频到相册。**耗时约 1–3 分钟，用后台运行**。先跑 ENV 片段，再：

```bash
# 先粘贴上面的 ENV 片段，然后：
scripts/emulators/android.sh start
```

看到「测试媒体注入完成。」即就绪。
若报「未找到 AVD」：用 Android Studio 创建任意一个 AVD（API 26+ / Android 8.0+）即可，脚本会自动选用。

### 2. 构建 Debug APK

构建不需要设备，可与第 1 步并行。约 1 分钟：

```bash
cd "$(git rev-parse --show-toplevel)"
scripts/build-android.sh
```

成功标志：`BUILD SUCCESSFUL` + `✅ APK: .../刷刷鸭-direct.apk`。
（`build-android.sh` 内部自带 SDK 路径处理，这步无需 ENV 片段。）

### 3. 安装并启动

```bash
# 先粘贴 ENV 片段，然后：
adb install -r "androidApp/build/outputs/apk/direct/debug/刷刷鸭-direct.apk"
adb shell am start -n com.cleanpic.android/.MainActivity
```

> `monkey -p ... LAUNCHER` 有时不会把 App 拉到前台，**用显式的 `am start -n com.cleanpic.android/.MainActivity` 最可靠**。

### 4. 确认真的跑起来了（不要跳过）

启动 ≠ 运行成功，必须看到界面：

```bash
# 先粘贴 ENV 片段，然后：
sleep 4   # 等 App 冷启动渲染；慢机器可调到 6–8 秒，黑屏时先加长等待再去查 logcat
adb shell dumpsys activity activities | grep topResumedActivity   # 应含 com.cleanpic.android/.MainActivity
adb logcat -d -b crash | tail -20                                  # 应为空（无崩溃）
adb shell screencap -p /sdcard/run.png && adb pull /sdcard/run.png /tmp/cleanpic_run.png
```

然后**用 Read 工具打开 `/tmp/cleanpic_run.png` 看一眼**：正常应显示「刷刷鸭 / 随机一刷，相册清爽」标题 + 「清理照片」「清理视频」两张卡片 + 底部设置按钮。黑屏/白屏 = 启动失败，去查 `adb logcat`。

## 收尾

```bash
# 先粘贴 ENV 片段，然后：
scripts/emulators/android.sh stop   # 关闭模拟器（可选）
```

## 踩坑速查

| 现象 | 原因 / 解法 |
|------|------------|
| `emulator: command not found` | PATH 没加 `$ANDROID_HOME/emulator`；每个命令块都要先跑 ENV 片段 |
| `adb: command not found` | 同上，ENV 片段会把 `$ANDROID_HOME/platform-tools` 加进 PATH |
| `ANDROID_HOME` 相关报错 / 推导为空 | `local.properties` 缺 `sdk.dir`；补上它，或先 `export ANDROID_SDK_ROOT=<你的SDK路径>` |
| `adb: no devices/emulators found` | 先跑第 1 步 `scripts/emulators/android.sh start` |
| `未找到 AVD` | 用 Android Studio 创建任意一个 AVD（API 26+），脚本会自动选第一个 |
| `am start` 后顶层还是 launcher | 用显式 `-n com.cleanpic.android/.MainActivity`，别用 monkey |
| 装上但黑屏 | 看 `adb logcat -d -b crash`，多半是运行时崩溃 |
| APK 路径报找不到 | 文件名是中文 `刷刷鸭-direct.apk`，命令里务必加引号 |
| 怀疑产物陈旧 / 改了代码没生效 | `scripts/build-android.sh` 是增量构建；需要全新构建时先 `./gradlew clean` 再构建 |

## 环境体检（可选）

不确定本机环境是否齐全时，先跑 ENV 片段，再：

```bash
scripts/check-env.sh
```

它会逐项报告 JDK / Gradle / adb / AVD / Maestro 等是否就绪。
