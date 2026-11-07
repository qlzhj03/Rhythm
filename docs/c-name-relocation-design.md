# C 项「名实归位」设计（短稿）

- 日期：2026-11-07 · 状态：待批准
- 来源：重构路线图 C 项 + 本次全仓侦查
- 范围：包名归位（一次性）+ MusicRepository 改名 + proguard keep 审计；Gson 收尾挂账 B2

## 意图与动机

多模块拆分采用零 churn 策略，留下 32 文件 / 19 组「目录 ≠ 声明包」错位，靠
`android.uniquePackageNames=false` 宽容。目标：声明包与所在目录一致，**import 即定位
文件位置**，AGENTS.md §4 的模块≠包名对照说明可撤。

## 目标 / 非目标 / 反目标

- **目标**：三类错位全清（声明就位 / 目录就位 / 测试目录就位）；MusicRepository 混淆点
  改名；keep 规则与现实对齐；收尾更新 AGENTS.md §4。
- **非目标**：不迁移任何文件跨模块；不重构 `shared.*` / `features.*` / `core.*` 既有
  领域式命名（另一个量级的工程）；Gson→kotlinx 不做（实测 29 文件 / 9 模块在用，远超
  C 项描述的三个点，并入 B2）；manifest 与对外协议零改动（已核实：无错位类被 manifest
  引用；`ComponentName` 引用的 MediaPlaybackService / RhythmTileService /
  RhythmWidgetReceiver 本就对位）。
- **反目标（失败态）**：改出运行时回归——尤其是 WorkManager 持久化任务与 release 混淆。

## 方案（已选定：一次性归位，分两 PR）

- **PR-1「名字与文件就位」**：改名 + 纯 `git mv`，近零 import 面（仅改名涉及的约 5 处引用）。
- **PR-2「声明就位」**：11 个文件的 package 声明改为目录包 + 引用方 import 更新
  （已知 30 处 `FontLoader` + 1 处 `StatusBroadcaster` import，多为 :feature:settings）。

否决项：「随触碰顺手归位」永不收敛，两条约定长期共存、对照表必须常留；「只做改名」
让 19 组错位继续付认知税。

## 既定决策（实施直接照做）

1. **对齐规则**：
   - Rule 1 声明就位：声明指向错误模块/旧包的 11 文件，package 改为目录包（附录 A）。
   - Rule 2 目录就位：声明正确、目录缺层的 12 文件，`git mv` 进声明对应子目录，
     零代码改动（附录 B）。
   - Rule 3 测试目录就位：8 个测试文件 `git mv` 进 `features/` 路径，零代码改动（附录 C）。
   - 例外：`feature/streaming/src/test/java/net/FakeUri.kt` 是刻意的 android.net 影子
     桩，不动。
2. **Worker 特例（红线级）**：三个 Worker **只挪目录、不改声明**——WorkManager 的
   WorkSpec 持久化 worker 类名字符串，改声明会导致「升级后 → 首次启动前」窗口内旧任务以
   旧类名实例化失败（AppSettings 用 `ExistingPeriodicWorkPolicy.UPDATE` 首启重排，但该
   窗口无人补救）。保持 `chromahub.rhythm.app.worker.*` 声明可归零此风险；RhythmApplication
   的全限定引用原样编译。
3. **改名侧**：改具体类——`:feature:local` 的 `class MusicRepository`（位于
   `MusicRepositoryImpl.kt`，全仓并无 MusicRepositoryImpl 类，文件名亦是遗留误导）→
   `LocalMusicRepository`，文件改名 `LocalMusicRepository.kt`，类内 TAG 同步；
   `:core:data` 接口 `core.domain.repository.MusicRepository` 与
   `StreamingMusicRepository : MusicRepository` 继承链不动。对仗 streaming 侧命名。
   引用方：MusicViewModel、LibraryNotificationManager、HomeScreen、
   ArtistArtworkFilenameTest（改前 `rg` 复核全量）。
4. **keep 审计**：修 `app/proguard-rules.pro:24`（`chromahub.rhythm.app.util.ExtractedColors`
   指向已不存在的包，实际类在 `chromahub.rhythm.app.core.designsystem.utils`，现为静默
   失效）；`:55` 的 entity keep 在 Rule 2 下原样有效；其余 keep 逐条对照移动后 FQCN 复核。
