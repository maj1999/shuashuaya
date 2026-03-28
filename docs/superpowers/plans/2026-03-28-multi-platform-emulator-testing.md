# 多平台模拟测试环境 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 Android Emulator + iOS Simulator + HarmonyOS Emulator + AGC 云测试的完整测试脚本体系，使开发者可通过 scripts/ 目录一键启停模拟器、注入测试媒体、运行各平台测试。

**Architecture:** 所有操作通过 scripts/ 下的 shell 脚本驱动，日志统一输出到 logs/。测试媒体资源存放在 test-assets/，由 Git LFS 管理。Maestro YAML 脚本编排 Android/iOS E2E 测试，HarmonyOS 用 hdc + ArkTS。

**Tech Stack:** Bash、adb、xcrun simctl、hdc、Maestro CLI、Git LFS

**Spec:** `docs/superpowers/specs/2026-03-28-multi-platform-emulator-testing-design.md`

---

## 文件结构

### 新增文件

```
.gitattributes                                — Git LFS 规则
.gitignore                                    — 追加 test-assets 媒体文件排除
scripts/generate-test-assets.sh               — 测试媒体生成脚本
scripts/check-env.sh                          — 环境检查脚本
scripts/test-android.sh                       — Android 构建 + 部署 + Maestro 测试
scripts/test-ios.sh                           — iOS 构建 + 部署 + Maestro 测试
scripts/test-harmony.sh                       — HarmonyOS 构建 + 部署（待 target 启用）
scripts/test-all-platforms.sh                 — 一键顺序执行三端测试
scripts/emulators/android.sh                  — Android Emulator 启停 + 媒体注入
scripts/emulators/ios.sh                      — iOS Simulator 启停 + 媒体注入
scripts/emulators/harmony.sh                  — HarmonyOS Emulator 启停
scripts/emulators/harmony-cloud.sh            — AGC 云测试引导
maestro/config.yaml                           — Maestro 全局配置
maestro/flows/browse-photos.yaml              — Maestro E2E: 浏览照片流程
maestro/flows/delete-confirm.yaml             — Maestro E2E: 标记删除 + 确认流程
maestro/flows/switch-theme.yaml               — Maestro E2E: 切换主题流程
test-assets/photos/.gitkeep                   — 目录占位
test-assets/videos/.gitkeep                   — 目录占位
```

### 修改文件

```
scripts/test.sh                               — 增加日志输出到 logs/test-common.log
```

### 目录结构（符合每层 <= 8 文件规范）

```
scripts/               (8 个文件: test.sh, build-android.sh, check-env.sh, generate-test-assets.sh,
                        test-android.sh, test-ios.sh, test-harmony.sh, test-all-platforms.sh)
scripts/emulators/     (4 个文件: android.sh, ios.sh, harmony.sh, harmony-cloud.sh)
maestro/               (1 个文件: config.yaml)
maestro/flows/         (3 个文件: browse-photos.yaml, delete-confirm.yaml, switch-theme.yaml)
test-assets/photos/    (占位 .gitkeep，实际媒体由脚本生成且不入 git)
test-assets/videos/    (占位 .gitkeep，实际媒体由脚本生成且不入 git)
```

---

## Task 1: Git LFS 配置 + 测试媒体资源准备

**Files:**
- Create: `.gitattributes`
- Create: `test-assets/photos/.gitkeep`
- Create: `test-assets/videos/.gitkeep`
- Create: `scripts/generate-test-assets.sh`

本任务用脚本生成测试用的占位媒体文件，避免手动准备大文件。

- [ ] **Step 1: 安装并初始化 Git LFS**

```bash
brew install git-lfs
git lfs install
```

Expected: `Git LFS initialized.`

- [ ] **Step 2: 创建 .gitattributes**

```gitattributes
test-assets/**/*.mp4 filter=lfs diff=lfs merge=lfs -text
test-assets/**/*.jpg filter=lfs diff=lfs merge=lfs -text
test-assets/**/*.png filter=lfs diff=lfs merge=lfs -text
```

- [ ] **Step 3: 创建测试媒体生成脚本 scripts/generate-test-assets.sh**

用 ffmpeg 生成标准尺寸的测试图片和视频（纯色 + 文字标注尺寸信息，足够测试用途）：

