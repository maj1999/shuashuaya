# EP7 随机算法增强 — 测试场景

> 关联 US: US-CP-22（更聪明的随机）、US-CP-23（重置浏览记录）
> 技术设计: [../../architecture/cleanpic/random-picker.md](../../architecture/cleanpic/random-picker.md)

## AC 级追溯表

| AC 场景 | 单元 | 集成 | E2E |
|---------|------|------|-----|
| 一轮内不重复 | U-RP-01 | — | — |
| 多轮不重复直到看完一圈（洗牌袋） | U-RP-02 | I-VM-01 | E-RP-01 |
| 跨重启记忆 | — | I-VM-02 | — |
| 回首页不丢记忆 | — | I-VM-03 | E-RP-01 |
| 已保留过的沉底 | U-RP-03, U-RP-04 | I-VM-04 | — |
| 新内容同等参与 | U-RP-05 | — | — |
| 删除自愈（残留 id 被忽略） | U-RP-06 | I-VM-05 | — |
| 看完一圈大循环重置 | U-RP-07 | — | — |
| count ≥ 非保留项数（单轮仍不重复） | U-RP-08 | — | — |
| 天数新鲜度（近 N 天后排） | U-RP-09 | — | — |
| 空相册 | U-RP-10 | — | — |
| 存储封顶懒清理 | U-RP-11 | — | — |
| 重置浏览记录（设置项 + 二次确认） | — | I-VM-06 | E-RP-02 |

## 单元测试用例（RandomPicker 纯函数 + PickState）

| ID | 用例 | 预期 |
|----|------|------|
| U-RP-01 | `pick(100 项, count=10, 空 state)` | 返回 10 项且互不重复 |
| U-RP-02 | 同一 state 连续 pick 直到覆盖全库 | 在抽满整库前，任一 id 不被重复抽中（同 cycle 不重复） |
| U-RP-03 | 将部分 id 标 `kept=true` 后 pick | 非保留项足够时，结果不含任何 kept 项 |
| U-RP-04 | 非保留项不足 count、需动用保留项 | 优先非保留，保留项按 `lastSeenMillis` 最久优先补足 |
| U-RP-05 | state 中已有旧记录，live 新增无记录 id | 新 id 与其他未抽项被抽中概率相当（多次采样统计无偏向） |
| U-RP-06 | state.records 含 live 中不存在的 id | 这些幽灵 id 不被抽中、不影响结果 |
| U-RP-07 | 全部 id 均 `kept=true` 后 pick | 触发大循环重置：返回非空、新 state 中 kept 全部清零、cycle+1 |
| U-RP-08 | `pick(8 项, count=10)` | 返回 ≤ 8 项，单轮内不重复 |
| U-RP-09 | 部分 id `lastSeenMillis=now`（今天），有更优新鲜项 | 今天看过的排在新鲜项之后（有更优项时不被选） |
| U-RP-10 | `pick(空 live, …)` | 返回空，state 不变 |
| U-RP-11 | records 数超过封顶阈值 | 懒清理：先剔除不在 live 的 id，仍超则丢最老，结果正确 |

## 集成测试用例（ViewerViewModel + PickStateStore）

| ID | 用例 | 预期 |
|----|------|------|
| I-VM-01 | 30 张照片连续 loadMedia 三轮（每轮 10） | 三轮 30 个 id 互不重复（一圈内不重复） |
| I-VM-02 | loadMedia 一轮 → 重建 VM（共用同一 store，模拟重启）→ loadMedia | 第二段不出现第一段刚看过的 |
| I-VM-03 | loadMedia → 回首页（不再 clearSession）→ loadMedia | 第二轮不重复第一轮（替换旧 `next_round_excludes_shown` 语义并固化"回首页不清空"） |
| I-VM-04 | 一轮全部 markKept → 后续 loadMedia | 已保留项在非保留项耗尽前不再出现 |
| I-VM-05 | 某 id confirmDelete 后 | 该 id 从 records 移除，后续不再出现 |
| I-VM-06 | `clearSession()`（重置浏览记录）后 loadMedia | store 被清空，之前看过/保留过的重新可被抽到 |

> 替换旧用例：原 `ViewerViewModelTest.next_round_excludes_shown`、`clearSession_resets_shown_ids` 基于内存 `_shownIds`，随实现迁移到 I-VM-01/03/06。

## E2E 用例（Maestro）

### E-RP-01 刷一轮回首页再刷不重复

| 项 | 内容 |
|----|------|
| 前置 | 设备有 30+ 张照片，roundCount=10 |
| 操作 | 随机清理照片→记录本轮首张内容→全部保留→结果页"返回首页"→再次"随机清理照片" |
| 预期 | 新一轮不出现刚才那轮看过的内容（断言进度 1/10 且首张不同；或借助测试钩子校验 id 集合不重叠） |

### E-RP-02 重置浏览记录

| 项 | 内容 |
|----|------|
| 前置 | 已清理过若干轮（记忆非空） |
| 操作 | 设置页→"重置浏览记录"→二次确认→开始随机清理 |
| 预期 | 二次确认弹窗出现；确认后清空记忆；取消则保持不变 |

> 说明：E2E 难以直接断言"id 不重复"（无稳定可见标识），以"进度/首图可见性 + 重置入口可达性"为主，精确去重断言由单元/集成层 U-RP-*/I-VM-* 保证。
