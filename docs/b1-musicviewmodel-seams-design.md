# B1 专项（第一阶段）：MusicViewModel 窄接口化 —— 解除 settings→local

> 状态：已批准（2026-11-07）。§4.0 实例寿命裁定：A 案（统一到根实例 + 缓解）。
> 上游蓝图：`docs/multi-module-refactor-plan.md`（PR-1..7 已收口）。本专项即蓝图 §二.4 接口倒置清单中 `LocalQueueController` 之后的延续，对应重构计划「B1 MusicViewModel 接口手术」的第一阶段。
> 范围裁定：本阶段只解除 **settings→local**；`:feature:player` 重拆（需要 EQ 状态 store 真抽取）单独立项，见 Non-goals。

## 1. 意图与动机

`:feature:settings → :feature:local` 是现存 4 条 feature→feature 依赖中最重的一条（AGENTS.md §4 登记）。它使模块隔离持续失效：下一个设置页功能会自然滑回直接 import 本地 ViewModel 的老路。根因是 `MusicViewModel`（10808 行，308 个公开成员）成为万能货币：settings 屏直接消费它的 EQ 状态、播放列表操作、缓存清理与备份恢复。

2026-11-07 实测（本设计的依据）：

- 33 个 settings 文件 import `MusicViewModel`，**仅 8 个文件真实消费**，其余 ~25 个是复制粘贴的僵尸 import（同一批文件还普遍 import 了从未使用的对话框组件，如 `PlaylistOperationResultDialog`、`QueueActionDialog` 在 settings 内零使用）。
- 真实消费聚类：EqualizerScreen（43 处调用，EQ/空间音频/AutoEQ 全域 ~30 成员 + 借道 `viewModel.appSettings`）、PlaylistsSettingsScreen、DefaultPlaylistsSettingsScreen、CacheManagementSettingsScreen、BackupRestoreSettingsScreen、MediaScanSettingsScreen、PlaybackSettingsScreen、SettingsScreenWrapper（`currentSong`/`isPlaying` 两只读流）。
- 除 MusicViewModel 外，settings 还真实使用 12 个 local 组件（对话框/BottomSheet），全部在 local 与 settings **双侧使用**。
- settings 屏对 MusicViewModel 的获取方式有两种：SettingsScreenWrapper 由 :app 参数下传（activity 级根实例）；其余 7 屏 `= hiltViewModel()` 自取——**自取拿到的是 NavBackStackEntry 级短命实例**（wrapper 内部路由的屏共用 settings-entry 一个；经 LocalNavigation 顶层路由的屏各自一个），不是根实例。Builder check（2026-11-07）证实了这一点，推翻了本设计初稿「自取=根实例」的判断。因此参数下传 = **实例寿命统一到根实例**（附带消灭今天多个全量 MusicViewModel 并发生命周期的隐性开销），属边缘态行为变化，裁定见 §4.0。

## 2. 目标 / Non-goals / 反目标

**目标**
1. 删除 `feature/settings/build.gradle.kts:25` 的 `implementation(project(":feature:local"))`，settings→local 债务解除，AGENTS.md §4 与蓝图债务清单同步修订。
2. settings 对 MusicViewModel 的一切消费改为对窄接口的消费；MusicViewModel 实现这些接口（延续 `LocalQueueController` 已验证的模式：接口在 core、:app 持有具体 VM 并以接口类型下传）。
3. 顺手清掉 ~25 个文件的僵尸 import（编译零风险，缩小后续审计噪音）。

**Non-goals（另行立项或不动）**
- `:feature:player` 重拆：需要把 EQ 状态从 MusicViewModel 抽成独立 store（设计阶段定为「方案 B」，本阶段不做）。
- 其余 3 条 feature→feature 债务（settings→widget / settings→streaming / local→streaming）。
- AppSettings 拆分 → DataStore（B2 专项，另立）。
- widget 刷新域：已存在 `PlaybackWidgetBridgeInstaller.bridge` 缝隙，无 local→widget 依赖，不动。
- 统计域：`PlaybackStatsRepository` 无 settings 消费者，不动。
- MusicViewModel 行数缩减：本阶段只窄化契约，不迁移逻辑（防回归红线）。

