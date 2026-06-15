# 日志体系 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 CleanPic（刷刷鸭）KMP 项目引入 Kermit 日志体系：统一入口 + 文件落地 + App 内导出下载（带时间戳文件名）+ 全量埋点，release 包脱敏只留 Warn/Error。

**Architecture:** `commonMain` 提供 `AppLog`（logger/LogConfig/脱敏）与 `LogExportController`（导出触发回调）；`androidMain` 提供 `FileLogWriter`（滚动写 `filesDir/logs/app.log`）与 `LogExporter`（拼装内容 + 时间戳文件名）；`MainActivity` 注册 `CreateDocument` launcher 并把 `FileLogWriter` 注入 `ServiceLocator.initialize`；埋点覆盖原生边界、ViewModel 流转、所有 `Result.failure`。

**Tech Stack:** Kotlin 2.1.21 + Kermit 2.0.4 + Compose Multiplatform 1.7.3，Android SAF（`ActivityResultContracts.CreateDocument`），测试 kotlin.test + Robolectric 4.16.1。

参考设计：`docs/superpowers/specs/2026-06-15-logging-system-design.md`

---

## File Structure

| 文件 | 职责 |
|------|------|
| `buildSrc/src/main/kotlin/CleanPicBuildConfig.kt` | 新增 `Versions.KERMIT` |
| `shared/build.gradle.kts` | commonMain 加 kermit 依赖 |
| `shared/src/commonMain/kotlin/com/cleanpic/log/AppLog.kt` | 新增：`logger(tag)` / `LogConfig.init` / `redactCount`·`redactType` |
| `shared/src/commonMain/kotlin/com/cleanpic/log/LogExportController.kt` | 新增：导出触发回调注册点（平台无关） |
| `shared/src/androidMain/kotlin/com/cleanpic/log/FileLogWriter.kt` | 新增：滚动文件 LogWriter |
| `shared/src/androidMain/kotlin/com/cleanpic/log/LogExporter.kt` | 新增：拼装日志内容 + 时间戳文件名 |
| `shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt` | `initialize` 增 `logWriters` 参数 + 调 `LogConfig.init` |
| `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt` | 增 `onExportLogs` 回调 |
| `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreen.kt` | 接线 `onExportLogs` |
| `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt` | 新增「导出诊断日志」按钮 |
| `androidApp/src/main/java/com/cleanpic/android/MainActivity.kt` | 注册 CreateDocument launcher + 注入 FileLogWriter + 埋点 |
| `shared/.../media/AndroidMediaRepository.kt`、`permission/AndroidPermission.kt`、`media/AndroidVideoPlayer.kt` | 埋点 |
| `shared/.../viewmodel/ViewerViewModel.kt` | 埋点 |
| 测试文件（见各 Task） | 单测 |

---

## Task 1: 引入 Kermit 依赖

**Files:**
- Modify: `buildSrc/src/main/kotlin/CleanPicBuildConfig.kt:29` 附近（依赖版本区）
- Modify: `shared/build.gradle.kts:59`（commonMain dependencies 内）

- [ ] **Step 1: 加版本常量**

在 `CleanPicBuildConfig.kt` 的 `object Versions` 里，`KTOR` 那组附近加：

```kotlin
const val KERMIT = "2.0.4"
```

- [ ] **Step 2: commonMain 加依赖**

在 `shared/build.gradle.kts` 的 `commonMain` dependencies 块（`zoomable` 那行之后）加：

```kotlin
            // 日志（KMP 原生，自带平台 writer）
            implementation("co.touchlab:kermit:${Versions.KERMIT}")
```

- [ ] **Step 3: 验证依赖解析**

Run: `./gradlew :shared:dependencies --configuration androidDebugCompileClasspath 2>/dev/null | grep -i kermit`
Expected: 出现 `co.touchlab:kermit:2.0.4`（说明已解析）

