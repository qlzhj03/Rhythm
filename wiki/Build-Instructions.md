> ⚠️ **本 wiki 为多模块拆分（2026-11）之前的历史快照。** 模块结构、构建命令与路径
> 已过时；权威信息见根目录 `AGENTS.md` 与 `docs/multi-module-refactor-plan.md`。
> 保留仅供历史参考，内容可能不再准确。

# Rhythm 构建指南

从源代码构建 Rhythm 的完整指南。无论你是贡献者、想要自定义，还是仅仅好奇，本指南都能帮你快速上手。

---

## 📋 前置条件

### 所需软件

#### 1. **Android Studio**
- **版本**：Ladybug 或更新版本（2024.2+）
- **下载**：[Android Studio](https://developer.android.com/studio)
- **组件**：Android SDK、SDK Tools、Platform Tools

#### 2. **Java 开发工具包（JDK）**
- **版本**：**JDK 21**（统一基线）。三份 GitHub Actions workflow、`app/build.gradle.kts` 的 `compileOptions` 与 Kotlin `jvmTarget` 均为 21。
- **下载**：[OpenJDK 21](https://adoptium.net/temurin/releases/?version=21)
- **依据**：JDK 基线与依赖版本一律以 `app/build.gradle.kts`、`gradle/libs.versions.toml`、`gradle/wrapper/gradle-wrapper.properties` 与 `.github/workflows/` 为准；本页出现的数值只是快照，不要反向作为版本依据。

#### 3. **Git**
- **版本**：2.30 或更新版本
- **下载**：[Git](https://git-scm.com/downloads)

### 系统要求

**最低配置：**
- **内存**：8 GB
- **存储**：10 GB 可用空间
- **操作系统**：Windows 10/11、macOS 10.14+ 或 Linux（Ubuntu 18.04+）

**推荐配置：**
- **内存**：16 GB
- **存储**：20 GB 可用空间（推荐 SSD）
- **CPU**：多核处理器

---

## 🚀 快速开始

### 1. 克隆仓库

```bash
git clone <仓库地址>
cd Rhythm
```

### 2. 在 Android Studio 中打开

1. 启动 **Android Studio**
2. 选择 **Open an Existing Project**
3. 导航到克隆的 `Rhythm` 文件夹
4. 点击 **OK**

### 3. 同步 Gradle

Android Studio 会自动提示同步 Gradle。如果没有：

1. 点击 **File** → **Sync Project with Gradle Files**
2. 等待依赖下载完成（首次约 5-10 分钟）

### 4. 运行应用

1. 通过 USB 连接 Android 设备（Android 8.0+）**或**创建模拟器
2. 在设备上启用 **开发者选项** 和 **USB 调试**
3. 点击 Android Studio 中的 **Run** 按钮（▶️）
4. 选择你的设备/模拟器
5. 等待构建完成（首次构建约 3-5 分钟）

---

## 🔧 详细构建步骤

### 第 1 步：在 Android Studio 中配置 JDK

确保使用 JDK 21（与三份 CI workflow 及构建脚本一致）：

1. **File** → **Project Structure** → **SDK Location**
2. 在 **JDK Location** 下，确认已选择 JDK 21
3. 如果没有，点击 **Download JDK** 并选择版本 21

### 第 2 步：配置 Android SDK

所需的 SDK 组件：

1. **Tools** → **SDK Manager**
2. **SDK Platforms** 选项卡：
   - ✅ Android API 37 - Compile SDK
   - ✅ Android 8.0 (API 26) - Minimum SDK
3. **SDK Tools** 选项卡：
   - ✅ Android SDK Build-Tools 35.0.0
   - ✅ Android SDK Platform-Tools
   - ✅ Android SDK Command-line Tools
4. 点击 **Apply** 并等待下载完成

### 第 3 步：配置 Gradle 属性

在项目根目录创建/编辑 `local.properties`：

```properties
# Android SDK location (auto-generated)
sdk.dir=/path/to/Android/Sdk

# Optional: Gradle JVM arguments for better performance
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
```

### 第 4 步：验证构建配置

检查 `app/build.gradle.kts`：

```kotlin
android {
    namespace = "chromahub.rhythm.app"
    compileSdk = 37
    
    defaultConfig {
        applicationId = "chromahub.rhythm.app"
        minSdk = 26          // Android 8.0
        targetSdk = 37       // Android 15
        versionCode = 555001306
        versionName = "5.5.500.1306 Beta"
    }
    
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }
}
```

> 以上为文档快照；`versionCode` / `versionName` 与 SDK 版本一律以 `app/build.gradle.kts` 为准（CI 可用 `-PversionCodeOverride` / `-PversionNameOverride` 覆盖）。

#### **Debug 构建**（开发）
```bash
./gradlew assembleDebug
```
- 输出：`app/build/outputs/apk/debug/`
- 文件命名格式为 `Rhythm-{versionName}-{buildType}.apk`（例如 `Rhythm-5.5.500.1306 Beta-debug.apk`）；所有 APK 仅包含 `arm64-v8a` 架构
- 特性：启用调试、启用日志
- 签名：Debug 密钥库（自动生成）

#### **Release 构建**（生产）
```bash
./gradlew assembleRelease
```
- 输出：`app/build/outputs/apk/release/`
- 特性：已优化、启用 R8/ProGuard
- 签名：需要签名配置

---

## 🔐 Release 构建配置

### 生成签名密钥

```bash
keytool -genkey -v -keystore rhythm-release-key.jks \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -alias rhythm-key
```

**重要**：请安全存储此密钥库，并牢记密码！

### 配置签名

创建 `.config/keystore.properties`（切勿提交此文件）：

```properties
store_file=../rhythm-release-key.jks
store_password=YourStorePassword
key_alias=rhythm-key
key_password=YourKeyPassword
```

实际的 `app/build.gradle.kts` 需要这些特定的 snake_case 属性名，并使用 `getProperties()` 辅助函数：

```kotlin
// In app/build.gradle.kts (top level)
val signingProperties = getProperties(".config/keystore.properties")
val releaseSigning = if (signingProperties != null) {
    signingConfigs.create("release") {
        keyAlias = signingProperties.property("key_alias")
        keyPassword = signingProperties.property("key_password")
        storePassword = signingProperties.property("store_password")
        storeFile = rootProject.file(signingProperties.property("store_file"))
    }
} else {
    signingConfigs.getByName("debug")
}

// Applied to build types:
release {
    signingConfig = releaseSigning
    isMinifyEnabled = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

### 构建已签名的 Release APK

```bash
./gradlew assembleRelease
```

输出：`app/build/outputs/apk/release/`
APK 命名：`Rhythm-{versionName}-{buildType}.apk`

---

## 🛠️ 命令行构建

### 使用 Gradle Wrapper（推荐）

**Windows：**
```cmd
.\gradlew.bat clean assembleDebug
```

**macOS/Linux：**
```bash
./gradlew clean assembleDebug
```

### 常用 Gradle 任务

```bash
# Clean build artifacts
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Install debug APK on connected device
./gradlew installDebug
```

---

## 🧪 测试构建

### 启用开发者选项

1. **Settings** → **About Phone**
2. 点按 **Build Number** 7 次
3. 如提示则输入 PIN 码/密码
4. **Developer Options** 现已可用

### USB 调试

1. **Settings** → **Developer Options**
2. 启用 **USB 调试**
3. 通过 USB 连接设备
4. 在设备上接受授权对话框

### 验证设备连接

```bash
adb devices
```

预期输出：
```
List of devices attached
ABC123456789    device
```

### 手动安装 APK

```bash
adb install app/build/outputs/apk/debug/Rhythm-*-debug.apk
```

---

## 🎯 构建优化

### 加快构建速度

#### 1. **增大堆内存**

编辑 `gradle.properties`：

```properties
org.gradle.jvmargs=-Xmx6144m -XX:MaxMetaspaceSize=1024m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

#### 2. **启用 Gradle 构建缓存**

```properties
android.enableBuildCache=true
android.buildCacheDir=~/.android/build-cache
```

#### 3. **使用配置缓存**

```bash
./gradlew --configuration-cache assembleDebug
```

### 排查构建缓慢问题

```bash
# Profile build performance
./gradlew assembleDebug --profile

# Scan report: build/reports/profile/
```

---

## ❌ 常见构建错误

### 错误 1：找不到 SDK

**错误：**
```
SDK location not found. Define location with sdk.dir in the local.properties file
```

**解决方法：**
创建 `local.properties`：
```properties
sdk.dir=/path/to/Android/Sdk
```

### 错误 2：JDK 版本不匹配

**错误：**
```
Unsupported class file major version 61
```

**解决方法：**
`major version 61` 对应 Java 17，说明使用了旧版 JDK。请配置为 JDK 21（File → Project Structure → SDK Location），与三份 CI workflow 保持一致。

### 错误 3：内存不足

**错误：**
```
OutOfMemoryError: Java heap space
```

**解决方法：**
在 `gradle.properties` 中增大堆内存：
```properties
org.gradle.jvmargs=-Xmx8192m
```

### 错误 4：找不到依赖

**错误：**
```
Could not resolve all dependencies
```

**解决方法：**
1. 检查网络连接
2. 同步 Gradle：**File** → **Sync Project with Gradle Files**
3. 清除缓存：**File** → **Invalidate Caches / Restart**

### 错误 5：Build Tools 版本问题

**错误：**
```
Failed to find Build Tools revision 35.0.0
```

**解决方法：**
通过 SDK Manager 安装缺失的构建工具。

### 错误 6：NDK 未配置（如需要）

**错误：**
```
NDK not configured
```

**解决方法：**
通过 **SDK Manager** → **SDK Tools** → **NDK (Side by side)** 下载 NDK

---

## 📦 构建产物

### Debug APK
- **路径**：`app/build/outputs/apk/debug/`
- **文件**：`Rhythm-{versionName}-debug.apk`
- **大小**：约 50-60 MB
- **签名**：Debug 密钥库
- **可调试**：是
- **已优化**：否

### Release APK
- **路径**：`app/build/outputs/apk/release/`
- **文件**：`Rhythm-{versionName}-release.apk`
- **大小**：约 30-40 MB（经 R8/ProGuard 优化）
- **签名**：Release 密钥库
- **可调试**：否
- **已优化**：是（R8/ProGuard）

### 构建报告
- **Lint**：`app/build/reports/lint-results.html`
- **测试**：`app/build/reports/tests/`
- **性能分析**：`build/reports/profile/`

---

## 🔍 验证构建完整性

### 检查 APK 签名

```bash
keytool -printcert -jarfile app-release.apk
```

### 查看 APK 内容

```bash
unzip -l app-release.apk
```

### APK Analyzer（Android Studio）

1. **Build** → **Analyze APK**
2. 选择 APK 文件
3. 查看大小、资源、DEX 文件

---

## 🌐 CI/CD 构建

### GitHub Actions 示例

> 仓库已无 product flavor：`assembleGithub*` 任务不存在，也不会生成 `app/build/outputs/apk/github/...`。正确的任务是 `assembleDebug` / `assembleRelease`，产物位于 `app/build/outputs/apk/{debug,release}/`。以下示例与 `.github/workflows/` 中的实际配置保持一致。

```yaml
name: Build APK

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      
      - name: Set up JDK 21
        uses: actions/setup-java@v6
        with:
          java-version: '21'
          distribution: 'zulu'
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Build with Gradle
        run: ./gradlew assembleDebug
      
      - name: Upload APK
        uses: actions/upload-artifact@v7
        with:
          name: rhythm-debug
          path: app/build/outputs/apk/debug/Rhythm-*-debug*.apk
```

---

## 🧹 清理构建

### 完全清理

```bash
./gradlew clean
rm -rf .gradle
rm -rf build
rm -rf app/build
```

### 清除缓存（Android Studio）

**File** → **Invalidate Caches** → 勾选所有选项 → **Invalidate and Restart**

---

## 📚 更多资源

- [Android Studio 用户指南](https://developer.android.com/studio/intro)
- [配置你的构建](https://developer.android.com/studio/build)
- [构建变体](https://developer.android.com/studio/build/build-variants)
- [为应用签名](https://developer.android.com/studio/publish/app-signing)
- [Gradle 文档](https://docs.gradle.org/)

---

**祝构建愉快！** 🎉
