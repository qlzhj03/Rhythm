# Rhythm 多模块化重构方案

> 状态：执行中（2026-11-07 启动）。每个 PR 启动前再次确认；每阶段收口时 `./gradlew assembleDebug :app:testDebugUnitTest` 全绿。
> 本文档是权威蓝图；`AGENTS.md` 随各 PR 落地同步修订。

## Context

当前是单 `:app` 模块（378 个 .kt）+ `:macrobenchmark`。重构前确认的核心问题：

- **全局耦合枢纽**：`AppSettings`（6967 行单例，110 个文件 import）、`util/`（400+ 处引用）、`MaterialSymbolIcon`/`RhythmIcons`（111/128 文件）、主题包（53 文件）
- **6 组反向依赖**：`core.domain.scan/backup`→Room 实体、`infrastructure.service.player`→`features.streaming.di`、`shared.presentation`→`features.local`（45 文件）、`util.CrashReporter`→`CrashActivity`、`MusicViewModel`→`MainActivity`/`RhythmGuardTimeoutActivity`、`StreamingMusicViewModel`→`MusicViewModel`（仅 `playNext`/`addSongToQueue` 两个方法以参数传入，解耦成本低）
- **导航成环**：`RhythmNavigation`(2173 行)→`LocalNavigation`(4894 行)→streaming screens；streaming 路由寄居在 LocalNavigation
- **God class**：`MusicViewModel` 10800 行直连 DB/Service/Widget/Activity 四层

已确认决策：**中粒度 ~15 模块**、**AppSettings 整体搬入 :core:data（本次不拆 DataStore）**、**UDF 仅新代码约定**、**分阶段独立 PR**。

技术选型：Hilt、kotlinx.serialization、DataStore（本期仅引入依赖与骨架，AppSettings 拆分另立专项）。此项修订 AGENTS.md 中「不引入 DI 框架」「不新建 Gradle 模块」「不把 Gson 换成 kotlinx-serialization」三条禁令（已获授权）。

---

## 一、目标架构（15 模块 + build-logic）

```
build-logic/          included build：约定插件（AGP 9 内建 Kotlin，绝不声明 kotlin-android）
:app                  4 Activity、RhythmApplication、导航装配（两个 NavHost）、Hilt 组合根
:core:model           纯领域模型：Song/Album/Artist/Playlist、PlayableItem、AppMode、
                      streaming domain 模型、toLibrary* 映射
:core:common          纯 Kotlin 工具（util/utils 的可共享部分）、CrashReporter 接口、
                      NetworkLogRedactor、AppIntentLauncher 等解耦接口
:core:designsystem    主题（ui.theme + festive/）、RhythmIcons/MaterialSymbolIcon、字体、
                      common 基础组件（ExpressiveComponents 等 33 个）、共享资源中心
:core:ui              跨功能业务组件：bottomsheets(22) + dialogs(10) + 根级组件 5 个
:core:data            Room、AppSettings（整体搬入）、MusicRepositoryImpl、PlaybackStatsRepository、
                      MediaScanEngine、StableDateAddedResolver、BackupRestoreManager
:core:network         infrastructure/network 15 文件：NetworkClient + 9 个 API service + NetworkLog
:core:media           MediaPlaybackService、RhythmTileService、player/ 引擎、DSP/EQ、ReplayGain、
                      RhythmAlbumArtProvider、AudioCacheManager
:feature:local        11 屏 + MusicViewModel + LibraryMetadataManager 等 + LocalScreen 路由
:feature:streaming    provider/Subsonic·Jellyfin·网易云、repository、StreamingMusicViewModel、5 屏
:feature:player       3 个播放器屏 + player/ 组件(14) + lyrics/ 组件(4)
:feature:settings     44 个设置屏 + 从 local 迁入的 settings 组件
:feature:widget       glance/ 15 文件 + widget res + 4 个 receiver
:macrobenchmark       保留现状（targetProjectPath :app、benchmark 变体不变）
```