- [ ] **Step 4: 编译通过**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add buildSrc/src/main/kotlin/CleanPicBuildConfig.kt shared/build.gradle.kts
git commit -m "build(log): 引入 Kermit 2.0.4 到 commonMain"
```

---

## Task 2: AppLog 核心（logger + LogConfig + 脱敏）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/log/AppLog.kt`
- Test: `shared/src/commonTest/kotlin/com/cleanpic/log/AppLogTest.kt`

- [ ] **Step 1: 写失败测试**

`shared/src/commonTest/kotlin/com/cleanpic/log/AppLogTest.kt`：

```kotlin
package com.cleanpic.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.cleanpic.model.MediaType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class RecordingWriter : LogWriter() {
    val entries = mutableListOf<Pair<Severity, String>>()
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        entries.add(severity to message)
    }
}

class AppLogTest {
    @AfterTest
    fun tearDown() {
        // 复位为默认，避免污染其它测试
        LogConfig.init(debug = true, extraWriters = emptyList())
    }

    @Test
    fun release_filters_below_warn() {
        val rec = RecordingWriter()
        LogConfig.init(debug = false, extraWriters = listOf(rec))
        val log = logger("T")
        log.i { "info-msg" }
        log.w { "warn-msg" }
        log.e { "err-msg" }
        val severities = rec.entries.map { it.first }
        assertFalse(severities.contains(Severity.Info), "release 不应记录 Info")
        assertTrue(severities.contains(Severity.Warn))
        assertTrue(severities.contains(Severity.Error))
    }

    @Test
    fun debug_records_verbose() {
        val rec = RecordingWriter()
        LogConfig.init(debug = true, extraWriters = listOf(rec))
        logger("T").v { "verbose-msg" }
        assertTrue(rec.entries.any { it.first == Severity.Verbose })
    }

    @Test
    fun redact_count_outputs_number_only() {
        assertEquals("5", redactCount(5))
    }

    @Test
    fun redact_type_outputs_enum_name_not_content() {
        val out = redactType(MediaType.PHOTO)
        assertEquals("PHOTO", out)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `scripts/test.sh`
Expected: FAIL —— `Unresolved reference: logger / LogConfig / redactCount / redactType`

- [ ] **Step 3: 实现 AppLog**

`shared/src/commonMain/kotlin/com/cleanpic/log/AppLog.kt`：

```kotlin
package com.cleanpic.log

import co.touchlab.kermit.Logger
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.cleanpic.model.MediaType

/** 取一个带 tag 的 logger。约定：各模块 private val log = logger("ModuleTag")。 */
fun logger(tag: String): Logger = Logger.withTag(tag)

/**
 * 全局日志配置。在平台初始化（ServiceLocator.initialize）时调用一次。
 * - debug: 控制最低级别。debug=true → Verbose 全开；false → 仅 Warn/Error。
 * - extraWriters: 平台注入的额外 writer（如 Android 的 FileLogWriter）。
 */
object LogConfig {
    fun init(debug: Boolean, extraWriters: List<LogWriter> = emptyList()) {
        Logger.setLogWriters(listOf(platformLogWriter()) + extraWriters)
        Logger.setMinSeverity(if (debug) Severity.Verbose else Severity.Warn)
    }
}

/** 脱敏：数量直接输出，不含任何内容。 */
fun redactCount(n: Int): String = n.toString()

/** 脱敏：仅输出媒体类型枚举名，不含文件名/路径/URI。 */
fun redactType(type: MediaType): String = type.name
```

- [ ] **Step 4: 运行确认通过**

Run: `scripts/test.sh`
Expected: PASS（AppLogTest 4 个用例全绿）

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/log/AppLog.kt shared/src/commonTest/kotlin/com/cleanpic/log/AppLogTest.kt
git commit -m "feat(log): AppLog 统一入口 + 级别策略 + 脱敏助手"
```

---

## Task 3: FileLogWriter（滚动文件落地）

**Files:**
- Create: `shared/src/androidMain/kotlin/com/cleanpic/log/FileLogWriter.kt`
- Test: `shared/src/androidUnitTest/kotlin/com/cleanpic/log/FileLogWriterTest.kt`

- [ ] **Step 1: 写失败测试**

