> ℹ️ **文档状态**：本 wiki 已按 2026-11 多模块拆分与唯一包名重构后的代码更新。
> 约定与红线见根目录 [AGENTS.md](../AGENTS.md)，设计与重构历史见 [docs/](../docs/)；一切事实以源码为准。

# API 文档

面向 Rhythm 代码库开发者的内部 API 参考：模块入口、关键接口与服务。具体实现细节始终以源代码为准。

> 本文中的「API」包含内部接口与外部网络服务；Rhythm 不对外提供 SDK。

---

## 📚 模块与包

2026-11 完成唯一包名重构，`android.uniquePackageNames=true` 由 `gradle.properties` 强制。

| 包根 | 模块 | 主要内容 |
|:---|:---|:---|
| `com.qlzhj.rhythm.app` | `:app` | Application、单 Activity、导航装配、引导页、`AppModule` |
| `com.qlzhj.rhythm.core.model` | `:core:model` | 领域模型 |
| `com.qlzhj.rhythm.core.common` | `:core:common` | 工具与缝隙接口（`AppBuildInfo`、`NetworkLogRedactor` 等） |
| `com.qlzhj.rhythm.core.designsystem` | `:core:designsystem` | strings / theme / icons / 基础组件 |
| `com.qlzhj.rhythm.core.network` | `:core:network` | `NetworkClient`、各外部 API service |
| `com.qlzhj.rhythm.core.data` | `:core:data` | `AppSettings`、Room、仓储、统计 |
| `com.qlzhj.rhythm.core.media` | `:core:media` | 播放服务、引擎、DSP、歌词解析 |
| `com.qlzhj.rhythm.core.ui` | `:core:ui` | 跨 feature 共享组件与 `SettingsRoutes` |
| `com.qlzhj.rhythm.feature.local` | `:feature:local` | 本地曲库、播放器、`Screen` 路由 |
| `com.qlzhj.rhythm.feature.streaming` | `:feature:streaming` | provider、Go 模式 UI、`StreamingRoutes` |
| `com.qlzhj.rhythm.feature.settings` | `:feature:settings` | 设置页 |
| `com.qlzhj.rhythm.feature.widget` | `:feature:widget` | Glance 小组件与磁贴 |

> `applicationId = com.qlzhj.rhythm`（debug `.debug`）与代码包根分离。自定义 action / broadcast / extra 协议串仍保持 `chromahub.rhythm.app.*` 字面前缀不变；`macrobenchmark` 仍使用 `chromahub.rhythm.app.benchmark` 包。

---

## 🚪 入口与装配

| 组件 | 位置 | 说明 |
|:---|:---|:---|
| `RhythmApplication` | `:app` `@HiltAndroidApp`，实现 `ImageLoaderFactory` | 注入 `AppBuildInfo`、初始化网络开关与设置预热 |
| `MainActivity` | `:app` `@AndroidEntryPoint` | 单 Activity Compose 宿主 |
| 根导航 | `app/.../navigation/RhythmNavigation.kt` | `NavHost(startDestination = "main")`、`composable("settings")` |
| 内层导航 | `LocalNavigation.kt` / `LocalNavigationRoutes.kt` / `LocalNavigationStreaming.kt` | 本地与流媒体页面 route body |
| 路由常量 | `Screen.kt`、`SettingsRoutes.kt`、`StreamingRoutes.kt` | 字符串路由，禁止序列化对象路由 |
| Hilt 装配 | `app/.../di/NetworkClientInitializer.kt`、`feature/streaming/.../di/StreamingMusicModule.kt` | `@Module @InstallIn(SingletonComponent::class)` |

示例路由：`"main"`、`"settings"`、`"home"`、`"search"`、`"library?tab={tab}"`、`"player"`、`"equalizer"`、`"rhythm_stats"`、`"playlist/{playlistId}"`、`"tuner_go_settings"`。

新增页面 = 新增 Composable route 并在 `:app` 的导航装配处接线。

---

## 🎵 播放服务

**`MediaPlaybackService`**（`:core:media`，`MediaLibraryService` + `Player.Listener`，`exported=true`）：管理 `MediaLibrarySession`、`Player` 实例、媒体通知、音频焦点、Rhythm Guard 音量钳制与主循环计时。Manifest 同时声明 `MediaSessionService` 与 `MediaBrowserService` action。

**`RhythmPlayerEngine`**（`:core:media/.../infrastructure/service/player/`）：覆写 `DefaultRenderersFactory.buildAudioSink()` 组装 `AudioProcessorChain`，设置 float PCM 输出与 FFmpeg 扩展渲染模式，并用 `OkHttpDataSource` + `CacheDataSource` + `ResolvingDataSource` 解析 `streaming://` URI。

