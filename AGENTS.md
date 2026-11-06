# AGENTS.md — Rhythm

AI 编码代理上下文文件。只写「读代码无法可靠推断」的项目约定、可直接执行的命令，以及会破坏本项目的反模式。

**事实来源优先级：** 源码 / `app/build.gradle.kts` > `gradle/libs.versions.toml` > 本文件 > `wiki/`。
`wiki/` 是历史快照（14 个 `.md`），已确认存在过期内容（提到的 CI workflow / `.githooks` 不存在、product flavor 已移除）。仓库已 `git init`（默认分支 `main`，尚无提交），没有 `.github/`、`.githooks/`、`docs/`；根目录有 `README.md`。

## 1. 项目定位与技术基线

Android 音乐播放器：本地曲库（MediaStore + Room）+ 自托管流媒体（Subsonic/Navidrome/Jellyfin、网易云等）+ 歌词 + Glance 小组件 + 均衡器/ReplayGain，Material 3 Expressive。

- 工具链：Kotlin 2.4.21（K2）· AGP 9.4.1 · Gradle 9.7.1（wrapper）· KSP 2.3.6 · JDK/JVM 21。
- **不要添加 `org.jetbrains.kotlin.android` 插件**：AGP 9.x 内建 Kotlin 支持，加了会直接构建失败（`:app` 与 `:macrobenchmark` 都不行）。
- SDK：`minSdk = 26`、`targetSdk = 37`、`compileSdk = 37`；coreLibraryDesugaring 开启。
- Compose BOM `2026.09.00`；`material3` 显式钉在 `1.5.0-beta01`；`foundation-layout` / `animation` / `animation-core` 显式钉在 `1.12.1`。这些 pin 是刻意的，不要「顺手」去掉版本或交给 BOM。
- 编译期已全局 opt-in `ExperimentalMaterial3Api` / `ExperimentalMaterial3ExpressiveApi`（`freeCompilerArgs`），新文件不需要逐处 `@OptIn`；新增 Experimental API 加到 `app/build.gradle.kts`。
- 依赖必须经 Version Catalog（`gradle/libs.versions.toml`）；不要在 `build.gradle.kts` 写死 `group:artifact:version`。
- 异步模型：Coroutines + Flow（`StateFlow` 为主）；没有 LiveData / RxJava，不要引入。
- 图标：`res/font/material_symbols_outlined.ttf` 变量字体 + `MaterialSymbolIcon` / `RhythmIcons`；禁止 `Icons.Default.*`、`androidx.compose.material3.Icon` 配 `Icons.*`、`material-icons-extended`。
- 禁止新增 XML 布局；`res/layout/` 的 4 个 `widget_preview_*.xml` 只服务 Glance 小组件预览。
- 构建系统：Gradle Kotlin DSL + Version Catalog；没有 `buildSrc` / `build-logic` / convention plugin，构建逻辑集中在 `app/build.gradle.kts`。

## 2. 构建与验证命令

| 用途 | 命令 |
| :-- | :-- |
| 日常安装 debug | `./gradlew :app:installDebug` |
| 最快编译检查 | `./gradlew :app:compileDebugKotlin` |
| 全部 JVM 单测（36 个测试类） | `./gradlew :app:testDebugUnitTest` |
| 单个测试类 | `./gradlew :app:testDebugUnitTest --tests "chromahub.rhythm.app.util.VersionComparatorTest"` |
| 构建 debug APK | `./gradlew assembleDebug` |
| 构建 release（仅发布前） | `./gradlew assembleRelease` |
| 复数占位符校验（改 strings 后） | `python3 scripts/check_plural_specifiers.py` |
| 提交前最小集合 | `./gradlew assembleDebug :app:testDebugUnitTest` |

