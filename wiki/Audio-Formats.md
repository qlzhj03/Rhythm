> ℹ️ **文档状态**：本页已按 2026-11 多模块拆分与唯一包名重构后的代码更新。
> 约定与红线见根目录 [AGENTS.md](../AGENTS.md)，设计与重构历史见 [docs/](../docs/)；一切事实以源码为准。

# 音频格式支持

Rhythm 使用 **Media3 ExoPlayer 1.11.1 + Jellyfin media3-ffmpeg-decoder 1.9.0+1** 播放本地与流媒体音频。本页介绍支持的格式、扫描白名单、音质识别与技术限制。

> **解码器扩展可开关**：设置中的「解码器扩展」页（`AppSettings.decoderExtensionsEnabled` / `disabledDecoderExtensions`）可整体关闭扩展渲染器，或单独禁用某个扩展；关闭后只依赖设备平台解码器，并非「所有构建变体都无条件可用」。

**媒体库识别的文件后缀**（以 `MediaScanEngine` / `LocalMusicRepository` 白名单为准）：

- 文件夹扫描：`mp3, m4a, flac, ogg, opus, opa, wav, aac, alac, aiff, aif, wma, mka, ac3, ac4, oga, mid, midi, adts, m4b`
- 播放与外部打开额外支持：`eac, eac3, mhm, mhm1, dts, dtshd, dtsx, truehd, ape, wv, tta, tak, dsf, dff, dsd` 及 MP4 / Matroska 容器中的音频流

---

## ✅ 内置格式（平台解码器）

这些格式主要由 Android 平台编解码器提供；具体位深、采样率与编解码支持随设备而异，表中数值是常见上限而非承诺：

| 格式 | 容器 | 音质 | 位深 | 采样率 | 备注 |
|:---:|:---:|:---:|:---:|:---:|:---|
| **FLAC** | `.flac` | 无损 | 最高 32 位 | 最高 384kHz | 无损音频的推荐格式 |
| **ALAC** | `.m4a` | 无损 | 最高 32 位 | 最高 384kHz | Apple 无损音频编解码器 |
| **MP3** | `.mp3` | 有损 | 不适用 | 最高 48kHz | 支持所有比特率、支持 VBR |
| **AAC** | `.m4a`, `.aac`, `.mp4`, `.adts`, `.m4b` | 有损 | 不适用 | 最高 96kHz | AAC-LC、HE-AAC、HE-AACv2；ADTS、有声书（M4B） |
| **MP4 音频** | `.mp4`, `.m4a` | 有损/无损 | 不适用 | 最高 96kHz | MP4 容器内的音轨（AAC/ALAC/AC-3） |
| **Matroska 音频** | `.mkv`, `.mka` | 有损/无损 | 不适用 | 不适用 | MKV 容器内的音频流（EAC3-JOC、DTS 等） |
| **OPA** | `.opa` | 无损 | 不适用 | 不适用 | 优化的音频格式（基于 OPUS） |
| **Vorbis** | `.ogg`, `.oga` | 有损 | 不适用 | 最高 192kHz | Ogg Vorbis 音频 |
| **Opus** | `.opus`, `.ogg` | 有损/无损 | 不适用 | 最高 48kHz | 现代、高效的编解码器 |
| **WAV** | `.wav` | 无损 | 最高 32 位 | 最高 192kHz | 未压缩的 PCM 音频 |
| **PCM** | 多种 | 无损 | 最高 32 位 | 最高 192kHz | 原始音频数据 |
| **AIFF** | `.aiff`, `.aif` | 无损 | 最高 32 位 | 最高 192kHz | Apple 音频交换格式 |
| **MIDI** | `.mid`, `.midi` | 指令 | 不适用 | 不适用 | 乐器数字接口（Musical Instrument Digital Interface） |

---

## 🎬 FFmpeg 解码格式

这些格式由 FFmpeg 扩展（`EXTENSION_RENDERER_MODE_PREFER`）解码，需在「解码器扩展」设置中保持启用。扩展表涵盖 **AC-4、E-AC-3 / EAC3-JOC (Dolby Atmos)、AC-3、WMA / WMA-Pro / Lossless、ALAC、DTS / DTS-HD MA、Dolby TrueHD**；Standard MIDI 走 `midi` 扩展：

