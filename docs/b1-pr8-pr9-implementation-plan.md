# B1 PR-8/PR-9 实施计划：MusicViewModel 窄接口化，解除 settings→local

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** settings 模块对 MusicViewModel 的一切消费改为窄接口消费，最终删除 `feature/settings/build.gradle.kts` 的 `:feature:local` 依赖行（PR-9 收口）。

**Architecture:** 延续 `LocalQueueController` 已验证的缝隙模式——接口定义在 `:core:media` / `:core:data`，`MusicViewModel` 实现，`:app` 组合根持有 activity 级根实例并以接口类型下传。7 个设置屏从「`= hiltViewModel()` 自取 entry 级短命实例」改为「参数接收根实例的接口视图」（实例寿命统一，设计 §4.0 A 案）。15 个共享组件按签名干净度分两批下沉 `:core:ui`。

**Tech Stack:** Kotlin、Compose、Hilt（既有装配，不加新 DI 形态）、StateFlow。

**Spec:** `docs/b1-musicviewmodel-seams-design.md`（已批准；本计划按抽取实测修正其两处：`AudioEffectsController` 实为 34 成员而非 36；`AutoEQPresetPickerBottomSheet` 签名干净，归 PR-8 批。另新增 `PlaylistImportExportUtils.kt` → `:core:data` 的前置搬移，见 Task 3）。

## Global Constraints（每个任务隐含遵守）

- AGENTS.md 红线全程有效：不 commit/push/tag 除非用户明确许可（本计划的 commit 步骤执行前须确认）；manifest/exported 组件不动；版本只进 catalog；不加 `kotlin-android` 插件；BuildConfig 开关不删。
- **搬移文件保留原 package**（模块 ≠ 包名，PR-3/5 既有策略）；import 按文件内 package 声明写。
- **接口成员签名逐字照抄 MusicViewModel 现有声明**（含参数名与可空性），不改任何方法体。唯一例外：`LibraryCacheManager` 的 3 个缓存方法是 VM 新增的一行转调（Task 5）。
- **接口方法不能声明默认参数**：~~VM 现有默认参数在接口类型调用点不可用~~ **执行修正（Task 5）**：Kotlin 允许接口方法声明默认值、禁止 override 声明默认值——5 个带默认值的成员已把默认值移入接口声明，所有调用点（VM 类型或接口类型）语义不变，无需显式传全参。
- `hiltViewModel()` 只允许用于屏自有的 VM；改造后 8 个消费屏/组件不再自取 MusicViewModel。
- 快速编译检查：`./gradlew :app:compileDebugKotlin`。全量单测按 AGENTS.md §1 命令（按模块列名，禁裸 `test`）。
- 工作分支：`refactor/b1-settings-local-seams`（起分支须经用户许可）。
- 测试规则（AGENTS.md §6）：本计划为接线改造，不新增 JVM 测试；验证 = 编译 + 既有单测 + 设备冒烟。

## Review Focus（规格隐含、任务测试覆盖不到的五个失效面，各自由冒烟步骤钉住）

1. **播放中改 EQ 实时生效**——预设/频段/低音增强/空间音频在播放态修改应即时作用于引擎（PR-9 最大回归面）。钉在 Task 17/21 冒烟。
2. **备份恢复全流程 + 陈旧结果残留**——恢复发起→Queued→离开屏→完成→重进屏：缓解后不得显示上次的完成面板。钉在 Task 10/21 冒烟。
3. **播放列表导入/导出格式行为**——CSV/M3U/JSON 三格式与 `AppRestartDialog` 重启流程不变。钉在 Task 4/21 冒烟。
4. **睡眠定时器跨入口一致**——settings（wrapper）与 player 双入口设置/取消同一全局定时器。钉在 Task 12/21 冒烟。
5. **wrapper 的 portrait/landscape 双分支**——每个屏在 wrapper 内有两处调用点（~320-371 横屏、~475-526 竖屏），漏一处即另一方向崩。每个转换任务的 grep 验证步骤钉住。

---

## File Structure 总览

**新建（:core:media，`core/media/src/main/kotlin/chromahub/rhythm/app/infrastructure/service/`，包 `chromahub.rhythm.app.infrastructure.service`）**：`PlaybackStateSource.kt`、`PlaybackSettingsController.kt`、`SleepTimerController.kt`（含顶层 `enum SleepAction`）、`AudioEffectsController.kt`（PR-9）、`AudioDeviceController.kt`（PR-9）

**新建（:core:data，`core/data/src/main/kotlin/chromahub/rhythm/app/core/domain/repository/`，包 `chromahub.rhythm.app.core.domain.repository`）**：`PlaylistAdminController.kt`、`LibraryAdminController.kt`、`LibraryCacheManager.kt`、`BackupRestoreController.kt`
**新建（:core:data，`core/data/src/main/kotlin/chromahub/rhythm/app/core/domain/backup/`）**：`RestoreResult.kt`（包 `chromahub.rhythm.app.core.domain.backup`）

**搬移（保 package）**：Task 4 九个组件文件 → `:core:ui`；Task 3 `PlaylistImportExportUtils.kt` → `:core:data`；Task 16 三个组件文件 → `:core:ui`；Task 17 `getLocalizedPresetName` → `:core:designsystem`

