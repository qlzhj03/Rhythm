# AGENTS.md — Rhythm

AI 编码代理上下文文件。只写「读代码无法可靠推断」的项目约定、可直接执行的命令，以及会破坏本项目的反模式。

**事实来源优先级：** 源码 / `app/build.gradle.kts` > `gradle/libs.versions.toml` > 本文件 > `wiki/`。
`wiki/` 是历史快照（14 个 `.md`），已确认存在过期内容（提到的 CI workflow / `.githooks` 不存在、product flavor 已移除）。仓库已 `git init`（默认分支 `main`，尚无提交），没有 `.github/`、`.githooks/`；根目录有 `README.md`；`docs/multi-module-refactor-plan.md` 是多模块拆分的权威蓝图（2026-11 启动，PR-1~6 已落地，PR-7 收尾）。

## 1. 项目定位与技术基线

Android 音乐播放器：本地曲库（MediaStore + Room）+ 自托管流媒体（Subsonic/Navidrome/Jellyfin、网易云等）+ 歌词 + Glance 小组件 + 均衡器/ReplayGain，Material 3 Expressive。

- 工具链：Kotlin 2.4.21（K2）· AGP 9.4.1 · Gradle 9.7.1（wrapper）· KSP 2.3.6 · JDK/JVM 21。
- **不要添加 `org.jetbrains.kotlin.android` 插件**：AGP 9.x 内建 Kotlin 支持，加了会直接构建失败（`:app` 与 `:macrobenchmark` 都不行）。
- SDK：`minSdk = 26`、`targetSdk = 37`、`compileSdk = 37`；coreLibraryDesugaring 开启。
- Compose BOM `2026.09.00`；`material3` 显式钉在 `1.5.0-beta01`；`foundation-layout` / `animation` / `animation-core` 显式钉在 `1.12.1`。这些 pin 是刻意的，不要「顺手」去掉版本或交给 BOM。
- 编译期已全局 opt-in `ExperimentalMaterial3Api` / `ExperimentalMaterial3ExpressiveApi`（`freeCompilerArgs`），新文件不需要逐处 `@OptIn`；新增 Experimental API 加到 `app/build.gradle.kts`（:app）或 `build-logic` 的 compose 约定插件（库模块）。
- 依赖必须经 Version Catalog（`gradle/libs.versions.toml`）；不要在 `build.gradle.kts` 写死 `group:artifact:version`。
- 异步模型：Coroutines + Flow（`StateFlow` 为主）；没有 LiveData / RxJava，不要引入。
- 图标：`res/font/material_symbols_outlined.ttf` 变量字体 + `MaterialSymbolIcon` / `RhythmIcons`；禁止 `Icons.Default.*`、`androidx.compose.material3.Icon` 配 `Icons.*`、`material-icons-extended`。
- 禁止新增 XML 布局；`res/layout/` 的 4 个 `widget_preview_*.xml` 只服务 Glance 小组件预览。
- 构建系统：Gradle Kotlin DSL + Version Catalog + `build-logic/` included build（约定插件 `rhythm.android.library` / `rhythm.android.library.compose` / `rhythm.android.application` / `rhythm.hilt`）。多模块拆分进行中：`:app` 构建逻辑仍集中在 `app/build.gradle.kts`（尚未迁移到约定插件），新库模块一律用约定插件，不要复制 :app 的手写配置。

## 2. 构建与验证命令

| 用途 | 命令 |
| :-- | :-- |
| 日常安装 debug | `./gradlew :app:installDebug` |
| 最快编译检查 | `./gradlew :app:compileDebugKotlin` |
| 全部 JVM 单测（36 个测试类，分属 7 个模块） | `./gradlew :app:testDebugUnitTest :core:common:testDebugUnitTest :core:media:testDebugUnitTest :core:ui:testDebugUnitTest :core:designsystem:testDebugUnitTest :feature:local:testDebugUnitTest :feature:streaming:testDebugUnitTest` |
| 单个测试类 | `./gradlew :core:media:testDebugUnitTest --tests "chromahub.rhythm.app.util.VersionComparatorTest"`（测试在哪个模块就跑哪个模块的任务） |
| 构建 debug APK | `./gradlew assembleDebug` |
| 构建 release（仅发布前） | `./gradlew assembleRelease` |
| 复数占位符校验（改 strings 后） | `python3 scripts/check_plural_specifiers.py` |
| 提交前最小集合 | `./gradlew assembleDebug` + 上表「全部 JVM 单测」一行 |