```bash
#!/usr/bin/env bash
#
# 生成测试用媒体资源（需要 ffmpeg）
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSETS_DIR="$PROJECT_ROOT/test-assets"

# 检查 ffmpeg
if ! command -v ffmpeg &>/dev/null; then
    echo "错误: 需要 ffmpeg。请运行: brew install ffmpeg"
    exit 1
fi

mkdir -p "$ASSETS_DIR/photos" "$ASSETS_DIR/videos"

echo "=== 生成测试照片 ==="

# test_01.jpg — 1080x1920, ~1MB
ffmpeg -y -f lavfi -i "color=c=#4A90D9:s=1080x1920:d=1" \
    -vf "drawtext=text='test_01 1080x1920':fontsize=60:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -frames:v 1 "$ASSETS_DIR/photos/test_01.jpg" 2>/dev/null
echo "  test_01.jpg (1080x1920)"

# test_02.jpg — 2160x3840, ~3MB
ffmpeg -y -f lavfi -i "color=c=#D94A4A:s=2160x3840:d=1" \
    -vf "drawtext=text='test_02 2160x3840':fontsize=120:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -frames:v 1 -q:v 2 "$ASSETS_DIR/photos/test_02.jpg" 2>/dev/null
echo "  test_02.jpg (2160x3840)"

# test_03.png — 720x1280, ~500KB
ffmpeg -y -f lavfi -i "color=c=#4AD97A:s=720x1280:d=1" \
    -vf "drawtext=text='test_03 720x1280':fontsize=40:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -frames:v 1 "$ASSETS_DIR/photos/test_03.png" 2>/dev/null
echo "  test_03.png (720x1280)"

echo "=== 生成测试视频 ==="

# test_01.mp4 — 1080p, 10s, ~5MB
ffmpeg -y -f lavfi -i "color=c=#D9A84A:s=1920x1080:d=10:r=30" \
    -vf "drawtext=text='test_01 1080p %{pts\:hms}':fontsize=60:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -c:v libx264 -preset fast -crf 23 \
    "$ASSETS_DIR/videos/test_01.mp4" 2>/dev/null
echo "  test_01.mp4 (1080p, 10s)"

# test_02.mp4 — 4K, 30s, ~15MB
ffmpeg -y -f lavfi -i "color=c=#8A4AD9:s=3840x2160:d=30:r=30" \
    -vf "drawtext=text='test_02 4K %{pts\:hms}':fontsize=120:fontcolor=white:x=(w-tw)/2:y=(h-th)/2" \
    -c:v libx264 -preset fast -crf 28 \
    "$ASSETS_DIR/videos/test_02.mp4" 2>/dev/null
echo "  test_02.mp4 (4K, 30s)"

echo "=== 测试媒体生成完成 ==="
ls -lh "$ASSETS_DIR/photos/" "$ASSETS_DIR/videos/"
```

- [ ] **Step 4: 运行生成脚本验证**

```bash
chmod +x scripts/generate-test-assets.sh
scripts/generate-test-assets.sh
```

Expected: 5 个文件生成到 test-assets/，输出文件列表和大小。

- [ ] **Step 5: Commit**

- [ ] **Step 5: 创建 .gitkeep 占位文件并更新 .gitignore**

```bash
mkdir -p test-assets/photos test-assets/videos
touch test-assets/photos/.gitkeep test-assets/videos/.gitkeep
```

在 `.gitignore` 末尾追加（媒体文件不入 git，仅由脚本本地生成）：

```
# Test assets (generated locally, not tracked)
test-assets/**/*.jpg
test-assets/**/*.png
test-assets/**/*.mp4
!test-assets/**/.gitkeep
```

- [ ] **Step 6: Commit**

```bash
git add .gitattributes .gitignore scripts/generate-test-assets.sh test-assets/
git commit -m "feat: add Git LFS config, test asset generation script, and .gitkeep"
```

---

## Task 2: 环境检查脚本 check-env.sh

**Files:**
- Create: `scripts/check-env.sh`

- [ ] **Step 1: 编写 check-env.sh**

```bash
#!/usr/bin/env bash
#
# 检查多平台模拟测试环境是否就绪
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

PASS=0
FAIL=0
WARN=0

check() {
    local label="$1"
    local cmd="$2"
    local detail

    if detail=$(eval "$cmd" 2>/dev/null); then
        echo "[OK] $label — $detail"
        ((PASS++))
    else
        echo "[NO] $label"
        ((FAIL++))
    fi
}

check_warn() {
    local label="$1"
    local cmd="$2"
    local detail

    if detail=$(eval "$cmd" 2>/dev/null); then
        echo "[OK] $label — $detail"
        ((PASS++))
    else
        echo "[--] $label（非阻塞）"
        ((WARN++))
    fi
}

echo "=== CleanPic: 多平台测试环境检查 ==="
echo ""

# Java / Gradle
check "JDK" \
    "java -version 2>&1 | head -1 | sed 's/.*\"\(.*\)\".*/\1/' | xargs -I{} echo 'JDK {}'"

check "Gradle Wrapper" \
    "cd '$PROJECT_ROOT' && ./gradlew --version 2>/dev/null | grep '^Gradle ' | head -1"

# Xcode / iOS Simulator
check "Xcode" \
    "xcodebuild -version 2>/dev/null | head -1"

check "iOS Simulator" \
    "xcrun simctl list devices available 2>/dev/null | grep -c 'iPhone' | xargs -I{} echo '{} devices available'"

# Android Studio / Emulator
check "Android SDK (adb)" \
    "adb version 2>/dev/null | head -1"

check "Android Emulator (AVD)" \
    "emulator -list-avds 2>/dev/null | head -1 | xargs -I{} echo 'AVD: {}'"

# DevEco Studio / HarmonyOS
check_warn "HarmonyOS SDK (hdc)" \
    "hdc version 2>/dev/null | head -1"

# Maestro
check_warn "Maestro" \
    "maestro --version 2>/dev/null | head -1"

# ffmpeg (test asset generation)
check_warn "ffmpeg" \
    "ffmpeg -version 2>/dev/null | head -1 | awk '{print \$1,\$2,\$3}'"

# Git LFS
check "Git LFS" \
    "git lfs version 2>/dev/null | head -1"

# Test assets
check_warn "test-assets" \
    "ls '$PROJECT_ROOT/test-assets/photos/test_01.jpg' >/dev/null 2>&1 && echo '媒体文件已生成'"

echo ""
echo "=== 结果: $PASS 通过, $FAIL 失败, $WARN 可选项未就绪 ==="

if [ "$FAIL" -gt 0 ]; then
    echo "请先安装缺失的必要工具。"
    exit 1
fi
```