**修改**：`MusicViewModel.kt`（implements 列表 + 3 个转调方法 + 嵌套类型删除）、7 个设置屏 + `EqualizerScreen.kt` + `SettingsScreenWrapper.kt`、`LocalNavigation.kt`、`RhythmNavigation.kt`、`SleepTimerBottomSheetNew.kt` 及其 3 处调用点、`feature/settings/build.gradle.kts`（PR-9 删依赖行）、`AGENTS.md` + `docs/multi-module-refactor-plan.md`（PR-9 收口）

---

# PR-8：机械面（依赖行保留）

### Task 1: 僵尸 import 清理

**Files:**
- Modify: `feature/settings/src/main` 下 ~25 个仅 import 引用 `MusicViewModel` / local 组件的文件

**Interfaces:**
- Consumes: 无
- Produces: settings 模块内除 8 个真实消费文件 + `EqualizerScreen.kt` 外，零 `features.local.` import

- [ ] **Step 1: 枚举僵尸文件**

```bash
grep -rlE "import chromahub\.rhythm\.app\.features\.local\." feature/settings/src/main --include=*.kt \
  | grep -vE "EqualizerScreen|PlaylistsSettingsScreen|DefaultPlaylistsSettingsScreen|CacheManagementSettingsScreen|BackupRestoreSettingsScreen|MediaScanSettingsScreen|PlaybackSettingsScreen|SettingsScreenWrapper"
```
对每个命中文件逐个确认：文件体内（排除 import 行）无 `MusicViewModel` 与下述组件的符号引用后，删除对应 import 行。实测已确认 `SettingsScreen.kt`、`LyricsSettingsScreen.kt`、`PerformanceSettingsScreen.kt` 等仅剩 import；`PlaylistOperationResultDialog`、`QueueActionDialog` 在 settings 内零真实使用。

- [ ] **Step 2: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: PASS（删错在用 import 会在此暴露）

- [ ] **Step 3: Commit**（许可后）

```bash
git add -A feature/settings && git commit -m "refactor: drop stale features.local imports in settings"
```

### Task 2: RestoreResult 外迁 :core:data

**Files:**
- Create: `core/data/src/main/kotlin/chromahub/rhythm/app/core/domain/backup/RestoreResult.kt`
- Modify: `MusicViewModel.kt:131-136`（删除嵌套类）、`BackupRestoreSettingsScreen.kt:238,243,257`、MusicViewModel 内全部 `RestoreResult` 引用

**Interfaces:**
- Produces: 顶层 `sealed class RestoreResult { object Idle; object Queued; data class Success(val wasQueued: Boolean); data class Failure(val errorMessage: String) }`，包 `chromahub.rhythm.app.core.domain.backup`（Task 9 的 `BackupRestoreController` 依赖它）

- [ ] **Step 1: 新建 `RestoreResult.kt`**——逐字搬移 MusicViewModel.kt:131-136 的四个子类为顶层声明，补 package/import（`AppSettings` 无关，纯类型）。
- [ ] **Step 2: MusicViewModel 删除嵌套类**，内部引用（`_restoreResult`、`restoreResult`、`QueuedRestore` 等）改指顶层类型（同模块不同包需 import；编译驱动）。
- [ ] **Step 3: `BackupRestoreSettingsScreen.kt` 三处 `MusicViewModel.RestoreResult` → `RestoreResult`**（import `chromahub.rhythm.app.core.domain.backup.RestoreResult`）。
- [ ] **Step 4: 全仓 grep `MusicViewModel.RestoreResult` 应为空**（:app 的 OnboardingScreen 若有引用一并机械替换）。
- [ ] **Step 5: 编译验证** `./gradlew :app:compileDebugKotlin` → PASS。
- [ ] **Step 6: Commit**（许可后）`refactor: extract RestoreResult to :core:data`

### Task 3: PlaylistImportExportUtils.kt 下沉 :core:data

**Files:**
- Move: `feature/local/src/main/kotlin/chromahub/rhythm/app/util/PlaylistImportExportUtils.kt` → `core/data/src/main/kotlin/chromahub/rhythm/app/util/PlaylistImportExportUtils.kt`（package `chromahub.rhythm.app.util` 保留）
- Modify: `core/data/build.gradle.kts`（+documentfile）

**Interfaces:**
- Produces: `PlaylistImportExportUtils`（含嵌套 `enum PlaylistExportFormat(val extension: String, val mimeType: String, val displayName: String)`，行 110）对 :core:data / :core:ui / :feature:local 三方可见——Task 4 的接口与对话框依赖此类型

- [ ] **Step 1: `git mv` 该文件到 :core:data 同 package 路径**（import 零改动；其依赖 = R（designsystem，:core:data 已依赖）、core:model、androidx 框架、gson（:core:data 已有））。
- [ ] **Step 2: `core/data/build.gradle.kts` 依赖区追加 `implementation(libs.androidx.documentfile)`**（catalog 已有 `androidx-documentfile = "1.0.1"`，版本不新写）。
- [ ] **Step 3: 编译验证** `./gradlew :app:compileDebugKotlin` → PASS（:feature:local 5 个消费文件 import 不变）。
- [ ] **Step 4: Commit**（许可后）`refactor: relocate PlaylistImportExportUtils to :core:data`

### Task 4: PR-8 批组件下沉 :core:ui（9 文件 12 组件）