```kotlin
val renderersFactory = object : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
        enableOffload: Boolean
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setAudioProcessorChain(audioProcessorChain)
        .setEnableFloatOutput(floatOutputEnabled)
        .build()
}.apply {
    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
}
```

**`EqDspController`**（`:core:media/.../infrastructure/service/`）：EQ / DSP 状态与逻辑的唯一持有者，通过 `AudioEffectsController` 接口暴露给屏幕；配套 `RhythmBassBoostProcessor`、`RhythmSpatializationProcessor`、`RhythmMonoAudioProcessor`、`ReplayGainAudioProcessor`、`TransitionController`（交叉淡入淡出）。

---

## 🗂️ 数据层

- **`AppSettings`**（`:core:data`，实现 `NetworkSettingsProvider`）：设置唯一来源；状态与写入委托给 10 个 `*SettingsSection` 分区文件。新增设置项不要另起 SharedPreferences 通道。
- **`LocalMusicRepository`**（`:feature:local`）：`MediaStore` / 文件夹扫描、曲库索引、播放列表、歌词获取与缓存。
- **`StreamingMusicRepositoryImpl`**（`:feature:streaming`）：远程目录、下载索引（`downloaded_songs.json`）、离线模式；经 `StreamingMusicModule` 的 `@Binds` 注入。
- **`PlaybackStatsRepository`**（`:core:data`）：Rhythm Stats 统计与 `StatsTimeRange`。
- **Room / DataStore**：位于 `:core:data`，KSP 生成 Room 代码。

领域模型位于 `:core:model`，标识符均为 `String`：

| 模型 | 关键字段 |
|:---|:---|
| `PlayableItem` | `id`, `title`, `artist`, `album`, `albumId`, `duration`, `uri` |
| `AlbumItem` / `ArtistItem` / `PlaylistItem` | 曲库浏览实体 |
| `StreamingSong` / `StreamingAlbum` / `StreamingArtist` / `StreamingPlaylist` | 流媒体目录实体 |
| `AppMode` / `StreamingServiceId` / `StreamingConfig` / `StreamingQuality` | 模式与服务配置 |
| `AutoEQProfile` / `CustomEqualizerPreset` | 均衡器预设 |
| `LyricsData` / `LyricsSourcePreference` | 歌词与来源偏好 |
| `SemanticLyrics`（`:core:common`） | 逐词 / 多声部歌词解析结果 |

> 仓库不使用强制的 UseCase 层；仅多处复用的逻辑才抽取。ViewModel 不直接触碰 SQL、MediaStore、HTTP。

---

## 🌐 网络层

**`NetworkClient`**（`:core:network`）：集中持有各外部 API 的 Retrofit / OkHttp 客户端，日志走 `NetworkLog` 统一入口并脱敏。service 按开关**懒加载、可空**，调用前必须判空。

### 三层门控

```
BuildConfig 开关 (app/build.gradle.kts)
      ↓  AppBuildInfo → NetworkFeatureFlags → NetworkClientInitializer
全局开关 AppSettings.integrationsEnabled（默认 false）
      ↓
单项开关 AppSettings.xxxApiEnabled（默认 false，置 true 会自动打开全局开关）
      ↓
NetworkClient.isXxxApiEnabled() → 可空 service 判空后调用
```

引导页按编译期 flags 决定开关可见性；设置页「API 管理」负责全局与单项开关。

### 外部 API service（`:core:network`）

| Service / Provider | 用途 |
|:---|:---|
| `LRCLibApiService` | `@GET("api/search")`、`@GET("api/get/{id}")`，同步歌词 |
| `BetterLyricsApiService` / `RhythmLyricsApiService` | 多源歌词、逐词 / TTML 歌词 |
| `DeezerApiService` | 专辑封面搜索 |
| `YTMusicApiService` / `SpotifySearchApiService` / `ITunesSearchApiService` | 封面与元数据查询 |
| `WikipediaProvider` / `AppleMusicCanvasProvider` | 艺人简介 / Canvas 封面 |
| `GitHubApiService` | 应用内更新检查 |
| `NetworkManager` / `NetworkLog` / `NetworkSettingsProvider` | 网络状态、脱敏日志、设置缝隙接口 |

`:core:network` 已迁移到 **kotlinx.serialization**；Gson 仍用于 assets（如 AutoEQ 预设）等场景。

### 流媒体 provider（`:feature:streaming`）

