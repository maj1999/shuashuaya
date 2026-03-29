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

echo "=== Android 测试完成（日志: ${LOG_FILE}）==="
