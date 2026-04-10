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
    version: "1.2.1",
    versionCode: 15,
    forceUpdate: false,
    minVersion: "1.0.0",
    changelog: "修复全屏模式下视频切换不更新的问题",
    downloadUrl: "" // 将在运行时生成
  },
  ios: {
    version: "1.2.1",
    forceUpdate: false,
    minVersion: "1.0.0",
    changelog: "修复全屏模式下视频切换不更新的问题",
    downloadUrl: ""
  },
  harmonyos: {
    version: "1.2.1",
    forceUpdate: false,
    minVersion: "1.0.0",
    changelog: "修复全屏模式下视频切换不更新的问题",
    downloadUrl: ""
  }
};

// GitHub Release 资源文件名映射
const ASSET_NAMES = {
  android: "刷刷鸭.apk",
  harmonyos: "刷刷鸭.hap"
};

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
      // 为 Android 和 HarmonyOS 生成代理下载链接
      for (const platform of ["android", "harmonyos"]) {
        if (config[platform]) {
          config[platform].downloadUrl =
            `${workerUrl}/download/${platform}/v${config[platform].version}`;
        }
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

      // 从 GitHub Release 获取资源下载 URL
      const releaseUrl = `https://api.github.com/repos/${repo}/releases/tags/${tag}`;
      const ghResponse = await fetch(releaseUrl, {
        headers: {
          "User-Agent": "CleanPic-Update-Worker",
          "Accept": "application/vnd.github.v3+json"
        }
      });

      if (!ghResponse.ok) {
        return new Response(JSON.stringify({ error: "未找到该版本" }), {
          status: 404,
          headers: { "Content-Type": "application/json", ...corsHeaders }
        });
      }

      const release = await ghResponse.json();
      const asset = release.assets?.find(a => a.name === assetName);

      if (!asset) {
        return new Response(JSON.stringify({ error: "未找到安装包" }), {
          status: 404,
          headers: { "Content-Type": "application/json", ...corsHeaders }
        });
      }

      // 代理下载（流式转发）
      const assetResponse = await fetch(asset.browser_download_url, {
        headers: { "User-Agent": "CleanPic-Update-Worker" },
        redirect: "follow"
      });

      return new Response(assetResponse.body, {
        headers: {
          "Content-Type": "application/octet-stream",
          "Content-Disposition": `attachment; filename="${assetName}"`,
          "Content-Length": asset.size.toString(),
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
