#!/usr/bin/env bash
#
# iOS 平台测试：构建 Framework → 运行 Maestro E2E
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
