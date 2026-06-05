# 浏览页：点击全屏查看 + 撤销上一步

> 对应 User Story：US-CP-18（点击全屏查看媒体）、US-CP-19（撤销上一步重新决策）。
> 归属模块：UI - Viewer（`ui/viewer/`）+ ViewModel（`ViewerViewModel`）。

## 1. 目标与边界

| 功能 | 范围 | 不做 |
|------|------|------|
| 点击全屏（US-CP-18） | 轮播相册式 / 卡片左右滑两种模式下，点击照片或视频进入全屏查看，全屏内可删/留/返回 | 全屏上下滑模式本就全屏，**不**响应点击进全屏 |
| 撤销上一步（US-CP-19） | 三种模式均提供单步撤销，回到上一项并清除其标记重选 | 不做多步撤销；不做跨轮撤销 |

术语遵循 [domain-model.md](../domain-model.md)。新增术语见本文档 §6，需同步回写 domain-model.md。

## 2. 现状回顾

- `ViewerScreen` 根据 `InteractionMode` 分发到 `CarouselMode` / `SwipeCardMode` / `FullscreenMode`。
- 三模式共享 `ViewerViewModel`：`markKept()` / `markDelete()` = 设置当前项 `OperationState` + `currentIndex++`，**无撤销**。
- `FullscreenMode` 已实现"全屏图/视频 + 顶部计数 + 侧边删/留 + 底部信息"，但 UI 与"全屏上下滑"交互模式耦合在一个 Composable 内。
- 媒体点击：`CarouselMode`/`SwipeCardMode` 的卡片当前不可点击进全屏（视频仅有内联播放按钮）。

## 3. 设计概览

```
                    ┌──────────────────────────────┐
                    │  ViewerViewModel              │
                    │  + canUndo: StateFlow<Boolean>│
                    │  + undo()                     │
                    └──────────────┬───────────────┘
                                   │ 共享
        ┌──────────────┬──────────┴──────────┬──────────────┐
        ▼              ▼                     ▼              ▼
  CarouselMode    SwipeCardMode        FullscreenMode   (设置:全屏上下滑)
   点击→overlay    点击→overlay         直接使用
        │              │                     │
        └──────┬───────┘                     │
               ▼                             ▼
        FullscreenViewer  ◄───────────────────┘
        （无状态展示组件：图/视频 + 计数 + 删/留/撤销/返回）
```

核心思路：**抽出一个无状态的 `FullscreenViewer` 展示组件**，三处复用——

1. `FullscreenMode`（全屏上下滑交互模式）作为它的薄封装。
2. `CarouselMode` / `SwipeCardMode` 点击媒体时，以**叠加层（overlay）**形式弹出它。

## 4. 详细设计

### 4.1 ViewModel：单步撤销

在 `ViewerViewModel` 新增：

```kotlin
private val _canUndo = MutableStateFlow(false)
val canUndo: StateFlow<Boolean> = _canUndo
private var lastDecisionIndex: Int? = null   // 最近一次决策的项下标
```

改造决策方法（记录下标 → 设状态 → 前进）：

```kotlin
fun markKept()   { recordDecision(); updateCurrent(OperationState.KEPT);           advance() }
fun markDelete() { recordDecision(); updateCurrent(OperationState.PENDING_DELETE); advance() }

private fun recordDecision() {
    lastDecisionIndex = _currentIndex.value
    _canUndo.value = true
}

fun undo() {
    val idx = lastDecisionIndex ?: return
    val list = _items.value.toMutableList()
    if (idx < list.size) {
        list[idx] = list[idx].copy(state = OperationState.PENDING)
        _items.value = list
    }
    _currentIndex.value = idx
    lastDecisionIndex = null
    _canUndo.value = false
}
```

`loadMedia()` 与 `resetForNextRound()` 末尾重置：`lastDecisionIndex = null; _canUndo.value = false`。

**状态机**：
```
未决策            → canUndo=false（undo() 直接返回）
做出决策(留/删)    → 记录 lastDecisionIndex, canUndo=true, currentIndex++
撤销              → currentIndex 回退到 lastDecisionIndex, 该项→PENDING, canUndo=false
再次决策          → 重新 canUndo=true（单步，不累积）
```

**已知边界**：决策最后一项后 `currentIndex >= size`，`ViewerScreen` 立即导航到结果页，因此"最后一项决策"的撤销在浏览页 UI 内不可达（属可接受范围，US-CP-19 聚焦浏览过程中的反悔）。在结果页可通过既有 `cancelDelete(id)` 取消删除。

### 4.2 FullscreenViewer：抽出的无状态展示组件

新建 `ui/viewer/FullscreenViewer.kt`，把现 `FullscreenMode` 的内部 UI（`FullscreenContent` / `TopBar` / `SideActions` / `BottomInfo` 及静音状态）迁入：

```kotlin
@Composable
fun FullscreenViewer(
    item: ViewerItem,
    theme: ThemeTokens,
    current: Int,
    total: Int,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    onBack: () -> Unit,
)
```

