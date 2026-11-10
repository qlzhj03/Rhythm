> ℹ️ **文档状态**：本页已按 2026-11 多模块拆分与唯一包名重构后的代码更新。
> 约定与红线见根目录 [AGENTS.md](../AGENTS.md)，设计与重构历史见 [docs/](../docs/)；一切事实以源码为准。

# 安装指南

本指南介绍在 Android 设备上安装 Rhythm 音乐播放器的方法。

## 📋 系统要求

- **Android 版本**：8.0+（API 26）
- **CPU 架构**：**`arm64-v8a`** —— APK 仅包含该 ABI，x86_64 模拟器无法安装
- **内存**：最低 2 GB
- **存储空间**：应用占用约 50 MB + 你的音乐库所需空间
- **权限**：本地模式需要音频媒体读取权限（Android 13+ 为 `READ_MEDIA_AUDIO`）；Go 模式不需要本地曲库权限

---

## 📥 获取 APK

Rhythm 从源码构建，不提供预编译分发：

```bash
./gradlew assembleDebug      # 开发 / 日常使用
./gradlew assembleRelease    # 发布前构建
```

产物位于 `app/build/outputs/apk/{debug,release}/`，命名为 `Rhythm-{versionName}-{buildType}.apk`。构建、JDK 与签名细节见[构建说明](Build-Instructions.md) 与 [AGENTS.md](../AGENTS.md)。

> release 包在项目根目录 `keystore.properties` 缺失时会回退使用 debug 签名；覆盖安装时请始终使用同一签名来源。

---

## ✅ 安装

### 从文件管理器安装

1. **启用未知来源**：设置 → 安全 → 安装未知应用 → 选择浏览器 / 文件管理器 → 允许
2. 打开 APK 文件 → 点按「安装」→ 完成后「打开」

### 通过 ADB 安装

```bash
adb devices
adb install app/build/outputs/apk/debug/Rhythm-*-debug.apk
# 或直接使用 Gradle
./gradlew :app:installDebug
```

> **安全说明**：只安装由可信来源构建、且使用同一密钥签名的 APK。

---

## 🆕 首次启动

1. **授予权限**
   - 音乐与音频（本地模式必需）
   - 通知（Android 13+，用于播放控制）
   - 可选：蓝牙（无线音频设备检测）
2. **完成引导流程**：选择本地 / Go 模式、Rhythm Guard 听力保护、主题与手势等偏好
3. **允许首次媒体扫描**（本地模式）
4. **开始聆听**

权限清单与版本差异详见[权限指南](Permissions.md)，基础用法见[快速入门](Getting-Started.md)。

---

## 🔄 更新 Rhythm

- **应用内更新**：设置 → 更新与信息 → 更新（可选功能，需要 `REQUEST_INSTALL_PACKAGES`）
- **手动更新**：安装新版本 APK 覆盖现有应用，数据保留

> ⚠️ **包名变更（2026-11）**：`applicationId` 已从 `chromahub.rhythm.app` 改为 **`com.qlzhj.rhythm`**（debug 为 `com.qlzhj.rhythm.debug`）。旧包**无法原地更新**，会作为两个应用并存；迁移前请先备份设置与播放列表，再卸载旧版。
>
> 自定义 action / broadcast / extra 协议字面前缀仍为 `chromahub.rhythm.app.*`，这是应用内部约定，不影响安装与更新。

---

## 🆘 安装故障排查

| 错误 | 常见原因 | 处理 |
|:---|:---|:---|
| 「应用未安装」 | 签名不匹配 / APK 损坏 | 坚持同一签名来源；必要时先卸载旧版再安装（先备份） |
| 「安装被阻止」 | 未启用未知来源 | 为文件管理器 / 浏览器启用「安装未知应用」 |
| 「解析错误」 | 下载不完整 / 架构不匹配 | 重新构建 / 下载；确认设备是 `arm64-v8a` |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | x86_64 设备或模拟器 | 换用 arm64 设备 / 系统镜像；APK 不含 x86_64 |
| 出现两个 Rhythm 图标 | 新旧 `applicationId` 不同 | 卸载旧 `chromahub.rhythm.app` 版本 |
| 更新无法安装 | 不同来源签名不一致 | 使用同一构建来源，或卸载后全新安装 |

更多问题见[故障排查](Troubleshooting.md)。

---

## 🔒 安全说明

- 请安装由可信来源构建并使用同一密钥签名的 APK
- 不同签名的 APK 无法覆盖安装
- 验证包名：`com.qlzhj.rhythm`（debug：`com.qlzhj.rhythm.debug`）
- 项目不提交 `local.properties`、`keystore.properties` 或任何密钥文件

---

## 🔗 相关文档

- [构建说明](Build-Instructions.md) · [快速入门](Getting-Started.md) · [权限指南](Permissions.md) · [常见问题](FAQ.md)
