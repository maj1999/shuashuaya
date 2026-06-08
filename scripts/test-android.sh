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
    # ⚠️ Maestro 2.4.0 路径展开有两个坑，必须用 find 显式枚举文件来规避：
    #    1) `test <单目录>` 不递归子目录 → 直接传 direct/ 会漏跑 video/ 下 7 个 flow；
    #    2) `test <父目录> <子目录>`（曾用的双路径写法）会让父目录转入递归模式，
    #       于是 video flow 既被 direct/ 递归到、又被 direct/video/ 显式带到，重复跑一遍。
    #    传「显式文件列表」则每个 flow 恰好跑一次，确定性最强。store 同理，见 e2e-store。
    if [ -d "$PROJECT_ROOT/maestro/flows/direct" ]; then
        local flows=()
        while IFS= read -r f; do flows+=("$f"); done \
            < <(find "$PROJECT_ROOT/maestro/flows/direct" -name '*.yaml' | sort)
        echo "全量 ${#flows[@]} 个 flow" | tee -a "$LOG_FILE"
        maestro test "${flows[@]}" 2>&1 | tee -a "$LOG_FILE"
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
    # 全量 = store/ 顶层(16) + store/video/(5)，共 21 个 flow。
    # 同 do_e2e：用 find 显式枚举文件，规避 Maestro 单目录不递归 / 父子双路径重复跑两个坑。
    if [ -d "$PROJECT_ROOT/maestro/flows/store" ]; then
        local flows=()
        while IFS= read -r f; do flows+=("$f"); done \
            < <(find "$PROJECT_ROOT/maestro/flows/store" -name '*.yaml' | sort)
        echo "全量 ${#flows[@]} 个 flow" | tee -a "$LOG_FILE"
        maestro test "${flows[@]}" 2>&1 | tee -a "$LOG_FILE"
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