- **不要用裸 `./gradlew test`**：会连带 release / benchmark 变体，慢且容易因环境失败；始终指定 `:app:testDebugUnitTest`。
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
- **结构**：不为新页面建 Activity、不引入 Fragment（`MainActivity` 是唯一 Compose 宿主，辅助 Activity 仅 `CrashActivity` / `ExternalPlaybackActivity` / `RhythmGuardTimeoutActivity`）；不新增 product flavor；不新增第三种语言目录；不引入 DI 框架；不把 Gson 换成 kotlinx-serialization。
- **Manifest**：`exported=true` 共 10 个组件，其 `exported` 与 intent-filter 是对外协议（Android Auto / 系统 / 启动器 / 小组件 / 其他应用依赖），勿移除、勿额外加 permission（`RhythmTileService` 已有的 `BIND_QUICK_SETTINGS_TILE` 除外）。cleartext + user CA 放行（`res/xml/network_security_config.xml`）是自托管 Navidrome/Jellyfin 的既定取舍，勿收紧。
- **生成物与敏感区**：不改 `build/`、`app/build/generated/`、`app/src/main/baseline-prof/`；新增 `.github/workflows/`、`.githooks/` 或改动 `scripts/prepare_release.py` / `scripts/generate_release_notes.py` 前先询问（两者交互式，会改版本号并写 changelog / fastlane，后两者当前不存在）。
- **Gradle**：只用 `./gradlew`，不用系统 `gradle`；不改 `gradle-wrapper.properties`；不动 `gradle.properties` 的内存与并发调优。
- **Git**：仓库已 `git init`（默认分支 `main`，尚无提交）；未经明确要求，不执行 `commit` / `push` / `tag`。
- **功能开关**：不删除 `BuildConfig.ENABLE_*`、`FLAVOR="github"`、`IS_NIGHTLY`、`RELEASE_DATE` 或 `APPLE_MUSIC_FALLBACK_TOKEN` 相关逻辑，即使看起来像死代码。

## 4. 工程结构与模块边界

`app/src/main/java/chromahub/rhythm/app/`（377 个 `.kt`，7 个一级目录 + 根文件 `RhythmApplication.kt`）：

| 目录 | 职责 |
| :-- | :-- |
| `activities/` | 4 个 Activity：`MainActivity`（唯一 Compose 宿主 + 权限/外部 Intent）、`CrashActivity`（独立 `:crash_process`）、`ExternalPlaybackActivity`（外部 VIEW 音频）、`RhythmGuardTimeoutActivity`（`singleTop`） |
| `core/` | `domain/`（`model` / `repository` / `usecase` / `scan` MediaStore 扫描 / `backup`）；`device/`（`DeviceCapabilitiesProvider`、`HiFiCapabilityChecker`） |
| `features/local/` | 本地音乐：`data`（Room DB/DAO/Entity + repository）、`di`、`presentation`（`viewmodel` / `screens` / `components` / `navigation`） |
| `features/streaming/` | 在线音乐：`data`（`provider` 含 Subsonic / Jellyfin / 网易云 API client）、`domain`、`di`、`infrastructure/notification`、`presentation`（无 `navigation/`，页面挂在 `LocalNavigation`）；Deezer / LRCLib / BetterLyrics / YouTube Music / Spotify 搜索 / Wikipedia / Apple Music Canvas / iTunes / GitHub 等 API 定义在 `infrastructure/network/` |
| `shared/` | `data/model`（`AppSettings` 全局设置单例）、`data/repository`、`domain/utils`、`presentation`（`navigation` 根导航、`screens` 设置页、`components`、`theme`、`ui`、`viewmodel`） |
| `infrastructure/` | `audio`（DSP/EQ/重采样/ReplayGain）、`network`、`service`（Media3 播放服务）、`widget/glance`、`worker`、`provider` |
| `util/`、`utils/` | 纯 Kotlin 工具；`utils/` 目录仅 `StatusBroadcaster.kt`，但另有若干 `util/*.kt` 声明 `...app.utils` 包 |