**反目标（出现即失败）**
- 除 §4.0 裁定的实例寿命边缘态（以缓解对冲）外，任何用户可见行为变化（EQ 生效性、播放列表增删导入导出、备份恢复、缓存清理、扫描刷新、单声道/系统音量切换）。
- 为消化参数下传而把接口塞成另一个 God interface。
- 引入新的 DI 装配形态（@Binds/@Provides 缝隙、CompositionLocal、事件总线）。

## 3. 约束（来自 AGENTS.md，全程有效）

- 红线：不 commit/push（除非明确要求）；exported 组件与 manifest 不动；BuildConfig 功能开关不删；import 按文件内 package 声明写（模块 ≠ 包名）。
- 搬移文件保留原 package（PR-3/5 既有策略，零 import churn 由编译驱动修正）。
- ViewModel 只持 Application 级 context；UI 侧不新建 ViewModel；`hiltViewModel()` 约定仅适用于屏自有的 VM。
- 测试栈固定（JUnit4，禁 MockK/Turbine/Robolectric）；本阶段以接线为主，新增 JVM 测试仅限可抽纯函数处。
- 每步收口：`./gradlew :app:compileDebugKotlin`；PR 收口：assembleDebug + 全部 JVM 单测（按模块列名）+ installDebug 冒烟。
- AGENTS.md §4 的「代码放哪」规则是组件下沉目的地的裁决依据（见 §4.3）。

## 4. 设计（已定部分）

### 4.0 实例模型裁定（builder check 提出，用户已裁定：A 案）

自取屏改参数下传后，其实例从「entry 级短命」变为「activity 级根实例」。影响盘点：

- `restoreResult`（BackupRestore 屏）：今天每次进入都是 Idle（新实例）；统一后上次恢复的结果会跨访问残留。**缓解**：屏离开时 `clearRestoreResult()`（实施时选 onDispose 或路由离开钩子）。
- `isLibraryRefreshing`、睡眠定时器状态：统一后为全局共享——语义上更正确（定时器本就是全局的），记为改善。
- `playlists`/`songs` 等数据流：今天 entry 级实例各自从 DB 加载（重复劳动但可用）；统一后单一来源。
- 附带收益：不再为每个设置入口创建全量 MusicViewModel（广播接收器、AudioDeviceManager、StatusBroadcaster 等重复初始化随之消失）。

**选项**：A. 统一到根实例 + 上述缓解（推荐——简单且消除隐性多实例）；B. 保持 entry 级语义——对接口下传不可行（`hiltViewModel<T>()` 无法解析接口，且 wrapper ViewModel 无法注入根实例），只能整段推迟到 EQ store 抽取专项（B1 第一阶段随之搁置）。

### 4.1 方案取舍

- **方案 A（采纳）：窄接口倒置 + 参数下传。** 接口定义在 :core:media / :core:data，MusicViewModel 实现，:app 以接口类型传给 settings 屏。行为零变化、不迁移逻辑；代价是 settings 屏签名从自取变显式参数，且 EQ 逻辑本体仍留在 MusicViewModel（player 重拆受限，已接受并另行立项）。
- **方案 B（否决，后置）：EQ 域状态 store 真抽取。** 把 EQ/DSP 状态抽成独立 controller，MusicViewModel 委托。是 player 重拆的真正前置，但触及 10800 行的内部状态共享，回归风险中高，不应与断依赖混在一个 PR。
- **方案 C（否决）：接口绑定进 Hilt 图。** MusicViewModel 是 ViewModel，无法作为 SingletonComponent 绑定目标；为此改造装配形态违背「不加新 DI 形态」反目标。

### 4.2 窄接口清单（6 + 1 类型外迁）

接口成员必须与 MusicViewModel 现有公开成员**签名逐字一致**（`StateFlow` 类型不变性），实现零方法体改动，编译器逐个验收。

