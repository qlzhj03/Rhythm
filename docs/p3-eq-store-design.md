# P3 EQ store 设计稿（B1 方案 B 立项，2026-11-08）

## 1. 背景与目标

B1 PR-8/PR-9 把 EQ 的**对外缝隙**建好了（`AudioEffectsController`/`AudioDeviceController`，:core:media，34 成员，由 MusicViewModel 实现、EqualizerScreen 消费），但**状态与逻辑本体**仍内联在 MusicViewModel（10,818 行）里——即 b1 设计稿否决后置的「方案 B：EQ 域状态 store 真抽取」。持久化半边已在 B2-3 落到 `AudioEqSettingsSection`（511 行）。

本专项目标：EQ/DSP 域的**运行时状态 + 业务逻辑**从 MusicViewModel 抽成独立 controller，VM 委托。预期收益是**所有权收敛与一致性修复**，不是体量削减（可外移约 600 行 / 10,818）。

## 2. 现状事实（2026-11-08 勘测）

### 2.1 MusicViewModel 内的 EQ/DSP 资产（feature/local，MusicViewModel.kt）

| 组 | 位置 | 内容 |
| :-- | :-- | :-- |
| 状态透传 | 292–305、272–278 | 16+ 个 `= appSettings.X` StateFlow 直通（equalizer*/bassBoost*/virtualizer*/monoAudio/customAutoEQProfiles/pinnedAutoEQProfiles/…） |
| 本地可变状态 | 251–254、306–317 | `_autoEQProfiles/_autoEQLoading`（源自 AutoEQManager.loadProfiles）、`lastHeadphoneAutoEQProfile`、`_spatializationStatus`、`_isBassBoostAvailable`（**唯一 divergence 点**，见 §2.3）、`audioSessionId` get |
| 写设置 + 发 Intent | 10205–10356、10377–10404、8760–8816 | setEqualizerEnabled/BandLevel/BassBoost/Virtualizer/MonoAudio/applyEqualizerPreset 等：先写 AppSettings，再 `ServiceStartUtils.startServiceSafely` 发 ACTION_SET_* Intent |
| AutoEQ 族 | 10408–10730 | load/search/apply/getRecommended/save/delete/pin/assign/dismiss（apply 内含硬编码 300ms 延迟，:10469） |
| 设备族（AudioDeviceController） | 10487–10548 | save/delete/setActive/getActive/findMatchingUserDevice |
| init 块反应式胶水 | 1453–1499 | 收集 customAutoEQProfiles → autoEQManager.setCustomProfiles；收集 audioDeviceManager.currentDevice → speaker bypass + 耳机档恢复 |

### 2.2 引擎通道（:core:media，本轮**零改动**）

MediaPlaybackService 收 EQ 变更有三条既有通道，全部保留：
1. start-service Intent（onStartCommand :1925–1997 处理 7 个 ACTION_SET_*/APPLY_*）；
2. 直读 AppSettings 单例（:589 取同一实例；`loadSavedAudioEffects` :3502–3596、`initializeAudioEffectsInternal` :3458–3500 内联构造 `android.media.audiofx.Equalizer`）；
3. 引擎内响应式收集（ReplayGain combine :1222–1236；TransitionController 收集 crossfade :223–246；RhythmPlayerEngine 应用期直读 :438–449）。

服务还会**回写** AppSettings（setBassBoostAvailable，:608/616/3393/3397）。

### 2.3 已核实的不一致（本专项要修的行为债）

1. **isBassBoostAvailable divergence**：服务写 AppSettings（:608/616/3393/3397），但 VM 的 `_isBassBoostAvailable` 只在 EqualizerScreen 调 `updateBassBoostAvailability()`（MusicViewModel.kt:316–317、10300–10302）时手动刷新——读 VM 流与读 AppSettings 流可能不一致。
2. **PlaybackBottomSheet 混合路径**（PlaybackBottomSheet.kt:332–345）：同一交互块里既调 `musicViewModel.setEqualizerEnabled`（接口）又直写 `appSettings.setAudioNormalization/setReplayGain`（绕过 VM）。
3. **ReplayGainSettingsScreen 完全绕过 VM**（直写 appSettings :124–257）——与 #2 同根： setters 有两条等价通路。
4. **isAudioOffloadActive 派生流落在 AppSettings facade 本体**（AppSettings.kt:1611–1612, 1670），未随 B2-3 进 section。

### 2.4 模块依赖事实

