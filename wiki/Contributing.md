> ℹ️ **文档状态**：本 wiki 已按 2026-11 多模块拆分与唯一包名重构后的代码更新。
> 约定与红线见根目录 [AGENTS.md](../AGENTS.md)，设计与重构历史见 [docs/](../docs/)；一切事实以源码为准。

# 为 Rhythm 做贡献

感谢你有兴趣为 Rhythm 音乐播放器做贡献！本指南帮助你入门。动手前请先读一遍根目录 [AGENTS.md](../AGENTS.md)：那里列出命令、红线与架构约定。

---

## 🤝 贡献方式

### 1. 报告 Bug

**报告之前**：查看已有问题避免重复、用最新版本复现、准备好设备与应用版本信息。

**报告内容**：
- Android 版本（例如 Android 14）
- 设备型号（例如 Pixel 7）
- Rhythm 版本（以 `app/build.gradle.kts` 为准）
- 复现步骤、预期行为与实际行为
- 必要时附上截图 / 日志（应用内「设置 → 更新与信息 → 崩溃日志」）

### 2. 提出功能建议

先搜索已有请求，并考虑是否符合 Rhythm 的理念（隐私、FOSS、简洁）；描述「想要什么、为什么有用、应如何工作」，实现思路可选。

### 3. 改进文档

修正错别字、澄清章节、补充缺失信息、更新过时内容、完善中英文案。编辑 `wiki/` 或 `docs/` 中的 `.md` 文件后提交拉取请求即可。

### 4. 代码贡献

欢迎 Bug 修复、性能改进、UI/UX 增强、重构与测试覆盖。写代码前先在 issue 中认领并讨论方案。

---

## 🔧 开发环境

### 前置条件

- **Android Studio**：兼容 AGP 9.x / Gradle 9.7.1 的最新稳定版
- **JDK 21**：Java `sourceCompatibility` / `targetCompatibility` 与 Kotlin `jvmTarget` 统一为 `JVM_21`（由 `build-logic/convention/.../AndroidCommon.kt` 注入）
- **Kotlin**：版本以 `gradle/libs.versions.toml` 为准；AGP 9.x 内建 Kotlin，**不要**加 `kotlin-android` 插件
- **Android SDK**：`compileSdk 37`、`minSdk 26`、`targetSdk 37`
- **设备**：`arm64-v8a` Android 8.0+（API 26+）；APK 仅含 `arm64-v8a`，**x86_64 模拟器装不上**

### 快速开始

```bash
git clone <仓库地址>
cd Rhythm
./gradlew :app:compileDebugKotlin   # 最快编译检查
./gradlew assembleDebug             # 提交前最小集合之一
./gradlew :app:installDebug         # 安装到已连接设备
```

完整命令见 [构建说明](Build-Instructions.md) 与 [AGENTS.md](../AGENTS.md)。

---

## 📝 代码规范

### 风格

- 无格式化 / lint 门禁，以邻近文件风格为准，**不要全量重排**
- import 禁通配符，不加版权头
- 新增代码不用 `!!`、`GlobalScope`、空 `catch`、裸异常做流程控制
- 非致命异常记入统一 CrashReporter；错误文案不得直接使用 `e.message`
- 协程 Dispatcher 直接引用，不做注入抽象；主线程不做 IO

### Kotlin

