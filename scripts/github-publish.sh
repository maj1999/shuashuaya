#!/usr/bin/env bash
#
# 发布到 GitHub Releases（境外备用下载源）：创建/复用 Release → 上传 APK（幂等）→ 匿名下载+sha256 门禁
#   → 把「已验证可匿名下载」的下载 URL 打到 stdout（供 release-direct.sh 捕获，写入 version.json 的 downloadUrlFallback）
#
# 用法:
#   scripts/github-publish.sh <version> <apk_path> ["<changelog>"]
#
# 依赖：gh CLI（已登录，见 `gh auth status`）。仓库默认取 .env 的 GITHUB_DIST_REPO，缺省 maj1999/shuashuaya（须为 public）。
#
# 设计要点（对齐 gitee-publish.sh 的门禁哲学）：
#   - tag/release 已存在则复用；附件已存在则跳过（绝不重传，避免换链）
#   - 必须「匿名 curl -L 下载 + sha256 比对」通过，本脚本才算成功并回显 URL
#   - 所有进度信息打到 stderr，只有最终下载 URL 打到 stdout
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -f "$PROJECT_ROOT/.env" ]; then set -a; source "$PROJECT_ROOT/.env"; set +a; fi

if [ $# -lt 2 ]; then
    echo "用法: $0 <version> <apk_path> [\"<changelog>\"]" >&2
    exit 1
fi
VERSION="$1"; APK="$2"; CHANGELOG="${3:-}"
GH_REPO="${GITHUB_DIST_REPO:-maj1999/shuashuaya}"

command -v gh >/dev/null 2>&1 || { echo "❌ 需要安装 gh (GitHub CLI): brew install gh" >&2; exit 1; }
[ -f "$APK" ] || { echo "❌ APK 不存在: $APK" >&2; exit 1; }

TAG="v${VERSION}"
ASSET="shuashuaya-direct.apk"   # GitHub 不支持中文资产名，统一 ASCII
DLURL="https://github.com/${GH_REPO}/releases/download/${TAG}/${ASSET}"
SHA="$(shasum -a 256 "$APK" | cut -d' ' -f1)"

echo "【GitHub 1/3】创建/复用 Release ${TAG}（repo: ${GH_REPO}）..." >&2
if gh release view "$TAG" --repo "$GH_REPO" >/dev/null 2>&1; then
    echo "  release ${TAG} 已存在，复用" >&2
else
    gh release create "$TAG" --repo "$GH_REPO" --title "v${VERSION}" --notes "${CHANGELOG}" >&2
    echo "  release ${TAG} 已创建" >&2
fi

echo "【GitHub 2/3】上传附件（已存在则跳过，绝不重传）..." >&2
HAS="$(gh release view "$TAG" --repo "$GH_REPO" --json assets \
    -q "[.assets[].name] | index(\"${ASSET}\")" 2>/dev/null || echo "null")"
if [ "$HAS" != "null" ] && [ -n "$HAS" ]; then
    echo "  附件已存在，跳过上传" >&2
else
    UPLOAD_APK="/tmp/${ASSET}"
    cp "$APK" "$UPLOAD_APK"
    gh release upload "$TAG" "$UPLOAD_APK" --repo "$GH_REPO" >&2
    echo "  uploaded: ${ASSET}" >&2
fi

echo "【GitHub 3/3】门禁：匿名下载校验（无 token，比对 sha256）..." >&2
curl -sL --max-time 300 -o /tmp/github-verify.apk "$DLURL"
GOT="$(shasum -a 256 /tmp/github-verify.apk | cut -d' ' -f1)"
if [ "$GOT" != "$SHA" ]; then
    echo "  ❌ 匿名下载校验失败：期望 ${SHA:0:12}... 实得 ${GOT:0:12}...（仓库可能非 public / 上传不完整）" >&2
    echo "  ⚠️ 备用源未通过门禁，不会写入 version.json。" >&2
    exit 1
fi
echo "  ✅ 匿名下载 sha256 一致，备用源门禁通过：${DLURL}" >&2

# 仅把已验证的下载 URL 输出到 stdout，供调用方捕获
echo "$DLURL"
