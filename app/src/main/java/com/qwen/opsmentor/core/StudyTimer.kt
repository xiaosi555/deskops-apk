package com.qwen.opsmentor.core

import android.os.Handler
import android.os.Looper
import com.qwen.opsmentor.App
import com.qwen.opsmentor.state.Store

/**
 * 学习时长计时：仅学习页面可见且最近 120 秒内有用户操作时累计。
 * 与 HTML 的 5 秒 tick / 2 分钟空闲暂停行为一致。
 */
class StudyTimer {
    private val handler = Handler(Looper.getMainLooper())
    private var acc = 0L
    private var lastTick = 0L
    private var lastActivityAt = 0L
    private var active = false
    private var pageKey = ""

    private val ticker = object : Runnable {
        override fun run() {
            if (!active) return
            val now = System.currentTimeMillis()
            if (lastActivityAt > 0L && now - lastActivityAt <= 120_000L) {
                acc += (now - lastTick).coerceIn(0L, 7_000L)
                flushWholeMinutes(App.inst.store)
            }
            lastTick = now
            handler.postDelayed(this, 5_000L)
        }
    }

    fun onPage(key: String, learning: Boolean) {
        // 在两个学习页面之间切换时保留不足一分钟的余量，不因导航而丢失。
        if (!learning) flush()
        handler.removeCallbacks(ticker)
        pageKey = if (learning) key else ""
        active = learning
        val now = System.currentTimeMillis()
        lastTick = now
        lastActivityAt = if (learning) now else 0L
        if (active) handler.postDelayed(ticker, 5_000L)
    }

    fun onPause() {
        flush()
        active = false
        lastActivityAt = 0L
        handler.removeCallbacks(ticker)
    }

    fun onResume() {
        if (pageKey.isNotEmpty()) {
            active = true
            lastTick = System.currentTimeMillis()
            // 恢复前台后等待一次真实交互，再开始计算，避免后台停留时间计入。
            lastActivityAt = 0L
            handler.removeCallbacks(ticker)
            handler.postDelayed(ticker, 5_000L)
        }
    }

    /** 任意点击、滚动、按键等用户交互都会刷新空闲计时。 */
    fun poke(store: Store) {
        if (!active) return
        lastActivityAt = System.currentTimeMillis()
    }

    private fun flushWholeMinutes(store: Store) {
        if (acc < 60_000L) return
        val minutes = (acc / 60_000L).toInt()
        acc %= 60_000L
        addMinutes(store, minutes)
    }

    private fun flush() {
        if (acc > 0L) {
            val minutes = Math.round(acc / 60_000f).toInt()
            addMinutes(App.inst.store, minutes)
            acc = 0L
        }
    }

    private fun addMinutes(store: Store, min: Int) {
        if (min <= 0) return
        val key = Store.today()
        store.studyLog[key] = (store.studyLog[key] ?: 0) + min
        store.save()
    }
}
