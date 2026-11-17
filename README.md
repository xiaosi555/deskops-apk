# 桌面运维学习 · Android 原生版

本仓库正在将应用重构为真正的 Android 原生应用，不再使用 WebView 加载旧 HTML。

## 技术架构

- Kotlin + Jetpack Compose：原生页面、导航和主题系统
- AndroidX Media3 / ExoPlayer：原生视频与音频播放
- Storage Access Framework：用户主动选择文件或文件夹，不申请宽泛存储权限
- Android TextToSpeech：系统语音朗读
- SharedPreferences（当前基础版本）：保存课程和媒体资料索引
- GitHub Actions：自动构建 APK，并在 Android 模拟器中进行启动冒烟测试

## 当前原生版本已实现

- 原生学习首页、课程列表、课程详情页面
- 新建课程及本地保存
- 媒体资料库：多文件导入、文件夹递归扫描
- 视频/音频通过持久化的 content URI 直接交给 Media3 播放
- 原生播放器控制、播放进度、倍速和快退
- 系统 TTS 朗读
- 主题色切换、深色模式
- Android 返回键页面导航

## 构建

GitHub Actions → Build Native Android APK → Artifacts → deskops-native-debug-apk。

下载 ZIP 并解压后安装 app-debug.apk。

## 说明

这是原生重构的第一阶段基础版本。课程章节编辑、学习进度、划线摘录、学习日历、课程导入导出、播放历史、媒体排序与更多细节将继续逐项实现和测试。模拟器启动测试不等于已覆盖所有真实手机型号、视频编码格式和系统语音引擎。
