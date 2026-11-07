# B2 专项：AppSettings 瘦身（死重清理 + 行为外迁 + 分域重组 + 启动预热）

> 状态：**草稿（待批准）**（2026-11-08）。上游：`docs/multi-module-refactor-plan.md`（"AppSettings 整体搬入 :core:data（本次不拆 DataStore）"、"DataStore 本期仅引入依赖与骨架，AppSettings 拆分另立专项"）。
> **Scope 修正（本设计核心裁定，需批准）**：蓝图原列 B2 含 DataStore 迁移；勘测后本设计将 DataStore 推迟至 **B3**（理由见 §1.2），B2 专注「瘦身」——不改存储、不改 API、不改消费方。

## 1. 意图与动机

### 1.1 熵增点（勘测实测，2026-11-08）

- **API 失控**：`AppSettings.kt` 6,973 行、公共面 **854 成员**（386 `StateFlow` + 378 `fun setX` + ~90 业务 fun）；每个新设置项默认堆进来。
- **四重职责混杂**：设置存储（本职）+ WorkManager 调度（:4582-4720、:3996-4008）+ 备份/恢复业务逻辑（:4862-5250，~400 行）+ 歌词偏好 CSV 导入导出（:5811-5920）。另有 5 处内联键迁移、Gson 序列化、`NetworkSettingsProvider` 缝隙实现、同文件 6+ 个公共兄弟类型（CrashLogEntry/AlbumViewType 等）。
- **启动主线程 I/O**：`RhythmApplication.onCreate`（:73）主线程 `getInstance` → ~390 个属性初始化器同步读 SharedPreferences；macrobenchmark baseline profile 明确把 "AppSettings init" 列为覆盖路径。
- **死重**：**~37 个公共成员零外部消费**（含整组 rhythmAura* 废弃别名、sleep timer 三件套——:core:media 已有 SleepTimerController 缝隙取代之、一批 home/player 定制项）。抽验 6/6 确认。

### 1.2 为什么 DataStore 推迟至 B3（Scope 修正理由）

勘测发现**朴素 DataStore 迁移在本仓库当前形态下不可行**：

1. **354/386 成员被外部同步 `.value` 消费**，其中包括 `RhythmPlayerEngine` 音频热路径（EQ band levels、播放速度等）——DataStore 的 suspend-only 读模型要么迫使 190 个文件改异步（churn + 回归面巨大），要么引入「hydration 完成前 `.value` 返回默认值」的冷启动窗口（音频热路径在窗口内读到错误 EQ = 行为回归，触碰 B1 以来的反目标红线）。
2. SharedPreferences 本身工作正常、键稳定；真正的启动痛点（主线程等 XML 加载）可用**后台预热**零 API 变化地解决（§4.4），收益接近而风险≈0。
3. B2 的死重清理 + 行为外迁 + 分域拆分**独立于存储选型**，且会把未来 B3 必须迁移的面从 6,973 行/854 成员缩到 ~4k 行/~817 成员——B3 到来时迁移对象更小、域界更清晰。

故 B2 交付瘦身本体，DataStore（含 SharedPreferencesMigration、hydration 策略、热路径成员的白名单直读设计）立 **B3 专项**，前置问题清单见 §8。

## 2. 已核验事实（勘测摘要）

| 维度 | 实测 |
| :-- | :-- |
| 访问形态 | 纯手写单例（非 Hilt）：122 处 `getInstance` + 67 处手穿参数，**零 `@Inject`**；134 文件引用 |
| 消费分布 | `:feature:settings` 触 244 个不同成员、`:feature:local` 241（各 ~63%）；audio/eq（5 模块消费）、home/player 定制（4）、guard/stats（5）、library/scan（5）全是高扇出——**无大块干净可分区** |
| 存储 | 单 `SharedPreferences("rhythm_preferences")`；Gson 序列化复杂值；443 处 `edit{}`（apply 语义）+ 1 处显式 `commit()` |
| 行为块 | WorkManager 调度 ~140 行；backup/restore ~400 行；歌词 CSV ~110 行；guard 策略常量表 |
| 迁移 | 5 处内联键迁移（artwork、audioEngine v2、lyrics online_only、thumbStyle、restore 时 rhythm_aura_→rhythm_guard_）——**必须随存储原样保留** |
| 备份现状 | `BackupRestoreManager`（:core:data）已有 IO 版 `createBackupPayload/restoreFromBackupPayload`，但 AppSettings 内旧同步实现仍在，直接调用方 4 处（OnboardingScreen、BackupWorker、BackupRestoreSettingsScreen×1）+ 歌词 CSV 2 处（LyricsSettingsScreen） |
| DataStore | catalog 已入（datastore 1.3.0），:core:data 未引用；仓内唯一在用是 :feature:widget 的 Glance 状态（非设置） |