- `:core:media` → `api(":core:data")`（引擎可见 AppSettings）；`:feature:local` → `implementation(":core:media")`；`:core:ui` 两者都可见。
- AutoEQManager、AudioDeviceManager 均在 `:core:media`（util/），由 MusicViewModel 构造（:242、:250）。
- `:core:common` 无 EQ/DSP 接口；`ServiceStartUtils.startServiceSafely` 在 :core:common。
- `AudioEffectsController`/`AudioDeviceController` 是 UI 侧缝隙（服务不实现不消费）。

## 3. 方案

**裁定：方案 B——`EqDspController` 落 `:core:media`，由 MusicViewModel 构造并委托（不新增 Hilt 组件）。**

落 :core:media 而非 :core:data 的理由：AutoEQManager/AudioDeviceManager 已在 :core:media，:core:data 不能反向依赖；控制器与设备管理器同模块，B3+ 引擎复用成为可能。不进 Hilt 的理由：生命周期与 MusicViewModel 同步（autoEQ 装载是 entry 级状态）、避免引入应用级 CoroutineScope 裁定、B1 已有「VM 内构造 manager」先例（:242/:250）。

### 形态

```
:core:media  infrastructure/equalizer/EqDspController.kt（新）
  ├─ 构造参数: context, appSettings: AppSettings（沿用 VM 现状取单例）
  ├─ 自持: autoEQManager, audioDeviceManager, _autoEQProfiles/_autoEQLoading,
  │        _spatializationStatus, lastHeadphoneAutoEQProfile
  ├─ 实现: AudioEffectsController + AudioDeviceController 全部成员（从 VM 平移）
  ├─ init 块反应式胶水整体平移（customProfiles 订阅 + 设备→档位恢复）
  └─ isBassBoostAvailable: 直通 appSettings 流（divergence 修复，见 PR-P3-3）
```

MusicViewModel：构造 `private val eqDsp = EqDspController(...)`；AudioEffectsController/AudioDeviceController 实现逐成员改为 `eqDsp.xxx` 转发（PR-P3-1），随后删接口实现、暴露 `val audioEffects: AudioEffectsController get() = eqDsp` + `val audioDevices: AudioDeviceController get() = eqDsp`（PR-P3-2）。EqualizerScreen 底座参数（`AudioEffectsController`/`AudioDeviceController`）签名不变——B1 PR-9 的缝隙设计正是为此预留。

### PR 切片

| PR | 内容 | 门禁 |
| :-- | :-- | :-- |
| PR-P3-1 | EqDspController 骨架 + 成员/胶水平移；VM 全量转发（对外零变化） | 每步 compileDebugKotlin；收口 assembleDebug + 8 模块单测 |
| PR-P3-2 | VM 撤销两个接口实现，暴露 interface 引用；:app 两处注册点（LocalNavigation.kt:2266–2271、2437–2442）改传 `viewModel.audioEffects/audioDevices`；PlaybackBottomSheet 混合路径统一走 `audioEffects` 接口 | 同上 + 树差异断言（VM 净减 ≈600 行） |
| PR-P3-3 | isBassBoostAvailable 直通 appSettings 流（删 VM 手动刷新通道，EqualizerScreen 的 updateBassBoostAvailability 调用点一并清）；`updateSpatializationStatus` 随迁 controller | 同上 + 真机 EQ 冒烟 |
| PR-P3-4 | EqualizerScreen.kt（1,478 行）从 :feature:settings 归位 `:feature:local`（screens/player/），包名同步修正（C 阶段收尾）；依赖已满足（local→media ✓、local 无 settings 依赖 ✓） | 同上 + `git grep` 包名零残留 |

**isAudioOffloadActive 派生流归位**：依赖 facade 内跨域状态，B2-3 已验证此类移动需 updateDerived 胶水，收益低——**顺延 B3**（键空间/存储迁移时一并处理），不在本专项。

### 不动清单

- 引擎三通道（Intent / 直读 / 引擎内收集）零改动；服务回写 AppSettings 不变
- AppSettings 公共 API 零变化（EQ 域委托成员原样保留——外部仍可 `appSettings.equalizerEnabled` 直读）
- PlaybackSettingsController（gapless/skip-silence/crossfade 族）留守 MusicViewModel——播放行为域，非 EQ/DSP
- ReplayGainSettingsScreen 的直写通路保留（公共 API 不变），混合路径只在 PlaybackBottomSheet 层收敛
- applyAutoEQProfile 的 300ms 延迟原样平移（看似 hack，实为等 audioSession 就绪的既有取舍）

## 4. 与 P4/B3 的边界

- EqualizerScreen 归位放在 P3（P4-3 路由拆解只做剪切，若先动 P4 会两次触碰同一文件）。
- 存储 backend（SharedPreferences→DataStore）是 B3 的事；P3 只动状态所有权，不动存储。
