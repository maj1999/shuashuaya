# NFR / 权限 / 兼容性 — 测试场景

## 权限测试

| 编号 | 用例 | 操作 | 预期 |
|------|------|------|------|
| P01 | 首次授权 | 首次点击开始→允许 | 正常进入浏览页 |
| P02 | 拒绝权限 | 权限弹窗→拒绝 | 提示需要权限，返回首页 |
| P03 | 永久拒绝 | 拒绝且不再询问 | 引导页 + "去设置"按钮→跳转系统设置 |
| P04 | 部分权限 (iOS 14+) | 选择部分照片 | 首页提示条；从已授权照片中选取 |
| P05 | 部分权限 (Android 14+) | 选择部分照片 | 同 P04 |
| P06 | 运行时撤销 | 浏览中去设置关闭权限→返回 | 检测到权限丢失，提示并返回首页 |

## 性能测试

| 编号 | 指标 | 目标值 | 测试方法 |
|------|------|--------|---------|
| F01 | 冷启动时间 | < 2 秒 | 点击图标到首页完全渲染 |
| F02 | 浏览页切换帧率 | >= 55fps | 连续快速滑动 10 张时平均帧率 |
| F03 | 图片加载时间 | < 200ms | 滑到当前张到全尺寸显示 |
| F04 | 媒体列表查询 | < 3 秒 (5 万+) | 大相册 queryPhotos() 耗时 |
| F05 | 内存峰值 | < 150MB | 浏览 10 张 4K 照片时最大值 |
| F06 | 视频播放启动 | < 500ms | 滑到视频到播放首帧 |
| F07 | 主题切换延迟 | < 100ms | 点击主题到 UI 更新完成 |
| F08 | 包体积 | Android<15MB, iOS<25MB | 安装包大小 |

## 兼容性测试矩阵

### Android

| API 版本 | 代表机型 | 重点关注 |
|---------|---------|---------|
| API 26-28 | 旧设备 | 旧版 MediaStore API 兼容性 |
| API 29-32 | Xiaomi 12, OPPO Reno | Scoped Storage 适配 |
| API 33 | Samsung S23, Pixel 7 | READ_MEDIA_IMAGES/VIDEO 新权限 |
| API 34-35 | Samsung S24, Pixel 8/9 | 部分照片权限 |

### iOS

| 版本 | 代表机型 | 重点关注 |
|------|---------|---------|
| 14.x | iPhone SE(2nd), iPhone 8 | PHPicker, Limited Photo Access |
| 15.x | iPhone 12/13 | 稳定版本基线 |
| 16.x-18.x | iPhone 14/15/16 | 最新 API 变更 |

### HarmonyOS

| 版本 | 代表机型 | 重点关注 |
|------|---------|---------|
| NEXT 5.0+ | Mate 60, Mate X5 | 旗舰机性能基线 |
| NEXT 5.0+ | P60, nova 12 | 中端机性能验证 |

## L3 权限集成测试

| 编号 | 平台 | 用例 | 预期 |
|------|------|------|------|
| L3-P01 | Android API 33+ | requestPhotoPermission() | 弹出 READ_MEDIA_IMAGES 权限弹窗 |
| L3-P02 | Android API 26-32 | requestPhotoPermission() | 弹出 READ_EXTERNAL_STORAGE 权限弹窗 |
| L3-P03 | Android API 34+ | 用户选择部分照片 | checkPermissionStatus() 返回 LIMITED |
| L3-P04 | iOS 14+ | requestPhotoPermission() | 弹出 PHAuthorization 弹窗 |
| L3-P05 | iOS 14+ | 用户选择部分照片 | status == .limited |
| L3-P06 | HarmonyOS | requestPhotoPermission() | 弹出 READ_IMAGEVIDEO 权限弹窗 |
| L3-P07 | 全平台 | openAppSettings() | 跳转到 App 系统设置页 |
| L3-P08 | 全平台 | deleteMedia() 系统弹窗 | 弹窗正常显示，确认/取消行为正确 |
