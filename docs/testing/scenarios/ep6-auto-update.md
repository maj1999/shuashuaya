# EP6: 自动升级 — 测试场景

|文档状态| 更新 | 2026-04-12 |

## 测试约束

自动升级功能依赖远程 API，E2E 测试需要以下前置条件之一：
- 本地 mock server（推荐，用于 CI）
- 真实 Cloudflare Workers 测试环境

Maestro 测试使用本地 mock server（`scripts/mock-update-server.sh`），通过 BuildConfig 注入测试 API 地址。

## 分发渠道与测试目录

升级功能引入 `direct` / `store` 两个 productFlavor，测试按渠道分目录组织：

```
maestro/flows/
├── direct/          ── direct flavor 专属测试（包含应用内升级 UI）
│   ├── browse-photos.yaml          # 通用功能（任何 flavor 都应通过）
│   ├── ep6-optional-update.yaml    # E-UPD-01
│   ├── ep6-force-update.yaml       # E-UPD-02
│   ├── ...                         # E-UPD-01 ~ 11
│   └── us-cp-17-direct-smoke.yaml  # E-UPD-15（direct flavor 回归）
└── store/           ── store flavor 专属测试
    ├── browse-photos.yaml          # 通用功能在 store flavor 下也必须通过
    ├── us-cp-17-settings-no-update.yaml    # E-UPD-12
    ├── us-cp-17-home-no-dialog.yaml        # E-UPD-13
    └── us-cp-17-startup-no-request.yaml    # E-UPD-14（行为层，字节码扫描见 L3）
```

构建与运行命令：
- 构建 direct APK：`./gradlew :androidApp:assembleDirectDebug`
- 构建 store APK：`./gradlew :androidApp:assembleStoreDebug`
- 运行 direct flows：`adb install direct.apk && maestro test maestro/flows/direct/`
- 运行 store flows：`adb install store.apk && maestro test maestro/flows/store/`

## L1 单元测试

### U-UPD-01 版本号比较

| 用例 | 输入 | 期望 |
|------|------|------|
| 新版本可用 | current="1.1.0", remote="1.2.0" | isNewerVersion = true |
| 已是最新 | current="1.2.0", remote="1.2.0" | isNewerVersion = false |
| 本地更新 | current="1.3.0", remote="1.2.0" | isNewerVersion = false |
| 次版本差异 | current="1.1.0", remote="1.1.1" | isNewerVersion = true |
| 主版本差异 | current="1.9.9", remote="2.0.0" | isNewerVersion = true |

### U-UPD-02 强制更新判定

| 用例 | 输入 | 期望 |
|------|------|------|
| forceUpdate=true | current="1.1.0", forceUpdate=true, minVersion="1.0.0" | FORCE_UPDATE |
| 低于 minVersion | current="0.9.0", forceUpdate=false, minVersion="1.0.0" | FORCE_UPDATE |
| 可选更新 | current="1.1.0", forceUpdate=false, minVersion="1.0.0" | OPTIONAL_UPDATE |
| 已是最新 | current="1.2.0", forceUpdate=false, minVersion="1.0.0" | UP_TO_DATE |

### U-UPD-03 平台分发

| 用例 | 输入 | 期望 |
|------|------|------|
| Android 平台 | platform="Android" | 返回 android 字段的 UpdateInfo |
| iOS 平台 | platform="iOS" | 返回 ios 字段的 UpdateInfo |
| HarmonyOS 平台 | platform="HarmonyOS" | 返回 harmonyos 字段的 UpdateInfo |
| 未知平台 | platform="Unknown" | 返回 null（UP_TO_DATE） |

### U-UPD-04 API 响应解析

| 用例 | 输入 | 期望 |
|------|------|------|
| 正常 JSON | 完整版本 JSON | 正确解析为 UpdateInfo |
| 缺少当前平台 | JSON 中无 android 字段 | 返回 UP_TO_DATE |
| 格式错误 | 非 JSON 响应 | 异常处理，返回 UP_TO_DATE |
| 空响应 | HTTP 200 空 body | 异常处理，返回 UP_TO_DATE |

### U-UPD-05 autoCheckUpdate 设置