| 接口 | 定义模块 | 成员（来自实测调用面） | 消费者 |
|---|---|---|---|
| `PlaybackStateSource` | :core:media | `currentSong`、`isPlaying` | SettingsScreenWrapper |
| `PlaybackSettingsController` | :core:media | `setMonoAudioEnabled`、`setUseSystemVolumeMode` | PlaybackSettingsScreen |
| `PlaylistAdminController` | :core:data | `playlists`、`createPlaylist`、`deletePlaylist`、`importPlaylist`、`exportAllPlaylists`、`ensurePlaylistsSaved`、`setDefaultPlaylistsEnabled`、`setShowLikedInPlaylists`、`setSmartPlaylist{MostPlayed,RecentlyPlayed,RecentlyAdded,OnRepeat,ForgottenFavorites}` | PlaylistsSettingsScreen、DefaultPlaylistsSettingsScreen、BackupRestoreSettingsScreen（ensurePlaylistsSaved） |
| `LibraryAdminController` | :core:data | `refreshLibrary`、`songs`、`filteredSongs`、`isLibraryRefreshing` | MediaScanSettingsScreen、BackupRestoreSettingsScreen（isLibraryRefreshing） |
| `LibraryCacheManager` | :core:data | `clearInMemoryCaches`、`clearSongCacheData`、`getRoomSongCount`、`clearAllLyricsCache`（builder check 补遗：VM 侧方法 MusicViewModel.kt:8349） | CacheManagementSettingsScreen |
| `BackupRestoreController` | :core:data | `restoreFromBackup`、`restoreResult`、`clearRestoreResult` | BackupRestoreSettingsScreen |
| `SleepTimerController` | :core:media | `sleepTimerActive`、`sleepTimerRemainingSeconds`、`sleepTimerTotalSeconds`、`sleepTimerAction`、`serviceConnected`、`startSleepTimer`、`stopSleepTimer`（builder check 补遗） | SleepTimerBottomSheetNew（SettingsScreenWrapper.kt:557；local 侧 PlayerScreen 系与 :app 也在用） |
| （类型外迁）`RestoreResult` | :core:data | `sealed class RestoreResult { Idle / Queued / Success(wasQueued) / Failure(errorMessage) }`（现嵌套于 MusicViewModel.kt:131） | BackupRestoreSettingsScreen 等引用点 |

实现归属：全部接口由 `MusicViewModel` 实现并下传（builder check 修正：原「LibraryCacheManager 由具体 MusicRepository 实现」的特例废除——`clearAllLyricsCache` 在 VM 上，且 VM 三个缓存方法本就是 `getMusicRepository().xxx()` 透传，VM 实现即一行转调，等价现状，少一条特殊下传路径）。

放置依据：播放域接口入 :core:media（与既有 `LocalQueueController`、DSP/EQ 同域）；曲库/播放列表/备份域接口入 :core:data（`MusicRepository` 接口与 `BackupRestoreManager` 均在彼处，且 :core:data 已依赖 :core:model，`Song`/`Playlist` 类型可用）。

### 4.3 组件下沉（builder check 修正后 15 个，全部 → :core:ui）

实测双侧使用（local + settings），按 AGENTS.md「跨 feature 复用的组件进 :core:ui」下沉。**分两批，按接口可用性定序**：

- **PR-8 批（签名干净，11 个）**：dialogs `CreatePlaylistDialog`、`PlaylistImportDialog`、`BulkPlaylistExportDialog`、`PlaylistOperationProgressDialog`、`AppRestartDialog`；bottomsheets/settings `LibraryTabOrderBottomSheet`、`HomeSectionOrderBottomSheet`、`LanguageSwitcherBottomSheet`、`SaveCustomPresetDialog`、`PlayerChipOrderBottomSheet`、`ExpressiveBottomButtonsOrderBottomSheet`（后两个为 builder check 补遗，PlayerCustomizationSettingsScreen 使用，纯回调）。
- **PR-9 批（持 VM 参数，须等对应接口就绪后随 EQ 改造一起搬，4 个）**：`EqualizerPresetOrderBottomSheet`（需 6 个 EQ 排序/删除成员，见附录 A）、`DeviceConfigurationBottomSheet`（需音频设备域，见 `AudioDeviceController`）、`AutoEQPresetPickerBottomSheet`、`SleepTimerBottomSheetNew`（随 `SleepTimerController` 在 PR-8 定义接口后仍留 PR-9 搬移，因它同时被 player 双屏与 :app 使用，改参数波及面大，单独批）。

