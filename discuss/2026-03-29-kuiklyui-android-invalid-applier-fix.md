# 技术调研: KuiklyUI Android Invalid Applier 崩溃修复

## 问题摘要

App 在 Android 模拟器上启动即崩溃，报错 `java.lang.IllegalStateException: Invalid applier`。
根因：`MainActivity` 使用标准 Jetpack Compose 的 `setContent` 创建 composition（使用 `UiApplier`），
但所有 UI 代码使用 `com.tencent.kuikly.compose.*` 组件（需要 `KuiklyApplier`），两者不兼容。

## 需求约束

- 项目使用 Kotlin Multiplatform，目标平台：Android / iOS / HarmonyOS
- 已配置 Compose Multiplatform 插件 (`org.jetbrains.compose` + `kotlin("plugin.compose")`)
- KuiklyUI 版本 2.4.0-2.0.21
- `core-render-android` 依赖已声明但未使用

## 方案对比

### 方案 1: 完整 KuiklyUI Android 渲染管线

**原理:** 使用 KuiklyUI 官方 Android 渲染引擎 (`core-render-android`)，通过 `KuiklyRenderView` + `KuiklyRenderViewBaseDelegator` + XML 布局 承载 KuiklyUI 页面。

**所需组件:**
- `core-render-android` 依赖
- XML 布局文件（容器 View）
- 多个必须 Adapter：Image、Log、Font、Color、Router、Thread 等
- `ContextCodeHandler` + `PagerManager` 页面注册
- `ComposeContainer` 作为 Pager 注册

**优点:** 完整保留 KuiklyUI 跨平台一致性

**缺点:**
- 复杂度极高（需实现 6+ 个 Adapter）
- `core-render-android` 未在项目依赖中，可能存在版本兼容问题
- 偏离标准 Android Compose 生态

**复杂度:** L

### 方案 2: 切换到标准 Compose Multiplatform（推荐）

**原理:** 将所有 `com.tencent.kuikly.compose.*` 导入替换为标准 Compose Multiplatform 等价物（`androidx.compose.*`）。项目已配置 CMP 插件，CMP 会自动为各平台提供正确实现。

**所需改动:**
- 批量替换 import：`com.tencent.kuikly.compose.` -> 对应的 `androidx.compose.` 包
- 保留 `com.tencent.kuikly.core.*`（非 UI 部分，如 annotations）
- 可能需要少量 API 差异适配

**优点:**
- 改动最小，API 几乎完全对齐
- 立即可用，无需额外依赖
- 符合标准 Compose Multiplatform 生态
- Android/iOS 都能正常渲染

**缺点:**
- 放弃 KuiklyUI 的渲染优化（如果有的话）
- 后续如需 HarmonyOS 支持，可能需要重新引入 KuiklyUI

**复杂度:** S

### 方案 3: expect/actual 分平台 UI

**原理:** 用 `expect`/`actual` 模式，Android 用标准 Compose，iOS/Harmony 用 KuiklyUI。

**优点:** 各平台使用最优方案
**缺点:** 维护两套 UI 代码，违背 KMP 初衷

**复杂度:** L

## 对比表

| 维度         | 方案 1 (KuiklyUI 渲染管线) | 方案 2 (标准 CMP) | 方案 3 (分平台 UI) |
|-------------|--------------------------|------------------|------------------|
| 复杂度      | L                        | S                | L                |
| 即时可用    | 否                       | 是               | 否               |
| 维护成本    | 高                       | 低               | 高               |
| 跨平台一致性 | 最佳                     | 良好             | 一般             |

## 推荐方案

**方案 2: 切换到标准 Compose Multiplatform。**

理由：项目已配置 CMP 插件，KuiklyUI Compose API 与标准 Compose API 高度一致，只需批量替换 import 即可修复崩溃。这是最小改动、最快见效的方案。

## 实施步骤

1. 将所有 `.kt` 文件中的 `com.tencent.kuikly.compose.` 替换为对应的 `androidx.compose.` 包
2. 确认 `shared/build.gradle.kts` 中 CMP 依赖正确（`compose.foundation`、`compose.material3` 等）
3. 移除或注释 KuiklyUI compose 依赖
4. 构建验证 + 模拟器测试

## 来源

- [KuiklyUI GitHub](https://github.com/Tencent-TDS/KuiklyUI)
- KuiklyUI compose AAR 反编译分析（`compose-release.aar` 中的 `KuiklyApplier.class`、`ComposeContainer.kt`）
- KuiklyUI 官方 Android Demo（`KuiklyRenderActivity.kt`）