**Files:**
- Move（`git mv` 到 `core/ui/src/main/kotlin/` 下同 package 路径，import 零改动）：
  1. `feature/local/.../features/local/presentation/components/dialogs/CreatePlaylistDialog.kt`（含 QueueActionDialog、QueueListActionDialog）
  2. `feature/local/.../components/dialogs/PlaylistImportExportDialogs.kt`（含 AppRestartDialog、PlaylistExportDialog、BulkPlaylistExportDialog、PlaylistImportDialog、PlaylistOperationProgressDialog、PlaylistOperationResultDialog）
  3. `feature/local/.../components/dialogs/SaveCustomPresetDialog.kt`
  4. `feature/local/.../components/settings/LibraryTabOrderBottomSheet.kt`
  5. `feature/local/.../components/settings/HomeSectionOrderBottomSheet.kt`
  6. `feature/local/.../components/settings/LanguageSwitcher.kt`（含 LanguageSwitcherDialog）
  7. `feature/local/.../shared/presentation/components/player/PlayerChipOrderBottomSheet.kt`
  8. `feature/local/.../shared/presentation/components/player/ExpressiveBottomButtonsOrderBottomSheet.kt`
  9. `feature/local/.../components/bottomsheets/AutoEQPresetPickerBottomSheet.kt`（抽取实测纯回调，设计原列 PR-9，提前至此）

**Interfaces:**
- Consumes: 无（全部纯回调组件；`LibraryTabOrder`/`HomeSectionOrder`/`PlayerChipOrder`/`ExpressiveBottomButtons` 收 `appSettings: AppSettings` + `HapticFeedback`，均 :core:ui 可见类型）
- Produces: 上述组件自 :core:ui 可被 settings 消费（package 不变，消费方 import 零改动）

- [ ] **Step 1: 逐文件签名核查后 `git mv`**（9 文件）；若某文件编译暴露 :feature:local 独有依赖，停下报告，不得擅自改组件逻辑。
- [ ] **Step 2: 编译验证** `./gradlew :app:compileDebugKotlin` → PASS。
- [ ] **Step 3: Commit**（许可后）`refactor: sink shared settings/local dialogs and bottom sheets to :core:ui`

### Task 5: 7 个接口定义 + MusicViewModel implements

**Files:**
- Create: `core/media/src/main/kotlin/chromahub/rhythm/app/infrastructure/service/PlaybackStateSource.kt`、`PlaybackSettingsController.kt`、`SleepTimerController.kt`
- Create: `core/data/src/main/kotlin/chromahub/rhythm/app/core/domain/repository/PlaylistAdminController.kt`、`LibraryAdminController.kt`、`LibraryCacheManager.kt`、`BackupRestoreController.kt`
- Modify: `MusicViewModel.kt:124-129`（supertype 列表）、`MusicViewModel.kt`（新增 3 个转调方法；`clearAllLyricsCache` 加 `override`）

**Interfaces:**
- Consumes: Task 2 `RestoreResult`、Task 3 `PlaylistImportExportUtils`
- Produces: 7 个接口（下为逐字成员表；`SleepAction` 顶层 enum 随 `SleepTimerController.kt` 落 :core:media，VM 内嵌套定义 MusicViewModel.kt:1219-1221 删除）

```kotlin
// :core:media — PlaybackStateSource.kt
interface PlaybackStateSource {
    val currentSong: StateFlow<Song?>          // MusicViewModel.kt:927
    val isPlaying: StateFlow<Boolean>          // :910
}

// :core:media — PlaybackSettingsController.kt
interface PlaybackSettingsController {
    fun setMonoAudioEnabled(enabled: Boolean)          // :10328
    fun setUseSystemVolumeMode(enable: Boolean)        // :8695（参数名是 enable 不是 enabled）
}

// :core:media — SleepTimerController.kt（含 enum SleepAction { PAUSE, STOP, FADE_OUT }，原 :1219）
interface SleepTimerController {
    val sleepTimerActive: StateFlow<Boolean>           // :10738
    val sleepTimerRemainingSeconds: StateFlow<Long>    // :10741
    val sleepTimerTotalSeconds: StateFlow<Long>        // :10744
    val sleepTimerAction: StateFlow<String>            // :10747（String，默认 "FADE_OUT"）
    val serviceConnected: StateFlow<Boolean>           // :342
    fun startSleepTimer(minutes: Int, action: SleepAction)   // :10096
    fun stopSleepTimer()                                // :10165
}

// :core:data — PlaylistAdminController.kt
interface PlaylistAdminController {
    val playlists: StateFlow<List<Playlist>>           // :895
    fun createPlaylist(name: String, songs: List<Song>, onCreated: ((Playlist) -> Unit)?, showSnackbar: ((String) -> Unit)?)   // :7125（VM 有默认值，接口不带）
    fun deletePlaylist(playlistId: String)             // :7502
    fun importPlaylist(uri: Uri, onResult: (Result<String>) -> Unit, onRestartRequired: (() -> Unit)?)   // :7782
    fun exportAllPlaylists(format: PlaylistImportExportUtils.PlaylistExportFormat, includeDefaultPlaylists: Boolean, userSelectedDirectoryUri: Uri?, onResult: (Result<String>) -> Unit)   // :7649
    fun ensurePlaylistsSaved()                          // :3428
    fun setDefaultPlaylistsEnabled(enabled: Boolean)   // :3334
    fun setShowLikedInPlaylists(enabled: Boolean)      // :3360
    fun setSmartPlaylistMostPlayed(enabled: Boolean)   // :3385
    fun setSmartPlaylistRecentlyPlayed(enabled: Boolean)   // :3400
    fun setSmartPlaylistRecentlyAdded(enabled: Boolean)    // :3380
    fun setSmartPlaylistOnRepeat(enabled: Boolean)     // :3390
    fun setSmartPlaylistForgottenFavorites(enabled: Boolean)   // :3395
}

// :core:data — LibraryAdminController.kt
interface LibraryAdminController {
    val songs: StateFlow<List<Song>>                   // :577
    val filteredSongs: StateFlow<List<Song>>           // :605
    val isLibraryRefreshing: StateFlow<Boolean>        // :1059
    fun refreshLibrary(showMediaScanLoader: Boolean)   // :2478（VM 默认 true，接口不带）
}

// :core:data — LibraryCacheManager.kt
interface LibraryCacheManager {
    fun clearInMemoryCaches()                          // VM 新增：{ repository.clearInMemoryCaches() }（方法本体在 MusicRepositoryImpl.kt:5983）
    fun clearSongCacheData()                           // VM 新增：{ repository.clearSongCacheData() }（:6123）
    suspend fun getRoomSongCount(): Int                // VM 新增：= repository.getRoomSongCount()（:6151，suspend）
    fun clearAllLyricsCache()                          // VM 已有 :8349，加 override
}

// :core:data — BackupRestoreController.kt
interface BackupRestoreController {
    val restoreResult: StateFlow<RestoreResult>        // :145（RestoreResult 已外迁 Task 2）
    fun restoreFromBackup(backupJson: String, sections: AppSettings.BackupRestoreSections)   // :3275（VM 有默认值，接口不带）
    fun clearRestoreResult()                           // :149
}
```

