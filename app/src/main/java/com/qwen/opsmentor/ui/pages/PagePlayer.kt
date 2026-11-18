package com.qwen.opsmentor.ui.pages
import com.qwen.opsmentor.data.toJson

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.VideoView
import com.qwen.opsmentor.App
import com.qwen.opsmentor.ui.Bars
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.Opt
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.btn
import com.qwen.opsmentor.core.btnRow
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.iconBtn
import com.qwen.opsmentor.core.iconView
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.media.MediaCourses
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.ui.MainActivity
import com.qwen.opsmentor.ui.Page
import java.io.File

/**
 * 播放页（视频 / 音频课程，对应 Web v14 独立播放页）：
 * 视频区 + 进度与倍速 + 上下一集 + 本集列表；音频用紧凑播放条。
 */
class PagePlayer(act: Activity, app: App) : Page(act, app) {

    override val key = "player"
    // HTML 将 player 纳入 STUDY_VIEWS：播放课程时同样累计学习时长。
    override val learning = true
    override fun title(): String = current()?.t ?: ""

    private var player: MediaPlayer? = null
    private var video: VideoView? = null
    private var ticker: Runnable? = null
    private var seek: android.widget.SeekBar? = null
    private var timeLabel: android.widget.TextView? = null
    private var playBtn: ImageView? = null
    private var rate = 1f
    private var paused = true
    private var autoplayOnRender = false

    private fun current() = pack.chById(PagePlayer.curCh.ifBlank {
        val b = store.bag()
        pack.route(b.route).getOrElse(b.idx) { pack.ch.firstOrNull()?.id ?: "" }
    }) ?: pack.ch.firstOrNull()

    companion object {
        var curCh = ""
    }