遵循 [Kotlin 官方约定](https://kotlinlang.org/docs/coding-conventions.html)：

```kotlin
class MusicPlayer {
    fun playTrack(song: Song) { /* ... */ }
}
```

### Jetpack Compose

- 首个参数是 `modifier`；点击目标 ≥48dp 并提供 `contentDescription`
- 列表项给稳定 key；状态模型不暴露可变集合
- 昂贵对象必须 `remember`；Composable 不做业务逻辑与 IO；副作用只走既有副作用 API
- 颜色 / 字号 / 形状取自主题与项目 token，不硬编码；图标走项目 variable font 体系，禁 `material-icons-extended`
- 用户可见字符串中英双语同步维护

```kotlin
@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier) { /* ... */ }
}
```

### 架构约定

- **分层**：Screen → ViewModel → Repository → 数据源；ViewModel 不直接碰 SQL、MediaStore、HTTP
- **依赖方向**：data 不依赖 presentation 与 Compose；**feature 模块之间禁止互相依赖**
- **导航**：字符串路由；新页面 = 新增 Composable route 并在 `:app` 导航装配处接线
- **DI**：全仓 Hilt；UI 取 ViewModel 一律 `hiltViewModel()`（`viewModel()` 不走 Hilt 工厂，多参构造必崩）
- **状态**：`StateFlow` 单向流（私有 `Mutable` + `asStateFlow()`，派生用 `stateIn`），UI 统一 `collectAsState()`；非 MVI，无 Intent / Effect 层
- **设置**：唯一来源是 `AppSettings`，勿另起 SharedPreferences 通道
- **外部 API**：编译期开关 → 全局开关 → 单项开关三层门控；service 懒加载、可空，调用必须判空
- **ViewModel**：只持 Application 级 context，禁止 Activity / View / Fragment
- 跨 feature 复用组件放 `:core:ui`，播放器域组件放 `:feature:local` 的播放器共享目录，设置页组件放 `:feature:settings`
- 既有数千行级大文件（ViewModel、大屏、工具类）**不追加功能**，新功能新建文件 / ViewModel

### 模块与包

每模块唯一包根（`android.uniquePackageNames=true`）：`:app` = `com.qlzhj.rhythm.app`，core / feature = `com.qlzhj.rhythm.<core|feature>.<name>`。新增文件按所在目录写 package；**import 即定位「模块 + 文件」**。

```
app/                 :app
core/{common,model,designsystem,network,data,media,ui}/
feature/{local,streaming,settings,widget}/
macrobenchmark/
build-logic/         约定插件（新增库模块必须使用，勿复制 :app 手写配置）
scripts/             校验、迁移、发布脚本
docs/                设计与重构历史
```

> `applicationId = com.qlzhj.rhythm` 与代码包根分离；自定义 action / broadcast / extra 协议串保持 `chromahub.rhythm.app.*` 字面前缀不变，勿"顺手"改名。

### 红线

- 依赖版本**只**进 Version Catalog，模块脚本不写死；不自行升级工具链与 Compose 体系；catalog 中显式钉住的版本勿解除
- 不加 `kotlin-android`、新 Activity / Fragment / product flavor / 语言目录 / 其他 DI 框架
- manifest 的 exported 组件、intent-filter 与 cleartext 放行是对外契约 / 既定取舍，勿收紧、勿改
- `minSdk` / `targetSdk` / `compileSdk` / Java target / release 混淆与资源收缩均不修改
- `gradle.properties` 的内存 / 并发调优不要改
- 不改构建产物与生成代码；看似死代码（BuildConfig 开关、flavor / nightly / 日期字段）勿删
- 凭据：`local.properties`、`keystore.properties`、`*.jks` 内容不提交、不输出；令牌不复制到新位置
- 未经明确要求不 commit / push / tag；只用 `./gradlew` wrapper

### 注释

自解释代码优先；复杂逻辑补注释；公共 API 用 KDoc。

---

## 🧪 测试

### 编写测试

- 栈：**JUnit4 + MockWebServer3 + 真 `org.json`**；测试与源码同包，放对应模块 `src/test/java` 或 `src/test/kotlin`
- **禁用**：MockK、Turbine、coroutines-test、JUnit 5、Robolectric、截图测试框架
- 不打真实网络 / 数据库（HTTP 打桩），不用 `Thread.sleep` 等待异步
- 可测逻辑优先抽顶层纯函数
- `android.jar` stub 返回默认值是预期配置（`unitTests.isReturnDefaultValues = true`），勿因此引入 Robolectric
- **无 `androidTest` 源集，不新建**

```kotlin
@Test
fun `word timed lyrics expose active word index`() {
    val parsed = parseWordByWordLyrics(sampleEnhancedLrc)
    assertTrue(parsed.isWordTimed)
}
```

### 运行测试

按模块指定任务，**禁止裸 `./gradlew test`**（会连带 release / benchmark 变体）：

```bash
./gradlew :app:testDebugUnitTest :core:common:testDebugUnitTest \
  :core:media:testDebugUnitTest :core:ui:testDebugUnitTest \
  :core:designsystem:testDebugUnitTest :core:data:testDebugUnitTest \
  :feature:local:testDebugUnitTest :feature:streaming:testDebugUnitTest
```

lint / ktlint / detekt 刻意禁用，不恢复，也不以其输出作为改动依据。仓库**无 CI**，验证靠本地命令。

---

## 📤 拉取请求流程

### 1. 创建主题分支

从最新 `main` 拉出，命名 `<type>/<slug>`（`type` 与提交类型一致）：

```bash
git switch main && git pull --ff-only
git switch -c feat/lyrics-sync-adjust
```

分支存活期间用 `git rebase origin/main` 同步上游，不要把 `main` merge 回分支。

### 2. 进行更改

- 保持提交聚焦且原子化（一个提交只做一件事）
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/v1.0.0/)：

