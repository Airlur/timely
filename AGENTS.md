# Timely（时予）项目协作规则

> 任何会话（无论 GLM-5.3 还是 GLM-5.3-flash）开工前必须先读完本文件和 `docs/TASKS.md`。

## 〇、项目是什么

- **时予 / Timely**：Android 原生「倒数日 / 正数日」App
- 技术栈：Kotlin + Jetpack Compose + Material 3 + Room，单模块 `app/`
- 由 2024 年的 Flutter 项目 remind_me 重构而来；旧代码在 `D:\Projects\Android\remind_me`（**只读参考，不改动**）
- 仓库：https://github.com/Airlur/timely
- 目标：功能对齐 remind_me 已实现部分，并**根治通知可靠性问题**（旧版在国产 ROM 上通知经常不响）

## 一、改动流程（强制，不可跳过）

任何功能或界面的改动，必须严格按这个顺序：

```
1. 先改原型（prototype/index.html）
2. 等用户确认原型
3. 再改 App 代码
4. 自查：代码逻辑是否真的对应本次要改的问题
5. 再等用户确认是否推送
6. 用户明确说「推送」之后，才能 git commit + git push
```

**绝对禁止：**
- 跳过原型直接改 Kotlin
- 未经用户明确同意就 `git commit` 或 `git push`
- 交付前不做自查就声称「改好了」

## 二、交付前必做的自查

在请用户确认之前，逐条核对：

1. 这次要修的问题，每一处都对应到具体代码/原型位置了吗？
2. 有没有可能重现用户上次反馈过的同类问题？
3. 原型和 App 的界面、文案、交互是否**完全一致**？
4. App 端改动是否已经在原型里同步体现？
5. Kotlin 编译是否通过（本地 kotlinc 语法检查，见下）？
6. 原型渲染是否验证过（headless 截图）？
7. **改到通知/调度相关代码时**，必须说明将如何通过 ARCHITECTURE.md 里的「通知三件套验证清单」。

## 三、本地验证手段（不装 Android SDK）

- Kotlin 语法检查：`D:\Projects\Android\_toolchain` 下的 JDK 17 + kotlinc 1.9.24 + `platform34\android-34\android.jar`
  （`R` 类需用工具链目录下的 stub，不能放进仓库；Compose 代码本地无法完整编译属正常，交给 GitHub Actions）
- 原型渲染：`"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --headless=new --screenshot`
  测量时 CSS 必须加 `*,*::before,*::after{transition:none!important;animation:none!important}`
- APK 出包：push 到 main 后由 GitHub Actions `build.yml` 编译，产物在 Actions Artifacts 和 Releases，**本地永远不做完整构建**

## 三之二、禁止在未验证的系统机制上冒险（血泪教训，继承自 AirMouse）

**不要在没查清副作用的情况下，使用会改变「应用自身身份」的系统 API。**

| 尝试 | 后果 |
|---|---|
| `activity-alias` 动态换桌面图标 | 华为桌面上图标重复、切换后消失，全部 alias 被禁用后应用彻底没有入口 |

规则：
1. 涉及 `PackageManager.setComponentEnabledSetting`、`activity-alias`、多入口组件的功能，默认不做
2. 确实要做必须先说明风险和回退方案，由用户决定
3. 一旦导致「应用不可用」，立刻整体回退，不在坏方案上打补丁

## 四、文档地图（跨会话记忆）

| 文档 | 内容 | 什么时候读 |
|---|---|---|
| `docs/PRD.md` | v1.0 功能范围 + 明确不做清单 | 做任何功能前 |
| `docs/DESIGN.md` | 设计语言：颜色/圆角/字体/logo | 改任何 UI 前 |
| `docs/ARCHITECTURE.md` | 技术栈、数据模型、通知调度设计 | 改数据层/通知前 |
| `docs/TASKS.md` | 任务看板（每完成一项更新状态） | 每次开工前 + 收工前 |
| `prototype/index.html` | 可交互原型（UI 的唯一事实源） | 改任何界面前 |

## 五、模型分工

- **GLM-5.3**：方案设计、文档维护、原型骨架与设计语言、疑难仲裁（通知调度等）
- **GLM-5.3-flash**：按既定规范执行——补全原型页面、写 Kotlin、跑本地语法检查、更新 TASKS.md
- flash 遇到方案级问题（不知道该怎么做、规范没写）：**停下来让用户转给 5.3**，不要自行发明方案

## 六、版本号

- 开发期从 `0.1.0` 起；首发 `1.0.0`
- 小修 `1.0.x`，功能迭代 `1.1`，架构调整才动主版本
- 原型标题、`versionName`、Actions Release 保持一致

## 七、构建与签名

- GitHub Actions：`build.yml`（JDK 17 + Gradle 8.7 + `:app:assembleRelease`），可参照 `D:\Projects\Android\airmouse\.github\workflows\build.yml`
- 固定签名：`keystore/debug.jks`（标准 debug 密钥 android/android，属于公开信息），debug/release 共用，保证可覆盖升级
