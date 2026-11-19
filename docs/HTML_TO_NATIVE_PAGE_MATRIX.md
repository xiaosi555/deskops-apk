# HTML → Android 原生页面映射验收矩阵

本表是 Phase B 的逐页验收入口。当前只记录已发现的 HTML 基准函数与现有 Kotlin 路由，不代表视觉验收通过。状态初始均为 **待审计 / 待对照**。

## 1. 主页面映射

| HTML 页面/入口 | HTML 基准函数（需进一步定位 DOM/CSS） | 当前 Kotlin route / 类 | 视觉验收 | 交互验收 |
|---|---|---|---|---|
| 课程首页 | `renderMine` / `renderMineMinSearch` | `mine` / `PageMine` | 待对照 | 待逐项核验 |
| 我的 | `renderProfile` / `renderProfilePlanet` / `renderProfileMin` | `profile` / `PageProfile` | 待对照 | 待逐项核验 |
| 课程详情 | `renderCourse` / `renderCoursePlanet` / `renderCourseMin` | `course` / `PageCourse` | 待对照 | 待逐项核验 |
| 今日学习/章节 | `renderToday` / `renderLong` | `today` / `PageToday` | 待对照 | 待逐项核验 |
| 知识详解 | `renderKnow` | `know` / `PageKnow` | 待对照 | 待逐项核验 |
| 我的划线（课程入口） | `renderAnno` | `anno` / `PageMarksCourse` | 待对照 | 待逐项核验 |
| 我的划线列表 | `renderMyMarks` / `renderMyMarksOrig` | `my-marks` / `PageMarks` | 待对照 | 待逐项核验 |
| 数据 | `renderIo` / `renderExportPage` / `renderImportPage` | `io` / `PageIo` | 待对照 | 待逐项核验 |
| 复习队列 | `renderReview` | `review` / `PageReview` | 待对照 | 待逐项核验 |
| 案例库 | `renderCases` | `case` / `PageCases` | 待对照 | 待逐项核验 |
| 术语手册 | `renderGloss` | `gloss` / `PageGloss` | 待对照 | 待逐项核验 |
| 排障手册 | `renderPlay` | `play` / `PagePlaybook` | 待对照 | 待逐项核验 |
| 学习进度 | `renderProg` | `prog` / `PageProgress` | 待对照 | 待逐项核验 |
| 学习日历 | `renderCalendar` / `renderCalendarOrig` | `calendar` / `PageCalendar` | 待对照 | 待逐项核验 |
| 课程管理 | `renderCourseManage` / `tidyCourseManage` | `course-manage` / `PageCourseManage` | 待对照 | 待逐项核验 |
| 课程导入 | `renderCourseImport` / `mediaPane` | `course-import` / `PageCourseImport` | 待对照 | 待逐项核验 |
| 设置 | `renderSettings` | `settings` / `PageSettings` | 待对照 | 待逐项核验 |
| 快捷入口 | `renderShortcuts` | `shortcuts` / `PageShortcuts` | 待对照 | 待逐项核验 |
| 媒体播放器 | `renderPlayer` / `buildMediaPlayer` | `player` / `PagePlayer` | 待对照 | 待逐项核验 |
| 个人数据 | `renderPersonalData` / `openPersonalImport` | `personal-data` / `PagePersonalData` | 待对照 | 待逐项核验 |

## 2. HTML 外壳与通用交互

| HTML 区域/行为 | HTML 入口/DOM | 原生承载位置 | 验收点 | 状态 |
|---|---|---|---|---|
| 顶部栏 | `#appbar` / `#backBtn` / `#abTitle` / `#abActions` / `#abEdit` | MainActivity shell | 高度、返回、标题、动作区、安全区 | 待对照 |
| 底部 Tab | `#tabbar` / `#tbin` | MainActivity shell | 选中态、切换、点击反馈 | 待对照 |
| 迷你继续学习条 | `#mini` / `#miniInner` | MiniBar | 显隐、横滑、恢复、丢弃 | 待对照 |
| 弹层/底部 Sheet | `openSheet` 及相关弹层函数 | Sheets / Dialog components | 尺寸、圆角、遮罩、关闭、拖拽、焦点 | 待对照 |
| 页面切换 | `render` / `afterRender` | MainActivity | 进入/退出动画、滚动位置、状态保留 | 待对照 |
| 划线浮层 | `#selBar` / `showBarAtSelection` | SelectionBar | 命中、拖动、微调、定位、持久化 | 待对照 |
| Toast/成功反馈 | `#toast` / `#checkPop` | Sheets | 位置、时长、动效、反馈文本 | 待对照 |
| 主题 | CSS `--l0..--l3` / `--pri` / `data-theme` | Tokens / Theme | 全主题色派生、明暗、对比度、组件覆盖 | 待对照 |

## 3. 媒体相关 HTML 功能映射

| HTML 函数 | 当前原生实现 | 必须重新验收 |
|---|---|---|
| `openMediaImport` / `mediaPane` | PageCourseImport + MediaCourses.scan | SAF 授权、递归、MIME、错误与取消 |
| `mediaChapters` / `createMediaCourse` | MediaCourses.createCourse | 每个文件映射章节、排序、去重、元数据 |
| `relinkMedia` | 当前实现需定位 | 失效 URI 重新连接 |
| `buildMediaPlayer` / `renderPlayer` | PagePlayer | 视频/音频实际播放、进度、重播、生命周期 |
| `addMediaItems` / `importDone` | 当前实现需定位 | 导入状态、部分成功、结果报告 |
| `resortMedia` / `tidyMediaCourse` | MediaCourses.sortItems 等 | 自然排序、重排后稳定 ID 与进度 |
| `renameMediaItem` / `deleteMediaItem` | 当前实现需定位 | 章节重命名/删除、媒体副本清理 |

## 4. 每页验收记录模板

每完成一个页面，必须补充：
- HTML 文件版本与函数/DOM/CSS 位置
- 原生 screen 与 component 清单
- 截图尺寸/设备密度/主题模式
- 截图对照结果（结构、位置、尺寸、颜色、字体、间距）
- 控件逐项 action → state change → visible result
- 自动化测试/人工真机测试步骤与结果
- 未实现差异、原因、用户批准的取舍

**当前状态：** 这是初始映射。所有页面的视觉与设备端交互均未完成正式验收，不得标记为“与 HTML 一致”。
