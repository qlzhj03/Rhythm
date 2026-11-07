# D3 专项：解除最后一条 feature→feature 债务 local→streaming

> 状态：**草稿（待批准）**（2026-11-08）。上游：`docs/multi-module-refactor-plan.md`（PR-1..7）、`docs/b1-musicviewmodel-seams-design.md`（settings→local）、`docs/d-settings-decoupling-design.md`（settings→widget / settings→streaming 已解除）。
> 本专项完成后，feature 模块完全平行，AGENTS.md §4 债务清零。

## 1. 意图与动机

`local→streaming`（`feature/local/build.gradle.kts:28-29`）是最后一条也是最初的一条跨 feature 债务：HomeScreen/UniversalSearch 在多模块拆分前就与流媒体共生。B1 已验证「窄接口 + :app 下传」模式，D2 已验证「出栈导航」模式；本专项把同一纪律应用到数据层与嵌入 UI。

反向边**无需处理**：`StreamingMusicViewModel.playNext/addSongToQueue` 已以 `LocalQueueController`（:core:media）参数注入，零 local import（PR-5 完成的缝隙）。

## 2. 已核验事实（2026-11-08 实测）

### 2.1 消费清单（5 文件）

| 文件 | 消费 | 用途 |
| :-- | :-- | :-- |
| `MusicViewModel.kt` | 构造注入 `StreamingMusicRepository`（:127-129） | 仅 `reportPlaybackStart`(:5553)/`reportPlaybackStop`(:5640) 两处 scrobble 上报，`streaming://` URI 守卫 |
| `LocalMusicRepository.kt` | FQ 构造参数同接口（:150） | 仅 `getLyrics`(:4446-4459) 作流媒体歌词首选源 |
| `UniversalSearchScreen.kt` | `StreamingMusicViewModel`（:82,:112，`hiltViewModel()` 默认值，:app :2047 显式覆盖传根实例） | 直驱 `search(query)`(:241-243)，收集 `searchResults/isLoading/likedSongs/browseCategories`(:215-221)；渲染结果全是 `:core:model` 类型 + 回调，**无流媒体 UI 组件** |
| `PlaybackBottomSheet.kt` | 仅 2 个顶层符号 `streamingQualityOptions`/`streamingQualityLabel`（:114-115，定义在 StreamingSettingsScreen.kt:670/:708） | 质量卡片(:1042-1060)与质量选择 sheet(:1572-1583)，数据全走 AppSettings |
| `HomeScreen.kt` | 最重：`StreamingMusicViewModel` 公开参数(:283，:app :1979 传根实例)、私有 `StreamingHomeBody`(:694-~1260)/`StreamingHomeWelcomeContent`(:1267-1420)、FQ 引用 `StreamingServiceStateCard`(:1038)/`StreamingHomeSectionOrderBottomSheet`(:555)/`StreamingSyncProgress`(:808) | Go 模式 home 整体嵌入 |

### 2.2 关键机制核验

- **Hilt 绑定已存在**：`StreamingMusicModule.kt`（:feature:streaming）`@Module @InstallIn(SingletonComponent) @Binds` 提供接口绑定。库模块 ViewModel 的依赖在 :app 组合根聚合解析——**接口搬家到 :core 后，绑定不依赖消费方模块可见性**，MusicViewModel/LocalMusicRepository 的 `@Inject` 构造零改动。
- **接口已是纯 core 类型**：`StreamingMusicRepository` 继承 `MusicRepository`（`core/data/.../core/domain/repository/MusicRepository.kt`），全部引用 `:core:model`（StreamingSong/Album/Artist/Playlist、BrowseCategory、LyricsData、SourceType）+ kotlinx Flow。搬家零阻力。
- **`StreamingHomeBody` 对 MusicViewModel 的依赖仅 3 个播放状态成员**：`progress`/`duration`/`isPlaying`（:758-760 实测，含 WelcomeContent 区域待实施时全量复核）——恰是既有缝隙 `PlaybackStateSource`（:core:media，B1 PR-8，MusicViewModel 已实现）的同族成员，扩展即可，无需新缝隙形态。
- **VM 成员全为 core 类型**：`searchResults: StateFlow<StreamingSearchResults>`、`likedSongs/browseSongs: List<StreamingSong>`、`downloadedSongs/downloadingSongIds/allSongs/recommendations` 等均为 `:core:model` 或基元类型。
- **`StreamingSearchResults`/`StreamingSyncProgress` 是 StreamingMusicViewModel.kt 内的顶层类**（非 :core:model）——搜索缝隙引用前者，需先迁 `:core:model`；后者随 body 迁移，原地不动。
- **`StreamingServiceStateCard`、`StreamingHomeSectionOrderBottomSheet` 已在 :feature:streaming**；body/welcome 是 HomeScreen 私有，随迁即可。
- 全仓 `StreamingMusicRepository` 消费者 8 文件：MusicViewModel、LocalMusicRepository、RhythmApplication(:146)、StreamingPlaybackResolver(:core:media，仅 KDoc 提及，实施时确认)、streaming 模块 4 文件（Module/VM/Impl/测试）。

