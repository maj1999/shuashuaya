#!/usr/bin/env bash
#
# 发布 direct flavor 新版本：构建 Release APK → 创建 GitHub Release → 更新 Worker
#
# 用法:
#   ./scripts/release-direct.sh 1.2.0 "修复了xxx，新增了xxx"
#
# 参数:
#   $1 — 版本号（如 1.2.0）
#   $2 — 更新说明（如 "修复了照片加载问题"）
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 加载项目级环境变量
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

# 检查参数
if [ $# -lt 2 ]; then
    echo "用法: $0 <版本号> <更新说明>"
    echo "示例: $0 1.2.0 \"修复了照片加载问题，新增了视频支持\""
    exit 1
fi

VERSION="$1"
CHANGELOG="$2"
TAG="v${VERSION}"
APK_PATH="$PROJECT_ROOT/androidApp/build/outputs/apk/direct/release/刷刷鸭-direct.apk"
WORKER_DIR="$PROJECT_ROOT/worker"
WORKER_CONFIG="$WORKER_DIR/src/index.js"
BUILD_CONFIG="$PROJECT_ROOT/buildSrc/src/main/kotlin/CleanPicBuildConfig.kt"

# 网络操作重试包装：最多 3 次、间隔 3 秒，应对 GitHub/Cloudflare 偶发的 SSL/EOF 抖动
retry() {
    local n=1 max=3
    until "$@"; do
        if [ $n -ge $max ]; then
            echo "  ❌ 命令失败（已重试 $max 次）: $*"
            return 1
        fi
        echo "  ⚠️  网络操作失败，第 $n/$max 次重试中..."
        n=$((n + 1))
        sleep 3
    done
}

echo "══════════════════════════════════════"
echo "  刷刷鸭 v${VERSION} 发布流程"
echo "══════════════════════════════════════"
echo ""

# Step 1: 检查工具
echo "【1/6】检查工具..."
command -v gh >/dev/null 2>&1 || { echo "❌ 需要安装 gh (GitHub CLI): brew install gh"; exit 1; }
command -v npx >/dev/null 2>&1 || { echo "❌ 需要安装 Node.js"; exit 1; }
echo "  ✅ 工具检查通过"

# Step 2: 检查工作区干净
echo ""
echo "【2/6】检查 Git 状态..."
if [ -n "$(git -C "$PROJECT_ROOT" status --porcelain)" ]; then
    echo "❌ 工作区有未提交的更改，请先提交或暂存"
    git -C "$PROJECT_ROOT" status --short
    exit 1
fi
echo "  ✅ 工作区干净"

# Step 3: 更新版本号
echo ""
echo "【3/6】更新版本号..."

# 获取当前 VERSION_CODE 并 +1
CURRENT_CODE=$(grep 'VERSION_CODE' "$BUILD_CONFIG" | grep -o '[0-9]*')
NEW_CODE=$((CURRENT_CODE + 1))

# 更新 CleanPicBuildConfig.kt
sed -i '' "s/const val VERSION_NAME = \".*\"/const val VERSION_NAME = \"${VERSION}\"/" "$BUILD_CONFIG"
sed -i '' "s/const val VERSION_CODE = [0-9]*/const val VERSION_CODE = ${NEW_CODE}/" "$BUILD_CONFIG"

# 更新 AppInfo.kt
sed -i '' "s/const val VERSION = \".*\"/const val VERSION = \"${VERSION}\"/" \
    "$PROJECT_ROOT/shared/src/commonMain/kotlin/com/cleanpic/AppInfo.kt"

echo "  ✅ VERSION_NAME=$VERSION, VERSION_CODE=$NEW_CODE"

# Step 4: 构建 Release APK
echo ""
echo "【4/6】构建 Release APK..."
cd "$PROJECT_ROOT"
./gradlew :androidApp:assembleDirectRelease --quiet
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK 未生成: $APK_PATH"
    exit 1
fi
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "  ✅ APK 构建成功 ($APK_SIZE)"

# Step 5: 提交版本号变更 + 打 Tag + 创建 GitHub Release
echo ""
echo "【5/6】创建 GitHub Release..."
cd "$PROJECT_ROOT"
git add "$BUILD_CONFIG" "$PROJECT_ROOT/shared/src/commonMain/kotlin/com/cleanpic/AppInfo.kt"
git commit -m "chore: bump version to ${VERSION}"
git tag "$TAG"
retry git push origin main
retry git push origin "$TAG"

# GitHub 不支持中文资产名，复制为 ASCII 文件名上传
UPLOAD_APK="/tmp/shuashuaya-direct.apk"
cp "$APK_PATH" "$UPLOAD_APK"

retry gh release create "$TAG" "$UPLOAD_APK" \
    --title "v${VERSION}" \
    --notes "$CHANGELOG"
echo "  ✅ GitHub Release 创建成功"

# Step 6: 更新 Worker 版本信息并部署
echo ""
echo "【6/6】更新 Cloudflare Worker..."

# 更新 worker/src/index.js 中的版本号和 changelog（JS 对象 key 无引号）
# changelog 经环境变量传入 perl 按字面量替换，避免内容含 / | & 等字符破坏替换命令
sed -i '' "s/version: \"[^\"]*\"/version: \"${VERSION}\"/g" "$WORKER_CONFIG"
NEW_CHANGELOG="$CHANGELOG" perl -i -pe 's/(changelog: ")[^"]*(")/$1 . $ENV{NEW_CHANGELOG} . $2/ge' "$WORKER_CONFIG"
sed -i '' "s/versionCode: [0-9]*/versionCode: ${NEW_CODE}/" "$WORKER_CONFIG"

cd "$WORKER_DIR"
if [ -z "${CLOUDFLARE_API_TOKEN:-}" ]; then
    echo "  ⚠️  未设置 CLOUDFLARE_API_TOKEN，跳过自动部署"
    echo "  请手动运行: cd worker && CLOUDFLARE_API_TOKEN=你的token npx wrangler deploy"
else
    retry npx wrangler deploy
    echo "  ✅ Worker 部署成功"
fi

# 提交 Worker 变更
cd "$PROJECT_ROOT"
git add "$WORKER_CONFIG"
git commit -m "chore: update worker version to ${VERSION}"
retry git push origin main

echo ""
echo "══════════════════════════════════════"
echo "  ✅ v${VERSION} 发布完成！"
echo ""
echo "  APK: $APK_PATH"
echo "  Release: https://github.com/maj1999/shuashuaya/releases/tag/${TAG}"
echo "══════════════════════════════════════"