搬移前置仍是逐个签名核查（已验证 `CreatePlaylistDialog` 纯回调）；搬移保留原 package（模块 ≠ 包名，既有策略），proguard 按需同步。

### 4.4 分两个 PR

**PR-8「机械面」**（除 EQ/设备域外全部）：
1. 删 ~25 文件僵尸 import（MusicViewModel + 未使用组件 import；`PlaylistOperationResultDialog`/`QueueActionDialog` 等零使用 import 一并清）。
2. 11 个签名干净组件下沉 :core:ui（§4.3 PR-8 批），import 修正。
3. 定义 7 接口（含 `SleepTimerController`）+ `RestoreResult`、`SleepAction` 类型外迁；MusicViewModel 加 implements（`LibraryCacheManager` 三方法为一行转调）。
4. 7 个消费文件改造：自取改参数（新签名以接口类型收参）；调用点 = :app `LocalNavigation.kt:2168-2261` 一族 + `SettingsScreenWrapper.kt:320-368` 内部路由；SettingsScreenWrapper 自身签名增加 7 个接口参数（:app 侧 RhythmNavigation.kt:243、LocalNavigation.kt:2104 传入，含 sleep timer）。`SleepTimerBottomSheetNew` 本 PR 只改参数为 `SleepTimerController`（文件搬移留 PR-9）。EqualizerScreen 本 PR **不动**（仍收 MusicViewModel），故依赖行保留。
5. 验收：compileDebugKotlin + 全模块单测 + assembleDebug + installDebug 冒烟（设置页全量走查：播放列表 CRUD/导入导出、默认与智能播放列表开关、缓存清理、备份恢复、扫描、单声道/系统音量、mini 播放条状态、睡眠定时器；**补「恢复完成后再进入屏」场景**验证缓解）。

**PR-9「EQ 接口 + 断依赖」**：
1. `AudioEffectsController`（:core:media，36 成员：附录 A 30 项 + builder check 补遗 6 项）与 `AudioDeviceController`（:core:media：`saveUserAudioDevice`、`deleteUserAudioDevice`、`setActiveAudioDevice`、`dismissAutoEQSuggestion`、`findMatchingUserDevice`、`audioDeviceManager`）+ MusicViewModel implements。
2. EqualizerScreen：`viewModel: MusicViewModel` → `audioEffects: AudioEffectsController`；12 处 `viewModel.appSettings.*` 改用文件内既有 `AppSettings.getInstance(context)` 先例（EqualizerScreen.kt:338）；LocalNavigation.kt:2223/2387 传接口。
3. 4 个 PR-9 批组件改参数后下沉 :core:ui；`getLocalizedPresetName`（EqualizerScreen.kt:869 消费 :feature:local `EqualizerUtils` 的顶层函数）下沉 :core:designsystem（`formatDuration`/TimeFormat 先例；:core:media 无 designsystem 依赖放不下含 `stringResource` 的函数，`EqualizerUtils` 本体留守，实施时核对其余消费者）。
4. `SettingsScreenWrapper.kt:202,221` 的 `Screen.RhythmStats.route`/`Screen.Equalizer.route` 引用改为 :core:ui `SettingsRoutes` 新增常量（保持字符串路由约定）。
5. **删除 `feature/settings/build.gradle.kts:25` 依赖行**。**门禁 = 删行后 compileDebugKotlin 绿**（模块可见性由 Gradle 依赖图唯一裁决，编译即完备门禁）；辅以 settings 内 `import ...features.local.` 清零做卫生检查（settings 模块内 EqualizerScreen.kt 自身 package 仍为 `features.local.presentation.screens`，属既有「模块 ≠ 包名」策略，不构成依赖）。
6. AGENTS.md §4 债务登记更新（4 条 → 3 条）+ 蓝图偏差回写。
7. 验收：同 PR-8 + EQ 专项冒烟（预设/自定义频段/AutoEQ/空间音频/低音增强/系统均衡器跳转/播放中改 EQ 实时生效/设备配置与 AutoEQ 建议）。

