# EP8 清理成果统计 — 测试场景

> 关联 US: US-CP-27（查看清理成果）、US-CP-28（结果页即时成果反馈）
> 技术设计: [../../architecture/cleanpic/cleanup-stats.md](../../architecture/cleanpic/cleanup-stats.md)（阶段二/三见 §12）

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
| **连续清理天数（今天空挡顺延昨天/断档清零）** | U-ST-SK-01~07 | — | E-ST-01 |
| **里程碑徽章（达成点亮/未达成灰显/概览）** | U-ST-ML-01~05 | — | E-ST-01 |
| **照片/视频构成环形图** | U-ST-AG-02 | — | E-ST-01 |
| **月度回顾（按自然月聚合、最近月在前）** | U-ST-MR-01~04 | — | E-ST-01 |
| **日期运算（epochDay/yearMonth 闰年·跨月·非法）** | U-ST-DM-01~06 | — | — |
| **结果页本次/累计成果注脚（US-CP-28）** | — | I-ST-01 | E-ST-03 |
| **结果页全保留时累计不变（诚实性）** | — | I-ST-02, I-ST-04 | — |

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

### DateMath（`U-ST-DM-*`，阶段二/三日期底座）

| ID | 用例 | 预期 |
|----|------|------|
| U-ST-DM-01 | epochDay("1970-01-01") | 0（原点） |
| U-ST-DM-02 | 相邻两天 epochDay | 差值恰为 1 |
| U-ST-DM-03 | 跨月（月末→次月初） | 连续、差 1 |
| U-ST-DM-04 | 闰年 2-29 | 合法且连续 |
| U-ST-DM-05 | 非法格式（乱码/越界月日） | 返回 null，不抛 |
| U-ST-DM-06 | yearMonth 个位月补零 | "2026-06" |

### StatsStreak（`U-ST-SK-*`，连续清理天数）

| ID | 用例 | 预期 |
|----|------|------|
| U-ST-SK-01 | 空 daily | 0 |
| U-ST-SK-02 | 仅今天 | 1 |
| U-ST-SK-03 | 含今天的连续 3 天 | 3 |
| U-ST-SK-04 | 中间断一天 | 只数最近连续段 |
| U-ST-SK-05 | 今天空挡、昨天有 | 从昨天起算，不清零 |
| U-ST-SK-06 | 最近一次在前天（昨天也空） | 0（陈旧 streak） |
| U-ST-SK-07 | 连续区间跨自然月 | 正确跨月计数 |

### Milestones（`U-ST-ML-*`，里程碑徽章）

| ID | 用例 | 预期 |
|----|------|------|
| U-ST-ML-01 | 空 lifetime | 全部未达成 |
| U-ST-ML-02 | 累计达 1 GB | 对应徽章 achieved=true |
| U-ST-ML-03 | 文件数 / 轮次维度独立 | 各维度互不影响达成判定 |
| U-ST-ML-04 | evaluate 顺序 | 固定稳定（便于渲染/断言） |
| U-ST-ML-05 | 阈值差 1 未到 | 未达成（边界严格 >=） |

### MonthlyReview（`U-ST-MR-*`，月度回顾）

| ID | 用例 | 预期 |
|----|------|------|
| U-ST-MR-01 | 空 daily | 无月份 |
| U-ST-MR-02 | 同月多天 | 合并为一条月汇总（照片/视频各自累加） |
| U-ST-MR-03 | 跨多月 | 分条且按月份倒序（最近在前） |
| U-ST-MR-04 | forMonth 命中/未命中 | 命中返回该月、未命中返回 null |

## 集成测试用例（ViewerViewModel + InMemoryStatsStore）

| ID | 用例 | 预期 |
|----|------|------|
| I-ST-01 | loadMedia→全部 markDelete→confirmDelete | store 累计 count/bytes 与删除项一致（按类型）；结果页注脚据此读累计 |
| I-ST-02 | recordRoundReached 重复调用 | 同一轮只 +1 轮 |
| I-ST-03 | 未清理直接 load | 返回默认空快照 |
| I-ST-04 | confirmDelete 失败/系统弹窗取消（repo.shouldFail） | 不记录清理量（诚实性红线，对应 `cancelled_or_failed_delete_does_not_record_amount`） |

## E2E 用例（Maestro）

### E-ST-01 进入统计页核心元素可见（含阶段二/三区块）

| 项 | 内容 |
|----|------|
| 前置 | App 已安装（可有/无既往清理记录） |
| 操作 | 首页点击 `stats_button` → 进入清理成果页 |
| 预期 | 可见「累计已清理」「分类构成」「里程碑」「月度回顾」「设备存储」「全程离线 · 清理记录仅存本机，不上传」（对应 `maestro/flows/direct/cleanup-stats.yaml`） |

### E-ST-02 主题切换后统计页跟随

| 项 | 内容 |
|----|------|
| 前置 | 任一主题 |
| 操作 | 设置页切换主题 → 返回首页 → 进入清理成果页 |
| 预期 | 统计页外观与所选主题一致（页面可正常进入与展示；视觉一致性以人工/截图核对为主） |

### E-ST-03 结果页即时成果反馈（US-CP-28）

| 项 | 内容 |
|----|------|
| 前置 | App 已安装、有可清理媒体 |
| 操作 | 完成一轮清理到达结果页「完成」态 |
| 预期 | 可见「本次清理」「累计已清理」（数字滚动到位后断言文案，对应 `maestro/flows/direct/result-cumulative.yaml`） |

> 说明：E2E 以"入口可达 + 关键文案/区块可见"为主；精确数值、连续天数/里程碑/月度聚合逻辑由单元层 U-ST-*（SK/ML/MR/DM）保证，删除入账诚实性由 I-ST-* 保证。
