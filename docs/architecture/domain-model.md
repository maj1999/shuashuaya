# CleanPic — 领域模型（术语 SSOT）

|文档状态| 初稿 | 2026-03-28 |

## 术语映射

| 业务术语 | 技术术语 | 说明 |
|---------|---------|------|
| 照片 | MediaItem (type=PHOTO) | 用户相册中的图片文件 |
| 视频 | MediaItem (type=VIDEO) | 用户相册中的视频文件 |
| 随机选取 | RandomPicker.pick() | 从媒体列表中不重复随机抽取指定数量 |
| 一轮清理 | Round / Session | 用户从点击"开始"到查看结果的完整流程 |
| 每轮数量 | roundCount (AppSettings) | 每轮随机选取的媒体数量，默认 10 |
| 保留 | OperationState.KEPT | 用户决定不删除该媒体 |
| 标记删除 | OperationState.PENDING_DELETE | 浏览阶段的删除意图标记，尚未执行 |
| 确认删除 | MediaModule.deleteMedia(ids) | 结果页用户确认后，批量调用系统删除 API |
| 系统回收站 | 系统 Trash / Recently Deleted | 各平台系统级回收站，通常保留 30 天 |
| 主题 | Theme (ThemeTokens) | 一组语义化 UI Token 的集合，包含视觉参数 + 布局标识 |
| 主题布局 | ThemeLayoutId | 主题绑定的布局标识枚举（MINIMAL/GEOMETRIC/WARM/PLAYFUL/EDITORIAL） |
| 进度条样式 | ProgressStyle | 进度条视觉枚举（THIN/BOLD/SOFT/GLASS/EDITORIAL） |
| 按钮样式 | ButtonStyle | 按钮视觉枚举（OUTLINED/FILLED/SHADOW/GLASS/TEXT） |
| 矢量图标 | AppIcons (ImageVector) | 统一图标入口，根据主题返回对应线宽/颜色/端点的图标 |
| 页面状态接口 | HomeScreenState 等 | 页面业务逻辑的共享接口，5 个布局变体通过它接收数据和回调 |
| 交互模式 | InteractionMode | 浏览页的操作方式（轮播/卡片/全屏） |
| 设置 | AppSettings | 持久化的用户偏好（主题/模式/数量） |
| 已展示集合 | shownIds (SessionState) | 本次会话中已展示过的媒体 ID 集合，用于去重 |

## 核心数据模型

```
MediaItem
├── id: String          — 各平台媒体唯一标识
├── type: PHOTO | VIDEO
├── name: String        — 文件名
├── size: Long          — 字节数
├── date: Timestamp     — 拍摄/创建日期
├── width: Int          — 像素宽
├── height: Int         — 像素高
└── duration: Long?     — 视频时长（毫秒），照片为 null

ViewerItem (浏览页运行时)
├── media: MediaItem
├── state: PENDING | KEPT | PENDING_DELETE
└── thumbnailLoaded: Boolean
```
