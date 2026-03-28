# CleanPic — 主题系统设计

|文档状态| 初稿 | 2026-03-28 |

> 父文档: [overview.md](overview.md)

## 设计原则

语义化 Token 架构：所有 UI 组件引用 Token 而非硬编码色值。切换主题 = 替换一组 Token 值，UI 即时响应。

## Token 定义

```
colorBackground    — 页面底色
colorSurface       — 卡片/容器底色
colorPrimary       — 主操作色/按钮色
colorAccent        — 强调色/高亮色
colorDanger        — 删除操作色（红系）
colorSuccess       — 保留操作色（绿系）
colorText          — 主文本色
colorTextSecondary — 次文本色
gradientMain       — 主渐变（方向 + 色值数组）
borderRadius       — 圆角大小 (px)
shadowStyle        — 阴影风格（偏移/模糊/颜色）
fontFamily         — 字体风格
animDuration       — 基础动画时长 (ms)
animEasing         — 缓动曲线
animButtonPress    — 按钮按压动效（scale / none / bounce）
```

## 5 套主题定义

### 梦幻渐变（默认）

| Token | 值 |
|-------|----|
| colorPrimary | #C4B5FD |
| colorAccent | #F9A8D4 |
| colorBackground | 渐变 #EDE9FE → #FCE7F3 → #FEF9C3 |
| colorSurface | rgba(255,255,255,0.6) + backdrop-blur |
| gradientMain | 135deg, #C4B5FD → #F9A8D4 → #FCD34D |
| borderRadius | 16px |
| animButtonPress | scale(0.95) |

**特色元素**：毛玻璃卡片、按钮渐变发光、背景粒子微动画

### 柔和极简

| Token | 值 |
|-------|----|
| colorPrimary | #FBCFE8 |
| colorAccent | #F9A8D4 |
| colorBackground | #FDF2F8 |
| colorSurface | #FFFFFF |
| gradientMain | 无（纯色） |
| borderRadius | 20px |
| animButtonPress | none |

**特色元素**：纸质阴影、无渐变纯色、大字留白

### 可爱活泼

| Token | 值 |
|-------|----|
| colorPrimary | #FDE68A |
| colorAccent | #86EFAC |
| colorBackground | #FFFBEB |
| colorSurface | #FFFFFF |
| gradientMain | 135deg, #FDE68A → #86EFAC |
| borderRadius | 24px |
| animButtonPress | bounce (scale 1.0→1.1→0.95→1.0) |

**特色元素**：多彩配色（黄/绿/蓝/粉）、按钮弹跳动画

### 优雅暗黑

| Token | 值 |
|-------|----|
| colorPrimary | #C4B5FD |
| colorAccent | #FBBF24 |
| colorBackground | #1E1B4B |
| colorSurface | #312E81 |
| colorText | #E5E7EB |
| gradientMain | 135deg, #1E1B4B → #4C1D95 |
| borderRadius | 12px |
| animButtonPress | scale(0.97) |

**特色元素**：金色描边高亮、暗面微光纹理、衬线标题字

### 自然温暖

| Token | 值 |
|-------|----|
| colorPrimary | #A8A29E |
| colorAccent | #57534E |
| colorBackground | #FAFAF9 |
| colorSurface | #F5F5F4 |
| gradientMain | 135deg, #FEF3C7 → #D6D3D1 |
| borderRadius | 8px |
| animButtonPress | scale(0.97) |

**特色元素**：纸张纹理背景、手写风装饰元素、暖色阴影

## 主题总览表

| 主题 | 主色调 | 圆角 | 动效风格 |
|------|--------|------|---------|
| 梦幻渐变（默认） | 紫粉金 | 16px | 毛玻璃 + 粒子动画 |
| 柔和极简 | 浅粉奶白 | 20px | 无动效，纯静态 |
| 可爱活泼 | 黄绿蓝粉 | 24px | 弹跳动画 |
| 优雅暗黑 | 深紫金 | 12px | 微光 + 金色描边 |
| 自然温暖 | 灰棕暖黄 | 8px | 纸张纹理 |
