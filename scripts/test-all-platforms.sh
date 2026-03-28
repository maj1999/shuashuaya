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

run_step "共享层 commonTest" test.sh
run_step "Android 平台" test-android.sh
run_step "iOS 平台" test-ios.sh
run_step "HarmonyOS 平台" test-harmony.sh

echo ""
echo "============================================"
echo "  全平台测试汇总: $PASS 通过, $FAIL 失败"
echo "============================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
