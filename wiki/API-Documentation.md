> ⚠️ **本 wiki 为多模块拆分（2026-11）之前的历史快照。** 模块结构、构建命令与路径
> 已过时；权威信息见根目录 `AGENTS.md` 与 `docs/multi-module-refactor-plan.md`。
> 保留仅供历史参考，内容可能不再准确。

# API 文档

面向 Rhythm 代码库开发者的内部 API 参考。涵盖项目中实际使用的接口、类和模式。

> **注意：** 具体实现细节请始终以源代码为准。本文档提供关键架构组件的高层概述。

---

## 📚 概述

### 关键包

| 包 | 用途 |
|:---|:---|
| `chromahub.rhythm.app.core.domain` | 领域接口与模型 |
| `chromahub.rhythm.app.features.local` | 本地播放功能 |
| `chromahub.rhythm.app.features.streaming` | 流媒体播放功能 |
| `chromahub.rhythm.app.shared` | 共享工具、主题、导航 |
| `chromahub.rhythm.app.infrastructure` | 播放引擎、音频处理器、服务、小部件、后台任务 |
| `chromahub.rhythm.app.network` | 网络 API 服务、`NetworkClient` / `NetworkManager` |

> 注意：`chromahub.rhythm.app.network` 的源码位于 `infrastructure/network/` 目录，**package 与目录路径不一致**。这是仓库既有约定：新增网络接口必须继续声明 `package chromahub.rhythm.app.network`，import 时以目标文件的 `package` 声明为准，不要按目录路径推断。

---

## 🎵 播放服务

### MediaPlaybackService

**文件：** `infrastructure/service/player/RhythmPlayerEngine.kt`

核心播放引擎，基于 Media3 ExoPlayer 并使用自定义的 `DefaultRenderersFactory`，启用 FFmpeg 解码器扩展并串联应用的音频处理器（响度均衡(Replay Gain)、低音增强(Bass Boost)、虚拟化(Virtualizer)、单声道下混(Mono downmix)）：

```kotlin
val renderersFactory = object : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
        enableOffload: Boolean
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setAudioProcessorChain(audioProcessorChain)
            .build()
    }
}.apply {
    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
}
```

**服务：** `infrastructure/service/MediaPlaybackService.kt` 继承自 `MediaLibraryService`，负责管理：
- 用于外部控制的 `MediaLibrarySession`
- 通过 `RhythmPlayerEngine` 提供的 `Player` 实例
- 带播放控制的媒体通知
- 音频焦点处理
- 单声道音频下混状态（`RhythmMonoAudioProcessor`），通过设备配置面板切换

---

## 🗂️ 数据层

### MusicRepository

**文件：** `core/domain/repository/MusicRepository.kt`

```kotlin
interface MusicRepository {
    fun getSongs(): Flow<List<PlayableItem>>
    fun getAlbums(): Flow<List<AlbumItem>>
    fun getArtists(): Flow<List<ArtistItem>>
    fun getPlaylists(): Flow<List<PlaylistItem>>
    suspend fun getSongById(id: String): PlayableItem?
    suspend fun getAlbumById(id: String): AlbumItem?
    suspend fun getArtistById(id: String): ArtistItem?
    suspend fun getPlaylistById(id: String): PlaylistItem?
    suspend fun getSongsForAlbum(albumId: String): List<PlayableItem>
    suspend fun searchSongs(query: String): List<PlayableItem>
    suspend fun searchAlbums(query: String): List<AlbumItem>
    suspend fun searchArtists(query: String): List<ArtistItem>
    suspend fun searchPlaylists(query: String): List<PlaylistItem>
}
```

### 领域模型

所有标识符均为 `String` 类型。核心模型位于 `core/domain/model/`：