- **不要用裸 `./gradlew test`**：会连带 release / benchmark 变体，慢且容易因环境失败；始终按模块指定 `:module:testDebugUnitTest`。PR-5 起 :app 只剩 1 个测试类（RhythmBitmapLoaderTest），只跑 `:app:testDebugUnitTest` 不再覆盖全量单测。
- 仓库没有 CI（无 `.github/workflows/`），单测只能本地跑；`wiki/Build-Instructions.md` 里的 workflow 描述已过时。
- 设备要求：APK 只含 `arm64-v8a`（`defaultConfig.ndk.abiFilters`），x86_64 模拟器装不上；instrumentation / macrobenchmark 需要 arm64 真机或 arm64 模拟器。
- `app/src/androidTest/` 不存在（`app/build.gradle.kts` 仍保留 4 条 `androidTestImplementation` 依赖，但没有源码）。
- 产物：APK 命名 `Rhythm-{versionName}-{buildType}.apk`（如 `Rhythm-5.5.500.1306 Beta-debug.apk`），输出到 `app/build/outputs/apk/<variant>/`，单 APK 不做 ABI 拆分；versionName 含空格，引用时加引号。AAB 仅 release：`app/build/outputs/bundle/release/app-release.aab`。
- `lint` / ktlint / detekt 是**刻意禁用**的（`app/build.gradle.kts` 里 `lint { abortOnError = false; checkReleaseBuilds = false }` 且禁用所有 `lint*` 任务；根构建脚本注释说明为 AI 工作流移除格式化门禁）。不要恢复，也不要以 lint 输出作为改动依据。
- `gradle.properties` 按 16 GB 内存机器调优：`org.gradle.parallel=false`、`workers.max=4`、configuration cache 开启、堆上限固定；不要改。

## 3. Do NOT（红线）

- **凭据**：不提交 / 输出 `local.properties`、`keystore.properties`、`*.jks`、`private.properties*` 的内容，也不把 `APPLE_MUSIC_FALLBACK_TOKEN` 复制到新文件、文档、日志或提交信息。
  - 签名读取的是**项目根目录 `keystore.properties`**（`getProperties("keystore.properties")`），`store_file` 相对 `rootProject` 解析；文件缺失时 release / debug / benchmark 回退 debug 签名。
  - `.config/` 目录当前不存在，也不是签名路径；根目录 `rhythm_keystore.jks` 存在但已被 `.gitignore` 忽略。
- **版本**：不在 `libs.versions.toml` 之外指定依赖版本；不在 `build.gradle.kts` 硬编码版本；不自行升级 Kotlin / AGP / Compose BOM / Material3。
- **结构**：不为新页面建 Activity、不引入 Fragment（`MainActivity` 是唯一 Compose 宿主，辅助 Activity 仅 `CrashActivity` / `ExternalPlaybackActivity` / `RhythmGuardTimeoutActivity`）；不新增 product flavor；不新增第三种语言目录。DI：Hilt 已全面接线（PR-6，见 §5）。**不要**再手写新的单例工厂对象。JSON：`:core:network` 已于 PR-7 迁到 kotlinx.serialization（DTO @Serializable + retrofit converter-kotlinx-serialization）；其余模块（:core:data AppSettings、:core:designsystem ColorExtractor、catalog 缓存、备份等）仍用 Gson——迁移范围刻意限定在 :core:network，勿全仓散改。
- **Manifest**：`exported=true` 共 10 个组件，其 `exported` 与 intent-filter 是对外协议（Android Auto / 系统 / 启动器 / 小组件 / 其他应用依赖），勿移除、勿额外加 permission（`RhythmTileService` 已有的 `BIND_QUICK_SETTINGS_TILE` 除外）。PR-5 起组件声明随类所在模块拆分：4 个 Activity 在 `:app`；`MediaPlaybackService` + `RhythmAlbumArtProvider` 在 `:core:media` 的 manifest；4 个 widget receiver + `RhythmTileService` 在 `:feature:widget` 的 manifest（manifest merge 拼回同一份最终清单，属性一字不改）。cleartext + user CA 放行（`res/xml/network_security_config.xml`）是自托管 Navidrome/Jellyfin 的既定取舍，勿收紧。
- **生成物与敏感区**：不改 `build/`、`app/build/generated/`、`app/src/main/baseline-prof/`；新增 `.github/workflows/`、`.githooks/` 或改动 `scripts/prepare_release.py` / `scripts/generate_release_notes.py` 前先询问（两者交互式，会改版本号并写 changelog / fastlane，后两者当前不存在）。
- **Gradle**：只用 `./gradlew`，不用系统 `gradle`；不改 `gradle-wrapper.properties`；不动 `gradle.properties` 的内存与并发调优。
- **Git**：仓库已 `git init`（默认分支 `main`，尚无提交）；未经明确要求，不执行 `commit` / `push` / `tag`。
- **功能开关**：不删除 `BuildConfig.ENABLE_*`、`FLAVOR="github"`、`IS_NIGHTLY`、`RELEASE_DATE` 或 `APPLE_MUSIC_FALLBACK_TOKEN` 相关逻辑，即使看起来像死代码。

## 4. 工程结构与模块边界

