# P4 导航归位设计稿（2026-11-08）

## 1. 背景与范围

主计划 §5 的「导航归位」四项动作已在 multi-module PR-5（2026-11-07）完成：两个导航文件移入 `:app`、feature 导出 Routes 常量、字符串路由维持。但 :app 组合根内积欠两块体量债，且 D3 实施时为避免 churn 又新增了重复与漂移（D3 设计稿 §8 已预告「加重 P4 导航债务」）。本专项处理**残留债务**：文件巨石化、死路由表、重复注册、路由词汇漂移。

**主计划既定不变式**：字符串路由（禁 @Serializable）、双 NavHost 结构不变、`:app` 持有导航装配（组合根可见全部 feature）。

## 2. 现状事实（2026-11-08 勘测）

### 2.1 文件与体量

| 文件 | 行数 | 问题 |
| :-- | --: | :-- |
| `app/.../navigation/LocalNavigation.kt` | 4,886 | 内部 `LocalNavigationContent` 单个 composable 3,750 行、54 参数、49 个 route 注册 + 尾部对话框簇 |
| `app/.../navigation/RhythmNavigation.kt` | 2,152 | 其中 `RhythmGuardWarningHost` + 10 个 helper + 气泡/时间选择器 ≈ **1,620 行**（274–1894）是听力守护功能体，与路由装配无关；另有 ~260 行死代码（ModeSelectorScreen/ModeCard/CompactModeSwitcher，全仓零调用） |
| `feature/local/.../navigation/LocalScreen.kt` | 79 | **死文件**：`LocalScreen` 全仓零引用 |
| `app/.../navigation/MainScreen.kt` | 35 | **死文件**：`MainScreen` 全仓零引用；根 NavHost 实际用裸字面量 `"main"`/`"settings"` |

### 2.2 已核实的缺陷清单

1. **`Screen.Equalizer` 双重注册**：LocalNavigation.kt:2245–2273 与 2416–2444 逐字节相同的两个 `composable()` 注册（D3 遗留）。
2. **幻影路由**：`"streaming_rhythm_stats"` 被 SettingsScreenWrapper.kt:217 `findNode` 探测，但全仓无任何 composable 注册它（流媒体时代遗留）。
3. **Player 非页面**：`Screen.Player` 路由渲染空 `Box`（LocalNavigation.kt:3625–3633），却是 14+ 处 `navigate(Screen.Player.route)` 的目标（播放器实际是 sheet）。这是既有机制——**只登记，不改**。
4. **死接线**：`RhythmNavigation.navigateToSettingsTrigger`（:130–148）恒为默认值 `false`，唯一调用方 MainActivity 不传参。
5. **冗余 import**：LocalNavigation.kt:223–226 四个 `// Redundant, but ensuring it's there` 重复 import。
6. **词汇漂移**：SettingsRoutes 键（`"notifications_settings"`）↔ 内层图路由（`"tuner_notifications_settings"`）两套词汇，靠 :app 手写 when 映射（LocalNavigation.kt:2154–2181、2305–2320）+ wrapper 内联 when（SettingsScreenWrapper.kt:340+）人工对齐；`SettingsRoutes` 中 5 个常量本身就是内层路由字面量（`LOCAL_EQUALIZER="equalizer"` 等），另一些不是。
7. **StreamingRoutes 私有于 :app**：4 个常量 + 4 个 builder（LocalNavigation.kt:234–249），仅本文件 ~20 处引用；:feature:streaming 自身没有任何路由常量出口——不符合主计划「各 feature 导出 XxxRoutes」。
8. **同字面量双图**：`"settings"` 既是根 NavHost 目的地（RhythmNavigation.kt:224）也是内层 `Screen.Settings`（LocalNavigation.kt:2103），渲染同一 wrapper。
9. **模式分支重复**：RhythmNavigation.kt:200–208（STREAMING）与 210–218（else）对 `LocalNavigation(...)` 的调用逐字节相同（模式实际在 LocalNavigation 内部读 `appSettings.appMode` 处理，:876）。
10. **回退导航跨界**：内层图 lambda 在 `popBackStack()` 失败后 navigate 到根路由 `"main"`（LocalNavigation.kt:2384）。
11. **导航设置字符串**：OnboardingScreen.kt:3942/4021 直接写裸字符串 `"home"`/`"library"` 进 AppSettings.defaultScreen，LocalNavigation.kt:449–452 读它决定 startDestination。

### 2.3 非缺陷（登记不动）

- `SettingsScreenWrapper` 内部 routeStack 状态机是**第三套导航机制**（含独立 BackHandler 与 4 个特殊路由分流）——与 NavHost 并存是有意取舍（平板 master-detail），收敛它属高风险，本轮不动。
- 跨图路由经 `AppSettings.setInitialStreamingRoute/consumeInitialSettingsSubroute` 字符串交接——机制丑但是 D 链/B1 的既定裁定，不动。
- `:core:ui` 9 个文件携带 `features.local.presentation.components.*` 包名（split package）——C 阶段既定格局，非本专项。
- EqualizerScreen.kt（1,478 行）编译在 `:feature:settings`（包名 `features.local.presentation.screens`）——**归入 P3**（P3 会重度触达该文件，避免两次 churn）。