- [ ] **Step 1: 建 7 个接口文件**（成员与类型逐字照上表 + 注释中的 VM 行号核对；`Song`/`Playlist` 来自 `chromahub.rhythm.app.core.model`，`AutoEQProfile` 无关此批）。
- [ ] **Step 2: MusicViewModel supertype 追加 7 接口**（沿用现 :129 全限定风格）；新增 3 个转调方法；`clearAllLyricsCache` 与 7 接口全部成员加 `override`。VM 内嵌套 `SleepAction` 删除，`startSleepTimer` 与内部引用改指顶层 enum。
- [ ] **Step 3: 编译验证** `./gradlew :app:compileDebugKotlin` → PASS（签名不符会在此逐个暴露）。
- [ ] **Step 4: Commit**（许可后）`refactor: define seven narrow seams implemented by MusicViewModel`

### Task 6: SettingsScreenWrapper 增参 + :app 传参（musicViewModel 暂留）

**Files:**
- Modify: `SettingsScreenWrapper.kt:156-161`（签名增 7 参）、`LocalNavigation.kt:2104-2111`、`RhythmNavigation.kt:243-255`

**Interfaces:**
- Consumes: Task 5 的 7 接口
- Produces: wrapper 可向子屏以接口类型传参（Task 7-12 逐屏接线；Task 13 删除 `musicViewModel` 参数）

- [ ] **Step 1: wrapper 签名在 `musicViewModel: MusicViewModel` 之后追加**：`playbackState: PlaybackStateSource, playbackSettings: PlaybackSettingsController, playlistAdmin: PlaylistAdminController, libraryAdmin: LibraryAdminController, cacheManager: LibraryCacheManager, backupRestore: BackupRestoreController, sleepTimer: SleepTimerController`（import 各接口）。
- [ ] **Step 2: 两个 :app 调用点各追加 7 行实参**（`playbackState = viewModel, …, sleepTimer = viewModel`；RhythmNavigation 处用其 `musicViewModel` 参数名）。
- [ ] **Step 3: 编译验证** `./gradlew :app:compileDebugKotlin` → PASS。
- [ ] **Step 4: Commit**（许可后）`refactor: thread seven seams into SettingsScreenWrapper`

### Task 7: PlaybackStateSource → wrapper；PlaybackSettingsController → PlaybackSettingsScreen

**Files:**
- Modify: `SettingsScreenWrapper.kt:167-168`、`PlaybackSettingsScreen.kt:48-55` 及其体内 `musicViewModel.` 引用、`SettingsScreenWrapper.kt:368-371`（横屏）与 `:523-526`（竖屏）、`LocalNavigation.kt:2241-2244`

**Interfaces:**
- Consumes: Task 5 两接口

- [ ] **Step 1: wrapper `currentSong`/`isPlaying` 改源** `playbackState.currentSong` / `playbackState.isPlaying`。
- [ ] **Step 2: PlaybackSettingsScreen**：签名追加 `playbackSettings: PlaybackSettingsController`；删除 `val musicViewModel: MusicViewModel = hiltViewModel()`（:55）；体内 `setMonoAudioEnabled`/`setUseSystemVolumeMode` 调用改经 `playbackSettings`（显式传参，无默认参问题——两方法无默认值）。
- [ ] **Step 3: wrapper 两分支 PLAYBACK 路由与 LocalNavigation:2242 各追加 `playbackSettings = playbackSettings`（或 `= viewModel`）**。
- [ ] **Step 4: 编译 + grep 验证**：`grep -c "musicViewModel" .../PlaybackSettingsScreen.kt` → 0；`grep -c "PlaybackSettingsScreen(" .../SettingsScreenWrapper.kt .../LocalNavigation.kt` 逐处确认两分支均已传参。
- [ ] **Step 5: Commit**（许可后）`refactor: route playback state/settings screens through seams`

