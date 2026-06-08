#!/usr/bin/env bash
#
# Android 平台测试：构建 APK → 部署到模拟器 → 运行 Maestro E2E
#
# 用法:
#   scripts/test-android.sh          — 完整流程（构建+部署+E2E）
#   scripts/test-android.sh build    — 仅构建
#   scripts/test-android.sh deploy   — 仅部署（需已构建）
#   scripts/test-android.sh e2e        — 仅 E2E 测试（direct 全量，需已部署）
#   scripts/test-android.sh e2e-store  — 仅跑 store 渠道 flows（需已装 store 包）
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

_maestro_ok() {
    if ! command -v maestro &>/dev/null; then
        echo "警告: Maestro 未安装，跳过 E2E 测试。"
        echo "安装: curl -Ls 'https://get.maestro.mobile.dev' | bash"
        return 1
    fi
}

do_e2e() {
    check_emulator
    echo "--- 运行 Maestro E2E 测试 (direct，全量基准) ---"
    _maestro_ok || return 0
    # 全量 = direct/ 顶层(25) + direct/video/(7)，共 32 个 flow。
    # ⚠️ Maestro 的 `test <dir>` 不递归子目录，必须显式带上 video/ 路径，否则
    #    视频类 7 个 flow 会被静默漏跑（曾导致"全量"只跑 25 个的覆盖洞）。
    # store/ 的 13 个与 direct 同名、仅断言文案差异，应对 store flavor 包单独跑，见 e2e-store。
    if [ -d "$PROJECT_ROOT/maestro/flows/direct" ]; then
        maestro test \
            "$PROJECT_ROOT/maestro/flows/direct/" \
            "$PROJECT_ROOT/maestro/flows/direct/video/" \
            2>&1 | tee -a "$LOG_FILE"
    else
        echo "提示: maestro/flows/direct/ 目录不存在，跳过 E2E 测试。"
    fi
}

do_e2e_store() {
    check_emulator
    echo "--- 运行 Maestro E2E 测试 (store 渠道，按需) ---"
    echo "    注意: store flows 验证 store flavor 包的渠道差异（无自动更新入口等），"
    echo "    请先用 scripts/build-store.sh 构建并安装 store 包，再跑本命令。"
    _maestro_ok || return 0
    if [ -d "$PROJECT_ROOT/maestro/flows/store" ]; then
        maestro test "$PROJECT_ROOT/maestro/flows/store/" 2>&1 | tee -a "$LOG_FILE"
    else
        echo "提示: maestro/flows/store/ 目录不存在，跳过。"
    fi
}

echo "=== CleanPic: Android 平台测试 ===" | tee "$LOG_FILE"
echo "时间: $(date)" | tee -a "$LOG_FILE"

case "${1:-all}" in
    build)     do_build ;;
    deploy)    do_deploy ;;
    e2e)       do_e2e ;;
    e2e-store) do_e2e_store ;;
    all)
        do_build
        do_deploy
        do_e2e
        ;;
    *)
        echo "用法: $0 {build|deploy|e2e|e2e-store|all}"
        exit 1
        ;;
esac

echo "=== Android 测试完成（日志: ${LOG_FILE}）==="
