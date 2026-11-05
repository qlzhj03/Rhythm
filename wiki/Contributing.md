# 为 Rhythm 做贡献

感谢你有兴趣为 Rhythm 音乐播放器做贡献！本指南将帮助你入门。

---

## 🤝 贡献方式

### 1. 报告 Bug

发现了一个 bug？帮助我们修复它！

**报告之前：**
- 查看已有问题以避免重复
- 尝试最新版本，看是否已被修复
- 收集有关你的设备和应用版本的信息

**如何报告：**
1. 在问题追踪中新建 Bug 报告
2. 选择 "Bug Report" 模板
3. 填写所有必填信息：
   - Android 版本（例如 Android 14）
   - 设备型号（例如 Pixel 7）
    - Rhythm 版本（例如 v5.5.500.1306 Beta；实际以 `app/build.gradle.kts` 为准）
   - 复现步骤
   - 预期行为与实际行为
   - 如有需要，附上截图/日志

### 2. 提出功能建议

有一个想法？我们很乐意倾听！

**提出建议之前：**
- 查看已有的功能请求
- 在讨论区搜索类似想法
- 考虑它是否符合 Rhythm 的理念（隐私、FOSS、简洁）

**如何建议：**
1. 使用 "Feature Request" 标签新建问题
2. 描述：
   - 你想要什么功能
   - 它为什么有用
   - 它应该如何工作
   - 潜在的实现思路（可选）

### 3. 改进文档

文档对用户体验至关重要！

**文档需求：**
- 修正错别字和语法错误
- 澄清令人困惑的章节
- 补充缺失的信息
- 更新过时的内容
- 完善中文与英文文案

**如何贡献：**
1. Fork 仓库
2. 编辑 `/wiki/` 或 `/docs/` 中的 `.md` 文件
3. 提交拉取请求
4. 附上清晰的更改说明

### 4. 代码贡献

准备写代码了吗？以下是如何开始！

**我们需要：**
- Bug 修复
- 性能改进
- UI/UX 增强
- 新功能实现
- 代码重构
- 测试覆盖

**写代码之前：**
2. 在问题上评论以认领它
3. 如有需要，讨论实现方案
4. Fork 仓库

---

## 🔧 开发环境设置

### 前置条件

- **Android Studio**：最新稳定版（Ladybug 或更新版本）
- **JDK**：21（三份 CI workflow 与 `app/build.gradle.kts` 的 `compileOptions` / Kotlin `jvmTarget` 统一为 21）
- **Kotlin**：版本以 `gradle/libs.versions.toml` 为准（随 Android Studio 捆绑提供）
- **Git**：用于版本控制
- **Android 设备/模拟器**：用于测试的 API 26+

### 克隆仓库

```bash
git clone <仓库地址>
cd Rhythm
```

### 构建项目

1. 打开 Android Studio
2. 选择 **File → Open** 并选择 Rhythm 文件夹
3. 等待 Gradle 同步完成
4. 点击 **Build → Make Project**（或 Ctrl+F9）

### 在设备上运行

1. 连接已启用 USB 调试的 Android 设备
2. 或启动 Android 模拟器（API 26+）
3. 点击 **Run → Run 'app'**（或 Shift+F10）

---

## 📝 代码规范

### Kotlin 风格

