# 时予 Timely — 架构与施工图

> 起草：GLM-5.3，2026-10-24。flash 施工前必读；与本文冲突的实现一律以本文为准，本文没写的先问 5.3。

## 1. 技术栈

| 项 | 选择 |
|---|---|
| 语言 | Kotlin 1.9.24（与本地工具链 kotlinc 版本一致） |
| UI | Jetpack Compose + Material 3（androidx.compose BOM 2024.05.00） |
| 构建 | Gradle 8.7 + AGP 8.4，JDK 17，Kotlin DSL |
| SDK | minSdk 28，targetSdk 34，compileSdk 34 |
| 存储 | Room 2.6（事件/分类）+ DataStore Preferences（设置） |
| 其他 | 仅 AndroidX 官方依赖 + kotlin serialization（JSON 槽位存储）。**不引入** DI 框架、网络库、图片库（本 App 无网络功能） |
| 模块 | 单模块 `app/`，namespace `com.airlur.timely` |

农历：使用 `cn.6tail:lunar-java`（与旧版 Flutter 所用 `lunar` 包同作者同算法，输出口径一致），不手写对照表；干支年 + 中文月日格式化在 UI 层完成。

## 2. 包结构

```
com.airlur.timely/
├─ MainActivity.kt            单 Activity，全 Compose
├─ ui/
│  ├─ theme/                  颜色/形状/字体（对应 DESIGN.md token）
│  ├─ AppNav.kt               底部 3 tab：日期 / 时间 / 设置
│  ├─ date/                   DateListScreen, DateBanner, EventCard,
│  │                          EditEventScreen, EventDetailScreen, CalendarScreen,
│  │                          CategorySheet, BackgroundPicker
│  ├─ time/                   TimerScreen, StopwatchScreen
│  └─ settings/               SettingsScreen, NotificationSettingsScreen, AboutScreen
├─ data/
│  ├─ db/                     AppDatabase, Converters
│  ├─ entity/                 EventEntity, CategoryEntity
│  ├─ dao/                    EventDao, CategoryDao
│  ├─ repo/                   EventRepository（增删改后回调 notify 包重排）
│  └─ prefs/                  SettingsStore（语言/主题/排序）
├─ notify/
│  ├─ AlarmScheduler.kt       全部调度逻辑（唯一入口）
│  ├─ AlarmReceiver.kt        触发：发通知 + 排下一次
│  ├─ BootReceiver.kt         开机/换版/时间变更 → 全量重排
│  └─ Notifier.kt             渠道创建与通知展示
└─ domain/
   ├─ DateMath.kt             天数/单位换算（年月组合、含首日）——纯函数，可单测
   ├─ Lunar.kt                农历换算
   └─ RepeatRule.kt           下一次出现日期计算（移植 reminder_service.dart 算法）
```

## 3. 数据模型（Room）

### 3.1 EventEntity（对照 remind_me `lib/models/event.dart`）

| Room 字段 | 类型 | 旧 Hive 字段 | 说明 |
|---|---|---|---|
| `id` | Int 自增主键 | id (uuid) | 不用 uuid；通知 requestCode 依赖它 |
| `title` | String | title | ≤20 字 |
| `targetDate` | Long (epochMilli) | targetDate | 目标时刻 |
| `createdAt` | Long | createdAt | |
| `displayUnit` | String 枚举 | displayUnit | DAY/WEEK/MONTH/YEAR |
| `isPinned` | Boolean | isPinned | 全表最多 1 个 true |
| `pinnedAt` | Long? | pinnedAt | |
| `bgType` | String 枚举 | backgroundType | v1.0 只有 COLOR/GRADIENT |
| `bgColor` | Int? (ARGB) | backgroundColorValue | 纯色背景 |
| `bgGradientStart` / `bgGradientEnd` | Int? | gradientColorValues | 旧 List 拆成两列，渐变固定两色 |
| `isLunar` | Boolean | isLunar | 农历事件 |
| `categoryId` | Int? FK | category (String) | 改外键关联 CategoryEntity |
| `sortOrder` | Int | order | |
| `iconName` | String? | iconName | 分类图标体系内选用 |
| `reminderOffsets` | String (JSON `[Int分钟]`) | reminderTime (单时间) | **改为提前量列表**，≤5 项，5 分钟~7 天；相对 targetDate 的分钟数 |
| `includeStartDay` | Boolean | includeStartDay | |
| `repeat` | String 枚举 | repeat | NONE/DAILY/WEEKLY/MONTHLY/YEARLY/CUSTOM |
| `repeatEnd` | Long? | endTime | |
| `customRule` | String? (JSON) | customRepeatRule | `{interval,unit,weekdays[],monthDays[],endDate}` |