`shared/src/androidUnitTest/kotlin/com/cleanpic/log/FileLogWriterTest.kt`：

```kotlin
package com.cleanpic.log

import co.touchlab.kermit.Severity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class FileLogWriterTest {

    @Test
    fun writes_log_line_to_file() {
        val dir = Files.createTempDirectory("logtest").toFile()
        val writer = FileLogWriter(dir, maxBytes = 1024)
        writer.log(Severity.Warn, "hello-line", "Tag", null)
        val logFile = File(dir, "app.log")
        assertTrue("日志文件应存在", logFile.exists())
        assertTrue("应包含写入内容", logFile.readText().contains("hello-line"))
        assertTrue("应包含 tag", logFile.readText().contains("Tag"))
    }

    @Test
    fun rotates_when_exceeding_max_bytes() {
        val dir = Files.createTempDirectory("logtest").toFile()
        val writer = FileLogWriter(dir, maxBytes = 200)
        repeat(50) { writer.log(Severity.Warn, "padding-line-$it-xxxxxxxxxx", "T", null) }
        val current = File(dir, "app.log")
        val rotated = File(dir, "app.log.1")
        assertTrue("轮转后应存在历史文件", rotated.exists())
        assertTrue("当前文件不应超过上限太多", current.length() <= 200 + 256)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `scripts/test.sh`
Expected: FAIL —— `Unresolved reference: FileLogWriter`

- [ ] **Step 3: 实现 FileLogWriter**

`shared/src/androidMain/kotlin/com/cleanpic/log/FileLogWriter.kt`：

```kotlin
package com.cleanpic.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import java.io.File

/**
 * 把日志滚动写入 dir/app.log，超过 maxBytes 轮转为 app.log.1（保留 1 份历史，总占用 ≤ 2*maxBytes）。
 * 线程安全：写操作整体加锁。仅落脱敏内容（调用方保证不传 PII）。
 */
class FileLogWriter(
    private val dir: File,
    private val maxBytes: Long = 1_000_000L
) : LogWriter() {

    private val lock = Any()
    private val current: File get() = File(dir, "app.log")
    private val rotated: File get() = File(dir, "app.log.1")

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        synchronized(lock) {
            if (!dir.exists()) dir.mkdirs()
            val line = buildString {
                append(severity.name.first())  // V/D/I/W/E/A 首字母
                append('/')
                append(tag)
                append(": ")
                append(message)
                if (throwable != null) {
                    append(" | ")
                    append(throwable::class.simpleName)
                    append(": ")
                    append(throwable.message ?: "")
                }
                append('\n')
            }
            val f = current
            if (f.exists() && f.length() + line.length > maxBytes) {
                if (rotated.exists()) rotated.delete()
                f.renameTo(rotated)
            }
            current.appendText(line)
        }
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `scripts/test.sh`
Expected: PASS（FileLogWriterTest 2 个用例绿）

- [ ] **Step 5: Commit**

```bash
git add shared/src/androidMain/kotlin/com/cleanpic/log/FileLogWriter.kt shared/src/androidUnitTest/kotlin/com/cleanpic/log/FileLogWriterTest.kt
git commit -m "feat(log): FileLogWriter 滚动写入 filesDir/logs"
```

---

## Task 4: ServiceLocator 接线 LogConfig

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt`
- Test: `shared/src/commonTest/kotlin/com/cleanpic/log/LogConfigWiringTest.kt`

- [ ] **Step 1: 写失败测试**

`shared/src/commonTest/kotlin/com/cleanpic/log/LogConfigWiringTest.kt`：

```kotlin
package com.cleanpic.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.cleanpic.di.ServiceLocator
import com.cleanpic.mock.MockMediaRepository
import com.cleanpic.mock.MockAppSettings
import com.cleanpic.mock.MockPermissionManager
import com.cleanpic.mock.MockVideoPlayer
import kotlin.test.Test
import kotlin.test.assertTrue

private class Rec : LogWriter() {
    val msgs = mutableListOf<String>()
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) { msgs.add(message) }
}

