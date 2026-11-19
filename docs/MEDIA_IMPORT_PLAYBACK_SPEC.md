# 媒体导入与播放技术设计

本文件约束视频/音频课程的端到端实现。当前媒体流程未通过真机验收，本文是目标设计与测试契约，不代表功能已完成。

## 1. 用户流程

1. 用户进入「课程管理 → 导入课程 → 视频/语音」。
2. 用户选择“选择文件夹”或“选择文件”。
3. 系统文件选择器返回 SAF content URI。
4. 应用验证并持久化 URI READ 授权。
5. 扫描目录/文件，显示真实扫描进度与文件列表。
6. 用户确认课程名称、排序和要导入的媒体。
7. 应用创建课程/章节，并记录每个媒体资源的 URI 与元数据。
8. 如用户开启“保存到应用内”，后台复制并校验文件；复制失败的章节仍保留原始 URI，显示部分成功状态。
9. 用户进入课程后打开章节，播放器从原 URI 或本地副本播放。
10. 播放进度、播放状态和媒体引用按稳定 ID 保存；重新进入时恢复。

“上传”是网络传输概念。本产品本地导入不上传服务器，界面统一使用“选择文件”“导入文件”“保存到应用内”。

## 2. SAF 授权契约

### 文件夹
- 使用 `ACTION_OPEN_DOCUMENT_TREE`。
- 读取 result Intent 的实际 flags；对系统返回的 READ/WRITE grant 仅保留必要权限。
- 调用 `takePersistableUriPermission` 时记录成功/失败，不得吞掉异常。
- 保存 tree URI 与授权 flags；启动时核对 `persistedUriPermissions`。
- 失效时提供“重新连接文件夹”，不能静默显示空列表。

### 单文件/多文件
- 使用 `ACTION_OPEN_DOCUMENT` 或 `OpenMultipleDocuments`，根据用户动作选择单选/多选。
- 保存每个 URI 的 READ grant；不将 URI 转成路径。
- 使用 ContentResolver 查询 MIME、DISPLAY_NAME、SIZE、LAST_MODIFIED；字段缺失时有 fallback。
- 对每个 URI 执行可打开性探测；错误需要按文件返回。

## 3. 目录扫描器

建议接口：
```kotlin
interface MediaDocumentGateway {
    suspend fun pickFolder(): Result<GrantedTree>
    suspend fun pickFiles(kind: MediaKind, multiple: Boolean): Result<List<GrantedDocument>>
    suspend fun scanTree(tree: GrantedTree, kind: MediaKind, progress: (ScanProgress) -> Unit): Result<ScanReport>
    suspend fun inspect(document: GrantedDocument): Result<MediaMetadata>
    suspend fun open(document: GrantedDocument): Result<InputStream>
}
```

扫描约束：
- 使用 DocumentsContract / DocumentFile 访问树内容，支持递归。
- 深度、总文件数和耗时上限可配置；提供取消信号。
- 目录 URI 去重，查询失败逐项记录，不让一个坏目录中断整个扫描。
- 识别规则结合 MIME 与扩展名；最终必须能打开输入流。
- 默认自然排序；时间排序必须定义时间缺失时的稳定次序。
- 过滤临时文件、隐藏文件和零字节文件时，UI 显示跳过原因。
- ScanReport 至少包含 found / supported / rejected / inaccessible / skipped / failures。

## 4. 导入状态与结果

```text
Idle
  → Picking
  → PermissionGranted
  → Scanning
  → Reviewing
  → Importing
  → Ready | PartialSuccess
任意阶段 → Cancelled | Failed(retryable, reason)
```

导入过程必须幂等：用户重复点击不能创建两门重复课程；同一文件不能意外生成多个章节。创建课程和媒体引用写入应在一次可恢复的事务/快照更新中完成。

## 5. 媒体引用模型

```kotlin
data class MediaAsset(
    val id: String,
    val courseId: String,
    val chapterId: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val lastModifiedMillis: Long?,
    val sourceUri: String,
    val treeUri: String?,
    val persistedReadGrant: Boolean,
    val localCopyPath: String?,
    val durationMillis: Long?,
    val importState: MediaImportState,
    val lastError: String?
)
```

- 稳定 ID 不依赖列表序号，排序或重命名不能让进度错配。
- URI 是外部引用，不保证永久有效；必须支持重新连接。
- 应用内副本要用临时文件 + 长度/校验值验证 + 原子替换。
- 删除课程仅删除本应用副本，不删除用户源文件。

## 6. 播放器边界

`PlaybackController` 独占播放器实例，提供：
- `prepare(asset)`
- `play()` / `pause()` / `seekTo(position)`
- `setSpeed(rate)`
- `release()`
- `StateFlow<PlaybackState>`

PlaybackState 至少包含 Idle、Preparing、Ready、Playing、Paused、Ended、Error。错误携带可读说明、底层原因类别、是否可重试。

优先评估 AndroidX Media3 ExoPlayer，理由是统一音视频 API、生命周期/播放状态管理和多格式适配；但格式能否播放仍取决于设备解码器与容器/编码组合。不可承诺“任意视频格式都支持”。

页面不得直接持有 VideoView/MediaPlayer/ExoPlayer 实例，不得自己创建无限轮询 ticker。播放进度由 Controller 节流发布，Repository 负责持久化。

## 7. 端到端验收表

| ID | 场景 | 通过标准 |
|---|---|---|
| M01 | 选择单个 MP4 | 能选中、登记、显示章节并播放 |
| M02 | 多选 MP4 | 每个有效文件均成为章节，数量准确 |
| M03 | 文件夹导入 | 根目录和多层子目录均被扫描，进度准确 |
| M04 | 音频导入 | MP3/M4A/WAV 常见设备支持格式可播放 |
| M05 | 授权持久化 | 关闭应用重开仍可读取；权限失效有重连提示 |
| M06 | 用户取消 | 不创建空课程、不破坏已有课程 |
| M07 | 复制到应用内 | 文件完整性验证通过，重启后可播放 |
| M08 | 复制失败 | 显示部分成功和具体文件错误，可重试 |
| M09 | 无扩展名/MIME 异常 | 能按实际可读性判断，不误报空目录 |
| M10 | 不支持编码 | 明确提示设备不支持的格式/编码 |
| M11 | 进度/倍速/重播 | 进度按媒体稳定 ID 保存，结束后重播从头开始 |
| M12 | 生命周期 | 切页、后台、旋转、退出时状态符合产品策略且无泄漏 |

## 8. 当前代码的调查结论

从当前代码静态检查已确认：
- `MainActivity.pickMedia` 使用 `OpenMultipleDocuments`，即便 UI 按钮文字为“改选单个文件”，实际系统选择器仍是多选入口。
- `MediaCourses.fromUris` 目前主要用扩展名判断类型；MIME 正确但无扩展名的文件可能被过滤。
- `pickTree` 的授权持久化异常被 `runCatching` 忽略，缺少授权失败状态回传。
- `MediaCourses.scan` 的异常通过外层 Result 传回，但单个目录查询异常没有完整错误报告，用户可能只看到“没有文件”。
- 创建课程只保存 URI/章节数据；应用内复制取决于 mediaKeep，复制失败时课程记录仍然建立。
- 当前没有真机日志或复现步骤，尚不能断言用户设备上的具体失败根因。

因此第一轮媒体框架实现应先补全授权/扫描/导入状态与诊断报告，再实现播放器适配；不得先做 UI 样式掩盖导入失败。