- **只有两个 Gradle 模块**：`:app` + `:macrobenchmark`（`com.android.test`，仅 `benchmark` 变体启用，`targetProjectPath = :app`）。不存在 `:core:*` / `:feature:*` Gradle 模块；「模块化」是包级分层，不要为「更 Clean」新建 Gradle 模块。
- 调用链：`Screen(Composable)` → `AndroidViewModel` → `Repository`（持有 DAO / MediaStore / `MediaController`）→ 数据源。ViewModel **禁止**直接写 SQL、查询 MediaStore 或拼 HTTP 请求，必须经 Repository / DAO / API service。
- 依赖方向：`presentation` → `data` / `domain`；`data` **不得**依赖 `presentation` 或 Compose；`infrastructure` **不得**反向依赖 `features/*/presentation`。
- `features/local` 与 `features/streaming` 的主要缝合点是 `features/local/presentation/navigation/LocalNavigation.kt` 与 `StreamingMusicViewModel`；跨模式数据转换沿用 `toLibrarySong/Album/Artist/Playlist`，**禁止**新增两个 feature 的 data 层互相 import。
- 可复用 Composable 放 `shared/presentation/components/{common,player,bottomsheets,dialogs,icons,lyrics}`；设置页放 `shared/presentation/screens/settings`。**禁止**把新的复用组件放进 `features/*/presentation/components`（现存 8 个历史遗留的本地专用组件，其 package 已与目录统一，不要照抄、不要再扩散）。
- **巨型文件禁令**：不要往 `MusicViewModel.kt`（约 480 KB）、`StreamingMusicViewModel.kt`（约 108 KB）、`AppUpdaterViewModel.kt`、`OnboardingScreen.kt`（10,000+ 行）、`LibraryScreen.kt`（约 7,000 行）、`HomeScreen.kt`（约 3,400 行）继续追加功能；新功能新建文件、新建 ViewModel。
- **UseCase 层不是强制的**：`core/domain/usecase/SongUseCases.kt` 存在但当前无人调用；只有被多个 ViewModel 复用的逻辑才抽 UseCase，不要机械地「每个方法建一个类」。
- **包名与目录不一致（必须按文件里的 `package` 声明 import，不要按目录猜）**：
  - `infrastructure/network/`（14 个文件）→ `chromahub.rhythm.app.network`
  - `infrastructure/worker/`（3 个文件）→ `chromahub.rhythm.app.worker`
  - `shared/presentation/theme/`（14 个文件）→ `chromahub.rhythm.app.ui.theme`（`festive/` 下 `...ui.theme.festive`），已全部统一，无例外。
  - `features/local/presentation/components/` 下 13 个文件的 package 已与目录统一（`...features.local.presentation.components.{bottomsheets,dialogs,player,settings}`）；`shared/presentation/viewmodel/MusicViewModel.kt` 的 `typealias` 已归入 `...shared.presentation.viewmodel`。
  - `util/FontLoader.kt`、`util/ProfileImportExportUtils.kt` → `...app.utils`；`util/ScrollStateUtils.kt` → `...app.ui.utils`
  - `gradle.properties` 已设 `android.uniquePackageNames=false`，这类重复包名不会被构建拦住——**靠自觉**。

## 5. 架构与状态管理

- 导航：`MainActivity.setContent` → `shared/presentation/navigation/RhythmNavigation`。共 **2 个 NavHost**：根 NavHost（承载 `settings` 等根级路由）+ 内嵌 NavHost（`features/local/presentation/navigation/LocalNavigation.kt`）；`appMode` 的 LOCAL 与 STREAMING 两种状态都渲染 `LocalNavigation`。新页面 = 新增 Composable route 并在上述导航文件接线，不是新 Activity。
- DI：手动。`features/local/di/LocalMusicModule` 与 `features/streaming/di/StreamingMusicModule` 是 `object` + `@Volatile` + `synchronized` 双检锁单例工厂；`shared/data/model/AppSettings.getInstance(context)` 管设置；`RhythmApplication.instance` 提供全局入口。新增依赖照此模式扩展对应 Module。**没有 Hilt / Koin / Dagger**，不要引入。
- ViewModel：5 个 ViewModel 全部继承 `AndroidViewModel(application)`（`MusicViewModel`、`StreamingMusicViewModel`、`AppModeViewModel`、`ThemeViewModel`、`AppUpdaterViewModel`）；多数 UI 用默认 `viewModel()` 获取，`AppUpdaterViewModel` 等已有 `rememberAppUpdaterViewModel()` 工厂，沿用同文件已有做法。只允许 Application 级 context，禁止持有 Activity / View / Fragment / Context。
- 状态：主流写法是 `private val _x = MutableStateFlow(initial)` + 暴露 `val x: StateFlow<T> = _x.asStateFlow()`；派生状态用 `combine/map` + `stateIn(...)`。现存两种 `SharingStarted` 都有：`MusicViewModel` 的大列表用 `Eagerly`，`AppModeViewModel` 与组合状态用 `WhileSubscribed(5000)`；跟随所在文件 / 邻近代码，不要强行统一。
- **UI 收集方式统一 `collectAsState()`**（全仓 1000+ 处，91 个文件）。不要新写 `collectAsStateWithLifecycle()`，也不要两种混用；`lifecycle-runtime-compose` 未作为直接依赖声明（只显式依赖 `lifecycle-runtime-ktx`，`LocalLifecycleOwner` 可经传递依赖 import）。若要切换，属于需要审批的依赖 + 全量迁移。
- **本项目不是 MVI**：没有 `XxxIntent` / `XxxEffect`。屏幕直接调用 ViewModel 的 `fun xxx()`；不要为「架构纯洁」引入 Intent/Reducer 层。
- 一次性结果不向 UI 发事件：用可清空 / 可复位的 `StateFlow`（如 `RestoreResult.Idle → Done`），不要在同一功能里混用 `Channel`、`SharedFlow`、`StateFlow`。
- `AppSettings` 是设置唯一来源，勿另建 SharedPreferences 读写通道；`shared/data/repository/UserPreferencesRepository` 是重复实现（`AppModeViewModel` 注释明确说明改用 `AppSettings`），新代码不要再依赖它。
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
- 禁止用 `@Serializable` 定义导航路由：`kotlin-serialization` 插件是注释掉的，导航使用字符串路由（`MainScreen` + `composable("...")`）。