- [ ] **Step 2: 运行验证**

```bash
chmod +x scripts/check-env.sh
scripts/check-env.sh
```

Expected: 输出各工具的检查状态，JDK / Gradle / Git LFS 应通过，IDE 相关工具根据安装情况显示。脚本不应报错退出（即使工具未安装也只是显示 `[NO]`）。

- [ ] **Step 3: Commit**

```bash
git add scripts/check-env.sh
git commit -m "feat: add multi-platform environment check script"
```

---

## Task 3: Android Emulator 启停脚本

**Files:**
- Create: `scripts/emulators/android.sh`

- [ ] **Step 1: 创建目录并编写 scripts/emulators/android.sh**

```bash
mkdir -p scripts/emulators
```

```bash
#!/usr/bin/env bash
#
# Android Emulator 启动/停止/状态查询
#
# 用法:
#   scripts/emulators/android.sh start        — 启动模拟器并注入测试媒体
#   scripts/emulators/android.sh start --cold  — 冷启动（忽略快照）
#   scripts/emulators/android.sh stop          — 关闭模拟器
#   scripts/emulators/android.sh status        — 查看模拟器状态
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ASSETS_DIR="$PROJECT_ROOT/test-assets"
LOG_FILE="$PROJECT_ROOT/logs/emulator-android.log"

mkdir -p "$PROJECT_ROOT/logs"

get_avd() {
    local avd
    avd=$(emulator -list-avds 2>/dev/null | head -1)
    if [ -z "$avd" ]; then
        echo "错误: 未找到 AVD。请在 Android Studio 中创建一个模拟器。" >&2
        exit 1
    fi
    echo "$avd"
}

inject_media() {
    echo "注入测试媒体到模拟器..."
    if [ -d "$ASSETS_DIR/photos" ]; then
        adb push "$ASSETS_DIR/photos/." /sdcard/DCIM/TestPhotos/ 2>/dev/null || true
    fi
    if [ -d "$ASSETS_DIR/videos" ]; then
        adb push "$ASSETS_DIR/videos/." /sdcard/DCIM/TestVideos/ 2>/dev/null || true
    fi
    # 触发 MediaStore 扫描（API 30+ 兼容方式）
    adb shell cmd media scan volume external_primary 2>/dev/null || true
    echo "测试媒体注入完成。"
}

case "${1:-status}" in
    start)
        AVD=$(get_avd)
        echo "=== 启动 Android Emulator: $AVD ==="
        EXTRA_ARGS="-gpu auto"
        if [ "${2:-}" = "--cold" ]; then
            EXTRA_ARGS="$EXTRA_ARGS -no-snapshot-load"
        fi
        nohup emulator -avd "$AVD" $EXTRA_ARGS \
            > "$LOG_FILE" 2>&1 &
        echo "等待模拟器启动..."
        adb wait-for-device
        while [ "$(adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
            sleep 2
        done
        echo "模拟器已启动。"
        inject_media
        ;;
    stop)
        echo "=== 关闭 Android Emulator ==="
        adb emu kill 2>/dev/null || true
        echo "模拟器已关闭。"
        ;;
    status)
        if adb get-state >/dev/null 2>&1; then
            echo "Android Emulator: 运行中"
            adb shell getprop ro.product.model 2>/dev/null || true
        else
            echo "Android Emulator: 未运行"
        fi
        ;;
    *)
        echo "用法: $0 {start|stop|status}"
        exit 1
        ;;
esac
```

- [ ] **Step 2: 验证脚本语法**

```bash
chmod +x scripts/emulators/android.sh
bash -n scripts/emulators/android.sh
```

Expected: 无输出（语法正确）。

- [ ] **Step 3: Commit**

```bash
git add scripts/emulators/android.sh
git commit -m "feat: add Android Emulator start/stop script with media injection"
```

---

## Task 4: iOS Simulator 启停脚本

**Files:**
- Create: `scripts/emulators/ios.sh`

- [ ] **Step 1: 编写 scripts/emulators/ios.sh**

