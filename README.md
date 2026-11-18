# 桌面运维学习 · Android 原生版

本仓库正在进行原生 Android 重构，应用不再使用 WebView 加载 HTML。当前开发分支为 `native-android-rebuild`，原有 `main` 分支保持不变。

## 技术架构

- Kotlin + Jetpack Compose：原生页面、导航和主题
- AndroidX Media3 / ExoPlayer：本地视频与音频播放
- Android Storage Access Framework：用户主动选择文件/文件夹并持久化授权
- Android TextToSpeech：系统朗读、语速/音调、可用音色选择
- SharedPreferences + JSON：本地保存课程、媒体索引、摘录和学习记录
- GitHub Actions：手动触发 APK 构建，构建后执行模拟器安装与启动检查

## 已实现的原生功能

- 学习首页、课程列表、课程详情、媒体资料库和个性化设置
- 课程新增、编辑、删除
- 章节新增、编辑、删除；章节正文、完成状态和课程完成比例
- 章节关联已导入的视频/音频文件
- 从文件选择器导入多个视频/音频，或递归扫描所选文件夹
- 媒体库按文件名称或文件修改时间排序
- 将筛选后的视频/音频批量生成课程，媒体文件按当前排序生成章节
- Media3 原生播放器、快退/快进、0.75× 至 2× 倍速
- 课程章节媒体播放位置保存与继续播放
- 播放器错误提示；具体可播放格式仍取决于设备解码器
- 章节正文朗读、系统音色读取/选择、语速和音调调节
- 跳转 Android 系统 TTS 设置，安装或切换系统语音服务/语音数据
- 章节内容摘录收藏与删除
- 播放学习时长记录、近 7 天学习统计
- 完整 JSON 备份与恢复：课程、章节、媒体索引、摘录和学习记录
- 多主题色和深色模式

## 构建 APK

构建工作流已改为手动触发，开发期间提交代码不会自动启动耗时的 APK 构建。

1. 打开 GitHub 仓库的 Actions。
2. 选择 **Build Native Android APK**。
3. 点击 **Run workflow**，选择 `native-android-rebuild` 分支。
4. 构建成功后，在 Artifacts 下载 `deskops-native-debug-apk`，解压并安装 `app-debug.apk`。

## 测试与限制

当前正在继续完善和检查原生代码。功能代码提交不代表已经通过 Android 编译或真机验证；最终 APK 只有在完整构建、模拟器安装启动检查通过后才会交付。不同手机的系统 TTS 引擎、语音数据和视频编码器存在差异，仍需在实际设备上验证。