| 用例 | 输入 | 期望 |
|------|------|------|
| 默认值 | 首次读取 | autoCheckUpdate = true |
| 关闭 | 设置为 false → 重新读取 | autoCheckUpdate = false |
| 持久化 | 设置为 false → 重启 → 读取 | autoCheckUpdate = false |

### U-UPD-07 AppHooks 默认空实现行为

**对应 AC**：US-CP-17 AC1/AC2/AC3 的代码层基础保证

| 用例 | 操作 | 期望 |
|------|------|------|
| onAppStart 默认空实现 | 调用 `AppHooks.Empty.onAppStart()` | 无异常，无副作用 |
| HomeOverlay 默认空 Composable | 在 Compose 测试环境渲染 `AppHooks.Empty.HomeOverlay()` | 无任何节点输出 |
| SettingsExtras 默认空 Composable | 在 Compose 测试环境渲染 `AppHooks.Empty.SettingsExtras()` | 无任何节点输出 |

代码位置：`shared/src/commonTest/kotlin/com/cleanpic/ui/AppHooksTest.kt`

### 国内分发与迁移（本次新增，对应 US-CP-15 国内/迁移/完整性 AC + 方案 §12）

#### U-UPD-08 检查端点超时与双端点 fallback（F2）

| 用例 | 输入 | 期望 |
|------|------|------|
| 主端点成功 | 主(CN)返回有效 version.json | 直接用主端点结果，不调兜底 |
| 主端点超时 | 主端点 hang 超过 timeout | 触发兜底(worker)端点，用其结果 |
| 主异常兜底成功 | 主抛异常、兜底正常 | 用兜底结果 |
| 两端点都失败 | 主超时 + 兜底异常 | 返回 UP_TO_DATE，不挂起 |
| 端点路径可配 | 主=Gitee raw 完整 URL（无 `/api/version` 后缀） | 正确 GET 该 URL 并解析 |

> 关键：必须装 `HttpTimeout`，否则"超时"分支永不触发（现状无超时）。

#### U-UPD-09 versionCode 优先比较（M1）

| 用例 | 输入 | 期望 |
|------|------|------|
| 同 version 名仅 bump code | current code=29, remote version 相同、code=30 | 判为有新版 |
| version 名更新 | current 1.5.0, remote 1.6.0 | 有新版（兼容旧逻辑） |
| code 相等 version 相等 | 同名同 code | UP_TO_DATE |
| remote 无 versionCode（旧响应） | 仅 version 字段 | 回退到 version 字符串比较 |

#### U-UPD-10 下载完整性校验（F1）

| 用例 | 下载内容 | 期望 |
|------|---------|------|
| 正常 APK | 真 ZIP，sha256/size 匹配 | 校验通过 → INSTALLING |
| 大小不符 | 字节数 ≠ size | 判 FAILED，不安装 |
| 哈希不符 | size 同但 sha256 不同 | 判 FAILED，不安装 |
| HTML 伪装包 | 内容是 HTML（非 ZIP 魔数 `PK\x03\x04`） | 判 FAILED，不安装；可回退 Worker 代理重试 |
| version.json 无 sha256（兜底/旧） | 缺完整性字段 | 跳过哈希校验（保持向后兼容），仅 ZIP 魔数兜底 |

#### U-UPD-11 version.json schema 解析

| 用例 | 输入 | 期望 |
|------|------|------|
| 含 sha256/size | 新字段齐全 | 正确反序列化进 UpdateInfo |
| 缺新字段 | 旧 schema | 默认空/0，不报错（`ignoreUnknownKeys` + 默认值） |

## L3 构建产物验证（编译期隔离断言）

本节是为 US-CP-17 新增的测试层级，验证 store flavor APK 不含升级相关字节码与资源。通过独立脚本 `scripts/verify-store-apk.sh <apk-path>` 执行，发布前手动跑一次，CI 上可在 Store flavor build job 末尾自动跑。

### B-UPD-01 字节码字符串扫描（核心断言）

**对应 AC**：US-CP-17 AC2（无升级网络请求）