## 3. 目标 / Non-goals / 反目标

**目标**
1. 死重清零（~37 成员，逐成员 builder-check 后删除）。
2. 行为外迁：WorkManager 调度与歌词 CSV 移出 AppSettings，AppSettings 收敛为「纯设置存储 + 迁移」（backup/restore 经实施裁定留守，见 §4.2 补记）。
3. 分域文件拆分：实现按域拆为 ~10 个 section 文件（:core:data 内），公共 API 一行不改。
4. 启动后台预热，消除主线程等 XML 加载（macrobenchmark startup 指标对照验证）。

**Non-goals**
- 公共 API 面不缩（386 vals + 在用 funs 全部保留在 AppSettings 上）；不改任何键名；不动 134 个消费文件；AGENTS.md「设置唯一来源是 AppSettings」规则不变。
- DataStore 迁移（→B3）；`NetworkSettingsProvider` 缝隙不动；兄弟公共类型位置随域文件自然调整，不做独立搬家专项。

**反目标（出现即失败）**
- 任何用户可见行为变化（含冷启动窗口内设置值语义、备份/恢复产物格式、worker 调度时机）。
- 引入第二条 SharedPreferences/DataStore 通道。
- 分域后出现新的跨域耦合（跨域反应只能经 facade 既有 `updateDerivedSettings` 等入口）。

## 4. 设计

### 4.1 B2-1 死重清理

- 37 成员清单以勘测报告为准，**逐成员独立核验**（`grep -rn "\.member\b"` 全仓排除 AppSettings.kt 自身 = 0，含反射/字符串引用排查）后才删除；`_x` backing flow、setter、键常量一并删。
- **键不清理**：旧安装残留键无害且不可达，避免任何迁移面。
- 废弃别名（rhythmAura* 6 个）与被 SleepTimerController 取代的 sleep timer 三件套优先——它们已有替代缝隙，删除最安全。

### 4.2 B2-2 行为外迁

| 块 | 去处 | 调用方处置 |
| :-- | :-- | :-- |
| WorkManager 调度（~140 行 + set 路径触发点） | 新文件 `SettingsJobScheduler`（:core:data），沿用 `AppWorkerRegistry` 缝隙 | AppSettings 内部改调 scheduler，公共 API 不变（✅ 已实施） |
| ~~backup/restore 体~~ | **实施裁定：取消外迁，留守 AppSettings**（见下） | — |
**§4.2 实施裁定补记（2026-11-08）**：通读 backup/restore 体后推翻本表第二行的前提——`createBackup` 实为 prefs↔JSON 的**存储编解码**（读 `prefs.all` + 分节过滤 + Gson 类型标注，零流访问），`restoreFromBackup` 直写 390 个私有 backing 并在中段触发派生更新，本质是「设置状态恢复」的存储内聚逻辑，与键迁移同级，不属于「业务逻辑」。外迁需要（a）行为风险的 setter 改写（setter 带副作用：调度/重扫/归一化）或（b）390 个 backing 可见性放宽——均违背零行为变化与内聚目标。裁定：**二者留守 AppSettings**（存储职责的一部分），B2-2 以 workers + CSV 外迁收口。`triggerImmediateUpdateCheck` 实施中经核验为零调用方死代码，随块删除（未迁移）。

| 歌词 CSV（~110 行） | 新文件 `LyricsPreferenceIo`（:core:data，纯编解码） | AppSettings 保留薄委托，backing 写入留在原处（✅ 已实施） |
| guard 策略常量、`getRhythmGuardPolicyBands` | 随 GuardStats 域文件（§4.3） | 经 AppSettings 委托，API 不变 |

键迁移逻辑（5 处）**留在 AppSettings**——迁移属于存储职责。`NetworkSettingsProvider` 实现留在 facade。

### 4.3 B2-3 分域文件拆分（API 不变）

**§4.3 实施方式裁定补记（2026-11-08）**：§4.2 裁定 restoreFromBackup 留守后，原定的「internal section 委托类」形态代价剧增——restore 直写 390 个私有 backing，委托类要求这些写点全部改写为跨对象访问（390 处机械改写 + 回归面）。改用**同包扩展拆分**：backing（改 internal）与 prefs/KEY 留守 AppSettings，域内 setter/域 fun/StateFlow 暴露以 **扩展属性（`val AppSettings.x get() = _x.asStateFlow()`）与扩展函数**形式迁入 `settings/` 目录的域文件——调用方语法零变化、restore 零改写、公共 API 不变。接口 override 成员（NetworkSettingsProvider）与嵌套类型留守类内。逐域分批提交。原「section 委托类 + 状态封装」推迟到 B3（DataStore 迁移时状态本就要重构）。



