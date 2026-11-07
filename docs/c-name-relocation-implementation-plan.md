# C 项「名实归位」实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 清除 32 文件 / 19 组「目录 ≠ 声明包」错位，具体类 `MusicRepository` 改名 `LocalMusicRepository`，keep 审计收尾；分两个 PR 落地。

**Architecture:** 纯机械重构、零行为变更：PR-1 挪文件与改名（FQCN 与声明一律不变），PR-2 改 package 声明并更新 import。三个 WorkManager Worker 只挪目录、声明红线不动。

**Tech Stack:** Kotlin / AGP（仅 wrapper）、git mv、GNU sed、python3（错位扫描）。零依赖变更。

**Spec:** `docs/c-name-relocation-design.md`（其附录 A/B/C 是 32 文件权威清单；本计划从中论证，执行者两份都要读）

## Global Constraints

- 每任务后快检 `./gradlew :app:compileDebugKotlin`；禁止裸 `./gradlew test`，测试按模块指定（门禁任务给出全量命令）。
- 本计划的 commit 步骤已获批准；**push / 建 PR 是 Task 11，需用户明示**。不自行打 tag。
- 三 Worker 的 `package chromahub.rhythm.app.worker` 声明不得改动（WorkManager WorkSpec 持久化 worker 类名字符串）。
- `feature/streaming/src/test/java/net/FakeUri.kt` 不动——刻意的 android.net 影子桩，是终态扫描唯一预期残留。
- 不改 gradle.properties / Version Catalog / 依赖；manifest 零改动。
- import 禁通配符；移动后产生的同包冗余 import 一并删除。
- `ProfileImportExportUtils` 全仓零引用，按「看似死代码勿删」只改声明、不删码。
- 所有 `git mv` 先 `mkdir -p` 目标目录（git 不追踪空目录；`database/dao`、`database/entity` 已存在于磁盘）。

## Review Focus

1. **Worker 类名漂移**（升级后→首启前实例化失败）：Task 3 移动后三文件 package 行必须仍是 `chromahub.rhythm.app.worker`，`app/src/main/java/chromahub/rhythm/app/RhythmApplication.kt:184-189` 全限定引用零改动。
2. **utils/util 前缀误伤**：Task 9 的 sed 必须整行匹配完整 FQCN，禁止裸 `utils`→`util` 替换；收尾全仓 grep 归零。
3. **接口引用误改**：`core.domain.repository.MusicRepository` 接口不得被替换波及——Task 2 的 sed 只作用于列出的 5 个文件，且替换后跑反证 grep。
4. **keep 与 FQCN 失配**：Task 1 逐条核对；Task 5 / Task 10 的 `assembleRelease`（R8）是最终闸门。
5. **扫描残留**：终态扫描应恰好 1 条 = FakeUri；多于 1 条即漏网，禁止动 FakeUri 凑数。

---

### Task 1: PR-1 分支 + proguard keep 审计

**Files:**
- Modify: `app/proguard-rules.pro`

**Interfaces:**
- Produces: 干净的 keep 面（无指向不存在 FQCN 的规则），后续任务移动文件时逐条有效。

- [ ] **Step 1: 建分支**

```bash
git checkout -b refactor/c-pr1-align-files
```

- [ ] **Step 2: 提取并核对全部项目内 keep**

```bash
rg -n "^-keep" app/proguard-rules.pro
```

对每条含 `chromahub.rhythm.app.*` 的规则，取其 SimpleName 核对实际声明位置与包是否一致：
`rg -n "(class|object|interface) <SimpleName>" -g '*.kt' -g '!**/build/**'`。
`-keep class *` / `-keepclassmembers class *` / `$$serializer` 等框架规则跳过。

- [ ] **Step 3: 修已知失配（其余按 Step 2 结果同法处理）**

`app/proguard-rules.pro:24` 的 `chromahub.rhythm.app.util.ExtractedColors` 指向已不存在的包（实际声明在 `core/designsystem/src/main/kotlin/chromahub/rhythm/app/core/designsystem/utils/ColorExtractor.kt:35`），改为：

```proguard
-keep class chromahub.rhythm.app.core.designsystem.utils.ExtractedColors { *; }
```

- [ ] **Step 4: Commit**

```bash
git add app/proguard-rules.pro
git commit -m "refactor: fix stale ExtractedColors proguard keep"
```

（keep 改动无 debug 编译影响，R8 验证在 Task 5 门禁。）

### Task 2: MusicRepository → LocalMusicRepository

