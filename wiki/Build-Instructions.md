> ℹ️ **文档状态**：本 wiki 已按 2026-11 多模块拆分与唯一包名重构后的代码更新。
> 约定与红线见根目录 [AGENTS.md](../AGENTS.md)，设计与重构历史见 [docs/](../docs/)；一切事实以源码为准。

# Rhythm 构建指南

从源码构建 Rhythm、运行本地验证的完整指南。**只用 `./gradlew` wrapper**，仓库无 CI。

---

## 📋 前置条件

| 项目 | 要求 |
|:---|:---|
| JDK | **21**（Java `sourceCompatibility` / `targetCompatibility` 与 Kotlin `jvmTarget` 均为 `JVM_21`，由 `build-logic/convention/.../AndroidCommon.kt` 注入） |
| Android Studio | 兼容 AGP 9.4.1 / Gradle 9.7.1 的最新稳定版 |
| Android SDK | `compileSdk 37`、`minSdk 26`、`targetSdk 37`、`targetSdk` 对应平台与 Platform-Tools、Build-Tools、Command-line Tools |
| Gradle | 由 wrapper 提供（`gradle-wrapper.properties` 指向 Gradle `9.7.1`），无需全局安装 |
| Git | 2.30+ |
| 设备 | Android 8.0+（API 26+）、**`arm64-v8a`**；APK 仅含该 ABI，x86_64 模拟器装不上 |

内存建议 8 GB 起（推荐 16 GB），首次同步与构建需预留 10–20 GB 磁盘空间。

> JDK、SDK、依赖与 Gradle 版本一律以 `build-logic/`、`app/build.gradle.kts`、`gradle/libs.versions.toml`、`gradle/wrapper/gradle-wrapper.properties` 为准；本页数值只是快照，不要反向作为版本依据。

---

## 🚀 快速开始

```bash
git clone <仓库地址>
cd Rhythm
./gradlew :app:compileDebugKotlin   # 最快编译检查
./gradlew assembleDebug             # 构建 debug APK
./gradlew :app:installDebug         # 安装到已连接的 arm64-v8a 设备
```

在 Android Studio 中：`File → Open` 选择仓库根目录 → 等待 Gradle Sync → 连接设备后点击 **Run**。若未自动同步，用 `File → Sync Project with Gradle Files`。

> 首次运行需授予音乐与通知权限并完成引导流程，详见[快速入门](Getting-Started.md)。

---

## 🔧 构建配置速览

| 项目 | 当前值 / 位置 |
|:---|:---|
| 模块 | `settings.gradle.kts` 注册 13 个模块（`:app`、7 个 `:core:*`、4 个 `:feature:*`、`:macrobenchmark`） |
| 约定插件 | `rhythm.android.application`、`rhythm.android.library`、`rhythm.android.library.compose`、`rhythm.hilt`（`build-logic/convention`） |
| Kotlin | **AGP 9.x 内建**；`kotlin-android` 别名在 catalog 中定义但未使用，**不要添加**（加了直接构建失败） |
| `namespace` | `com.qlzhj.rhythm.app` |
| `applicationId` | `com.qlzhj.rhythm`（debug 后缀 `.debug`） |
| SDK | `compileSdk 37` / `minSdk 26` / `targetSdk 37`；Java 与 Kotlin `JVM_21`；启用 core library desugaring |
| ABI | `ndk { abiFilters += "arm64-v8a" }`，无 ABI splits |
| buildTypes | `debug` / `release` / `benchmark`；**无 product flavor**，`assembleGithub*` 任务不存在 |
| release | R8 混淆 + 资源收缩；签名回退见下节 |
| 产物 | `Rhythm-{versionName}-{buildType}.apk` |
| 静态检查 | lint / ktlint / detekt 刻意禁用，`lint*` 任务被硬禁用 |

> `versionCode` / `versionName` 由发布脚本维护；临时构建用 `-PversionCodeOverride` / `-PversionNameOverride` 覆盖，不要手改。

### `local.properties`

由 Android Studio 自动生成，只需 SDK 路径，**不要提交**：

```properties
sdk.dir=/path/to/Android/Sdk
```

`gradle.properties` 的内存 / 并发调优是刻意配置，**不要修改**；如遇内存不足，优先清理后台进程或使用 `-Dorg.gradle.jvmargs=...` 的临时命令行覆盖，而不是改文件。

---

## 🔐 Release 签名

`app/build.gradle.kts` 通过 `getProperties()` 读取**项目根目录**的 `keystore.properties`，键名为 `store_file`、`store_password`、`key_alias`、`key_password`：

- 文件存在 → 创建 `release` signingConfig，用于 `release` / `debug` / `benchmark` 三个 buildType
- 文件缺失 → **回退 `debug` 签名**，仍可打出 release 包

```bash
./gradlew assembleRelease
```

> `keystore.properties`、`local.properties`、`*.jks` 与任何令牌内容都不提交、不输出、不复制到新位置。本文只说明机制，不记录任何凭据值或签名路径内容。

---

## 🧪 测试与本地验证

### 常用任务