## 7. Kotlin 风格与命名

- 已移除格式化 / lint 强制检查（AI agent 主导开发，检查只拖慢速度）：根 `build.gradle.kts` 无 Spotless / ktlint，`app/build.gradle.kts` 里 lint 全关且无 detekt。代码风格以邻近文件为准，新文件照抄邻近文件的 import 写法和排版，勿借机全量重排。
- 不在 `.kt` / `.kts` 顶部添加 SPDX / 版权文件头（仓库已统一移除，许可证以根目录 `LICENSE` 为准）；仅从第三方项目引入的代码保留其原有版权与许可证声明。
- import 禁通配符；命名后缀：`XxxViewModel` / `XxxRepository`（接口在 domain，实现在 data）/ `XxxModule`（DI object）/ `XxxTest`。UseCase 层很薄，不强制 `XxxUseCase` 后缀。
- Dispatcher 直接引用 `Dispatchers.IO` / `Dispatchers.Default`（仓储内用 `withContext(Dispatchers.IO)`，部分 `flowOn`）；不要为此引入 dispatcher 注入抽象。`Dispatchers.Main` 只在 UI 层使用，禁止在主线程做文件 / 数据库 / 网络 IO。
- 网络错误按异常类型分类处理并统一重试（`infrastructure/network/NetworkClient.kt` 的 `retryInterceptor`：指数退避、429 按 `Retry-After` 退避、4xx 除 408/429 外不重试、SSL/FileNotFound 不重试）；日志不输出请求头与 token，新增日志勿扩大敏感信息范围。
- 新增代码禁止 `!!`（现有 130 处是历史遗留，不要继续扩散）；改用 `requireNotNull`、早退或 `Result`。禁止用裸 `RuntimeException` / `IllegalStateException` 做流程控制；用返回值或 sealed 结果类型。
- 禁止 `GlobalScope`（当前全仓 0 处）；用 `viewModelScope`、`WorkManager`，或 `RhythmApplication` 中显式创建并管理的 scope。禁止用 `delay()` 假装等待异步结果。
- 禁止空 `catch {}` / `catch (_: Exception) {}` 静默吞掉；至少 `Log.w/e`，用户可见失败要更新状态。非致命异常用 `util/CrashReporter` 记录，不要自建崩溃上报通道；不要把 `e.message` 直接当用户文案。

## 8. 测试策略

- 只有 JVM 单测：36 个 `*Test.kt` + 1 个 `java/android/net/FakeUri.kt` Uri stub；JUnit4 + MockWebServer3 + 真 `org.json`（均为 `testImplementation`）。
- **禁止引入**：MockK、Turbine、`kotlinx-coroutines-test`、JUnit 5、Robolectric、Paparazzi / Roborazzi 截图测试（仓库无基准图目录）。
- `unitTests.isReturnDefaultValues = true`（android.jar stub 返回默认值），这是预期行为；不要因此改成 Robolectric。
- 测试与源码同包放在 `app/src/test/java/...`；可测逻辑优先抽成顶层纯函数再测。禁止在单元测试里打真实网络或真实数据库；HTTP 用 `MockWebServer` 打桩；禁止 `Thread.sleep` 等异步等待，用 `CompletableDeferred`、`withTimeout`、`awaitCancellation`、`cancelAndJoin`。
- 必须补 / 改测试的区域：`util/`、domain 模型、`infrastructure/audio` DSP、`infrastructure/service/player`、`features/streaming/data/provider` 协议解析。
- `app/src/androidTest/` 不存在；不要新建 androidTest 源集或往里面放文件。仓库无 CI，测试是否通过只有本地 `./gradlew :app:testDebugUnitTest` 能证明。

## 9. 发布与安全边界