PR-5 后共 **11 个 Gradle 模块** + `:macrobenchmark` + `build-logic/`。模块物理路径：`core/<name>`、`feature/<name>`、`app/`。**所有模块 namespace 复用 `chromahub.rhythm.app`，且搬移文件保留原 package（零 import churn，沿用 PR-3 策略）**——所以「文件在哪个模块」与「package 是什么」相互独立，import 一律按 package 写。

| 模块 | 职责（物理目录 → package 另见下表） |
| :-- | :-- |
| `:app` | 组合根：4 个 Activity、`RhythmApplication`（缝隙安装）、导航装配（`navigation/`：`RhythmNavigation` / `LocalNavigation` / `MainScreen`，package 仍为旧路径）、`onboarding/`（OnboardingScreen 三件套）、`PermissionHandler`、`FestiveOverlay`/`FestiveThemeExamples`、`AppModeViewModel`（零引用遗留）、`RhythmBitmapLoader`、2 个 worker、`ProfileImportExportUtils` |
| `:core:model` | 纯领域模型（`core.model.*`）+ `ExtendedSongInfo`（PR-5 自 bottomsheets 抽出） |
| `:core:common` | 纯工具（`core.common.*`）+ 缝隙对象（`AppBuildInfo`/`AppStrings`/`HapticsGate`/`LyricsSettingsGate`/`RhythmGuardRisk`）+ `ArtistSeparator`/`LrcUtils`/`SemanticLyrics`/`DragDropUtils`/`AudioQualityDetector` 等 |
| `:core:designsystem` | 资源中心（strings 中英 3285 条/drawable/font/theme）+ theme + icons + 基础组件；`formatDuration` 在 `.../shared/presentation/components/player/TimeFormat.kt`（包名未改） |
| `:core:network` | NetworkClient + 9 个 API service + `NetworkSettingsProvider`/`AppWorkerRegistry`/`NetworkBuildInfo` 缝隙（package `chromahub.rhythm.app.network`） |
| `:core:data` | Room + **AppSettings（整体，包名仍 `shared.data.model`）** + `MusicRepository` 接口（`core.domain.repository`）+ `BackupRestoreManager`（`core.domain.backup`）+ `StableDateAddedResolver` + 仓储；依赖 `:core:designsystem`（BackupRestoreManager 的 R.string） |
| `:core:media` | 播放核心：MediaPlaybackService、player/ 引擎、DSP/EQ/ReplayGain、`RhythmAlbumArtProvider`、`AudioArtworkFetcher`（coil keyers 同文件）、`MediaUtils`（3698 行）、`MetadataHeuristics`、`LyricsParser`/`RhythmLyricsParser`、`CacheManager`、`QueueUtils`、`AudioFormatDetector`、`AudioDeviceManager`、`AutoEQ*`、缝隙（`MediaServiceHooks`/`PlaybackWidgetBridge`/`PlaybackResolverInstaller`/`LocalQueueController`）；manifest 携带 MediaPlaybackService + RhythmAlbumArtProvider 组件声明 |
| `:core:ui` | 跨功能共享 UI（包名保持 `shared.presentation.components.*` / `shared.presentation.screens.settings`）：bottomsheets 20 + dialogs 9 + `Material3SettingsGroup`/`AudioQualityBadges` + common 组件 12（MarqueeText/CollapsibleHeader/M3Placeholders/占位图等）+ 设置原语（`TunerAnimatedSwitch`/`SettingsSearchBar`/`SettingsRoutes`/`smartPlaylistDisplayName`）+ `ImageUtils`/`M3ImageUtils` + **`AppUpdaterViewModel`/`ThemeViewModel`/`rememberAppUpdaterViewModel`**（设置/local/app 共用，故放这里）+ `CrashReporter`（crashScreenStarter 缝隙）；依赖 `:core:data`/`:core:media` |
| `:feature:local` | 本地音乐 + **播放器 UI（蓝图中的 `:feature:player` 已并入，原因见蓝图 PR-5 偏差）**：`features/local/**`（11 屏 + 组件 + `MusicViewModel` 等 5 个 viewmodel 文件 + `MusicRepositoryImpl` + `LocalMusicModule` + `MediaScanEngine`）、`shared/presentation/components/{player,lyrics}`（16 组件）、`shared/presentation/screens/player`（3 屏）、`PlaybackBottomSheet`、`MediaScanLoader`、`AutoEQProfileSelector`、`UniversalSearchScreen`/`RhythmStatsScreen`、`Screen` 路由 sealed class + `LocalScreen.kt`、`EqualizerUtils`/`PlaylistImportExportUtils`/`LyricsFileUtils` |
| `:feature:streaming` | 自托管流媒体：provider 3 家（Subsonic/Jellyfin/网易云）、repository、`StreamingMusicViewModel`、5 屏、`StreamingNotificationManager`；**无任何 feature 依赖**（被 local/settings 反向依赖） |
| `:feature:settings` | 45 个设置屏 + 设置对话框/选择器 + `SettingsScreen`（含 SettingItem/SettingGroup/SettingRow/SettingsTips*）+ `SettingsScreenWrapper`（settings 子页 NavHost 宿主）+ `TunerComponents` + `SettingsSearch`；**EqualizerScreen 也在本模块**（包名仍是 `features.local.presentation.screens`，PR-5 断环调整） |
| `:feature:widget` | Glance 4 widget + receivers + `GlanceWidgetUpdater`/`WidgetUpdater` + `RhythmTileService` + `GlanceShapeBitmaps` + widget res（4 个 widget_info xml、4 个 preview layout、widget_colors） |

