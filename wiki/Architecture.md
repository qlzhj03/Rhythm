> ⚠️ **本 wiki 为多模块拆分（2026-11）之前的历史快照。** 模块结构、构建命令与路径
> 已过时；权威信息见根目录 `AGENTS.md` 与 `docs/multi-module-refactor-plan.md`。
> 保留仅供历史参考，内容可能不再准确。

# Rhythm 架构指南

Rhythm 应用结构、设计模式与架构决策的技术文档。

## 🔄 双模式架构

Rhythm 采用独特的双模式架构，在共享核心基础设施的同时支持本地播放与流媒体播放体验。

### 本地模式 (`features/local`)
聚焦于基于设备的媒体，使用 Android `MediaStore` API。它负责本地文件索引、从文件中提取元数据以及本地播放状态。

### 流媒体模式 (`features/streaming`)
为流媒体服务器提供完全独立的处理管线。它包含自己的数据仓库和展示层，使应用能够作为流媒体客户端运行而不干扰本地音乐库。在近期版本中，流媒体 UI 已逐步合并到共享的本地 UI 中，同时保留其专用的仓库和 ViewModel。

### 共享核心
两种模式都利用 `shared` 和 `infrastructure` 层：
- **共享数据**：通用的领域模型（Song、Album、Artist）保证一致性。
- **播放服务**：统一的 `MediaPlaybackService` 通过 ExoPlayer 处理实际的音频输出，无论来源是本地还是流媒体。
- **音频处理器**：音频管线（`RhythmAudioProcessor`）在输出前串联响度均衡(Replay Gain)、低音增强(Bass Boost)、虚拟化/空间化(Virtualizer/Spatialization)以及单声道下混(`RhythmMonoAudioProcessor`)等效果。
- **基础设施**：网络、权限和后台任务等通用工具被两种模式共用。

### 状态观察

播放状态通过附加到 ExoPlayer `Player` 接口的 `Player.Listener` 来观察。

```kotlin
// Observe ExoPlayer state via MediaController
mediaController.addListener(object : Player.Listener {
    override fun onPlaybackStateChanged(state: Int) { ... }
    override fun onIsPlayingChanged(isPlaying: Boolean) { ... }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { ... }
})

// In Compose, use collectAsState() on StateFlow from ViewModel
@Composable
fun PlayerScreen() {
    val playbackState by viewModel.playbackState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
}
```

---

## 🌐 网络层

### API 集成

```kotlin
// LRCLib API for synchronized lyrics
interface LRCLibApiService {
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): Response<List<LyricsData>>
}

// Deezer API for artwork
interface DeezerApiService {
    @GET("search/track")
    suspend fun searchTrack(
        @Query("q") query: String
    ): Response<DeezerSearchResponse>
}

// YouTube Music API for artwork
interface YouTubeMusicApiService {
    @GET("api/music/song")
    suspend fun searchSong(
        @Query("q") query: String
    ): Response<YouTubeMusicResponse>
}
```

---

## 🧪 测试架构

### 单元测试

```kotlin
class MusicViewModelTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `player displays current track title`() {
        composeTestRule
            .onNodeWithTag("track_title")
            .assertIsDisplayed()
    }
}
```

---

## 🔐 安全与隐私

### 数据隐私

- 没有分析或跟踪代码
- 所有数据都存储在本地
- 除可选功能外不与服务器通信

### 权限

```kotlin
// Runtime permission requests are handled via Accompanist Permissions API
// and AndroidX Activity Result API at the composable level
@Composable
fun RequestAudioPermission() {
    val permissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            android.Manifest.permission.READ_MEDIA_AUDIO
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    // LaunchedEffect to trigger request
}
```

---

## ⚡ 性能优化

### 懒加载

```kotlin
@Composable
fun SongList(songs: List<Song>) {
    LazyColumn {
        items(songs, key = { it.id }) { song ->
            SongItem(song)
        }
    }
}
```

### 图片缓存

```kotlin
// Coil for efficient image loading
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(song.albumArtUri)
        .crossfade(true)
        .build(),
    contentDescription = "Album Art"
)
```

### 后台处理

```kotlin
// Media scanning happens via ContentResolver + MediaStore queries
// in the MusicRepository layer, not in a dedicated worker
suspend fun scanMedia(context: Context): List<PlayableItem> {
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DATA
    )
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection, null, null, null
    )
    // Parse cursor into PlayableItem list
}
```

---

## 📊 依赖注入

目前使用手动 DI（手动构造函数注入 + 功能模块中的服务定位器模式）。

```kotlin
// MusicViewModel receives a MediaController from MediaPlaybackService
// and interacts with it directly for playback control
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private var mediaController: MediaController? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun onConnected(controller: MediaController) {
        mediaController = controller
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }
}
```

---

## 🔄 构建系统

### Gradle Kotlin DSL

```kotlin
// build.gradle.kts
android {
    namespace = "chromahub.rhythm.app"
    compileSdk = 37
    
    defaultConfig {
        applicationId = "chromahub.rhythm.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 555001306
        versionName = "5.5.500.1306 Beta"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

> 以上为文档快照；`versionCode` / `versionName` 与 SDK 版本一律以 `app/build.gradle.kts` 为准。

### 版本目录

```toml
[versions]
agp = "9.4.1"
kotlin = "2.4.21"
ksp = "2.3.6"
composeBom = "2026.09.00"
material3 = "1.5.0-beta01"
media3 = "1.11.1"

[libraries]
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
```

> 以上为文档快照；依赖版本以 `gradle/libs.versions.toml` 为准，Gradle 版本以 `gradle/wrapper/gradle-wrapper.properties` 为准。

---

## 🎯 设计模式

### 仓库模式
- 对数据源的抽象
- 可测试的业务逻辑
- 单一数据源

### 观察者模式
- 使用 StateFlow 实现响应式更新
- LiveData 的替代方案
- 生命周期感知

### 工厂模式
- ViewModel 的创建
- 小部件的实例化

### 单例模式
- AppSettings
- 仓库实例

---

## 📚 延伸阅读

- [Jetpack Compose 文档](https://developer.android.com/jetpack/compose)
- [Android 架构指南](https://developer.android.com/topic/architecture)
- [Media3 文档](https://developer.android.com/guide/topics/media/media3)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Material Design 3](https://m3.material.io/)

---

**有问题？** 查看[贡献指南](Contributing.md)