| 格式 | 容器 | 音质 | 位深 | 采样率 | 备注 |
|:---:|:---:|:---:|:---:|:---:|:---|
| **APE** | `.ape` | 无损 | 最高 24 位 | 最高 192kHz | Monkey's Audio（高压缩率） |
| **WavPack** | `.wv` | 无损 | 最高 32 位 | 最高 192kHz | 混合无损/有损格式 |
| **TAK** | `.tak` | 无损 | 最高 24 位 | 最高 192kHz | TOM's lossless Audio Kompressor |
| **TTA** | `.tta` | 无损 | 最高 24 位 | 最高 96kHz | True Audio 无损 |
| **WMA** | `.wma` | 有损 | 不适用 | 最高 48kHz | Windows Media Audio |
| **WMA Lossless** | `.wma` | 无损 | 最高 24 位 | 最高 96kHz | Windows Media Audio Lossless |
| **AC-3 (Dolby Digital)** | `.ac3`, `.m4a` | 有损 | 不适用 | 最高 640kbps | 通过 FFmpeg 解码；也可能使用硬件解码器 |
| **EAC3-JOC (Dolby Atmos)** | `.eac3`, `.m4a`, `.mkv` | 有损 | 不适用 | 最高 6Mbps | 通过 FFmpeg 解码；立体声/环绕声输出 |
| **AC-4 (Dolby AC-4)** | `.ac4` | 有损 | 不适用 | 最高 448kbps | 通过 FFmpeg 扩展解码 |

---

## ⚠️ 取决于设备的格式

这些格式需要硬件支持，并非在所有设备上都能工作：

| 格式 | 容器 | 备注 |
|:---:|:---:|:---|
| **DTS** | `.dts`, `.m4a` | 需要兼容的设备/硬件解码器 |
| **DTS-HD MA** | `.dts`, `.m4a` | 高分辨率多声道 DTS |
| **DTS:X** | `.dts`, `.m4a` | 基于对象的 DTS 环绕声 |
| **DSD** | `.dsd`, `.dsf`, `.dff` | Super Audio CD；需要兼容的 DAC |

### 检查设备兼容性

要检查你的设备是否支持 Dolby/DTS：
1. 在 Rhythm 中尝试播放一个测试文件
2. 如果能播放，说明你的设备具备必要的解码器
3. 如果失败，请转换为受支持的格式（见下文）

---

## 📊 音质检测

Rhythm 会自动检测并在播放器中显示音质徽章，并在“歌曲”标签页中提供**基于音质的音乐库筛选**：

### 音质徽章

| 徽章 | 标准 | 格式 |
|:---:|:---|:---|
| **STUDIO MASTER** | ≥24 位/192kHz 无损 | FLAC、ALAC、WAV、AIFF |
| **HI-RES LOSSLESS** | ≥48kHz 无损（非 Studio Master） | FLAC ≥48kHz、ALAC ≥48kHz |
| **CD QUALITY** | 16 位/44.1kHz 无损 | FLAC、ALAC、WAV、PCM、AIFF |
| **LOSSLESS** | 标准无损压缩 | FLAC、ALAC、WAV、PCM、APE、WV、TAK、TTA、AIFF |
| **DSD** | Direct Stream Digital | DSD、DSF、DFF |
| **DOLBY ATMOS** | Dolby Atmos / TrueHD | EAC3-JOC、TrueHD |
| **DOLBY DIGITAL PLUS** | Enhanced Dolby | E-AC-3 |
| **DOLBY DIGITAL** | 标准 Dolby | AC-3 |
| **DTS** | DTS 环绕声 | DTS、DTS-HD MA、DTS:X |
| **LOSSY** | 任何有损压缩 | MP3、AAC、OGG、Opus、WMA |

### 基于音质的音乐库筛选

音乐库界面的“歌曲”标签页提供动态筛选标签，仅在存在匹配曲目时显示：

