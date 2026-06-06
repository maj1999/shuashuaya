/**
 * CleanPic 版本检查 + GitHub Release 下载代理
 *
 * 部署: cd worker && npx wrangler deploy
 *
 * API:
 *   GET /api/version        — 返回各平台最新版本信息
 *   GET /download/:platform/:tag — 代理 GitHub Release 资源下载
 */

// 版本信息配置 — 发布新版本时更新此处
const VERSION_CONFIG = {
  android: {
    version: "1.9.1",
    versionCode: 37,
    forceUpdate: false,
    minVersion: "1.0.0",
    changelog: "修复全屏预览操作按钮遮挡画面；修复全屏播放时静音图标与实际声音不一致（进全屏不再有底层声音泄漏）。",
    downloadUrl: "" // 运行时按 version 确定性构造 Gitee 直链（见 GITEE_DIST）
  },
  ios: {
    version: "1.9.1",
    forceUpdate: false,
    minVersion: "1.0.0",
    changelog: "修复全屏预览操作按钮遮挡画面；修复全屏播放时静音图标与实际声音不一致（进全屏不再有底层声音泄漏）。",
    downloadUrl: ""
  },
  harmonyos: {
    version: "1.9.1",
    forceUpdate: false,
    minVersion: "1.0.0",
    changelog: "修复全屏预览操作按钮遮挡画面；修复全屏播放时静音图标与实际声音不一致（进全屏不再有底层声音泄漏）。",
    downloadUrl: ""
  }
};

// GitHub Release 资源文件名映射
const ASSET_NAMES = {
  android: "shuashuaya-direct.apk",
  harmonyos: "shuashuaya.hap"
};

// 国内分发：Gitee 公开分发仓库。downloadUrl 按 version 确定性构造（稳定 URL 格式已验证可匿名下载）。
const GITEE_DIST = {
  owner: "ma_mark",
  repo: "shuashuaya-dist",
  asset: "shuashuaya-direct.apk"
};
function giteeDownloadUrl(version) {
  return `https://gitee.com/${GITEE_DIST.owner}/${GITEE_DIST.repo}/releases/download/v${version}/${GITEE_DIST.asset}`;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const workerUrl = url.origin;

    // CORS headers
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type"
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    // GET /api/version
    if (url.pathname === "/api/version") {
      const config = structuredClone(VERSION_CONFIG);
      // android：国内走 Gitee 直链（国内可达），按 version 确定性构造 → 存量 app 检查更新即从 Gitee 下载完成迁移。
      if (config.android) {
        config.android.downloadUrl = giteeDownloadUrl(config.android.version);
      }
      // harmonyos：暂无国内产物，仍回退 workers.dev 代理。
      if (config.harmonyos && !config.harmonyos.downloadUrl) {
        config.harmonyos.downloadUrl = `${workerUrl}/download/harmonyos/v${config.harmonyos.version}`;
      }
      return new Response(JSON.stringify(config), {
        headers: {
          "Content-Type": "application/json",
          "Cache-Control": "max-age=300",
          ...corsHeaders
        }
      });
    }

    // GET /download/:platform/:tag
    const downloadMatch = url.pathname.match(/^\/download\/(android|ios|harmonyos)\/(v[\d.]+)$/);
    if (downloadMatch) {
      const [, platform, tag] = downloadMatch;
      const repo = env.GITHUB_REPO || "maj1999/shuashuaya";
      const assetName = ASSET_NAMES[platform];

      if (!assetName) {
        return new Response(JSON.stringify({ error: "此平台不支持直接下载" }), {
          status: 400,
          headers: { "Content-Type": "application/json", ...corsHeaders }
        });
      }

      // 直接构造 GitHub Release 下载 URL（无需调 API，避免速率限制）
      const downloadUrl = `https://github.com/${repo}/releases/download/${tag}/${assetName}`;

      // 代理下载（流式转发）
      const assetResponse = await fetch(downloadUrl, {
        headers: { "User-Agent": "CleanPic-Update-Worker" },
        redirect: "follow"
      });

      if (!assetResponse.ok) {
        return new Response(JSON.stringify({ error: "未找到安装包" }), {
          status: 404,
          headers: { "Content-Type": "application/json", ...corsHeaders }
        });
      }

      return new Response(assetResponse.body, {
        headers: {
          "Content-Type": "application/octet-stream",
          "Content-Disposition": `attachment; filename="${assetName}"`,
          "Content-Length": assetResponse.headers.get("Content-Length") || "",
          ...corsHeaders
        }
      });
    }

    // 404
    return new Response(JSON.stringify({ error: "Not Found" }), {
      status: 404,
      headers: { "Content-Type": "application/json", ...corsHeaders }
    });
  }
};