- **Gradle 依赖方向**：`:app` → 全部；`:feature:local` → `:feature:streaming`（HomeScreen/UniversalSearch 嵌入流媒体状态卡，已审计债务）；`:feature:settings` → `:feature:local`（MusicViewModel + 本地对话框 + 播放器排序 sheet，蓝图明示的已知债务）+ `:feature:widget`（WidgetSettingsScreen 预览，债务）+ `:feature:streaming`（GoSettingsScreen，债务）；`:core:ui` → `:core:data`/`:core:media`；`:core:data` → `:core:designsystem`；`features 之间禁止新增依赖`，新增前先在蓝图记录。
- 调用链：`Screen(Composable)` → `AndroidViewModel` → `Repository`（持有 DAO / MediaStore / `MediaController`）→ 数据源。ViewModel **禁止**直接写 SQL、查询 MediaStore 或拼 HTTP 请求，必须经 Repository / DAO / API service。
- 依赖方向（源码层）：`presentation` → `data` / `domain`；`data` **不得**依赖 `presentation` 或 Compose；`infrastructure` **不得**反向依赖 `features/*/presentation`。
- `:app/navigation/LocalNavigation.kt` 与 `StreamingMusicViewModel` 是 local↔streaming 的缝合点；跨模式数据转换沿用 `toLibrarySong/Album/Artist/Playlist`，**禁止**新增两个 feature 的 data 层互相 import。
- 可复用 Composable 放 `:core:ui`（跨 feature）或 `:feature:local` 的 `shared/presentation/components/{player,lyrics}`（仅播放器域）；设置页放 `:feature:settings`。**禁止**把新的复用组件放进 `features/*/presentation/components`（现存 13 个历史遗留的本地专用组件，不要照抄、不要再扩散）。
- **巨型文件禁令**：不要往 `MusicViewModel.kt`（约 480 KB，现 `:feature:local`）、`StreamingMusicViewModel.kt`（约 108 KB，`:feature:streaming`）、`AppUpdaterViewModel.kt`（`:core:ui`）、`OnboardingScreen.kt`（10,000+ 行，`:app/onboarding/`）、`LibraryScreen.kt`（约 7,000 行）、`HomeScreen.kt`（约 3,400 行）继续追加功能；新功能新建文件、新建 ViewModel。
- **UseCase 层不是强制的**：当前没有 UseCase（零引用的 `SongUseCases.kt` 已删除）；只有被多个 ViewModel 复用的逻辑才抽 UseCase，不要机械地「每个方法建一个类」。
- **包名与目录/模块不一致（必须按文件里的 `package` 声明 import，不要按目录或模块猜）**：
  - `:core:network`（物理 `core/network/src/main/kotlin/chromahub/rhythm/app/infrastructure/network/`… 实为直接放 `network/` 源码目录）→ package `chromahub.rhythm.app.network`（PR-3）
  - `:core:data` 的 AppSettings/Room 保留 `shared.data.model` / `features.local.data.database.*`（PR-3）；`MusicRepository`/`BackupRestoreManager` 用 `core.domain.repository`/`core.domain.backup`（PR-5 自 :app 迁入）
  - `:core:media` 与 `:core:ui`、`:feature:local` 均含 package `chromahub.rhythm.app.util`（同一包跨模块拆分：MediaUtils/MetadataHeuristics/歌词解析器/CacheManager 在 media；ImageUtils/M3ImageUtils 在 ui；EqualizerUtils/PlaylistImportExportUtils/LyricsFileUtils 在 local）
  - `:core:ui` 宿主 `shared.presentation.components.*` 与 `shared.presentation.screens.settings` 包（PR-5 零 churn）；`:core:common` 宿主 `shared.presentation.navigation.RhythmGuardRisk`；`:core:designsystem` 宿主 `shared.presentation.components.player.TimeFormat`
  - `:feature:settings` 宿主 `features.local.presentation.screens.EqualizerScreen` 与 `shared.presentation.screens.settings.*`；`:feature:local` 宿主 `shared.presentation.screens.player`、`shared.presentation.components.{player,lyrics}`、`features.local.presentation.navigation.Screen`
  - `infrastructure/worker/`（2 个文件）→ `chromahub.rhythm.app.worker`，仍在 `:app`
  - `util/ProfileImportExportUtils.kt` → `...app.utils`（:app）；`ScrollStateUtils` → `...app.ui.utils`（:core:designsystem）
  - `gradle.properties` 已设 `android.uniquePackageNames=false`，这类重复包名不会被构建拦住——**靠自觉**。

