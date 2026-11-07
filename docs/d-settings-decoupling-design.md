# D 专项：settings 模块跨 feature 债务解除（D1 widget / D2 streaming）

> 状态：已批准并收口（2026-11-08）——D1、D2 均完成（改造 + 依赖剪断 + 全量门禁绿），仅剩 local→streaming 待 D3。上游蓝图：`docs/multi-module-refactor-plan.md`（PR-1..7 已收口）、`docs/b1-musicviewmodel-seams-design.md`（settings→local 已解除）。
> 范围：AGENTS.md §4 登记 3 条 feature→feature 债务中的两条——**settings→widget（D1）** 与 **settings→streaming（D2）**。第三条 local→streaming 消费面更重，另立 D3 设计，本专项不动。

## 1. 意图与动机

B1 PR-9 解除 settings→local 后，settings 还挂着两条 feature 依赖（`feature/settings/build.gradle.kts:22-24`）。只要依赖存在一天，下一个设置页功能就会自然滑回直接 import 其他 feature 的老路（B1 设计原话）。两条债务的实测消费面都很小，属于性价比最高的收尾。

2026-11-08 实测（本设计的依据）：

- **settings→widget**：全部消费集中在 `WidgetSettingsScreen.kt` 一个文件的公开顶层助手 `updateAllWidgets(context)`（:675-685）——向 `RhythmWidgetReceiver` 发 `ACTION_APPWIDGET_UPDATE` 广播 + 调 `GlanceWidgetUpdater.forceUpdateAll(context)`。文件内 9 个调用点全是「写 AppSettings → 刷新小组件」。settings 无任何 widget 资源引用（widget res 全在 `:feature:widget`，settings 的 `R` 来自 designsystem）。`:app` 的 OnboardingScreen 也 import 该助手（:203, :7899 等 4 处），app→settings 依赖合法，不受影响。
- **settings→streaming**：全部消费是 `SettingsScreenWrapper.kt` 平板(:363-372)/手机(:530-539)两处**逐字相同**的内嵌渲染 `GoSettingsScreen`（GO_SETTINGS 内部栈路由）。`:app` 已有语义一致的宿主路由 `Screen.TunerGoSettings`（"tuner_go_settings"，LocalNavigation.kt:2357-2369，onConfigureCurrentProvider 逻辑与 wrapper 分支相同，显式传根 `streamingMusicViewModel`），**目前零调用方**——正是为此预留的落点。

## 2. 目标 / Non-goals / 反目标

**目标**
1. D1：删除 `feature/settings/build.gradle.kts:22` 的 `implementation(project(":feature:widget"))`。
2. D2：删除 `feature/settings/build.gradle.kts:23-24` 的 `implementation(project(":feature:streaming"))` 及注释。
3. AGENTS.md 债务清单同步修订，D2 完成后仅剩 local→streaming。

**Non-goals**
- local→streaming（HomeScreen/UniversalSearch/MusicViewModel/PlaybackBottomSheet/LocalMusicRepository 5 文件消费，另立 D3）。
- AppSettings→DataStore（B2 专项）、MusicViewModel 减量/EQ store（B1「方案 B」另立项）。
- `Screen.TunerWidget`/`TunerGoSettings` 等零调用方路由的清理（看似死代码勿删；D2 让 TunerGoSettings 获得首个消费者）。

**反目标（出现即失败）**
- D1：任何行为变化。广播 + forceUpdateAll 双刷新路径**原样保留**，仅改走既有缝隙。
- D2：除下述§4 裁定的导航呈现微调外，任何行为变化（GO 设置页功能、onConfigureCurrentProvider 语义、搜索/Labs/初始子路由入口全部保留）。

## 3. 设计

### 3.1 D1：扩展既有 `PlaybackWidgetBridge` 缝隙（服务定位器惯例用法）

B1 设计已裁定 widget 刷新域的缝隙就是 `PlaybackWidgetBridgeInstaller.bridge`（`:core:media` `MediaServiceHooks.kt:45-66`），`:app` 在 `RhythmApplication.onCreate` 安装匿名实现。本设计不新建接口、不新增 DI 形态，只加一个方法：

- `PlaybackWidgetBridge` 接口新增 `fun updateAllWidgets(context: Context)`（无默认实现，唯一实现方在 :app）。
- `GlanceWidgetUpdater`（`:feature:widget`）新增同名 `updateAllWidgets(context)`，逻辑从 WidgetSettingsScreen **原样搬入**（广播块 + forceUpdateAll）——广播目标 `RhythmWidgetReceiver` 与被调方同模块，搬运零成本。
- `:app` 匿名 bridge 实现新增 override，委托 `GlanceWidgetUpdater.updateAllWidgets`。
- `WidgetSettingsScreen.updateAllWidgets` 助手体改为一行 `PlaybackWidgetBridgeInstaller.bridge?.updateAllWidgets(context)`（沿用 MusicViewModel 的 `bridge?` 惯例；framework 层用 `require()`、VM/UI 侧用 `bridge?` 是既有不对称约定）。9 个调用点与 OnboardingScreen 零改动。

