# 日志体系设计（Kermit + 全量埋点 + 可导出）

- 日期：2026-06-15
- 状态：待评审
- 触发背景：`删除失败: deleteLauncher not registered` 问题排查时发现**全项目零日志**（`shared/` + `androidApp/` 无任何 `android.util.Log`/`println`/三方日志库），原生边界故障无法定位。

## 1. 目标

为这个 Kotlin Multiplatform 项目建立**必要、可定位、可在物理机手测场景下取回**的日志体系：

1. 关键路径（尤其原生边界）出问题时，日志能还原调用链
2. 不依赖 adb 连接也能拿到日志：用户在 App 内一键导出日志文件下载到本机，再自行处理（发送/查看）
3. release 包零 PII 泄露（照片 App，绝不打文件名/URI/路径）

## 2. 非目标（YAGNI）

- 不做远程日志上报 / 崩溃采集后台（后续如需可接 Kermit 的 CrashlyticsLogWriter，本期只留扩展点）
- 不做日志检索 UI（导出文件即可）
- iOS/Harmony 仅保证日志「能打印」（NSLog/println），不做文件落地与导出（这两端目前是 stub）

## 3. 框架选型：Kermit 2.0.4

- `co.touchlab:kermit:2.0.4`，加入 `commonMain` 依赖，版本登记到 buildSrc `Versions.KERMIT`
- 选 Kermit 理由：KMP 原生、自带平台 writer（Android→Logcat，iOS→NSLog）、severity/tag 完整、可自定义 `LogWriter` 实现文件落地、后续可接崩溃采集
- 兼容性：Kermit 2.0.x 支持 Kotlin 2.x（项目用 2.1.21）

## 4. 架构

### 4.1 统一入口 `commonMain/com/cleanpic/log/AppLog.kt`

```
fun logger(tag: String): Logger = Logger.withTag(tag)        // 各模块: private val log = logger("MediaRepo")

object LogConfig {
    fun init(debug: Boolean, writers: List<LogWriter>) {
        Logger.setLogWriters(writers)                         // 平台 writer + 文件 writer
        Logger.setMinSeverity(if (debug) Severity.Verbose else Severity.Warn)
    }
}

// 脱敏助手：URI/路径 -> 仅保留计数或尾部短 hash，绝不输出原值
fun redactCount(n: Int): String
fun redactType(type: MediaType): String
```

### 4.2 初始化接线

- `ServiceLocator.initialize(...)` 已持有 `isDebugBuild`；在其中调用 `LogConfig.init(isDebugBuild, writers)`
- `writers` 由平台侧装配（见 4.3），commonMain 不感知具体 writer 类型
- 全平台一处生效，无散落配置

### 4.3 输出目的地（① + ② + ③ 全做）

| # | 目的地 | 实现 | 适用场景 |
|---|--------|------|----------|
| ① | **Logcat** | Kermit 默认 `platformLogWriter()` | 连电脑时 `adb logcat` |
| ② | **设备文件** | Android 侧自定义 `FileLogWriter`，写 `context.filesDir/logs/app.log` | 随时 `adb pull` 或配合 ③ 取回 |
| ③ | **App 内导出下载** | 设置页触发 → SAF `ACTION_CREATE_DOCUMENT` 保存框，用户选位置下载 | 不连 adb，把日志文件存到本机自行处理 |

**② 文件落地细节**
- 路径：App 私有目录 `filesDir/logs/app.log`（随卸载清除，无需存储权限）
- 滚动策略：单文件上限 ~1MB，超限轮转为 `app.log.1`（保留 1 个历史，总占用 ≤ 2MB）
- 仅 Android `actual` 实现；iOS/Harmony 不注册此 writer
- 线程安全：写入加锁或单线程 dispatcher，避免并发损坏

**③ 导出下载细节**
- `SettingsScreenState` 新增回调 `onExportLogs: () -> Unit`
- 渲染落点**复用已被全部 6 个主题布局渲染的 `extras` 注入槽**，零布局文件改动
- 触发交互：设置页「导出诊断日志」行（放入 extras 注入内容内）
- Android 导出：用 SAF `ActivityResultContracts.CreateDocument("text/plain")` 弹系统保存框，用户自选位置下载；**默认文件名带导出时刻时间戳**，格式 `刷刷鸭-日志-yyyyMMdd-HHmmss.log`（如 `刷刷鸭-日志-20260615-153012.log`），时间戳在点击导出时生成
- 不需要存储权限、不需要 FileProvider；导出时把 `filesDir/logs/app.log`（含轮转的 `app.log.1`）内容写入用户选中的 URI
- 沿用 `deleteLauncher` 式 companion 回调把宿主 Activity 的 `CreateDocument` launcher 注入导出器
- 若日志文件为空/不存在：导出当前内存缓冲或提示「暂无日志」，不报错
- iOS/Harmony：`onExportLogs` 为 no-op（这两端无文件 writer）

