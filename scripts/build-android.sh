#!/usr/bin/env bash
#
# 构建 Android App (Debug)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== CleanPic: 构建 Android App (Debug) ==="

cd "$PROJECT_ROOT"
./gradlew :androidApp:assembleDebug "$@"

echo "=== 构建完成 ==="
echo "APK 路径: $PROJECT_ROOT/androidApp/build/outputs/apk/debug/"
