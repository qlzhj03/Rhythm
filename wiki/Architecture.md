> ℹ️ **文档状态**：本 wiki 已按 2026-11 多模块拆分与唯一包名重构后的代码更新。
> 约定与红线见根目录 [AGENTS.md](../AGENTS.md)，设计与重构历史见 [docs/](../docs/)；一切事实以源码为准。

# Rhythm 架构指南

Rhythm 采用**单 Activity + Jetpack Compose + 多模块分层**架构。本文描述应用结构、依赖方向与关键设计决策。

## 🧱 模块结构

`settings.gradle.kts` 注册 13 个模块，`build-logic/` 通过 `includeBuild` 提供约定插件。

| 模块 | 职责 |
|:---|:---|
| `:app` | 组合根：Application、单 Activity、导航装配、引导页、签名配置 |
| `:core:model` | 纯领域模型 |
| `:core:common` | 纯工具与缝隙接口 |
| `:core:designsystem` | 资源中心：strings（中/英）、theme、icons、基础组件 |
| `:core:network` | 网络客户端与各外部 API service |
| `:core:data` | Room、`AppSettings`、仓储与统计 |
| `:core:media` | 播放服务与引擎、DSP / EQ / ReplayGain、媒体工具 |
| `:core:ui` | 跨 feature 共享 UI 组件（对话框、BottomSheet、播放状态组件等） |
| `:feature:local` | 本地音乐 + 播放器 UI |
| `:feature:streaming` | 流媒体 provider 与 UI |
| `:feature:settings` | 设置页 |
| `:feature:widget` | Glance 小组件与快捷磁贴 |
| `:macrobenchmark` | Macrobenchmark 性能测量（`com.android.test` 模块） |

**依赖方向**：presentation → data / domain；`data` 不依赖 presentation 与 Compose；**feature 模块之间禁止互相依赖**（跨 feature 债务已清零）；只有 `:app` 允许同时看到全部 `core:*` 与 `feature:*` 模块。新增库模块一律使用 `build-logic` 约定插件，不要复制 `:app` 的手写配置。

```
:app  ──>  :feature:local / :feature:streaming / :feature:settings / :feature:widget
  │              │
  │              └──> :core:ui ──> :core:data ──> :core:model ──> :core:common
  │                        │            │
  │                        │            └──> :core:network / :core:designsystem
  │                        └──> :core:media ──> :core:data / :core:network
  └──────────────────────> 全部 core 模块
```

## 📦 唯一包名

2026-11-08 完成唯一包名重构，由 `gradle.properties` 的 `android.uniquePackageNames=true` 强制。

| 模块 | 包根 |
|:---|:---|
| `:app` | `com.qlzhj.rhythm.app` |
| core 模块 | `com.qlzhj.rhythm.core.<name>` |
| feature 模块 | `com.qlzhj.rhythm.feature.<name>` |

`applicationId = com.qlzhj.rhythm`（debug 后缀 `.debug`），与代码包根**分离**；旧 `chromahub.rhythm.app` 安装无法原地更新。自定义 action / broadcast / extra 协议串仍保持 `chromahub.rhythm.app.*` 字面前缀不变。`macrobenchmark` 是唯一例外，仍以 `chromahub.rhythm.app.benchmark` 为代码包，目标进程是 `com.qlzhj.rhythm`。**import 即定位「模块 + 文件」**。

## 🔄 分层与双模式

调用链固定为 **Screen → ViewModel → Repository → 数据源**；ViewModel 不直接触碰 SQL、MediaStore 或 HTTP。

- **本地模式**：`:feature:local` 通过 `MediaStore` / 文件夹扫描建立曲库索引，`LocalMusicRepository` 提供曲目、专辑、艺术家、播放列表与歌词。
- **流媒体模式（Go）**：`:feature:streaming` 拥有独立的 provider 与仓库（`SubsonicApiClient`、`JellyfinApiClient`、`NetEaseApiClient`），浏览与播放复用共享 UI；下载曲目保存在 `filesDir/streaming_downloads`，支持离线模式。
- **共享核心**：`MediaPlaybackService`（`:core:media`，`MediaLibraryService` + `MediaSessionService` / `MediaBrowserService`）持有一个 ExoPlayer，由 `RhythmPlayerEngine` 组装 `AudioProcessorChain`，无论来源本地还是流媒体都走同一输出管线。