**不迁移字段**：`fontFamily`、`localImagePath`、`animationType`（v1.0 无此功能）。

### 3.2 CategoryEntity：`id` 自增、`name`、`iconName?`、`sortOrder`。上限 8 个（业务层校验）。

### 3.3 Settings（DataStore）：`language`（system/zh/en）、`theme`（system/light/dark）、`listSort`（created/date/manual）。

## 4. 通知调度设计（本项目成败关键）

### 4.1 权限（Manifest 只声明这四个）

```
POST_NOTIFICATIONS（运行时请求，Android 13+）
RECEIVE_BOOT_COMPLETED
VIBRATE
USE_FULL_SCREEN_INTENT（仅倒计时到点全屏提醒用，可选）
```

**不再声明**：SCHEDULE_EXACT_ALARM、USE_EXACT_ALARM、SYSTEM_ALERT_WINDOW、REQUEST_IGNORE_BATTERY_OPTIMIZATIONS。

### 4.2 调度

- 核心 API：`AlarmManager.setAlarmClock(info, operation)` —— 闹钟级触发，**无需任何精确闹钟特殊授权**，系统在状态栏显示闹钟图标，Doze 下照常触发
- 每个「事件 × 提前量槽位」一个 PendingIntent：`requestCode = eventId * 10 + slotIndex`（稳定、无碰撞）
- 触发流程（AlarmReceiver）：查库取事件 → 无通知权限则静默跳过 → Notifier 发通知（channel `reminder`，high + 声音/震动）→ 若事件有重复规则，按 RepeatRule 算下一次出现并重排全部槽位
- 排下一次时先算「下一次出现时刻 − 提前量」，已过期的槽位直接丢弃

### 4.3 全量重排时机（`AlarmScheduler.rescheduleAll()`）

1. App 冷启动（MainActivity onCreate）
2. 事件增/删/改（Repository 回调）
3. `BOOT_COMPLETED`
4. `MY_PACKAGE_REPLACED`
5. `ACTION_TIME_CHANGED` / `ACTION_TIMEZONE_CHANGED`

时区：全部用 `java.time.*` + 系统默认时区，**任何地方不得硬编码时区**（旧版教训）。

### 4.4 通知三件套验证清单（改通知必测，向用户报告结果）

- [ ] 锁屏状态下到点触发（有声/震动）
- [ ] 重启手机后既存提醒仍会触发
- [ ] 最近任务里划掉 App 后到点仍触发
- [ ]（重复事件）触发后下一次被正确排上
- [ ]（时区）改系统时区后提醒时刻跟随变化

### 4.5 权限引导流

首次保存带提醒的事件 → 请求 `POST_NOTIFICATIONS`；拒绝 → 卡片上显示「提醒未开启」灰色标记；设置页提供跳转系统通知设置的入口。

## 5. UI 结构

- 单 Activity + Compose；底部导航 3 tab（日期/时间/设置），状态自持不引入导航库（两个二级页用简单的 `NavHost` 或状态切换，从简）
- 日期 tab：顶部子导航（全部/倒数/正数/日历）；列表 = 置顶 banner + 普通卡片流；FAB 新建
- 数字显示规则：`DateMath` 纯函数输出（含年/月组合、含首日序数），与原型逐字一致

## 6. 构建与 CI

- `.github/workflows/build.yml`：照抄 airmouse（JDK 17 + Gradle 8.7 + `:app:assembleRelease` + 版本号从 `app/build.gradle.kts` 读 + artifact + 按版本 tag 发 Release）
- 签名：`keystore/debug.jks`（标准 debug 密钥），debug/release 共用；keystore 生成命令见 TASKS.md Phase 0
- 本地不构建 APK，只做 kotlinc 语法检查

## 7. 测试策略

- `domain/` 三个纯函数类（DateMath/Lunar/RepeatRule）写 JUnit 单测，Actions 上随构建跑（`gradle test`）
- UI 手测：每阶段出 APK 由用户装机验证；通知改动必须过 4.4 清单