## 5. 风险与已核对事项

| 风险 | 状态/缓解 |
|---|---|
| 接口签名与 VM 成员不一致 | 编译驱动逐个验收；接口成员照抄现签名，不改任何方法体 |
| 实例寿命统一（entry 短命 → 根实例） | §4.0 裁定项；`restoreResult` 残留以屏离开时清除缓解；冒烟含「恢复完成后再进入屏」用例 |
| 下沉组件暗持 VM 或播放器域依赖 | builder check 已实抓 3 个（EqualizerPresetOrder/DeviceConfiguration/SleepTimer）并按 §4.3 分批；PR-8 批 11 个仍逐一签名核查后才搬 |
| proguard keep 指向外迁类型 | builder check 已核实现无 `MusicViewModel$RestoreResult` keep；外迁 `SleepAction` 时顺手确认 |
| gradle 依赖删除后隐藏引用漏网 | 删行后 compileDebugKotlin 即完备门禁（模块可见性只由 Gradle 依赖图裁决）；`features.local.` import 清零作卫生检查 |
| settings 内对 local `Screen` 路由常量的引用 | PR-9 第 4 步：提升为 `SettingsRoutes` 常量（:core:ui） |
| `EqualizerUtils` 等零散工具引用漏网 | PR-9 第 3 步处理 `getLocalizedPresetName`；实施时以编译驱动捕获其余零散符号 |
| 智能播放列表 setter 不是纯 AppSettings 写 | 已核实需触发 playlistDao 重算（`toggleSingleSmartPlaylist`），归入 `PlaylistAdminController`，不归 AppSettings |

## 6. 留给实施者的裁量

- 接口命名（表内为建议名，语义不变即可）；`RestoreResult` 落位包名（建议随 `BackupRestoreManager`）。
- 僵尸 import 清理的文件清单以 grep 实测为准（本文档数字是 2026-11-07 快照）。
- settings 屏新参数的默认值处理：建议全部显式必填（杜绝静默退化为自取）。
- PR 描述与提交切分粒度。

## 附录 A：AudioEffectsController 成员清单（EqualizerScreen 实测 28 项 + builder check 补遗 6 项 = 34 项）

状态/流（16）：`equalizerEnabled`、`equalizerPreset`、`equalizerPresetOrder`、`equalizerBandLevels`、`customEqualizerPresets`、`hiddenEqualizerPresets`、`autoEQProfiles`、`customAutoEQProfiles`、`pinnedAutoEQProfiles`、`virtualizerEnabled`、`virtualizerStrength`、`spatializationStatus`、`isSpatializationAvailable`、`bassBoostEnabled`、`bassBoostStrength`、`isBassBoostAvailable`

动作（12）：`setEqualizerEnabled`、`setEqualizerBandLevel`、`applyEqualizerPreset`、`saveCustomEqualizerPreset`、`applyAutoEQProfile`、`loadAutoEQProfiles`、`pinAutoEQProfile`、`setVirtualizer`、`setBassBoost`、`openSystemEqualizer`、`updateSpatializationStatus`、`updateBassBoostAvailability`

补遗（6，来自 EqualizerPresetOrderBottomSheet 消费面）：`setEqualizerPresetOrder`、`resetEqualizerPresetOrder`、`deleteAutoEQProfile`、`deleteCustomEqualizerPreset`、`setHiddenEqualizerPresets`、`saveCustomAutoEQProfiles`

（另有 12 处 `viewModel.appSettings.*` 直改 AppSettings，不走接口；设备域成员单独归 `AudioDeviceController`，见 §4.4 PR-9。）