### Task 8: PlaylistAdminController → Playlists/DefaultPlaylists 两屏

**Files:**
- Modify: `PlaylistsSettingsScreen.kt:166-173`+体内、`DefaultPlaylistsSettingsScreen.kt:34-40`+体内、`SettingsScreenWrapper.kt:320-323`（横）与 `:475-477`（竖）、`LocalNavigation.kt:2174-2181` 与 `:2185`

**Interfaces:**
- Consumes: Task 5 `PlaylistAdminController`（注意：`createPlaylist`/`exportAllPlaylists`/`importPlaylist` 调用点须显式传全参——VM 默认值在接口类型上不可见；`exportAllPlaylists` 的 format 参数类型经 lambda 推断，无需 import）

- [ ] **Step 1: 两屏签名各追加 `playlistAdmin: PlaylistAdminController`**，删自取行，体内 `musicViewModel.` → `playlistAdmin.`（成员：playlists/createPlaylist/deletePlaylist/importPlaylist/exportAllPlaylists；DefaultPlaylists 屏为 setDefaultPlaylistsEnabled/setShowLikedInPlaylists/setSmartPlaylist×6）。
- [ ] **Step 2: 四处调用点追加 `playlistAdmin = …`**（wrapper 两分支 + LocalNavigation 两处）。
- [ ] **Step 3: 编译 + grep 验证**（两文件 `musicViewModel` 清零；四处调用点齐）。
- [ ] **Step 4: Commit**（许可后）`refactor: route playlist settings screens through PlaylistAdminController`

### Task 9: LibraryAdminController → MediaScanSettingsScreen + BackupRestore 屏（isLibraryRefreshing）

**Files:**
- Modify: `MediaScanSettingsScreen.kt:180-184`+体内、`BackupRestoreSettingsScreen.kt`（仅 isLibraryRefreshing 引用；其余 Task 10）、`SettingsScreenWrapper.kt:327`（横）与 `:482`（竖）、`LocalNavigation.kt:2170`

**Interfaces:**
- Consumes: Task 5 `LibraryAdminController`

- [ ] **Step 1: MediaScanSettingsScreen 签名追加 `libraryAdmin: LibraryAdminController`**，删自取，体内 `songs`/`filteredSongs`/`refreshLibrary` 改经 `libraryAdmin`；**`refreshLibrary()` 调用点改 `refreshLibrary(true)`**（接口无默认参）。
- [ ] **Step 2: BackupRestoreSettingsScreen 的 isLibraryRefreshing 改经 `libraryAdmin`**（本任务先在签名追加 `libraryAdmin` 参数并接线；`backupRestore` 参数 Task 10 追加）。
- [ ] **Step 3: 四处调用点传参**（wrapper 横/竖 + LocalNavigation:2170）。
- [ ] **Step 4: 编译 + grep 验证。**
- [ ] **Step 5: Commit**（许可后）`refactor: route media scan screen through LibraryAdminController`

### Task 10: BackupRestoreController → BackupRestoreSettingsScreen + 残留缓解

**Files:**
- Modify: `BackupRestoreSettingsScreen.kt:190-195`+体内（restoreFromBackup/restoreResult/clearRestoreResult）、`SettingsScreenWrapper.kt:362`（横）与 `:517`（竖）、`LocalNavigation.kt:2196`

**Interfaces:**
- Consumes: Task 5 `BackupRestoreController`、Task 2 `RestoreResult`（已换引用）

- [ ] **Step 1: 签名追加 `backupRestore: BackupRestoreController`**，删自取，体内三个成员改经 `backupRestore`。
- [ ] **Step 2: 缓解（设计 §4.0 A 案）**：屏离开时清残留——在屏的 `DisposableEffect(Unit)` 中 `onDispose { backupRestore.clearRestoreResult() }`（若屏内已有等价生命周期钩子则复用）。
- [ ] **Step 3: 三处调用点传参**。
- [ ] **Step 4: 编译 + grep 验证。**
- [ ] **Step 5: Commit**（许可后）`refactor: route backup/restore screen through BackupRestoreController`

### Task 11: LibraryCacheManager → CacheManagementSettingsScreen

**Files:**
- Modify: `CacheManagementSettingsScreen.kt:249-252`+体内（:300、:609-610、:673、:675 共 5 处 `musicViewModel.getMusicRepository().x()`）、`SettingsScreenWrapper.kt:361`（横）与 `:516`（竖）、`LocalNavigation.kt:2193`

**Interfaces:**
- Consumes: Task 5 `LibraryCacheManager`

- [ ] **Step 1: 签名追加 `cacheManager: LibraryCacheManager`**，删自取；5 处 `musicViewModel.getMusicRepository().clearInMemoryCaches()` 等 → `cacheManager.clearInMemoryCaches()` 等；`clearAllLyricsCache()` 调用同步改 `cacheManager.`；**`getRoomSongCount()` 调用点确认已在协程内**（suspend，现状即如此，不改语义）。
- [ ] **Step 2: 三处调用点传参。**
- [ ] **Step 3: 编译 + grep 验证。**
- [ ] **Step 4: Commit**（许可后）`refactor: route cache management screen through LibraryCacheManager`

