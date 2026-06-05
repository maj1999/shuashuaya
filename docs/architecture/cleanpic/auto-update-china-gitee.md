# CleanPic — 国内自动升级分发方案（Gitee 双端点 + 存量迁移）

|文档状态| 已评审·待落地 | 2026-06-06 |

> 已经 subagent 对抗性评审：核心 P0 桥接逻辑经代码核实成立；评审挖出 3 致命缺口 + 5 需修正项，已并入 §7 改动清单、§9 测试方案与 §12 Must-Fix。

> 父文档: [auto-update.md](auto-update.md)、[auto-update-distribution.md](auto-update-distribution.md)
> 关联 US：US-CP-13/14/15（检查 / 提示 / 下载安装更新）。

## 1. 背景与目标

**问题**：当前 direct 渠道的自动升级，检查更新与 APK 下载都指向 Cloudflare `*.workers.dev` 域名，该域名在中国大陆被 GFW 限制——小请求偶可穿透，14MB APK 持续传输必被 reset。**国内用户必须挂代理才能升级下载**。

**目标**：
1. 国内用户**无需代理**即可自动检查 + 下载 + 安装升级。
2. **存量已安装 app（当前指向 workers.dev）能通过一次自动升级切换到国内渠道**，不依赖用户手动重装。
3. 免费、可快速落地（不依赖 ICP 备案的数周周期）。
4. 海外用户体验不退化。

## 2. 现状链路与根因（已查证）

```
App ──①检查──► workers.dev/api/version  ──返回──► downloadUrl = workers.dev/download/android/vX
App ──②下载──► workers.dev/download/...  ──Worker fetch──► github.com release ─302─► githubusercontent(Azure)
```

- 检查与下载**同一个 workers.dev 域名**（`worker/src/index.js:65` 用请求 origin 动态生成 downloadUrl）。
- 链路上每一跳（workers.dev / Cloudflare IP / github.com / githubusercontent）对 GFW 都不友好。
- 实测：Worker 代理逻辑本身正常（返回 200 + 14MB 真 APK），**非代码 bug，是域名可达性问题**。

## 3. 迁移可行性基石（关键事实）

| 事实 | 来源 | 含义 |
|------|------|------|
| `downloadUrl` 每次实时从 `/api/version` 下发 | `UpdateChecker.kt:22` → `UpdateInstaller` 用 `updateInfo.downloadUrl` | 改服务端即可改变所有存量 app 的下载地址 |
| `UpdateResultCache` 仅会话级内存、无持久化 | `UpdateResultCache.kt` | 旧 downloadUrl 不会被持久化钉死 |
| 存量 app 只认 baked-in 的 `UPDATE_API_URL=workers.dev` | `CleanPicBuildConfig.kt:55` | 存量 app 的**检查端点无法改**，只能改 workers.dev 的**返回内容** |

→ **存量 app 唯一可操控的杠杆 = workers.dev 的 `/api/version` 返回值**。

## 4. 总体设计：Gitee 双端点

选 **Gitee（码云）** 作为国内端点：国内服务器、免费、个人实名即时、无需备案。源码不公开——**新建一个公开的纯分发仓库**（仅含 `version.json` + APK release，无源码）。

```
源码仓库（GitHub，私有，不动）
        │ 发布时推产物
        ▼
分发仓库 <owner>/shuashuaya-dist（Gitee，公开）
   ├─ update/version.json          ← 新版 app 检查更新读这里（Gitee raw，国内秒开、海外亦可达）
   └─ Release v1.x 附件 *.apk       ← 实际下载源（Gitee 国内可达）
```

**两类客户端的链路**：

```
新版 app：  ①检查 Gitee raw/version.json（主）→ workers.dev（海外兜底，可选）
            ②下载 Gitee release 附件
存量 app：  ①检查 workers.dev/api/version（只能走这）  ← 小请求，国内偶可穿透
            ②下载 ← Worker 返回的 downloadUrl 改成 Gitee 直链 → 国内可达 ✅
```

