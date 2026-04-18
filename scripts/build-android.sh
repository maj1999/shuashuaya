#!/usr/bin/env bash
#
# 构建 Android Debug APK
#
# 用法:
#   scripts/build-android.sh                # 构建 direct flavor（默认）
#   scripts/build-android.sh direct
#   scripts/build-android.sh store
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

FLAVOR="${1:-direct}"
case "$FLAVOR" in
    direct|store) ;;
    *) echo "❌ 未知 flavor: $FLAVOR （支持: direct, store）"; exit 1 ;;
esac

# capitalize first letter for gradle task
FLAVOR_CAP="$(echo "$FLAVOR" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')"
TASK="assemble${FLAVOR_CAP}Debug"

cd "$PROJECT_ROOT"
echo "=== CleanPic: 构建 ${FLAVOR} flavor debug APK ==="
./gradlew ":androidApp:${TASK}"

APK="$PROJECT_ROOT/androidApp/build/outputs/apk/${FLAVOR}/debug/刷刷鸭-${FLAVOR}.apk"
if [ -f "$APK" ]; then
    SIZE=$(du -h "$APK" | cut -f1)
    echo "=== 构建完成 ==="
    echo "✅ APK: $APK ($SIZE)"
else
    echo "❌ APK 未找到: $APK"
    exit 1
fi