## 5. 级别与隐私策略

| 级别 | Debug | Release |
|------|-------|---------|
| Verbose/Debug/Info | ✅ 打印 | ❌ 被 minSeverity(Warn) 过滤 |
| Warn/Error | ✅ | ✅（脱敏后） |

- **代码层硬约束**：任何级别都**只传数量/类型/耗时/错误类型/布尔结果**，禁止传文件名、URI、绝对路径、相册标题等 PII
- 文件日志同样脱敏；文件限私有目录、随卸载清除
- review 检查项：新增日志语句不得拼接 `item.displayName`/`uri.toString()`/路径

## 6. 埋点清单（全量）

### 6.1 原生边界（最高优先 — 本次踩坑域）
- `MainActivity`：`onCreate` 注册 `deleteLauncher`/`permissionLauncher`（INFO）、`onDestroy` 注销（INFO）
- `AndroidMediaRepository`：
  - 删除请求发起（数量、类型）
  - **`deleteLauncher == null` → ERROR**（精确暴露本次问题）
  - 系统删除授权回调 granted 结果
  - `queryMedia` 查询到的条数
- `AndroidPermission`：权限请求发起 + 回调 granted map 结果（仅布尔）
- `AndroidVideoPlayer`：prepare/play/release/error
- 更新流程接线（`UpdateWiring` / direct flavor）：检测/下载进度里程碑/失败

### 6.2 ViewModel 关键流转
- `loadMedia(type)`：开始 + 命中数量
- `confirmDelete()`：发起 + 成功数 / 失败
- 洗牌袋（Shuffle Bag）抽取：袋容量、本轮抽取数量（不打具体 id/内容）

### 6.3 统一错误
- 所有 `Result.failure` 分支：ERROR + 异常类型 + message（message 已是我方可控文案，不含 PII）

## 7. 测试方案（遵守 CLAUDE.md 测试纪律）

### 单元测试（新增）
- `AppLogTest` / `LogConfigTest`，用 Kermit `TestLogWriter` 捕获输出断言：
  1. debug=true → Verbose 级别可被记录；debug=false → Info/Debug/Verbose 被过滤，Warn/Error 保留
  2. `redactCount`/`redactType` 输出不含原始 URI/路径/文件名
  3. `LogConfig.init` 正确装配 writers 与 minSeverity
- `FileLogWriter`（androidUnitTest/Robolectric）：写入后文件存在、超限轮转、并发写不崩

### E2E
- 日志不改变任何 UI 行为，现有 Maestro 流不受影响、不需新增流
- 删除流程修复验证仍按既定：重装当前 direct/debug 包后跑删除 flow（与本日志体系解耦）

## 8. 改动文件清单（预估）

| 文件 | 改动 |
|------|------|
| `buildSrc/.../CleanPicBuildConfig.kt` | 新增 `Versions.KERMIT` |
| `shared/build.gradle.kts` | commonMain 加 kermit 依赖 |
| `shared/.../log/AppLog.kt` | 新增：logger/LogConfig/redact |
| `shared/.../log/FileLogWriter.kt`（androidMain） | 新增：文件 writer + 轮转 |
| `shared/.../di/ServiceLocator.kt` | 调用 `LogConfig.init` |
| `shared/.../media/AndroidMediaRepository.kt` | 埋点 |
| `shared/.../permission/AndroidPermission.kt` | 埋点 |
| `shared/.../media/AndroidVideoPlayer.kt` | 埋点 |
| `shared/.../viewmodel/*ViewModel.kt` | 埋点 |
| `shared/.../ui/settings/SettingsScreenState.kt` | 加 `onExportLogs` |
| 设置页 extras 注入处 | 加「导出诊断日志」行 |
| `shared/.../log/LogExporter.kt`（androidMain） | 新增：拼装日志内容 + 时间戳文件名 + 写入用户选中 URI |
| `androidApp/.../MainActivity.kt` | 埋点 + 注册 `CreateDocument` launcher 注入导出器 |
| `shared/.../log/*Test.kt` | 新增单测 |

## 9. 开放项（评审确认）

1. 导出入口交互：长按版本号 vs 显式「导出诊断日志」行 —— 本设计取**显式行（放 extras 内）**，更易发现且零布局改动
2. 导出行可见性：全包可见（含 release，便于手测取回） vs 仅 debug —— 本设计取**全包可见**，因 release 也需要诊断
3. 文件轮转保留份数：当前取 1 份历史（总 ≤2MB），如需更多可调