```bash
# 最快编译检查
./gradlew :app:compileDebugKotlin

# debug / release 包
./gradlew assembleDebug
./gradlew assembleRelease      # 仅发布前

# 安装 debug
./gradlew :app:installDebug

# 全部 JVM 单测（按模块指定；禁止裸 ./gradlew test）
./gradlew :app:testDebugUnitTest :core:common:testDebugUnitTest \
  :core:media:testDebugUnitTest :core:ui:testDebugUnitTest \
  :core:designsystem:testDebugUnitTest :core:data:testDebugUnitTest \
  :feature:local:testDebugUnitTest :feature:streaming:testDebugUnitTest

# strings 复数校验
python3 scripts/check_plural_specifiers.py
```

**提交前最小集合**：`assembleDebug` + 上述全部 JVM 单测（涉及复数资源时再跑校验脚本）。

### 测试现状

- JUnit4 + MockWebServer3 + 真 `org.json`，测试与源码同包，放各模块 `src/test/java` 或 `src/test/kotlin`
- `unitTests.isReturnDefaultValues = true` 是预期配置；`android.jar` stub 返回默认值属正常
- **无 `androidTest` 源集**，因此没有 `connectedAndroidTest` 任务可跑；UI 验证在真机上进行
- `:macrobenchmark` 是独立的 `com.android.test` 模块，提供冷/热启动、滚动帧时序与 Baseline Profile 生成用例
- 禁用 MockK、Turbine、coroutines-test、JUnit 5、Robolectric、截图测试框架

### 无 CI

仓库**没有** `.github/workflows/` 或任何 CI 配置，也不存在 `Android CI / build` 状态检查；新增 CI 配置或运行发布脚本前请先询问维护者。验证完全依赖上述本地命令。

---

## 📦 构建产物

| 变体 | 路径 | 说明 |
|:---|:---|:---|
| debug | `app/build/outputs/apk/debug/` | 可调试、未优化、debug 签名（或 release 签名，视 `keystore.properties` 而定） |
| release | `app/build/outputs/apk/release/` | R8 混淆 + 资源收缩 |
| benchmark | `app/build/outputs/apk/benchmark/` | `initWith(release)`，供 Macrobenchmark 使用 |

产物名为 `Rhythm-{versionName}-{buildType}.apk`，仅含 `arm64-v8a`。

```bash
adb install app/build/outputs/apk/debug/Rhythm-*-debug.apk
keytool -printcert -jarfile <apk>      # 检查签名
unzip -l <apk>                          # 查看内容
```

> 由于 lint 任务被禁用，不存在 `app/build/reports/lint-results.html`；测试报告在各模块的 `build/reports/tests/` 下。

---

## ❌ 常见构建错误

| 现象 | 处理 |
|:---|:---|
| `SDK location not found` | 在 `local.properties` 写入 `sdk.dir=/path/to/Android/Sdk` |
| `Unsupported class file major version 61` | 用的是 Java 17；切到 **JDK 21**（`File → Project Structure → SDK Location`） |
| `OutOfMemoryError: Java heap space` | 清理后台进程 / 关闭其他 JVM；用命令行临时覆盖堆参数，**不要改 `gradle.properties` 调优** |
| `Could not resolve all dependencies` | 检查网络、`jitpack.io` 可达性，重新 Sync Gradle / Invalidate Caches |
| `Failed to find Build Tools revision ...` | 在 SDK Manager 安装对应 Build-Tools（版本以 AGP 要求为准，不写死） |
| 声明了 `org.jetbrains.kotlin.android` 导致构建失败 | AGP 9.x 内建 Kotlin，移除该插件（红线） |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | APK 仅含 `arm64-v8a`，换用 arm64 真机或对应系统镜像 |
| 应用无法覆盖安装旧版 | 2026-11 起 `applicationId` 由 `chromahub.rhythm.app` 改为 `com.qlzhj.rhythm`，旧包无法原地更新 |

---

## 🧹 清理与构建分析

```bash
./gradlew clean
./gradlew assembleDebug --profile     # 报告位于 build/reports/profile/
```

Android Studio 侧可用 `File → Invalidate Caches / Restart`；`Build → Analyze APK` 可查看产物结构。不要提交或改写任何构建产物、生成代码。

---

## 📜 脚本与发布

`scripts/` 下的工具（**运行发布脚本前先询问**）：

| 脚本 | 用途 |
|:---|:---|
| `check_plural_specifiers.py` | 校验中英复数字符串不引入源串没有的格式占位符 |
| `prepare_release.py` | 交互式发布准备：更新版本号 / 发布日、变更日志与渠道元数据 |
| `generate_release_notes.py` | 由 tag 与提交生成 Release 说明 |
| `relocate_packages.py` / `fix_split_package_refs.py` | 2026-11 唯一包名重构的迁移与引用修复工具（历史工具） |

版本号由发布脚本维护，AI / 贡献者不自行 bump；临时构建用 `-P` 覆盖参数。

---

## 📚 更多资源

- [AGENTS.md](../AGENTS.md)（命令与红线）· [架构指南](Architecture.md) · [技术栈](Technology-Stack.md) · [贡献指南](Contributing.md) · [安装指南](Installation-Guide.md)
- [Android Studio 用户指南](https://developer.android.com/studio/intro) · [配置构建](https://developer.android.com/studio/build) · [为应用签名](https://developer.android.com/studio/publish/app-signing) · [Gradle 文档](https://docs.gradle.org/)

---

**祝构建愉快！** 🎉
