# CleanPic — 原生 Module 设计

|文档状态| 初稿 | 2026-03-28 |

> 父文档: [overview.md](overview.md)

## MediaModule

各平台需独立实现（`expect/actual`）的媒体访问能力：

| 方法 | 功能 | Android | iOS | HarmonyOS |
|------|------|---------|-----|-----------|
| `queryPhotos()` | 获取照片列表 | MediaStore.Images | PHAsset (image) | photoAccessHelper |
| `queryVideos()` | 获取视频列表 | MediaStore.Video | PHAsset (video) | photoAccessHelper |
| `getThumbnail(id)` | 获取缩略图 | ContentResolver | PHImageManager | getThumbnail |
| `getFullImage(id)` | 获取原图 | ContentResolver | PHImageManager | requestImageData |
| `deleteMedia(ids)` | 批量删除 | createDeleteRequest | PHAssetChangeRequest | deleteAssets |

### 性能约束

- `queryPhotos/Videos()` 仅查询 ID + 基本元数据，不加载二进制内容
- 大相册（10 万+）分页查询，每页 500 条
- `getThumbnail()` 返回固定尺寸缩略图（300x300），非原图
- `getFullImage()` 仅对当前展示项调用

## PermissionModule

| 方法 | 功能 |
|------|------|
| `requestPhotoPermission()` | 请求相册读取+删除权限 |
| `checkPermissionStatus()` | 检查当前权限状态 |
| `openAppSettings()` | 跳转系统设置页 |

### 各平台权限差异

| 场景 | Android | iOS | HarmonyOS |
|------|---------|-----|-----------|
| 基础权限 | API 33+: `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO`；API 26-32: `READ_EXTERNAL_STORAGE` | `PHAuthorizationStatus.authorized` | `ohos.permission.READ_IMAGEVIDEO` |
| 部分权限 | API 34+: `READ_MEDIA_VISUAL_USER_SELECTED` | iOS 14+: `.limited` | 暂无 |
| 部分权限处理 | 首页提示条"仅能访问部分照片"，引导授予全部 | 同左 | N/A |
| 删除权限 | `createDeleteRequest()` 自带系统确认 | `PHAssetChangeRequest` 自带确认 | `deleteAssets()` 自带确认 |
| 永久拒绝 | 引导页 + "去设置"按钮 → `openAppSettings()` | 同左 | 同左 |

## VideoPlayerModule

| 功能 | Android | iOS | HarmonyOS |
|------|---------|-----|-----------|
| 播放器 | ExoPlayer (Media3) | AVPlayer | AVPlayer (ArkUI) |
| 支持格式 | MP4/MOV/HEVC/3GP/WebM | MP4/MOV/HEVC/M4V | MP4/MOV/HEVC/MKV |
| 预览模式 | 缩略图 + 点击内联播放 | 同左 | 同左 |
| 全屏模式 | 自动静音播放，点击切声音 | 同左 | 同左 |
| 内存管理 | 仅当前张初始化播放器，滑出 ±1 张即释放 | 同左 | 同左 |
