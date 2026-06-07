# 文档 Review 跟踪清单

## AI 协作规范

- [x] `CLAUDE.md` — Claude Code 项目级协作规范
- [x] `AGENTS.md` — 跨 AI 工具通用协作规范

## 产品层

- [x] `product/prd.md` — 产品需求概述
- [x] `product/user-stories/README.md` — US 索引
- [x] `product/user-stories/cleanpic.md` — 核心功能 US

## 架构层

- [x] `architecture/overview.md` — 系统架构总览
- [x] `architecture/domain-model.md` — 领域模型术语 SSOT
- [x] `architecture/tech-stack.md` — 技术栈选型
- [x] `architecture/cleanpic/overview.md` — 模块总览（数据流/状态/导航）
- [x] `architecture/cleanpic/theme-system.md` — 主题系统设计
- [x] `architecture/cleanpic/native-modules.md` — 原生 Module 设计
- [x] `architecture/cleanpic/auto-update.md` — 自动升级模块设计
- [x] `architecture/cleanpic/auto-update-distribution.md` — 升级功能的分发渠道与编译期隔离
- [x] `architecture/cleanpic/random-picker.md` — 随机选取算法 Shuffle Bag + 持久化浏览记忆（US-CP-22/23）
- [x] `architecture/cleanpic/result-delete-preview.md` — 待删除项全屏预览（US-CP-24）
- [x] `architecture/cleanpic/viewer-fullscreen-undo.md` — 浏览页点击全屏 + 撤销上一步（US-CP-18/19）
- [x] `architecture/cleanpic/viewer-zoom.md` — 全屏查看缩放：双击 + 双指捏合（US-CP-20）
- [x] `architecture/cleanpic/viewer-carousel-swipe-nav.md` — 轮播模式左右滑动切换前后媒体（US-CP-21）
- [x] `architecture/cleanpic/viewer-video-playback.md` — 全屏视频播放：静音统一 + 单播放器 + 拖拽进度条（US-CP-18/25）
- [x] `architecture/cleanpic/viewer-immersive.md` — 全屏沉浸模式：单击隐藏/显示界面（US-CP-26）
- [x] `architecture/cleanpic/auto-update-china-gitee.md` — 国内自动升级分发（Gitee 双端点 + 存量迁移）
- [x] `architecture/cleanpic/cleanup-stats.md` — 清理成果统计：按类型累计 + 统计页 + 首页入口（US-CP-27）

## 部署层

- [x] `deployment/auto-update-setup.md` — 自动更新部署与配置指南

## 测试层

- [x] `testing/strategy.md` — 分层测试策略总览（已更新 US-CP-07/07a 追溯）
- [x] `testing/scenarios/ep1-photo-cleanup.md` — Epic 1 照片清理场景
- [x] `testing/scenarios/ep2-video-cleanup.md` — Epic 2 视频清理场景
- [x] `testing/scenarios/ep3-theme-interaction.md` — Epic 3 主题与交互场景（已更新 v2 主题）
- [x] `testing/scenarios/ep5-browsing-enhancement.md` — EP5 浏览体验增强场景（US-CP-12/18/19/21/26）
- [x] `testing/scenarios/ep6-auto-update.md` — EP6 自动升级场景
- [x] `testing/scenarios/tech-nfr.md` — NFR 性能/兼容性/安全
- [x] `testing/scenarios/ep7-random-enhancement.md` — EP7 随机算法增强场景（US-CP-22/23）
- [x] `testing/scenarios/ep8-cleanup-stats.md` — EP8 清理成果统计场景（US-CP-27）

## 设计 Spec

- [x] `superpowers/specs/2026-04-04-ui-theme-redesign-design.md` — UI 主题重设计 Spec
- [x] `superpowers/specs/2026-06-06-shuffle-bag-random-design.md` — 随机算法优化 Shuffle Bag 设计 Spec
- [x] `superpowers/specs/2026-06-07-cleanup-stats-design.md` — 清理成果统计设计 Spec（含五主题 UI mockup）

## 实现计划

- [x] `superpowers/plans/2026-04-04-ui-theme-redesign-plan-a-infrastructure.md` — Plan A 基础设施
- [x] `superpowers/plans/2026-04-04-ui-theme-redesign-plan-b-warm-theme.md` — Plan B 温暖主题端到端
- [x] `superpowers/plans/2026-04-04-ui-theme-redesign-plan-c-remaining-themes.md` — Plan C 剩余 4 主题
- [x] `superpowers/plans/2026-04-18-update-flavor-split.md` — 升级模块分发渠道隔离
- [x] `superpowers/plans/2026-06-07-cleanup-stats.md` — 清理成果统计（阶段一）实现计划
