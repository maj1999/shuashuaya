---
description: 技术文档体系——写文档、审查文档结构、维护文档索引时使用
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

<objective>
CleanPic 项目技术文档体系规范。定义从需求到开发的完整文档写作顺序、各类文档的结构规范。
所有 AI Agent 在本项目中写文档、审查文档、新增功能时必须遵守。
</objective>

<essential_principles>

1. **文档先于代码** — 每个步骤的文档产物是下一步的输入，不跳步、不倒序
2. **User Story 纯用户视角** — US 只写用户体验和验收标准，绝不混入技术细节（类名、API 路径、数据库字段等）
3. **单文件 <= 400 行** — 超过则以 overview.md 为入口向下拆分，层层导航
4. **术语统一** — `docs/architecture/domain-model.md` 是业务术语与技术术语的唯一映射 SSOT，写作前先查阅，不得另起概念
5. **会话产物必须归并** — 会话中生成的设计方案在开始编码前必须写入 `docs/`

</essential_principles>

<writing_order>

文档写作有严格的先后顺序，后续步骤依赖前面步骤的产物：

```
1. User Story          → 明确用户体验流程和验收标准（不含技术细节）
2. 整体技术方案         → overview.md + domain-model.md，确立术语与架构边界
3. 开发/测试环境设计    → 为 AI Agent TDD 构建反馈闭环
4. 分模块细节设计       → 每个服务/模块独立文档，<= 400 行
5. 分层测试方案（L2-L4）→ 基于 US + 技术方案 + 环境设计生成
6. 并行 TDD 开发        → 使用测试驱动实现
```

</writing_order>

<directory_structure>

所有文档必须位于项目根目录的 `docs/` 下：

```
docs/
├── TODO.md                              # 文档 review 跟踪清单（SSOT）
├── product/                             # 产品层：用户视角
│   ├── prd.md                           # 产品需求概述
│   └── user-stories/
│       ├── README.md                    # US 索引表
│       └── cleanpic.md                  # 核心功能 US（按 Epic 组织）
├── architecture/                        # 架构层：系统设计决策
│   ├── overview.md                      # 系统架构总览（<= 400 行）
│   ├── domain-model.md                  # 术语 SSOT（必读！）
│   ├── tech-stack.md                    # 技术栈选型
│   └── cleanpic/                        # 模块细节
│       ├── overview.md                  # 模块总览
│       ├── theme-system.md              # 主题系统
│       └── native-modules.md            # 原生模块
├── testing/                             # 测试层
│   ├── strategy.md                      # 分层测试策略
│   └── scenarios/                       # AC 级场景矩阵
│       ├── ep1-photo-cleanup.md
│       ├── ep2-video-cleanup.md
│       ├── ep3-theme-interaction.md
│       └── tech-nfr.md
└── deployment/                          # 部署层
```

</directory_structure>

<process>

## 执行流程

当你被要求写文档、审查文档、或开始新功能时，按以下步骤执行：

### Step 0: 读取术语表

在任何文档工作前，先读取 `docs/architecture/domain-model.md`，确保术语一致。

### Step 1: 判断当前任务类型

- **写 User Story** → 执行 US 规范
- **写技术方案** → 执行架构文档规范
- **写测试方案** → 执行测试文档规范
- **审查文档** → 执行审查清单
- **新增功能** → 从 Step 1（US）开始，按写作顺序执行

### Step 2: 写 User Story（如需要）

文件路径：`docs/product/user-stories/{module}.md`

规范：
- ID 格式：`US-{模块缩写}-{序号}`（如 `US-CP-01`）
- 每条 US 必须包含：**背景(Why)** + **用户故事(Who/What/Goal)** + **AC（Given/When/Then）**
- **绝对禁止**在 US 中出现：类名、方法名、API 路径、数据库字段、协议细节
- 状态标记：`✅ 已实现` / `⚙️ 进行中` / `待开始`
- 写完后同步更新 `docs/product/user-stories/README.md` 索引表

正确示例：
```markdown
### US-CP-01 随机浏览照片
**背景**：用户相册积累了大量照片，需要一种轻松方式快速决定去留。
**用户故事**：作为手机用户，我希望 App 随机选出一批照片逐一展示。
**AC**：
- Given 用户相册中有 100+ 张照片
- When 用户点击"随机清理照片"
- Then App 随机选出若干张照片，进入浏览页逐一展示
```

错误示例（混入技术细节）：
```markdown
用户发送请求后，通过 ContentResolver 查询 MediaStore.Images.Media
获取 cursor，遍历 _ID 和 DISPLAY_NAME 列...
```

### Step 3: 写架构文档（如需要）

- 文件路径：`docs/architecture/overview.md` 或 `docs/architecture/cleanpic/{topic}.md`
- 单文件 <= 400 行，超过则拆分为 overview + 子文件
- 文件开头用 `|文档状态|` 记录 review 时间
- Mermaid 图节点换行用 `<br/>`，禁止用 `\n`
- 所有术语必须与 `domain-model.md` 对齐

### Step 4: 写测试方案（如需要）

- 总览：`docs/testing/strategy.md`
- 场景：`docs/testing/scenarios/` 下按 Epic 或模块拆分
- 场景文件必须追溯到 US 的 AC

### Step 5: 完成后更新索引

任何文档变更后，必须同步更新：

| 操作 | 必须更新 |
|------|---------|
| 新增/删除/重命名文档 | `docs/TODO.md` |
| 新增 User Story | `docs/product/user-stories/README.md` |
| 架构组件职责变更 | `docs/architecture/overview.md` |
| 新增领域术语 | `docs/architecture/domain-model.md` |

</process>

<review_checklist>

## 文档审查清单

审查文档时，逐项检查：

- [ ] 单文件是否超过 400 行（超过则必须拆分）
- [ ] 术语是否与 `domain-model.md` 一致
- [ ] User Story 是否混入了技术实现细节
- [ ] 跨文档引用路径是否正确（相对路径）
- [ ] US 状态标记是否与代码实现同步
- [ ] `docs/TODO.md` 是否包含所有文档条目
- [ ] `user-stories/README.md` 索引是否完整
- [ ] 新增术语是否已登记到 `domain-model.md`

</review_checklist>

<session_artifact_merge>

## 会话产物归并规则

AI 会话中生成的设计方案必须在开始编码前归并进 `docs/`：

| 产物类型 | 归并目标 |
|---------|---------|
| 功能方案 / 需求讨论结论 | `docs/product/user-stories/` 对应 Epic |
| 整体技术方案 | `docs/architecture/overview.md` 或对应模块 overview |
| 模块实现计划 | `docs/architecture/cleanpic/{topic}.md` |
| 测试方案 | `docs/testing/strategy.md` 或 `scenarios/` |

归并时机：方案确认后、开始编码前。

</session_artifact_merge>

<success_criteria>

文档工作完成的标志：
- 所有新增文档已在 `docs/TODO.md` 中登记
- 术语与 `domain-model.md` 一致
- 单文件 <= 400 行
- US 中无技术实现细节
- 跨文档引用路径正确
- 索引表（README.md、TODO.md）已同步

</success_criteria>
