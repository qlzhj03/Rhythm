# D 专项实施计划：D1 widget 缝隙化 + D2 GO 设置出栈导航

> 上游设计：`docs/d-settings-decoupling-design.md`（已批准，2026-11-08）。分支：`refactor/d-pr1-widget-seam` ← main，`refactor/d-pr2-go-settings-exit` ← D1。每步收口 `./gradlew :app:compileDebugKotlin`；每 PR 收口 assembleDebug + 按模块全量 JVM 单测。

## PR-D1：settings→widget

### Task 1: 设计稿与本计划入库
- [x] Step 1: 新建 `docs/d-settings-decoupling-design.md`、本文件
- [ ] Step 2: Commit `docs: add D-phase settings decoupling design and plan`

### Task 2: 缝隙四处改造
- [ ] Step 1: `core/media/.../infrastructure/service/MediaServiceHooks.kt` — `PlaybackWidgetBridge` 新增 `fun updateAllWidgets(context: Context)`（无默认实现）
- [ ] Step 2: `feature/widget/.../glance/GlanceWidgetUpdater.kt` — 新增 `updateAllWidgets(context)`，逻辑从 `WidgetSettingsScreen.kt:675-685` 原样搬入（广播块 + `forceUpdateAll`）
- [ ] Step 3: `app/.../RhythmApplication.kt:116-141` 匿名 bridge 实现 — 新增 override 委托 `GlanceWidgetUpdater.updateAllWidgets`
- [ ] Step 4: `feature/settings/.../WidgetSettingsScreen.kt` — 助手体改为一行 `PlaybackWidgetBridgeInstaller.bridge?.updateAllWidgets(context)`；删 import `AppWidgetManager`/`ComponentName`/`GlanceWidgetUpdater`/`RhythmWidgetReceiver`，增 `PlaybackWidgetBridgeInstaller`
- [ ] Step 5: `./gradlew :app:compileDebugKotlin` 收口
- [ ] Step 6: Commit `refactor: route settings widget refresh through PlaybackWidgetBridge`

### Task 3: 剪断依赖
- [ ] Step 1: `feature/settings/build.gradle.kts` 删除 `implementation(project(":feature:widget"))`（:22）
- [ ] Step 2: `./gradlew :app:compileDebugKotlin`（单独可编译 = 依赖确实剪断）
- [ ] Step 3: Commit `build!: drop :feature:widget dependency from :feature:settings`

### Task 4: 登记与门禁
- [ ] Step 1: AGENTS.md §4 债务清单去掉 settings→widget；设计稿/本计划状态标记 D1 完成
- [ ] Step 2: Commit `docs: mark D1 closed in debt registry`
- [ ] Step 3: `./gradlew assembleDebug` + 全量 JVM 单测（`:app` `:core:common` `:core:media` `:core:ui` `:core:designsystem` `:core:data` `:feature:local` `:feature:streaming`）

## PR-D2：settings→streaming（基于 D1 分支 stacked）

### Task 5: 出栈导航改造
- [ ] Step 1: `core/ui/.../SettingsRoutes.kt` — 新增 `const val STREAMING_GO_SETTINGS = "tuner_go_settings"`（注释镜像 LOCAL_EQUALIZER 先例）
- [ ] Step 2: `SettingsScreenWrapper.kt` 四处成对编辑：
  - `onNavigateToSubsetting`(:231 EQUALIZER 后) 新增 GO_SETTINGS 出栈分支
  - 平板 master 列表 lambda(:254) 特殊条件加入 GO_SETTINGS
  - LaunchedEffect 初始子路由(:199-204) pending==GO_SETTINGS 时出栈导航不入栈
  - 删除两处 `GO_SETTINGS -> GoSettingsScreen(...)` 分支（平板 :363-372 / 手机 :530-539）
- [ ] Step 3: `./gradlew :app:compileDebugKotlin` 收口
- [ ] Step 4: Commit `refactor: exit-navigate GO settings to :app-hosted route`

### Task 6: 剪断依赖
- [ ] Step 1: `feature/settings/build.gradle.kts` 删除 `:feature:streaming` 依赖与注释（:23-24）
- [ ] Step 2: `./gradlew :app:compileDebugKotlin`
- [ ] Step 3: Commit `build!: drop :feature:streaming dependency from :feature:settings`

### Task 7: 登记与门禁
- [ ] Step 1: AGENTS.md 债务清单更新为仅剩 local→streaming；文档状态标记 D2 完成
- [ ] Step 2: Commit `docs: mark D2 closed — only local→streaming remains`
- [ ] Step 3: `./gradlew assembleDebug` + 全量 JVM 单测

## 收尾（需用户明示后执行）

- 向用户呈报两分支 commit 列表与门禁结果；push 与 PR 创建（stacked：D2 base = D1）仅在用户明确同意后执行。
- installDebug 真机冒烟（APK 仅 arm64，无设备时留给用户）。