- 新目录 `core/data/.../shared/data/model/settings/`（包名随目录，C 阶段规则）；每域一个 `internal class XxxSettingsSection(ctx: SectionContext)`，`SectionContext` 持 prefs/Gson/appContext/scope 与跨域只读引用。
- AppSettings 构造全部 section 并委托——386 个 vals 的 getter/setter 逐个转委托调用（机械改写，编译驱动）；`updateDerivedSettings`、`commit()`、5 处迁移、`NetworkSettingsProvider` 留 facade。
- 域清单（按勘测聚类，~10 文件）：**CustomizationSections**（home/player/miniplayer/gesture/expressive，~89 props，可拆 2 文件）、**AudioEqSection**（~45）、**LibrarySection**（~42，含 view types/folders）、**GuardStatsSection**（~39，含 listening stats 与 guard 策略常量）、**PlaybackQueueSection**（~28）、**LyricsSection**（~27）、**ThemeSection**（~24）、**StreamingModeSection**（~17，含 appMode/offline）、**CollectionsSection**（playlists/默认列表 ~16）、**PlatformSection**（notifications/updates/backup 键/onboarding/cache/widget/misc ~35，widget 11 成员虽近单消费者但体量小不单列）。
- 兄弟公共类型随域文件走（AlbumViewType → LibrarySection 文件等）。
- 跨域反应盘点：勘测仅确认 `updateDerivedSettings` 与 restore 路径两处，实施第一步做全量盘点并登记（B1 §4.0 式 builder check），若发现密集跨域反应则该反应留在 facade 编排。

### 4.4 B2-4 启动预热

- `RhythmApplication.onCreate` 起点起 daemon 线程先 touch `rhythm_preferences`（任意 `getString`），触发并等待 XML 异步加载完成后主线程 `getInstance` 的 ~390 次同步读不再阻塞。
- 零 API 变化、零语义变化（SharedPreferences 内部锁保证读写正确性；预热只是提前了加载等待点）。
- 验证：macrobenchmark startup 指标前后对照（baseline profile 已覆盖该路径）。

## 5. 行为裁定

- 全程零用户可见行为变化；备份产物 JSON 结构不变（消费方 BackupRestoreManager 两路径兼容旧格式）。
- 预热只移动「谁等待磁盘加载」，不改变任何设置的读取时序语义。
- 删除死重成员 = 删除不可达代码路径（builder-check 证明不可达），非行为变更。

## 6. 风险与复审焦点

| 风险 | 缓解 |
| :-- | :-- |
| 死重误判（反射/字符串取值） | 逐成员独立 builder check + 全仓 grep 双确认 + 全量门禁 |
| 分域改写 386 vals 的机械错误 | 逐 section 分批 commit，每批编译 + 全量单测；rename/搬移保高相似度 |
| 跨域反应遗漏 | §4.3 首任务全量盘点 updateDerivedSettings/restore 触发的联动 |
| 外迁改变备份/worker 时序 | BackupWorker/OnboardingScreen 调用路径逐个走查 + 真机冒烟备份/恢复 |
| git 历史切割 | 分域文件用 git mv + 逐段搬移，保留 rename 相似度 |

## 7. PR 切片与门禁

| PR | 内容 | 门禁 |
| :-- | :-- | :-- |
| PR-B2-1 | 死重清理（可分 2-3 个 commit：别名组 / sleep-timer 组 / 其余） | 每步 compileDebugKotlin；PR 收口 assembleDebug + 全量 JVM 单测 |
| PR-B2-2 | 行为外迁（workers / backup / CSV 三个 commit） | 同上 + 真机备份/恢复冒烟 |
| PR-B2-3 | 分域拆分（按域分批 commit，每批 2-3 域） | 同上 |
| PR-B2-4 | 启动预热 + 文档收口（AGENTS.md 不需改——「唯一来源」规则不变；设计稿标收口） | 同上 + macrobenchmark startup 对照（可选） |

分支：`refactor/b2-pr1-deadweight` → `b2-pr2-behavior-extraction` → `b2-pr3-domain-split` → `b2-pr4-startup-warmup`（stacked）。文档随 PR-B2-1 入库。

预期效果：AppSettings.kt 6,973 → ~1,500 行以内（facade），总公共成员 854 → ~817；职责从 4 重收敛为 1 重；启动主线程 I/O 消除。

## 8. B3 预告（DataStore 迁移前置问题清单）

1. 热路径成员白名单：哪些成员必须保持同步读语义（EQ/playback 速度等）。
2. hydration 策略：`SharedPreferencesMigration` + 阻塞首读 vs 默认值窗口的产品裁定。
3. 迁移完成后的 API 形态：维持 facade 还是按域开放 store API（B1 窄接口模式）。
4. 键空间清理（B2 残留键的最终回收）。