class LogConfigWiringTest {
    @Test
    fun initialize_applies_log_writers() {
        val rec = Rec()
        ServiceLocator.isDebugBuild = true
        ServiceLocator.initialize(
            mediaRepo = MockMediaRepository(),
            settings = MockAppSettings(),
            permission = MockPermissionManager(),
            player = MockVideoPlayer(),
            logWriters = listOf(rec)
        )
        logger("WireTest").i { "wired-ok" }
        assertTrue(rec.msgs.any { it == "wired-ok" }, "ServiceLocator.initialize 应把 logWriters 接入全局 logger")
    }
}
```

> 注：若 `MockAppSettings`/`MockPermissionManager`/`MockVideoPlayer` 在 `commonTest/mock` 下不存在，先查 `shared/src/commonTest/kotlin/com/cleanpic/mock/` 实际类名并替换（已知存在 `MockMediaRepository`）。缺失的 mock 用最简实现补齐（只为通过 initialize）。

- [ ] **Step 2: 运行确认失败**

Run: `scripts/test.sh`
Expected: FAIL —— `initialize` 没有 `logWriters` 参数（编译错误）

- [ ] **Step 3: 改 ServiceLocator**

在 `ServiceLocator.kt` 顶部加 import：

```kotlin
import co.touchlab.kermit.LogWriter
import com.cleanpic.log.LogConfig
```

把 `initialize` 签名与体改为（新增 `logWriters` 参数 + 末尾调用 `LogConfig.init`）：

```kotlin
    fun initialize(
        mediaRepo: MediaRepository,
        settings: AppSettings,
        permission: PermissionManager,
        player: VideoPlayer,
        pickStateStore: PickStateStore = InMemoryPickStateStore(),
        statsStore: com.cleanpic.stats.StatsStore = com.cleanpic.stats.InMemoryStatsStore(),
        logWriters: List<LogWriter> = emptyList()
    ) {
        mediaRepository = mediaRepo
        appSettings = settings
        permissionManager = permission
        videoPlayer = player
        this.pickStateStore = pickStateStore
        this.statsStore = statsStore
        themeManager.switchTheme(settings.theme)
        LogConfig.init(isDebugBuild, logWriters)
    }
```

- [ ] **Step 4: 运行确认通过**

Run: `scripts/test.sh`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/di/ServiceLocator.kt shared/src/commonTest/kotlin/com/cleanpic/log/LogConfigWiringTest.kt
git commit -m "feat(log): ServiceLocator.initialize 接入 LogConfig + logWriters"
```

---

## Task 5: 导出能力（LogExportController + LogExporter + MainActivity 接线）

**Files:**
- Create: `shared/src/commonMain/kotlin/com/cleanpic/log/LogExportController.kt`
- Create: `shared/src/androidMain/kotlin/com/cleanpic/log/LogExporter.kt`
- Modify: `androidApp/src/main/java/com/cleanpic/android/MainActivity.kt`
- Test: `shared/src/androidUnitTest/kotlin/com/cleanpic/log/LogExporterTest.kt`

- [ ] **Step 1: 写失败测试（文件名 + 内容拼装，纯逻辑）**

`shared/src/androidUnitTest/kotlin/com/cleanpic/log/LogExporterTest.kt`：

```kotlin
package com.cleanpic.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Calendar
import java.util.TimeZone

class LogExporterTest {

    @Test
    fun filename_contains_timestamp() {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            clear(); set(2026, Calendar.JUNE, 15, 15, 30, 12)
        }
        val name = LogExporter.exportFileName(cal.timeInMillis)
        assertEquals("刷刷鸭-日志-20260615-153012.log", name)
    }

    @Test
    fun collect_concatenates_current_and_rotated() {
        val dir = Files.createTempDirectory("exp").toFile()
        File(dir, "app.log.1").writeText("OLD\n")
        File(dir, "app.log").writeText("NEW\n")
        val content = LogExporter.collect(dir)
        // 旧的在前、当前在后
        assertTrue(content.indexOf("OLD") < content.indexOf("NEW"))
    }

    @Test
    fun collect_empty_when_no_files() {
        val dir = Files.createTempDirectory("exp2").toFile()
        assertEquals("", LogExporter.collect(dir))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `scripts/test.sh`
Expected: FAIL —— `Unresolved reference: LogExporter`

- [ ] **Step 3a: 实现 LogExportController（commonMain）**

`shared/src/commonMain/kotlin/com/cleanpic/log/LogExportController.kt`：

```kotlin
package com.cleanpic.log