| 模型 | 关键字段 |
|:---|:---|
| `PlayableItem` | `id: String`, `title: String`, `artist: String?`, `album: String?`, `albumId: String?`, `duration: Long`, `uri: String?` |
| `AlbumItem` | `id: String`, `title: String`, `artist: String?`, `year: Int?`, `artworkUri: Uri?` |
| `ArtistItem` | `id: String`, `name: String`, `albumCount: Int`, `trackCount: Int` |
| `PlaylistItem` | `id: String`, `name: String`, `dateCreated: Long`, `dateModified: Long` |

---

## 🌐 网络 API

### LRCLib（同步歌词）

```kotlin
interface LRCLibApiService {
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): Response<List<LyricsData>>
}
```

### Deezer（专辑封面）

```kotlin
interface DeezerApiService {
    @GET("search/track")
    suspend fun searchTrack(
        @Query("q") query: String
    ): Response<DeezerSearchResponse>
}
```

### YouTube Music（专辑封面）

```kotlin
interface YouTubeMusicApiService {
    @GET("api/music/song")
    suspend fun searchSong(
        @Query("q") query: String
    ): Response<YouTubeMusicResponse>
}
```

---

## 📱 小部件

### Glance 小部件

| 小部件 | 类 |
|:---|:---|
| 音乐播放器小部件 | `RhythmMusicWidget : GlanceAppWidget()` |
| 歌词小部件 | `RhythmLyricsWidget : GlanceAppWidget()` |

小部件更新由 `RhythmWidgetWorker`（一个 `CoroutineWorker`）和 `GlanceWidgetUpdater` 管理。

---

## 🎨 状态管理

- **播放状态**：通过 ExoPlayer `Player` 接口上的 `Player.Listener` 观察
- **UI 状态**：ViewModel 中的 `StateFlow`，通过 `collectAsState()` 以 Compose 状态形式收集
- **设置**：由 `DataStore` / `SharedPreferences` 支撑的 `AppSettings` 类
- **导航**：Compose Navigation，配合 sealed `Route` 类层次结构

---

## 📦 构建配置

### BuildConfig 标志

所有标志均为 `true`：

| 标志 | 用途 |
|:---|:---|
| `ENABLE_YOUTUBE_MUSIC` | YouTube Music 封面搜索 |
| `ENABLE_SPOTIFY_SEARCH` | Spotify 元数据搜索 |
| `ENABLE_LYRICALLY_API` | Lyricall API 歌词 |
| `ENABLE_DEEZER` | Deezer 封面搜索 |
| `ENABLE_LRCLIB` | LRCLib 同步歌词 |
| `ENABLE_BETTERLYRICS` | BetterLyrics 多源歌词回退 |
| `ENABLE_WIKIPEDIA` | Wikipedia 艺人简介查询 |
| `FLAVOR` | 分发渠道标识（`github`）。这是 `BuildConfig` 常量，**不代表存在 product flavor**；仓库只有 `debug` / `release` / `benchmark` |
| `IS_NIGHTLY` | 每夜构建与稳定渠道 |
| `APPLE_MUSIC_FALLBACK_TOKEN` | Apple Music 封面令牌，由环境变量 / `local.properties` 注入 `BuildConfig`；文档只记录变量名，不记录任何值 |

### 依赖版本

| 依赖 | 版本 |
|:---|:---|
| AGP | `9.4.1` |
| Kotlin | `2.4.21` |
| KSP | `2.3.6` |
| Compose BOM | `2026.09.00` |
| Material3 | `1.5.0-beta01` |
| Media3 | `1.11.1` |
| FFmpeg Decoder | `1.9.0+1`（Jellyfin fork） |
| Gradle | `9.7.1` |

> 以上为文档快照；依赖版本以 `gradle/libs.versions.toml` 为准，Gradle 版本以 `gradle/wrapper/gradle-wrapper.properties` 为准。

---

## 📚 延伸阅读

- [架构指南](Architecture.md)
- [技术栈](Technology-Stack.md)
- [贡献指南](Contributing.md)

---

**有问题？** 查看 [FAQ](FAQ.md)