| 检查项 | 命令 | 期望 |
|------|------|------|
| 无 UpdateChecker 类名 | `unzip -p <apk> classes*.dex \| strings \| grep -c "UpdateChecker"` | `0` |
| 无 UpdateInstaller 类名 | 同上，搜 `UpdateInstaller` | `0` |
| 无 UpdateDialog 类名 | 同上，搜 `UpdateDialog` | `0` |
| 无 Workers API URL | 同上，搜 `workers.dev` | `0` |
| 无 GitHub Release URL | 同上，搜 `releases/download` | `0` |
| 无升级包名 | 同上，搜 `com/cleanpic/update` | `0` |

只要字节码里没有升级 API URL 字符串，运行时就不可能发起对应网络请求，这是 AC2 "无升级网络流量" 的充分条件。因此不再做运行时抓包断言。

### B-UPD-02 Android 权限扫描

**对应 AC**：US-CP-17 AC1 + AC3 的系统层证据

| 检查项 | 命令 | 期望 |
|------|------|------|
| 无 REQUEST_INSTALL_PACKAGES | `aapt dump permissions <apk>` | 输出不含 `REQUEST_INSTALL_PACKAGES` |
| 无 INTERNET 权限（可选） | 同上 | 若项目除升级外无其他网络需求，应无 `INTERNET` |

### B-UPD-03 Manifest 资源扫描

| 检查项 | 命令 | 期望 |
|------|------|------|
| 无 FileProvider 声明 | `aapt dump xmltree <apk> AndroidManifest.xml \| grep -c "FileProvider"` | `0` |
| 无升级 mock URL | `aapt dump resources <apk> \| grep -c "workers.dev\|update"` | `0` |

### direct flavor 对称断言（防止反向回归）

同一套脚本应**也能**反向验证 direct APK **含有**这些项（只要 `UpdateChecker` 类存在即合格），防止未来重构不小心把升级代码从 direct flavor 也弄丢。

```
scripts/verify-store-apk.sh direct.apk  → 预期 FAIL（含有升级代码）
scripts/verify-store-apk.sh store.apk   → 预期 PASS（不含升级代码）
```

## L4 E2E 测试（Maestro）

### E-UPD-01 启动时可选更新弹窗

**前置**：mock server 返回可选更新（forceUpdate=false，version > 当前版本）

**对应 AC**：US-CP-13 AC1

```
步骤：
1. 启动 App
2. 等待 Splash 结束
3. 断言：更新弹窗可见
4. 断言：弹窗显示新版本号
5. 断言：弹窗显示"立即更新"按钮
6. 断言：弹窗显示"稍后提醒"按钮
7. 点击"稍后提醒"
8. 断言：弹窗消失，进入首页
```

### E-UPD-02 启动时强制更新弹窗

**前置**：mock server 返回强制更新（forceUpdate=true）

**对应 AC**：US-CP-13 AC2

```
步骤：
1. 启动 App
2. 等待 Splash 结束
3. 断言：全屏更新弹窗可见
4. 断言：仅显示"立即更新"按钮
5. 断言：无"稍后提醒"按钮
6. 断言：返回键无效（弹窗不可关闭）
```

### E-UPD-03 已是最新版不弹窗

**前置**：mock server 返回的版本 = 当前版本

**对应 AC**：US-CP-13 AC3

```
步骤：
1. 启动 App
2. 等待 Splash 结束
3. 断言：直接进入首页，无弹窗
```

### E-UPD-04 无网络不弹窗

**前置**：mock server 不可达（或关闭网络）

**对应 AC**：US-CP-13 AC4

```
步骤：
1. 启动 App
2. 等待 Splash 结束
3. 断言：直接进入首页，无弹窗，App 正常可用
```

### E-UPD-05 关闭自动检查后启动不弹窗

**前置**：mock server 返回可选更新

**对应 AC**：US-CP-13 AC5 + US-CP-16

```
步骤：
1. 启动 App → 稍后提醒 → 进入首页
2. 进入设置页
3. 关闭"自动检查更新"开关
4. 返回首页 → 关闭 App → 重新启动
5. 断言：直接进入首页，无弹窗
```

### E-UPD-06 设置页红点与版本提示

**前置**：mock server 返回可选更新（version="9.9.9"）

**对应 AC**：US-CP-14 AC1

