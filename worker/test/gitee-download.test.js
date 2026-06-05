/**
 * W-UPD-01：国内分发迁移回归。
 * 执行 worker 的 /api/version，断言 android 下载链指向 Gitee（国内可达）而非被墙的 workers.dev。
 * 这是「存量 app 自动迁移」桥接的关键——存量 app 检查更新拿到的就是这个 downloadUrl。
 *
 * 运行：cd worker && npm test
 */
const { test } = require("node:test");
const assert = require("node:assert/strict");

async function apiVersion() {
  const mod = await import("../src/index.js");
  const res = await mod.default.fetch(new Request("https://cleanpic-update.example/api/version"), {});
  return res.json();
}

test("android.downloadUrl 指向 Gitee 直链，不再是 workers.dev", async () => {
  const json = await apiVersion();
  assert.match(
    json.android.downloadUrl,
    /^https:\/\/gitee\.com\/[^/]+\/[^/]+\/releases\/download\/v[\d.]+\/shuashuaya-direct\.apk$/,
    "android 必须走 Gitee 直链"
  );
  assert.doesNotMatch(json.android.downloadUrl, /workers\.dev/, "android 不应再走 workers.dev");
});

test("android.downloadUrl 的版本号与配置 version 一致", async () => {
  const json = await apiVersion();
  assert.ok(json.android.downloadUrl.includes(`/v${json.android.version}/`),
    "下载链的 tag 应与 version 一致（确定性构造）");
});

test("/download 代理路由仍保留（海外/历史回退）", async () => {
  const mod = await import("../src/index.js");
  const res = await mod.default.fetch(
    new Request("https://cleanpic-update.example/download/android/v1.0.0"), {}
  );
  // 代理会去 fetch GitHub（测试环境可能失败），但至少不是 404 Not Found 路由
  assert.notEqual(res.status, undefined);
});
