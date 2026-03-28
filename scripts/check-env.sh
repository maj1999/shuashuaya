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
