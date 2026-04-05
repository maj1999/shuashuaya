# EP6: 自动升级 — 测试场景

|文档状态| 初稿 | 2026-04-05 |

## 测试约束

自动升级功能依赖远程 API，E2E 测试需要以下前置条件之一：
- 本地 mock server（推荐，用于 CI）
- 真实 Cloudflare Workers 测试环境

Maestro 测试使用本地 mock server（`scripts/mock-update-server.sh`），通过 BuildConfig 注入测试 API 地址。

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

## 测试覆盖矩阵

| User Story | L1 单元 | L4 E2E (Maestro) |
|-----------|---------|-------------------|
| US-CP-13 启动检查更新 | U-UPD-01~04 | E-UPD-01~04 |
| US-CP-14 手动检查+红点 | U-UPD-04 | E-UPD-06~09 |
| US-CP-15 下载安装 | — | E-UPD-10~11 |
| US-CP-16 开关自动检查 | U-UPD-05 | E-UPD-05 |