| 筛选类别 | 显示内容 |
|:---|:---|
| **Studio Master** | 24 位/192kHz+ 无损曲目 |
| **Hi-Res Lossless** | ≥48kHz 无损（不含 Studio Master、DSD、Dolby） |
| **CD Quality** | 16 位/44.1kHz 无损 |
| **Lossless** | 标准无损（不含高解析度、CD 音质、DSD、Dolby） |
| **Lossy** | 所有有损压缩曲目 |
| **DSD** | DSD/DSF/DFF 曲目 |
| **Dolby Atmos** | Dolby Atmos 或 TrueHD 曲目 |
| **Dolby Digital Plus** | E-AC-3 曲目 |
| **Dolby Digital** | AC-3 曲目 |
| **DTS** | DTS、DTS-HD MA、DTS:X 曲目 |
| **Dolby / Surround** | 任何环绕声（Dolby、DTS、多声道） |
| **High Quality** | 有损 ≥320kbps（非 Dolby/环绕声） |
| **Standard** | 有损 128–319kbps（非 Dolby/环绕声） |
| **Mono** | 单声道曲目 |
| **Short / Medium / Long** | 按时长筛选 |

### 音质检测详情

- **无损检测**：基于编解码器类型 + 文件扩展名回退判断
- **高解析度检测**：采样率 ≥48kHz 或计算位深 ≥18 位
- **Studio Master 检测**：采样率 ≥192kHz，或在 ≥96kHz 下计算位深 ≥22 位
- **CD 音质检测**：无损、采样率 ≤48kHz、计算位深 <20 位
- **Dolby/DTS 检测**：从元数据识别编解码器
- **容器检测**：从文件头解析

---

## 🔧 技术限制

### 容器格式的歧义
某些容器可以容纳多种编解码器：
- `.m4a` 可包含：AAC、ALAC、AC-3、E-AC-3
- `.ogg` 可包含：Vorbis、Opus、FLAC
- `.mp4` 可包含：AAC、AC-3、视频轨

Rhythm 识别的是内部的**实际编解码器**，而不仅仅是容器。

### MP4 / Matroska 音频（.mp4/.mkv/.mka）
Rhythm 会扫描并播放嵌入在 MP4 和 Matroska 容器中的音频流（包括 `.mka` 纯音频文件）。在媒体扫描时，`video/mp4`、`video/x-matroska` 和 `application/x-matroska` MIME 类型会与常规音频文件一同建立索引。从这些容器中提取的文件通过其**实际编解码器**（AAC、ALAC、AC-3、E-AC-3、DTS 等）识别，而不仅仅依据容器扩展名。

> ⚠️ **注意：** 如果**允许的格式**设置（设置 → 音乐库与媒体 → 媒体扫描 → 允许的格式）过滤掉了 MP4/MKV，它们会被排除在默认音乐库之外 —— 可在该处切换 `mp4`/`mkv` 以包含或排除。

### 硬件依赖
- **Dolby/DTS**：需要特定设备的硬件解码器
- **采样率**：受 Android AudioTrack 限制（通常最高 192kHz）
- **位深**：大多数 Android 设备支持 16 位或 24 位
- **声道配置**：立体声普遍支持，多声道因设备而异

### ExoPlayer 能力
- **ExoPlayer 1.11.1 + FFmpeg 扩展**：在默认支持之外增加 EAC3-JOC、E-AC-3、AC-3、AC-4、WMA、ALAC、DTS / DTS-HD MA、Dolby TrueHD 解码；扩展可在「解码器扩展」设置中整体或逐项关闭
- **无缝播放**：支持 MP3、AAC、FLAC、Opus
- **进度跳转**：大多数格式精确，部分流媒体编解码器为近似值
- **元数据**：取决于容器格式（MP3 使用 ID3，FLAC 使用 Vorbis 注释）
- **专辑封面**：FLAC、MP3、M4A 中的内嵌图片

---

## 💡 格式建议

### 追求最大兼容性
✅ **使用这些格式**：
1. **FLAC** - 通用的无损支持
2. **MP3** - 通用的有损支持（320kbps 或 V0 以保证音质）
3. **AAC** - 现代有损格式（推荐 256kbps）

### 无损音频音乐库
✅ **FLAC 是黄金标准**：
- 在所有平台上广泛支持
- 开源且免版税
- 压缩出色（约为 WAV 体积的 50-60%）
- 完美保留所有音频数据
- 支持内嵌封面和元数据

### 高解析度音频
✅ **FLAC 24 位/96kHz 或更高**：
- 先检查设备的 DAC 能力
- 大多数现代智能手机支持最高 192kHz
- 对大多数听者而言，超过 96kHz 收益递减
- 使用高品质耳机/扬声器才能体会差异