## 🎛️ 播放与 DSP

`RhythmPlayerEngine` 覆写 `DefaultRenderersFactory.buildAudioSink()`，串联静音跳过、低音增强、空间化、单声道下混、ReplayGain 与 Sonic 处理器；通过 `EXTENSION_RENDERER_MODE_PREFER` 启用 Jellyfin FFmpeg 解码扩展，并使用 `OkHttpDataSource` + `CacheDataSource` + `ResolvingDataSource` 解析 `streaming://` URI。

EQ / DSP 状态与逻辑集中在 `EqDspController`（`:core:media`），由屏幕经接口调用；`RhythmBassBoostProcessor`、`RhythmSpatializationProcessor`、`RhythmMonoAudioProcessor` 均为自研 `AudioProcessor`。

## 🔀 导航

单 Activity（`MainActivity`），字符串路由，禁止用序列化对象定义路由。

- 根图：`app/.../navigation/RhythmNavigation.kt`（`NavHost(startDestination = "main")`、`composable("settings")`）
- 内层图：`LocalNavigation.kt`、`LocalNavigationRoutes.kt`、`LocalNavigationStreaming.kt`
- 路由常量：`feature/local/.../presentation/navigation/Screen.kt`（`sealed class Screen(val route: String)`）、`core/ui/.../SettingsRoutes.kt`、`feature/streaming/.../StreamingRoutes.kt`
- 新增页面 = 新增 Composable route 并在 `:app` 的导航装配处接线

示例路由：`"main"`、`"home"`、`"search"`、`"library?tab={tab}"`、`"player"`、`"settings"`、`"equalizer"`、`"rhythm_stats"`、`"playlist/{playlistId}"`。

## 💉 依赖注入

全仓 **Hilt**（`rhythm.hilt` 约定插件 + KSP），禁止引入其他 DI 框架。

- `@HiltAndroidApp` 仅出现在 `:app` 的 `RhythmApplication`
- `@AndroidEntryPoint`：`MainActivity`、`CrashActivity`、`ExternalPlaybackActivity`、`RhythmGuardTimeoutActivity`
- 库模块使用 `@HiltViewModel` / `@Module @InstallIn` / `@Inject`
- UI 取 ViewModel 一律 `hiltViewModel()`；`viewModel()` 不走 Hilt 工厂，多参构造必崩
- ViewModel 只持 Application 级 context，禁止持有 Activity / View / Fragment

## 📊 状态管理

`StateFlow` 单向流：私有 `MutableStateFlow` + `asStateFlow()` 暴露，派生状态用 `stateIn`，UI 统一 `collectAsState()`。非 MVI，无 Intent / Effect 层，屏幕直接调用 ViewModel 方法；状态模型不暴露可变集合。

设置的**唯一来源是 `AppSettings`**（`:core:data`），不另起 SharedPreferences 通道；其状态与写入按职责委托给 10 个分区文件（`AudioEqSettingsSection`、`CustomizationSettingsSection`、`GuardStatsSettingsSection`、`LibrarySettingsSection`、`LyricsSettingsSection`、`PlatformSettingsSection`、`PlaybackQueueSettingsSection`、`StreamingModeSettingsSection`、`ThemeSettingsSection`、`WidgetSettingsSection`）。

## 🌐 外部 API 三层门控

外部 API 功能走「**编译期开关 → 全局开关 → 单项开关**」三层门控：

1. `app/build.gradle.kts` 的 `BuildConfig` 开关（`ENABLE_LRCLIB`、`ENABLE_DEEZER` 等）经 `AppBuildInfo` 注入 `NetworkFeatureFlags`；
2. `AppSettings.integrationsEnabled` 全局开关（默认关闭）；
3. `AppSettings` 中每个 API 的单项开关（默认关闭）。

`NetworkClient`（`:core:network`）中的各 service 按开关**懒加载、可空**，调用前必须判空；网络日志走统一入口并保持脱敏。

## 🧪 测试架构