```
<type>(<scope>): <subject>

[body]

[footer]
```

`type` 小写（`feat` / `fix` / `perf` / `refactor` / `docs` / `test` / `build` / `ci` / `chore` / `style` / `revert`），`scope` 可选，`subject` 用祈使句、≤72 字符、不以句号结尾；破坏性变更用 `type(scope)!:` + `BREAKING CHANGE:` footer。

```bash
git add <path>
git commit -m "feat(player): add lyrics sync adjustment controls"
```

> 仓库没有 `.githooks/` 钩子或 CI 校验脚本，以上规范靠提交者与评审自觉维护。

### 3. 本地验证（提交前最小集合）

```bash
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest :core:common:testDebugUnitTest \
  :core:media:testDebugUnitTest :core:ui:testDebugUnitTest \
  :core:designsystem:testDebugUnitTest :core:data:testDebugUnitTest \
  :feature:local:testDebugUnitTest :feature:streaming:testDebugUnitTest
python3 scripts/check_plural_specifiers.py
```

### 4. 发起拉取请求

- **标题**：符合 Conventional Commits（只启用 Squash 合并，标题会成为 `main` 上的提交标题）
- **描述**：做什么、为什么、怎么做；用 `Fixes #123` 关联问题
- **截图**：UI 更改必附
- **测试**：写明你执行了哪些本地验证命令及结果

### 5. 代码评审与合并

及时回应反馈并将更新推到同一分支；维护者以 **Squash and merge** 合并，保持 `main` 线性历史。合并后主题分支会被删除，请勿复用。禁止直接向 `main` 提交或强制推送。

---

## 🎨 UI/UX 贡献

- **Material 3 Expressive**：匹配既有 UI 模式与项目 token
- **无障碍**：屏幕阅读器、≥48dp 点击目标、`contentDescription`、高对比度
- **适配**：手机 / 平板 / 折叠屏姿态、横竖屏、浅色与深色模式
- **性能**：流畅动画、列表稳定 key、避免主线程 IO

---

## 🌍 语言

Rhythm 仅维护 **中文（简体）** 与 **英文**，不新增语言目录。

| 语言 | 资源文件 |
|:---|:---|
| 中文（默认，简体） | `core/designsystem/src/main/res/values/strings.xml` |
| 英文 | `core/designsystem/src/main/res/values-en/strings.xml` |

`app/src/main/res/values/` 下**没有** `strings.xml`；`:core:designsystem` 是资源中心。`core/media/src/main/res/` 另有少量模块内字符串，同样有 `values` / `values-en` 两份。

### 更新文案

1. 同步修改中英两份 `strings.xml`
2. 保持占位符不变（`%s`、`%d` 等）
3. 涉及复数时运行 `python3 scripts/check_plural_specifiers.py`（校验不引入源串没有的格式占位符）
4. 在浅色 / 深色、手机 / 平板上检查文案是否被截断

---

## 🔍 提交前检查清单

- [ ] `assembleDebug` 与全部 JVM 单测通过
- [ ] 新页面已用字符串路由并在 `:app` 装配；ViewModel 用 `hiltViewModel()` 获取
- [ ] 新增文件 package 符合所在模块唯一包根
- [ ] 依赖版本只在 `gradle/libs.versions.toml` 中声明
- [ ] 用户可见字符串中英双语同步；复数已跑校验脚本
- [ ] 没有硬编码颜色 / 字号 / 形状，图标来自项目体系
- [ ] 新增代码无 `!!`、`GlobalScope`、空 `catch`、主线程 IO
- [ ] 已在 `arm64-v8a` 设备上验证（APK 不支持 x86_64 模拟器）
- [ ] 文档已同步更新

---

## 📄 许可证

通过贡献，你同意你的贡献依据 [GPL-3.0 许可证](../LICENSE)授权。

---

**感谢你为 Rhythm 做贡献！🎵**
