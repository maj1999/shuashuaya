#!/usr/bin/env bash
#
# 发布 direct flavor 新版本：构建 Release APK → 提交打 Tag → GitHub Releases(境外备用源) → Gitee(国内主源 + 写 version.json)
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

# Step 2.5: 发布前快照 —— 给当前 git 记录打 tag release-<版本号>，作为本次发布的回滚点
PRE_TAG="release-${VERSION}"
echo ""
echo "【发布前快照】打 tag: $PRE_TAG ..."
if git -C "$PROJECT_ROOT" rev-parse -q --verify "refs/tags/${PRE_TAG}" >/dev/null; then
    echo "  ⚠️  tag $PRE_TAG 已存在，跳过创建"
else
    git -C "$PROJECT_ROOT" tag "$PRE_TAG"
    echo "  ✅ 已创建 ${PRE_TAG}（指向 $(git -C "$PROJECT_ROOT" rev-parse --short HEAD)）"
fi
retry git -C "$PROJECT_ROOT" push origin "$PRE_TAG" \
    && echo "  ✅ $PRE_TAG 已推送到远程" \
    || echo "  ⚠️  $PRE_TAG 推送失败，但本地 tag 已存在（回滚点已保留）"

# Step 3: 更新版本号
echo ""
echo "【3/6】更新版本号..."

# 获取当前 VERSION_CODE 并 +1
CURRENT_CODE=$(grep 'VERSION_CODE' "$BUILD_CONFIG" | grep -o '[0-9]*')
NEW_CODE=$((CURRENT_CODE + 1))

# 更新 CleanPicBuildConfig.kt
sed -i '' "s/const val VERSION_NAME = \".*\"/const val VERSION_NAME = \"${VERSION}\"/" "$BUILD_CONFIG"
sed -i '' "s/const val VERSION_CODE = [0-9]*/const val VERSION_CODE = ${NEW_CODE}/" "$BUILD_CONFIG"

# 更新 AppInfo.kt（VERSION 名 + VERSION_CODE，供客户端 versionCode 比较，需与 BUILD_CONFIG 一致）
APPINFO="$PROJECT_ROOT/shared/src/commonMain/kotlin/com/cleanpic/AppInfo.kt"
sed -i '' "s/const val VERSION = \".*\"/const val VERSION = \"${VERSION}\"/" "$APPINFO"
sed -i '' "s/const val VERSION_CODE = [0-9]*/const val VERSION_CODE = ${NEW_CODE}/" "$APPINFO"

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

# Step 4.5: 提交版本号变更 + 打 Tag + 推送（GitHub Release 需要 tag 先就位）
echo ""
echo "【4.5/6】提交版本变更并打 Tag ${TAG} ..."
cd "$PROJECT_ROOT"
git add "$BUILD_CONFIG" "$PROJECT_ROOT/shared/src/commonMain/kotlin/com/cleanpic/AppInfo.kt"
git commit -m "chore: bump version to ${VERSION}"
git tag "$TAG"
retry git push origin main
retry git push origin "$TAG"
echo "  ✅ 已提交并推送 ${TAG}"

# Step 5: 发布到 GitHub Releases（境外备用下载源，门禁式）。best-effort：
#   失败不阻断国内发布（Gitee 才是主源），仅令 version.json 的 downloadUrlFallback 留空。
echo ""
echo "【5/6】发布境外备用源到 GitHub Releases ..."
FALLBACK_URL=""
if FALLBACK_URL="$("$SCRIPT_DIR/github-publish.sh" "$VERSION" "$APK_PATH" "$CHANGELOG")"; then
    echo "  ✅ 备用源就绪：$FALLBACK_URL"
else
    FALLBACK_URL=""
    echo "  ⚠️ GitHub 备用源发布/门禁失败，本次仅发 Gitee（境外用户体验不变）。请稍后排查后重发。"
fi

# Step 6: 发布到 Gitee 国内分发渠道（门禁式：上传 + 匿名下载校验通过才更新 version.json）。
#   把已门禁校验过的 GitHub URL 作为 downloadUrlFallback 一并写入，供客户端境外下载失败时自动回退。
echo ""
echo "【6/6】发布国内主源到 Gitee ..."
DLURL_FALLBACK="$FALLBACK_URL" "$SCRIPT_DIR/gitee-publish.sh" "$VERSION" "$APK_PATH" "$NEW_CODE" "$CHANGELOG"

# 更新检测只走 Gitee（update/version.json），下载主源 Gitee + 备用源 GitHub，不再部署 Cloudflare Worker。
# 客户端端点见 UpdateWiring.kt（仅 UPDATE_API_URL_CN）；下载回退逻辑见 AndroidUpdateInstaller.kt。

echo ""
echo "══════════════════════════════════════"
echo "  ✅ v${VERSION} 发布完成！"
echo ""
echo "  APK: $APK_PATH"
echo "  国内主源 (Gitee): update/version.json downloadUrl"
echo "  境外备用源 (GitHub): ${FALLBACK_URL:-（本次未发布 / 门禁未通过）}"
echo "  GitHub Release: https://github.com/maj1999/shuashuaya/releases/tag/${TAG}"
echo "══════════════════════════════════════"