- 图片：`MediaImage(ContentScale.Fit)` 铺满；视频：`VideoPlayerView` 直接播放（全屏即播）。
- 侧边操作区按 `SideActions` 现样式，新增**撤销按钮**（`canUndo=false` 时置灰/降透明并禁用点击）。
- 顶部 `onBack` 文案随调用方语义：交互模式下=「退出」，overlay 下=「返回」。用同一参数，文案由调用方决定（传入 label）或统一用返回图标 + 文案，详见 §4.5。

### 4.3 FullscreenMode：薄封装（全屏上下滑模式）

`FullscreenMode(theme, vm, router)` 收敛为：

```kotlin
val items by vm.items.collectAsState()
val idx by vm.currentIndex.collectAsState()
val canUndo by vm.canUndo.collectAsState()
if (items.isEmpty() || idx >= items.size) return
FullscreenViewer(
    item = items[idx], theme = theme, current = idx + 1, total = items.size,
    canUndo = canUndo, onUndo = vm::undo,
    onDelete = vm::markDelete, onKeep = vm::markKept,
    onBack = { router.popBackStack() },   // 全屏上下滑模式：返回=退出浏览
)
```

### 4.4 Carousel / SwipeCard：点击进全屏（overlay）

两模式各新增本地状态 `var showFullscreen by remember { mutableStateOf(false) }`：

- 给当前主卡片（`MainCard` / 前景 `CardContent`）的根 `Box` 加 `clickable { showFullscreen = true }`。
  - 视频卡片：保留现有内联播放按钮（点播放=内联播放）；点击卡片其它区域=进全屏。两者不冲突（播放按钮在上层独立 `clickable`）。
- 当 `showFullscreen` 为真时，在该模式根 `Box` 顶层叠加：

```kotlin
if (showFullscreen) {
    FullscreenViewer(
        item = items[currentIndex], theme = theme,
        current = currentIndex + 1, total = items.size,
        canUndo = canUndo, onUndo = { vm.undo(); showFullscreen = false },
        onDelete = { vm.markDelete(); showFullscreen = false },
        onKeep = { vm.markKept(); showFullscreen = false },
        onBack = { showFullscreen = false },   // overlay：返回=关闭叠加层
    )
}
```

决策后 `currentIndex` 前进、`showFullscreen` 关闭 → 用户回到原模式看到下一项（满足 US-CP-18 AC）。`onBack` 仅关闭叠加层，不改标记、不改进度。

> 全屏上下滑模式不接入此 overlay（其本身即全屏），满足"仅该模式不响应点击进全屏"。

### 4.5 撤销按钮与图标

- 在 `AppIcons.paths` map 新增条目 `"undo" to "<SVG path>"`（逆时针弯箭头），自动随 `get("undo")` / `allNames` 生效，无需改 `IconPainter`。
- 三处放置：
  - Carousel：`ActionButtons` 行内，删/留按钮旁加 `ThemedActionButton(iconName="undo", testTag="undo_button")`。
  - SwipeCard：顶部提示行旁加一个小撤销按钮。
  - FullscreenViewer：`SideActions` 列内加撤销按钮。
- `canUndo=false` 时：`ThemedActionButton` 增加 `enabled: Boolean = true` 参数控制点击与透明度（禁用态 `alpha 0.4`、不触发 `onClick`）。

## 5. 影响面与改动清单

| 文件 | 改动 |
|------|------|
| `viewmodel/ViewerViewModel.kt` | 新增 `canUndo` / `undo()` / `recordDecision()`，改 `markKept`/`markDelete`，重置点补充 |
| `ui/viewer/FullscreenViewer.kt` | **新建**：从 FullscreenMode 迁入全屏展示 UI，加撤销按钮 |
| `ui/viewer/FullscreenMode.kt` | 收敛为薄封装，调用 FullscreenViewer |
| `ui/viewer/CarouselMode.kt` | 主卡片可点击、叠加 FullscreenViewer、ActionButtons 加撤销 |
| `ui/viewer/SwipeCardMode.kt` | 前景卡片可点击、叠加 FullscreenViewer、提示行加撤销 |
| `ui/viewer/ThemedActionButton.kt` | 增 `enabled` 参数（禁用态样式） |
| `icons/AppIcons.kt` | 在 `paths` map 新增 `"undo"` 图标条目 |

无新增数据层/平台层/`expect/actual` 改动；纯 commonMain UI + ViewModel。

## 6. 新增术语（回写 domain-model.md）

| 业务术语 | 技术术语 | 说明 |
|---------|---------|------|
| 全屏查看 | FullscreenViewer | 无状态全屏展示组件，三种模式复用；区别于交互模式"全屏上下滑(Fullscreen)" |
| 撤销 | Undo / canUndo | 单步撤销上一次删/留决策，回到上一项重选 |
| 最近决策项 | lastDecisionIndex | 最近一次做出删/留决策的媒体项下标，撤销的目标 |

## 7. 测试要点（详见 Step 3 测试方案）

- 单元（`ViewerViewModelTest`）：决策后 `canUndo=true`；`undo()` 回退 index 且状态归 PENDING；undo 后 `canUndo=false`；未决策时 `undo()` 无副作用；`loadMedia` 重置。
- E2E（Maestro）：轮播/滑卡点击媒体出现全屏（`exit_button`/删留按钮可见）→ 删/留后回原模式且进度+1；全屏上下滑模式点击不弹新全屏；三模式撤销按钮可用性与回退效果。
