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