## 5. 架构与状态管理

- 导航：`MainActivity.setContent` → `:app` 的 `navigation/RhythmNavigation.kt`（物理 `app/src/main/java/chromahub/rhythm/app/navigation/`，package 未改）。共 **2 个 NavHost**：根 NavHost（承载 `settings` 等根级路由）+ 内嵌 NavHost（同目录 `LocalNavigation.kt`）；`appMode` 的 LOCAL 与 STREAMING 两种状态都渲染 `LocalNavigation`。新页面 = 新增 Composable route 并在上述导航文件接线，不是新 Activity；路由常量用 `Screen`（`:feature:local`）与 `SettingsRoutes`（`:core:ui`）。
- DI：**Hilt 已全面接线（PR-6）**。`@HiltAndroidApp` 在 RhythmApplication（缝隙安装保留在 onCreate）；`@AndroidEntryPoint` ×4 Activity；`@HiltViewModel` ×5（MusicViewModel/StreamingMusicViewModel/ThemeViewModel/AppUpdaterViewModel/AppModeViewModel，全部构造注入 `Application`，MusicViewModel 另注入 MusicRepository + StreamingMusicRepository）。绑定：`:feature:streaming` 的 StreamingMusicModule `@Binds` 接口←Impl（`@Inject constructor(@ApplicationContext)`）；本地具体类 `MusicRepository` 直接 `@Inject constructor(@ApplicationContext, StreamingMusicRepository)`（它**不实现** core.domain.repository.MusicRepository 接口，勿想当然 @Binds）。`NetworkClientInitializer`（:app di/）+ AppModule（@Provides NetworkSettingsProvider←AppSettings）。`AppSettings.getInstance()` 调用点不改（整体搬入方案）；`RhythmApplication.instance` 全局入口保留。禁止引入 Koin 等其他 DI 框架。
- **UI 获取 ViewModel 一律 `hiltViewModel()`**（androidx.hilt:hilt-navigation-compose，33 处已迁移）：`viewModel()` 在 NavBackStackEntry 上走 SavedStateViewModelFactory、不走 Hilt 工厂，对多参构造反射必崩（PR-6 真机踩坑）。`rememberAppUpdaterViewModel()` 内部已是 hiltViewModel。
- ViewModel：5 个 ViewModel 全部继承 `AndroidViewModel(application)` 且 `@HiltViewModel`（`MusicViewModel`、`StreamingMusicViewModel`、`AppModeViewModel`（零引用，待清理）、`ThemeViewModel`、`AppUpdaterViewModel`）。只允许 Application 级 context，禁止持有 Activity / View / Fragment / Context。
- 状态：主流写法是 `private val _x = MutableStateFlow(initial)` + 暴露 `val x: StateFlow<T> = _x.asStateFlow()`；派生状态用 `combine/map` + `stateIn(...)`。现存两种 `SharingStarted` 都有：`MusicViewModel` 的大列表用 `Eagerly`，`AppModeViewModel` 与组合状态用 `WhileSubscribed(5000)`；跟随所在文件 / 邻近代码，不要强行统一。
- **UI 收集方式统一 `collectAsState()`**（全仓 1000+ 处，91 个文件）。不要新写 `collectAsStateWithLifecycle()`，也不要两种混用；`lifecycle-runtime-compose` 未作为直接依赖声明（只显式依赖 `lifecycle-runtime-ktx`，`LocalLifecycleOwner` 可经传递依赖 import）。若要切换，属于需要审批的依赖 + 全量迁移。
- **本项目不是 MVI**：没有 `XxxIntent` / `XxxEffect`。屏幕直接调用 ViewModel 的 `fun xxx()`；不要为「架构纯洁」引入 Intent/Reducer 层。
- 一次性结果不向 UI 发事件：用可清空 / 可复位的 `StateFlow`（如 `RestoreResult.Idle → Done`），不要在同一功能里混用 `Channel`、`SharedFlow`、`StateFlow`。
- `AppSettings` 是设置唯一来源，勿另建 SharedPreferences 读写通道；重复的 `shared/data/repository/UserPreferencesRepository` 因零引用已删除。
- 新功能开关走三层门控：编译期 `BuildConfig.ENABLE_*` → `AppSettings.integrationsEnabled` 总开关 → 各 API 单项开关（`NetworkClient.kt` 的 `isXxxApiEnabled()` 与 `AppSettings` 的 `*ApiEnabled` StateFlow），设置页 / 引导页也按开关显隐。
  - `NetworkClient.kt` 的各 API service 是**可空** `by lazy`（按 `BuildConfig.ENABLE_*` 条件初始化，如 `ytmusicApiService: YTMusicApiService?`），调用处必须空安全访问，否则可能 NPE。
  - 当前所有 `ENABLE_*` 均为 `true`（清单见第 9 节），API 与自动更新默认开启，用户可在设置中逐项关闭。排查「某 API 在设置中缺失」时先查 `BuildConfig.ENABLE_*`，再查用户设置。