### 节省空间
✅ **现代有损格式**：
- **Opus** @ 128-160kbps：每比特率音质最佳
- **AAC** @ 256kbps：音质出色、兼容性广
- **MP3** @ 320kbps 或 V0：对大多数听者依然出色

---

## 🛠️ 转换不支持的格式

### 推荐转换工具

#### 桌面端（Windows/Mac/Linux）
1. **[FFmpeg](https://ffmpeg.org/)**（命令行）
   ```bash
   # APE to FLAC
   ffmpeg -i input.ape output.flac
   
   # DSD to FLAC
   ffmpeg -i input.dsf output.flac
   
   # AIFF to WAV
   ffmpeg -i input.aiff output.wav
   ```

2. **[dBpoweramp](https://www.dbpoweramp.com/)**（Windows/Mac - 付费）
   - 用户友好的图形界面
   - 批量转换
   - 保留元数据

3. **[foobar2000](https://www.foobar2000.org/)**（Windows - 免费）
   - 出色的转换器
   - 广泛的格式支持
   - 免费且轻量

#### 在线（基于网页）
- **[CloudConvert](https://cloudconvert.com/)**：支持 200+ 种格式
- **[Online-Convert](https://www.online-convert.com/)**：音频转换
- **[Zamzar](https://www.zamzar.com/)**：多种音频格式

⚠️ **隐私警告**：在线转换器会上传你的文件。敏感/个人音频请使用桌面工具。

### 批量转换脚本

适用于拥有大量文件的 Linux/Mac 用户：

```bash
#!/bin/bash
# Convert all APE files to FLAC in current directory
for file in *.ape; do
    ffmpeg -i "$file" "${file%.ape}.flac"
done
```

```bash
# Convert all DSD files to FLAC (24-bit/88.2kHz)
for file in *.dsf; do
    ffmpeg -i "$file" -sample_fmt s32 -ar 88200 "${file%.dsf}.flac"
done
```

---

## 🎯 播放问题排查

### 文件无法播放
1. **检查格式**：确认后缀在媒体库白名单中
2. **检查解码器扩展**：在「解码器扩展」设置中确认总开关已启用、对应扩展没有被单独禁用
3. **检查编解码器**：使用 MediaInfo 或 FFprobe 识别实际编解码器
4. **在设备上测试**：尝试在其他应用中播放（VLC、YouTube Music）
5. **转换格式**：使用 FFmpeg 转换为 FLAC 或 MP3

### Dolby/DTS 文件无法播放
- **设备不支持**：转换为 FLAC 或 AAC
- **查看设备规格**：在制造商规格中查找 Dolby Atmos/DTS 支持
- **尝试其他应用**：测试 VLC 或其他播放器是否能播放

### 音质不佳
- **检查比特率**：低比特率 = 音质差
- **确认音源**：确保原始文件是高品质的
- **使用均衡器**：为你设备应用 AutoEQ 预设
- **检查输出设备**：音质受耳机/扬声器限制

### 元数据未显示
- **重新标记文件**：使用 MP3Tag 或 Picard 修复元数据
- **重新扫描音乐库**：Settings → Library & Media → Media Scan
- **检查文件权限**：确保 Rhythm 拥有读取权限

---

## 📚 其他资源

### 格式信息
- [Wikipedia: Audio Coding Format](https://en.wikipedia.org/wiki/Audio_coding_format)
- [Hydrogen Audio Wiki](https://wiki.hydrogenaud.io/)
- [ExoPlayer 支持的格式](https://exoplayer.dev/supported-formats.html)

### 元数据工具
- **[Mp3tag](https://www.mp3tag.de/)** - Windows/Mac 元数据编辑器
- **[MusicBrainz Picard](https://picard.musicbrainz.org/)** - 自动标记工具
- **[Kid3](https://kid3.kde.org/)** - 跨平台标签编辑器

### 音频分析
- **[MediaInfo](https://mediaarea.net/MediaInfo)** - 技术文件信息
- **[FFprobe](https://ffmpeg.org/ffprobe.html)** - 命令行媒体分析器
- **[Spek](https://www.spek.cc/)** - 声学频谱分析仪

---

**有疑问？** 查看 [FAQ](FAQ.md)。
