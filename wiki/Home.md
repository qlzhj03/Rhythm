> ℹ️ **文档状态**：本 wiki 已按 2026-11 多模块拆分与唯一包名重构后的代码更新。
> 约定与红线见根目录 [AGENTS.md](../AGENTS.md)，设计与重构历史见 [docs/](../docs/)；一切事实以源码为准。

# Rhythm 音乐播放器 Wiki

欢迎来到 Rhythm 音乐播放器官方文档。Rhythm 是一款基于 Material 3 Expressive 的单 Activity Compose 音乐播放器，支持本地曲库与自托管流媒体。

## 📚 用户文档

- [安装指南](Installation-Guide.md) - 系统要求、获取与安装 APK
- [快速入门](Getting-Started.md) - 首次设置与基础用法
- [功能总览](Features.md) - 完整的功能列表与能力介绍
- [音频格式](Audio-Formats.md) - 支持的格式与技术细节
- [权限指南](Permissions.md) - 应用申请的权限及用途
- [桌面小部件](Widgets.md) - 4 款小部件与快捷磁贴
- [故障排查](Troubleshooting.md) - 常见问题与解决方案
- [常见问题](FAQ.md) - 常见问题解答

## 🛠️ 开发者文档

- [架构指南](Architecture.md) - 模块分层、依赖方向、状态与导航
- [技术栈](Technology-Stack.md) - 技术架构、依赖库与构建配置
- [API 文档](API-Documentation.md) - 内部接口、服务与数据层
- [构建说明](Build-Instructions.md) - 从源码构建与本地验证
- [参与贡献](Contributing.md) - 代码规范、测试与 PR 流程

## 🎵 关于 Rhythm

- 🎨 Material 3 Expressive 与基于壁纸的动态 Material You 主题
- 🎵 Media3 ExoPlayer 播放引擎，支持响度均衡(ReplayGain)、交叉淡入淡出、10 段 EQ 与 6032 条 AutoEQ 预设、单声道音频
- 🎤 同步歌词：逐词高亮、全屏视图、歌词小部件，多来源优先级
- 📊 Rhythm Stats 播放统计与 Rhythm Guard 听力保护
- 🌐 Go 模式：本地 + 流媒体（Subsonic 兼容服务含 Navidrome、Jellyfin、网易云音乐），支持附近服务器发现与离线下载
- 📱 平板多栏布局、折叠屏姿态感知播放器
- 🔒 无分析与跟踪，数据保存在设备本地；可选联网功能默认关闭

## 📌 权威来源

| 内容 | 位置 |
|:---|:---|
| 约定、红线、命令 | [AGENTS.md](../AGENTS.md) |
| 模块与导航装配 | `settings.gradle.kts`、`app/src/main/java/com/qlzhj/rhythm/app/navigation/` |
| 设计与重构历史 | [docs/](../docs/) |
| 依赖版本 | `gradle/libs.versions.toml` |
| 资源与字符串 | `core/designsystem/src/main/res/values/` 与 `values-en/` |

> `wiki/` 面向用户与贡献者的入门说明；涉及代码细节时，以源码、Version Catalog 与 `AGENTS.md` 为准。