**Files:**
- Rename: `feature/local/src/main/kotlin/chromahub/rhythm/app/features/local/data/repository/MusicRepositoryImpl.kt` → `LocalMusicRepository.kt`（git mv）
- Modify: `LocalMusicRepository.kt`（类名、TAG、Log 标签）、`MusicViewModel.kt`、`HomeScreen.kt`、`LibraryNotificationManager.kt`、`feature/local/src/test/java/chromahub/rhythm/app/local/data/repository/ArtistArtworkFilenameTest.kt`

**Interfaces:**
- Produces: `class LocalMusicRepository @Inject constructor(...)`，FQCN `chromahub.rhythm.app.features.local.data.repository.LocalMusicRepository`；访问器 `MusicViewModel.getLocalMusicRepository(): LocalMusicRepository`。接口 `core.domain.repository.MusicRepository` 与 `StreamingMusicRepository : MusicRepository` 不动。

- [ ] **Step 1: 全量引用复核（预期恰 5 个文件）**

```bash
rg -n "\bMusicRepository\b" app feature/local -g '*.kt' -g '!**/build/**' | rg -v "StreamingMusicRepository|getMusicRepository"
```

预期仅：LocalMusicRepository.kt（改名前为 MusicRepositoryImpl.kt，13 处）、MusicViewModel.kt（import:49、类型:128、注释:10098-10099、返回类型:10101）、LibraryNotificationManager.kt（:13、:169）、ArtistArtworkFilenameTest.kt（14 处）。HomeScreen 只有 `getMusicRepository()` 调用（已过滤）。出现预期外文件 → 逐个加入后续替换清单，不得凭空跳过。

- [ ] **Step 2: git mv + 词边界替换**

```bash
git mv feature/local/src/main/kotlin/chromahub/rhythm/app/features/local/data/repository/MusicRepositoryImpl.kt \
      feature/local/src/main/kotlin/chromahub/rhythm/app/features/local/data/repository/LocalMusicRepository.kt
sed -i -E 's/\bMusicRepository\b/LocalMusicRepository/g' \
  feature/local/src/main/kotlin/chromahub/rhythm/app/features/local/data/repository/LocalMusicRepository.kt \
  feature/local/src/main/kotlin/chromahub/rhythm/app/features/local/presentation/viewmodel/MusicViewModel.kt \
  feature/local/src/main/kotlin/chromahub/rhythm/app/features/local/presentation/viewmodel/LibraryNotificationManager.kt \
  feature/local/src/main/kotlin/chromahub/rhythm/app/features/local/presentation/screens/HomeScreen.kt \
  feature/local/src/test/java/chromahub/rhythm/app/local/data/repository/ArtistArtworkFilenameTest.kt
sed -i 's/getMusicRepository/getLocalMusicRepository/g' \
  feature/local/src/main/kotlin/chromahub/rhythm/app/features/local/presentation/viewmodel/MusicViewModel.kt \
  feature/local/src/main/kotlin/chromahub/rhythm/app/features/local/presentation/screens/HomeScreen.kt
```

说明：`\b` 保证 `StreamingMusicRepository`（前邻 `g`）与 `getMusicRepository`（前邻 `t`）不受第一个 sed 影响；`LocalMusicRepository.kt` 内 5600-5678 的 Log 字符串 `"MusicRepository"` 属预期内一并更新（TAG 同步）。

- [ ] **Step 3: 反证复核**

```bash
rg -n "StreamingLocalMusicRepository|core\.domain\.repository\.LocalMusicRepository" -g '*.kt' -g '!**/build/**'   # 预期 0
rg -n "getMusicRepository" -g '*.kt' -g '!**/build/**'                                                              # 预期 0
rg -n "\bMusicRepository\b" app feature/local -g '*.kt' -g '!**/build/**' | rg -v "StreamingMusicRepository|getMusicRepository"   # 预期 0
```

- [ ] **Step 4: 快检 + 模块单测**

```bash
./gradlew :app:compileDebugKotlin :feature:local:testDebugUnitTest
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: rename concrete MusicRepository to LocalMusicRepository"
```

### Task 3: Rule 2 目录就位（12 文件，声明不动）

**Files:** 仅移动，零代码改动。清单 = 设计稿附录 B。

- [ ] **Step 1: Worker ×3（红线：声明不动）**

