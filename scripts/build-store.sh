#!/usr/bin/env bash
#
# 构建 store flavor APK，产出到 dist/ 待手动上架应用商店。
#
# 用法:
#   scripts/build-store.sh <版本号>
#
# 示例:
#   scripts/build-store.sh 1.2.6
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ $# -lt 1 ]; then
    echo "用法: $0 <版本号>"
    echo "示例: $0 1.2.6"
    exit 1
fi

VERSION="$1"
APK_PATH="$PROJECT_ROOT/androidApp/build/outputs/apk/store/release/刷刷鸭-store.apk"
DIST_DIR="$PROJECT_ROOT/dist"
DIST_APK="$DIST_DIR/刷刷鸭-store-v${VERSION}.apk"

echo "═══════════════════════════════════════"
echo "  构建 store flavor v${VERSION}"
echo "═══════════════════════════════════════"

cd "$PROJECT_ROOT"

# 构建
echo ""
echo "【1/3】构建 store flavor APK..."
./gradlew :androidApp:assembleStoreRelease --quiet
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK 未生成: $APK_PATH"
    exit 1
fi
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "  ✅ APK 构建成功 ($APK_SIZE)"

# 字节码扫描
echo ""
echo "【2/3】字节码扫描验证..."
"$SCRIPT_DIR/verify-store-apk.sh" "$APK_PATH"

# 复制到 dist/
echo ""
echo "【3/3】产出到 dist/..."
mkdir -p "$DIST_DIR"
cp "$APK_PATH" "$DIST_APK"
echo "  ✅ APK 已复制到: $DIST_APK"

echo ""
echo "═══════════════════════════════════════"
echo "  ✅ 完成。可上架到各应用商店。"
echo "═══════════════════════════════════════"
echo ""
echo "  APK: $DIST_APK"
echo "  注意: 商店上架需各渠道独立签名与元数据，请手动处理。"