    override fun body(host: LinearLayout) {
        val ctx = act
        val co = Courses.find(store, store.cur)
        if (co.media.isEmpty()) {
            host.addView(muted(ctx, "这门课程不是媒体课程。请在「课程管理 → 导入课程」里选择视频 / 音频文件夹。"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return
        }
        val ch = current() ?: return
        val isVideo = co.media == "video"
        // 标题
        val hd = vrow(host)
        hd.addView(label(ctx, ch.id + " · " + ch.t, 19f, Tokens.ink, Tokens.weightH(), 2))
        ch.media?.let { hd.addView(muted(ctx, it.fname + " · " + MediaCourses.fmtSize(it.size)),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { lp -> lp.topMargin = ctx.dp(4f) }) }
        // 播放区
        if (isVideo) {
            val vv = VideoView(ctx)
            video = vv
            val box = FrameLayout(ctx)
            box.flat(0xFF000000.toInt(), ctx.dp(Tokens.rInner()))
            box.addView(vv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            host.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(210f)).also { it.topMargin = ctx.dp(12f) })
        }
        // 控制条
        val ctrl = card(host, 14f, 12)
        seek = android.widget.SeekBar(ctx).apply {
            max = 1000
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val d = duration()
                    if (d > 0) seekTo((p / 1000f * d).toInt())
                }

                override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
            })
        }
        ctrl.addView(seek, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val row = btnRow(ctx)
        playBtn = iconBtn(ctx, "play", "播放 / 暂停", 42f, Tokens.pri) { toggle() }
        row.addView(playBtn)
        timeLabel = label(ctx, "0:00 / 0:00", 12.5f, Tokens.sub)
        row.addView(timeLabel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
        row.addView(iconBtn(ctx, "chev", "下一集", 34f, Tokens.sub) { step(1) })
        row.addView(iconBtn(ctx, "close", "倍速", 30f, Tokens.sub) { rateSheet() })
        ctrl.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        // 重连文件
        if (!playable(ch)) {
            ctrl.addView(notebox("这个文件当前读不到。可能是权限过期或文件被移动，请重新选择。", co.media), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = ctx.dp(10f)
            })
        }
        // 本集列表
        val listCard = card(host, 14f, 12)
        listCard.addView(label(ctx, "本门课程 · ${pack.ch.size} 集", 15.5f, Tokens.ink, Tokens.weightH()))
        pack.ch.forEach { c ->
            val r = hrow(ctx)
            r.setPadding(0, ctx.dp(10f), 0, ctx.dp(10f))
            r.isClickable = true
            r.setOnClickListener {
                PagePlayer.curCh = c.id
                autoplayOnRender = true
                rerender(false)
            }
            val th = FrameLayout(ctx).apply {
                flat(Tokens.priSoft, ctx.dp(4f).toFloat())
                layoutParams = LinearLayout.LayoutParams(ctx.dp(52f), ctx.dp(34f))
            }
            if (isVideo) {
                val p = MediaCourses.poster(ctx, store.cur, c)
                if (p != null && p.exists()) {
                    val iv = ImageView(ctx).apply {
                        setImageURI(Uri.fromFile(p))
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    th.addView(iv, FrameLayout.LayoutParams(ctx.dp(52f), ctx.dp(34f)))
                } else th.addView(iconView(ctx, "g_video", Tokens.pri, 18f), FrameLayout.LayoutParams(ctx.dp(18f), ctx.dp(18f)).also { it.gravity = Gravity.CENTER })
            } else th.addView(iconView(ctx, "speaker", Tokens.pri, 18f), FrameLayout.LayoutParams(ctx.dp(18f), ctx.dp(18f)).also { it.gravity = Gravity.CENTER })
            r.addView(th)
            val box = com.qwen.opsmentor.core.vcol(ctx)
            box.addView(label(ctx, c.id + " " + c.t, 14.5f, if (c.id == ch.id) Tokens.pri else Tokens.ink, Tokens.weightM(), 1))
            if (c.s.isNotEmpty()) box.addView(muted(ctx, c.s, 11.5f, 1), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(2f) })
            r.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(10f) })
            if (c.id == ch.id) r.addView(iconView(ctx, "play", Tokens.pri, 16f))
            listCard.addView(r)
        }
        host.addView(listCard)
        val autoplay = autoplayOnRender
        autoplayOnRender = false
        start(ch, autoplay)
    }


    private fun vrow(host: LinearLayout): LinearLayout {
        val ctx = act
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        host.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = ctx.dp(if (isTab) 26f else 14f)
        })
        return box
    }

    private fun notebox(msg: String, kind: String): View {
        val ctx = act
        val box = com.qwen.opsmentor.core.noteBox(ctx, "", msg)
        val re = btn(ctx, "重新选择文件", "n", small = true)
        re.setOnClickListener {
            (act as MainActivity).pickMedia(if (kind == "video") "video" else "audio") { uris ->
                if (uris.isNullOrEmpty()) return@pickMedia
                val item = MediaCourses.fromUris(ctx, uris, kind).firstOrNull() ?: return@pickMedia
                val ch = current() ?: return@pickMedia
                ch.media = com.qwen.opsmentor.data.MediaRef(kind, item.name, item.uri.toString(), item.size, ch.media?.dur ?: 0)
                val uc = store.userCourses.firstOrNull { it.id == store.cur }
                uc?.let { it.packJson = pack.toJson().toString() }
                store.save(); app.packs.put(store.cur, pack); rerender(true)
            }
        }
        box.addView(re, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        return box
    }

    private fun playable(ch: com.qwen.opsmentor.data.Chapter): Boolean {
        if (ch.media == null) return false
        val path = MediaCourses.resolve(act, store.cur, ch)
        return if (path.startsWith("/")) File(path).exists() else runCatching {
            act.contentResolver.openInputStream(Uri.parse(path))?.close()
            true
        }.getOrDefault(false)
    }

    private fun start(ch: com.qwen.opsmentor.data.Chapter, autoplay: Boolean) {
        val ctx = act
        stopPlayer()
        app.reader?.stop()
        val src = MediaCourses.resolve(act, store.cur, ch)
        if (src.isEmpty()) { Sheets.toast(ctx, "还没有连接文件"); return }
        val uri = if (src.startsWith("/")) Uri.fromFile(File(src)) else Uri.parse(src)
        val co = Courses.find(store, store.cur)
        if (co.media == "video") {
            video?.apply {
                setVideoURI(uri)
                setOnPreparedListener { mp ->
                    mp.isLooping = false
                    preparedVideoPlayer = mp
                    applyRate(mp)
                    restorePos(ch)
                    paused = !autoplay
                    if (autoplay) start()
                    paintPlay()
                    postTick()
                }
                setOnErrorListener { _, _, _ -> Sheets.toast(ctx, "这个格式在当前设备上播放失败，建议用 mp4"); true }
            }
        } else {
            player = MediaPlayer().apply {
                runCatching {
                    setDataSource(act, uri)
                    setOnPreparedListener { mp ->
                        applyRate(mp)
                        restorePos(ch)
                        paused = !autoplay
                        if (autoplay) start()
                        paintPlay()
                        postTick()
                    }
                    setOnErrorListener { _, _, _ -> Sheets.toast(ctx, "这个格式在当前设备上播放失败，建议用 mp3 / m4a"); true }
                    prepareAsync()
                }.onFailure { Sheets.toast(ctx, "无法读取该文件") }
            }
        }
        if (autoplay) {
            store.bag().pin = ch.id
            store.save()
        }
    }

    private fun duration(): Int = try {
        video?.duration ?: player?.duration ?: 0
    } catch (e: Exception) {
        0
    }

    private fun position(): Int = try {
        video?.currentPosition ?: player?.currentPosition ?: 0
    } catch (e: Exception) {
        0
    }

    private fun seekTo(ms: Int) {
        runCatching { video?.seekTo(ms) ?: player?.seekTo(ms) }
    }

    private fun toggle() {
        val ch = current() ?: return
        val v = video
        if (v != null) {
            if (v.isPlaying) { v.pause(); paused = true } else { v.start(); paused = false }
        } else player?.let { p ->
            if (p.isPlaying) { p.pause(); paused = true } else { p.start(); paused = false }
        } ?: run { start(ch, true); return }
        paintPlay()
    }

    private fun paintPlay() {
        playBtn?.setImageDrawable(Icons.get(act, if (paused) "play" else "pause", Tokens.pri))
    }

    private fun step(d: Int) {
        val idx = pack.ch.indexOfFirst { it.id == current()?.id }
        if (idx < 0) return
        val next = (idx + d).coerceIn(0, pack.ch.size - 1)
        PagePlayer.curCh = pack.ch[next].id
        autoplayOnRender = true
        rerender(false)
    }

    private var preparedVideoPlayer: MediaPlayer? = null

    private fun applyRate(target: MediaPlayer) {
        runCatching { target.playbackParams = target.playbackParams.setSpeed(rate) }
    }

    private fun setRate() {
        preparedVideoPlayer?.let { applyRate(it) }
        player?.let { applyRate(it) }
    }

    private fun rateSheet() {
        Sheets.picker(act, "播放速度", listOf(
            Opt("0.75", "0.75×"), Opt("1", "1×（默认）"), Opt("1.25", "1.25×"), Opt("1.5", "1.5×"), Opt("2", "2×"),
        ), rate.toString()) { v, _ ->
            rate = v.toFloatOrNull() ?: 1f
            setRate()
        }
    }

    private fun restorePos(ch: com.qwen.opsmentor.data.Chapter) {
        val ms = (store.mediaPos["${store.cur}:${ch.id}"] ?: 0L).toInt()
        if (ms > 0) runCatching { seekTo(ms) }
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun postTick() {
        val ctx = act
        ticker?.let { handler.removeCallbacks(it) }
        ticker = object : Runnable {
            override fun run() {
                val d = duration(); val p = position()
                if (d > 0) {
                    seek?.progress = (p * 1000L / d).toInt()
                    timeLabel?.text = "${fmt(p)} / ${fmt(d)}"
                    val ch = current()
                    if (ch != null) store.mediaPos["${store.cur}:${ch.id}"] = p.toLong()
                }
                handler.postDelayed(this, 500)
            }
        }
        ticker?.let { handler.post(it) }
    }

    private fun fmt(ms: Int): String {
        val s = ms / 1000
        return "${s / 60}:${"%02d".format(s % 60)}"
    }

    private fun stopPlayer() {
        runCatching { video?.stopPlayback() }
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        preparedVideoPlayer = null
        paused = true
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
    }

    /** 供外部查询：页面是否正在播放 */
    fun isPlaying(): Boolean = runCatching { video?.isPlaying == true || player?.isPlaying == true }.getOrDefault(false)

    /** 切走页面时立即暂停（与 Web v16 一致） */
    fun onLeave() {
        store.save()
        stopPlayer()
    }

}