### 依赖规则（模块边界红线）

```
:app → 全部（唯一组合根）
:feature:* → :core:*（禁止 feature→feature；PR-5 审计后如 :feature:settings 仍需 :feature:local，显式声明并记录为已知债务）
:core:data → :core:model / :core:common / :core:network
:core:media → :core:data / :core:model / :core:common / :core:network
:core:network → :core:common
:core:ui → :core:model / :core:designsystem / :core:common
:core:designsystem → :core:common
:core:model / :core:common → 无依赖
```

禁止：`infrastructure`→`features`（由接口倒置替代）、`data`→`presentation`（现状已干净，保持）。

---

## 二、关键设计决策

### 1. Hilt 与 AGP 9 的兼容（PR-1 spike 已验证 ✅）
- 原假设「Hilt Gradle 插件依赖 `org.jetbrains.kotlin.android`、AGP 9 下必挂」**已被证伪**：反编译确认插件 2.60.1 只依赖 `com.android.base` 标记 + AGP Variant API，不再触碰 kotlin-android 插件。
- **spike 结论（2026-11-07）**：`com.google.dagger.hilt.android` 官方插件 + KSP 在 AGP 9.4.1 内建 Kotlin 下完整可用——插件正常应用、`@HiltViewModel`/`@Module @InstallIn`/`@Inject` 代码生成成功、`hilt_aggregated_deps` 聚合产物与 proguard META-INF 规则正常产出。
- 注意：`@HiltAndroidApp` 只能出现在 application 模块（处理器强制校验），即只能在 :app 使用；库模块用 `@HiltViewModel` / `@Module @InstallIn` / `@Inject`。
- 技术细节：AGP 9 内建 Kotlin 的 Kotlin 编译器选项挂在 **project 级 `kotlin` 扩展**（`KotlinAndroidExtension`）；`:app` 脚本里的 `android { kotlin { … } }` 只是词法嵌套，实际解析到同一扩展；且该扩展在插件应用完成后才注册，约定插件需在 `afterEvaluate` 中配置。AGP 9.4.1 的 `CommonExtension` 已去泛型化，且不再提供 `defaultConfig{}` 等 Action 风格重载（二进制插件中须用属性式赋值）。
- Version Catalog 新增：`hilt-android`、`hilt-compiler`、`androidx-hilt-navigation-compose`、`kotlinx-serialization-json`、`retrofit-converter-kotlinx-serialization`、`datastore-preferences`（datastore 本期仅入 catalog）。

### 2. 资源策略（避免拆 3285 条 strings）
- `:core:designsystem` 作为**单一资源中心**：strings.xml（中英 3285 条整体搬移，不拆分）+ 通用 drawable + font + dimens。`scripts/check_plural_specifiers.py` 路径同步更新。
- widget 专属 res（4 个 widget_info xml、4 个 preview layout、preview drawable、widget_colors）→ `:feature:widget`；应用主题 themes.xml、values-night/ → `:app`；res/xml 配置（network_security_config 等 5 个）→ `:app`。
- `MaterialSymbolIcon` 依赖的 `res/font/material_symbols_outlined.ttf` 随图标进 designsystem。

### 3. 包名映射（搬移即改名，机械替换由编译驱动）
- `shared.data.model`（模型部分）→ `chromahub.rhythm.app.core.model`；AppSettings → `core.data.settings`
- `chromahub.rhythm.app.network`（现物理在 infrastructure/network/）→ `chromahub.rhythm.app.core.network`
- `chromahub.rhythm.app.ui.theme` → `core.designsystem.theme`（物理目录与包名自此一致）
- 同步更新 proguard-rules.pro 中按旧包名的 keep（`shared.data.model.**`、`network.**`、Room entity 包等）