5. **验证门禁**：每 PR：`./gradlew :app:compileDebugKotlin` → AGENTS.md 全部 JVM 单测
   清单 → `assembleDebug` + `assembleRelease`（PR-1 动 keep、PR-2 动声明，都必须跑 R8）。
   终态自检：错位扫描脚本仅剩 FakeUri；全仓 grep `chromahub.rhythm.app.utils` 归零。
6. **AGENTS.md 收尾**：PR-2 落地后更新 §4：「import 按文件内 package 声明写」保留，
   模块≠包名错位说明删除。

## 留给实施者

- `LocalMusicRepository` 撞名检查已完成（全仓无此名）。
- PR-2 的 import 替换用精确 FQCN（`chromahub.rhythm.app.utils.FontLoader` → 新包），
  避免在 `utils`/`util` 前缀上误伤。
- `ProfileImportExportUtils` 当前无任何外部引用（仅自身文件），按「看似死代码勿删」
  只改声明、不删码、不在本项内处置。

## 附录 A：Rule 1 声明就位（11 文件，package 改为目录包）

| 文件（缩写至 src 根后） | 现声明 | 新声明 |
| :-- | :-- | :-- |
| app/.../shared/presentation/theme/festive/FestiveOverlay.kt | ...app.core.designsystem.theme.festive | ...app.shared.presentation.theme.festive |
| app/.../shared/presentation/theme/festive/FestiveThemeExamples.kt | 同上 | 同上 |
| app/.../navigation/LocalNavigation.kt | ...app.features.local.presentation.navigation | ...app.navigation |
| app/.../onboarding/OnboardingScreen.kt | ...app.features.local.presentation.screens | ...app.onboarding |
| app/.../onboarding/OnboardingModels.kt | ...app.features.local.presentation.screens.onboarding | ...app.onboarding |
| app/.../onboarding/StreamingProviderChoice.kt | 同上 | 同上 |
| app/.../navigation/MainScreen.kt | ...app.shared.presentation.navigation | ...app.navigation |
| app/.../navigation/RhythmNavigation.kt | 同上 | 同上 |
| app/.../util/ProfileImportExportUtils.kt | ...app.utils | ...app.util |
| core/designsystem/.../core/designsystem/utils/FontLoader.kt | ...app.utils | ...app.core.designsystem.utils |
| core/media/.../app/util/StatusBroadcaster.kt | ...app.utils | ...app.util |

（`...app` = `chromahub.rhythm.app`）

## 附录 B：Rule 2 目录就位（12 文件，声明不变，纯 git mv）

| 现路径 | 目标路径（同名目录补层） |
| :-- | :-- |
| app/src/main/java/.../app/infrastructure/worker/RhythmPulseNotificationWorker.kt | app/src/main/java/.../app/worker/ |
| app/src/main/java/.../app/infrastructure/worker/UpdateNotificationWorker.kt | 同上 |
| core/media/src/main/kotlin/.../app/infrastructure/worker/BackupWorker.kt | core/media/.../app/worker/ |
| core/data/src/main/kotlin/.../features/local/data/database/{SongDao,SongArtistDao,ArtistDao,PlaylistDao}.kt | .../database/dao/ |
| core/data/src/main/kotlin/.../features/local/data/database/{PlaylistEntity,PlaylistSongEntity,ArtistEntity,SongEntity,SongArtistEntity}.kt | .../database/entity/ |

## 附录 C：Rule 3 测试目录就位（8 文件，声明不变，纯 git mv）

| 现路径（test 源集） | 目标路径 |
| :-- | :-- |
| feature/local/.../app/local/data/repository/ArtistArtworkFilenameTest.kt | .../app/features/local/data/repository/ |
| feature/local/.../app/local/presentation/screens/DiscoverCarouselSupportTest.kt | .../app/features/local/presentation/screens/ |
| feature/streaming/.../app/streaming/StreamingFixesTest.kt | .../app/features/streaming/ |
| feature/streaming/.../app/streaming/data/provider/{NetEaseApiClientTest,NetEaseCryptoTest,SubsonicLibrarySyncTest}.kt | .../app/features/streaming/data/provider/ |
| feature/streaming/.../app/streaming/data/repository/StreamingCatalogOptimizationTest.kt | .../app/features/streaming/data/repository/ |
| feature/streaming/.../app/streaming/domain/model/StreamingPlaybackUrisTest.kt | .../app/features/streaming/domain/model/ |