## 6. Compose / UI 与资源

- 颜色 / 字号 / 形状一律取自 Material 3 Expressive 主题（动态取色）+ 项目自定义 `ExtendedTheme` / `RhythmColors` / `Spacing` / `MusicDimensions`（`shared/presentation/theme/`，含 `festive/` 节日主题包），不硬编码。
- 用户可见字符串只进 `values/strings.xml` 并同步 `values-en/strings.xml`（两文件占位符索引与类型一致；debug 覆盖见 `app/src/debug/res/values/strings.xml`）；涉及复数后跑 `python3 scripts/check_plural_specifiers.py`（退出码 1 即有问题，支持 `--files`）。
- 组件首个可选参数 `modifier: Modifier = Modifier`；可点击目标 ≥48dp 并提供 `contentDescription`；`LazyColumn` / `LazyRow` 项给稳定 `key`（歌曲/专辑统一 `key = { it.id }`）；Composable 内不做业务逻辑、IO，也不直接改状态，必须通过回调 / 事件上抛给 ViewModel。
- composition 中禁止创建昂贵对象（`Brush`、`Shape`、`Painter`、`SimpleDateFormat`、正则、`ImageRequest`）而不 `remember`；副作用只走 `LaunchedEffect` / `DisposableEffect` / `rememberCoroutineScope`；禁止在 Composable 中直接 `CoroutineScope(Dispatchers.IO).launch { ... }`。
- 状态模型若使用 `data class` / `sealed interface` 作为 UI 状态，建议标 `@Immutable`；**不要**把 `MutableList` / `MutableMap` 直接作为参数或 StateFlow 值，改为 `List` / `toList()` 副本。
- `@Preview` **不是本仓的强制约定**（全仓仅 5 个文件使用，且多在 Glance / 小组件）。不要给每个 screen 批量补 4 种状态的 Preview；需要时同文件底部加 `private` Preview。
- 禁止新增 XML 布局或 `AndroidView` 包裹传统 UI；禁止新增 `values-xx` 语言目录——只维护 `values/`（中文默认）、`values-en/`（英文）、`values-night/`（仅 `colors.xml`），`res/xml/locale_config.xml` 只列 `zh` / `en`。
- 图标统一走 `RhythmIcons` / `MaterialSymbolIcon`；功能性图标必须提供 `contentDescription`，纯装饰图标显式传 `null`。
- 禁止用 `@Serializable` 定义导航路由：导航使用字符串路由（`MainScreen` + `composable("...")`）。`kotlin-serialization` 插件已入 catalog（PR-7 供 `:core:network` DTO 用），不要把它用于导航路由。

## 7. Kotlin 风格与命名

- 已移除格式化 / lint 强制检查（AI agent 主导开发，检查只拖慢速度）：根 `build.gradle.kts` 无 Spotless / ktlint，`app/build.gradle.kts` 里 lint 全关且无 detekt。代码风格以邻近文件为准，新文件照抄邻近文件的 import 写法和排版，勿借机全量重排。
- 不在 `.kt` / `.kts` 顶部添加 SPDX / 版权文件头（仓库已统一移除，许可证以根目录 `LICENSE` 为准）；仅从第三方项目引入的代码保留其原有版权与许可证声明。
- import 禁通配符；命名后缀：`XxxViewModel` / `XxxRepository`（接口在 domain，实现在 data）/ `XxxModule`（DI object）/ `XxxTest`。UseCase 层很薄，不强制 `XxxUseCase` 后缀。
- Dispatcher 直接引用 `Dispatchers.IO` / `Dispatchers.Default`（仓储内用 `withContext(Dispatchers.IO)`，部分 `flowOn`）；不要为此引入 dispatcher 注入抽象。`Dispatchers.Main` 只在 UI 层使用，禁止在主线程做文件 / 数据库 / 网络 IO。
- 网络错误按异常类型分类处理并统一重试（`infrastructure/network/NetworkClient.kt` 的 `retryInterceptor`：指数退避、429 按 `Retry-After` 退避、4xx 除 408/429 外不重试、SSL/FileNotFound 不重试）；网络日志统一走 `NetworkLog`（`infrastructure/network/NetworkLog.kt`），纯脱敏函数在 `util/NetworkLogRedactor.kt`：`d` 仅 debug、release 不打堆栈、URL query 中的 `u/p/t/s/ApiKey/api_key/token` 等一律打码，所有 `HttpLoggingInterceptor` 共用 debug=BASIC / release=NONE 与统一 header 清单；新增日志勿绕过该入口，勿扩大敏感信息范围。
- 新增代码禁止 `!!`（现有 130 处是历史遗留，不要继续扩散）；改用 `requireNotNull`、早退或 `Result`。禁止用裸 `RuntimeException` / `IllegalStateException` 做流程控制；用返回值或 sealed 结果类型。
- 禁止 `GlobalScope`（当前全仓 0 处）；用 `viewModelScope`、`WorkManager`，或 `RhythmApplication` 中显式创建并管理的 scope。禁止用 `delay()` 假装等待异步结果。
- 禁止空 `catch {}` / `catch (_: Exception) {}` 静默吞掉；至少 `Log.w/e`，用户可见失败要更新状态。非致命异常用 `util/CrashReporter` 记录，不要自建崩溃上报通道；不要把 `e.message` 直接当用户文案。