/**
 * 导出触发的平台无关入口。Android MainActivity 注册 onRequestExport（弹 SAF 保存框）；
 * 设置页「导出诊断日志」按钮调用 export()。iOS/Harmony 未注册时 export() 为 no-op。
 */
object LogExportController {
    var onRequestExport: (() -> Unit)? = null
    fun export() { onRequestExport?.invoke() }
}
```

- [ ] **Step 3b: 实现 LogExporter（androidMain）**

`shared/src/androidMain/kotlin/com/cleanpic/log/LogExporter.kt`：

```kotlin
package com.cleanpic.log

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 把 filesDir/logs 下的日志拼装并生成带时间戳的导出文件名。 */
object LogExporter {

    /** 导出文件名：刷刷鸭-日志-yyyyMMdd-HHmmss.log，时间戳取点击导出时刻。 */
    fun exportFileName(nowMillis: Long): String {
        val fmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return "刷刷鸭-日志-${fmt.format(Date(nowMillis))}.log"
    }

    /** 拼装日志内容：历史(app.log.1) 在前，当前(app.log) 在后；都不存在则空串。 */
    fun collect(logsDir: File): String {
        val sb = StringBuilder()
        File(logsDir, "app.log.1").takeIf { it.exists() }?.let { sb.append(it.readText()) }
        File(logsDir, "app.log").takeIf { it.exists() }?.let { sb.append(it.readText()) }
        return sb.toString()
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `scripts/test.sh`
Expected: PASS（LogExporterTest 3 个用例绿）

- [ ] **Step 5: MainActivity 接线（编译验证，非单测）**

在 `MainActivity.kt` 加 import：

```kotlin
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.cleanpic.log.FileLogWriter
import com.cleanpic.log.LogExportController
import com.cleanpic.log.LogExporter
import com.cleanpic.log.logger
import java.io.File
```

在类内（permissionLauncher/deleteLauncher 旁）加 logs 目录与导出 launcher：

```kotlin
    private val logsDir by lazy { File(filesDir, "logs") }
    private val log = logger("MainActivity")

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) { log.i { "导出取消" }; return@registerForActivityResult }
        runCatching {
            contentResolver.openOutputStream(uri)?.use { out ->
                val content = LogExporter.collect(logsDir).ifEmpty { "(暂无日志)" }
                out.write(content.toByteArray())
            }
        }.onFailure { log.e(it) { "导出写入失败" } }
    }
```

在 `onCreate` 里，`ServiceLocator.initialize(...)` 之前注册导出回调，并把 FileLogWriter 传入 initialize：

```kotlin
        LogExportController.onRequestExport = {
            exportLauncher.launch(LogExporter.exportFileName(System.currentTimeMillis()))
        }
```

把 `ServiceLocator.initialize(...)` 调用补上 `logWriters` 参数：

```kotlin
        ServiceLocator.initialize(
            mediaRepo = AndroidMediaRepository(applicationContext),
            settings = AndroidAppSettings(applicationContext),
            permission = androidPermission,
            player = AndroidVideoPlayer(),
            pickStateStore = AndroidPickStateStore(applicationContext),
            statsStore = com.cleanpic.stats.AndroidStatsStore(applicationContext),
            logWriters = listOf(FileLogWriter(logsDir))
        )
```

在 `onDestroy` 里清回调（与现有 launcher 清理并列）：

```kotlin
        LogExportController.onRequestExport = null
```

- [ ] **Step 6: 编译通过**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/log/LogExportController.kt shared/src/androidMain/kotlin/com/cleanpic/log/LogExporter.kt shared/src/androidUnitTest/kotlin/com/cleanpic/log/LogExporterTest.kt androidApp/src/main/java/com/cleanpic/android/MainActivity.kt
git commit -m "feat(log): SAF 导出下载日志（带时间戳文件名）"
```

---

## Task 6: 设置页「导出诊断日志」入口

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt`
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt`

- [ ] **Step 1: State 增回调**

`SettingsScreenState.kt` 在 `onResetHistory` 之后加字段：

```kotlin
    /** 导出诊断日志（SAF 下载） */
    val onExportLogs: () -> Unit = {},
```

- [ ] **Step 2: SettingsScreen 接线**

`SettingsScreen.kt`：顶部加 import `import com.cleanpic.log.LogExportController`，在 `onResetHistory = ...` 之后加：

```kotlin
        onExportLogs = { LogExportController.export() },
```

- [ ] **Step 3: SharedSettingsLayout 渲染按钮**

在「浏览记录」section（`ResetHistoryButton` 之后、`Spacer` 之前，约 `SharedSettingsLayout.kt:127`）加：

```kotlin
                ExportLogsButton(theme) { state.onExportLogs() }
```

并在文件末尾 `ResetHistoryButton` 定义附近新增组件（复刻其样式）：

```kotlin
@Composable
private fun ExportLogsButton(theme: ThemeTokens, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .testTag("export_logs_button")
            .clip(RoundedCornerShape(theme.borderRadius.dp))
            .background(cardBackground(theme))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconPainter(name = "back", theme = theme, size = 18.dp, colorOverride = theme.iconStrokeColor)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "导出诊断日志", fontSize = 14.sp, color = Color(theme.colorText))
    }
}
```

- [ ] **Step 4: 编译通过**

Run: `scripts/build-android.sh`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreenState.kt shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SettingsScreen.kt shared/src/commonMain/kotlin/com/cleanpic/ui/settings/SharedSettingsLayout.kt
git commit -m "feat(log): 设置页新增「导出诊断日志」入口"
```