```bash
mkdir -p app/src/main/java/chromahub/rhythm/app/worker
git mv app/src/main/java/chromahub/rhythm/app/infrastructure/worker/RhythmPulseNotificationWorker.kt app/src/main/java/chromahub/rhythm/app/worker/
git mv app/src/main/java/chromahub/rhythm/app/infrastructure/worker/UpdateNotificationWorker.kt app/src/main/java/chromahub/rhythm/app/worker/
mkdir -p core/media/src/main/kotlin/chromahub/rhythm/app/worker
git mv core/media/src/main/kotlin/chromahub/rhythm/app/infrastructure/worker/BackupWorker.kt core/media/src/main/kotlin/chromahub/rhythm/app/worker/
```

- [ ] **Step 2: 钉死 Worker 声明**

```bash
rg -n "^package" app/src/main/java/chromahub/rhythm/app/worker/*.kt core/media/src/main/kotlin/chromahub/rhythm/app/worker/BackupWorker.kt
```

预期三行均为 `package chromahub.rhythm.app.worker`。

- [ ] **Step 3: dao ×4 / entity ×5 落入既有空子目录**

```bash
cd core/data/src/main/kotlin/chromahub/rhythm/app/features/local/data/database  # 或用全路径逐个 git mv
git mv SongDao.kt SongArtistDao.kt ArtistDao.kt PlaylistDao.kt dao/
git mv PlaylistEntity.kt PlaylistSongEntity.kt ArtistEntity.kt SongEntity.kt SongArtistEntity.kt entity/
```

（若 shell 禁 cd，用全路径逐个 `git mv`，语义相同。`RhythmDatabase.kt` 不动。）

- [ ] **Step 4: 快检 + Commit**

```bash
./gradlew :app:compileDebugKotlin
git add -A && git commit -m "refactor: place dao/entity/worker files into matching package dirs"
```

### Task 4: Rule 3 测试目录就位（8 文件，声明不动）

**Files:** 仅移动，零代码改动。清单 = 设计稿附录 C。

- [ ] **Step 1: git mv（目标目录 mkdir -p）**

```bash
mkdir -p feature/local/src/test/java/chromahub/rhythm/app/features/local/{data/repository,presentation/screens}
git mv feature/local/src/test/java/chromahub/rhythm/app/local/data/repository/ArtistArtworkFilenameTest.kt feature/local/src/test/java/chromahub/rhythm/app/features/local/data/repository/
git mv feature/local/src/test/java/chromahub/rhythm/app/local/presentation/screens/DiscoverCarouselSupportTest.kt feature/local/src/test/java/chromahub/rhythm/app/features/local/presentation/screens/
mkdir -p feature/streaming/src/test/java/chromahub/rhythm/app/features/streaming/{data/provider,data/repository,domain/model}
git mv feature/streaming/src/test/java/chromahub/rhythm/app/streaming/StreamingFixesTest.kt feature/streaming/src/test/java/chromahub/rhythm/app/features/streaming/
git mv feature/streaming/src/test/java/chromahub/rhythm/app/streaming/data/provider/NetEaseApiClientTest.kt feature/streaming/src/test/java/chromahub/rhythm/app/streaming/data/provider/NetEaseCryptoTest.kt feature/streaming/src/test/java/chromahub/rhythm/app/streaming/data/provider/SubsonicLibrarySyncTest.kt feature/streaming/src/test/java/chromahub/rhythm/app/features/streaming/data/provider/
git mv feature/streaming/src/test/java/chromahub/rhythm/app/streaming/data/repository/StreamingCatalogOptimizationTest.kt feature/streaming/src/test/java/chromahub/rhythm/app/features/streaming/data/repository/
git mv feature/streaming/src/test/java/chromahub/rhythm/app/streaming/domain/model/StreamingPlaybackUrisTest.kt feature/streaming/src/test/java/chromahub/rhythm/app/features/streaming/domain/model/
```

注意：`net/FakeUri.kt` 不在清单内，不动。Task 2 已把 ArtistArtworkFilenameTest 的引用改为 LocalMusicRepository——本任务只挪位置。

- [ ] **Step 2: 模块单测 + Commit**

```bash
./gradlew :feature:local:testDebugUnitTest :feature:streaming:testDebugUnitTest
git add -A && git commit -m "refactor: align test source dirs with declared packages"
```

### Task 5: PR-1 门禁

- [ ] **Step 1: 错位扫描（脚本见附录）**——预期 12 条：设计稿附录 A 的 11 文件 + FakeUri。多/少都要先解释再继续。
- [ ] **Step 2: 全部 JVM 单测（AGENTS.md 原样）**

