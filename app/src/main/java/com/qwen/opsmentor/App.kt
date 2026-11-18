package com.qwen.opsmentor

import android.app.Application
import android.content.res.Configuration
import com.qwen.opsmentor.core.AnnotatedText
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.data.Pack
import com.qwen.opsmentor.data.PackRegistry
import com.qwen.opsmentor.state.Bus
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.state.StyleMode

class App : Application() {

    lateinit var store: Store
        private set
    lateinit var packs: PackRegistry
        private set
    var systemDark = false
        private set

    /** 当前页面上渲染出来的可划线块（供浮层 / 编辑面板微调与定位） */
    var blocks: MutableList<AnnotatedText> = mutableListOf()
        private set

    /** 当前页面重绘回调 */
    var rerender: (() -> Unit)? = null

    /** 朗读引擎（章节页） */
    var reader: com.qwen.opsmentor.core.Reader? = null

    /** 学习计时 */
    val timer = com.qwen.opsmentor.core.StudyTimer()

    override fun onCreate() {
        super.onCreate()
        inst = this
        store = Store(this)
        packs = PackRegistry(this)
        systemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        refreshTheme()
        Bus.toast = { msg -> currentActivity?.let { Sheets.toast(it, msg) } ?: run { } }
        Bus.invalidate = { rerender?.invoke() }
        Bus.navigator = { key -> (currentActivity as? com.qwen.opsmentor.ui.MainActivity)?.nav(key) }
        Bus.back = { (currentActivity as? com.qwen.opsmentor.ui.MainActivity)?.goBack() }
        // 媒体课程：恢复上次选择的文件夹里的文件（不重扫，避免卡顿）
        com.qwen.opsmentor.media.MediaCourses.restore(this, store)
    }

    fun updateSystemDark(value: Boolean) { systemDark = value; refreshTheme() }

    fun refreshTheme() {
        Tokens.refresh(store, systemDark)
    }

    /** 主题色 / 风格 / 明暗变化后统一重算并重绘 */
    fun retheme() {
        refreshTheme()
        rerender?.invoke()
    }

    /** 划线数据变化：刷新所有块 + 页面重绘 */
    fun invalidateMarks() {
        blocks.forEach { it.refresh() }
        rerender?.invoke()
    }

    fun registerBlocks(list: MutableList<AnnotatedText>) {
        blocks = list
    }

    fun currentPack(): Pack = packs.of(store, store.cur)

    fun isPlanet(): Boolean = store.style == StyleMode.PLANET

    var currentActivity: android.app.Activity? = null

    companion object {
        lateinit var inst: App
            private set
    }
}
