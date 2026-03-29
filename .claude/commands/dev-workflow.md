---
description: 研发流程编排器——引导从 User Story 到发布的完整研发生命周期。开始新功能、新需求、恢复开发时使用。
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Agent, Skill
---

<objective>
CleanPic 项目标准研发流程编排器。引导从 User Story 到发布的完整生命周期，每步需人工确认后继续。
与 /tech-doc-system 协作：本 skill 负责"当前在第几步、下一步是什么"；tech-doc-system 负责"具体怎么写文档"。
</objective>

<rules>

1. **强制全程 checklist** — 每次运行必须从 Step 1 走到 Step 6，不得跳过任何步骤
2. **每步人工确认** — 每步展示状态后等待确认，确认后才进入下一步
3. **文档先于代码** — Step 4 TDD 实现前，Steps 1-3 的文档产物必须全部存在
4. **所有 US 必须有 E2E 覆盖** — 每条 User Story 必须有对应的 Maestro 测试流
5. **任何代码修改必须有测试** — 新增功能必须新增测试，bug 修复必须先补测试，禁止无测试的裸提交
6. **不过度触发** — 单行 bug fix、样式微调不需要完整流程，但仍然必须有测试覆盖

</rules>

<scope_check>

**应该触发本流程的场景：**
- "开始新功能"、"新需求"、"我要做 XXX"
- "开发一个新的 XXX 模块"
- "下一步是什么"、"继续开发"

**不应触发的场景（直接修复）：**
- 单行 bug fix、样式微调
- 构建脚本调整
- 依赖版本升级

</scope_check>

<checklist>

## 全流程 Checklist（每次必须执行）

对每一步，先扫描对应路径判断当前状态：

| 状态 | 含义 | 行动 |
|------|------|------|
| ✅ 已完成 | 文档/产物存在且完整 | 展示摘要，询问是否需要更新 |
| ⚠️ 需更新 | 文档存在但与当前需求不匹配 | 触发更新 |
| ❌ 缺失 | 文档/产物不存在 | 从头创建 |

</checklist>

<step_1>

## Step 1／6 — User Story

**目标**：明确用户体验流程和验收标准（不含技术细节）。

**检查路径**：`docs/product/user-stories/cleanpic.md` + `docs/product/user-stories/README.md`

**行动**：
1. 读取现有 US，判断本次需求是新增 US 还是更新已有 US
2. 按 /tech-doc-system 的 US 规范编写：背景(Why) + 用户故事(Who/What/Goal) + AC(Given/When/Then)
3. 绝对禁止在 US 中出现技术细节
4. 写完后同步更新 `docs/product/user-stories/README.md` 索引表

**完成标准**：每条 US 有 Background + User Story + AC，无技术实现细节。

**确认模板**：
```
── Step 1／6 — User Story ──────────────
状态：[✅/⚠️/❌]
产物：docs/product/user-stories/cleanpic.md

需要更新这一步吗？[y/N]
```

</step_1>

<step_2>

## Step 2／6 — 技术方案

**目标**：确立架构边界、术语 SSOT、关键设计决策。

**检查路径**：
- `docs/architecture/overview.md`
- `docs/architecture/domain-model.md`
- `docs/architecture/cleanpic/` 下对应模块文档

**行动**：
1. 读取 domain-model.md 确保术语一致
2. 确定本次需求是否需要新增模块文档（如 `docs/architecture/cleanpic/{topic}.md`）
3. 产物写入 docs/architecture/（遵循 /tech-doc-system 规范）
4. 单文件 <= 400 行，超过则拆分

**完成标准**：架构图或组件说明 + 术语映射已更新。

**确认**：展示方案摘要 → "✅ 技术方案完成，进入 Step 3（测试方案）？"

</step_2>

<step_3>

## Step 3／6 — 测试方案

**目标**：确保所有 US 的 AC 都有对应的测试覆盖。

