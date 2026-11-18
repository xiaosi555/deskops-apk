# 桌面运维全能学习导师 · Android 原生版

由 Web 单文件应用 `桌面运维_星球主题版261113_v6.html` 逆向移植的原生工程。**功能与 UI 与 Web 版保持一致**，学习存档格式互通，可双向导入导出。

## 1 构建

要求：Android Studio Hedgehog+（或 Gradle 8.2+ / JDK 17）。

```bash
# 目录内未附带 gradle wrapper jar，二选一
gradle wrapper --gradle-version 8.4      # 生成 wrapper
./gradlew :app:assembleDebug             # 构建 debug apk
# 或直接用 Android Studio 打开本目录，Sync 后 Run
```

- `compileSdk 34` / `minSdk 24` / `targetSdk 34`
- 依赖仅 AndroidX：core-ktx、appcompat、activity-ktx、fragment-ktx、recyclerview、constraintlayout
- 无网络、无统计、无云同步、无 ExoPlayer、无图片库；`assets/course_ops.json`（217KB）为内置课程数据

## 2 目录

```text
app/src/main/
├─ AndroidManifest.xml
├─ assets/course_ops.json              # 7 模块 / 30 章 / 219 知识详解 / 84 输出样例 / 4 路线
├─ java/com/qwen/opsmentor/
│  ├─ App.kt                            # 全局：Store + PackRegistry + 主题 + 计时 + 朗读
│  ├─ core/  Tokens Dsl Components Sheets Selection MarkEngine AnnotatedText Icons Pages Reader StudyTimer
│  ├─ data/  Pack PackRegistry Lecture
│  ├─ state/ Store Records Courses ReviewEngine
│  ├─ io/    Exporter Importer Saver
│  ├─ media/ MediaCourses                # SAF 视频 / 音频课程建课、副本、首帧封面
│  └─ ui/    MainActivity Page Rows MiniBar Editor CropEditor ShortcutsEdit
│     └─ pages/ 20 个页面（课程 / 我的 / 课程详情 / 章节学习 / 知识详解 / 复习队列 / 案例库 /
│                术语手册 / 排障手册 / 学习进度 / 我的划线两层 / 数据 / 个人数据 / 学习日历 /
│                课程管理 / 导入课程 / 设置 / 快捷入口 / 播放页）
└─ res/ drawable（87 个由 Web SVG path 自动转换的 Vector Drawable）+ values + xml + mipmap
tools/
├─ build-assets.cjs    # 从基准 HTML 抽取课程数据与图标（node tools/build-assets.cjs）
├─ ktcheck.py         # Kotlin 括号 / 引号配平自检
└─ xref.py            # 包内符号交叉引用自检
```

## 3 与 Web 版的对应关系

| Web | Android |
| --- | --- |
| `localStorage dlmentor.v3` 单 JSON | `SharedPreferences` 同键同结构（`state/Store.kt`），存档可互导 |
| CSS 变量 `--l0..--l3 --pri` | `core/Tokens.kt` 运行期计算（含 WCAG on-pri、暗色降字重、双风格、9 色板） |
| `cubic-bezier(.22,.74,.18,1)` / `(.32,1.34,.5,1)` | `SNAP` / `POP` PathInterpolator |
| `renderXxx()` 动态建 DOM | `Page.body(host)` 动态建 View，整页重绘 |
| `[data-blk]` + `segs{sig,s,e,txt,pre,post}` | `AnnotatedText` + `MarkEngine`（FNV-1a 签名 + 上下文打分纠偏） |
| 浮动划线工具条（可拖拽、位置持久化） | `SelectionBar`（长按命中整句 + 按字符微调 + 整段） |
| Blob 下载 / File System Access | SAF `CreateDocument` / `OpenDocumentTree` + 权限持久化 |
| Web Speech TTS | `android.speech.tts.TextToSpeech` |
| IndexedDB 文件副本 | `filesDir/media/<courseId>/` 副本 + `MediaMetadataRetriever` 首帧 |
| 打印版手册 | 复制全文 / 系统分享（PrintManager 待补，见 PRD §16.2） |

## 4 已实现 / 待补

已实现：PRD §13.1 的 10 条主流程走查项；全部 19 个页面路由；划线引擎与分层绘制；间隔提取；案例五栏与编号规则；术语手册；排障手册；学习进度；学习日历（日/周/月/年 + 自动计时 + 校正）；课程管理与导入（文字 JSON/MD/CSV、视频/音频文件夹）；数据 6 类档案 × 3 格式导出预览与导入合并/覆盖；主题色 / 明暗 / 星球版·极简版 / 12 种顶部装饰；迷你继续学习条横滑丢弃；朗读与媒体互斥。

待补（V1.1，见 PRD §16.2）：跨段落合并为一条划线、视频手动选帧封面、PrintManager 打印版、TTS 引擎/音色选择、动态装饰着色器级还原、API 31+ 背景模糊。

## 5 静态自检

```bash
python3 tools/ktcheck.py app/src/main/java   # 括号 / 字符串配平：0 issues
python3 tools/xref.py app/src/main/java      # 包内符号引用检查
```

本工程的静态检查已通过；未在构建环境（无 Android SDK）中做真机编译验证，首次在 Android Studio 中 Sync 时若有个别 API 版本兼容告警，按提示微调即可。
