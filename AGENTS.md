# AGENTS.md — Rhythm

Android 音乐播放器:本地曲库 + 自托管流媒体(Subsonic / Navidrome / Jellyfin、网易云)+ 歌词 + 小组件 + 均衡器,Material 3 Expressive,单 Activity Compose 架构。

本文件只写「读代码无法可靠推断」的**约定与红线**,保持克制:具体文件、类、方法用搜索获得,不在此罗列。开发进度与重构历史见 `docs/` 与 git log;`wiki/` 已过时,勿参照。仓库无 CI,验证靠本地命令。事实来源:源码 > Version Catalog > 本文件。

## 1. 命令

| 用途 | 命令 |
| :-- | :-- |
| 最快编译检查 | `./gradlew :app:compileDebugKotlin` |
| 安装 debug | `./gradlew :app:installDebug` |
| 全部 JVM 单测 | `./gradlew :app:testDebugUnitTest :core:common:testDebugUnitTest :core:media:testDebugUnitTest :core:ui:testDebugUnitTest :core:designsystem:testDebugUnitTest :core:data:testDebugUnitTest :feature:local:testDebugUnitTest :feature:streaming:testDebugUnitTest` |
| debug / release 包 | `./gradlew assembleDebug` / `assembleRelease`(release 仅发布前) |
| strings 复数校验 | `python3 scripts/check_plural_specifiers.py` |
| 提交前最小集合 | assembleDebug + 全部 JVM 单测 |

- 禁止裸 `./gradlew test`(连带 release / benchmark 变体);测试始终按模块指定。
- APK 仅含 `arm64-v8a`,x86_64 模拟器装不上;无 androidTest 源集,不新建。
- lint / ktlint / detekt 刻意禁用:不恢复,不以其输出作为改动依据。
- `gradle.properties` 的内存 / 并发调优不要改。

## 2. 红线

- **凭据**:`local.properties`、`keystore.properties`、`*.jks` 内容不提交、不输出;密钥与令牌(如 Apple Music fallback token)不复制到任何新位置。
- **依赖**:版本只进 Version Catalog,不在模块脚本写死;不自行升级工具链与 Compose 体系;catalog 里显式钉住的版本是刻意的,勿解除。
- **插件**:不加 `kotlin-android`——AGP 内建 Kotlin,加了直接构建失败。
- **结构**:不加新 Activity / Fragment / product flavor / 语言目录 / 其他 DI 框架。
- **对外契约**:manifest 的 exported 组件与 intent-filter 是对外协议,cleartext 放行是自托管场景的既定取舍,均勿收紧、勿改。
- **生成物**:不改构建产物与生成代码;新增 CI 配置或运行发布脚本前先询问。
- **Git / Gradle**:未经明确要求不 commit / push / tag;只用 `./gradlew` wrapper。
- **看似死代码勿删**:BuildConfig 功能开关、flavor / nightly / 日期字段等,部分 UI 在读。

## 3. 架构约定

- 分层:Screen → ViewModel → Repository → 数据源;ViewModel 不直接碰 SQL、MediaStore、HTTP。
- 依赖方向:presentation → data / domain;data 不依赖 presentation 与 Compose;**feature 模块之间禁止互相依赖**,既有跨 feature 引用是已知债务(settings→streaming / local→streaming;B1 PR-9 已解除 settings→local,D1 已解除 settings→widget),勿新增。
- 导航:字符串路由,新页面 = 新增 Composable route 并在 `:app` 导航装配处接线;禁止用序列化对象定义路由。
- DI:全仓 Hilt,禁止引入其他 DI 框架;UI 取 ViewModel 一律 `hiltViewModel()`——`viewModel()` 不走 Hilt 工厂,多参构造必崩。
- 状态:`StateFlow` 单向流(私有 Mutable + `asStateFlow()` 暴露,派生用 `stateIn`),UI 统一 `collectAsState()`;非 MVI,无 Intent / Effect 层,屏幕直接调 ViewModel 方法。
- 设置唯一来源是 `AppSettings`,勿另起 SharedPreferences 通道。
- 外部 API 功能走「编译期开关 → 全局开关 → 单项开关」三层门控;对应 service 按开关懒加载、可空,调用必须判空。
- ViewModel 只持 Application 级 context,禁止 Activity / View / Fragment。

