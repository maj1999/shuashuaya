# CleanPic 模块总览 — 数据流 / 导航 / 状态 / 删除策略

|文档状态| 初稿 | 2026-03-28 |

> 父文档: [../overview.md](../overview.md)

| 子模块 | 文档 | 说明 |
|--------|------|------|
| 主题系统 | [theme-system.md](theme-system.md) | Token 定义与 5 套主题 |
| 原生 Module | [native-modules.md](native-modules.md) | 各平台 MediaModule/权限/视频播放 |

## 数据流

```
用户点击"随机清理照片/视频"
    │
    ├─ 已有权限? ── 否 → PermissionModule.requestPhotoPermission()
    │                          │
    │                     用户拒绝 → 提示并返回首页
    │                     用户允许 ↓
    │
    ├─ 从 AppSettings 读取 roundCount（默认 10）
    │
    ├─ MediaRepository.queryPhotos() / queryVideos()
    │     → 返回 MediaItem[]
    │     → 空列表? → 展示空状态页
    │     → 数量 < roundCount? → 用实际数量，Toast 提示
    │
    ├─ RandomPicker.pick(items, count=roundCount, exclude=shownIds)
    │     → 随机不重复抽取，排除本次会话已展示的 ID
    │     → 剩余不足时，清空 shownIds 重新开始
    │
    ├─ 进入 Viewer 页，逐一展示
    │     → 照片: getThumbnail() 预加载 + getFullImage() 按需
    │     → 视频: getThumbnail() + VideoPlayer 按需初始化
    │     → 加载失败 → 占位图 + 提示
    │
    ├─ 用户操作: 保留→KEPT / 删除→PENDING_DELETE（仅标记）
    │
    ├─ 全部操作完毕 → 跳转 Result 页
    │     → 展示待删除列表预览 + 统计
    │     → 用户可取消部分删除标记（反悔）
    │     → 确认删除 → MediaRepository.deleteMedia(ids[])
    │         → 1 次系统弹窗
    │         → 取消 → 全部保留
    │         → 异常 → 提示失败
    │     → "再来一轮" → 重新 pick()
    │     → "返回首页" → 清空 shownIds
    │
    └─ 会话结束 → 清空 SessionState
```

## 导航栈

栈式导航：Home → Viewer → Result。设置页为模态弹出。

| 跳转 | 方式 | 转场动画 |
|------|------|---------|
| Home → Viewer | push | 从右滑入 |
| Viewer → Result | push (替换 Viewer) | 渐变过渡 |
| Result → Viewer ("再来一轮") | push (替换 Result) | 从右滑入 |
| Result → Home | pop to root | 从左滑出 |
| Home → Settings | 模态弹出 | 从下滑入 |

## 状态管理

| 状态 | 生命周期 | 内容 |
|------|---------|------|
| ViewerState | 单轮 (Viewer→Result) | MediaItem 列表、当前索引、每项 OperationState |
| SessionState | 会话级 (首页到返回首页) | 已展示 MediaItem ID 集合 (shownIds) |
| AppSettings | 持久化 | theme / interactionMode / roundCount |

### 应用生命周期处理

- **前后台切换**：ViewerState 保持在内存中，恢复后继续
- **进程被系统回收**：不持久化 ViewerState，重启从首页开始
- **来电/通知中断**：保持当前页面状态

## 删除行为（批量延迟删除）

1. **浏览阶段**：标记 `PENDING_DELETE`，不调用系统 API。UI 标红该项
2. **结果页确认**：展示待删除缩略图列表，用户可反悔
3. **批量执行**：`deleteMedia(ids[])` 一次性调用，仅 1 次系统弹窗
4. **结果处理**：确认→删除成功 / 取消→全部保留 / 异常→提示失败
5. **系统回收站**：被删除文件保留约 30 天，可在系统相册恢复

## 数据持久化

### AppSettings Schema

```
theme: String           — 默认 "dreamy-gradient"
interactionMode: String — 默认 "carousel"
roundCount: Int         — 默认 10，可选 5/10/15/20
```

### 各平台存储方案

| 平台 | 实现 |
|------|------|
| Android | SharedPreferences (EncryptedSharedPreferences) |
| iOS | UserDefaults |
| HarmonyOS | Preferences (data/preferences) |

共享层通过 `expect/actual` 声明统一接口。

## 错误处理与边界场景

| 场景 | 处理方式 |
|------|---------|
| 相册为空 | 空状态页：插画 + 提示 + 返回按钮 |
| 数量不足 N 张 | 用实际数量，Toast 提示 |
| 图片加载失败 | 主题色占位图 + 提示文案，操作按钮仍可用 |
| 视频缩略图失败 | 视频图标占位 + 文件名 |
| 视频播放失败 | 占位图 + "无法播放" + "跳过"按钮 |
| 删除弹窗被取消 | 全部保留，提示"已取消删除" |
| 删除 API 异常 | 提示"部分文件删除失败"，展示失败列表 |
| 权限永久拒绝 | 引导页 + "去设置"按钮 |
| 部分权限 | 首页提示条，引导授权全部 |
| 无更多未展示媒体 | 清空 shownIds，Toast 提示"重新开始" |
| 大相册（10 万+） | 分页查询，每页 500 条 |