```bash
#!/usr/bin/env bash
#
# iOS Simulator 启动/停止/状态查询
#
# 用法:
#   scripts/emulators/ios.sh start   — 启动模拟器并注入测试媒体
#   scripts/emulators/ios.sh stop    — 关闭模拟器
#   scripts/emulators/ios.sh status  — 查看模拟器状态
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ASSETS_DIR="$PROJECT_ROOT/test-assets"
LOG_FILE="$PROJECT_ROOT/logs/emulator-ios.log"

mkdir -p "$PROJECT_ROOT/logs"

get_device_udid() {
    local udid
    udid=$(xcrun simctl list devices available -j 2>/dev/null \
        | python3 -c "
import json, sys
data = json.load(sys.stdin)
for runtime, devices in data.get('devices', {}).items():
    if 'iOS' in runtime:
        for d in devices:
            if 'iPhone' in d['name'] and d['isAvailable']:
                print(d['udid'])
                sys.exit(0)
" 2>/dev/null)
    if [ -z "$udid" ]; then
        echo "错误: 未找到可用的 iPhone Simulator。请在 Xcode 中安装 iOS Simulator Runtime。" >&2
        exit 1
    fi
    echo "$udid"
}

get_device_name() {
    local target_udid="$1"
    UDID="$target_udid" python3 -c "
import json, sys, os
target = os.environ['UDID']
data = json.load(sys.stdin)
for runtime, devices in data.get('devices', {}).items():
    for d in devices:
        if d['udid'] == target:
            print(d['name'])
            sys.exit(0)
" < <(xcrun simctl list devices -j 2>/dev/null)
}

inject_media() {
    local udid="$1"
    echo "注入测试媒体到 iOS Simulator..."
    if [ -d "$ASSETS_DIR/photos" ]; then
        for f in "$ASSETS_DIR/photos/"*.{jpg,png}; do
            [ -f "$f" ] && xcrun simctl addmedia "$udid" "$f" 2>/dev/null || true
        done
    fi
    if [ -d "$ASSETS_DIR/videos" ]; then
        for f in "$ASSETS_DIR/videos/"*.mp4; do
            [ -f "$f" ] && xcrun simctl addmedia "$udid" "$f" 2>/dev/null || true
        done
    fi
    echo "测试媒体注入完成。"
}

case "${1:-status}" in
    start)
        UDID=$(get_device_udid)
        DEVICE_NAME=$(get_device_name "$UDID")
        echo "=== 启动 iOS Simulator: $DEVICE_NAME ($UDID) ==="
        xcrun simctl boot "$UDID" 2>/dev/null || true
        open -a Simulator
        echo "等待模拟器启动..."
        sleep 5
        echo "模拟器已启动。"
        inject_media "$UDID"
        ;;
    stop)
        echo "=== 关闭 iOS Simulator ==="
        xcrun simctl shutdown all 2>/dev/null || true
        echo "模拟器已关闭。"
        ;;
    status)
        booted_count=$(xcrun simctl list devices booted 2>/dev/null | grep -c "Booted" || true)
        if [ "$booted_count" -gt 0 ]; then
            echo "iOS Simulator: 运行中"
            xcrun simctl list devices booted 2>/dev/null | grep "Booted"
        else
            echo "iOS Simulator: 未运行"
        fi
        ;;
    *)
        echo "用法: $0 {start|stop|status}"
        exit 1
        ;;
esac
```

- [ ] **Step 2: 验证脚本语法**

```bash
chmod +x scripts/emulators/ios.sh
bash -n scripts/emulators/ios.sh
```

Expected: 无输出（语法正确）。

- [ ] **Step 3: Commit**

```bash
git add scripts/emulators/ios.sh
git commit -m "feat: add iOS Simulator start/stop script with media injection"
```

---

## Task 5: HarmonyOS Emulator 启停脚本 + AGC 云测试引导

**Files:**
- Create: `scripts/emulators/harmony.sh`
- Create: `scripts/emulators/harmony-cloud.sh`

注意：harmony.sh 可独立运行（DevEco Studio 模拟器不依赖 Gradle target）。

- [ ] **Step 1: 编写 scripts/emulators/harmony.sh**

