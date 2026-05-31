/**
 * 回归测试：worker 代理下载用的 APK 资产名，必须与发布脚本实际上传到
 * GitHub Release 的文件名保持一致。
 *
 * 背景：v1.2.7 引入 direct/store 渠道拆分后，发布脚本把 APK 改名为
 * shuashuaya-direct.apk，但 worker 仍写死 shuashuaya.apk，导致代理下载
 * 始终 404 → 手机端报「下载失败检查网络」。该缺陷三个版本未被发现的根因
 * 正是这两处文件名分别硬编码、会悄悄漂移。本测试将二者钉死。
 *
 * 运行：cd worker && npm test
 */
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const WORKER_SRC = path.join(REPO_ROOT, "worker", "src", "index.js");
const RELEASE_SCRIPT = path.join(REPO_ROOT, "scripts", "release-direct.sh");

/** 从 worker/src/index.js 的 ASSET_NAMES 中取出 android 资产名 */
function workerAndroidAssetName() {
  const src = fs.readFileSync(WORKER_SRC, "utf8");
  const block = src.match(/const ASSET_NAMES\s*=\s*\{([\s\S]*?)\}/);
  assert.ok(block, "未能在 index.js 中定位 ASSET_NAMES");
  const m = block[1].match(/android:\s*"([^"]+)"/);
  assert.ok(m, "ASSET_NAMES 中未找到 android 条目");
  return m[1];
}

/** 从 release-direct.sh 中取出实际上传到 Release 的 APK 文件名 */
function releaseUploadedApkName() {
  const sh = fs.readFileSync(RELEASE_SCRIPT, "utf8");
  const m = sh.match(/UPLOAD_APK="([^"]+\.apk)"/);
  assert.ok(m, "release-direct.sh 中未找到 UPLOAD_APK");
  return path.basename(m[1]);
}

test("worker 的 android 资产名与发布脚本上传名一致", () => {
  const workerName = workerAndroidAssetName();
  const uploadedName = releaseUploadedApkName();
  assert.equal(
    workerName,
    uploadedName,
    `worker ASSET_NAMES.android=「${workerName}」 与发布脚本上传名「${uploadedName}」不一致，` +
      `代理下载会 404。请同步二者。`
  );
});

test("android 资产名固定为 shuashuaya-direct.apk（direct 渠道）", () => {
  assert.equal(workerAndroidAssetName(), "shuashuaya-direct.apk");
});
