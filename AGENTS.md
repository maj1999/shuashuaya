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

## 测试规范

- 单元测试：`scripts/test.sh`
- E2E 测试：Maestro，测试流文件位于 `maestro/flows/*.yaml`
- 新增功能必须有对应的 E2E 测试
- 修复 bug 必须有回归测试覆盖

## 快速上手

1. 阅读 `docs/architecture/domain-model.md` 了解术语
2. 阅读 `docs/architecture/overview.md` 了解架构
3. 阅读 `docs/product/user-stories/cleanpic.md` 了解功能需求
4. 阅读 `docs/testing/strategy.md` 了解测试策略
5. 运行 `scripts/build-android.sh` 验证环境
