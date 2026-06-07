# EP8 清理成果统计 — 测试场景

> 关联 US: US-CP-27（查看清理成果）
> 技术设计: [../../architecture/cleanpic/cleanup-stats.md](../../architecture/cleanpic/cleanup-stats.md)

## AC 级追溯表

| AC 场景 | 单元 | 集成 | E2E |
|---------|------|------|-----|
| 从首页入口进入统计页 | — | — | E-ST-01 |
| 累计大小/文件数/轮次展示 | U-ST-AG-01 | I-ST-01 | E-ST-01 |
| 照片/视频分项（大小·数量·轮次·占比） | U-ST-AG-02 | I-ST-01 | E-ST-01 |
| 设备存储占用展示 | — | — | E-ST-01 |
| 离线声明可见 | — | — | E-ST-01 |
| 语录可见（情境选句） | U-ST-QT-01~03 | — | E-ST-01 |
| 跟随当前主题 | — | — | E-ST-02 |
| 无数据显示 0 不报错 | U-ST-AG-06 | I-ST-03 | — |
| 一轮到达结果页 +1（无论删留） | U-ST-AG-03 | I-ST-02 | — |
| 全保留：轮次+1 量+0 | U-ST-AG-03 | I-ST-02 | — |
| 只记真实删除（系统取消不计） | — | I-ST-04 | — |
| 持久化往返 + 脏数据安全 | U-ST-CD-01~03 | — | — |

## 单元测试用例（commonTest 纯函数）

### StatsCodec（`U-ST-CD-*`）

| ID | 用例 | 预期 |
|----|------|------|
| U-ST-CD-01 | encode→decode 完整快照（含 daily） | 与原对象相等 |
| U-ST-CD-02 | decode(null) / decode("") | 默认空快照 |
| U-ST-CD-03 | decode("乱码") | 默认空快照，不抛异常 |

### StatsAggregator（`U-ST-AG-*`）

| ID | 用例 | 预期 |
|----|------|------|
| U-ST-AG-01 | applyRound 后 | 对应类型 rounds+1、bytes/count 不变、totalRounds 同步、daily 同步 |
| U-ST-AG-02 | applyDeletion(VIDEO, 500, 3) | video.bytes+500、count+3、photo 不变、totalBytes 正确 |
| U-ST-AG-03 | 全保留一轮（只 applyRound） | rounds+1 且 bytes/count=0（轮次/清理量解耦） |
| U-ST-AG-04 | 同日多事件 | 合并为同一 daily 行；跨日则新增行 |
| U-ST-AG-05 | first/last 时间 | firstCleanupAt 取首次、lastCleanupAt 取最近 |
| U-ST-AG-06 | 空快照默认值 | 各项 0，合计 0 |

### CleanupQuotes（`U-ST-QT-*`）

| ID | 用例 | 预期 |
|----|------|------|
| U-ST-QT-01 | totalRounds≤1 | 命中"首次"池 |
| U-ST-QT-02 | isStreak=true | 命中"连续"池 |
| U-ST-QT-03 | 普通 + 同 seed | 命中"日常"池且同 seed 结果稳定 |

## 集成测试用例（ViewerViewModel + InMemoryStatsStore）

| ID | 用例 | 预期 |
|----|------|------|
| I-ST-01 | loadMedia→全部 markDelete→confirmDelete | store 累计 count/bytes 与删除项一致（按类型） |
| I-ST-02 | recordRoundReached 重复调用 | 同一轮只 +1 轮 |
| I-ST-03 | 未清理直接 load | 返回默认空快照 |
| I-ST-04 | confirmDelete 失败（删除返回失败） | 不记录清理量 |

## E2E 用例（Maestro）

### E-ST-01 进入统计页核心元素可见

| 项 | 内容 |
|----|------|
| 前置 | App 已安装（可有/无既往清理记录） |
| 操作 | 首页点击 `stats_button` → 进入清理成果页 |
| 预期 | 可见「清理成果」「累计已清理」「分类构成」「设备存储」「全程离线 · 清理记录仅存本机，不上传」 |

### E-ST-02 主题切换后统计页跟随

| 项 | 内容 |
|----|------|
| 前置 | 任一主题 |
| 操作 | 设置页切换主题 → 返回首页 → 进入清理成果页 |
| 预期 | 统计页外观与所选主题一致（页面可正常进入与展示；视觉一致性以人工/截图核对为主） |

> 说明：E2E 以"入口可达 + 关键文案/区块可见"为主；精确数值与解耦逻辑由单元/集成层 U-ST-*/I-ST-* 保证。