## 3. 目标 / Non-goals / 反目标

**目标**
1. 删除 `feature/local/build.gradle.kts:28-29` 的 `implementation(project(":feature:streaming"))` 及债务注释。
2. AGENTS.md §4 债务清零；`docs/multi-module-refactor-plan.md` 蓝图收口。

**Non-goals**
- 反向缝隙（LocalQueueController 参数模式）不动。
- `isStreamingMode` 类 Boolean 参数与 `"streaming_album/…"` 字符串路由（PlayerScreen/MaterialPlayerScreen/LibraryScreen 等**软耦合**）——不构成编译依赖，留待功能性需求出现时顺手处理。
- MusicViewModel/StreamingMusicViewModel 行为与行数：只改契约与归属，不迁移逻辑（B1 防回归红线）。

**反目标（出现即失败）**
- 任何用户可见行为变化（Go 模式 home、下载徽章/开关、拉刷新、统一搜索、质量选择、歌词获取、scrobble）。
- 新增 DI 装配形态（@Binds/@Provides 新缝隙、CompositionLocal、事件总线）；复用既有绑定与参数下传。
- 把接口塞成 God interface：`StreamingLibraryController`/`StreamingSearchController` 成员**严格限于实测消费集**。

## 4. 设计

### 4.1 PR-D3-1：数据层接口归位（MusicViewModel + LocalMusicRepository）

- `StreamingMusicRepository.kt` 从 `features/streaming/domain/repository/` 迁至 `core/data/src/main/kotlin/chromahub/rhythm/app/core/domain/repository/`，package 同步改为 `chromahub.rhythm.app.core.domain.repository`（C 阶段「import 即可定位」规则）。
- 8 个消费文件 import/FQ 修正（编译驱动）；`StreamingMusicModule` 的 @Binds 不动。
- MusicViewModel/LocalMusicRepository 调用点零改动（守卫逻辑、scrobble、歌词链原样）。

### 4.2 PR-D3-2：搜索缝隙 + 质量助手下沉（UniversalSearchScreen + PlaybackBottomSheet）

- `StreamingSearchResults` 顶层类迁至 `:core:model`（streaming 模块 import 同步）。
- `:core:data` 新增窄接口 `StreamingSearchController`：`fun search(query: String)` + `val searchResults/isLoading/likedSongs/browseCategories`（严格=UniversalSearchScreen 消费集）。`StreamingMusicViewModel` implements（成员已存在，签名逐一比对）。
- `UniversalSearchScreen` 参数 `streamingViewModel: StreamingMusicViewModel = hiltViewModel()` 改为 `streamingSearch: StreamingSearchController`（必填，:app :2047 本就显式传根实例，改传接口类型）；删除 :82 import。
- `streamingQualityOptions`/`streamingQualityLabel`/`StreamingQualityOption`/`normalizeStreamingQuality` 迁至 `:core:ui`（新文件，package 随目录；引用 designsystem R.string，:core:ui → designsystem 既有依赖）；StreamingSettingsScreen 与 PlaybackBottomSheet 改 import。

### 4.3 PR-D3-3：HomeScreen 解嵌 + 依赖剪断（最重）

1. **窄缝**：`:core:data` 新增 `StreamingLibraryController`：`downloadedSongs/downloadingSongIds/allSongs/recommendations` 四只读流 + `downloadSong(song)/downloadSongById(id)/removeDownload(id)/refreshHome()`（严格=HomeScreen :420-443,:644 实测消费集）。`StreamingMusicViewModel` implements。
2. **body 迁移**：`StreamingHomeBody` + `StreamingHomeWelcomeContent` 迁至 `:feature:streaming`（与 StreamingServiceStateCard 同域）；`musicViewModel: MusicViewModel` 参数改 `playbackState: PlaybackStateSource`（:core:media 既有缝隙**扩展** `progress`/`duration` 两成员——MusicViewModel 已实现，仅加接口声明）；:app 在 slot 闭包内传根实例。
3. **slot 组合**：HomeScreen 以 `streamingHomeContent: (@Composable () -> Unit)? = null` 替代对 body/welcome/VM 的直接引用；`streamingViewModel` 参数改为 `streamingLibrary: StreamingLibraryController? = null`（下载徽章/开关、拉刷新走缝）；HomeScreen 的 `streaming*` 数据参数逐一归属清点——body 专用者随迁入 slot 闭包，主文件仍用者（如 `streamingServiceName/Connected` 头部 :568-575）保留（均 core 类型）。
4. **:app 装配**：LocalNavigation :1979 调用点改为传 seam + slot 闭包（闭包内组装已回调集 :1989-2015 与映射数据 :942-989——这些本就是 :app 收集好的）。
5. **剪断**：删 `feature/local/build.gradle.kts:28-29`（独立 commit，单独可编译）；AGENTS.md 债务清零、蓝图标记完成。

