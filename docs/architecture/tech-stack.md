# CleanPic — 技术栈选型

|文档状态| 初稿 | 2026-03-28 |

## 选型决策

| 项目 | 选择 | 理由 |
|------|------|------|
| 框架 | KuiklyUI（腾讯） | Kotlin 编译为原生机器码，零桥接开销；Android/iOS/鸿蒙三端均为正式版；QQ 等 20+ 应用、5 亿日活验证 |
| 语言 | Kotlin (KMP) | Compose DSL 编写跨平台 UI + 业务逻辑 |
| 渲染方式 | 原生渲染 | 各平台原生 UI 组件渲染，性能接近原生 |
| 构建工具 | Gradle (KMP) | KuiklyUI 官方构建链 |
| IDE | Android Studio + DevEco Studio | Android/iOS 用 AS，鸿蒙用 DevEco |

## 目标平台与版本

| 平台 | 最低版本 | 理由 |
|------|---------|------|
| Android | 8.0+ (API 26) | 兼顾现代 API (Scoped Storage) 和用户覆盖率 |
| iOS | 14.0+ | PHPicker + Limited Photo Access 支持 |
| HarmonyOS | NEXT 5.0+ | KuiklyUI 鸿蒙正式版最低要求 |

## 候选方案对比（归档）

最终选择 KuiklyUI 前，对比了以下方案：

| 框架 | 鸿蒙成熟度 | 性能 | 相册 API | 最终判断 |
|------|-----------|------|---------|---------|
| KuiklyUI | 正式版 | 原生机器码 | 需自行桥接 | **选用** |
| Lynx.js | 公测 | 优秀(Rust底层) | 需自行桥接 | 鸿蒙未稳定 |
| uni-app x | 接近稳定 | 中上 | 已封装 | 性能不足 |
| Flutter | Beta(社区fork) | 好 | 鸿蒙覆盖40% | fork 维护风险 |
| ArkUI-X | Beta | 中上 | 完整 | 生态太小 |
