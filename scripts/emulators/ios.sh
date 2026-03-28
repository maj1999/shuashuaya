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
