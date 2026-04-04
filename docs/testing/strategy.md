# CleanPic — 分层测试策略

|文档状态| 初稿 | 2026-03-28 |

## 一、测试层级总览表

| 层级 | 用例数 | 测试目标 | 真实依赖 | Mock 依赖 | 真实 Infra | Mock Infra | 执行时机 | 耗时 | 代码位置 |
|------|--------|---------|---------|-----------|-----------|-----------|---------|------|---------|
| L1 单元 | 14 | 共享层业务逻辑正确性 | 无 | MediaRepository, AppSettings | 无 | 内存 Mock | 每次提交 | <10s | shared/test/ |
| L2 组件 | 18 | UI 组件交互与状态 | ThemeManager, RandomPicker | MediaRepository | 无 | 模拟媒体数据 | 每次提交 | <30s | shared/test/ui/ |
| L3 集成 | 16 | 平台原生 Module 真机验证 | 系统相册, 系统权限 | 无 | 真机 | 无 | 每日/MR | <5min | {platform}/test/ |
| L4 E2E | 21 | 完整用户流程端到端 | 全部真实 | 无 | 真机 | 无 | 发版前 | <15min | e2e/ |

## 二、分层逻辑

| 层级 | 解决什么问题 | 为什么上层不够 |
|------|------------|--------------|
| L1 单元 | RandomPicker 随机/去重逻辑、ThemeManager 切换、AppSettings 读写 | 这些是纯逻辑，不需要 UI 或真机 |
| L2 组件 | 各页面状态转换、交互模式手势响应、主题切换视觉 | L1 不测 UI 层 |
| L3 集成 | 各平台 MediaModule/权限/视频播放器是否正常工作 | L2 使用 Mock 数据，无法验证真实系统 API |
| L4 E2E | 完整用户流程：首页→浏览→结果→删除→再来一轮 | L3 按模块测试，不覆盖跨页面流程和批量删除弹窗 |

## 三、Mock 基础设施

```
shared/test/
├── mock/
│   ├── MockMediaRepository.kt   — 内存中的假媒体列表
│   ├── MockAppSettings.kt       — 内存中的假偏好存储
│   └── TestMediaFactory.kt      — 生成测试用 MediaItem
```

| 层级 | MediaRepository | AppSettings | 系统权限 | 视频播放 |
|------|---------------|------------|---------|---------|
| L1 | Mock | Mock | N/A | N/A |
| L2 | Mock | Mock | Mock（始终授权） | Mock（静态帧） |
| L3 | 真实 | 真实 | 真实 | 真实 |
| L4 | 真实 | 真实 | 真实 | 真实 |

## 四、需求追溯矩阵

| User Story | L1 | L2 | L3 | L4 |
|-----------|----|----|----|----|
| US-CP-01 随机浏览照片 | U01-U07 | — | — | E01,E02 |
| US-CP-02 保留或删除 | — | E10-E12 | — | E01,E03-E06 |
| US-CP-03 确认删除与结果 | U-DEL-01,U-DEL-02 | — | — | E01,E03-E06 |
| US-CP-04 随机浏览视频 | U01-U07 | — | — | E07 |
| US-CP-05 预览视频（含静音） | — | — | L3-视频 | E08a,E08b,E09 |
| US-CP-06 确认删除视频 | — | — | — | E07 |
| US-CP-07 切换主题（v2: 5新主题+独立布局） | U08-U11,U15 | E14-E16,E20-E21 | — | E16,E20-E21 |
| US-CP-07a 矢量图标体验 | — | E20 | — | E20 |
| US-CP-08 切换交互模式 | — | E10-E13 | — | E13 |
| US-CP-09 动效与减少动效 | — | — | — | — |
| US-CP-10 每轮数量 | U12-U14 | E17 | — | E17 |
| US-CP-11 权限引导 | — | — | P01-P06 | P01-P06 |
| US-CP-12 中途退出浏览 | — | — | — | E19a,E19b,E19c |
| NFR-01~10 | — | — | F01-F08 | F01-F08 |

## 五、质量门禁

| 门禁 | 检查点 | 标准 |
|------|--------|------|
| 提交门禁 | L1 + L2 全部通过 | 100% pass，0 个 failure |
| MR 门禁 | L1 + L2 + L3 全部通过 | 100% pass |
| 发版门禁 | L1-L4 全部通过 | 100% pass；NFR 指标全部达标 |
| 覆盖率门禁 | shared/ 层代码覆盖率 | >= 80% 行覆盖率 |

## 六、场景文件索引

| 文件 | 覆盖 Epic | 用例编号 |
|------|----------|---------|
| [scenarios/ep1-photo-cleanup.md](scenarios/ep1-photo-cleanup.md) | EP1 照片清理 | E01-E06, B01-B10 |
| [scenarios/ep2-video-cleanup.md](scenarios/ep2-video-cleanup.md) | EP2 视频清理 | E07-E09 |
| [scenarios/ep3-theme-interaction.md](scenarios/ep3-theme-interaction.md) | EP3 主题与交互 | E10-E18 |
| [scenarios/ep5-browsing-enhancement.md](scenarios/ep5-browsing-enhancement.md) | EP5 浏览体验增强 | E19a-E19c |
| [scenarios/tech-nfr.md](scenarios/tech-nfr.md) | NFR + 权限 + 兼容性 | P01-P06, F01-F08, 兼容性矩阵 |