| Provider | 实现 | 鉴权 |
|:---|:---|:---|
| Subsonic 兼容（含 Navidrome / Gonic / Airsonic） | `SubsonicApiClient` | 服务器 URL + 用户名 + 密码；默认 `token = md5(password + salt)` + `salt`，服务端拒绝 token 时回退 `enc:` / 明文密码 |
| Jellyfin | `JellyfinApiClient` | `POST /Users/AuthenticateByName` → `AccessToken` + `UserId`；后续 `Authorization: MediaBrowser ...` 与 `X-Emby-Token`；自签证书由 `UserTrustManager` 处理 |
| 网易云音乐 | `NetEaseApiClient` + `NetEaseCrypto` | 二维码登录（unikey 轮询 + `MUSIC_U` cookie），weapi / eapi 加密，无服务器 URL |

`StreamingServiceId` 只有 `SUBSONIC` / `JELLYFIN` / `NETEASE` 三个常量：Navidrome **不是**独立 provider。附近服务器发现由 `NearbyServerScanner`（`NsdManager` + Jellyfin UDP 探测）实现。

---

## 📱 小部件

| 组件 | 说明 |
|:---|:---|
| `RhythmMusicWidget` / `RhythmWidgetReceiver` | 音乐小部件，11 个尺寸断点 |
| `RhythmLyricsWidget` / `RhythmLyricsWidgetReceiver` | 多行歌词 + 当前行高亮 |
| `RhythmCookieWidget` / `RhythmCookieWidgetReceiver` | 12 边形封面布局 |
| `RhythmStatsWidget` / `RhythmStatsWidgetReceiver` | 收听时长与连续天数 |
| `RhythmTileService` | Quick Settings 磁贴 |
| `GlanceWidgetUpdater` | 状态变更时写 Glance state 并 `updateAll()` |
| `RhythmWidgetWorker` | `CoroutineWorker`，仅一次性延迟兜底刷新 |

所有 `appwidget-provider` 的 `updatePeriodMillis=0`，**不存在周期刷新任务**。

---

## 🎨 状态与导航约定

- 播放状态通过 ExoPlayer `Player.Listener` 观察，转换为 `StateFlow`
- UI 状态：`StateFlow`（私有 `Mutable` + `asStateFlow()`，派生用 `stateIn`），Compose 侧 `collectAsState()`
- 屏幕直接调用 ViewModel 方法，无 Intent / Effect 层
- UI 取 ViewModel 一律 `hiltViewModel()`
- 设置唯一来源 `AppSettings`；导航使用字符串路由

---

## 📦 构建配置

### BuildConfig 标志

| 标志 | 用途 |
|:---|:---|
| `ENABLE_YOUTUBE_MUSIC` | YouTube Music 封面搜索 |
| `ENABLE_SPOTIFY_SEARCH` | Spotify 元数据搜索 |
| `ENABLE_LYRICALLY_API` | Lyrically 歌词 |
| `ENABLE_DEEZER` | Deezer 封面搜索 |
| `ENABLE_LRCLIB` | LRCLib 同步歌词 |
| `ENABLE_BETTERLYRICS` | BetterLyrics 多源歌词 |
| `ENABLE_WIKIPEDIA` | Wikipedia 艺人简介 |
| `FLAVOR` | 分发渠道标识（`github`）；**不代表存在 product flavor** |
| `IS_NIGHTLY` | 每夜构建标记 |
| `RELEASE_DATE` | 发布日期（可由 `-PreleaseDateOverride` 覆盖） |
| `APPLE_MUSIC_FALLBACK_TOKEN` | 由环境变量 / `local.properties` 注入；本文只记录变量名，不记录任何值 |

> 看似无用的 BuildConfig 开关、nightly / 日期字段部分仍被 UI 读取，**勿删**。

### 依赖版本快照

| 依赖 | 版本 | 依赖 | 版本 |
|:---|:---|:---|:---|
| AGP | `9.4.1` | Kotlin | `2.4.21` |
| KSP | `2.3.6` | Gradle | `9.7.1` |
| Compose BOM | `2026.09.00` | Material3 | `1.5.0-beta01` |
| Media3 | `1.11.1` | FFmpeg Decoder | `1.9.0+1` |
| Hilt | `2.60.1` | Room | `2.8.5` |

> 以 `gradle/libs.versions.toml` 与 `gradle/wrapper/gradle-wrapper.properties` 为准。

---

## 📚 延伸阅读

- [架构指南](Architecture.md) · [技术栈](Technology-Stack.md) · [贡献指南](Contributing.md) · [构建说明](Build-Instructions.md)
- [常见问题](FAQ.md)
