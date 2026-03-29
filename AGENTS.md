# CleanPic — AI Agent 协作规范

> 本文件定义所有 AI Agent 在本项目中必须遵守的文档体系和协作规则。
> 无论你是 Claude、Gemini、GPT、Copilot 还是其他 AI 工具，都必须遵守以下规范。

## 语言

所有文档、注释、对话一律使用简体中文。技术术语和代码标识符保持原文。

## 项目信息

- 类型：Kotlin Multiplatform + Compose Multiplatform 移动 App
- 目标：照片/视频随机清理工具
- 平台：Android（已实现）、iOS（stub）、HarmonyOS（stub）
- 构建脚本：`scripts/*.sh`，不要直接执行 gradle/npm/adb 等裸命令

## 代码规范

- 静态语言（Kotlin/Java/Go/Rust）：单文件 <= 400 行
- 动态语言（Python/JS/TS）：单文件 <= 300 行
- 每层文件夹中的文件 <= 8 个，超过需拆分为子文件夹
- 数据结构尽可能定义为强类型
- 跨平台代码使用 `expect/actual` 模式隔离

## 文档体系（必须遵守）

### 1. 文档先于代码

任何功能的开发流程必须按以下顺序，不跳步：

```
User Story → 整体技术方案 → 环境设计 → 模块细节 → 测试方案 → 编码实现
```

### 2. 目录结构

所有文档位于 `docs/` 下，结构固定：

```
docs/
├── TODO.md                     # 文档 review 跟踪清单
├── product/                    # 产品层（用户视角）
│   ├── prd.md
│   └── user-stories/
│       ├── README.md           # US 索引表
│       └── {module}.md         # 按模块拆分的 US
├── architecture/               # 架构层（技术视角）
│   ├── overview.md             # 系统架构总览
│   ├── domain-model.md         # 术语映射 SSOT（必读）
│   ├── tech-stack.md
│   └── {module}/               # 模块细节
│       ├── overview.md
│       └── {topic}.md
├── testing/                    # 测试层
│   ├── strategy.md
│   └── scenarios/
└── deployment/                 # 部署层
```

### 3. 术语统一

`docs/architecture/domain-model.md` 是业务术语与技术术语的唯一权威来源。
- 写文档或代码前先查阅此文件
- 不得自创与已定义术语含义相同但命名不同的概念
- 需要新术语时，先在此文件中登记

### 4. User Story 规范

- 路径：`docs/product/user-stories/{module}.md`
- ID：`US-{模块缩写}-{序号}`
- 必须包含：背景(Why) + 用户故事(Who/What/Goal) + 验收标准(Given/When/Then)
- **绝对禁止在 US 中出现技术实现细节**（类名、API 路径、数据库字段等）
- 状态：`✅ 已实现` / `⚙️ 进行中` / `待开始`

### 5. 单文件行数限制

- 文档文件：<= 400 行。超过时拆分为 `overview.md` + 子文件
- 代码文件：同上述代码规范

### 6. 变更时必须同步更新

| 操作 | 同步更新 |
|------|---------|
| 新增/删除文档 | `docs/TODO.md` |
| 新增 User Story | `docs/product/user-stories/README.md` |
| 架构变更 | `docs/architecture/overview.md` |
| 新增术语 | `docs/architecture/domain-model.md` |

### 7. 方案归并

AI 会话中产出的设计方案（brainstorm 结论、技术方案、实现计划）在开始编码前必须写入 `docs/` 对应位置。不允许方案只存在于会话上下文中。

## 研发流程（开发新功能时必须遵守）

开始新功能时，必须按以下 6 步顺序执行，每步确认后才能进入下一步：

```
Step 1: User Story     → 明确用户体验和验收标准（纯用户视角，无技术细节）
Step 2: 技术方案        → 架构设计、术语确认、组件职责
Step 3: 测试方案        → 每条 US AC 必须有对应 Maestro E2E 测试
Step 4: TDD 实现        → 先写测试再写实现（Steps 1-3 文档必须先完成）
Step 5: E2E 全量验证    → 运行全部 Maestro 测试确认零回归
Step 6: 提交与发布      → commit + Release APK
```

核心规则：
- **文档先于代码** — Step 4 开始编码前，Steps 1-3 文档必须全部存在
- **US 必须有 E2E** — 每条 User Story 必须有对应的 Maestro 测试流
- **不过度触发** — 单行 bug fix、样式微调不需要完整流程

详见 `.claude/commands/dev-workflow.md`。

## 测试纪律（强制执行）

**任何代码修改都必须保证测试覆盖完整。** 不可跳过：

1. **新增功能** — 必须同时新增 Maestro E2E 测试流和/或单元测试
2. **Bug 修复** — 先确认有覆盖该场景的测试，没有则先补测试再修复
3. **UI 变更** — 修改页面后运行相关 Maestro 测试验证无回归
4. **提交前** — 必须运行测试，全部通过才能提交。禁止裸提交

验证顺序：
```
1. scripts/build-android.sh              → 编译通过
2. scripts/test.sh                        → 单元测试通过
3. maestro test maestro/flows/           → E2E 通过
```

提交前自问：
- 本次修改涉及的功能，有没有测试覆盖？
- 现有测试是否需要同步更新？
- 新增了用户可见行为，是否新增了测试？
- 测试是否实际运行过并通过？

## 快速上手

1. 阅读 `docs/architecture/domain-model.md` 了解术语
2. 阅读 `docs/architecture/overview.md` 了解架构
3. 阅读 `docs/product/user-stories/cleanpic.md` 了解功能需求
4. 阅读 `docs/testing/strategy.md` 了解测试策略
5. 运行 `scripts/build-android.sh` 验证环境
