package com.qwen.opsmentor.core

import com.qwen.opsmentor.App
import com.qwen.opsmentor.state.Store

/**
 * 学习时长计时（对应 Web「学习时长」补丁）：
 * 只在被判定为学习页、页面可见且有操作时累计；2 分钟无操作暂停；离开页面立即落盘。
 */
class StudyTimer {
    private var acc = 0L            // 当日累计毫秒
    private var lastTick = 0L
    private var active = false
    private var pageKey = ""

    fun onPage(key: String, learning: Boolean) {
        flush()
        pageKey = if (learning) key else ""
        active = learning
        lastTick = System.currentTimeMillis()
    }

    fun onPause() {
        flush(); active = false
    }

    fun onResume() {
        if (pageKey.isNotEmpty()) {
            active = true; lastTick = System.currentTimeMillis()
        }
    }

    /** 用户有操作时调用：把上次 tick 到现在的时间计入（超过 120 秒的空档不计） */
    fun poke(store: Store) {
        if (!active) return
        val now = System.currentTimeMillis()
        val d = now - lastTick
        if (d in 1..120_000L) {
            acc += d
            if (acc >= 30_000L) {
                addMinutes(store, (acc / 60_000L).toInt())
                acc = acc % 60_000L
            }
        }
        lastTick = now
    }

    private fun flush() {
        if (acc > 0) {
            addMinutes(App.inst.store, (acc / 60_000L).toInt())
            acc = 0
        }
    }

    private fun addMinutes(store: Store, min: Int) {
        if (min <= 0) return
        val k = Store.today()
        store.studyLog[k] = (store.studyLog[k] ?: 0) + min
        store.save()
    }
}