---

## Task 7: 原生边界埋点（含 deleteLauncher 回归测试）

**Files:**
- Modify: `shared/src/androidMain/kotlin/com/cleanpic/media/AndroidMediaRepository.kt`
- Modify: `shared/src/androidMain/kotlin/com/cleanpic/permission/AndroidPermission.kt`
- Modify: `shared/src/androidMain/kotlin/com/cleanpic/media/AndroidVideoPlayer.kt`
- Modify: `androidApp/src/main/java/com/cleanpic/android/MainActivity.kt`
- Test: `shared/src/androidUnitTest/kotlin/com/cleanpic/media/DeleteLauncherLoggingTest.kt`

- [ ] **Step 1: 写失败测试（deleteLauncher==null 时打 ERROR）**

`shared/src/androidUnitTest/kotlin/com/cleanpic/media/DeleteLauncherLoggingTest.kt`：

```kotlin
package com.cleanpic.media

import androidx.test.core.app.ApplicationProvider
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.cleanpic.log.LogConfig
import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class Rec : LogWriter() {
    val errors = mutableListOf<String>()
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (severity == Severity.Error) errors.add(message)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])  // API >= R 才走 requestSystemDelete
class DeleteLauncherLoggingTest {

    private val rec = Rec()

    @Before
    fun setup() {
        AndroidMediaRepository.deleteLauncher = null  // 复现「未注册」
        LogConfig.init(debug = true, extraWriters = listOf(rec))
    }

    @Test
    fun logs_error_when_launcher_missing() = runBlocking {
        val repo = AndroidMediaRepository(ApplicationProvider.getApplicationContext())
        val items = listOf(MediaItem(id = "1", type = MediaType.PHOTO, size = 1L))
        val result = repo.deleteMediaItems(items)
        assertTrue("应返回失败", result.isFailure)
        assertTrue("应打出 ERROR 日志", rec.errors.any { it.contains("deleteLauncher") })
    }
}
```