遵循 [Kotlin 官方约定](https://kotlinlang.org/docs/coding-conventions.html)：

```kotlin
// Good
class MusicPlayer {
    fun playTrack(song: Song) {
        // Implementation
    }
}

// Bad
class musicPlayer {
    fun PlayTrack(Song: Song) {
        // Implementation
    }
}
```

### Jetpack Compose

- 使用 `@Composable` 函数构建 UI
- 状态优先使用 `remember` 和 `rememberSaveable`
- 提取可复用组件
- 遵循 Material 3 指南

```kotlin
@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        // UI implementation
    }
}
```

### 架构

- **MVVM 模式**：用 ViewModel 承载业务逻辑
- **仓库模式 (Repository Pattern)**：数据访问抽象
- **依赖注入**：手动 DI
- **整洁架构 (Clean Architecture)**：关注点分离

### 文件组织

```
app/src/main/java/chromahub/rhythm/app/
├── core/
│   └── domain/           # Domain interfaces and models
├── features/
│   ├── local/            # Local playback features
│   │   ├── data/         # Data layer
│   │   ├── domain/       # Business logic
│   │   └── presentation/ # UI components
│   └── streaming/        # Streaming playback features
│       ├── data/         # Network and data layer
│       ├── domain/       # Business logic
│       ├── di/           # Streaming DI modules
│       └── presentation/ # UI components (GoSettings, service setup, discovery)
├── shared/               # Shared utilities, themes, navigation, settings screens
├── infrastructure/       # Audio processors, player engine, service, widgets, workers, network
└── util/ & utils/        # General utilities and extension functions
```

> `infrastructure/network/` 下所有源文件的 `package` 均为 `chromahub.rhythm.app.network`，与目录路径不一致。新增网络接口沿用该 package；import 一律以目标文件的 `package` 声明为准，不要按目录推断。

### 注释

- 编写自解释的代码
- 为复杂逻辑添加注释
- 公共 API 使用 KDoc

```kotlin
/**
 * Plays the specified song and updates playback state.
 *
 * @param song The song to play
 * @param startPosition Position to start playback (milliseconds)
 */
fun playSong(song: Song, startPosition: Long = 0L) {
    // Implementation
}
```

---

## 🧪 测试

### 编写测试

- ViewModel 和工具类的单元测试
- 关键用户流程的 UI 测试
- 数据层的集成测试

```kotlin
@Test
fun `test playback state updates correctly`() {
    val player = ExoPlayer.Builder(context).build()
    player.prepare()
    player.play()
    assertThat(player.playbackState).isEqualTo(Player.STATE_READY)
}
```

### 运行测试

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

---

## 📤 拉取请求流程

### 1. 创建主题分支

从最新 `main` 拉出，命名 `<type>/<slug>`（`type` 与提交类型一致）：

```bash
git switch main && git pull --ff-only
git switch -c feat/lyrics-sync-adjust
# 或
git switch -c fix/playlist-empty-crash
```

不要用 `dev`、`test`、`patch-1` 这类命名。分支存活期间用 `git rebase origin/main` 同步上游，不要把 `main` merge 回分支。

### 2. 进行更改

- 编写简洁、有文档的代码
- 遵循现有代码风格
- 保持提交聚焦且原子化（一个提交只做一件事）
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/v1.0.0/)

```bash
git add <path>
git commit -m "feat(player): add lyrics sync adjustment controls"
# 或
git commit -m "fix(playlist): prevent crash on empty playlist"
```

### 3. 推送到你的 Fork

```bash
git push origin feature/your-feature-name
```

### 4. 发起拉取请求

1. 在仓库页面点击 **Pull Requests → New Pull Request**
2. 选择你的 fork 和分支
3. 填写 PR 模板：
   - **标题**：必须符合 Conventional Commits（如 `feat(player): add sleep timer`）——仓库只启用 Squash 合并，标题会直接成为 `main` 上的提交标题
   - **描述**：做什么、为什么、怎么做
   - **相关问题**：用 `Fixes #123` 关联
   - **截图**：针对 UI 更改
   - **测试**：你如何测试了更改

### 5. 代码评审

- 及时回应反馈
- 按要求进行修改
- 将更新推送到同一分支
- 保持耐心和尊重

### 6. 合并

一旦 approve 且 `Android CI / build` 全绿：
- 维护者以 **Squash and merge** 合并（`main` 保持线性历史，PR 标题即提交标题）
- 合并后主题分支会被自动删除，请勿复用
- 你的贡献将出现在下一个版本发布中，并在更新日志中获得署名

