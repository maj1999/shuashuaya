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
    # 触发 MediaStore 扫描，把注入的文件 finalize 成 is_pending=0，App 才看得见。
    # 坑：本机模拟器（API34）没有 `media` 服务，旧写法 `cmd media scan volume ...`
    # 会静默报 "Can't find service: media"，留下 owner=shell、is_pending=1 的不可见
    # 条目，导致删除类 flow 一进 viewer 即空、卡 ~90s 超时（E2E 大面积超时失败的头号原因）。
    # 必须用 content call scan_volume，它会把条目索引成 is_pending=0（可见）。
    adb shell content call --uri content://media --method scan_volume --arg external_primary 2>/dev/null || true
    echo "测试媒体注入完成。"
}

case "${1:-status}" in
    start)
        AVD=$(get_avd)
        echo "=== 启动 Android Emulator: $AVD ==="
        # -gpu host：直接用宿主机 GPU（macOS 上的 Apple GPU）硬件渲染，比默认/auto
        # 退化的 SwiftShader 软渲染快 ~15%，UI 动画类 flow 提速最明显。
        # 需本机有可用 GPU（macOS 桌面环境满足；headless/CI 无 GPU 时改回 -gpu swiftshader_indirect）。
        EXTRA_ARGS="-gpu host"
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
        # 关闭系统动画：Maestro 官方建议，提升 UI 稳定性、截图更干净。
        # 注：实测对本项目 flow 墙钟提速不明显——Maestro 的 waitForAnimationToEnd 是自己
        # 截屏比对判稳定、不依赖系统 animation scale，所以这里主要为稳定性而非提速。
        adb shell settings put global window_animation_scale 0 2>/dev/null || true
        adb shell settings put global transition_animation_scale 0 2>/dev/null || true
        adb shell settings put global animator_duration_scale 0 2>/dev/null || true
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