> 注：`MediaItem` 构造参数以实际定义为准（见 `shared/src/commonMain/.../model/MediaItem.kt`），按需补必填字段。

- [ ] **Step 2: 运行确认失败**

Run: `scripts/test.sh`
Expected: FAIL —— 当前 `requestSystemDelete` 返回 failure 但不打 ERROR 日志，断言 `rec.errors` 失败

- [ ] **Step 3: AndroidMediaRepository 埋点**

顶部加 import：

```kotlin
import com.cleanpic.log.logger
import com.cleanpic.log.redactCount
```

类内加 logger：

```kotlin
    private val log = logger("MediaRepo")
```

在 `requestSystemDelete` 的 launcher 判空处改为（保留原 failure 返回，补 ERROR 日志）：

```kotlin
        val launcher = deleteLauncher
        if (launcher == null) {
            log.e { "deleteLauncher not registered（删除请求数=${redactCount(count)}）" }
            return Result.failure(IllegalStateException("deleteLauncher not registered"))
        }
```

在 `deleteMediaItems`/`deleteMedia` 进入与 `requestSystemDelete` 成功回调处补 INFO（仅数量/类型）：

```kotlin
        log.i { "发起系统删除：数量=${redactCount(items.size)}" }   // deleteMediaItems 开头
```

`queryMedia` 返回前补：

```kotlin
        log.d { "查询媒体 type=${type.name} 命中=${redactCount(items.size)}" }
```

- [ ] **Step 4: 运行确认通过**

Run: `scripts/test.sh`
Expected: PASS（DeleteLauncherLoggingTest 绿）

- [ ] **Step 5: AndroidPermission 埋点**

顶部 `import com.cleanpic.log.logger`，类内 `private val log = logger("Permission")`：
- `requestPhotoPermission` 发起处：`log.i { "请求相册权限" }`
- launcher 为 null 分支：`log.w { "permissionLauncher 未注册，直接 DENIED" }`
- `onPermissionResult`：`log.i { "权限回调 granted=$allGranted" }`（仅布尔）

- [ ] **Step 6: AndroidVideoPlayer 埋点**

先读该文件确认方法名，在 prepare/play/release/错误回调处加 `logger("VideoPlayer")` 的 i/e 日志（仅状态，不含路径/URI）。

- [ ] **Step 7: MainActivity 埋点**

`onCreate` 末尾：`log.i { "MainActivity onCreate：launcher 已注册" }`；`onDestroy`：`log.i { "MainActivity onDestroy：launcher 注销" }`。

- [ ] **Step 8: 编译 + 全单测**

Run: `scripts/build-android.sh && scripts/test.sh`
Expected: BUILD SUCCESSFUL + 全部测试 PASS

- [ ] **Step 9: Commit**

```bash
git add shared/src/androidMain/kotlin/com/cleanpic/media/AndroidMediaRepository.kt shared/src/androidMain/kotlin/com/cleanpic/permission/AndroidPermission.kt shared/src/androidMain/kotlin/com/cleanpic/media/AndroidVideoPlayer.kt androidApp/src/main/java/com/cleanpic/android/MainActivity.kt shared/src/androidUnitTest/kotlin/com/cleanpic/media/DeleteLauncherLoggingTest.kt
git commit -m "feat(log): 原生边界埋点 + deleteLauncher 缺失 ERROR 回归测试"
```

---

## Task 8: ViewModel 流转 + Result.failure 埋点

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/cleanpic/viewmodel/ViewerViewModel.kt`

- [ ] **Step 1: 加 logger**

顶部 `import com.cleanpic.log.logger`、`import com.cleanpic.log.redactCount`；类内 `private val log = logger("ViewerVM")`。

- [ ] **Step 2: loadMedia 埋点**

`loadMedia` 内，`_items.value = ...` 之后加：

```kotlin
        log.i { "loadMedia type=${type.name} 抽取=${redactCount(result.items.size)}" }
```

`if (all.isEmpty())` 分支加：

```kotlin
        log.i { "loadMedia type=${type.name} 相册为空" }
