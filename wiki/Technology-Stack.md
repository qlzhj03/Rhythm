> ℹ️ **文档状态**：本 wiki 已按 2026-11 多模块拆分与唯一包名重构后的代码更新。
> 约定与红线见根目录 [AGENTS.md](../AGENTS.md)，设计与重构历史见 [docs/](../docs/)；一切事实以源码为准。

# 技术栈

本文档介绍 Rhythm 当前使用的技术、模块划分与依赖。所有版本以 `gradle/libs.versions.toml`、`gradle/wrapper/gradle-wrapper.properties` 与 `app/build.gradle.kts` 为准。

## 🏗️ 核心技术

### UI 与设计

| 技术 | 用途 |
|:---|:---|
| **Jetpack Compose** | 声明式 UI，单 Activity 架构 |
| **Material 3 Expressive** | `1.5.0-beta01`，自适应形状与表现力组件；Experimental API 已在构建配置全局 opt-in |
| **Material Symbols Variable Font** | 项目自有可变字重图标体系，禁用 `material-icons-extended` |
| **AndroidX Palette** | 从封面提取动态颜色 |
| **Coil** | 专辑封面加载与缓存 |
| **Navigation Compose** | 字符串路由导航（禁止序列化对象路由） |
| **AndroidX Window / WindowSizeClass** | 平板多栏与折叠屏姿态适配 |

### 音频与媒体

| 技术 | 用途 |
|:---|:---|
| **Media3 ExoPlayer** | `1.11.1`，含 `exoplayer` / `dash` / `hls` / `session` / `datasource-okhttp` / `exoplayer-midi` |
| **FFmpeg Decoder** | Jellyfin `media3-ffmpeg-decoder 1.9.0+1`，扩展 EAC3-JOC、AC-3、AC-4、WMA、ALAC、DTS、TrueHD 等解码 |
| **自定义 AudioProcessor** | ReplayGain、10 段 EQ、低音增强、空间化、单声道下混、静音跳过 DSP 链 |
| **MediaStore API** | 本地曲库索引与元数据 |
| **AudioFocus** | 来电 / 通知 / 设备切换时的焦点管理 |
| **JAudioTagger / TagLib** | 元数据与歌词标签读写 |

### 数据、网络与后台

| 技术 | 用途 |
|:---|:---|
| **Room + SQLite** | 播放列表、统计数据、缓存索引 |
| **DataStore / AppSettings** | 设置的唯一来源（`AppSettings`，委托给 10 个分区文件） |
| **Hilt** | 全仓依赖注入，UI 一律 `hiltViewModel()` |
| **Retrofit + OkHttp** | `3.0.0` / `5.5.0`，外部 API 与流媒体请求 |
| **kotlinx.serialization** | `:core:network` 的 JSON 模型（已迁移）；Gson 仍用于 assets（如 AutoEQ 预设）等场景 |
| **WorkManager** | 小部件的一次性延迟刷新与后台任务 |
| **Glance** | 4 款桌面小组件；另有 `TileService` 快捷磁贴 |
| **Accompanist Permissions** | 运行时权限申请 |

### 语言与异步

| 技术 | 用途 |
|:---|:---|
| **Kotlin** | 100% Kotlin，AGP 9.x 内建 Kotlin 支持（不加 `kotlin-android` 插件） |
| **Coroutines + Flow / StateFlow** | 异步与单向状态流 |
| **KSP** | Room、Hilt 注解处理 |

## 🎨 架构与状态

**MVVM + 分层**（Screen → ViewModel → Repository → 数据源），不是 MVI；UseCase 不强制，仅多处复用才抽。

```
┌─────────────────────────────────────────────────┐
│  :app（组合根）                                  │
│  RhythmApplication / MainActivity / 导航装配     │
└──────────────────────┬──────────────────────────┘
                       │ implementation
┌──────────────────────▼──────────────────────────┐
│  feature:*（互不依赖）                           │
│  local / streaming / settings / widget           │
│  Composable Screen + @HiltViewModel + StateFlow  │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│  core:ui / core:media / core:data                │
│  共享组件 · 播放服务与 DSP · Room 与 AppSettings  │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│  core:network / core:designsystem / core:model / │
│  core:common（网络服务 · 资源 · 领域模型 · 工具）│
└─────────────────────────────────────────────────┘
```

- **状态**：私有 `MutableStateFlow` + `asStateFlow()` 暴露，派生用 `stateIn`，UI 统一 `collectAsState()`
- **ViewModel**：只持 Application 级 context，不直接访问 SQL / MediaStore / HTTP
- **设置**：`AppSettings` 是唯一来源，勿另起 SharedPreferences 通道
- **外部 API**：编译期开关 → 全局开关 → 单项开关三层门控；`NetworkClient` 中的 service 懒加载、可空，调用必须判空

## 📁 项目结构

