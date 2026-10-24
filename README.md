# Timely（时予）

倒数日 / 正数日 Android 应用。记下你期待的日子（考试、旅行、发工资）和纪念的日子（在一起第 N 天、生日），支持农历、重复规则和到点提醒。

从 [remind_me](https://github.com/Airlur/remind_me)（Flutter 版，2024）重构而来：同样的功能骨架，换成 Kotlin + Jetpack Compose 原生实现，提醒改用 `AlarmManager.setAlarmClock`，不需要用户手动开"闹钟和提醒"权限，重启、杀后台、换时区之后照样响。

## 功能

- **倒数日 / 正数日**：置顶（单个或轮播）、分类（8 个，可配色和图标）、农历日期、重复规则（每天/每周/每月/每年/自定义）、天/周/月/年显示切换、含首日计数
- **卡片样式**：20 种纯色 + 23 种渐变预设，支持自定义颜色和相册图片，可选事件图标和字体
- **滑动操作**：两种模式可选——展开按钮（经典两段式）和松手触发（按滑动距离分段渐显，松手即执行），每个位置的动作可以在设置里自定义
- **提醒**：每事件一个提醒时间点，闹钟级调度；重启手机、覆盖安装、改时间/时区后自动重排
- **日历**：月视图 + 事件角标 + 当日列表
- **倒计时 / 秒表**：圆环倒计时、快捷时长、秒表计次（标最快最慢圈）
- **其他**：中英双语、深色模式、卡片水印（位置/透明度可调）、列表/网格视图

## 下载

到 [Releases](https://github.com/Airlur/timely/releases) 下载 APK 安装。Android 9.0+。

## 开发

单模块 `app/`，Kotlin 1.9.24 + Compose（BOM 2024.05）+ Room + DataStore，无第三方 DI / 网络 / 图片库；农历转换用 [lunar-java](https://github.com/6tail/lunar-java)（与旧版 Flutter 所用 lunar 库同源）。

原型与协作规范见 [AGENTS.md](AGENTS.md)，施工图见 [docs/](docs/)。

### 构建

push 到 main 后由 GitHub Actions 自动出包并发布 Release。正式签名通过仓库 Secrets 注入（`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`）；未配置时回退 debug 签名。本地构建需要 JDK 17 + Android SDK 34。

### 协议

MIT
