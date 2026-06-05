# EP5 浏览体验增强 — 测试场景

|文档状态| 更新 | 2026-05-31 |

覆盖 US-CP-12（中途退出）、US-CP-18（点击全屏查看）、US-CP-19（撤销上一步）、US-CP-21（轮播左右滑动切换前后媒体）。

## AC 级追溯表

| AC 场景 | L1 | L2 | L3 | L4 |
|---------|----|----|----|----|
| 轮播模式中途退出 | — | — | — | E19a |
| 卡片模式中途退出 | — | — | — | E19b |
| 全屏模式中途退出 | — | — | — | E19c |
| 轮播点击照片进全屏 | — | — | — | E22a |
| 卡片点击视频进全屏可播放 | — | — | — | E22b |
| 全屏内删/留后回原模式并推进 | — | — | — | E22c |
| 全屏内返回不改标记 | — | — | — | E22d |
| 全屏上下滑模式点击不重复进全屏 | — | — | — | E22e |
| 决策后可撤销（canUndo） | U-UNDO-01 | — | — | — |
| 撤销回退到上一项并清除标记 | U-UNDO-02 | — | — | E23a |
| 撤销后不可连撤（单步） | U-UNDO-03,06 | — | — | — |
| 未决策时撤销不可用 | U-UNDO-04 | — | — | — |
| 撤销删除决策清除 PENDING_DELETE | U-UNDO-07 | — | — | — |
| loadMedia 重置撤销态 | U-UNDO-05 | — | — | — |
| 卡片模式撤销 | — | — | — | E23b |
| 全屏模式撤销 | — | — | — | E23c |
| 轮播左滑未决策默认保留并前进 | U-NAV-01 | — | — | E24 |
| 轮播左滑保持已有删/留决定 | U-NAV-02,03 | — | — | E24 |
| 轮播右滑回上一项不改状态 | U-NAV-04 | — | — | E24 |
| 轮播首项右滑无副作用 | U-NAV-05 | — | — | E24 |
| 轮播左滑越过最后一项完成本轮 | U-NAV-06 | — | — | E24 |
| 左滑默认保留后撤销复原 | U-NAV-07 | — | — | — |
| 前后往返保留全部决定 | U-NAV-08 | — | — | E24 |
| 右滑回项后按钮可改判 | U-NAV-09 | — | — | — |

## 新增测试锚点（testTag / id）

Step 4 实现时必须新增以下稳定锚点，供 L4 断言：

| testTag | 位置 | 用途 |
|---------|------|------|
| `media_card` | 轮播/卡片模式当前主卡片根容器 | E2E 点击进全屏的目标 |
| `fullscreen_viewer` | FullscreenViewer 根容器 | 区分"全屏已出现/已关闭"（轮播自身也有 exit/delete/keep 按钮，需独立锚点） |
| `undo_button` | 三种模式的撤销按钮 | 点击撤销 + 断言可用性 |

## L1 单元用例（ViewerViewModelTest）

| 编号 | 用例 | 断言要点 |
|------|------|---------|
| U-UNDO-01 | 决策后 canUndo 为真 | `loadMedia` 后 `canUndo=false`；`markKept()`/`markDelete()` 后 `canUndo=true` |
| U-UNDO-02 | undo 回退索引并清除标记 | item0 `markDelete()`（index→1）后 `undo()`：`currentIndex==0`，`items[0].state==PENDING` |
| U-UNDO-03 | undo 后 canUndo 变假 | `markKept()` → `undo()` → `canUndo=false` |
| U-UNDO-04 | 未决策时 undo 无副作用 | 仅 `loadMedia` 后调 `undo()`：`currentIndex==0` 不变，无异常，`canUndo=false` |
| U-UNDO-05 | loadMedia 重置撤销态 | 决策后再 `loadMedia`：`canUndo=false` |
| U-UNDO-06 | 撤销后重新决策可再撤（单步） | 决策→撤销→再决策→`canUndo=true`，再撤回退到该项 |
| U-UNDO-07 | 撤销删除决策清除 PENDING_DELETE | `markDelete()`→`undo()`：`items[0].state==PENDING`，`deletedCount==0` |

US-CP-21 轮播左右滑动导航（`ViewerViewModelTest`，方法前缀 `nav_`）：

| 编号 | 用例 | 断言要点 |
|------|------|---------|
| U-NAV-01 | 左滑未决策默认保留并前进 | `goNext()`：`items[0].state==KEPT`，`currentIndex==1` |
| U-NAV-02 | 左滑保持已有删除决定 | `markDelete()`→`goPrevious()`→`goNext()`：`items[0].state==PENDING_DELETE` 不变 |
| U-NAV-03 | 左滑保持已有保留决定 | `markKept()`→`goPrevious()`→`goNext()`：`items[0].state==KEPT` 不变 |
| U-NAV-04 | 右滑回上一项不改状态 | 决策两项后 `goPrevious()`：`currentIndex==1`，各项状态保持 |
| U-NAV-05 | 首项右滑无副作用 | `loadMedia` 后 `goPrevious()`：`currentIndex==0`，`items[0].state==PENDING` |
| U-NAV-06 | 左滑越过最后一项完成本轮 | 连续 `goNext()` 到末项再 `goNext()`：`isComplete==true` |
| U-NAV-07 | 左滑默认保留后撤销复原 | `goNext()`→`undo()`：`currentIndex==0`，`items[0].state==PENDING` |
| U-NAV-08 | 前后往返保留全部决定 | 删/留后多次 `goPrevious`/`goNext`：各项删/留决定全程不变 |
| U-NAV-09 | 右滑回项后按钮可改判 | `goNext()`→`goPrevious()`→`markDelete()`：`items[0].state==PENDING_DELETE` |