### Task 12: SleepTimerController → SleepTimerBottomSheetNew 参数改型（搬移留 Task 16）

**Files:**
- Modify: `feature/local/.../shared/presentation/components/player/SleepTimerBottomSheetNew.kt:67-72`+体内、`SettingsScreenWrapper.kt:553-558`、`LocalNavigation.kt:2226-2234`、player 双屏调用点（`ExpressivePlayerScreen.kt` / `MaterialPlayerScreen.kt`，grep `SleepTimerBottomSheetNew(` 定位）

**Interfaces:**
- Consumes: Task 5 `SleepTimerController`（含顶层 `SleepAction`）

- [ ] **Step 1: 组件签名 `musicViewModel: MusicViewModel` → `sleepTimer: SleepTimerController`**，体内 sleepTimer 成员经参数（`startSleepTimer(minutes, SleepAction.X)` 引用改顶层 enum）。
- [ ] **Step 2: 全部调用点传参**：wrapper 传 `sleepTimer = sleepTimer`；LocalNavigation:2233 与 player 双屏传 `musicViewModel`（实现类上转，local/:app 均可见）。
- [ ] **Step 3: 编译 + grep `SleepTimerBottomSheetNew(` 全部调用点核齐（预期 4 处）。**
- [ ] **Step 4: Commit**（许可后）`refactor: type SleepTimerBottomSheetNew against SleepTimerController`

### Task 13: wrapper 摘除 musicViewModel

**Files:**
- Modify: `SettingsScreenWrapper.kt:156-161`、`LocalNavigation.kt:2104-2111`、`RhythmNavigation.kt:243-255`

- [ ] **Step 1: grep 确认 wrapper 内 `musicViewModel` 引用为零**（Task 6-12 应已全部切换；若有漏网回到对应任务补线）。
- [ ] **Step 2: 删除 wrapper 的 `musicViewModel: MusicViewModel` 参数与 import；两个 :app 调用点删除对应实参。**
- [ ] **Step 3: 编译验证 → PASS。**
- [ ] **Step 4: Commit**（许可后）`refactor: drop MusicViewModel parameter from SettingsScreenWrapper`

### Task 14: PR-8 收口

- [ ] **Step 1: 全模块单测**（AGENTS.md §1 命令）→ 全绿。
- [ ] **Step 2: `./gradlew assembleDebug`** → 绿。
- [ ] **Step 3: `./gradlew :app:installDebug` 设备冒烟**：设置页全量走查——播放列表 CRUD/导入导出（含格式选择与 AppRestartDialog）、默认与智能播放列表开关、缓存清理（含歌词缓存）、备份恢复（**发起→离开→重进屏无陈旧面板**）、扫描刷新、单声道/系统音量、mini 播放条状态、睡眠定时器（settings 与 player 双入口）。横竖屏两方向各走一遍关键屏。
- [ ] **Step 4: Commit**（许可后）`chore: PR-8 gate — full unit tests, assembleDebug, device smoke`

---

# PR-9：EQ 接口 + 断依赖

### Task 15: AudioEffectsController + AudioDeviceController 定义 + implements

**Files:**
- Create: `core/media/src/main/kotlin/chromahub/rhythm/app/infrastructure/service/AudioEffectsController.kt`、`AudioDeviceController.kt`
- Modify: `MusicViewModel.kt:124-129`（追加 2 接口）

**Interfaces:**
- Produces（34 成员，逐字；类型实测自 AppSettings.kt:1184-1258 与 VM 行号）：