```bash
./gradlew :app:testDebugUnitTest :core:common:testDebugUnitTest :core:media:testDebugUnitTest :core:ui:testDebugUnitTest :core:designsystem:testDebugUnitTest :core:data:testDebugUnitTest :feature:local:testDebugUnitTest :feature:streaming:testDebugUnitTest
```

- [ ] **Step 3: R8 闸门（Task 1 keep 修改在此验证）**

```bash
./gradlew assembleDebug assembleRelease
```

- [ ] **Step 4: 预期工作树干净（门禁任务不产生新 commit；红了先修再重跑本任务）**

### Task 6: PR-2 分支 + 导航簇声明就位（3 文件）

**Files:**
- Modify: `app/src/main/java/chromahub/rhythm/app/navigation/LocalNavigation.kt`、`MainScreen.kt`、`RhythmNavigation.kt`

**Interfaces:**
- Produces: 三者新声明均为 `chromahub.rhythm.app.navigation`（Task 7-9 同法）。

- [ ] **Step 1: 建 PR-2 分支（叠在 PR-1 上）**

```bash
git checkout -b refactor/c-pr2-align-declarations
```

- [ ] **Step 2: 三文件 package 行改为 `chromahub.rhythm.app.navigation`**（LocalNavigation 原 `features.local.presentation.navigation`；MainScreen/RhythmNavigation 原 `shared.presentation.navigation`）

- [ ] **Step 3: 更新 import（同包冗余删除）**

```bash
rg -n "import chromahub\.rhythm\.app\.(shared\.presentation\.navigation\.|features\.local\.presentation\.navigation\.)" -g '*.kt' -g '!**/build/**'
```

预期命中 RhythmNavigation.kt 与 MainActivity.kt 的 import 行（可能还有三文件自身的历史 import）：跨包引用改为新 FQCN；同在新包内的引用删除 import 行。

- [ ] **Step 4: 复核旧包前缀归零 + 快检 + Commit**

```bash
rg -n "import chromahub\.rhythm\.app\.(shared\.presentation\.navigation|features\.local\.presentation\.navigation)\." -g '*.kt' -g '!**/build/**'   # 预期 0（旧包若仍有对位文件，其声明不动，只查 import 行）
./gradlew :app:compileDebugKotlin
git add -A && git commit -m "refactor: align app navigation declarations with location"
```

### Task 7: onboarding 簇声明就位（3 文件）

**Files:** Modify: `app/src/main/java/chromahub/rhythm/app/onboarding/OnboardingScreen.kt`（原 `features.local.presentation.screens`）、`OnboardingModels.kt`、`StreamingProviderChoice.kt`（原 `features.local.presentation.screens.onboarding`）

- [ ] **Step 1: 三文件 package 行改为 `chromahub.rhythm.app.onboarding`**
- [ ] **Step 2: 更新 import**：`rg -n "chromahub\.rhythm\.app\.features\.local\.presentation\.screens\.onboarding|import chromahub\.rhythm\.app\.features\.local\.presentation\.screens\.OnboardingScreen" -g '*.kt' -g '!**/build/**'` → 预期命中 OnboardingScreen.kt（自身 import 模型，转同包后删除）、MainActivity.kt、`app/src/main/java/chromahub/rhythm/app/shared/presentation/components/PermissionHandler.kt`；同法改/删。
- [ ] **Step 3: 旧前缀归零复核 + 快检 + Commit** `refactor: align onboarding declarations with location`

### Task 8: festive 簇声明就位（2 文件）

**Files:** Modify: `app/src/main/java/chromahub/rhythm/app/shared/presentation/theme/festive/FestiveOverlay.kt`、`FestiveThemeExamples.kt`（原 `core.designsystem.theme.festive`）

- [ ] **Step 1: 两文件 package 行改为 `chromahub.rhythm.app.shared.presentation.theme.festive`**
- [ ] **Step 2: 更新 import**：`rg -n "chromahub\.rhythm\.app\.core\.designsystem\.theme\.festive\.(FestiveOverlay|FestiveThemeExamples)" -g '*.kt' -g '!**/build/**'` → 预期命中 HomeScreen.kt、`feature/settings/.../FestiveSettingsScreen.kt`、MainActivity.kt、FestiveThemeExamples.kt（同包冗余删除）。注：若 :core:designsystem 自身存在对位正确的同包文件，其 FQCN 不变、不受影响。
- [ ] **Step 3: 旧前缀归零复核 + 快检 + Commit** `refactor: align festive theme declarations with location`

### Task 9: utils 包清零（3 文件 + 31 处 import）