## 8. 测试策略

- 只有 JVM 单测：36 个 `*Test.kt` + 1 个 `FakeUri.kt` Uri stub（PR-5 起随流媒体测试位于 `:feature:streaming` 的 `src/test/java/android/net/`）；JUnit4 + MockWebServer3 + 真 `org.json`（均为 `testImplementation`）。
- 测试分布（跟随被测类的模块）：`:core:media` 19、`:feature:streaming` 6、`:core:common` 5、`:feature:local` 3、`:core:designsystem` 1、`:core:ui` 1、`:app` 1（RhythmBitmapLoaderTest）。跑全量见第 2 节命令。
- **禁止引入**：MockK、Turbine、`kotlinx-coroutines-test`、JUnit 5、Robolectric、Paparazzi / Roborazzi 截图测试（仓库无基准图目录）。
- `unitTests.isReturnDefaultValues = true`（android.jar stub 返回默认值），这是预期行为；不要因此改成 Robolectric。
- 测试与源码同包放在对应模块的 `src/test/java/...`；可测逻辑优先抽成顶层纯函数再测。禁止在单元测试里打真实网络或真实数据库；HTTP 用 `MockWebServer` 打桩；禁止 `Thread.sleep` 等异步等待，用 `CompletableDeferred`、`withTimeout`、`awaitCancellation`、`cancelAndJoin`。
- 必须补 / 改测试的区域：`util/`、domain 模型、`infrastructure/audio` DSP、`infrastructure/service/player`、`features/streaming/data/provider` 协议解析。
- `app/src/androidTest/` 不存在；不要新建 androidTest 源集或往里面放文件。仓库无 CI，测试是否通过只有本地跑第 2 节的全量单测命令能证明。

## 9. 发布与安全边界

- **不要修改**：`minSdk = 26`、`targetSdk = 37`、`compileSdk = 37`、Java / Kotlin target 21。
- **不要关闭** release 的 `isMinifyEnabled` / `isShrinkResources`；不要在 `app/proguard-rules.pro` 加 `-keep class ** { *; }` 这类通配 keep。
- 只打包 `arm64-v8a`（`defaultConfig.ndk.abiFilters`）；x86_64 模拟器装不上 APK。
- 签名：release / debug / benchmark 都读**项目根目录 `keystore.properties`**（`getProperties("keystore.properties")`），`store_file` 相对根目录解析；文件缺失时回退 debug 签名。`.config/keystore.properties` 不存在，也不是读取路径。**禁止**打印、提交或改写密钥；禁止擅自把签名路径「修」到别处。
- 版本号：`versionCode = 555001306`、`versionName = "5.5.500.1306 Beta"` 定义在 `app/build.gradle.kts`，由 `scripts/prepare_release.py` 更新。**AI 不得自行 bump**；临时构建用 `-PversionCodeOverride` / `-PversionNameOverride` / `-PreleaseDateOverride` / `-Pnightly=true`。
- 语言规则见第 6 节；新增字符串必须同时写 `values/strings.xml`（中文）与 `values-en/strings.xml`（英文），并保持 `%s` / `%d` 占位符一致。
- 平台契约（不要「顺手加固」，会破坏功能）：`MediaPlaybackService` 必须 `exported=true`（Android Auto / 系统媒体控制）、`MediaTransferReceiver` 必须 exported 无权限、`usesCleartextTraffic=true` + `networkSecurityConfig`（本地服务器常为 http）。
- `app/src/main/baseline-prof/baseline-prof.txt` 与 `androidx.profileinstaller` 是刻意配置的基线 profile；**不要删除**。当前文件是拆分前的旧档（混淆名 + 旧包名，对现行构建基本失效，不崩溃但启动性能回退）。重新生成流程（PR-7 实测，需 arm64 真机）：
  1. 临时把 `:app` benchmark buildType 改 `isMinifyEnabled = false`（混淆名 profile 无法经 R8 remap，必须真名生成，生成后还原）；
  2. `./gradlew :app:assembleBenchmark :macrobenchmark:assembleBenchmark`，安装两个 benchmark APK；
  3. 先在系统管家里给 `chromahub.rhythm.app` 加白名单（荣耀 iAware 会以 "by app" 杀掉 instrumentation 的 target 进程——PR-7 在 HONOR BVL-AN16 上实测被拦，需人工配合）；
  4. `adb shell am instrument -w -e class chromahub.rhythm.app.benchmark.BaselineProfileGenerator chromahub.rhythm.app.macrobenchmark/androidx.test.runner.AndroidJUnitRunner`；
  5. `adb pull /storage/emulated/0/Android/media/chromahub.rhythm.app.macrobenchmark/*-startup-prof.txt` → 替换 `baseline-prof.txt`；还原 benchmark buildType 的 minify 并卸载设备上的两个 benchmark 包。
  macrobenchmark 手工生成（无 baselineprofile-gradle-plugin）的关键前提——PR-7 已修入 `macrobenchmark/build.gradle.kts`：**不得**开 `android.experimental.self-instrumenting`（会让 targetPackage 指向 test 自身，只收得到库类）；benchmark buildType 签名必须与 `:app` 相同（keystore.properties）；`:app` 的 benchmark 变体需 `add("benchmarkImplementation", libs.androidx.benchmark.junit4)`（manifest 注入 IsolationActivity）；test 侧 lifecycle 版本与 :app 对齐（旧 ReportFragment 在 target 进程里引发 NoSuchFieldError）。