```bash
#!/usr/bin/env bash
#
# HarmonyOS Emulator 启动/停止/状态查询
#
# 用法:
#   scripts/emulators/harmony.sh start   — 等待模拟器连接
#   scripts/emulators/harmony.sh stop    — 提示关闭模拟器
#   scripts/emulators/harmony.sh status  — 查看模拟器状态
#
# 前置条件:
#   - DevEco Studio 已安装并创建了模拟器
#   - hdc 命令可用（位于 HarmonyOS SDK toolchains 目录）
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOG_FILE="$PROJECT_ROOT/logs/emulator-harmony.log"

mkdir -p "$PROJECT_ROOT/logs"

check_hdc() {
    if ! command -v hdc &>/dev/null; then
        echo "错误: hdc 未找到。请将 HarmonyOS SDK toolchains 加入 PATH。" >&2
        echo "例如: export PATH=\$PATH:\$HOME/Library/Huawei/Sdk/hmscore/toolchains" >&2
        exit 1
    fi
}

do_start() {
    check_hdc
    echo "=== 启动 HarmonyOS Emulator ==="
    echo "请在 DevEco Studio 中手动启动模拟器（Tools > Device Manager）。"
    echo "DevEco Studio 模拟器暂不支持命令行直接启动。"
    echo ""
    echo "等待模拟器连接..."
    retry=0
    while [ $retry -lt 30 ]; do
        if hdc list targets 2>/dev/null | grep -qv "Empty"; then
            echo "模拟器已连接:"
            hdc list targets
            return 0
        fi
        sleep 2
        ((retry++))
    done
    echo "超时: 60 秒内未检测到模拟器。请确认 DevEco Studio 模拟器已启动。"
    exit 1
}

do_status() {
    if command -v hdc &>/dev/null; then
        targets=$(hdc list targets 2>/dev/null || echo "")
        if [ -n "$targets" ] && ! echo "$targets" | grep -q "Empty"; then
            echo "HarmonyOS Emulator: 运行中"
            echo "$targets"
        else
            echo "HarmonyOS Emulator: 未运行"
        fi
    else
        echo "HarmonyOS Emulator: hdc 未安装"
    fi
}

case "${1:-status}" in
    start)  do_start ;;
    stop)
        check_hdc
        echo "=== 关闭 HarmonyOS Emulator ==="
        echo "请在 DevEco Studio 中手动关闭模拟器。"
        echo "当前已连接设备:"
        hdc list targets 2>/dev/null || echo "无设备连接"
        ;;
    status) do_status ;;
    *)
        echo "用法: $0 {start|stop|status}"
        exit 1
        ;;
esac
```

- [ ] **Step 2: 编写 scripts/emulators/harmony-cloud.sh**

```bash
#!/usr/bin/env bash
#
# 华为 AGC 云测试/云调试引导
#
# 用法:
#   scripts/emulators/harmony-cloud.sh             — 显示操作指引
#   scripts/emulators/harmony-cloud.sh --open      — 直接打开 AGC 控制台
#
set -euo pipefail

echo "=== CleanPic: AGC 云测试 ==="
echo ""
echo "AGC 云测试操作指引："
echo "  1. 打开浏览器访问: https://developer.huawei.com/consumer/cn/agconnect/cloud-test/"
echo "  2. 登录华为开发者账号"
echo "  3. 上传 HAP 包进行测试"
echo ""
echo "AGC 云调试（远程真机）："
echo "  1. 打开浏览器访问: https://developer.huawei.com/consumer/cn/agconnect/cloud-adjust/"
echo "  2. 选择 HarmonyOS 设备"
echo "  3. 每日免费 300 分钟"
echo ""

if [ "${1:-}" = "--open" ] && command -v open &>/dev/null; then
    open "https://developer.huawei.com/consumer/cn/agconnect/cloud-test/"
fi
```

- [ ] **Step 3: 验证语法并设置权限**

```bash
chmod +x scripts/emulators/harmony.sh scripts/emulators/harmony-cloud.sh
bash -n scripts/emulators/harmony.sh
bash -n scripts/emulators/harmony-cloud.sh
```

Expected: 无输出。

- [ ] **Step 4: Commit**

```bash
git add scripts/emulators/harmony.sh scripts/emulators/harmony-cloud.sh
git commit -m "feat: add HarmonyOS Emulator and AGC cloud testing scripts"
```

---

## Task 6: 改造现有 test.sh 增加日志输出

**Files:**
- Modify: `scripts/test.sh`

- [ ] **Step 1: 修改 test.sh 增加日志输出**

将现有的直接输出改为同时写入 logs/test-common.log：

```bash
#!/usr/bin/env bash
#
# 运行 shared 模块的全平台测试
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$PROJECT_ROOT/logs/test-common.log"

mkdir -p "$PROJECT_ROOT/logs"

echo "=== CleanPic: 运行 shared 模块测试 ==="

cd "$PROJECT_ROOT"
./gradlew :shared:allTests "$@" 2>&1 | tee "$LOG_FILE"

echo "=== 测试完成（日志: $LOG_FILE）==="
```

- [ ] **Step 2: 验证脚本语法**

```bash
bash -n scripts/test.sh
```

Expected: 无输出。

- [ ] **Step 3: Commit**

```bash
git add scripts/test.sh
git commit -m "fix: add log file output to test.sh"
```

---

## Task 7: Android 测试脚本 test-android.sh

**Files:**
- Create: `scripts/test-android.sh`

- [ ] **Step 1: 编写 test-android.sh**