仓库根下按 Gradle 模块划分，每模块唯一包根（`android.uniquePackageNames=true`）：

```
app/                     :app            com.qlzhj.rhythm.app
core/common/             :core:common    com.qlzhj.rhythm.core.common
core/model/              :core:model     com.qlzhj.rhythm.core.model
core/designsystem/       :core:designsystem  ← strings / theme / icons
core/network/            :core:network   ← Retrofit service、NetworkClient
core/data/               :core:data      ← Room、AppSettings、仓储
core/media/              :core:media     ← MediaPlaybackService、RhythmPlayerEngine
core/ui/                 :core:ui        ← 跨 feature 共享组件
feature/local/           :feature:local      ← 本地音乐 + 播放器 UI
feature/streaming/       :feature:streaming  ← provider + Go 模式 UI
feature/settings/        :feature:settings   ← 设置页
feature/widget/          :feature:widget     ← Glance 小组件 + Quick Settings 磁贴
macrobenchmark/          :macrobenchmark     ← 启动 / 帧时序测量
build-logic/convention/  约定插件源码
scripts/                 校验、迁移与发布脚本
docs/                    设计与重构历史
gradle/libs.versions.toml  Version Catalog（版本唯一来源）
```

> 用户可见字符串位于 `core/designsystem/src/main/res/values/strings.xml`（简体中文默认）与 `values-en/strings.xml`（英文）；`:app` 模块**没有** `strings.xml`。仅维护这两种语言，不新增语言目录。

## 📦 主要依赖

版本只在 `gradle/libs.versions.toml` 中维护，模块脚本不写死版本号。

| 分组 | 依赖（catalog 条目） |
|:---|:---|
| AndroidX | `androidx-core-ktx`、`androidx-lifecycle-*`、`androidx-core-splashscreen`、`androidx-appcompat`、`androidx-palette-ktx`、`androidx-documentfile`、`androidx-datastore-preferences` |
| Compose | `androidx-compose-bom`、`androidx-ui*`、`androidx-material3*`、`androidx-navigation-compose`、`androidx-compose-animation*`、`androidx-foundation-layout` |
| 媒体 | `androidx-media3-*`、`org-jellyfin-media3-ffmpeg-decoder`、`androidx-mediarouter` |
| 数据 | `androidx-room-*`、`androidx-paging-*`、`androidx-work-runtime-ktx` |
| 网络 | `com-squareup-retrofit2-retrofit`、`*-converter-gson`、`*-converter-kotlinx-serialization`、`com-squareup-okhttp3-*`、`kotlinx-serialization-json`、`com-google-code-gson-gson` |
| DI | `hilt-android` / `hilt-compiler`、`androidx-hilt-navigation-compose` |
| UI 其它 | `io-coil-kt-coil-compose`、`androidx-glance-appwidget`、`androidx-glance-material3`、`com-google-accompanist-accompanist-permissions`、`reorderables`、`com-google-zxing-core` |
| 测试 | `junit`（JUnit 4）、`com-squareup-okhttp3-mockwebserver3`、`org-json`、`androidx-benchmark-macro-junit4` |

当前主要版本快照（以 catalog 为准）：

| 组件 | 版本 | 组件 | 版本 |
|:---|:---|:---|:---|
| AGP | `9.4.1` | Kotlin | `2.4.21` |
| KSP | `2.3.6` | Gradle Wrapper | `9.7.1` |
| Compose BOM | `2026.09.00` | Material3 | `1.5.0-beta01` |
| Media3 | `1.11.1` | Hilt | `2.60.1` |
| Room | `2.8.5` | Retrofit / OkHttp | `3.0.0` / `5.5.0` |
| kotlinx-serialization-json | `1.12.0` | WorkManager | `2.12.0` |
| Glance | `1.2.0` | Coil | `2.7.0` |
| compileSdk / targetSdk | `37` | minSdk | `26` |

## 🎵 播放引擎

```
┌───────────────────────────────────────────────┐
│  MediaPlaybackService (:core:media)           │
│  MediaLibraryService + exported=true          │
│  MediaSessionService / MediaBrowserService    │
│  ┌─────────────────────────────────────────┐  │
│  │ RhythmPlayerEngine                     │  │
│  │ ExoPlayer + DefaultRenderersFactory    │  │
│  │  • FFmpeg 扩展（PREFER / OFF）         │  │
│  │  • OkHttp + Cache + Resolving DS      │  │
│  │  • float PCM 输出（可选）              │  │
│  └─────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────┐  │
│  │ AudioProcessorChain                    │  │
│  │ SilenceSkipping → EQ / Bass / Spatial  │  │
│  │ → Mono → ReplayGain → Sonic            │  │
│  │（状态与逻辑集中在 EqDspController）    │  │
│  └─────────────────────────────────────────┘  │
│  MediaNotification · AudioFocus · Rhythm Guard│
└───────────────────────────────────────────────┘
            ↕  MediaController / StateFlow
┌───────────────────────────────────────────────┐
│  :feature:local 播放器 UI / :feature:widget    │
└───────────────────────────────────────────────┘
```

