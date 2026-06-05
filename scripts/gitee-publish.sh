#!/usr/bin/env bash
#
# 发布到 Gitee 国内分发渠道：创建 Release → 上传 APK → 门禁式匿名下载校验 → 更新 version.json
#
# 用法:
#   scripts/gitee-publish.sh <version> <apk_path> <versionCode> "<changelog>"
#
# 依赖 .env：GITEE_OWNER / GITEE_DIST_REPO / GITEE_TOKEN
# 设计要点（方案 §12 #3 门禁式顺序 + 幂等）：
#   - tag 已存在则复用 Release；附件已存在则跳过（attach_files 重传会换 id，绝不重传）
#   - 上传后必须「匿名 curl -L 下载 + sha256 比对」通过，才更新 version.json（避免死链/坏链对外）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -f "$PROJECT_ROOT/.env" ]; then set -a; source "$PROJECT_ROOT/.env"; set +a; fi

if [ $# -lt 4 ]; then
    echo "用法: $0 <version> <apk_path> <versionCode> \"<changelog>\""
    exit 1
fi
VERSION="$1"; APK="$2"; VCODE="$3"; CHANGELOG="$4"
: "${GITEE_OWNER:?需在 .env 配置 GITEE_OWNER}"
: "${GITEE_DIST_REPO:?需在 .env 配置 GITEE_DIST_REPO}"
: "${GITEE_TOKEN:?需在 .env 配置 GITEE_TOKEN}"

TAG="v${VERSION}"
ASSET="shuashuaya-direct.apk"
API="https://gitee.com/api/v5"
REPO="${GITEE_OWNER}/${GITEE_DIST_REPO}"
DLURL="https://gitee.com/${REPO}/releases/download/${TAG}/${ASSET}"

[ -f "$APK" ] || { echo "❌ APK 不存在: $APK"; exit 1; }
SHA="$(shasum -a 256 "$APK" | cut -d' ' -f1)"
SIZE="$(stat -f%z "$APK")"

echo "【Gitee 1/4】创建/复用 Release ${TAG} ..."
RID="$(curl -s --max-time 30 "${API}/repos/${REPO}/releases/tags/${TAG}?access_token=${GITEE_TOKEN}" \
    | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('id') or '')" 2>/dev/null || true)"
if [ -z "$RID" ]; then
    RID="$(curl -s --max-time 30 -X POST "${API}/repos/${REPO}/releases" \
        -d "access_token=${GITEE_TOKEN}" -d "tag_name=${TAG}" --data-urlencode "name=${TAG}" \
        --data-urlencode "body=${CHANGELOG}" -d "target_commitish=master" -d "prerelease=false" \
        | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))")"
fi
[ -n "$RID" ] || { echo "❌ Gitee Release 创建失败"; exit 1; }
echo "  release id: $RID"

echo "【Gitee 2/4】上传附件（已存在则跳过，绝不重传）..."
HAS="$(curl -s --max-time 30 "${API}/repos/${REPO}/releases/${RID}/attach_files?access_token=${GITEE_TOKEN}" \
    | python3 -c "import sys,json;d=json.load(sys.stdin);print('yes' if isinstance(d,list) and any(a.get('name')=='${ASSET}' for a in d) else 'no')" 2>/dev/null || echo no)"
if [ "$HAS" = "yes" ]; then
    echo "  附件已存在，跳过上传"
else
    cp "$APK" "/tmp/${ASSET}"
    curl -s --max-time 300 -X POST "${API}/repos/${REPO}/releases/${RID}/attach_files" \
        -F "access_token=${GITEE_TOKEN}" -F "file=@/tmp/${ASSET}" \
        | python3 -c "import sys,json;d=json.load(sys.stdin);print('  uploaded:', d.get('name') or d.get('message'))"
fi

echo "【Gitee 3/4】门禁：匿名下载校验（无 token/referer，比对 sha256）..."
curl -sL --max-time 300 -o /tmp/gitee-verify.apk "$DLURL"
GOT="$(shasum -a 256 /tmp/gitee-verify.apk | cut -d' ' -f1)"
if [ "$GOT" != "$SHA" ]; then
    echo "  ❌ 匿名下载校验失败：期望 ${SHA:0:12}... 实得 ${GOT:0:12}...（可能防盗链/上传不完整）"
    echo "  ⚠️ 未更新 version.json，国内渠道保持上一可用版本。请检查后重试。"
    exit 1
fi
echo "  ✅ 匿名下载 sha256 一致，门禁通过"

echo "【Gitee 4/4】更新 update/version.json ..."
CL_JSON="$CHANGELOG"
cat > /tmp/version.json <<EOF
{
  "android": { "version": "${VERSION}", "versionCode": ${VCODE}, "forceUpdate": false, "minVersion": "1.0.0", "changelog": "${CL_JSON}", "downloadUrl": "${DLURL}", "sha256": "${SHA}", "size": ${SIZE} },
  "ios": { "version": "${VERSION}", "forceUpdate": false, "minVersion": "1.0.0", "changelog": "${CL_JSON}", "downloadUrl": "" },
  "harmonyos": { "version": "${VERSION}", "versionCode": ${VCODE}, "forceUpdate": false, "minVersion": "1.0.0", "changelog": "${CL_JSON}", "downloadUrl": "", "sha256": "", "size": 0 }
}
EOF
python3 -m json.tool /tmp/version.json >/dev/null || { echo "❌ 生成的 version.json 非法"; exit 1; }
BLOB_SHA="$(curl -s --max-time 30 "${API}/repos/${REPO}/contents/update/version.json?access_token=${GITEE_TOKEN}&ref=master" \
    | python3 -c "import sys,json;print(json.load(sys.stdin).get('sha',''))" 2>/dev/null || true)"
B64="$(base64 < /tmp/version.json | tr -d '\n')"
if [ -n "$BLOB_SHA" ]; then
    curl -s --max-time 30 -X PUT "${API}/repos/${REPO}/contents/update/version.json" \
        -d "access_token=${GITEE_TOKEN}" --data-urlencode "content=${B64}" -d "sha=${BLOB_SHA}" \
        -d "message=chore: publish ${TAG} (versionCode ${VCODE})" -d "branch=master" \
        | python3 -c "import sys,json;d=json.load(sys.stdin);print('  version.json 更新 commit:', (d.get('commit') or {}).get('sha','')[:10] or d.get('message'))"
else
    curl -s --max-time 30 -X POST "${API}/repos/${REPO}/contents/update/version.json" \
        -d "access_token=${GITEE_TOKEN}" --data-urlencode "content=${B64}" \
        -d "message=chore: init version.json ${TAG}" -d "branch=master" \
        | python3 -c "import sys,json;d=json.load(sys.stdin);print('  version.json 创建 commit:', (d.get('commit') or {}).get('sha','')[:10] or d.get('message'))"
fi

echo "✅ Gitee 发布完成：${DLURL}"