## 4. 模块

| 模块 | 职责 |
| :-- | :-- |
| `:app` | 组合根:Activity、导航装配、Application、引导页 |
| `:core:model` | 纯领域模型 |
| `:core:common` | 纯工具与缝隙接口 |
| `:core:designsystem` | 资源中心:strings、theme、icons、基础组件 |
| `:core:network` | 网络客户端与各 API service |
| `:core:data` | Room、AppSettings、仓储 |
| `:core:media` | 播放服务与引擎、DSP / EQ / ReplayGain、媒体工具 |
| `:core:ui` | 跨 feature 共享 UI 组件 |
| `:feature:local` | 本地音乐 + 播放器 UI |
| `:feature:streaming` | 流媒体 provider 与 UI(不依赖其他 feature) |
| `:feature:settings` | 设置页 |
| `:feature:widget` | Glance 小组件与磁贴 |

另有 `:macrobenchmark` 与 `build-logic/`(约定插件)。

- 所有模块 namespace 相同;包声明与源码目录一致(2026-11 归位完成):**import 即可定位文件位置**;`android.uniquePackageNames=false` 维持,新增文件按所在目录写 package(构建不拦重复包名)。
- 代码放哪:跨 feature 复用的组件进 `:core:ui`(设置/播放列表对话框、BottomSheet、PlayingEqIcon 等已随 B1 PR-8/9 下沉),播放器域组件进 local 的播放器共享目录,设置页组件进 `:feature:settings`;勿再往各 feature 的 presentation/components 扩散。
- 既有数千行级大文件(ViewModel、大屏、工具类)不追加功能,新功能新建文件 / ViewModel;UseCase 不强制,仅多处复用才抽。
- 新增库模块一律用 `build-logic` 约定插件,勿复制 `:app` 的手写配置。

## 5. 编码约定

- 风格:无格式化 / lint 门禁,以邻近文件为准,勿全量重排;import 禁通配符;不加版权头。
- Compose:首参 `modifier`;点击目标 ≥48dp 并给 contentDescription;列表项给稳定 key;Composable 不做业务逻辑与 IO,昂贵对象必须 `remember`,副作用只走既有副作用 API;状态模型不暴露可变集合。
- 资源:颜色 / 字号 / 形状取自主题与项目 token,不硬编码;图标走项目图标体系(variable font),禁 material-icons-extended;用户可见字符串中英双语同步维护,涉及复数后跑校验脚本。
- Kotlin:新增代码不用 `!!`、`GlobalScope`、空 `catch`、裸异常做流程控制;非致命异常记入统一 CrashReporter,错误文案不得直接用 `e.message`;网络日志走统一入口并保持脱敏。
- 协程:Dispatcher 直接引用,不做注入抽象;主线程不做 IO。
- Material3 Experimental API 已全局 opt-in,新文件无需 `@OptIn`;新启用 Experimental API 时在构建配置补 opt-in。

## 6. 测试

- 栈:JUnit4 + MockWebServer3 + 真 org.json;测试与源码同包放对应模块的 `src/test/java`。
- 禁引入:MockK、Turbine、coroutines-test、JUnit 5、Robolectric、截图测试框架。
- 不打真实网络 / 数据库(HTTP 打桩),不用 `Thread.sleep` 等待异步;可测逻辑优先抽顶层纯函数。
- android.jar stub 返回默认值是预期配置,勿因此引入 Robolectric。

## 7. 发布与安全

- `minSdk` / `targetSdk` / `compileSdk` / Java target / release 混淆与资源收缩均不修改(值见 `app/build.gradle.kts`)。
- 版本号由发布脚本维护,AI 不自行 bump;临时构建用 `-P` 覆盖参数。
- 签名读项目根目录 `keystore.properties`,缺失时回退 debug 签名;禁止打印 / 改写密钥与签名路径。