## 3. PR 切片

### PR-P4-1：死物清理（小步先行，B2-1 模式）

1. 删 `LocalScreen.kt`、`MainScreen.kt`（零引用）。
2. 删 `Screen.Equalizer` 第二处注册（2416–2444），保留第一处。
3. 删 RhythmNavigation 死代码：`ModeSelectorScreen`（1895–1955）+ `ModeCard`（1956–2063）+ `CompactModeSwitcher`（2064–2152）。
4. 删 `navigateToSettingsTrigger`/`onSettingsNavigationComplete` 死参数（MainActivity 唯一调用方同步去掉实参位置——签名收敛，零行为变化）。
5. 删 LocalNavigation.kt:223–226 冗余 import。
6. 幻影路由处置：删除 `"streaming_rhythm_stats"` 探测分支（SettingsScreenWrapper.kt:217）——该 findNode 永远返回 null，删除是零行为变化；若产品本意是从设置跳 stats，走既有 `RHYTHM_STATS` 通道（已存在于 wrapper 的特殊分流列表）。
7. `Player` 空 Box 路由保留，加注释说明「sheet 锚点，勿删」。

### PR-P4-2：RhythmGuardWarningHost 出栏

`RhythmGuardWarningHost` + 8 个 rhythmGuard* helper + `RhythmGuardResumeCountdownBubble` + `RhythmGuardTimePickerDialog` + 音量助手（RhythmNavigation.kt:274–1894，≈1,620 行）整体搬入新文件 `app/.../rhythmguard/RhythmGuardWarningHost.kt`。RhythmNavigation.kt 降为 ~530 行纯导航装配。**零行为变化**：纯剪切 + import 调整；RhythmGuard 相关符号（AppSettings 流、广播）本就模块内可见。

### PR-P4-3：LocalNavigationContent 拆解

3,750 行单 composable 按已核实的自然边界切为同包 4 个文件（全部留在 :app，:app 纯移动零行为变化）：

| 新文件 | 内容 | 预计行数 |
| :-- | :-- | --: |
| `LocalNavigation.kt`（瘦身） | `LocalNavigation()` 壳 + `LocalNavigationContent()` 的 Scaffold/导航栏/sheet chrome + 声明 | ~1,300 |
| `LocalNavigationRoutes.kt` | NavHost 的 49 个 `composable()` 注册：17 个大路由块原样搬出为 `@Composable Route(xxx)` 顶层私有函数，注册处一行调用 | ~1,900 |
| `LocalNavigationStreaming.kt` | `StreamingRoutes` object + 4 个 streaming→local mapper（251–330）+ 3 个流媒体详情大路由 | ~1,100 |
| `LocalNavigationRail.kt` | Rail 三件套（4568–4886） | ~330 |

方法约束：**只做剪切**——54 参数的参数表不动、回调闭包不动、`remember`/状态声明留在使用处；路由块提取时参数只传 `(navController, viewModel, …)` 现场所需，避免改语义。`StreamingRoutes` 保持 private（仅 :app 消费）。

### PR-P4-4：StreamingRoutes 出口归位（主计划 §5 收尾）

`StreamingRoutes`（4 常量 + 4 builder）移至 `:feature:streaming` 导出为 public `StreamingRoutes.kt`（沿用 SettingsRoutes 先例：feature 出口常量，宿主 :app 导入注册）。4 个 streaming→local mapper 留 :app（Song 模型映射属宿主装配逻辑）。

### 裁定点（默认不做的，需要时另立）

- **路由词汇完全统一**（两套词汇并一）：牵动 SettingsSearch.kt（2,964 行 ~50 处引用）与 wrapper 状态机键空间，churn 巨大且无行为收益——本轮只在 `Screen.kt` 内把 1:1 对应的 Tuner 别名继续走「常量引用 SettingsRoutes」模式（现有 3 处先例），不合并字符串值。
- **第三套导航机制收敛**：不动（§2.3）。

## 4. 门禁

- 每步：`./gradlew :app:compileDebugKotlin`
- 每 PR 收口：assembleDebug + 8 模块全量 JVM 单测
- 每 PR 树差异断言：P4-1 净删除、P4-2/P4-3 净迁移（git diff --stat 核对剪切量与搬入量一致）
- 真机冒烟（P4-1/P4-3 后各一轮）：平板+手机双形态走查 home/library/search/stats/settings 五 tab、详情页三层 push、均衡器与 GO 设置出栈导航、模式切换重启路径、听力守护弹窗（P4-2 后）

## 5. 不动清单

- 双 NavHost 结构、字符串路由、无 @Serializable
- SettingsScreenWrapper routeStack 机制全貌
- AppSettings 字符串交接路由（setInitialStreamingRoute 系）
- OnboardingScreen 写 `"home"/"library"` 字符串的既有行为（仅在 P4-1 注释登记）
- Player sheet 机制与空 Box 路由
