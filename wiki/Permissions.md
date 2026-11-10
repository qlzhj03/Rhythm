> ℹ️ **文档状态**：本页已按 2026-11 多模块拆分与唯一包名重构后的代码更新。
> 约定与红线见根目录 [AGENTS.md](../AGENTS.md)，设计与重构历史见 [docs/](../docs/)；一切事实以源码为准。

# 权限指南

本指南说明 Rhythm 申请的权限、用途与 Android 版本差异。声明清单以 `app/src/main/AndroidManifest.xml` 为准；运行时申请逻辑见 `app/.../shared/presentation/components/PermissionHandler.kt`。

## 🔒 隐私优先

- ✅ 无数据分析或跟踪
- ✅ 不收集个人数据，收听数据与设置保存在设备本地
- ✅ 核心播放功能不需要联网；外部在线集成默认关闭
- ✅ 仅申请功能所需权限

> 自托管服务器场景需要明文 HTTP / 用户 CA：Manifest 设置 `usesCleartextTraffic="true"` 并配合 `network_security_config.xml`。这是既定取舍，属于对外契约。

---

## 📋 完整声明清单（17 项）

| 权限 | 约束 | 用途 |
|:---|:---|:---|
| `READ_MEDIA_AUDIO` | — | Android 13+ 读取音频媒体 |
| `READ_EXTERNAL_STORAGE` | `maxSdkVersion=32` | Android 12L 及以下读取音频媒体 |
| `WRITE_EXTERNAL_STORAGE` | `maxSdkVersion=28` | 旧版本写入媒体文件 |
| `MODIFY_AUDIO_SETTINGS` | 普通权限 | 音频路由、音量与音频效果配置 |
| `POST_NOTIFICATIONS` | Android 13+ 运行时申请 | 播放通知、更新与下载进度、Rhythm Guard 提醒 |
| `FOREGROUND_SERVICE` | 普通权限 | 后台播放的前台服务 |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Android 14+ | 媒体播放类型前台服务 |
| `INTERNET` | 普通权限 | 可选在线功能与流媒体服务器连接 |
| `ACCESS_NETWORK_STATE` | 普通权限 | 判断网络可用性 |
| `REQUEST_INSTALL_PACKAGES` | `required="false"` | 应用内更新安装 APK（可选） |
| `VIBRATE` | 普通权限 | 触感反馈 |
| `CHANGE_WIFI_MULTICAST_STATE` | 普通权限 | 附近服务器发现所需的组播锁 |
| `WAKE_LOCK` | 普通权限 | 后台播放期间保持唤醒 |
| `BLUETOOTH` | — | 旧版本蓝牙音频设备检测 |
| `BLUETOOTH_ADMIN` | `maxSdkVersion=30` | 旧版本蓝牙设备管理 |
| `BLUETOOTH_CONNECT` | Android 12+ 运行时申请 | 连接蓝牙音频设备、读取设备名称 |
| `BLUETOOTH_SCAN` | `neverForLocation`，Android 12+ 运行时申请 | 扫描蓝牙音频设备（不用于定位） |

小部件接收器与快捷磁贴由系统绑定（`APPWIDGET_UPDATE` / `QS_TILE` action），Rhythm **不声明** `BIND_APPWIDGET` 或 `UPDATE_APPWIDGET` 权限。

---

## 📱 运行时权限

运行时只申请三类，且按系统版本分流：

| 类别 | Android 版本 | 权限 |
|:---|:---|:---|
| 音乐库（存储） | API ≥ 33 | `READ_MEDIA_AUDIO` |
| 音乐库（存储） | API ≤ 32 | `READ_EXTERNAL_STORAGE` |
| 通知 | API ≥ 33 | `POST_NOTIFICATIONS` |
| 通知 | API ≤ 32 | 无需权限 |
| 蓝牙 | API ≥ 31 | `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN` |
| 蓝牙 | API ≤ 30 | `BLUETOOTH` + `BLUETOOTH_ADMIN` |

申请流程使用 Accompanist `rememberMultiplePermissionsState()`，包含 `PermissionsRequired` / `ShowRationale` / `RedirectToSettings` / `PermissionsGranted` 状态机与 `onResume` 复查。引导流程中有音乐库、蓝牙、通知三类专门说明。

