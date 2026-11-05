# 技术栈

本文档详细介绍 Rhythm 音乐播放器中使用的技术架构和库。

## 🏗️ 核心技术

### UI 与设计

| 技术 | 用途 |
|:---|:---|
| **Jetpack Compose** | Android 现代声明式 UI 工具包 |
| **Material 3** | Material Design 组件与主题系统 |
| **Material Symbols Variable Font** | 替代已弃用的 material-icons-extended 的自定义静态字体资源 |
| **AndroidX Palette** | 从图片中提取动态颜色 |

### 音频与媒体

| 技术 | 用途 |
|:---|:---|
| **Media3 ExoPlayer** | 专业级媒体播放引擎 |
| **FFmpeg Decoder** | 通过 Jellyfin 的 media3-ffmpeg-decoder 扩展提供扩展编解码器支持（EAC3-JOC、AC-3、AC-4、WMA） |
| **Audio Processors** | 响度均衡(Replay Gain)、低音增强(Bass Boost)、虚拟化(Virtualizer)和单声道下混 DSP 链 |
| **MediaStore API** | Android 媒体内容提供器 |
| **AudioFocus** | 面向通话/通知的音频焦点管理 |

### 小部件

| 技术 | 用途 |
|:---|:---|
| **Glance** | 采用 Material 3 设计的现代响应式小部件 |
| **WorkManager** | 小部件的后台更新 |

### 编程语言

| 技术 | 用途 |
|:---|:---|
| **Kotlin** | 100% Kotlin 代码库 |
| **Kotlin Coroutines** | 异步编程 |
| **Kotlin Flow** | 响应式流与状态管理 |

## 🎨 架构

### 设计模式

**MVVM（Model-View-ViewModel）+ Clean Architecture**

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│  ┌──────────────────────────────────────────┐   │
│  │   Composables (Screens & Components)     │   │
│  └──────────────────────────────────────────┘   │
│                      ↕                           │
│  ┌──────────────────────────────────────────┐   │
│  │         ViewModels (State)               │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│                 Domain Layer                     │
│  ┌──────────────────────────────────────────┐   │
│  │      Use Cases (Business Logic)          │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │     Repository Interfaces                │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │      Models (Data Entities)              │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│                  Data Layer                      │
│  ┌──────────────────────────────────────────┐   │
│  │    Repository Implementations            │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │  Data Sources (Local & Remote)           │   │
│  │  • MediaStore                            │   │
│  │  • LRCLib API                            │   │
│  │  • Deezer API                            │   │
│  │  • Local Storage                         │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### 项目结构

```
app/src/main/java/chromahub/rhythm/app/
├── activities/                  # Main Android activities (e.g. MainActivity.kt)
├── core/                        # Core Shared Business/Domain Logic
│   └── domain/
│       ├── model/               # Core domain entities (Song, Album, Artist, Playlist)
│       ├── repository/          # Core repository interfaces
│       └── usecase/             # Core business use cases
├── features/                    # Feature Modules
│   ├── local/                   # Local media playback feature (Clean Architecture)
│   │   ├── data/                # Local repositories, room database, MediaStore integration
│   │   ├── di/                  # Local dependency injection configs
│   │   ├── domain/              # Local use cases and business logic
│   │   └── presentation/        # Local screens, viewmodels, themes, and views
│   └── streaming/               # Streaming server client feature (Clean Architecture)
│       ├── data/                # Remote repositories and networking clients
│       ├── di/                  # Streaming dependency injection configs
│       ├── domain/              # Streaming use cases
│       └── presentation/        # Streaming screens and viewmodels
├── infrastructure/              # Base Infrastructure layer
│   ├── audio/                   # ExoPlayer setup, controller, and FFmpeg configuration
│   ├── network/                 # Retrofit, OkHttp, and REST API definitions
│   ├── service/                 # MusicService.kt & background media session handlers
│   ├── widget/                  # Glance app widget definitions
│   └── worker/                  # WorkManager background sync/scan workers
├── shared/                      # Shared Cross-Cutting Presentation/Domain/Data components
│   ├── data/
│   ├── domain/
│   └── presentation/            # Shared UI elements, color themes, styling, and custom icons
└── util/ & utils/               # General utility files and extension functions
```

## 📦 库与依赖

### AndroidX 与 Jetpack

```kotlin
// Core
androidx.core:core-ktx
androidx.core:core-splashscreen
androidx.lifecycle:lifecycle-runtime-ktx
androidx.lifecycle:lifecycle-viewmodel-compose
androidx.fragment:fragment-ktx

// Compose
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.compose.ui:ui-tooling

// Navigation
androidx.navigation:navigation-compose

// Media
androidx.media3:media3-exoplayer
androidx.media3:media3-session
androidx.media3:media3-ui

// Widgets
androidx.glance:glance-appwidget
androidx.work:work-runtime-ktx

// Room Database
androidx.room:room-runtime
androidx.room:room-ktx

// Other
androidx.palette:palette-ktx
```

### 网络

```kotlin
// HTTP Client
com.squareup.retrofit2:retrofit
com.squareup.retrofit2:converter-gson
com.squareup.okhttp3:okhttp
com.squareup.okhttp3:logging-interceptor

// JSON
com.google.code.gson:gson
```

### 图片加载

```kotlin
// Coil for Compose
io.coil-kt:coil-compose
```

### 工具库

```kotlin
// Permissions
com.google.accompanist:accompanist-permissions

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android
```

## 🔄 状态管理

