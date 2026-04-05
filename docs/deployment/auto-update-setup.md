# 自动更新功能 — 部署与配置指南

本文档说明如何从零配置自动更新功能所需的全部外部服务和项目设置。

## 前置条件

| 工具 | 用途 | 安装方式 |
|------|------|----------|
| Node.js (v18+) | 运行 Cloudflare Workers CLI | https://nodejs.org/ |
| GitHub CLI (`gh`) | 创建 Release、上传 APK | `brew install gh` 后执行 `gh auth login` |
| Cloudflare 账号 | 部署版本检查 API（免费） | https://dash.cloudflare.com/sign-up |

## 一、Cloudflare Workers 部署（首次）

### 1.1 创建 API Token

1. 登录 https://dash.cloudflare.com/profile/api-tokens
2. 点击 **Create Token**
3. 选择 **"Edit Cloudflare Workers"** 模板，点 **Use template**
4. Account Resources 选择你的账号
5. Zone Resources 选 **All zones**
6. 点 **Continue to summary** → **Create Token**
7. **立即复制 Token**（只显示一次）

### 1.2 配置项目环境变量

在项目根目录创建 `.env` 文件（已在 `.gitignore` 中，不会提交）：

```bash
# .env
CLOUDFLARE_API_TOKEN=你的token
```

### 1.3 修改 Worker 配置

编辑 `worker/wrangler.toml`，替换为你自己的值：

```toml
name = "cleanpic-update"
main = "src/index.js"
compatibility_date = "2024-01-01"
account_id = "你的Account ID"           # ← 在 Cloudflare 控制台首页右下角可见

[vars]
GITHUB_REPO = "你的GitHub用户名/仓库名"  # ← 如 "maj1999/shuashuaya"
```

> **Account ID 获取方式**：登录 Cloudflare → 左侧 Workers & Pages → 右下角 Account Details → Account ID

### 1.4 部署 Worker

```bash
cd worker
npm install
source ../.env
CLOUDFLARE_API_TOKEN=$CLOUDFLARE_API_TOKEN npx wrangler deploy
```

部署成功后会显示 Worker 地址，类似：
```
https://cleanpic-update.你的子域名.workers.dev
```

### 1.5 更新 App 中的 API 地址

编辑 `buildSrc/src/main/kotlin/CleanPicBuildConfig.kt`：

```kotlin
object AppConfig {
    // ...
    const val UPDATE_API_URL = "https://cleanpic-update.你的子域名.workers.dev"  // ← 改为你的 Worker 地址
    const val ENABLE_UPDATE_CHECK = true
}
```

### 1.6 验证部署

在浏览器或终端访问：

```bash
curl https://你的Worker地址/api/version
```

应返回 JSON 格式的版本信息。

---

## 二、发布新版本

配置完成后，每次发布只需一条命令：

```bash
./scripts/release.sh 版本号 "更新说明"
```

例如：
```bash
./scripts/release.sh 1.3.0 "新增视频清理功能，修复照片加载问题"
```

脚本会自动完成以下 6 步：

| 步骤 | 做什么 |
|------|--------|
| 1 | 检查 `gh`、`npx` 是否已安装 |
| 2 | 确认 Git 工作区干净 |
| 3 | 更新 `VERSION_NAME`、`VERSION_CODE`（自动 +1）、`AppInfo.VERSION` |
| 4 | 构建 Release APK |
| 5 | 提交 → 打 Tag → 推送 → 创建 GitHub Release 并上传 APK |
| 6 | 更新 Worker 中的版本号和更新说明 → 部署 Worker |

---

## 三、配置项速查表

### 必须配置（首次部署）

| 配置项 | 文件 | 说明 |
|--------|------|------|
| `CLOUDFLARE_API_TOKEN` | `.env` | Cloudflare API Token，用于部署 Worker |
| `account_id` | `worker/wrangler.toml` | Cloudflare Account ID |
| `GITHUB_REPO` | `worker/wrangler.toml` | GitHub 仓库地址，格式 `用户名/仓库名` |
| `UPDATE_API_URL` | `buildSrc/.../CleanPicBuildConfig.kt` | Worker 部署后的完整 URL |

### 可选配置

| 配置项 | 文件 | 默认值 | 说明 |
|--------|------|--------|------|
| `ENABLE_UPDATE_CHECK` | `buildSrc/.../CleanPicBuildConfig.kt` | `true` | 设为 `false` 可完全禁用更新功能（适用于自编译用户） |
| `forceUpdate` | `worker/src/index.js` | `false` | 设为 `true` 强制用户更新到该版本 |
| `minVersion` | `worker/src/index.js` | `"1.0.0"` | 低于此版本的用户将被强制更新 |
| `autoCheckUpdate` | App 设置页 | 开启 | 用户可在设置页关闭自动检查更新 |

### Worker 版本信息字段说明

`worker/src/index.js` 中的 `VERSION_CONFIG`：

```javascript
{
  android: {
    version: "1.2.0",       // 最新版本号（与 AppInfo.VERSION 比较）
    versionCode: 13,         // Android 版本码
    forceUpdate: false,      // true = 强制更新弹窗（不可跳过）
    minVersion: "1.0.0",     // 低于此版本也触发强制更新
    changelog: "更新说明",    // 弹窗中显示的更新内容
    downloadUrl: ""          // 留空，运行时自动生成代理下载链接
  }
}
```

---

## 四、Fork 项目用户指南

如果你 Fork 了本项目并希望使用自动更新功能：

1. 注册 Cloudflare 账号（免费）
2. 按上述"首次部署"步骤配置你自己的 Worker
3. 修改 `wrangler.toml` 中的 `account_id` 和 `GITHUB_REPO` 为你自己的
4. 修改 `CleanPicBuildConfig.kt` 中的 `UPDATE_API_URL` 为你的 Worker 地址

如果不需要自动更新功能：
- 将 `ENABLE_UPDATE_CHECK` 设为 `false` 即可，App 中不会出现任何更新相关 UI 和网络请求

---

## 五、故障排查

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `Authentication failed (code: 9106)` | API Token 无效或权限不足 | 重新创建 Token，使用 "Edit Cloudflare Workers" 模板 |
| `No active routes` | Workers 子域名未注册 | 访问 Cloudflare 控制台 → Workers & Pages 注册子域名 |
| App 启动无更新弹窗 | 当前版本 >= Worker 中的版本 | 确认 `AppInfo.VERSION` < Worker 中的 `version` |
| `Failed to connect` | 设备无法访问 Worker | 检查网络连接，确认 Worker URL 正确 |
| release.sh 报 `gh: command not found` | 未安装 GitHub CLI | `brew install gh && gh auth login` |
| release.sh 报 `CLOUDFLARE_API_TOKEN 未设置` | `.env` 文件缺失或未配置 | 在项目根目录创建 `.env` 并填入 Token |

---

## 六、架构简图

```
用户手机                    Cloudflare Workers              GitHub
┌──────────┐    HTTPS     ┌──────────────────┐   HTTPS   ┌──────────┐
│ 刷刷鸭    │ ──────────→ │ /api/version     │           │ Releases │
│ App      │ ←────────── │ (返回版本信息)    │           │          │
│          │              │                  │           │          │
│ 点击更新  │ ──────────→ │ /download/...    │ ────────→ │ APK 文件 │
│          │ ←────────── │ (代理流式下载)    │ ←──────── │          │
└──────────┘              └──────────────────┘           └──────────┘
```

所有请求仅包含平台标识，不传输任何用户数据。
