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