- `BuildConfig` 功能开关定义在 `app/build.gradle.kts` `defaultConfig`，当前均为 `true`：`ENABLE_YOUTUBE_MUSIC`、`ENABLE_LYRICALLY_API`、`ENABLE_DEEZER`、`ENABLE_LRCLIB`、`ENABLE_BETTERLYRICS`、`ENABLE_SPOTIFY_SEARCH`、`ENABLE_WIKIPEDIA`；另有字符串 `FLAVOR="github"`。**不要**为「清理」删除这些字段（部分 UI 会读取）。

## 10. 协作流程与上下文入口

- 仓库已 `git init`（默认分支 `main`，尚无提交），没有 CI；未经用户明确要求，不执行 `git commit` / `git push` / `git tag`。仓库约定见 `wiki/Contributing.md`。
- 文档现状：`wiki/` 14 个 `.md`（PR-7 起每个文件顶部带过时横幅，仅历史参考）；`docs/` 仅有 `multi-module-refactor-plan.md`（多模块拆分蓝图，进行中）；没有 `.github/`、`.githooks/`；原根目录 `BUILD_VARIANTS.md` 已删除，构建变体 / BuildConfig / 产物说明并入本文件。不要按旧文档里的路径或 CI 配置行事（GitHub Actions 片段只是示例，本仓库无 CI）。
- 发布脚本 `scripts/prepare_release.py` 与 `scripts/generate_release_notes.py` 是交互式脚本，会改版本号并写 changelog / fastlane；未经要求不要运行。
- 开始工作前按区域先读源码内注释（非常详尽）：构建 / 签名 → `app/build.gradle.kts`；播放 → `core/media/.../infrastructure/service/MediaPlaybackService.kt`；导航 → `app/src/main/java/chromahub/rhythm/app/navigation/RhythmNavigation.kt`；主题 → `core/designsystem/.../theme/Theme.kt` 与 `ExtendedTheme.kt`；缝隙安装 → `RhythmApplication.kt`。
- 当前没有模块级 `AGENTS.md`；若未来新增 `:core:*` / `:feature:*` 模块，才在该模块根目录加就近 `AGENTS.md`，只写模块入口、关键类、局部禁令，不要复制根规则。

---

## 快速参考卡片

| 项 | 值 |
| :--- | :--- |
| 构建 | `./gradlew assembleDebug`（arm64-only，debug applicationId `chromahub.rhythm.app.debug`） |
| 测试 | 第 2 节「全部 JVM 单测」命令（36 类分布 7 个模块；无 MockK / Turbine / coroutines-test） |
| 关键路径 | `app/src/main/java/chromahub/rhythm/app/`（组合根：Activity/导航/RhythmApplication）+ `core/`、`feature/` 模块（package 保留旧路径，import 按 package 不按模块） |
| 状态 | `StateFlow` + `asStateFlow()`，UI 用 `collectAsState()`；无 MVI、无 XML 布局；Hilt 已接线（PR-6）：UI 取 VM 用 `hiltViewModel()`，勿用 `viewModel()` |
| 质量 | 无 lint / detekt / ktlint / spotless；只跑 `python3 scripts/check_plural_specifiers.py` |
| 禁止 | 加 `kotlin-android` 插件、加 Koin、用 `material-icons-extended`、新增语言 / flavor、bump 版本号、拆分外新建 Gradle 模块、feature→feature 新依赖、`viewModel()` 取 5 个 Hilt VM |
