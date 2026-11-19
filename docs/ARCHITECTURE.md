# 原生工程架构审计与目标框架

## 1. 审计结论（2026-11-19）

当前工程不是从零开始的原生空项目，而是已有单 Activity + 多个 Page 类的 Kotlin View 工程。其主要现状：

- `MainActivity` 同时承担 Activity 生命周期、SAF 文件选择、导航栈、页面注册/创建、窗口 Insets、主题刷新、页面渲染和部分资源管理。
- `Page` 基类直接持有 Activity、App、Store，并提供整页 View 构建、滚动、状态访问和重绘。
- 页面类（如 `PageMine`、`PageCourse`、`PageToday`、`MePages` 中的多个页面）直接构建 Android View，也直接调用 Store、MediaCourses、Importer/Exporter 等数据/平台逻辑。
- `MediaCourses` 将 SAF 扫描、URI 元数据、课程创建、应用内文件复制、首帧封面和播放地址解析集中在一个对象中。
- `PageCourseImport` 自己启动 Thread 执行目录扫描/文件复制，再用 Handler 回主线程更新 UI。
- `PagePlayer` 自己管理音视频播放器、ticker、进度保存、生命周期相关状态。
- 现有 `README.md` 曾描述 UI/功能与 HTML 完全一致，但缺少逐页证据；现已修正为“重构中，尚未通过 UI/真机验收”。

这些结构能够解释为何局部改动容易影响不相关页面，也难以证明媒体导入从授权到播放的端到端正确性。后续不能继续把新逻辑叠加进上述巨型入口类。

## 2. 目标依赖关系

```text
Android Application
  └── App Shell / MainActivity
        ├── Navigation Controller
        ├── Design System (Tokens / Theme / Motion / Icons)
        └── Screen Host
              └── Screen UI (只负责展示和派发事件)
                    ↓
              ViewModel / Screen State
                    ↓
              Domain UseCases
               ├── Course UseCases
               ├── Study / Mark / Review UseCases
               ├── Import / Export UseCases
               └── Media Import / Playback UseCases
                    ↓
              Repository Interfaces
                    ↓
              Data / Platform Implementations
               ├── Store / Preferences / Archive
               ├── SAF Document Gateway
               ├── Media File Repository
               └── Playback Controller (Media3 adapter)
```

依赖方向必须向内：UI 依赖 ViewModel/UseCase 接口；domain 不依赖 Android View/Activity；data/platform 实现 domain 定义的 repository contract。Activity 不直接读写媒体文件，不负责业务导入流程。

## 3. 目标目录

```text
com.qwen.opsmentor/
├── app/
│   ├── OpsMentorApplication.kt
│   └── AppGraph.kt
├── navigation/
│   ├── AppRoute.kt
│   ├── AppNavigator.kt
│   └── RouteRegistry.kt
├── design/
│   ├── DesignTokens.kt
│   ├── ThemeController.kt
│   ├── MotionTokens.kt
│   ├── IconRegistry.kt
│   └── components/
├── ui/
│   ├── shell/
│   ├── screens/
│   ├── components/
│   └── state/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── local/
│   ├── repository/
│   ├── archive/
│   └── migration/
└── platform/
    ├── storage/
    ├── media/
    ├── playback/
    └── tts/
```

迁移时可以暂时保留旧类作为兼容适配器，但新页面不得再直接访问 Android 平台细节。每次迁移一个垂直功能切片，旧功能未被新实现覆盖前不删除。

## 4. 核心契约

### Navigation
- 路由以 sealed/typed route 表达，禁止散落字符串常量。
- Navigator 是唯一管理返回栈、Tab 根路由和嵌套状态的组件。
- 页面实例/状态保存策略明确，Activity 重建后恢复 route 与必要 UI state。

### UI state
- 每个 Screen 具有独立 UiState（Loading / Content / Empty / Error 视业务需要）。
- UI 事件通过 ViewModel/Controller 派发；状态单向流向 UI。
- 不在页面渲染函数内启动无生命周期管理的裸 Thread。

### Repository
- 导入、导出、课程、学习进度、媒体资源都有接口。
- 文件 IO 通过 IO dispatcher / executor 执行，返回明确 Result/Failure。
- 错误对象包含阶段、资源标识、用户可读原因、是否可重试。

### Media
- StorageGateway 统一处理 SAF intent、URI 授权、目录枚举、文件元数据、输入流。
- MediaImportUseCase 负责验证、扫描、去重、建课和导入结果报告。
- MediaRepository 负责原 URI 与应用内副本、授权、元数据和重新连接。
- PlaybackController 封装 Media3，UI 不直接操作 ExoPlayer/MediaPlayer/VideoView。
- 播放器生命周期与页面生命周期分离定义，Activity 销毁/页面离开时按策略释放。

## 5. 框架阶段实施顺序

1. 建立 typed routes 和 route registry，先覆盖当前全部 route，不改页面外观。
2. 建立 AppGraph/Repository contracts 和 Result/Error 模型。
3. 将 SAF 选择器从 MainActivity 的业务回调迁移到 StorageGateway/ActivityResult contract adapter。
4. 将媒体扫描/复制/元数据逻辑迁移到 MediaRepository + MediaImportUseCase。
5. 将播放器状态与播放控制从 PagePlayer 拆到 PlaybackController。
6. 建立页面 UiState/事件边界，先迁移媒体导入这一完整切片，再迁移其他页面。
7. 添加单元测试：路由映射、URI/媒体类型校验、自然排序、去重、失败回滚、导入状态机、进度关联。
8. 完成依赖审计和架构验收，再进入 UI 逐页重建。

## 6. 本阶段不做的事情

- 不做 UI 美化、不改颜色/间距/圆角来掩盖结构问题。
- 不宣称视频导入已修复；当前缺少真机端到端证据。
- 不执行 APK 发布构建。
- 不删除现有业务页面和存档兼容代码。
