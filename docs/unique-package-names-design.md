# 唯一包名重构:每模块独立包根 + 应用身份切换(短稿)

- 日期:2026-11-08 · 状态:已批准并落地(P1–P14 共 14 个本地 commit,未 push)
- 来源:多模块架构梳理发现「12 模块共用 `chromahub.rhythm.app` 包根、边界纯靠构建脚本与约定维护」,用户拍板包根前缀 `com.qlzhj.`、applicationId 切 `com.qlzhj.rhythm`
- 范围:12 个生产模块包根迁移 + applicationId 切换 + R8 keep 规则重写 + `android.uniquePackageNames=true` 强制;macrobenchmark 包不动(已唯一,benchmark 包语义指向 applicationId)

## 意图与动机

C 项「名实归位」完成后目录 = 声明包,但 12 个模块仍共用同一包根,重复包段 14 组(`util`、`shared.presentation.*`、`core.domain.scan` 等),import 无法区分模块,构建不拦重复包名(`android.uniquePackageNames=false` 维持)。目标:**import 即定位「模块 + 文件」**,包根 = 模块身份,AGP 构建期强制唯一。

## 目标 / 非目标

- **目标**:映射表(附录 A)机械迁移;applicationId 切换;keep 规则重写;`uniquePackageNames=true` 收口;文档与 AGENTS.md 更新。
- **非目标**:`shared.*` / `features.*` / `util` 历史领域段重组(C 项已定性「另一个量级」);macrobenchmark 迁移;transitive R → nonTransitive R 切换(性能议题另行评估);品牌字符串(Team ChromaHub 等)不动。

## 既定决策

1. **applicationId 与代码包分离**:`applicationId = com.qlzhj.rhythm`(debug 后缀 `.debug`),代码包根 `com.qlzhj.rhythm.*`。后果:旧 `chromahub.rhythm.app` 安装无法原地更新(用户明确接受);provider authority 均为 `${applicationId}` 占位符、运行时读取全走 `context.packageName`,零手工适配;Room 无 schema 导出,新身份全新数据目录,无迁移链断裂。
2. **自定义协议串不改**:`chromahub.rhythm.app.action.* / broadcast.* / extra.*`(28 个 MediaPlaybackService 常量、快捷方式 action、widget 通信)为任意标识符,收发双方均在自身代码内;保持字面前缀,列入零残留审计白名单。`StatusBroadcaster.PACKAGE_NAME`(未使用常量)与 `AppBuildInfo.applicationId` 默认值随身份改为 `com.qlzhj.rhythm`。
3. **manifest 零改动**:4 个源码 manifest 全用相对类名,随各自 namespace 自动重绑(此前 grep 到的 FQCN 全来自 build/ 中间产物)。
4. **transitive R 维持**(`android.nonTransitiveRClass=false`):各模块 R 迁至自身 namespace,跨模块资源引用语义不变;`getLocalizedPresetName` 类顶层声明经各模块自身 R 解析。
5. **剥段规则**:旧包前导段与模块自身路径重复时剥离(如 feature:local 的 `features.local.*` → `feature.local.*`),避免双重段;其余历史领域段原样保留。core:network 内部首段 `network` 不匹配剥离模式,形成 `core.network.network.*`(按表执行,留观)。

## 执行结构(P1–P14,自底向上每阶段可编译)

core:common → model → designsystem → network → data → media → ui → feature:widget → streaming → local → settings → :app(+applicationId)→ keep 规则 + `uniquePackageNames=true` → 文档。每阶段 `:app:compileDebugKotlin` + 触及模块单测;P13 全量:assembleDebug(配置期唯一性检查)+ 8 模块 JVM 单测 + assembleRelease(R8)+ 复数脚本。

## 实现中发现并解决的边角(后续同类迁移的备忘)