### 4. 解耦接口清单（消除反向依赖的关键）
| 接口 | 定义位置 | 实现位置 | 替代的反向依赖 |
|---|---|---|---|
| `NetworkSettingsProvider` | :core:network | AppSettings（:core:data） | `NetworkClient.initialize(AppSettings)` 直连 |
| `StreamingPlaybackResolver` / `LocalQueueController` / `WidgetRefresher` / `AppIntentLauncher` | :core:media / :core:common | :feature:streaming / MusicViewModel / :feature:widget / :app | PreloadController·RhythmPlayerEngine→StreamingMusicModule；StreamingMusicViewModel→MusicViewModel；MusicViewModel→WidgetUpdater·MainActivity |
| `CrashReporter` 启动逻辑 | :core:common（接口） | :app（注入 CrashActivity starter） | util.CrashReporter→CrashActivity |

### 5. 导航归位
- `RhythmNavigation.kt` + `LocalNavigation.kt` 移入 `:app`（组合根可见全部 feature，环自然消除）。两个 NavHost 结构不变。
- 各 feature 导出 `XxxRoutes`（字符串常量 object，沿用字符串路由，不引入 @Serializable）。`LocalScreen.kt`/`MainScreen.kt` 拆归对应模块。
- `UniversalSearchScreen`/`RhythmStatsScreen` 按 import 归属放 `:feature:local`（在 LocalScreen 路由表内）。

### 6. 构建配置继承
- Convention plugin 固化：compileSdk/targetSdk 37、minSdk 26、jvm 21、desugaring、Material3 Expressive opt-in、Compose BOM 2026.09.00 + material3 1.5.0-beta01 + foundation 1.12.1 pin、禁 lint（沿用现配置）。
- `gradle.properties` 本期**不动**（含 `org.gradle.parallel=false`——多模块下并行构建收益明显，PR-7 列为需用户确认的调优项）。
- `kotlin-parcelize` 用 `id("kotlin-parcelize")` 直接加（:core:model 等需要 Parcelable 的模块），路径与现 :app 相同。

---

## 三、分阶段实施（7 个 PR）

### PR-1：基建（build-logic + catalog + AGENTS.md 修订）
- 新建 `build-logic/`（`rhythm.android.library`、`rhythm.android.library.compose`、`rhythm.android.application`、`rhythm.hilt` 约定插件），settings.gradle.kts `includeBuild`。
- catalog 增补 hilt/serialization/datastore 条目。
- **Hilt spike**：空模块 + `@HiltAndroidApp` 最小验证。
- 修订 AGENTS.md（新模块清单、依赖方向、删除三条被授权推翻的禁令）。
- 验收：`:app` 行为不变，assembleDebug + 单测绿。

### PR-2：叶子模块 `:core:model` + `:core:common` + `:core:designsystem`（已完成 ✅ 2026-11-07）
> 执行偏差（已回写）：`PlaybackQualityInfo`/`FestiveOverlay`/`FestiveThemeExamples`/`MarqueeText`/`RhythmButtonGroup`/`CollapsibleHeader` 等 R/AppSettings/模型依赖文件留守 `:app`（PR-3/5 归位）；`AudioFormatDetector` 因引用 Song 推迟；`HapticUtils` 经 `HapticsGate` 缝隙解耦（:app 启动时注入）；所有新模块 namespace=`chromahub.rhythm.app` + `android.nonTransitiveRClass=false`（R 引用零改动）；歌词/队列测试随声明归属留在 :app。
- model：core/domain/model + shared/data/model 纯模型 + streaming/domain/model + StreamingModelMappers.kt 的 toLibrary*（约 25 文件）。
- common：util/utils 中**纯工具**（依赖 AppSettings 的 M3ImageUtils/HapticUtils/LyricsParser 等留待 PR-3 随 AppSettings 同批）+ CrashReporter 接口化。
- designsystem：theme 14 文件 + icons 2 文件 + font + UiConstants + ScrollStateUtils + components/common 33 文件 + strings/drawable 资源中心。
- 全仓 import 机械替换（theme 53 文件、icons 100+ 文件、模型若干）；proguard keep 同步。
- 测试搬移：VersionComparatorTest、NetworkLogRedactorTest、AudioFormatDetectorTest、ArtistSeparatorTest、EnvelopeTest、RomanizationParsingTest、歌词 TTML/ELRC、QueueReorder/ShuffleState、ThemeSchemeTest、ExpressiveBottomButtonsTest 等。
- 验收：编译 + 单测绿 + installDebug UI 冒烟（主题/图标/字体渲染）。