> 禁止直接向 `main` 提交或强制推送；`main` 由分支保护规则与 `.githooks/` 双重拦截。

---

## 🎨 UI/UX 贡献

### 设计原则

- **Material 3**：遵循 Material Design 指南
- **一致性**：匹配现有的 UI 模式
- **无障碍**：支持屏幕阅读器、高对比度
- **性能**：流畅动画，无卡顿
- **简洁**：直观、不杂乱

### UI 指南

- 使用主题颜色，而非硬编码值
- 同时实现浅色和深色模式
- 在不同屏幕尺寸上测试
- 支持横屏方向
- 为交互添加触感反馈

---

## 🌍 语言

Rhythm 仅维护 **中文（简体）** 与 **英文** 两种语言，不再添加新的语言：

| 语言 | 资源文件 |
| :--- | :--- |
| 中文（默认，简体） | `app/src/main/res/values/strings.xml` |
| 英文 | `app/src/main/res/values-en/strings.xml` |

### 更新文案

1. Fork 仓库
2. 检查两个文件中缺失或过时的字符串
3. 同步更新 `values/strings.xml` 与 `values-en/strings.xml`
4. 提交拉取请求

**文案指南：**
- 保持原意
- 恰当使用正式/非正式语气
- 保持占位符不变（`%s`、`%d` 等）
- 测试 UI 以确保文本合适

---

## 📋 提交信息规范

遵循 [Conventional Commits 1.0.0](https://www.conventionalcommits.org/zh-hans/v1.0.0/)，完整规范见 docs/GIT_WORKFLOW.md：

```
<type>(<scope>): <subject>
                      ← 空行
[body]
                      ← 空行
[footer]
```

**规则（由 `.githooks/commit-msg` 校验）：**
- `type` 小写，取自下表枚举；`scope` 可选（`player`、`library`、`playlist`、`streaming`、`ui`、`build`…）
- `subject` 用祈使句，≤ 72 字符，不以句号结尾
- 标题与正文之间必须有空行；关联 Issue 写在 footer：`Closes #123`
- 破坏性变更：`type(scope)!: ...` + footer `BREAKING CHANGE: <说明>`

**类型：**
- `feat`：新功能
- `fix`：Bug 修复
- `perf`：性能优化
- `refactor`：代码重构（不改变行为）
- `docs`：文档更改
- `test`：添加/更新测试
- `build`：构建脚本、Gradle、依赖
- `ci`：CI、钩子、发布流水线
- `chore`：维护任务
- `style`：代码格式（无逻辑变更）
- `revert`：回滚历史提交

**示例：**
```
feat(player): add playback speed control

fix(lyrics): resolve sync offset not saving

docs(readme): update installation instructions

build(deps): bump media3 to 1.11.1
```

---

## 🔍 代码评审检查清单

提交拉取请求之前，请确认：

- [ ] 代码可构建且无错误
- [ ] 没有新的编译器警告
- [ ] 遵循 Kotlin 代码风格
- [ ] UI 符合 Material 3 指南
- [ ] 在 API 26+（compileSdk 37）上可工作
- [ ] 已在物理设备上测试
- [ ] 没有硬编码字符串（使用资源）
- [ ] 已添加/更新文档
- [ ] 已编写/更新测试（如适用）
- [ ] 无合并冲突

---

## 🤔 有问题？

**获取帮助：**
- 查阅[贡献指南](Contributing.md)与 [Wiki 主页](Home.md)
- 在问题追踪中提出具体问题

**保持尊重：**
- 对待回应保持耐心
- 在力所能及的情况下帮助他人
- 尊重维护者的决定

---

## 📄 许可证

通过贡献，你同意你的贡献将依据 [GPL-3.0 许可证](../LICENSE)进行授权。

---

**感谢你为 Rhythm 做贡献！🎵**

你的贡献帮助让 Rhythm 对每个人都变得更好。无论是代码、文档、翻译还是 bug 报告，每一份贡献都很重要！