```bash
#!/usr/bin/env bash
#
# Android 平台测试：构建 APK → 部署到模拟器 → 运行 Maestro E2E
#
# 用法:
#   scripts/test-android.sh          — 完整流程（构建+部署+E2E）
#   scripts/test-android.sh build    — 仅构建
#   scripts/test-android.sh deploy   — 仅部署（需已构建）
#   scripts/test-android.sh e2e      — 仅 E2E 测试（需已部署）
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$PROJECT_ROOT/logs/test-android.log"
APK_PATH="$PROJECT_ROOT/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
APP_PACKAGE="com.cleanpic.android"

mkdir -p "$PROJECT_ROOT/logs"

check_emulator() {
    if ! adb get-state >/dev/null 2>&1; then
        echo "错误: Android Emulator 未运行。请先执行: scripts/emulators/android.sh start"
        exit 1
    fi
}

do_build() {
    echo "--- 构建 Android APK (Debug) ---"
    cd "$PROJECT_ROOT"
    ./gradlew :androidApp:assembleDebug 2>&1 | tee -a "$LOG_FILE"
    echo "APK: $APK_PATH"
}

do_deploy() {
    check_emulator
    echo "--- 部署到 Android Emulator ---"
    adb install -r "$APK_PATH" 2>&1 | tee -a "$LOG_FILE"
    echo "部署完成。"
}

do_e2e() {
    check_emulator
    echo "--- 运行 Maestro E2E 测试 ---"
    if ! command -v maestro &>/dev/null; then
        echo "警告: Maestro 未安装，跳过 E2E 测试。"
        echo "安装: curl -Ls 'https://get.maestro.mobile.dev' | bash"
        return 0
    fi
    if [ -d "$PROJECT_ROOT/maestro/flows" ]; then
        maestro test "$PROJECT_ROOT/maestro/flows/" 2>&1 | tee -a "$LOG_FILE"
    else
        echo "提示: maestro/flows/ 目录不存在，跳过 E2E 测试。"
    fi
}

echo "=== CleanPic: Android 平台测试 ===" | tee "$LOG_FILE"
echo "时间: $(date)" | tee -a "$LOG_FILE"

case "${1:-all}" in
    build)  do_build ;;
    deploy) do_deploy ;;
    e2e)    do_e2e ;;
    all)
        do_build
        do_deploy
        do_e2e
        ;;
    *)
        echo "用法: $0 {build|deploy|e2e|all}"
        exit 1
        ;;
esac

echo "=== Android 测试完成（日志: $LOG_FILE）==="
```

- [ ] **Step 2: 验证脚本语法**

```bash
chmod +x scripts/test-android.sh
bash -n scripts/test-android.sh
```

Expected: 无输出。

- [ ] **Step 3: Commit**

```bash
git add scripts/test-android.sh
git commit -m "feat: add Android test script (build + deploy + Maestro E2E)"
```

---

## Task 8: iOS 测试脚本 test-ios.sh

**Files:**
- Create: `scripts/test-ios.sh`

- [ ] **Step 1: 编写 test-ios.sh**

```bash
#!/usr/bin/env bash
#
# iOS 平台测试：构建 Framework → 部署到模拟器 → 运行 Maestro E2E
#
# 用法:
#   scripts/test-ios.sh          — 完整流程（构建+E2E）
#   scripts/test-ios.sh build    — 仅构建 shared framework
#   scripts/test-ios.sh e2e      — 仅 E2E 测试（需已部署）
#
# 注意：iOS 应用的完整构建和部署需要 Xcode workspace 配置完成。
# 当前 build 阶段构建 shared KLib；完整部署需在 Xcode 中操作或
# 待 iOS app target 配置后通过 xcodebuild 命令自动化。
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$PROJECT_ROOT/logs/test-ios.log"

mkdir -p "$PROJECT_ROOT/logs"

check_simulator() {
    local booted
    booted=$(xcrun simctl list devices booted 2>/dev/null | grep -c "Booted" || true)
    if [ "$booted" -eq 0 ]; then
        echo "错误: iOS Simulator 未运行。请先执行: scripts/emulators/ios.sh start"
        exit 1
    fi
}

do_build() {
    echo "--- 构建 iOS shared framework ---"
    cd "$PROJECT_ROOT"
    ./gradlew :shared:iosSimulatorArm64MainKlibrary 2>&1 | tee -a "$LOG_FILE"
    echo "shared framework 构建完成。"
    echo "提示: 请在 Xcode 中打开 iOS 项目并部署到模拟器。"
}

do_e2e() {
    check_simulator
    echo "--- 运行 Maestro E2E 测试 (iOS) ---"
    if ! command -v maestro &>/dev/null; then
        echo "警告: Maestro 未安装，跳过 E2E 测试。"
        echo "安装: curl -Ls 'https://get.maestro.mobile.dev' | bash"
        return 0
    fi
    if [ -d "$PROJECT_ROOT/maestro/flows" ]; then
        maestro test --platform ios "$PROJECT_ROOT/maestro/flows/" 2>&1 | tee -a "$LOG_FILE"
    else
        echo "提示: maestro/flows/ 目录不存在，跳过 E2E 测试。"
    fi
}

echo "=== CleanPic: iOS 平台测试 ===" | tee "$LOG_FILE"
echo "时间: $(date)" | tee -a "$LOG_FILE"

case "${1:-all}" in
    build) do_build ;;
    e2e)   do_e2e ;;
    all)
        do_build
        do_e2e
        ;;
    *)
        echo "用法: $0 {build|e2e|all}"
        exit 1
        ;;
esac

echo "=== iOS 测试完成（日志: $LOG_FILE）==="
```