```kotlin
// AudioEffectsController.kt — 16 val + 18 fun
interface AudioEffectsController {
    val equalizerEnabled: StateFlow<Boolean>                     // AppSettings:1184 经 VM:288
    val equalizerPreset: StateFlow<String>                       // :1187 / VM:289
    val equalizerBandLevels: StateFlow<String>                   // :1190 / VM:290
    val bassBoostEnabled: StateFlow<Boolean>                     // :1205 / VM:291
    val bassBoostStrength: StateFlow<Int>                        // :1208 / VM:292
    val virtualizerEnabled: StateFlow<Boolean>                   // :1211 / VM:293
    val virtualizerStrength: StateFlow<Int>                      // :1214 / VM:294
    val customEqualizerPresets: StateFlow<List<CustomEqualizerPreset>>   // :1227 / VM:296
    val equalizerPresetOrder: StateFlow<List<String>>            // :1236 / VM:297
    val hiddenEqualizerPresets: StateFlow<Set<String>>           // :1245 / VM:298
    val pinnedAutoEQProfiles: StateFlow<List<String>>            // :1253 / VM:299
    val customAutoEQProfiles: StateFlow<List<AutoEQProfile>>     // :1258 / VM:300
    val autoEQProfiles: StateFlow<List<AutoEQProfile>>           // VM:248
    val spatializationStatus: StateFlow<String>                  // VM:306
    val isSpatializationAvailable: StateFlow<Boolean>            // VM:309
    val isBassBoostAvailable: StateFlow<Boolean>                 // VM:313
    fun setEqualizerEnabled(enabled: Boolean)                    // VM:10195
    fun setEqualizerBandLevel(band: Short, level: Short)         // VM:10236
    fun applyEqualizerPreset(preset: String, levels: List<Float>)    // VM:10367
    fun saveCustomEqualizerPreset(name: String, bands: List<Float>): CustomEqualizerPreset   // VM:10588
    fun applyAutoEQProfile(profile: AutoEQProfile)               // VM:10420
    fun loadAutoEQProfiles()                                     // VM:10398
    fun pinAutoEQProfile(name: String)                           // VM:10712
    fun setVirtualizer(enabled: Boolean, strength: Short)        // VM:10299（VM 默认 500）
    fun setBassBoost(enabled: Boolean, strength: Short)          // VM:10263（VM 默认 500）
    fun openSystemEqualizer(activity: Activity?, requestCode: Int)   // VM:8742（VM 默认 null/0）
    fun updateSpatializationStatus()                             // VM:10348
    fun updateBassBoostAvailability()                            // VM:10290
    fun setEqualizerPresetOrder(order: List<String>)             // VM:10700
    fun resetEqualizerPresetOrder()                              // VM:10704
    fun deleteAutoEQProfile(name: String)                        // VM:10645
    fun deleteCustomEqualizerPreset(id: String)                  // VM:10598
    fun setHiddenEqualizerPresets(hidden: Set<String>)           // VM:10708
    fun saveCustomAutoEQProfiles(profiles: List<AutoEQProfile>)  // VM:10629
}

// AudioDeviceController.kt
interface AudioDeviceController {
    val audioDeviceManager: AudioDeviceManager                   // VM:238（类型在 :core:media）
    fun saveUserAudioDevice(device: UserAudioDevice)             // VM:10477
    fun deleteUserAudioDevice(deviceId: String)                  // VM:10494
    fun setActiveAudioDevice(device: UserAudioDevice)            // VM:10509
    fun findMatchingUserDevice(deviceName: String): UserAudioDevice?   // VM:10538
    fun dismissAutoEQSuggestion(deviceId: String)                // VM:10576
}
```
（`UserAudioDevice`/`AutoEQProfile`/`CustomEqualizerPreset` 来自 `chromahub.rhythm.app.core.model`；`AudioDeviceManager` 为 :core:media 类型。）

- [ ] **Step 1: 建两接口文件**（逐字照上表）；Step 2: VM supertype 追加 2 接口，成员加 `override`（VM 默认参数保留在 VM 声明上）。Step 3: 编译验证 → PASS。Step 4: Commit（许可后）`refactor: define audio effects and device seams`

### Task 16: getLocalizedPresetName 下沉 :core:designsystem

**Files:**
- Move: `feature/local/src/main/kotlin/chromahub/rhythm/app/util/EqualizerUtils.kt` 中的**顶层** `@Composable fun getLocalizedPresetName(name: String): String`（:168 至文件尾）→ 新文件 `core/designsystem/src/main/kotlin/chromahub/rhythm/app/util/EqualizerPresetNames.kt`（package `chromahub.rhythm.app.util` 保留）
- Modify: `EqualizerUtils.kt`（移除该函数；`object EqualizerUtils`（:23，openSystemEqualizer/isSystemEqualizerAvailable）留守）

**Interfaces:**
- Produces: `getLocalizedPresetName` 对 local/settings 双方可见（同 package，import 零改动；:core:designsystem 有 R 与 Compose，:core:media 没有——故不落 media）

- [ ] **Step 1: 拆分搬移**（函数体逐字）；Step 2: 编译验证 → PASS（消费方：EqualizerScreen:869、ExpressiveShapesSettingsScreen、EqualizerPresetOrderBottomSheet、EqualizerUtils 内部）。Step 3: Commit（许可后）`refactor: sink getLocalizedPresetName to :core:designsystem`

### Task 17: PR-9 批组件改型 + 下沉（3 文件）——先于 EqualizerScreen 转换

**顺序理由**：组件先改型后，EqualizerScreen（仍持 MusicViewModel 类型）以实现类上转调用新签名（`audioEffects = viewModel`），每个任务结束均可编译；若先转 EqualizerScreen 则中间态编译失败。

**Files:**
- Modify+Move: `feature/local/.../components/bottomsheets/EqualizerPresetOrderBottomSheet.kt`（签名 `musicViewModel: MusicViewModel` → `audioEffects: AudioEffectsController`，:83-86；体内 `musicViewModel.` → `audioEffects.`；`git mv` → `core/ui` 同 package 路径）
- Modify+Move: `DeviceConfigurationBottomSheet.kt`（签名 `musicViewModel: MusicViewModel, onDismiss` → `audioDevices: AudioDeviceController, appSettings: AppSettings, onDismiss`，:123-126；体内设备域成员经 `audioDevices`、AppSettings 读写经 `appSettings` 参数；`git mv` → `core/ui`）
- Move: `feature/local/.../shared/presentation/components/player/SleepTimerBottomSheetNew.kt`（Task 12 已改型，`git mv` → `core/ui/src/main/kotlin/chromahub/rhythm/app/shared/presentation/components/player/`，package 保留）

**Interfaces:**
- Consumes: Task 15 两接口、Task 5 `SleepTimerController`
- 影响: DeviceConfigurationBottomSheet 其余调用方（`ExpressivePlayerScreen.kt`、`MaterialPlayerScreen.kt`，:feature:local）改传 `audioDevices = <其 musicViewModel>, appSettings = <其 appSettings>`（该屏经 `AppSettings.getInstance` 或既有参数，编译驱动）；EqualizerScreen 调用点同法上转（`audioEffects = viewModel, audioDevices = viewModel, appSettings = viewModel.appSettings`）