- 栈：**JUnit4 + MockWebServer3 + 真 `org.json`**；测试与源码同包，放在对应模块的 `src/test/java`（或 `src/test/kotlin`）
- 不打真实网络 / 数据库（HTTP 打桩），不用 `Thread.sleep` 等待异步；可测逻辑优先抽成顶层纯函数
- `testOptions.unitTests.isReturnDefaultValues = true` 是预期配置；`android.jar` stub 返回默认值属正常，不要因此引入 Robolectric
- 禁用：MockK、Turbine、coroutines-test、JUnit 5、Robolectric、截图测试框架
- **无 `androidTest` 源集，也不新建**；`:macrobenchmark` 提供启动、帧时序与 Baseline Profile 生成的测量用例
- lint / ktlint / detekt 刻意禁用（`AndroidCommon.kt` 硬禁用所有 `lint*` 任务），不以其输出作为改动依据

```kotlin
// JVM 单元测试示例：纯逻辑优先抽成顶层函数
@Test
fun `stable date added resolver keeps the original value`() {
    val resolved = resolveStableDateAdded(candidate = 0L, existing = 1_700_000_000L)
    assertEquals(1_700_000_000L, resolved)
}
```

运行方式见 [构建说明](Build-Instructions.md#-测试与本地验证)：按模块指定 `:xxx:testDebugUnitTest`，**禁止裸 `./gradlew test`**（会连带 release / benchmark 变体）。

## 🔐 安全与隐私

- 无分析或跟踪代码，所有数据保存在设备本地；除可选联网功能外不与服务器通信
- Manifest 声明 17 项权限，运行时权限由 `app/.../shared/presentation/components/PermissionHandler.kt` 通过 Accompanist Permissions 申请；Android 版本分支见 [权限指南](Permissions.md)
- `usesCleartextTraffic="true"` 与 `network_security_config.xml` 放行明文 HTTP / 用户 CA，是自托管服务器场景的既定取舍
- manifest 的 exported 组件与 intent-filter 是对外协议，勿收紧、勿改
- `keystore.properties` 不存在时回退 debug 签名；凭据文件与令牌不提交、不输出

## ⚡ 性能实践

- 列表用 `LazyColumn` 并给稳定 key；点击目标 ≥48dp 且提供 `contentDescription`
- 昂贵对象 `remember`；Composable 不做业务逻辑与 IO；副作用只走既有副作用 API
- 图片通过 Coil 加载并缓存；专辑封面在小部件侧另有 LRU 缓存
- 主线程不做 IO；Dispatcher 直接引用，不做注入抽象
- APK 仅含 `arm64-v8a`，无 ABI splits

## 🔧 构建系统

| 项目 | 值 / 位置 |
|:---|:---|
| 插件 | `rhythm.android.application`、`rhythm.android.library(.compose)`、`rhythm.hilt`（`build-logic/convention`） |
| Kotlin | AGP 9.x **内建 Kotlin**，禁止声明 `kotlin-android` |
| 身份 | `namespace = com.qlzhj.rhythm.app`，`applicationId = com.qlzhj.rhythm` |
| SDK | `compileSdk 37` / `minSdk 26` / `targetSdk 37`，Java 与 Kotlin `jvmTarget` 均为 21 |
| 版本 | `gradle/libs.versions.toml`（AGP 9.4.1、Kotlin 2.4.21、Gradle 9.7.1） |
| 产物 | `Rhythm-{versionName}-{buildType}.apk`，仅 `arm64-v8a` |
| 签名 | 根目录 `keystore.properties`，缺失时回退 debug 签名 |

> 以上为文档快照，版本与 SDK 值一律以源码 / Version Catalog 为准。

---

## 📚 延伸阅读

- [技术栈](Technology-Stack.md) · [API 文档](API-Documentation.md) · [构建说明](Build-Instructions.md) · [贡献指南](Contributing.md)
- [Jetpack Compose](https://developer.android.com/jetpack/compose) · [Android 架构指南](https://developer.android.com/topic/architecture) · [Media3](https://developer.android.com/guide/topics/media/media3) · [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) · [Material Design 3](https://m3.material.io/)
