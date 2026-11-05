
<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/icon.png">
  <source media="(prefers-color-scheme: light)" srcset="assets/icon.png">
  <img src="assets/icon_full.png" alt="Rhythm 音乐播放器" width="220" style="border-radius: 32px; margin: 20px 0; box-shadow: 0 8px 24px rgba(0,0,0,0.2);">
</picture>

# Project Rhythm
**你的音乐，你的节奏。**

[![Platform](https://img.shields.io/badge/Android-34A853?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![API Level](https://img.shields.io/badge/API-26%2B-4285f4?style=flat-square&logo=android&logoColor=white)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7c4dff?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-GPL_v3-4285f4?style=flat-square)](LICENSE)
</div>

---

## ✨ 为什么选择 Rhythm？

Rhythm 是一款采用 **Material 3 Expressive** 设计打造的现代开源音乐播放器。它通过高解析度(Hi-Res)播放和 FFmpeg 实现的 EAC3-JOC/Dolby Atmos 提供专业级音质，并完全尊重你的隐私。

| 🎨 **设计与界面** | 🎵 **音频引擎** |
| :--- | :--- |
| **Material You：** 基于壁纸颜色的动态主题（Android 12+）。 | **专业音质：** Media3 ExoPlayer，支持无缝播放与高解析度(Hi-Res)模式。 |
| **表现力界面：** 精致的自适应形状与 Material 3 设计。 | **10 段 EQ：** 专业均衡器，内置 6032+ AutoEQ 设备预设。 |
| **平板界面：** 为平板优化的多栏布局。 | **响度均衡(ReplayGain)：** 曲目/专辑响度归一化。 |
| **Material Symbols：** 清晰、可缩放的可变字重图标。 | **交叉淡入淡出：** 平滑过渡，时长可调。 |

| 🎤 **功能与工具** | 🔒 **隐私与数据** |
| :--- | :--- |
| **同步歌词：** 集成 LRCLib，支持逐词高亮 + 全屏视图。 | **100% FOSS：** 完全开源，零跟踪。 |
| **智能音乐库：** A–Z 滚动条、多选批量操作、专辑艺术家与日期标签页。 | **离线可用：** 专为在无网络时也能完美运行而设计。 |
| **Rhythm Stats 2.0：** 全面的统计数据，包含概览、歌曲、专辑、艺术家、历史。 | **本地优先：** 你的数据和使用习惯安全地保留在设备上。 |
| **Go 模式（流媒体）：** 本地 + 服务器播放（Subsonic、Navidrome、Jellyfin），支持附近服务器发现。 | |
| **Rhythm Go 下载：** 下载流媒体曲目以便离线播放。 | |
| **Rhythm Guard：** 内置听力安全与音量保护。 | |
| **单声道音频：** 混缩为单声道，便于单耳聆听。 | |
| **折叠屏适配：** 感知姿态的播放器布局，可适配桌面、书本和分屏等姿态。 | |
| **桌面小部件：** 多种布局，包括专属的歌词小部件。 | |

> **系统要求：** Android 8.0+ (API 26) • 2GB 内存 • 50MB 存储空间

---

## 📱 截图

<div align="center">
<table>
<tr>
<td align="center" width="25%">
<img src="assets/ScreenShots/Phone/Home.png" alt="主页" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>🏠 智能主页</b>
</td>
<td align="center" width="25%">
<img src="assets/ScreenShots/Phone/Player_2.png" alt="播放器" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>▶️ 精美播放器</b>
</td>
<td align="center" width="25%">
<img src="assets/ScreenShots/Phone/Player_Lyrics_View.png" alt="歌词" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>🎤 同步歌词</b>
</td>
<td align="center" width="25%">
<img src="assets/ScreenShots/Phone/Playlist.png" alt="音乐库" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>📚 丰富音乐库</b>
</td>
</tr>
<tr>
<td align="center">
<img src="assets/ScreenShots/Phone/Queue.png" alt="播放队列" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>📋 智能队列</b>
</td>
<td align="center">
<img src="assets/ScreenShots/Phone/Search.png" alt="搜索" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>🔍 即时搜索</b>
</td>
<td align="center">
<img src="assets/ScreenShots/Phone/Settings.png" alt="设置" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>⚙️ 丰富设置</b>
</td>
<td align="center">
<img src="assets/ScreenShots/Phone/Artist.png" alt="艺术家" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>🎤 艺术家页面</b>
</td>
</tr>
</table>
</div>

### 平板

<div align="center">
<table>
<tr>
<td align="center" width="25%">
<img src="assets/ScreenShots/Tablet/Player.png" alt="播放器" width="240" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>▶️ 正在播放</b>
</td>
<td align="center" width="25%">
<img src="assets/ScreenShots/Tablet/Library.png" alt="音乐库" width="240" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>📚 音乐库</b>
</td>
<td align="center" width="25%">
<img src="assets/ScreenShots/Tablet/Lyrics_View.png" alt="歌词" width="240" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>🎤 同步歌词</b>
</td>
<td align="center" width="25%">
<img src="assets/ScreenShots/Tablet/Album.png" alt="专辑" width="240" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>💿 专辑详情</b>
</td>
</tr>
<tr>
<td align="center">
<img src="assets/ScreenShots/Tablet/Search.png" alt="搜索" width="240" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>🔍 搜索</b>
</td>
<td align="center">
<img src="assets/ScreenShots/Tablet/Rhythm_Stats.png" alt="统计" width="240" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>📊 播放统计</b>
</td>
<td align="center">
<img src="assets/ScreenShots/Tablet/Settings.png" alt="设置" width="240" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>⚙️ 设置</b>
</td>
<td align="center">
<img src="assets/ScreenShots/Tablet/About.png" alt="关于" width="240" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"/>
<br/><b>ℹ️ 关于</b>
</td>
</tr>
</table>
</div>

---

## 📥 获取与安装

从源码构建 APK 后安装：

```bash
./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。构建与签名细节见 [AGENTS.md](AGENTS.md)，安装步骤见[安装指南](wiki/Installation-Guide.md)，更多说明见 [Wiki 构建说明](wiki/Build-Instructions.md)。

---

## 🛠 技术栈

采用现代工具与清晰的架构打造：

* **界面与设计：** Jetpack Compose、Material 3、Glance 小部件、Coil、AndroidX Palette。
* **音频引擎：** Media3 ExoPlayer、FFmpeg 解码器、JAudioTagger。
* **架构：** MVVM + 清洁架构，100% Kotlin，Kotlin 协程 + Flow。
* **数据与网络：** Room + SQLite、Retrofit、OkHttp、Gson。
* **构建与管理：** AGP、Kotlin、WorkManager、LeakCanary。

📖 **完整技术栈：** 详情请阅读 [Wiki](wiki/Technology-Stack.md)。

---

## 🌍 语言

Rhythm 仅维护 **中文（简体）** 与 **英文** 两种语言：

| 语言 | 资源文件 |
| :--- | :--- |
| 🇨🇳 中文（默认，简体） | `app/src/main/res/values/strings.xml` |
| 🇬🇧 英文 | `app/src/main/res/values-en/strings.xml` |

新增或修改面向用户的字符串时，请同步更新这两个文件。欢迎直接通过拉取请求修正文案。

---

## 🤝 贡献

欢迎贡献！贡献流程与代码规范见[贡献指南](wiki/Contributing.md)。

* 🐛 **发现 bug 或有新想法？** 在问题追踪中提交 issue
* 📖 **需要帮助？** 阅读 [Wiki](wiki/Home.md)
* 🌍 **发现文案问题？** 直接提交拉取请求修正 `values/strings.xml` 与 `values-en/strings.xml`

---

<div align="center">

<sub>**许可证：** [GNU General Public License v3.0](LICENSE)</sub>

</div>