10 段均衡器频段为 31/62/125/250/500/1k/2k/4k/8k/16k Hz；AutoEQ 预设数据位于 `app/src/main/assets/autoeq_profiles.json`，共 **6032** 条。

## 📱 小部件架构

`:feature:widget` 提供 4 个 `GlanceAppWidget` 与 1 个 `TileService`：

| 小部件 | 特点 |
|:---|:---|
| `RhythmMusicWidget` | `SizeMode.Exact`，11 个尺寸断点，播放控制与动态形状 |
| `RhythmLyricsWidget` | 多行歌词 + 当前行高亮 |
| `RhythmCookieWidget` | 12 边形封面布局、圆形播放徽章 |
| `RhythmStatsWidget` | 总收听时长与连续天数 |
| `RhythmTileService` | Quick Settings 磁贴，点击切换播放/暂停 |

更新机制：`GlanceWidgetUpdater` 在曲目 / 播放状态 / 歌词 / 封面变更时直接写入 Glance state 并 `updateAll()`；所有 `appwidget-provider` 的 `updatePeriodMillis=0`，不做系统定时轮询；`RhythmWidgetWorker`（`CoroutineWorker`）只用于一次性延迟兜底刷新，**没有周期任务**。

## 🔧 构建系统

| 项目 | 说明 |
|:---|:---|
| 约定插件 | `rhythm.android.application`、`rhythm.android.library`、`rhythm.android.library.compose`、`rhythm.hilt` |
| Kotlin | AGP 9.x 内建；`kotlin-android` 别名在 catalog 中定义但**未使用**（红线：加上即构建失败） |
| 身份 | `namespace = com.qlzhj.rhythm.app`、`applicationId = com.qlzhj.rhythm`，debug 后缀 `.debug` |
| SDK / Java | `compileSdk 37`、`minSdk 26`、`targetSdk 37`、Java / Kotlin `JVM_21`、core library desugaring |
| ABI | 仅 `arm64-v8a`（x86_64 模拟器装不上） |
| release | R8 混淆 + 资源收缩；`keystore.properties` 缺失时回退 debug 签名 |
| 产物 | `Rhythm-{versionName}-{buildType}.apk` |
| 静态检查 | lint / ktlint / detekt 刻意禁用，无格式化门禁 |
| CI | **仓库无 CI 配置**，也不存在 `.github/workflows/`；验证靠本地命令 |

## 🧪 测试与验证

- **栈**：JUnit4 + MockWebServer3 + 真 `org.json`；测试与源码同包，放在对应模块 `src/test/java` 或 `src/test/kotlin`
- **禁用**：MockK、Turbine、coroutines-test、JUnit 5、Robolectric、截图测试框架
- **无 `androidTest` 源集**；`:macrobenchmark` 是独立的 `com.android.test` 模块
- 常用命令（详见 [AGENTS.md](../AGENTS.md)）：

```bash
# 最快编译检查
./gradlew :app:compileDebugKotlin

# 全部 JVM 单测：按模块指定，禁止裸 ./gradlew test
./gradlew :app:testDebugUnitTest :core:common:testDebugUnitTest \
  :core:media:testDebugUnitTest :core:ui:testDebugUnitTest \
  :core:designsystem:testDebugUnitTest :core:data:testDebugUnitTest \
  :feature:local:testDebugUnitTest :feature:streaming:testDebugUnitTest

# 提交前最小集合
./gradlew assembleDebug
python3 scripts/check_plural_specifiers.py
```

`scripts/` 还包含复数占位符校验、包名迁移与引用修复、发布说明生成等脚本；运行发布脚本前先询问维护者。

## 🔐 安全与隐私

- 无分析 / 跟踪，数据保存在设备本地；外部 API 默认关闭且需三层门控
- 仅维护中 / 英两种语言；凭据文件（`local.properties`、`keystore.properties`、`*.jks`）不提交、不输出
- `usesCleartextTraffic="true"` 与用户 CA 信任是自托管服务器（明文 HTTP、私有 PKI）的既定取舍
- manifest 的 exported 组件与 intent-filter 属于对外协议，勿收紧、勿改

## 📊 性能实践

- 懒加载列表 + 稳定 key；昂贵对象 `remember`；派生状态 `derivedStateOf`
- Coil 图片缓存、封面 LRU 缓存、ExoPlayer 缓冲与 `CacheDataSource`
- 主线程不做 IO；Dispatcher 直接引用
- Macrobenchmark 覆盖冷/热启动、滚动帧时序与 Baseline Profile 生成

---

**想参与贡献？** 查看[贡献指南](Contributing.md)与[构建说明](Build-Instructions.md)！