```
步骤：
1. 启动 App → 稍后提醒 → 进入首页
2. 进入设置页
3. 断言："检查更新"旁显示新版本标记
4. 断言：显示"v9.9.9 可用"文字
```

### E-UPD-07 手动检查更新 — 有新版本

**前置**：mock server 返回可选更新

**对应 AC**：US-CP-14 AC2 + AC3

```
步骤：
1. 启动 App → 进入设置页
2. 点击"检查更新"
3. 断言：显示加载状态
4. 断言：弹出更新弹窗，显示版本号和更新日志
```

### E-UPD-08 手动检查更新 — 已是最新

**前置**：mock server 返回的版本 = 当前版本

**对应 AC**：US-CP-14 AC4

```
步骤：
1. 启动 App → 进入设置页
2. 点击"检查更新"
3. 断言：提示"已是最新版本"
```

### E-UPD-09 手动检查更新 — 无网络

**前置**：mock server 不可达

**对应 AC**：US-CP-14 AC5

```
步骤：
1. 启动 App → 进入设置页
2. 点击"检查更新"
3. 断言：提示"网络不可用，请稍后再试"
```

### E-UPD-10 Android 应用内下载

**前置**：mock server 返回可选更新，downloadUrl 指向测试 APK

**对应 AC**：US-CP-15 AC1

```
步骤：
1. 启动 App → 弹出更新弹窗
2. 点击"立即更新"
3. 断言：显示下载进度条
4. 断言：下载完成后弹出系统安装界面（验证 Intent 触发）
```

> 注：系统安装界面无法在 Maestro 中完整验证，验证到 Intent 触发即可。

### E-UPD-11 下载失败重试

**前置**：mock server 第一次下载请求返回错误，第二次正常

**对应 AC**：US-CP-15 AC4

```
步骤：
1. 触发更新下载
2. 断言：提示"下载失败，请检查网络后重试"
3. 断言：显示"重试"按钮
4. 点击"重试"
5. 断言：重新开始下载
```

### E-UPD-12 store flavor 设置页无升级区块

**前置**：安装 store flavor APK

**对应 AC**：US-CP-17 AC1

**目录**：`maestro/flows/store/us-cp-17-settings-no-update.yaml`

```
步骤：
1. 启动 App → 进入首页
2. 进入设置页
3. 断言：不存在 testTag 为 auto_check_update_toggle 的元素
4. 断言：不存在 testTag 为 check_update_button 的元素
5. 断言：设置页向下滚动到底，未见到任何"检查更新""自动检查""版本可用"文字
```

### E-UPD-13 store flavor 首页无升级弹窗

**前置**：安装 store flavor APK，设备可连网（验证即使网络可用也不会发起请求）

**对应 AC**：US-CP-17 AC3

**目录**：`maestro/flows/store/us-cp-17-home-no-dialog.yaml`

```
步骤：
1. 启动 App → 等待 Splash 结束
2. 断言：不存在 testTag 为 update_dialog 的元素
3. 等待 3 秒（模拟用户在首页停留）
4. 断言：仍不存在 update_dialog 元素
5. 断言：不存在 download_progress_dialog、installing_dialog、update_failed_dialog 等所有升级相关弹窗
```

### E-UPD-14 store flavor 启动无升级代码执行

**前置**：安装 store flavor APK

**对应 AC**：US-CP-17 AC2（行为层验证，流量层由 B-UPD-01 字节码扫描承担）

**目录**：`maestro/flows/store/us-cp-17-startup-no-request.yaml`

```
步骤：
1. 启动 App
2. 等待 Splash 结束，进入首页
3. 断言：首页正常渲染（无崩溃、无异常 Toast）
4. 断言：不存在任何升级相关 UI 元素（同 E-UPD-13）
5. 断言：App 无卡顿或异常加载状态
```

说明：AC2 的"无升级 API 网络流量"由 L3 字节码扫描 B-UPD-01 提供充分证明——store APK 不含 `workers.dev` 字符串，运行时不可能发起对应请求。本 Maestro flow 验证"启动过程无升级行为副作用"。

### E-UPD-15 direct flavor 全量回归

**前置**：安装 direct flavor APK，mock server 启动

