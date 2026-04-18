#!/usr/bin/env bash
#
# 验证 store flavor APK 不含升级相关代码、URL、权限。
#
# 用法:
#   scripts/verify-store-apk.sh <apk-path>
#
# 退出码:
#   0 = 通过（APK 干净）
#   1 = 失败（APK 含升级痕迹）
#
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "用法: $0 <apk-path>"
    exit 1
fi

APK="$1"
if [ ! -f "$APK" ]; then
    echo "❌ APK 不存在: $APK"
    exit 1
fi

# 必须存在 aapt（Android SDK build-tools）
AAPT="$(command -v aapt || true)"
if [ -z "$AAPT" ]; then
    SEARCH_ROOTS=(
        "${ANDROID_HOME:-}"
        "${ANDROID_SDK_ROOT:-}"
        "$HOME/Library/Android/sdk"
        "/opt/homebrew/share/android-commandlinetools"
        "/usr/local/share/android-commandlinetools"
    )
    for ROOT in "${SEARCH_ROOTS[@]}"; do
        if [ -n "$ROOT" ] && [ -d "$ROOT/build-tools" ]; then
            BUILD_TOOLS_DIR=$(ls -d "$ROOT"/build-tools/*/ 2>/dev/null | sort -V | tail -1 || true)
            if [ -n "$BUILD_TOOLS_DIR" ] && [ -x "${BUILD_TOOLS_DIR}aapt" ]; then
                AAPT="${BUILD_TOOLS_DIR}aapt"
                break
            fi
        fi
    done
    if [ -z "$AAPT" ] || [ ! -x "$AAPT" ]; then
        echo "❌ 找不到 aapt，请检查 ANDROID_HOME 或安装 Android SDK build-tools"
        exit 1
    fi
fi

echo "═══════════════════════════════════════"
echo "  扫描 APK: $APK"
echo "  使用 aapt: $AAPT"
echo "═══════════════════════════════════════"

FAIL=0

# B-UPD-01: 字节码字符串扫描
echo ""
echo "【1】字节码字符串扫描..."
TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT
unzip -p "$APK" 'classes*.dex' > "$TMPDIR/dexstrings.bin" 2>/dev/null || true

check_string() {
    local pattern="$1"
    local desc="$2"
    local count
    count=$(strings "$TMPDIR/dexstrings.bin" | grep -c "$pattern" || true)
    if [ "$count" -gt 0 ]; then
        echo "  ❌ 发现 $count 处 \"$pattern\" ($desc)"
        FAIL=1
    else
        echo "  ✅ 无 \"$pattern\" ($desc)"
    fi
}

check_string "UpdateChecker" "升级检查类名"
check_string "UpdateInstaller" "升级安装类名"
check_string "UpdateDialog" "升级弹窗类名"
check_string "workers.dev" "升级 API 域名"
check_string "releases/download" "GitHub Release 下载 URL"
check_string "Lcom/cleanpic/update/" "升级模块类引用 (DEX 类引用前缀，避开 auto_check_update SharedPrefs key 误命中)"

# B-UPD-02: 权限扫描
echo ""
echo "【2】权限扫描..."
PERMS=$("$AAPT" dump permissions "$APK" 2>/dev/null || true)

check_perm() {
    local perm="$1"
    if echo "$PERMS" | grep -q "$perm"; then
        echo "  ❌ 发现权限 $perm"
        FAIL=1
    else
        echo "  ✅ 无权限 $perm"
    fi
}

check_perm "REQUEST_INSTALL_PACKAGES"

# B-UPD-03: Manifest 扫描
echo ""
echo "【3】Manifest 资源扫描..."
MANIFEST_DUMP=$("$AAPT" dump xmltree "$APK" AndroidManifest.xml 2>/dev/null || true)

if echo "$MANIFEST_DUMP" | grep -q "FileProvider"; then
    echo "  ❌ Manifest 中发现 FileProvider 声明"
    FAIL=1
else
    echo "  ✅ Manifest 无 FileProvider"
fi

if echo "$MANIFEST_DUMP" | grep -q "fileprovider"; then
    echo "  ❌ Manifest 中发现 fileprovider authority"
    FAIL=1
else
    echo "  ✅ Manifest 无 fileprovider authority"
fi

# 总结
echo ""
echo "═══════════════════════════════════════"
if [ "$FAIL" -eq 0 ]; then
    echo "  ✅ APK 扫描通过：无升级相关代码与配置"
    exit 0
else
    echo "  ❌ APK 扫描失败：含有升级相关痕迹（详见上方）"
    exit 1
fi