## 5. 存量 app 迁移策略（核心）

**桥接原理**：存量 app 改不了检查端点，但 downloadUrl 是实时下发的。因此——

> **把 Worker `/api/version` 返回的 `android.downloadUrl` 从 workers.dev 代理地址改成 Gitee Release 直链。**

存量 app 下次检查（小请求，国内多数可穿透）→ 拿到 Gitee 直链 → `DownloadManager` 从 Gitee 下载（国内可达）→ 装上**新版**。新版 app 的检查端点已是 Gitee raw → 从此彻底脱离 workers.dev。**一次自动升级完成迁移。**

**两阶段滚动：**

| 阶段 | 动作 | 谁被迁移 | 是否需要发新版 |
|------|------|---------|--------------|
| **P0（服务端，立即生效）** | 改 Worker：`android.downloadUrl` → Gitee 直链；把当前/下个版本 APK 传到 Gitee | 所有能完成检查的**存量 app** | 否（仅改 Worker + 传 APK） |
| **P1（新客户端）** | 新版 app 检查改读 Gitee `version.json`（主），workers.dev 兜底；发布脚本同步双端点 | 完成过一次升级的用户 → 永久脱离 workers.dev | 是 |

P0 让存量用户"下载"立刻走国内；P1 让升级后的用户连"检查"也走国内。

## 6. 残留风险与兜底

| 风险 | 说明 | 兜底 |
|------|------|------|
| 小检查请求也被硬墙（DNS 污染/SNI RST）的用户 | 这部分存量用户连 `/api/version` 都到不了，**任何服务端改动都救不了**（代码无法触达连不上服务器的设备） | 提供 Gitee 公开下载页/二维码，由用户在可用网络手动装一次；或引导走应用商店（store flavor） |
| Gitee Release 附件防盗链/审核/限速 | Gitee 对公开仓库附件偶有审核与限速 | 实现期验证匿名直链可下；仓库提前实名过审 |
| Gitee 直链 URL 格式不确定 | Gitee 匿名资产 URL 可能是 `attach_files/.../download` 形态 | **发布脚本以 Gitee 上传 API 返回的 `browser_download_url` 为准**写入 version.json 与 Worker，不手写格式 |
| 下载链接是否会过期 | Gitee 链接分两层：**稳定页面级链接**（`browser_download_url`，只要 Release/附件不删、tag 不改名即永久有效）+ **302 后的签名存储链接**（会过期、每次请求由 Gitee 重新签发，对客户端透明） | 只把稳定链接写进 version.json/Worker，绝不持久化签名 URL；每个版本一版一链、发布时取当时 API 返回值；纪律：**不手动删/重传已发布附件**（attach_files 形态的 id 会变） |
| 海外用户访问 Gitee 较慢 | Gitee 海外可达但慢 | 新版保留 workers.dev 作为海外兜底；或按检查失败自动 fallback |

## 7. 改动清单