**对应 AC**：US-CP-17 AC4

**目录**：`maestro/flows/direct/` 全目录

```
步骤：
1. 安装 direct flavor APK
2. 运行 maestro test maestro/flows/direct/
3. 断言：E-UPD-01 ~ E-UPD-11 全部通过（现有升级相关 flow）
4. 断言：所有非升级 flow（browse-photos、delete-confirm 等）也全部通过
```

此测试本质是"现有 12+ 条 Maestro flow 在 direct flavor 下零回归"的保证。

### U-UPD-06 弹窗遮罩阻止背景点击

**层级**：L2 组件测试（Compose UI Test + Robolectric）

| 用例 | 弹窗类型 | 期望 |
|------|---------|------|
| 强制更新弹窗阻止穿透 | UpdateDialog (isForceUpdate=true) | 点击遮罩区域，背景按钮不响应 |
| 可选更新弹窗阻止穿透 | UpdateDialog (isForceUpdate=false) | 点击遮罩区域，背景按钮不响应 |
| 下载进度弹窗阻止穿透 | DownloadProgressDialog | 点击遮罩区域，背景按钮不响应 |
| 下载失败弹窗阻止穿透 | UpdateFailedDialog | 点击遮罩区域，背景按钮不响应 |

代码位置：`shared/src/androidUnitTest/kotlin/com/cleanpic/update/UpdateDialogOverlayTest.kt`

### W-UPD-01 Worker 下发 Gitee 直链（P0 桥接，worker/test）

| 检查项 | 期望 |
|------|------|
| `/api/version` 的 `android.downloadUrl` | 等于配置的 Gitee 直链，**不含 `workers.dev/download`** |
| `/api/version` 的 `android.sha256` / `size` | 与 Gitee APK 一致（供客户端完整性校验） |
| `/api/version` 的 `harmonyos.downloadUrl` | 暂仍为 workers.dev 代理（鸿蒙无国内产物、无真实用户；待鸿蒙产物上 Gitee 后再切） |
| `/download/*` 代理路由 | 仍保留（海外/历史回退可用） |

### E-UPD-16 存量 app 迁移（手动/真机）

**对应 AC**：US-CP-15 国内无代理升级 + 存量迁移 AC

```
前置：装一个旧版（UPDATE_API_URL=workers.dev）；Worker 已改下发 Gitee 直链；Gitee 已传新版 APK
步骤：
1. 启动旧版 → 触发检查更新（小请求经 workers.dev）
2. 断言：拿到的 downloadUrl 指向 gitee.com（非 workers.dev）
3. 点立即更新 → 从 Gitee 下载（国内网络不挂代理）
4. 断言：下载完成、完整性校验通过、弹出系统安装界面
5. 装上新版 → 新版检查走 Gitee raw version.json（断网 workers.dev 仍可检查）
```

### E-UPD-17 国内真机穿透实测（上线前，需国内环境）

```
前置：国内真机/真网，不挂代理
步骤：
1. 多次触发 workers.dev/api/version，记录穿透成功率（评估 P0 覆盖）
2. Gitee raw version.json 与 Gitee release 附件均可匿名直链下载
3. 手动"检查更新"失败时给出明确提示 + Gitee 下载页引导（不静默吞错）
```

## 测试覆盖矩阵

| User Story | L1 单元 | L2 组件 | L3 构建产物 | L4 E2E (Maestro) |
|-----------|---------|---------|------------|-------------------|
| US-CP-13 启动检查更新 | U-UPD-01~04 | U-UPD-06 | — | E-UPD-01~04（direct） |
| US-CP-14 手动检查+红点 | U-UPD-04 | — | — | E-UPD-06~09（direct） |
| US-CP-15 下载安装 | — | U-UPD-06 | — | E-UPD-10~11（direct） |
| US-CP-15 国内无代理升级+迁移+完整性 | U-UPD-08~11 | — | W-UPD-01 | E-UPD-16~17（手动/真机） |
| US-CP-16 开关自动检查 | U-UPD-05 | — | — | E-UPD-05（direct） |
| US-CP-17 商店版无应用内升级 | U-UPD-07 | — | B-UPD-01~03 | E-UPD-12~15 |