**方案取舍**：备选是 B1 PR-8/9 的「新窄接口 + 参数下传」（`WidgetRefresher` 穿透 wrapper + 3 个构造点），为一个单方法需要动 wrapper 签名与两个分支，churn 远大于收益；且 B1 文档明确本域已有缝隙。否决。

### 3.2 D2：GO_SETTINGS 出栈导航（B1 PR-9 equalizer 模式）

wrapper 的 `onNavigateToSubsetting` 已有出栈先例：`route == EQUALIZER → navController.navigate(SettingsRoutes.LOCAL_EQUALIZER)`（:231-232），字面量宿主在 `:core:ui` `SettingsRoutes`。GO_SETTINGS 完全复制该模式：

- `SettingsRoutes` 新增 `const val STREAMING_GO_SETTINGS = "tuner_go_settings"`（镜像 `Screen.TunerGoSettings.route`，注释说明字面量宿主在 :feature:local）。
- wrapper 四处成对编辑（B1 复审重点：两分支漏一处即另一方向崩溃）：
  1. `onNavigateToSubsetting` 新增 GO_SETTINGS 出栈分支；
  2. 平板 master 列表 lambda(:254) 的特殊路由条件加入 GO_SETTINGS——该路径 `routeStack = listOf(route)` 绕过 onNavigateToSubsetting，搜索结果（SettingsScreen.kt:1195 `onNavigateTo(it)`）也走这里；
  3. `LaunchedEffect` 消费初始子路由(:199-204)：pending 为 GO_SETTINGS 时改为出栈导航，不入栈（覆盖 StreamingSettingsScreen 经 `setInitialSettingsSubroute` 的进入路径）;
  4. 删除两处 `GO_SETTINGS -> GoSettingsScreen(...)` 渲染分支。
- 手机端列表/搜索本就走 `onNavigateToSubsetting`（:500），Labs 回调(:361/:528)自动被新分支覆盖，无需另改。

**方案取舍**：备选是 `@Composable` slot 参数（:app 把 GoSettingsScreen 作为可组合 lambda 下传，wrapper 继续内嵌渲染）——零 UX 变化但引入仓库未有的 slot 缝隙形态，且 wrapper 签名 + 2 个 :app 调用点都要改。equalizer 先例证明出栈导航是本仓库对「wrapper 内嵌他 feature 屏」的既定解法，且 `TunerGoSettings` 宿主路由已备好、语义逐字一致。选择出栈导航。

### 3.3 GO_SETTINGS 的全部入口盘点（改造后走向）

| 入口 | 现路径 | 改造后 |
| :-- | :-- | :-- |
| LabsSettingsScreen 回调（平板 :361 / 手机 :528） | `onNavigateToSubsetting(GO_SETTINGS)` 入内部栈 | 新出栈分支 → `tuner_go_settings` |
| SettingsSearch 3 条目（:1360/:1369/:1378） | SettingsScreen `onNavigateTo` → 平板 :254 直写栈 / 手机 :500 onNavigateToSubsetting | 平板条件加入 GO_SETTINGS → 统一出栈；手机不变 |
| StreamingSettingsScreen 初始子路由（AppSettings handoff） | wrapper LaunchedEffect 入栈(:199-204) | pending==GO_SETTINGS 时直接出栈导航 |

## 4. 行为裁定

- **D1**：零行为变化。bridge 在 `Application.onCreate` 安装，早于任何 UI，`bridge?` 恒非空；即便为空也只是不刷新（与 MusicViewModel 现状同语义）。
- **D2**：GO 设置页从「wrapper 内部栈窗格」变为「全屏 push 到 `tuner_go_settings`」。手机端返回语义完全一致（back → 设置主列表）；平板端 detail 窗格呈现变全屏、back 后 detail 重置为 placeholder——与 B1 PR-9 equalizer 出栈导航同款已接受裁定。`onConfigureCurrentProvider` 语义逐字一致（宿主路由已实现同款逻辑）。

## 5. 约束（全程有效）

AGENTS.md 红线全适用：不 push（用户已授权本地 commit）；exported 组件与 manifest 不动；BuildConfig 开关不删；import 按文件内 package 写；禁新增 DI 形态；每步 `:app:compileDebugKotlin`，每 PR assembleDebug + 按模块全量 JVM 单测；删依赖 commit 独立且单独可编译。