1. **多归属包前缀改写歧义**:重复包段下按前缀归属会错改,必须按「类实际归属」解析——按 (包路径, 文件 stem) 索引,多归属时下一段须命中本模块 stem 或**顶层声明**(Kotlin 顶层 fun/val 以 `包.成员` 引用,文件 stem 覆盖不了,如 `formatDuration`、`getLocalizedPresetName`;泛型接收者扩展函数 `fun <T> List<T>.x()` 需专门正则)。
2. **同包跨模块免 import 引用**:Kotlin 同名包在不同模块间可裸引用,拆分后包名分离即断。`fix_split_package_refs.py` 检测未迁移文件对已迁移符号的裸引用并自动补 import;需排除文件内同名局部声明(存量大文件存在函数体顶格的损坏缩进,如 MediaPlaybackService 的 `createNotificationChannel` 体内 `val channel` 顶格,会被误当顶层声明)与 private 顶层声明(本就不可跨文件导入)。
3. **通配符 import 三类**:指向本模块自有包(确定性,直接重写);指向单一外部归属(重写);指向多归属包(app 的 3 处 `screens.settings.*` 同时服务 core:ui 脚手架与 feature:settings 屏幕)——按可见性拆成两条,且 feature:settings 未迁移前新包不存在,须待 P11。
4. **重载消解行为变化**:同名顶层函数(本地 3 参包装 vs 库内 4 参全参)在库迁走后只剩本地候选,原靠通配符引入的重载消失,调用点参数不匹配报错(如 `TunerAnimatedSwitch` 的 `enabled`)。
5. **UTF-8 BOM 文件**:BOM 使 `^package` 锚点失配,包行漏改(VersionCodeDowngradeWarningDialog.kt 一例);脚本已做 BOM 兼容。
6. **增量构建缓存**:模块内类改名后 Hilt 增量生成物仍引用旧 FQCN(`hiltJavaCompileDebug` 找不到旧 Application 类),`./gradlew :app:clean` 即清。

## 零残留审计(P14 收口)

- 非字符串字面量位置的 `chromahub.rhythm.app.`:仅剩 macrobenchmark(按计划不动)。
- 字符串字面量 51 处 = 受保护协议串 + applicationId 常量(白名单)。
- build 脚本 / gradle.properties / manifest / XML 资源:零引用(资源名 `settings_about_team_chromahub` 为品牌文案,非包引用)。

## 附录 A:映射表

| 模块 | 新包根 | 内部段剥离 |
|---|---|---|
| :app | com.qlzhj.rhythm.app | 无 |
| :core:common | com.qlzhj.rhythm.core.common | `core.common` |
| :core:model | com.qlzhj.rhythm.core.model | `core.model` |
| :core:designsystem | com.qlzhj.rhythm.core.designsystem | `core.designsystem` |
| :core:network | com.qlzhj.rhythm.core.network | 无(→ `core.network.network.*`,留观) |
| :core:data | com.qlzhj.rhythm.core.data | 无 |
| :core:media | com.qlzhj.rhythm.core.media | 无 |
| :core:ui | com.qlzhj.rhythm.core.ui | 无 |
| :feature:local | com.qlzhj.rhythm.feature.local | `features.local` |
| :feature:streaming | com.qlzhj.rhythm.feature.streaming | `features.streaming` |
| :feature:settings | com.qlzhj.rhythm.feature.settings | 无 |
| :feature:widget | com.qlzhj.rhythm.feature.widget | 无 |
| :macrobenchmark | 不动(`chromahub.rhythm.app.macrobenchmark` namespace 已唯一) | — |

## 附录 B:工具归档

`scripts/relocate_packages.py`(单模块迁移:索引 → 引用改写 → git mv → 目录清理,支持 `--check / --dry-run / --residual`)与 `scripts/fix_split_package_refs.py`(同包拆分裸引用补 import,`--apply` 落盘)。一次性工具,包根硬编码;后续如再做包级迁移需按新映射调整 `CONFIG` 与 `NEW_BASE`。
