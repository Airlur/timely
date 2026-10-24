# 时予 Timely — 设计语言

> 起草：GLM-5.3，2026-10-24。色彩体系完整继承自 remind_me 的 `lib/theme/app_colors.dart`。
> 原型 `prototype/index.html` 是 UI 的唯一事实源，本文档定义它用的 token。

## 1. 品牌与命名

| 项 | 值 |
|---|---|
| 中文名 | 时予 |
| 英文名 | Timely |
| 包名 | `com.airlur.timely` |
| slogan（关于页用，可选） | 时予——把时间赠予你的每一天 |

## 2. Logo

**状态：已定稿（第一版）——A 引力环 Gravity Ring，2026-10-24 用户选定。**

- 符号：斜置 -24° 等宽椭圆环（rx : ry = 30 : 12），monoline 单线，无装饰
- 三色变体（都要产出）：**蓝底白环（默认桌面图标）**、白底蓝环、深底浅环
- 符号 SVG（96×96 视图，颜色由 currentColor 决定）：

```svg
<svg viewBox="0 0 96 96"><ellipse cx="48" cy="48" rx="30" ry="12" transform="rotate(-24 48 48)" fill="none" stroke="currentColor" stroke-width="8"/></svg>
```

- Adaptive icon（Android 12+）：前景 = 白色引力环（缩放到安全区内），背景 = 纯色 `#2196F3`，单色层 = 同形纯白（主题图标用）
- 生成 mipmap 各密度（48/72/96/144/192）时以蓝底白环为准
- 后续仅做粗细/倾角微调；改动需更新此处

## 3. 基础色板（Material 3 风格 token）

### 主色
| Token | 值 |
|---|---|
| primary | `#2196F3` |
| onPrimary | `#FFFFFF` |
| primaryContainer | `#BBDEFB` |
| onPrimaryContainer | `#1976D2` |

### 亮色表面
| Token | 值 | 用途 |
|---|---|---|
| surface | `#FFFFFF` | 卡片、对话框 |
| surfaceContainerLow | `#FBFCFE` | 次级容器 |
| surfaceContainer | `#F3F3F3` | 三级容器 |
| surfaceContainerHigh | `#ECEEF0` | 高一级容器 |
| background | `#F5F5F5` | 页面背景 |
| onSurface / onBackground | `#333333` | 正文 |

### 暗色表面
| Token | 值 |
|---|---|
| background | `#121212` |
| surface | `#1E1E1E` |
| surfaceContainerLow | `#171717` |
| surfaceContainerHigh | `#252525` |
| onSurface | `#E0E0E0` |

### 语义色
| Token | 值 |
|---|---|
| error | `#F02419`，errorContainer `#FFDAD6`，onErrorContainer `#410E0B` |
| 图标灰（次要文字） | `#757175` |

### 滑动操作色（列表卡片右滑菜单）
| 操作 | 颜色 |
|---|---|
| 置顶 | `#9C27B0` 紫 |
| 详情 | `#F33C76` 粉 |
| 编辑 | `#FF9800` 橙 |
| 删除 | `#F02419` 红 |

## 4. 事件卡片背景预设（完整保留自旧版）

### 4.1 纯色（20 色，已对旧版去重）

`#F44336 #E91E63 #9C27B0 #673AB7 #3F51B5 #2196F3 #03A9F4 #00BCD4 #009688 #4CAF50 #8BC34A #CDDC39 #FF0000 #00FFFF #607D8B #FFC107 #FF9800 #FF5722 #795548 #FFEB3B`

### 4.2 渐变（23 组，起→止，135°）

| # | 起 | 止 | # | 起 | 止 |
|---|---|---|---|---|---|
| 1 | `#FF416C` | `#FF4B2B` | 13 | `#1A2980` | `#26D0CE` |
| 2 | `#F85032` | `#E73827` | 14 | `#FF512F` | `#DD2476` |
| 3 | `#F12711` | `#F5AF19` | 15 | `#614385` | `#516395` |
| 4 | `#FF8008` | `#FFC837` | 16 | `#02AABD` | `#00CDAC` |
| 5 | `#FFE000` | `#FFBD00` | 17 | `#FF5F6D` | `#FFC371` |
| 6 | `#F7971E` | `#FFD200` | 18 | `#396AFC` | `#2948FF` |
| 7 | `#11998E` | `#38EF7D` | 19 | `#CC2B5E` | `#753A88` |
| 8 | `#00B09B` | `#96C93D` | 20 | `#42275A` | `#734B6D` |
| 9 | `#2193B0` | `#6DD5ED` | 21 | `#141E30` | `#243B55` |
| 10 | `#4364F7` | `#6FB1FC` | 22 | `#8E2DE2` | `#4A00E0` |
| 11 | `#834D9B` | `#D04ED6` | 23 | `#614385` | `#516395` |
| 12 | `#EE9CA7` | `#FFDDE1` | | | |

> 注：第 11 与第 15 旧版即为同值渐变，保留一处即可；实现时以本表数组顺序为准，卡片文字自动按背景亮度取白/深色（相对亮度 > 0.6 用深色文字 `#1A1A1A`，否则白色）。

## 5. 形状与排版

- 圆角：卡片 16dp，对话框 28dp，按钮/输入框 12dp，胶囊控件全圆
- 间距：页面边距 16dp，卡片内边距 16dp，列表项间距 12dp
- 字体：系统默认；**所有天数/计时数字用 tabular（等宽数字）**
- 天数字号层级：置顶 banner 64sp，普通卡片 32sp，详情页 96sp
- 亮暗主题切换无闪烁（Compose 侧用 Material3 dynamicColorScheme = false，固定品牌色）

## 6. 原型规范（prototype/index.html）

- 手机框居中（390×844 逻辑像素），页面切换用底部 3 tab + 日期页顶部子导航，全部可点击
- 静态示例数据写在原型顶部 `DATA` 常量里，覆盖：置顶事件、倒数/正数、农历、各背景样式、重复规则
- 原型同时承担「亮/暗主题预览」：右上角切换