| 位置 | 改动 | 阶段 |
|------|------|------|
| `worker/src/index.js` | `VERSION_CONFIG.android.downloadUrl` 改为 Gitee 直链（不再用 origin 拼 workers.dev 代理）；harmonyos 同理；保留 `/download` 代理给海外/历史 | P0 |
| `scripts/release-direct.sh` | 新增步骤：①调 Gitee API 创建 Release 并上传 APK，取回 `browser_download_url`；②用该 URL 更新 Gitee 仓库 `update/version.json` 并 push；③用该 URL 更新 Worker 的 downloadUrl 后再 deploy | P0/P1 |
| 分发仓库 `update/version.json` | 新增：`{android:{version,versionCode,forceUpdate,minVersion,changelog,downloadUrl,**sha256,size**}, ...}`，schema 与 `VersionResponse` 对齐（新增完整性字段见 F1/M4） | P0 |
| `update/.../UpdateChecker.kt` 客户端 | **装 `HttpTimeout`**（connect/request/socket 5-10s）——否则 P1 fallback 永不触发、协程泄漏（F2） | P1 |
| `update/.../UpdateModels.kt` + `AndroidUpdateInstaller.kt` | `UpdateInfo` 加 `sha256/size`；下载成功分支（`AndroidUpdateInstaller.kt:73`）**校验文件 sha256+大小+ZIP 魔数**，不符判 FAILED 并回退 Worker 代理重试一次（F1） | P1 |
| `UpdateChecker.determineStatus` 比较逻辑 | 改为**优先用 versionCode** 比较（现仅比 version 字符串，versionCode 是死字段），version 串仅作展示（M1） | P1 |
| `worker/src/index.js` harmonyos | downloadUrl **同样改 Gitee 直链**（评审提醒：现循环仍给 harmonyos 拼 workers.dev 代理，不改则鸿蒙渠道仍卡墙内） | P0 |
| `buildSrc/.../CleanPicBuildConfig.kt` | 新增常量 `UPDATE_API_URL_CN`（Gitee raw version.json 的 URL）；保留 `UPDATE_API_URL`（海外兜底） | P1 |
| `update/.../UpdateChecker.kt` | 支持主端点（CN/Gitee raw）+ 兜底端点（海外/worker）：主失败/超时再试兜底；兼容"直接 URL 指向 version.json"（现在固定拼 `/api/version`，需让路径可配） | P1 |
| `androidApp/src/direct/.../UpdateWiring.kt` | 用新的双端点构造 `UpdateChecker` | P1 |
| `.env` | 新增 `GITEE_TOKEN`（私人令牌，勾 `projects`）；`GITEE_OWNER` / `GITEE_DIST_REPO` | P0 |
| `docs/testing/scenarios/ep6-auto-update.md` | 新增国内端点 + 迁移相关场景 | P1 |
| `docs/architecture/cleanpic/overview.md`、`domain-model.md` | 登记新文档与术语（国内端点/分发仓库/迁移桥） | P1 |

## 8. 新发布流程（落地后）

```
scripts/release-direct.sh <version> "<changelog>"：
  1. 既有：打快照 tag、bump 版本、构建 direct Release APK
  2. 既有：GitHub Release + 上传 APK（海外/worker 代理源）
  3. 新增：Gitee 创建 Release + 上传同一 APK → 得 browser_download_url(CN)
  4. 新增：更新 Gitee 仓库 update/version.json（含 CN downloadUrl）并 push
  5. 改造：用 CN downloadUrl 更新 worker/src/index.js → wrangler deploy
  6. 既有：提交版本号/worker 变更
```

## 9. 测试方案

- **单元（UpdateCheckerTest）**：主端点成功直接用；**主端点超时（非仅异常）时 fallback 到兜底端点**；两者都失败返回 UP_TO_DATE；version.json schema 反序列化（含 CN downloadUrl + sha256/size）；versionCode 优先比较（同 version 名仅 bump code 也能判新版）。
- **完整性校验单元**：下载内容 sha256/大小不符 → 判 FAILED；非 ZIP/HTML 伪装包 → 判 FAILED（F1）。
- **Worker（worker/test）**：`/api/version` 的 android **与 harmonyos** downloadUrl 等于配置的 Gitee 直链（不再是 workers.dev）。
- **手动/E2E**：① 模拟存量 app（旧 UPDATE_API_URL）检查 → 得 Gitee 下载链 → 下载成功；② 新版 app 断网 workers.dev 仍能从 Gitee 检查+下载；③ 真机国内网络（或限制 workers.dev 大流量）验证全程不挂代理可升级；④ **设置页手动"检查更新"失败时给出明确提示 + Gitee 下载页引导（不静默吞错，M3）**。
- **回归**：海外路径（workers.dev/GitHub）仍可用。

## 10. 回滚

- P0 回滚：`git revert` worker 改动并 `wrangler deploy`，downloadUrl 立刻回到 workers.dev 代理。Worker 改动与客户端解耦，回滚零客户端影响。
- P1 回滚：新版客户端保留 workers.dev 兜底，即使 Gitee 故障也能走海外路径，不会变砖。