### StateFlow 与 Compose 状态

Rhythm 使用 Kotlin Flow 和 Compose 状态实现响应式 UI 更新：

```kotlin
// ViewModel observes ExoPlayer state via MediaController
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private var mediaController: MediaController? = null

    val playbackState: StateFlow<@Player.State Int> = 
        MutableStateFlow(Player.STATE_IDLE)

    fun connect(controller: MediaController) {
        mediaController = controller
        controller.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                (playbackState as MutableStateFlow).value = state
            }
        })
    }
}
```

### 仓库模式

通过仓库抽象数据访问：

```kotlin
interface MusicRepository {
    fun getSongs(): Flow<List<PlayableItem>>
    fun getAlbums(): Flow<List<AlbumItem>>
    fun getArtists(): Flow<List<ArtistItem>>
    fun getPlaylists(): Flow<List<PlaylistItem>>
    suspend fun getSongById(id: String): PlayableItem?
    suspend fun getAlbumById(id: String): AlbumItem?
    suspend fun searchSongs(query: String): List<PlayableItem>
}
```

## 🎵 音频播放架构

### ExoPlayer 集成

```
┌─────────────────────────────────────┐
│         MusicService                │
│   (Foreground Service)              │
│                                     │
│  ┌──────────────────────────────┐  │
│  │      ExoPlayer               │  │
│  │  • Media3 ExoPlayer 1.11.1   │  │
│  │  • FFmpeg decoder extension  │  │
│  │  • Gapless playback          │  │
│  │  • Audio focus handling      │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   MediaSession               │  │
│  │  • Playback state            │  │
│  │  • Queue management          │  │
│  │  • Media buttons             │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   MediaNotification          │  │
│  │  • Playback controls         │  │
│  │  • Album art                 │  │
│  │  • Metadata display          │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
            ↕
┌─────────────────────────────────────┐
│     UI (Player Composables)         │
│  • Observe playback state           │
│  • Send playback commands           │
│  • Display metadata                 │
└─────────────────────────────────────┘
```

## 📱 音频处理架构

Rhythm 通过 `RhythmPlayerEngine` 内部的 `RhythmAudioProcessor` 管线串联音频效果：

```
┌────────────────────────────────────────────┐
│            RhythmPlayerEngine             │
│   (infrastructure/service/player/)        │
│                                            │
│  ┌────────────────────────────────────┐   │
│  │      RhythmAudioProcessor         │   │
│  │  • Replay Gain (album/track)      │   │
│  │  • Bass Boost                     │   │
│  │  • Virtualizer / Spatialization   │   │
│  │  • Mono downmix (RhythmMono       │   │
│  │    AudioProcessor)                │   │
│  └────────────────────────────────────┘   │
│                                            │
│  • Posture-aware player layouts (#529)     │
│  • Device-specific audio routing           │
└────────────────────────────────────────────┘
```

## 📱 小部件架构

### Glance 小部件（现代方案）

```kotlin
class RhythmMusicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            RhythmMusicWidgetContent()
        }
    }
}

@Composable
fun RhythmMusicWidgetContent() {
    // Observe playback data via GlanceState
    // Material 3 widget UI
    MaterialTheme {
        // Widget content with play/pause, skip, track info, album art
    }
}
```

### 后台更新

```kotlin
class RhythmWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Update widget data from current playback state
        GlanceAppWidgetManager(context)
            .getGlanceIds(RhythmMusicWidget::class.java)
            .forEach { glanceId ->
                RhythmMusicWidget().update(context, glanceId)
            }
        return Result.success()
    }
}
```

## 🔧 构建系统

### Gradle Kotlin DSL

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

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
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }
}
```

### 版本目录

```toml
# gradle/libs.versions.toml
[versions]
agp = "9.4.1"
kotlin = "2.4.21"
ksp = "2.3.6"
composeBom = "2026.09.00"
material3 = "1.5.0-beta01"
media3 = "1.11.1"

[libraries]
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-material3-android = { group = "androidx.compose.material3", name = "material3-android", version.ref = "material3" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
```

> 以上为文档快照；依赖版本以 `gradle/libs.versions.toml`、Gradle 版本以 `gradle/wrapper/gradle-wrapper.properties`、`versionCode` / `versionName` 以 `app/build.gradle.kts` 为准。

## 🧪 测试

### 单元测试
- ViewModel 逻辑测试（JUnit 4）
- 仓库测试
- 用例测试

### UI 测试
- Compose UI 测试（Compose Test）
- 导航测试
- 集成测试
- 用于基线配置文件的 Macrobenchmark

### 构建与运行测试
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## 🔐 安全与隐私

- **无分析**：零跟踪代码
- **本地存储**：所有数据都存储在设备上
- **最小权限**：仅申请必要权限
- **FOSS 合规**：完全开源
- **可复现构建**：一致的 APK 生成

## 📊 性能优化

- **懒加载**：按需加载音乐库
- **图片缓存**：Coil 高效缓存专辑封面
- **后台处理**：使用 WorkManager 处理非紧急任务
- **Compose 优化**：Remember、derivedStateOf、keys
- **ExoPlayer 缓冲**：优化的缓冲区大小

## 🔄 CI/CD

- 使用 GitHub Actions 进行自动化构建（android.yml、beta.yml、release.yml）
- 推送时自动运行测试
- 带签名的版本发布自动化
- 代码质量检查与 lint
- 支持可复现构建

---

**想参与贡献？** 查看[贡献指南](Contributing.md)！