**检查路径**：
- `docs/testing/strategy.md`
- `docs/testing/scenarios/` 下对应 Epic 文件
- `maestro/flows/` 下 E2E 测试流

**行动**：
1. 读取 strategy.md 和 scenarios，确认本次 US 是否已有测试覆盖
2. 为新增 US 设计测试用例：
   - **单元测试**：ViewModel 逻辑、RandomPicker 等纯逻辑
   - **E2E 测试**：Maestro 测试流覆盖用户操作路径
3. 每条 US 的 AC 必须有至少一个 Maestro 测试流对应
4. 更新 `docs/testing/scenarios/` 对应文件

**完成标准**：
- 每条 US AC 有对应的 Maestro 测试流设计
- scenarios 文件已更新

**确认**：展示测试覆盖矩阵 → "✅ 测试方案完成，进入 Step 4（TDD 实现）？"

</step_3>

<step_4>

## Step 4／6 — TDD 实现

**前置检查**：Steps 1-3 的文档产物必须全部存在，否则回退补齐。

**目标**：Test-first 实现所有功能。

**行动**：
1. 根据 Step 2 技术方案，确定实现顺序
2. 对每个功能点：
   - 先写 Maestro 测试流（E2E）或单元测试
   - 再写实现代码
   - 运行测试验证
3. 使用 `scripts/build-android.sh` 构建
4. 使用 `scripts/test.sh` 运行单元测试
5. 使用 `~/.maestro/bin/maestro test maestro/flows/<flow>.yaml` 运行 E2E

**完成标准**：所有测试通过，功能按 US AC 验收。

**确认**："✅ 实现完成，进入 Step 5（E2E 验证）？"

</step_4>

<step_5>

## Step 5／6 — E2E 全量验证

**目标**：确保新功能没有破坏已有功能。

**行动**：
1. 构建最新 APK：`scripts/build-android.sh`
2. 安装到模拟器：`adb install -r <apk-path>`
3. 运行全部 Maestro 测试流：
   ```
   ~/.maestro/bin/maestro test maestro/flows/
   ```
4. 如有失败，修复后重新验证

**完成标准**：全部 Maestro 测试流通过，零回归。

**确认**："✅ E2E 全量通过，进入 Step 6（提交发布）？"

</step_5>

<step_6>

## Step 6／6 — 提交与发布

**目标**：提交代码，打包发布。

**行动**：
1. 确认所有变更已暂存（文档 + 代码 + 测试）
2. 提交 commit（文档和代码放在同一个 commit 或相关 commits 中）
3. 打包 Release APK：`./gradlew :androidApp:assembleRelease`
4. 同步更新 `docs/TODO.md`（如有新增文档）

**完成标准**：commit 完成，Release APK 构建成功。

**确认**：
```
══════════════════════════════════════
✅ 研发流程 Checklist 全部确认完毕

已确认步骤：
  ✅ Step 1 — User Story
  ✅ Step 2 — 技术方案
  ✅ Step 3 — 测试方案
  ✅ Step 4 — TDD 实现
  ✅ Step 5 — E2E 全量验证
  ✅ Step 6 — 提交与发布
══════════════════════════════════════
```

</step_6>

<flow_diagram>

## 流程图

```
新功能 ───────────────────────────────────────────────────────
  Step 1       Step 2       Step 3        Step 4       Step 5        Step 6
  User Story → Tech     →  Test       →  TDD      →  E2E Full  →  Commit &
               Design      Strategy      Implement    Verify        Release

重进项目 ─── 状态检测 ─── 从断点继续 ────────────────────────────
```

</flow_diagram>

<success_criteria>

流程完成的标志：
- 所有 US 有完整的 Background + User Story + AC
- 技术方案文档存在且与 domain-model.md 术语一致
- 每条 US AC 有对应的 Maestro 测试流
- 所有测试通过（单元 + E2E）
- 代码已提交，Release APK 已构建
- docs/TODO.md 已同步更新

</success_criteria>