**Files:**
- Modify: `app/src/main/java/chromahub/rhythm/app/util/ProfileImportExportUtils.kt`、`core/media/src/main/kotlin/chromahub/rhythm/app/util/StatusBroadcaster.kt`（→ `chromahub.rhythm.app.util`）、`core/designsystem/src/main/kotlin/chromahub/rhythm/app/core/designsystem/utils/FontLoader.kt`（→ `chromahub.rhythm.app.core.designsystem.utils`）

**Interfaces:**
- Produces: 包 `chromahub.rhythm.app.utils` 全仓消失。

- [ ] **Step 1: 三文件 package 行改为目录包**（见上；`ProfileImportExportUtils` 零外部引用，仅改声明）
- [ ] **Step 2: 整行精确替换 import（Review Focus 2）**

```bash
rg -l "import chromahub\.rhythm\.app\.utils\.FontLoader" -g '*.kt' -g '!**/build/**' \
| xargs sed -i 's/^import chromahub\.rhythm\.app\.utils\.FontLoader$/import chromahub.rhythm.app.core.designsystem.utils.FontLoader/'   # 预期 30 文件
rg -l "import chromahub\.rhythm\.app\.utils\.StatusBroadcaster" -g '*.kt' -g '!**/build/**' \
| xargs sed -i 's/^import chromahub\.rhythm\.app\.utils\.StatusBroadcaster$/import chromahub.rhythm.app.util.StatusBroadcaster/'        # 预期 1 文件
```

- [ ] **Step 3: 包归零复核 + 快检 + Commit**

```bash
rg -n "chromahub\.rhythm\.app\.utils" -g '*.kt' -g '!**/build/**'   # 预期 0
./gradlew :app:compileDebugKotlin
git add -A && git commit -m "refactor: eliminate chromahub.rhythm.app.utils package"
```

### Task 10: PR-2 门禁 + AGENTS.md 收尾

- [ ] **Step 1: 错位扫描**——预期恰好 1 条：FakeUri（declared `android.net` / dir `net`）。
- [ ] **Step 2: 全部 JVM 单测**（Task 5 Step 2 原样命令）。
- [ ] **Step 3: R8 闸门**：`./gradlew assembleDebug assembleRelease`。
- [ ] **Step 4: AGENTS.md §4 更新**——将「所有模块 namespace 相同,且搬移过的文件保留原 package:**import 按文件内 package 声明写,不要按目录或模块猜**(构建不拦重复包名)」改为：

```markdown
- 所有模块 namespace 相同；包声明与源码目录一致（2026-11 归位完成）：**import 即可定位文件位置**；`android.uniquePackageNames=false` 维持，新增文件按所在目录写 package（构建不拦重复包名）。
```

- [ ] **Step 5: Commit** `docs: update AGENTS.md package guidance after alignment`

### Task 11: push + PR 创建（需用户明示后执行）

- [ ] **Step 1: 向用户呈报两分支 commit 列表与门禁结果，获明确同意后**：`git push -u origin refactor/c-pr1-align-files refactor/c-pr2-align-declarations`。
- [ ] **Step 2: `gh pr create`**：PR-1 base `main` ← `refactor/c-pr1-align-files`；PR-2 base `refactor/c-pr1-align-files` ← `refactor/c-pr2-align-declarations`（stacked）。描述中链接设计稿与本计划。

---

## 附录：错位扫描脚本（Task 5 / Task 10 用）

```bash
python3 -I - <<'EOF'
import os, re
root = "/home/qlzhj/项目/AndroidStudioProjects/Rhythm"
pat = re.compile(r'^\s*package\s+([\w.]+)', re.M)
bad = []
for dp, dn, fn in os.walk(root):
    dn[:] = [d for d in dn if d not in {".git", "build", ".gradle", ".idea", ".zcode", ".superpowers"}]
    for f in fn:
        if not f.endswith(".kt"): continue
        p = os.path.join(dp, f)
        m = pat.search(open(p, encoding="utf-8", errors="replace").read(8192))
        if not m: continue
        mm = re.search(r"src/(?:main|test|androidTest)/(?:java|kotlin)/", p)
        if not mm: continue
        der = p[mm.end():p.rfind(os.sep)].replace(os.sep, ".")
        if m.group(1) != der: bad.append((os.path.relpath(p, root), m.group(1), der))
print(len(bad))
for b in sorted(bad): print(b)
EOF
```

预期：PR-1 前 32 条 → Task 5 后 12 条（附录 A 的 11 文件 + FakeUri）→ Task 10 后 1 条（FakeUri）。