- **流媒体（Go）模式跳过存储权限要求**：不扫描本地媒体库。
- 自定义封面选择使用系统 **Photo Picker**，无需任何照片 / 存储权限。
- `REQUEST_INSTALL_PACKAGES` 只用于可选的应用内更新，未在权限流程中主动申请。

### 撤销权限的影响

| 撤销 | 影响 |
|:---|:---|
| 音乐 / 音频读取 | 本地曲库为空、无法播放本地文件 |
| 通知 | 通知栏没有播放控制；播放器界面仍可使用 |
| 蓝牙 | 不显示蓝牙设备名称、不自动恢复播放 |
| 网络（通过系统防火墙） | 无在线歌词 / 封面、无更新检查、无流媒体 |

---

## 🚫 不会请求的权限

❌ `MANAGE_EXTERNAL_STORAGE`（宽泛文件访问）
❌ `ACCESS_MEDIA_LOCATION`（照片 GPS 信息）
❌ `RECORD_AUDIO`（麦克风）
❌ `CAMERA`（相机）
❌ `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`（定位；`BLUETOOTH_SCAN` 已声明 `neverForLocation`）
❌ `READ_CONTACTS` / `READ_PHONE_STATE`（联系人 / 电话）

---

## ⚙️ 管理权限

### 授予

1. 首次启动时按引导流程提示授予
2. 或：系统 **设置 → 应用 → Rhythm → 权限** 手动开启
3. 被永久拒绝后，用 `RedirectToSettings` 提示进入系统设置页

### 撤销 / 重置

系统 **设置 → 应用 → Rhythm → 权限** 中选择「不允许」。
清除应用数据会重置所有设置、播放列表与统计数据，**请先备份**（设置 → 数据与存储 → 备份与恢复）。

---

## 🔍 权限审计

```bash
# 列出已声明 / 已授予权限（注意新包名）
adb shell dumpsys package com.qlzhj.rhythm | grep permission

# debug 包使用 applicationIdSuffix
adb shell dumpsys package com.qlzhj.rhythm.debug | grep permission

# 查看系统权限分组
adb shell pm list permissions -g
```

也可直接使用 `aapt dump permissions <apk>`，或用开源工具 [App Manager](https://github.com/MuntashirAkon/AppManager)、[Exodus Privacy](https://reports.exodus-privacy.eu.org/) 审计。

---

## 🛡️ 隐私最佳实践

1. 只授予必需的权限；Go 模式用户无需本地媒体权限
2. 不需要在线功能就保持外部 API 集成关闭（编译期 → 全局 → 单项三层门控，默认关闭）
3. 只想完全离线可用可用防火墙阻断网络：[NetGuard](https://netguard.me/)、[RethinkDNS](https://rethinkdns.com/)（root 用户可用 AFWall+）
4. Android 11+ 使用分区存储，本地访问仅限于音频媒体

---

## ❓ 常见问题

### 为什么 Rhythm 需要网络访问？
仅用于可选功能：在线歌词、封面、应用更新与流媒体服务器连接。核心播放完全离线可用。

### 可以不授通知权限吗？
可以，但通知栏中看不到播放控制；播放器界面仍可正常使用。

### Rhythm 会访问我的照片吗？
不会。自定义封面使用系统 Photo Picker，只授予你所选图片的访问权，不申请照片权限。

### 为什么需要蓝牙权限？
检测蓝牙耳机 / 音箱连接并在设备重连时恢复播放；不需要时可拒绝，且 `BLUETOOTH_SCAN` 声明 `neverForLocation`，不用于定位。

### 附近服务器发现需要定位权限吗？
不需要。使用 `NsdManager` 与 `CHANGE_WIFI_MULTICAST_STATE` 组播锁扫描本地网络服务。

### 收听数据会被上传吗？
不会。Rhythm Stats 等数据全部保存在设备本地，无跟踪代码。

---

## 🔗 相关文档

- [安装指南](Installation-Guide.md) · [快速入门](Getting-Started.md) · [故障排查](Troubleshooting.md) · [API 文档](API-Documentation.md)