### PR-3：数据与网络 `:core:network` + `:core:data`（已完成 ✅ 2026-11-07）
> 执行偏差（已回写）：**零 import churn**——AppSettings/Room/仓储全部保留原包名（`shared.data.model`/`features.local.data.*`）；NetworkSettingsProvider 接口 + NetworkFeatureFlags + NetworkBuildInfo + AppWorkerRegistry + AppStrings 五个缝隙由 :app 启动注入（BuildConfig.VERSION_NAME/DEBUG/APPLE_MUSIC_FALLBACK_TOKEN/worker 类与 work name 均不得复制到 :core:*，红线遵守）；MediaScanEngine/StableDateAddedResolver(:app util 依赖)、BackupRestoreManager(R.string)、MusicRepositoryImpl+LocalMusicModule(PR-5 归 :feature:local)、LyricsParser/CacheManager/RhythmLyricsParser(RhythmApplication/coil 依赖) 留守 :app。
- network：infrastructure/network 15 文件整体迁入 + NetworkSettingsProvider 接口替代 `NetworkClient.initialize(AppSettings)`（过渡期 RhythmApplication 手动注入实现）。
- data：Room（database/dao/entity）+ **AppSettings 6967 行整体搬入**（110 文件 import 脚本化批量修正）+ MusicRepositoryImpl + PlaybackStatsRepository（63 处引用）+ MediaScanEngine + StableDateAddedResolver + BackupRestoreManager + 依赖 AppSettings 的 util 收尾。
- 消除 2 组反向依赖：core.domain→Room、util→AppSettings 就此落地。
- 测试搬移：StableDateAddedResolverTest、StatsTimeRangeTest 等；MockWebServer 网络类测试改放 :core:network。
- 验收：编译 + 全部单测绿（测试源集随模块走）+ 扫库/设置/搜索冒烟。

### PR-4：播放核心 `:core:media`
- 迁入：service 13 文件（MediaPlaybackService、RhythmTileService、player/ 引擎全家）+ audio DSP 4 文件 + replaygain + RhythmAlbumArtProvider + RhythmBitmapLoader + MediaUtils/AudioArtworkFetcher。
- 解耦：定义 `StreamingPlaybackResolver`（替代 PreloadController/RhythmPlayerEngine 对 StreamingMusicModule/UserTrustManager 的 import）、`AppIntentLauncher`（替代 MusicViewModel→MainActivity）、`WidgetRefresher`。组件 manifest 声明随库 merge（**10 个 exported 组件属性一字不改**——红线）。
- 测试搬移：播放器 9 个测试类 → :core:media。
- 验收：编译 + 单测绿 + **播放冒烟**（后台播放、锁屏/蓝牙控制、QS tile、媒体路由、EQ/ReplayGain）。

### PR-5：UI 层 `:core:ui` + 5 个 feature 模块 + 导航归位
- :core:ui：bottomsheets 22 + dialogs 10 + 根级组件 5（AudioQualityBadges、MediaScanLoader、PermissionHandler 等）。
- :feature:local：11 屏 + 13 组件 + MusicViewModel（10800 行原样搬移，构造/调用点改为注入接口）+ viewmodel 辅助 4 文件。
- :feature:streaming：provider 3 家 + repository + StreamingMusicViewModel（`playNext(song, localViewModel)` 改接 `LocalQueueController`）+ 5 屏 + notification。
- :feature:player：3 屏 + player 组件 14 + lyrics 组件 4（消费者仅 player 屏，不放 :core:ui）。
- :feature:settings：44 屏 + 把被 25 个设置屏 import 的 local settings 组件（HomeSectionOrderBottomSheet 等）迁入，消掉 shared→features.local 反向依赖的大头；剩余依赖审计后显式声明。
- :feature:widget：glance 15 文件 + widget res + receiver manifest。
- 导航：RhythmNavigation + LocalNavigation → `:app/navigation/`；feature 导出 Routes 常量。
- 测试搬移：Discover/Explorer/ArtistArtwork 等剩余测试类归位。
- 验收：编译 + 全量单测绿 + 全功能冒烟（本地/流媒体/设置/小组件/搜索/歌词/外链播放/Crash 流程）。