## 11. 实现期必须先验证的事项（写进任务）

1. Gitee 个人账号已实名；分发仓库公开且过审。
2. Gitee Release 附件**匿名**可直链下载（拿真实 `browser_download_url` 实测 `curl -L` 得到 APK）。
3. `DownloadManager` 能跟随 Gitee 附件的 302 到签名存储域名。
4. Gitee raw（`gitee.com/<o>/<r>/raw/<branch>/update/version.json`）匿名可读、`Content-Type` 不破坏 ktor JSON 解析。
5. Gitee OpenAPI 创建 Release + 上传附件的脚本化路径（token 权限范围）。

## 12. 评审 Must-Fix（落地前必堵，按序）

来自 subagent 对抗性评审，均已对照代码核实：

| # | 问题 | 依据 | 修复 |
|---|------|------|------|
| 1 | **HttpClient 无超时 → P1 fallback 永不触发、协程泄漏**。GFW 常见"连上但 hang"，主端点挂起则兜底永不被调 | `UpdateChecker.kt:89-95` 无 `HttpTimeout`；全模块无超时（已实测确认） | 先装 `HttpTimeout`（connect/request/socket 5-10s），fallback 基于"超时/异常"显式分支，不复用"任何异常→UP_TO_DATE"的吞错 |
| 2 | **下载无完整性校验 → Gitee 防盗链/风控返回 200+HTML，存成 .apk，安装时才"解析失败"** | `AndroidUpdateInstaller.kt:49,73` 仅凭 `STATUS_SUCCESSFUL` 判成功，无 Content-Type/大小/哈希校验 | version.json 加 `sha256+size`，下载后强校验 + ZIP 魔数，不符判 FAILED 并回退 Worker 代理重试一次 |
| 3 | **发布跨 GitHub+Gitee+Worker 三系统无原子性/幂等 → 可产生指向不存在附件的死链** | `release-direct.sh` `set -euo pipefail` 顺序无回滚；`gh release create` 对已存在 tag 非幂等 | 门禁式固定顺序：Gitee 上传成功并 `curl -L` 实测可下 → 才写 Worker downloadUrl；每步幂等（tag/附件存在即跳过，绝不重传 attach_files）；失败打印半状态+修复指令 |
| 4 | **versionCode 三处漂移 + 仅比 version 字符串** | `UpdateChecker.kt:30,43` 不读 `versionCode`（死字段）；真值源将变 Gitee/Worker/APK 三处 | 脚本单一真值源写三处；比较优先用 versionCode |
| 5 | **检查端点路径硬编码 `/api/version`，与 Gitee raw 静态文件不兼容** | `UpdateChecker.kt:22` `get("$apiUrl/api/version")` | 端点改为完整 URL 可配（主=raw 全 URL，兜底=worker+/api/version） |
| 6 | **检查失败全程静默 → 存量用户不知有新版、也不知失败，永远不会去扫兜底二维码** | `UpdateChecker.kt:24-26` + `UpdateWiring.kt:65 runCatching{}` 双层吞错 | 手动"检查更新"入口对失败给明确提示 + Gitee 下载页引导；**上线前国内真机实测 `/api/version` 穿透率**，据此评估 P0 实际覆盖 |
| 7 | **GITEE_TOKEN 权限粗 + 泄露面** | `projects` 是仓库写权限；`release-direct.sh:18-22` `set -a` 全环境导入 | `.env` 已确认 gitignore；token 用最小可用权限；注意别被子进程/日志泄露 |

**评审确认成立（无需改）**：downloadUrl 实时下发且无任何持久化钉死点（P0 桥接前提扎实）、存量检查端点不可改只能改 Worker 返回值、store flavor 完全解耦不受影响、Worker 回滚零客户端影响、Gitee 全程 https→https 无跨协议重定向坑。
