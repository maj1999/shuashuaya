# CleanPic (刷刷鸭) — AI 协作规范

## 项目概述

Kotlin Multiplatform + Compose Multiplatform 的照片/视频随机清理 App。
目标平台：Android 8.0+、iOS 14.0+、HarmonyOS NEXT 5.0+（当前仅 Android 完整实现）。

## 技术栈

- 语言：Kotlin 2.1.21 + Compose Multiplatform 1.7.3
- 图片加载：Coil 3.0.4（Android）
- 视频播放：Media3 ExoPlayer 1.3.1（Android）
- 跨平台隔离：`expect/actual` 模式
- E2E 测试：Maestro（`maestro/flows/*.yaml`）
- 构建：Gradle 8.5 + AGP 8.2.2，compileSdk = 34

## 构建与运行

所有操作通过 `scripts/` 目录下的脚本执行，不要直接使用 gradle/adb 命令：
- `scripts/build-android.sh` — 构建 Android Debug APK
- `scripts/test.sh` — 运行单元测试
- E2E 测试：`~/.maestro/bin/maestro test maestro/flows/<flow>.yaml`

---

## 项目级 Skill

- `/tech-doc-system` — 写文档、审查文档、维护文档索引时使用。包含完整的文档体系规范、写作顺序、审查清单。
- `/dev-workflow` — 开始新功能、新需求、恢复开发时使用。6 步研发流程：US → 技术方案 → 测试方案 → TDD 实现 → E2E 验证 → 提交发布。

---

## 测试纪律（强制执行）

**任何代码修改都必须保证测试覆盖完整。** 这是不可跳过的硬性规则：

1. **新增功能** — 必须同时新增对应的 Maestro E2E 测试流（`maestro/flows/`）和/或单元测试
2. **Bug 修复** — 必须先确认有覆盖该场景的测试，没有则先补测试，再修复代码
3. **UI 变更** — 修改任何 Viewer/Result/Settings 页面后，运行相关 Maestro 测试验证无回归
4. **提交前** — 必须运行 `scripts/test.sh`（单元测试）+ 相关 Maestro 测试流，全部通过才能提交
5. **不允许裸提交** — 禁止在没有运行测试的情况下提交代码变更

### 测试验证流程

每次代码修改完成后，按以下顺序验证：

```
1. scripts/build-android.sh                              → 编译通过
2. scripts/test.sh                                        → 单元测试通过
3. adb install -r <apk> && maestro test maestro/flows/   → E2E 全量通过
```

如果修改范围明确且仅影响特定功能，可以只运行相关的 Maestro 测试流，但提交前必须至少验证编译和单元测试。

### 测试覆盖检查清单

提交前自问：
- [ ] 本次修改涉及的功能，有没有 Maestro 测试覆盖？
- [ ] 如果修改了已有行为，现有测试是否需要同步更新？
- [ ] 如果新增了用户可见的行为，是否新增了对应测试？
- [ ] 测试是否实际运行过并通过？

---

## 技术文档体系（所有 AI Agent 必须遵守）

### 核心原则

1. **文档先于代码** — 每个步骤的文档产物是下一步的输入，不跳步、不倒序
2. **User Story 纯用户视角** — US 只写用户体验和验收标准，绝不混入技术细节（类名、API 路径、数据库字段等）
3. **单文件 <= 400 行** — 超过则以 overview.md 为入口向下拆分，层层导航
4. **术语统一** — `docs/architecture/domain-model.md` 是业务术语与技术术语的唯一映射 SSOT，写作前先查阅，不得另起概念
5. **会话产物必须归并** — 会话中生成的设计方案在开始编码前必须写入 `docs/`

### 文档写作顺序（严格按序）

```
1. User Story          → 明确用户体验和验收标准（纯用户视角）
2. 整体技术方案         → overview.md + domain-model.md，确立术语与架构边界
3. 开发/测试环境设计    → 为 TDD 构建反馈闭环
4. 分模块细节设计       → 每个服务/模块独立文档，<= 400 行
5. 分层测试方案         → 基于 US + 技术方案 + 环境设计生成
6. 并行开发             → 使用测试驱动实现
```

### 目录结构

```
docs/                                    # 所有文档的唯一根目录
├── TODO.md                              # 文档 review 跟踪清单（SSOT）
├── product/                             # 产品层：用户视角
│   ├── prd.md
│   └── user-stories/
│       ├── README.md                    # US 索引表
│       └── cleanpic.md                  # 核心功能 US（按 Epic 组织）
├── architecture/                        # 架构层：系统设计决策
│   ├── overview.md                      # 架构总览（<= 400 行）
│   ├── domain-model.md                  # 术语 SSOT
│   ├── tech-stack.md
│   └── cleanpic/                        # 模块细节
│       ├── overview.md
│       ├── theme-system.md
│       └── native-modules.md
├── testing/                             # 测试层
│   ├── strategy.md
│   └── scenarios/                       # AC 级场景矩阵
└── deployment/                          # 部署层
```

### User Story 规范

- 文件路径：`docs/product/user-stories/{module}.md`
- ID 格式：`US-{模块缩写}-{序号}`（如 `US-CP-01`）
- 每条 US 必须包含：背景(Why) + 用户故事(Who/What/Goal) + AC（Given/When/Then）
- AC 只描述用户看到的结果，禁止包含技术实现细节
- 状态标记：`✅ 已实现` / `⚙️ 进行中` / `待开始`
- 新增 US 后同步更新 `user-stories/README.md` 索引表

### 跨文档维护规约

| 操作 | 必须同步更新 |
|------|------------|
| 新增/删除/重命名文档 | `docs/TODO.md` |
| 新增 User Story | `docs/product/user-stories/README.md` 索引表 |
| 架构组件职责变更 | `docs/architecture/overview.md` 映射表 |
| 新增领域术语 | `docs/architecture/domain-model.md` |

### 会话产物归并规则

AI 会话中生成的设计方案必须在开始编码前归并进 `docs/`：

| 产物类型 | 归并目标 |
|---------|---------|
| 功能方案 / 需求讨论结论 | `docs/product/user-stories/` 对应 Epic |
| 整体技术方案 | `docs/architecture/overview.md` 或对应模块 overview |
| 模块实现计划 | `docs/architecture/cleanpic/{topic}.md` |
| 测试方案 | `docs/testing/strategy.md` 或 `scenarios/` |

### User Story 示例

**错误 — 混入技术细节：**
```
用户发送请求后，通过 ContentResolver 查询 MediaStore.Images.Media
获取 cursor，遍历 _ID 和 DISPLAY_NAME 列...
```

**正确 — 纯用户视角：**
```
背景：用户相册积累了大量照片，需要一种轻松方式快速决定去留。
用户故事：作为手机用户，我希望 App 随机选出一批照片逐一展示。
AC：
- Given 用户相册中有 100+ 张照片
- When 用户点击"随机清理照片"
- Then App 随机选出若干张照片，进入浏览页逐一展示
```