## L4 端到端用例

### E19a / E19b / E19c 中途退出（已有，回归）

| 编号 | 前置 | 操作 | 预期 |
|------|------|------|------|
| E19a | 轮播模式 | 浏览后点 `exit_button` | 回首页，不进结果页 |
| E19b | 卡片模式 | 切卡片→浏览后点 `exit_button` | 回首页，不进结果页 |
| E19c | 全屏模式 | 切全屏→浏览后点 `exit_button` | 回首页，不进结果页 |

### E22a 轮播点击照片进全屏

| 项 | 内容 |
|----|------|
| 前置 | 轮播模式（默认），设备有照片 |
| 操作 | 1. 首页→"随机清理照片" 2. 点击 `media_card` |
| 预期 | `fullscreen_viewer` 可见（照片全屏） |

### E22b 卡片点击视频进全屏可播放

| 项 | 内容 |
|----|------|
| 前置 | 卡片模式，设备有视频 |
| 操作 | 1. 设置→卡片模式→返回 2. "随机清理视频" 3. 点击 `media_card` 4. 在全屏点 `play_button` |
| 预期 | `fullscreen_viewer` 可见；点击播放后 `mute_button` 可见（视频在播） |

### E22c 全屏内删/留后回原模式并推进

| 项 | 内容 |
|----|------|
| 前置 | 轮播模式，照片 |
| 操作 | 1. "随机清理照片" 2. 点 `media_card` 进全屏 3. 点全屏内 `keep_button` |
| 预期 | `fullscreen_viewer` 消失（回到轮播）；轮播 `keep_button` 仍可见（已推进到下一项） |

### E22d 全屏内返回不改标记

| 项 | 内容 |
|----|------|
| 前置 | 轮播模式，照片 |
| 操作 | 1. "随机清理照片" 2. 点 `media_card` 进全屏 3. 点全屏内 `exit_button`（返回） |
| 预期 | `fullscreen_viewer` 消失，回到轮播当前项；轮播 `keep_button`/`delete_button` 可见 |

### E22e 全屏上下滑模式点击不重复进全屏（回归）

| 项 | 内容 |
|----|------|
| 前置 | 全屏上下滑模式，照片 |
| 操作 | 1. 设置→全屏上下滑→返回 2. "随机清理照片" 3. 点击媒体区域 |
| 预期 | 仍正常展示，`delete_button`/`keep_button` 可用（无异常叠层、无崩溃） |

### E23a 轮播模式撤销

| 项 | 内容 |
|----|------|
| 前置 | 轮播模式，照片 |
| 操作 | 1. "随机清理照片" 2. 点 `keep_button`（推进到第2项）3. 点 `undo_button` |
| 预期 | 回到第1项；撤销后浏览页正常（`keep_button` 可见） |

### E23b 卡片模式撤销

| 项 | 内容 |
|----|------|
| 前置 | 卡片模式，照片 |
| 操作 | 1. 切卡片→"随机清理照片" 2. 点 `keep_button` 或滑动一次 3. 点 `undo_button` |
| 预期 | 回到上一项，浏览页正常 |

### E23c 全屏模式撤销

| 项 | 内容 |
|----|------|
| 前置 | 全屏上下滑模式，照片 |
| 操作 | 1. 切全屏→"随机清理照片" 2. 点 `keep_button` 3. 点 `undo_button` |
| 预期 | 回到上一项，全屏正常展示 |

### E24 轮播模式左右滑动切换前后媒体

> 实现：`maestro/flows/direct/carousel-swipe-nav.yaml`。用进度文案 `N / M` 验证前后切换与边界。

| 项 | 内容 |
|----|------|
| 前置 | 轮播相册式模式（默认），每轮 5 张，设备有照片 |
| 操作 1 | "随机清理照片"，确认显示 `1 / 5` |
| 操作 2 | 第 1 张点 `delete_button` → 自动前进，显示 `2 / 5` |
| 操作 3 | 左滑（80%→15%）→ 跳到 `3 / 5`（第 2 张未决策，默认保留） |
| 操作 4 | 右滑（15%→80%）→ 回到 `2 / 5` |
| 操作 5 | 再右滑 → 回到 `1 / 5`（第 1 张仍为已删除决定） |
| 操作 6 | 在第 1 张继续右滑 → 仍 `1 / 5`（首张右滑无副作用） |
| 操作 7 | 连续左滑滑到底，越过最后一张 → 进入结果页 |
| 预期 | 结果页可见"删除"与"保留"统计（第 1 张删除 + 其余默认保留），可"返回首页" |

> 说明：L4 用例需在 `maestro/flows/direct/` 与 `maestro/flows/store/`（撤销/全屏属通用功能，两 flavor 都覆盖）下新建对应 yaml，视频相关放 `video/` 子目录。具体 yaml 在 Step 4 TDD 实现时编写。轮播左右滑动导航（E24）当前仅在 `direct/` 下覆盖。