- **不要修改**：`minSdk = 26`、`targetSdk = 37`、`compileSdk = 37`、Java / Kotlin target 21。
- **不要关闭** release 的 `isMinifyEnabled` / `isShrinkResources`；不要在 `app/proguard-rules.pro` 加 `-keep class ** { *; }` 这类通配 keep。
- 只打包 `arm64-v8a`（`defaultConfig.ndk.abiFilters`）；x86_64 模拟器装不上 APK。
- 签名：release / debug / benchmark 都读**项目根目录 `keystore.properties`**（`getProperties("keystore.properties")`），`store_file` 相对根目录解析；文件缺失时回退 debug 签名。`.config/keystore.properties` 不存在，也不是读取路径。**禁止**打印、提交或改写密钥；禁止擅自把签名路径「修」到别处。
- 版本号：`versionCode = 555001306`、`versionName = "5.5.500.1306 Beta"` 定义在 `app/build.gradle.kts`，由 `scripts/prepare_release.py` 更新。**AI 不得自行 bump**；临时构建用 `-PversionCodeOverride` / `-PversionNameOverride` / `-PreleaseDateOverride` / `-Pnightly=true`。
- 语言规则见第 6 节；新增字符串必须同时写 `values/strings.xml`（中文）与 `values-en/strings.xml`（英文），并保持 `%s` / `%d` 占位符一致。
- 平台契约（不要「顺手加固」，会破坏功能）：`MediaPlaybackService` 必须 `exported=true`（Android Auto / 系统媒体控制）、`MediaTransferReceiver` 必须 exported 无权限、`usesCleartextTraffic=true` + `networkSecurityConfig`（本地服务器常为 http）。
- `app/src/main/baseline-prof/baseline-prof.txt` 与 `androidx.profileinstaller` 是刻意配置的基线 profile；**不要删除**。
- `BuildConfig` 功能开关定义在 `app/build.gradle.kts` `defaultConfig`，当前均为 `true`：`ENABLE_YOUTUBE_MUSIC`、`ENABLE_LYRICALLY_API`、`ENABLE_DEEZER`、`ENABLE_LRCLIB`、`ENABLE_BETTERLYRICS`、`ENABLE_SPOTIFY_SEARCH`、`ENABLE_WIKIPEDIA`；另有字符串 `FLAVOR="github"`。**不要**为「清理」删除这些字段（部分 UI 会读取）。

## 10. 协作流程与上下文入口

- 仓库已 `git init`（默认分支 `main`，尚无提交），没有 CI；未经用户明确要求，不执行 `git commit` / `git push` / `git tag`。仓库约定见 `wiki/Contributing.md`。
- 文档现状：`wiki/` 14 个 `.md`（部分内容过时）；没有 `docs/`、`.github/`、`.githooks/`；原根目录 `BUILD_VARIANTS.md` 已删除，构建变体 / BuildConfig / 产物说明并入本文件。不要按旧文档里的路径或 CI 配置行事（GitHub Actions 片段只是示例，本仓库无 CI）。
- 发布脚本 `scripts/prepare_release.py` 与 `scripts/generate_release_notes.py` 是交互式脚本，会改版本号并写 changelog / fastlane；未经要求不要运行。
- 开始工作前按区域先读源码内注释（非常详尽）：构建 / 签名 → `app/build.gradle.kts`；播放 → `infrastructure/service/MediaPlaybackService.kt`；导航 → `shared/presentation/navigation/RhythmNavigation.kt`；主题 → `shared/presentation/theme/Theme.kt` 与 `ExtendedTheme.kt`。
- 当前没有模块级 `AGENTS.md`；若未来新增 `:core:*` / `:feature:*` 模块，才在该模块根目录加就近 `AGENTS.md`，只写模块入口、关键类、局部禁令，不要复制根规则。

---

## 快速参考卡片

| 项 | 值 |
| :--- | :--- |
| 构建 | `./gradlew assembleDebug`（arm64-only，debug applicationId `chromahub.rhythm.app.debug`） |
| 测试 | `./gradlew :app:testDebugUnitTest`（JUnit4，无 MockK / Turbine / coroutines-test） |
| 关键路径 | `app/src/main/java/chromahub/rhythm/app/`（单模块；`shared/` 与 `infrastructure/` 是跨模式核心） |
| 状态 | `StateFlow` + `asStateFlow()`，UI 用 `collectAsState()`；无 MVI、无 Hilt、无 XML 布局 |
| 质量 | 无 lint / detekt / ktlint / spotless；只跑 `python3 scripts/check_plural_specifiers.py` |
| 禁止 | 加 `kotlin-android` 插件、加 Hilt / Koin、用 `material-icons-extended`、新增语言 / flavor、bump 版本号 |
