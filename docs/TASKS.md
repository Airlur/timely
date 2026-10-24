# 时予 Timely — 任务看板

> 每完成一项把 `[ ]` 改 `[x]` 并在括号里记日期；发现新问题加到「待讨论」。
> 执行者标注：〔5.3〕= 主线模型做，〔flash〕= flash 做，〔用户〕= 需要用户拍板/操作。

## Phase 0 — 仓库骨架（〔flash〕，方案已定）

- [x] 0.1 Gradle 工程初始化：settings/build.gradle.kts、app 模块、namespace `com.airlur.timely`、minSdk 28 / targetSdk 34、versionName `0.1.0`（2026-10-24，5.3）
- [x] 0.2 双签名：debug.jks（仓库内，标准 debug 密钥）+ release.jks（本地生成 4096 位 RSA，密码与 base64 存于本机 `%USERPROFILE%\.zcode\timely_release_*.txt`，**不入库**；CI 由 4 个 Secrets 注入 keystore.properties，未配置时回退 debug 签名）（2026-10-24，5.3）
- [x] 0.3 MainActivity + Compose 主题（品牌色 + 暗色 scheme + 等宽数字）（2026-10-24，5.3）
- [x] 0.4 `.github/workflows/build.yml`（版本号读取 + Secrets 恢复签名 + 单测 + assembleRelease + apksigner 校验 + artifact + Release）（2026-10-24，5.3）
- [ ] 0.5 push 后 Actions 出第一个冒烟 APK →〔用户〕装机确认

## Phase 1 — 原型（骨架〔5.3〕，铺页面〔flash〕）

- [x] 1.1 prototype/index.html 骨架（2026-10-24，5.3）
- [x] 1.2 倒数日列表页（2026-10-24，5.3）
- [x] 1.3 新建/编辑事件页 v0.2.1（分块表单，2026-10-24，5.3）
- [x] 1.4 事件详情页（2026-10-24，5.3）
- [x] 1.5 日历页（2026-10-24，5.3）
- [x] 1.6 时间页（2026-10-24，5.3）
- [x] 1.7 设置页（2026-10-24，5.3）
- [x] 1.8 〔用户〕确认原型（v0.2.2：分块编辑页/间距/banner 点击进全屏详情/设置统一分段条/滑动与水印示例卡——2026-10-24 完成，用户批准进入开发）

## Phase 2-8 — Compose 实现（2026-10-24，5.3 一次写完）

- [x] 2.1 Room：EventEntity/CategoryEntity/Dao/AppDatabase（字段对照 ARCHITECTURE.md 3.1）
- [x] 2.2 DataStore：UiSettings（主题/语言/排序/视图/置顶模式/滑动模式/槽位/水印/自定义颜色）
- [x] 2.3 domain/DateMath（卡片前缀+详情多单位）+ 单测
- [x] 2.4 domain/RepeatRule（日/周/月/年/自定义+月末钳制）+ 单测（农历改用 lunar-java 库，与旧版 lunar 包同源——修正 ARCHITECTURE 手写方案）
- [x] 6.1 notify 三件套：setAlarmClock 调度 / AlarmReceiver（重复事件滚动重排）/ BootReceiver（开机/换版/时间/时区）
- [x] 3.x ui/date/DateScreen：TabBar、置顶轮播（HorizontalPager 自动滚动）、拖拽排序列表、双模式滑动卡片、⋯菜单、排序弹层（彩色分类筛选）、日历、网格视图、删除撤销 snackbar
- [x] 4.x ui/date/EditEventScreen：四块分卡 + 日期/分类/提醒/重复/结束弹层 + 样式页（纯色/渐变/图片 + 自定义颜色 HSV + 图标 + 字体 + 实时预览）
- [x] ui/date/DetailScreen：全屏沉浸 + 圆形扩散（CircularReveal）+ 大数字点击切单位 + 底部四操作
- [x] 7.x ui/time/TimeScreen：倒计时圆环（步进/预设/震动）+ 秒表计次（最快/最慢）
- [x] 8.x ui/settings/SettingsScreen：主题/语言行内分段（交互统一）+ 滑动操作页（分段切换 + 可拖拽示例卡 + 六槽位自定义）+ 水印页（开关/位置/透明度 + 实时示例）+ 通知页（权限/测试）+ 帮助 + 关于
- [x] AppRoot：底部 3 tab + overlay 导航 + 语言 per-app locale（AppCompat）+ 启动重排
- [x] 资源：中英双语 strings（60+ 条）、引力环 adaptive icon（前景/背景/单色三层）、通知小图标

## Phase 9 — 收尾

- [x] 9.0 〔5.3〕子代理静态审查（17 项 P0/P1 全修复，二轮复核 PASS）→ 推送 → CI 迭代（52→13→4→0 条编译错误）全绿（2026-10-24）
- [x] 9.1 v0.1.0 冒烟 APK 经 Actions 构建并发布 Release（首包为 debug 签名回退；〔用户〕配好 4 个 Secrets 后下个版本自动切换正式签名，注意两签名不可互覆盖，建议首发前切）
- [ ] 9.2 〔用户〕装机过「通知三件套验证清单」（ARCHITECTURE.md 4.4）
- [ ] 9.3 装机反馈 → 打磨 + versionName `1.0.0` 首发

## 已知限制（下轮处理）

- 农历事件目前存公历毫秒 + 显示农历文本，"按农历滚到下一次"未实现（PRD 2.1 完整口径）
- 倒计时到点用测试通知通道发声，尚无自定义铃声
- 硬编码中文残留（日期格式/星期表头/色相滑条标签），英文界面下会混杂
- 状态栏图标颜色未跟随应用内深色切换

## 待讨论

- logo 已定稿：A 引力环（2026-10-24），后续仅微调
- 小组件（v1.1）是否做、先做哪个尺寸
- 图片背景（v1.1 再议）