### 4.4 方案取舍（HomeScreen 的三条路）

- **N. body 留守 + 类型/缝隙化**：需把 ~15 个 VM 流（allSongs/recommendations/newReleases/savedAlbums/followedArtists/savedPlaylists/downloaded*/syncProgress/isOnline/serviceSessions/isAuthenticated…）塞进接口——正是 B1 反目标「为消化参数下传把接口塞成 God interface」。否决。
- **T. 状态聚合上提 :app**（body 变纯 (state, callbacks) 函数留守 local）：无迁移 churn，但聚合体落在已 4866 行的 LocalNavigation，与蓝图「:app 保持薄组合根」相悖，加重 P4 导航债务。**记为 fallback**。
- **S. body 迁入 :feature:streaming + :app slot（采纳）**：与 StreamingServiceStateCard 等同域归位，符合蓝图模块职责与 AGENTS.md「代码放哪」；跨缝需求仅 3 个播放状态成员（既有缝隙扩展）+ 8 个库操作成员（新窄缝）。迁移的真实风险是 body 内渲染组件的模块归属——见 §4.5 前置核验。

### 4.5 实施前置核验（PR-D3-3 第一任务，裁定点）

清点 `StreamingHomeBody`/`StreamingHomeWelcomeContent` 全区域（:694-1420）引用的非 core 组件归属：
- 引用 `:core:ui`/`:core:designsystem`/`:feature:streaming` 组件 → 直接随迁；
- 引用 `:feature:local` 私有组件 → 逐个下沉 `:core:ui`（c82ae06 EQ/sleep-timer sheet 下沉先例）或并入迁移文件；
- **若下沉代价超出阈值（>3 个大组件需下沉），按 §4.4 回退 Option T**，并在设计稿补记裁定（沿用 B1 §4.0 builder check 推翻初稿的先例）。
- 同任务复核 WelcomeContent 区域 musicViewModel 用法是否确为 3 成员（防 grep 区域漏尾）。

## 5. 行为裁定

- **零行为变化**。VM 实例寿命不变：:app 今日已对 HomeScreen(:1979)/UniversalSearch(:2049) 显式传根 `streamingMusicViewModel`，slot/seam 化不改变实例与入口归属——**无 B1 §4.0 类实例寿命问题**。
- `PlaybackStateSource` 扩展为加性变更（现有两消费点不受影响；MusicViewModel 单实现方）。
- 质量助手、搜索、scrobble、歌词链均为同语义搬家。

## 6. 风险与复审焦点

| 风险 | 缓解 |
| :-- | :-- |
| body 内组件归属不明导致迁移扩散 | §4.5 前置清点 + Option T fallback 裁定规则 |
| 接口成员与 VM 签名漂移 | 实施时逐一比对（B1「members signature-identical」纪律）；编译门禁 |
| 8 文件 import 修正遗漏 | 迁移保留旧 package 会造成静默双包——**不保留**，直接改 package，编译驱动修正（C 阶段验证过的做法） |
| slot 闭包内丢回调/参数 | :app 调用点 diff 逐参数核对；installDebug 真机走查 Go 模式 home 全交互 |
| 循环依赖回潮 | :core:data 不新增任何模块依赖；接口只引用 :core:model 类型 |

## 7. PR 切片与门禁

| PR | 内容 | 门禁 |
| :-- | :-- | :-- |
| PR-D3-1 | 接口归位 :core:data（8 文件） | 每步 compileDebugKotlin；收口 assembleDebug + 全量 JVM 单测 |
| PR-D3-2 | StreamingSearchResults 迁移 + 搜索缝隙 + 质量助手下沉 | 同上 |
| PR-D3-3 | StreamingLibraryController + body 迁移 + slot + **build!: 剪断依赖** + 债务清零 | 同上 + installDebug 真机冒烟（Go 模式 home / 下载 / 搜索 / 质量选择） |

分支：`refactor/d3-pr1-repo-relocate` → `refactor/d3-pr2-search-seam` → `refactor/d3-pr3-home-unhook`（stacked）。文档以 `docs:` commit 随 PR-D3-1 入库。
