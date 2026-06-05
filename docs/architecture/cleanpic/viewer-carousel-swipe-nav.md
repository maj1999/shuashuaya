# 浏览页：轮播模式左右滑动切换前后媒体

> 对应 User Story：US-CP-21（轮播模式左右滑动切换前后媒体）。
> 归属模块：UI - Viewer（`ui/viewer/CarouselMode.kt`）+ ViewModel（`ViewerViewModel`）。

|文档状态| 初稿 | 2026-06-06 |

## 1. 目标与边界

| 功能 | 范围 | 不做 |
|------|------|------|
| 左右滑动导航（US-CP-21） | 仅"轮播相册式"模式：左滑切下一个、右滑切上一个，前后自由翻看并保留已做决定 | 不改"卡片左右滑"模式（其滑动本就是删/留决策语义）；不改"全屏上下滑"模式 |

术语遵循 [domain-model.md](../domain-model.md)。新增术语见本文档 §6，需同步回写 domain-model.md。

## 2. 现状回顾

- `ViewerScreen` 根据 `InteractionMode` 分发到 `CarouselMode` / `SwipeCardMode` / `FullscreenMode`，三模式共享 `ViewerViewModel`。
- 决策推进：`markKept()` / `markDelete()` = 设当前项 `OperationState` + `advance()`（`currentIndex++`）。
- 撤销（US-CP-19）：`undo()` 基于位置——只要 `currentIndex > 0` 即可连续回退，把上一项恢复为 `PENDING` 并 `currentIndex--`；`canUndo = currentIndex > 0`。
- `CarouselMode` 此前已有 `detectHorizontalDragGestures`，但 `onDragEnd` 仅把 `offsetX` 复位（纯视觉回弹），**不切换媒体**——左右滑无导航效果。

## 3. 设计概览

核心：给共享的 `ViewerViewModel` 新增两个**纯位移**方法，由 `CarouselMode` 的水平拖动手势按阈值触发。

```
        CarouselMode（水平拖动手势）
              │  offsetX 累积位移
              ▼
   onDragEnd 判定（阈值 CAROUSEL_SWIPE_THRESHOLD_DP=72dp）
      ├─ 左滑越过阈值 ──► vm.goNext()    （离开项未决策→默认保留；已决策→保持）
      ├─ 右滑越过阈值且非首张 ──► vm.goPrevious()  （仅移动位置，决定不变）
      └─ 未达阈值 / 已到首张 ──► offsetX = 0f（回弹复位）
```

`goNext` / `goPrevious` 与既有 `undo()` 共用 `_currentIndex` 与 `refreshCanUndo()`，因此默认保留后撤销可直接复用 US-CP-19 的回退逻辑，无需额外状态。

## 4. 详细设计

### 4.1 ViewModel：前后位移方法

```kotlin
/** 左滑：离开当前项，未决策默认保留，已决策保持原样；越过最后一个触发完成 */
fun goNext() {
    val idx = _currentIndex.value
    if (idx >= _items.value.size) return
    val list = _items.value.toMutableList()
    if (list[idx].state == OperationState.PENDING) {
        list[idx] = list[idx].copy(state = OperationState.KEPT)   // 默认保留
        _items.value = list
    }
    _currentIndex.value = idx + 1
    refreshCanUndo()
}

/** 右滑：回上一项，仅移动位置，各项决定不变；已在首项无副作用 */
fun goPrevious() {
    val idx = _currentIndex.value
    if (idx <= 0) return
    _currentIndex.value = idx - 1
    refreshCanUndo()
}
```

**与决策/撤销的关系**：
- `goNext` 只在离开项**仍为 PENDING** 时落「保留」，绝不覆盖已有的 `PENDING_DELETE` / `KEPT`（AC2）。
- `goPrevious` 不动任何项状态（AC3）；滑回后再点删/留即走既有 `markDelete`/`markKept` 改判（AC4）。
- 越过最后一个：`currentIndex >= size` ⇒ `isComplete=true`，`ViewerScreen` 导航到结果页（AC6）。
- 左滑默认保留后 `currentIndex > 0` ⇒ `canUndo=true`，`undo()` 把该项恢复 `PENDING` 并回退（AC7），与 US-CP-19 同一通路。

### 4.2 CarouselMode：水平拖动手势

```kotlin
private const val CAROUSEL_SWIPE_THRESHOLD_DP = 72   // 触发切换的位移阈值

val thresholdPx = with(LocalDensity.current) { CAROUSEL_SWIPE_THRESHOLD_DP.dp.toPx() }

// 切到新项时重置偏移与播放状态
LaunchedEffect(currentIndex) { offsetX = 0f; isPlaying = false }

Modifier.pointerInput(currentIndex, items.size) {
    detectHorizontalDragGestures(
        onDragEnd = {
            when {
                offsetX <= -thresholdPx -> viewerViewModel.goNext()                    // 左滑
                offsetX >= thresholdPx && currentIndex > 0 -> viewerViewModel.goPrevious() // 右滑
                else -> offsetX = 0f                                                   // 回弹
            }
        },
        onDragCancel = { offsetX = 0f }
    ) { _, dragAmount -> offsetX += dragAmount }
}
```

- 阈值 72dp：低于此视为误触/犹豫，回弹复位（AC5 在首张右滑也走 `else` 回弹）。
- `pointerInput` key 含 `items.size`，列表变化时重建手势识别。
- `currentIndex` 变化时 `LaunchedEffect` 复位 `offsetX`，避免切换后残留位移。

## 5. 影响面与改动清单

| 文件 | 改动 |
|------|------|
| `viewmodel/ViewerViewModel.kt` | 新增 `goNext()` / `goPrevious()`（复用 `refreshCanUndo()`） |
| `ui/viewer/CarouselMode.kt` | 水平拖动 `onDragEnd` 按阈值调用 `goNext`/`goPrevious`；`LaunchedEffect` 复位 `offsetX`；新增阈值常量 |

无新增数据层/平台层/`expect/actual` 改动；纯 commonMain UI + ViewModel。不影响 SwipeCard / Fullscreen 两模式。

## 6. 新增术语（回写 domain-model.md）

| 业务术语 | 技术术语 | 说明 |
|---------|---------|------|
| 向后翻看 | goNext | 轮播左滑切下一个；离开项未决策默认保留，已决策保持 |
| 向前翻看 | goPrevious | 轮播右滑切上一个；纯位移，不改任何项决定 |
| 默认保留 | PENDING→KEPT on goNext | 左滑离开未决策项时的隐式保留语义，区别于显式点「保留」 |

## 7. 测试要点（详见 Step 3 测试方案）

- 单元（`ViewerViewModelTest`，前缀 `nav_`）：`goNext` 未决策默认保留并前进；`goNext` 保持已有删/留；`goPrevious` 纯位移不改状态；首项 `goPrevious` 无副作用；越过最后一项 `isComplete=true`；`goNext` 默认保留后 `undo()` 复原 PENDING；前后往返保留全部决定。
- E2E（Maestro，`carousel-swipe-nav.yaml`）：轮播 5 张，删第 1 张→左滑跳第 3→右滑回第 2→右滑回第 1（删除决定仍在）→首张右滑无副作用→连续左滑到底进结果页（1 删除 + 其余默认保留）。
