# 桌面运维全能学习导师 · Android 原生重构工程

本仓库目标是将根目录中的 `桌面运维_星球主题版261113_v6.html` 完整迁移为 Kotlin 原生 Android 应用。

> **重要：当前版本仍处于原生重构阶段。** 现有 APK 曾成功编译，但用户反馈 UI 与 HTML 基准差异明显，视频导入也未通过真实设备验收。因此不能将当前版本描述为与 Web 版完全一致或所有功能均已验证。编译成功不等于功能完成。

## 强制开发规范

后续开发必须严格执行：

- [Android 原生重构技术规范](docs/ANDROID_NATIVE_REBUILD_SPEC.md)

开发顺序固定为：

1. **Phase A：原生代码框架** — 分层、导航、状态、数据仓储、存储/媒体接口和测试骨架。
2. **Phase B：UI 逐页重建** — 按 HTML 页面、组件、主题、交互和动效逐项对照实现。
3. **Phase C：功能联调与测试** — 贯通页面到数据/平台层，尤其验证媒体导入、播放和恢复。
4. **Phase D：APK 构建** — 只有前三阶段验收通过后才构建 APK，并交付完整源码 ZIP。

任何阶段未通过验收，不得跳过阶段或为了“有提交”而构建 APK。

## 当前工程基线

- Android 原生：Kotlin + AndroidX View
- namespace / applicationId：`com.qwen.opsmentor`
- compileSdk：34
- minSdk：24
- JDK：17
- HTML 基准：`桌面运维_星球主题版261113_v6.html`
- 内置课程数据：`app/src/main/assets/course_ops.json`

现有代码包含 Activity、页面类、Store、导入导出、SAF 媒体扫描和播放器等实现。这些是待审计与重构的现状，不代表已通过新的架构、UI 或真机功能验收。

## 媒体导入特别说明

视频/音频在本产品中是**本地文件导入**，不是上传到服务器。必须通过 Android SAF 选择文件或文件夹，保存有效 URI 授权，并验证文件能被读取、建立课程、打开播放。复制到应用内部存储是可选能力，不能把课程条目创建成功误认为文件复制成功。

## 自动检查

当前仓库保留静态检查脚本：

```bash
python3 tools/ktcheck.py app/src/main/java
python3 tools/xref.py app/src/main/java
```

这些检查只能证明部分源码结构/引用条件，不代替 Android 编译、UI 对照、媒体端到端测试或真机验收。

## 构建说明

在 Phase A、B、C 均通过之前，不应执行 APK 发布构建。开发阶段的检查可以使用 Kotlin 编译、Lint、单元测试等轻量任务，但不得将其包装成可交付 APK 的完成证明。
