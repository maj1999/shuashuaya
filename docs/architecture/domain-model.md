# 刷刷鸭 — 领域模型（术语 SSOT）

|文档状态| 更新 | 2026-04-12 |

## 术语映射

| 业务术语 | 技术术语 | 说明 |
|---------|---------|------|
| 照片 | MediaItem (type=PHOTO) | 用户相册中的图片文件 |
| 视频 | MediaItem (type=VIDEO) | 用户相册中的视频文件 |
| 随机选取 | RandomPicker.pick() | 纯函数：基于浏览记忆从媒体列表不重复抽取，返回结果 + 新记忆 |
| 洗牌袋 | Shuffle Bag | 不放回抽取直到袋子耗尽再装满的随机策略，保证一圈内不重复 |
| 浏览记忆 | PickState | 持久化的随机选取状态：循环号 + 每媒体的 SeenRecord（替代旧 shownIds） |
| 浏览记录 | SeenRecord | 单个媒体的记忆：lastDrawnCycle / lastSeenMillis / kept |
| 循环 | cycle | 洗牌袋的一个完整轮次；袋中非保留项耗尽则 cycle++ |
| 保留集 | SeenRecord.kept | 被保留过的媒体，沉底，仅在非保留项耗尽时才再出现 |
| 天数新鲜度 | freshDays | 近 N 天看过的项在有更优项时轻微后排，默认 1 天 |
| 浏览记录存储 | PickStateStore | 持久化 PickState 的接口（load/save/clearAll），按 MediaType 分键 |
| 重置浏览记录 | PickStateStore.clearAll() | 清空全部浏览记忆，让所有媒体重新参与随机 |
| 一轮清理 | Round / Session | 用户从点击"开始"到查看结果的完整流程 |
| 每轮数量 | roundCount (AppSettings) | 每轮随机选取的媒体数量，默认 10 |
| 保留 | OperationState.KEPT | 用户决定不删除该媒体 |
| 标记删除 | OperationState.PENDING_DELETE | 浏览阶段的删除意图标记，尚未执行 |
| 确认删除 | MediaModule.deleteMedia(ids) | 结果页用户确认后，批量调用系统删除 API |
| 结果页阶段 | ResultPhase (CONFIRM/DONE) | 结果页的两个语义状态，由 resolveResultPhase() 派生 |
| 待确认态 | ResultPhase.CONFIRM | 删除前：有待删除项且尚未删除成功，标题"即将删除 N 项"，展示确认按钮 |
| 完成态 | ResultPhase.DONE | 删除成功后、或本轮无待删除项，标题"本轮清理完成" |
| 系统回收站 | 系统 Trash / Recently Deleted | 各平台系统级回收站，通常保留 30 天 |
| 主题 | Theme (ThemeTokens) | 一组语义化 UI Token 的集合，包含视觉参数 + 布局标识 |
| 主题布局 | ThemeLayoutId | 主题绑定的布局标识枚举（MINIMAL/GEOMETRIC/WARM/PLAYFUL/EDITORIAL） |
| 进度条样式 | ProgressStyle | 进度条视觉枚举（THIN/BOLD/SOFT/GLASS/EDITORIAL） |
| 按钮样式 | ButtonStyle | 按钮视觉枚举（OUTLINED/FILLED/SHADOW/GLASS/TEXT） |
| 矢量图标 | AppIcons (ImageVector) | 统一图标入口，根据主题返回对应线宽/颜色/端点的图标 |
| 页面状态接口 | HomeScreenState 等 | 页面业务逻辑的共享接口，5 个布局变体通过它接收数据和回调 |
| 交互模式 | InteractionMode | 浏览页的操作方式（轮播/卡片/全屏） |
| 设置 | AppSettings | 持久化的用户偏好（主题/模式/数量） |
| 版本检查 | UpdateChecker.checkForUpdate() | 请求远程 API 获取最新版本信息并与本地版本比较 |
| 更新信息 | UpdateInfo | 包含版本号、下载地址、是否强制、更新日志的数据结构 |
| 强制更新 | UpdateStatus.FORCE_UPDATE | 用户必须更新才能继续使用（不可关闭弹窗） |
| 可选更新 | UpdateStatus.OPTIONAL_UPDATE | 用户可选择稍后提醒 |
| 下载安装 | UpdateInstaller.startUpdate() | 各平台独立实现的更新包下载与安装 |
| 版本 API | Cloudflare Workers /api/version | 返回各平台最新版本信息的远程接口 |
| 下载代理 | Cloudflare Workers /download/ | 代理 GitHub Release 资源下载，解决国内访问 |
| 分发渠道 | Distribution Channel / productFlavor | 区分构建版本面向的用户群的维度，决定是否包含应用内升级 |
| 直装版 | Direct Build（`direct` flavor） | 通过 GitHub Release 分发的 APK，包含完整应用内升级能力 |
| 商店版 | Store Build（`store` flavor） | 通过应用商店分发的 APK，编译期完全移除升级相关代码与网络请求，升级由商店负责 |
| 全屏查看 | FullscreenViewer | 无状态全屏展示组件，三种交互模式复用；区别于交互模式"全屏上下滑(Fullscreen)" |
| 缩放播放内核 | ZoomableMediaContent | 抽取自 FullscreenViewer 的照片缩放 + 视频播放公共组件，浏览页与待删除预览共用 |
| 待删除预览 | DeletePreviewOverlay | 结果页待确认态点缩略图进入的全屏预览（HorizontalPager 左右滑 + 取消删除） |
| 撤销 | Undo / canUndo | 单步撤销上一次删/留决策，回到上一项重新选择 |
| 最近决策项 | lastDecisionIndex | 最近一次做出删/留决策的媒体项下标，撤销的目标 |
| 向后翻看 | goNext | 轮播模式左滑切到下一个媒体；离开项未决策则默认保留，已决策保持原样 |
| 向前翻看 | goPrevious | 轮播模式右滑切回上一个媒体；纯位移，不改任何项的决定 |
| 默认保留 | PENDING→KEPT on goNext | 轮播左滑离开未决策项时的隐式保留语义，区别于显式点「保留」 |

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