- [ ] **Step 2: 验证脚本语法**

```bash
chmod +x scripts/test-ios.sh
bash -n scripts/test-ios.sh
```

Expected: 无输出。

- [ ] **Step 3: Commit**

```bash
git add scripts/test-ios.sh
git commit -m "feat: add iOS test script (build + Maestro E2E)"
```

---

## Task 9: HarmonyOS 测试脚本（待启用）

**Files:**
- Create: `scripts/test-harmony.sh`

AGC 云测试引导脚本已在 Task 5 中创建为 `scripts/emulators/harmony-cloud.sh`。

- [ ] **Step 1: 编写 test-harmony.sh**

```bash
#!/usr/bin/env bash
#
# HarmonyOS 平台测试：构建 HAP → 部署到模拟器
#
# [待启用] 需要 ohosArm64 Gradle target 解除注释后才可运行。
# 当前状态：脚本框架已就绪，构建命令待确认。
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$PROJECT_ROOT/logs/test-harmony.log"

mkdir -p "$PROJECT_ROOT/logs"

echo "=== CleanPic: HarmonyOS 平台测试 ===" | tee "$LOG_FILE"
echo "时间: $(date)" | tee -a "$LOG_FILE"

# 检查 ohosArm64 target 是否启用
if ! grep -q "^[[:space:]]*ohosArm64" "$PROJECT_ROOT/shared/build.gradle.kts"; then
    echo "警告: ohosArm64 Gradle target 尚未启用。" | tee -a "$LOG_FILE"
    echo "请在 shared/build.gradle.kts 中取消 ohosArm64 块的注释。" | tee -a "$LOG_FILE"
    echo "此脚本将在 target 启用后可用。" | tee -a "$LOG_FILE"
    echo "" | tee -a "$LOG_FILE"
    echo "如需 AGC 云测试，请运行: scripts/emulators/harmony-cloud.sh" | tee -a "$LOG_FILE"
    exit 0
fi

# 检查 hdc
if ! command -v hdc &>/dev/null; then
    echo "错误: hdc 未找到。请安装 DevEco Studio 并配置 PATH。" | tee -a "$LOG_FILE"
    exit 1
fi

# 构建（具体 task 名称待 target 启用后确认）
echo "--- 构建 HarmonyOS HAP ---"
cd "$PROJECT_ROOT"
# TODO: 确认构建任务名称，可能是 :ohosApp:assembleDebug 或类似
echo "TODO: HarmonyOS 构建任务待确认" | tee -a "$LOG_FILE"

echo "=== HarmonyOS 测试完成（日志: $LOG_FILE）==="
```

- [ ] **Step 2: 验证语法并设置权限**

```bash
chmod +x scripts/test-harmony.sh
bash -n scripts/test-harmony.sh
```

Expected: 无输出。

- [ ] **Step 3: Commit**

```bash
git add scripts/test-harmony.sh
git commit -m "feat: add HarmonyOS test script (pending ohosArm64 target activation)"
```

---

## Task 10: 全平台一键测试脚本

**Files:**
- Create: `scripts/test-all-platforms.sh`

- [ ] **Step 1: 编写 test-all-platforms.sh**

```bash
#!/usr/bin/env bash
#
# 一键运行全平台测试（顺序执行）
#
# 执行顺序：
#   1. 共享层 commonTest (L1/L2)
#   2. Android 平台测试 (L3/L4)
#   3. iOS 平台测试 (L3/L4)
#   4. HarmonyOS 平台测试 (L3/L4，仅在 target 启用时)
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0

run_step() {
    local name="$1"
    local script="$2"
    shift 2

    echo ""
    echo "============================================"
    echo "  $name"
    echo "============================================"

    if "$SCRIPT_DIR/$script" "$@"; then
        echo "  => $name: 通过"
        ((PASS++))
    else
        echo "  => $name: 失败"
        ((FAIL++))
    fi
}

echo "=== CleanPic: 全平台测试 ==="
echo "时间: $(date)"

# 1. 共享层
run_step "共享层 commonTest" test.sh

# 2. Android
run_step "Android 平台" test-android.sh

# 3. iOS
run_step "iOS 平台" test-ios.sh

# 4. HarmonyOS
run_step "HarmonyOS 平台" test-harmony.sh

echo ""
echo "============================================"
echo "  全平台测试汇总: $PASS 通过, $FAIL 失败"
echo "============================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
```

- [ ] **Step 2: 验证脚本语法**

```bash
chmod +x scripts/test-all-platforms.sh
bash -n scripts/test-all-platforms.sh
```

Expected: 无输出。

- [ ] **Step 3: Commit**