- [ ] **Step 1: 两文件改型 + 三文件搬移**。
- [ ] **Step 2: 全部调用点接线**（grep `DeviceConfigurationBottomSheet(`、`EqualizerPresetOrderBottomSheet(`、`SleepTimerBottomSheetNew(` 核齐：EqualizerScreen、player 双屏、wrapper:553、LocalNavigation:2226）。
- [ ] **Step 3: 编译验证 → PASS。**
- [ ] **Step 4: Commit**（许可后）`refactor: retype and sink EQ/device/sleep-timer sheets to :core:ui`

### Task 18: EqualizerScreen 转换

**Files:**
- Modify: `feature/settings/.../features/local/presentation/screens/EqualizerScreen.kt:317-320`+体内（`viewModel.` ×28、`viewModel.appSettings` ×12）、`LocalNavigation.kt:2223`、`:2387`

**Interfaces:**
- Consumes: Task 15 两接口、Task 17 已就位的组件签名

- [ ] **Step 1: 签名改为**：
```kotlin
fun EqualizerScreen(
    navController: NavController,
    audioEffects: AudioEffectsController,
    audioDevices: AudioDeviceController,
    appSettings: AppSettings
)
```
（删除 `viewModel: MusicViewModel = hiltViewModel()` 默认值——两处调用点本就显式传参。）
- [ ] **Step 2: 体内 `viewModel.` → `audioEffects.`**；**12 处 `viewModel.appSettings.*` → `appSettings.*`**；带默认值的接口方法调用点补显式实参（如 `setVirtualizer(enabled, 500)`——以编译驱动逐处核对现值）；Task 17 传入的 `audioDevices = viewModel` / `appSettings = viewModel.appSettings` 实参同步改为新参数名。
- [ ] **Step 3: LocalNavigation 两处（:2223、:2387）改传 `audioEffects = viewModel, audioDevices = viewModel, appSettings = appSettings`**（LocalNavigation 自身有 appSettings 参数 :373；同一单例实例）。
- [ ] **Step 4: 编译 + grep 验证**：EqualizerScreen 内 `MusicViewModel` import 与 `viewModel.` 清零。
- [ ] **Step 5: Commit**（许可后）`refactor: route EqualizerScreen through audio seams`

### Task 19: wrapper 的 local 路由常量替换

**Files:**
- Modify: `core/ui/.../settings/SettingsRoutes.kt`（+1 常量）、`SettingsScreenWrapper.kt:202`、`:221`（+import 调整）

**Interfaces:**
- Produces: `SettingsRoutes.LOCAL_EQUALIZER = "equalizer"`（新；`Screen.Equalizer.route` 的字面值，Screen.kt:76。注意 `SettingsRoutes.EQUALIZER = "equalizer_settings"` 是设置子页路由，勿混用）。`RHYTHM_STATS = "rhythm_stats"` 已存在（Screen.kt:75 同值），直接换引用。

- [ ] **Step 1: SettingsRoutes 追加 `const val LOCAL_EQUALIZER = "equalizer"`**；Step 2: wrapper `Screen.RhythmStats.route` → `SettingsRoutes.RHYTHM_STATS`、`Screen.Equalizer.route` → `SettingsRoutes.LOCAL_EQUALIZER`；删除 wrapper 的 `Screen` import。Step 3: 编译验证 → PASS。Step 4: Commit（许可后）`refactor: replace local Screen route references with SettingsRoutes`

### Task 20: 删除依赖行 + 门禁

**Files:**
- Modify: `feature/settings/build.gradle.kts:22-25`（删除注释块与 `implementation(project(":feature:local"))`）

- [ ] **Step 1: 删依赖行与注释**。Step 2: **门禁 = `./gradlew :app:compileDebugKotlin` 绿**（模块可见性由 Gradle 依赖图唯一裁决，删行后编译通过即完备）；Step 3: 卫生检查 `grep -rn "import chromahub.rhythm.app.features.local." feature/settings/src/main --include=*.kt` → 仅剩 EqualizerScreen 自身 package 声明（同包物理异模块，非依赖）。Step 4: Commit（许可后）`build!: drop :feature:local dependency from :feature:settings`

### Task 21: PR-9 收口 + 文档回写

- [ ] **Step 1: 全模块单测 + assembleDebug** → 全绿。
- [ ] **Step 2: 设备冒烟**：EQ 专项（播放中改预设/频段/AutoEQ/空间音频/低音增强实时生效、自定义预设保存、系统均衡器跳转、设备配置与 AutoEQ 建议）+ PR-8 冒烟清单复跑 + 横竖屏。
- [ ] **Step 3: AGENTS.md §4 修订**：删除「settings → local」债务行（4 条 → 3 条）；§4 代码放置说明补一句「共享对话框/BottomSheet 已下沉 :core:ui（B1 PR-8/9）」。
- [ ] **Step 4: `docs/multi-module-refactor-plan.md` 偏差回写**：B1 第一阶段完成记录（含 `PlaylistImportExportUtils` → :core:data、`AudioEffectsController` 34 成员实测修正、AutoEQPresetPicker 提前 PR-8 三项偏差）。
- [ ] **Step 5: Commit**（许可后）`docs: close out B1 phase 1 — settings→local debt resolved`