### PR-6：Hilt 全面接线
- `@HiltAndroidApp`（RhythmApplication 瘦身：删 NetworkClient.initialize，保留 CrashReporter/Coil ImageLoaderFactory/ANR watchdog）。
- `@AndroidEntryPoint` ×4 Activity；`@HiltViewModel` ×5（MusicViewModel 10800 行构造注入为机械改动，单独评审）。
- LocalMusicModule/StreamingMusicModule → `@Module @InstallIn`；Multibinding 实现 `StreamingProviderRegistry`；过渡期手动注入点全部替换为 @Inject。
- AppSettings.getInstance() 调用点**不改**（用户已选整体搬入方案）。
- 验收：编译 + 单测绿 + 全功能冒烟 + **assembleRelease 一次**（验证 proguard × Hilt）。

### PR-7：kotlinx.serialization + 收尾
- :core:network Gson → kotlinx.serialization（15 文件 DTO、9 个 ApiService 注解、converter 替换、proguard 规则）。
- **baseline profile 重生成**：包名全变后旧 35272 行条目已失效（不崩溃但性能回退）——需 arm64 真机/模拟器跑 macrobenchmark BaselineProfileGenerator。
- AGENTS.md 终稿 + wiki 过时标注；与用户确认后重估 `org.gradle.parallel=false`。
- 验收：`./gradlew assembleDebug :app:testDebugUnitTest` + `assembleRelease` + installDebug 冒烟 + （可选）benchmark 变体跑通。

---

## 四、验证方案

- **每个 PR 的门禁**：`./gradlew assembleDebug :app:testDebugUnitTest`（不用裸 `test`）。
- **冒烟清单**（PR-4/5/6 各跑一次，arm64 设备 `installDebug`）：本地播放/后台/锁屏、流媒体登录与播放、4 个 widget、QS tile、设置页全量、歌词、外链 VIEW、CrashActivity。
- **发布链路**：PR-6/7 各跑一次 `assembleRelease` 验证 proguard keep 与新包名一致；APK 命名规则不变。
- **性能回归**：PR-7 重生成 baseline profile 后跑 StartupBenchmarks 对比。
- 本期 strings 只搬不改，占位符校验脚本跑一次确认路径可用即可。

## 五、风险与回退

| 风险 | 缓解 |
|---|---|
| Hilt Gradle 插件与 AGP 9 内建 Kotlin 冲突 | ~~原假设~~ **PR-1 spike 已证伪**：插件 2.60.1 与 AGP 9.4.1 兼容，官方插件可用；后续 PR 直接采用 |
| 110 文件 AppSettings import + 100+ 文件 theme/icons import 替换遗漏 | 脚本化批量替换 + 编译驱动逐模块清零 |
| proguard keep 旧包名导致 release 混淆破坏 | 每次 PR 同步更新；PR-6/7 强制 assembleRelease 验证 |
| baseline profile 失效性能回退 | PR-7 重生成；期间可接受 |
| 10.8k 行 MusicViewModel 搬移/注入出错 | 原样搬移不重写逻辑；构造注入单独 PR、单独评审 |
| exported 组件/平台契约破坏 | 组件声明只搬不改，manifest diff 逐项核对（AGENTS.md 红线） |
| 每阶段周期内 main 不可构建 | 分阶段 PR 本身保证每阶段收口时全绿 |