```

- [ ] **Step 3: confirmDelete 埋点（含失败分支）**

`confirmDelete` 内 `val result = repo.deleteMediaItems(items)` 之后加：

```kotlin
        result.onSuccess { log.i { "确认删除成功 count=${redactCount(it)}" } }
            .onFailure { log.e(it) { "确认删除失败" } }
```

- [ ] **Step 4: 编译 + 全单测**

Run: `scripts/build-android.sh && scripts/test.sh`
Expected: BUILD SUCCESSFUL + PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/cleanpic/viewmodel/ViewerViewModel.kt
git commit -m "feat(log): ViewModel 关键流转 + 删除失败埋点"
```

---

## Task 9: 全量验证 + 真机导出冒烟

**Files:** 无（验证）

- [ ] **Step 1: 编译 + 单测全绿**

Run: `scripts/build-android.sh && scripts/test.sh`
Expected: BUILD SUCCESSFUL + 全部 PASS

- [ ] **Step 2: 安装当前 direct/debug 包到真机**（解决记忆中「装陈旧包」坑）

Run: `adb install -r androidApp/build/outputs/apk/direct/debug/刷刷鸭-direct.apk`
Expected: Success

- [ ] **Step 3: E2E 回归**

Run: `~/.maestro/bin/maestro test maestro/flows/`
Expected: 全部通过（日志不改 UI 行为，应无回归）

- [ ] **Step 4: 真机手动冒烟**

1. 进设置页 → 点「导出诊断日志」→ 系统保存框出现，默认文件名形如 `刷刷鸭-日志-20260615-153012.log` → 保存成功
2. 跑一次删除流程 → 若设备 API≥30 且 launcher 正常，应弹系统删除确认而非 "deleteLauncher not registered"
3. 再导出一次日志，打开文件确认：有 MainActivity/MediaRepo/ViewerVM 等 tag 的行，且**不含任何文件名/路径/URI**

- [ ] **Step 5: 升级版本号**

本次为新增功能（MINOR）。按 CLAUDE.md 改：
- `buildSrc/.../CleanPicBuildConfig.kt`：`VERSION_NAME` MINOR+1、`VERSION_CODE`+1
- `shared/src/commonMain/kotlin/com/cleanpic/AppInfo.kt`：`VERSION`

（`scripts/release-direct.sh` 也能自动处理；此处仅在确认发布时执行。）

- [ ] **Step 6: Commit 版本号**

```bash
git add buildSrc/src/main/kotlin/CleanPicBuildConfig.kt shared/src/commonMain/kotlin/com/cleanpic/AppInfo.kt
git commit -m "chore: bump version (日志体系)"
```

- [ ] **Step 7: 询问用户是否发布 Release**（按 CLAUDE.md 强制项）

> "版本号已升级到 x.y.z，是否需要发布 Release？发布后 direct 渠道用户将可自动检测到新版本并升级。"

---

## Self-Review 记录

- **Spec 覆盖**：① Logcat（Task1+4 platformLogWriter）② 文件落地（Task3）③ 导出下载带时间戳（Task5）✅；脱敏（Task2 redact + 各埋点只传数量/类型/布尔）✅；级别策略（Task2 LogConfig）✅；埋点原生边界/ViewModel/Result.failure（Task7/8）✅；测试（Task2/3/4/5/7 单测 + Task9 E2E）✅；FileProvider 已按设计移除（SAF 不需要）✅。
- **类型一致性**：`logger(tag)`、`LogConfig.init(debug, extraWriters)`、`redactCount/redactType`、`LogExportController.export/onRequestExport`、`LogExporter.exportFileName/collect`、`FileLogWriter(dir, maxBytes)`、`ServiceLocator.initialize(..., logWriters)`、`SettingsScreenState.onExportLogs` 在各 Task 间一致。
- **已知待核实点（实现时查证，非占位）**：commonTest 下 Mock 类实际类名（Task4 注）、`MediaItem` 构造签名（Task7 注）、`AndroidVideoPlayer` 方法名（Task7 Step6）。
