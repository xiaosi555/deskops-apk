package com.qwen.opsmentor.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import java.util.Locale

/**
 * 章节朗读（对应 Web 的 Web Speech TTS）：逐段朗读 + 当前段高亮 + 语速调节。
 * 与音视频互斥：外部在媒体开始播放时调用 stop()。
 */
class Reader(private val act: android.app.Activity) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var on: Boolean = false
        private set
    var segIndex: Int = 0
        private set
    private var segs: List<String> = emptyList()
    private var rate = App.inst.store.ttsRate
    var onState: () -> Unit = {}
    var onSeg: (Int) -> Unit = {}

    init { tts = TextToSpeech(act, this) }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Sheets.toast(act, "系统朗读引擎不可用，可先在系统设置里启用 TTS")
            return
        }
        tts?.language = Locale.SIMPLIFIED_CHINESE
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                val i = (utteranceId ?: "0").toIntOrNull() ?: 0
                if (i + 1 < segs.size) speak(i + 1) else { stop(); Sheets.toast(act, "本章朗读完成") }
            }
            @Deprecated("deprecated in API level 23")
            override fun onError(utteranceId: String?) { stop() }
        })
    }

    /** 语速 0.75×–1.75× */
    fun setRate(r: Float) {
        rate = r.coerceIn(0.75f, 1.75f)
        App.inst.store.ttsRate = rate
        App.inst.store.save()
        tts?.setSpeechRate(rate)
        if (on) { stop(); start(segIndex) }
    }

    fun segsSize(): Int = segs.size

    fun load(seg: List<String>) {
        val old = segIndex
        segs = seg
        if (old >= segs.size) segIndex = 0
    }

    fun start(from: Int = 0) {
        if (segs.isEmpty()) { Sheets.toast(act, "本章没有可朗读的正文"); return }
        on = true
        tts?.setSpeechRate(rate)
        speak(from.coerceIn(0, segs.size - 1))
        onState()
    }

    private fun speak(i: Int) {
        segIndex = i
        val engine = tts ?: return
        onSeg(i)
        val text = segs[i]
        engine.stop()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, i.toString())
        onState()
    }

    fun pause() {
        tts?.stop()
        on = false
        onState()
    }

    fun stop() {
        tts?.stop()
        on = false
        segIndex = 0
        onState()
    }

    fun toggle() {
        if (on) pause() else start(segIndex)
    }

    fun next() { if (segs.isNotEmpty()) speak(minOf(segIndex + 1, segs.size - 1)) }
    fun prev() { if (segs.isNotEmpty()) speak(maxOf(segIndex - 1, 0)) }

    fun shutdown() {
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
    }

    private fun minOf(a: Int, b: Int) = if (a < b) a else b
    private fun maxOf(a: Int, b: Int) = if (a > b) a else b
}