```bash
git add scripts/test-all-platforms.sh
git commit -m "feat: add all-platform test orchestration script"
```

---

## Task 11: Maestro E2E 测试流程

**前置条件：** 需要 Android Emulator 或 iOS Simulator 已运行，且 CleanPic 应用已部署。

**兼容性风险：** KuiklyUI 的渲染层可能不暴露标准 accessibility 标签，导致 Maestro 的文本选择器失效。Step 2 是兼容性验证 checkpoint，如果失败则标注此 Task 为 blocked，需评估替代方案（截图对比等）。

**Files:**
- Create: `maestro/config.yaml`
- Create: `maestro/flows/browse-photos.yaml`
- Create: `maestro/flows/delete-confirm.yaml`
- Create: `maestro/flows/switch-theme.yaml`

注意：这些流程需要在 Maestro + KuiklyUI 兼容性验证通过后才能最终确认。当前先按标准 Maestro 语法编写，后续可能需要调整选择器。

- [ ] **Step 1: 创建 Maestro 全局配置**

```yaml
# maestro/config.yaml
appId: com.cleanpic.android
```

- [ ] **Step 2: 兼容性验证 checkpoint**

在 Android Emulator 上运行最小验证流程：

```bash
maestro test maestro/config.yaml <<'EOF'
appId: com.cleanpic.android
---
- launchApp
- waitForAnimationToEnd
- takeScreenshot: maestro-compat-check
EOF
```

Expected: Maestro 能正常启动应用并截图。如果 Maestro 报错无法连接或无法识别 UI 元素，则标注此 Task 为 **blocked**，需评估替代方案。

- [ ] **Step 3: 编写浏览照片流程 browse-photos.yaml**

```yaml
# maestro/flows/browse-photos.yaml
#
# E2E: 首页 → 选择照片 → 浏览照片 → 返回
#
# 注意: UI 选择器待 Maestro + KuiklyUI 兼容性验证后调整
#
appId: com.cleanpic.android
---
- launchApp
- assertVisible: "CleanPic"
# 等待 Splash 完成
- waitForAnimationToEnd
# 在首页选择照片模式
- tapOn: "照片"
# 等待照片加载
- waitForAnimationToEnd
# 验证照片浏览界面出现
- assertVisible: "保留"
- assertVisible: "删除"
# 截图留证
- takeScreenshot: browse-photos-loaded
```

- [ ] **Step 4: 编写删除确认流程 delete-confirm.yaml**

```yaml
# maestro/flows/delete-confirm.yaml
#
# E2E: 浏览 → 标记删除 → 完成一轮 → 确认删除 → 查看结果
#
appId: com.cleanpic.android
---
- launchApp
- waitForAnimationToEnd
- tapOn: "照片"
- waitForAnimationToEnd
# 标记删除当前照片
- tapOn: "删除"
- waitForAnimationToEnd
# 继续浏览直到完成一轮（可能需要多次操作）
- tapOn: "保留"
- waitForAnimationToEnd
- tapOn: "保留"
- waitForAnimationToEnd
- tapOn: "保留"
- waitForAnimationToEnd
- tapOn: "保留"
- waitForAnimationToEnd
# 到达结果页
- assertVisible: "删除"
- takeScreenshot: delete-confirm-result
```

- [ ] **Step 5: 编写切换主题流程 switch-theme.yaml**

```yaml
# maestro/flows/switch-theme.yaml
#
# E2E: 首页 → 设置 → 切换主题 → 返回验证
#
appId: com.cleanpic.android
---
- launchApp
- waitForAnimationToEnd
# 进入设置
- tapOn: "设置"
- waitForAnimationToEnd
# 切换主题
- tapOn: "主题"
- waitForAnimationToEnd
- takeScreenshot: theme-settings
# 选择一个不同的主题
- tapOn: "优雅暗色"
- waitForAnimationToEnd
- takeScreenshot: theme-dark-applied
# 返回首页验证主题生效
- tapOn: "返回"
- waitForAnimationToEnd
- takeScreenshot: theme-home-after-switch
```

- [ ] **Step 6: Commit**

```bash
git add maestro/
git commit -m "feat: add Maestro E2E test flows (browse, delete, theme)"
```

---

## Task 12: 最终验证

- [ ] **Step 1: 验证目录文件数量合规**

```bash
echo "scripts/: $(ls scripts/*.sh | wc -l) 文件"
echo "scripts/emulators/: $(ls scripts/emulators/*.sh | wc -l) 文件"
```

Expected: scripts/ <= 8 个文件，scripts/emulators/ <= 8 个文件。

- [ ] **Step 2: 运行 check-env.sh 做最终环境验证**

```bash
scripts/check-env.sh
```

Expected: JDK、Gradle、Git LFS 应通过。IDE 相关工具按实际安装情况显示。

- [ ] **Step 3: 运行 commonTest 确认现有测试未受影响**

```bash
scripts/test.sh
```

Expected: 28 个测试全部通过，日志写入 logs/test-common.log。

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: finalize multi-platform testing scripts setup"
```
