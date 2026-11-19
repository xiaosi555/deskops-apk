package com.qwen.opsmentor.navigation

/**
 * 唯一页面路由注册表。legacyKey 暂时与 HTML/旧存档中的路由键保持一致，
 * 迁移期间避免改变外部数据格式；新代码使用 AppRoute，不再新写裸字符串路由。
 */
enum class AppRoute(
    val legacyKey: String,
    val title: String,
    val isTab: Boolean = false,
) {
    COURSES("mine", "课程", true),
    PROFILE("profile", "我的", true),
    COURSE("course", "课程详情"),
    TODAY("today", "今日学习"),
    KNOW("know", "知识详解"),
    MARKS_COURSE("anno", "我的划线"),
    MY_MARKS("my-marks", "我的划线"),
    DATA("io", "数据"),
    REVIEW("review", "复习队列"),
    CASES("case", "案例库"),
    GLOSSARY("gloss", "术语手册"),
    PLAYBOOK("play", "排障手册"),
    PROGRESS("prog", "学习进度"),
    CALENDAR("calendar", "学习日历"),
    COURSE_MANAGE("course-manage", "课程管理"),
    COURSE_IMPORT("course-import", "导入课程"),
    SETTINGS("settings", "设置"),
    SHORTCUTS("shortcuts", "快捷入口"),
    PERSONAL_DATA("personal-data", "个人数据"),
    PLAYER("player", "");

    companion object {
        private val byKey = entries.associateBy(AppRoute::legacyKey)

        fun fromLegacyKey(key: String): AppRoute = byKey[key] ?: COURSES

        val tabKeys: Map<String, String>
            get() = entries.filter { it.isTab }.associate { it.legacyKey to it.title }

        val profileSubKeys: Set<String> = setOf(
            CALENDAR.legacyKey, MY_MARKS.legacyKey, COURSE_MANAGE.legacyKey,
            SETTINGS.legacyKey, SHORTCUTS.legacyKey, PLAYER.legacyKey,
            DATA.legacyKey, PERSONAL_DATA.legacyKey, COURSE_IMPORT.legacyKey,
        )
    }
}
