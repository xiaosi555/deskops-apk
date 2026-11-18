package com.qwen.opsmentor.ui

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.core.Covers
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.MarkEngine
import com.qwen.opsmentor.core.RingButton
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.circle
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.itemIn
import com.qwen.opsmentor.core.countUp
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.iconBtn
import com.qwen.opsmentor.core.iconView
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.surface
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.data.Chapter
import com.qwen.opsmentor.data.Pack
import com.qwen.opsmentor.state.Courses

import com.qwen.opsmentor.core.add
/**
 * 行组件：播客条目行（星球版）与卡片行（极简版）。
 * 与 Web 的 epChapter / epCourse / chRow / listRow 一一对应。
 */
object Rows {

    /** 章节行：封面 + 标题 + 摘要 + 元信息（状态 / 模块 / 划线数）+ 播放环 + 三点菜单 */
    fun chapter(act: Activity, app: App, pack: Pack, c: Chapter, currentId: String,
                onClick: () -> Unit, onMenu: (() -> Unit)? = null): View {
        val ctx = act
        val b = app.store.bag()
        val st = b.status[c.id]
        val isCur = c.id == currentId
        val co = Courses.find(app.store, app.store.cur)
        val el = if (Tokens.planet) planet(act, app, pack, c, co, st, isCur, onClick, onMenu) else minimal(act, app, pack, c, co, st, isCur, onClick)
        el.itemIn()
        return el
    }

    private fun planet(act: Activity, app: App, pack: Pack, c: Chapter, co: Courses.Entry, st: String?, isCur: Boolean,
                       onClick: () -> Unit, onMenu: (() -> Unit)?): LinearLayout {
        val ctx = act
        val b = app.store.bag()
        val el = hrow(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, ctx.dp(12f), 0, ctx.dp(12f))
            if (isCur) flat(Tokens.accentWash, Tokens.rInner())
            isClickable = true
            setOnClickListener { onClick() }
        }
        val (_, _, cover) = Courses.meta(app.store, co)
        val th = FrameLayout(ctx).apply {
            flat(Tokens.priSoft, ctx.dp(6f).toFloat())
            layoutParams = LinearLayout.LayoutParams(ctx.dp(52f), ctx.dp(52f))
        }
        th.addView(Covers.view(ctx, ctx.dp(52f), cover.ifBlank { null }, co.coverClass, Courses.glyph(app.store, co), 6f),
            FrameLayout.LayoutParams(ctx.dp(52f), ctx.dp(52f)))
        el.addView(th)
        val mn = vcol(ctx)
        mn.add(label(ctx, c.id + " " + c.t, 16.5f, if (isCur) Tokens.pri else Tokens.ink, Tokens.weightH(), 2), t = 2)
        if (c.s.isNotEmpty()) mn.add(muted(ctx, c.s, 13f, 2), t = 3)
        val meta = hrow(ctx)
        meta.setPadding(0, ctx.dp(6f), 0, 0)
        val label2 = when (st) {
            "green" -> "已过关"
            "yellow" -> "半过关"
            else -> if (b.learn.containsKey(c.id)) "学习中" else "未开始"
        }
        val dot = hrow(ctx)
        if (st == "green") {
            val d = FrameLayout(ctx).apply { circle(Tokens.pri); layoutParams = LinearLayout.LayoutParams(ctx.dp(16f), ctx.dp(16f)) }
            d.addView(iconView(ctx, "check", Tokens.onPri, 10f), FrameLayout.LayoutParams(ctx.dp(10f), ctx.dp(10f)).also { it.gravity = Gravity.CENTER })
            dot.addView(d)
        }
        dot.addView(label(ctx, label2, 11.5f, Tokens.sub, 400), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(5f) })
        meta.addView(dot)
        val mod = pack.moduleOf[c.m]
        meta.addView(label(ctx, mod?.n?.replace("模块 " + c.m + " · ", "") ?: ("模块 " + c.m), 11.5f, Tokens.sub, 400),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(11f) })
        val nm = MarkEngine.countOf(app.store.marks, c.id)
        if (nm > 0) {
            val x = hrow(ctx)
            x.addView(iconView(ctx, "highlighter", Tokens.sub, 13f))
            x.addView(label(ctx, " $nm", 11.5f, Tokens.sub, 400), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(5f) })
            x.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(11f) }
            meta.addView(x)
        }
        mn.addView(meta)
        el.addView(mn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(13f) })

        val act2 = hrow(ctx)
        val prog = Courses.prog(app.store, pack, app.store.cur)
        if (isCur && st != "green") {
            val rb = RingButton(ctx, if (prog.total == 0) 0f else prog.done.toFloat() / prog.total, "play")
            rb.setOnClickListener { onClick() }
            act2.addView(rb, LinearLayout.LayoutParams(ctx.dp(40f), ctx.dp(40f)))
        } else {
            val pb = FrameLayout(ctx).apply {
                flat(if (st == "green") Tokens.priSoft else Tokens.withAlpha(Tokens.ink, 0.05f), ctx.dp(20f).toFloat())
                layoutParams = LinearLayout.LayoutParams(ctx.dp(38f), ctx.dp(38f))
            }
            pb.addView(iconView(ctx, if (st == "green") "check" else "play", if (st == "green") Tokens.pri else Tokens.ink, if (st == "green") 20f else 18f),
                FrameLayout.LayoutParams(ctx.dp(20f), ctx.dp(20f)).also { it.gravity = Gravity.CENTER })
            pb.setOnClickListener { onClick() }
            act2.addView(pb)
        }
        if (onMenu != null) {
            val mb = iconBtn(ctx, "more", "更多", 30f, Tokens.ink) { onMenu() }
            act2.addView(mb, LinearLayout.LayoutParams(ctx.dp(28f), ctx.dp(40f)).also { it.marginStart = ctx.dp(2f) })
        }
        el.addView(act2)
        return el
    }

    private fun minimal(act: Activity, app: App, pack: Pack, c: Chapter, co: Courses.Entry, st: String?, isCur: Boolean,
                        onClick: () -> Unit): LinearLayout {
        val ctx = act
        val (_, _, cover) = Courses.meta(app.store, co)
        val el = hrow(ctx).apply {
            setPadding(ctx.dp(12f), ctx.dp(12f), ctx.dp(12f), ctx.dp(12f))
            if (isCur) flat(Tokens.accentWash, Tokens.rInner()) else surface(Tokens.l2, Tokens.rInner(), 1f, true)
            isClickable = true
            setOnClickListener { onClick() }
        }
        el.addView(label(ctx, c.id, 12f, Tokens.pri, Tokens.weightS()).also {
            it.flat(Tokens.priSoft, ctx.dp(6f).toFloat())
            val p = ctx.dp(6f)
            it.setPadding(p, ctx.dp(2f), p, ctx.dp(2f))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val ci = vcol(ctx)
        ci.addView(label(ctx, c.t, 15f, Tokens.ink, Tokens.weightM()))
        if (c.s.isNotEmpty()) ci.add(muted(ctx, c.s, 12.5f, 2), t = 2)
        el.addView(ci, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(10f) })
        if (st != null) el.addView(label(ctx, when (st) { "green" -> "🟢"; "yellow" -> "🟡"; else -> "" }, 13f, Tokens.sub),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = ctx.dp(6f) })
        el.addView(iconView(ctx, "chev", Tokens.sub, 18f))
        return el
    }

    /** 课程行（星球版播客条目） */
    fun course(act: Activity, app: App, pack: Pack, co: Courses.Entry, started: Boolean, prog: Courses.Prog,
               onClick: () -> Unit): LinearLayout {
        val ctx = act
        val meta = Courses.meta(app.store, co)
        val starred = (app.store.star[co.id] ?: 0) > 0
        val el = hrow(ctx).apply {
            setPadding(0, ctx.dp(12f), 0, ctx.dp(12f))
            isClickable = true
            setOnClickListener { onClick() }
        }
        val th = FrameLayout(ctx).apply {
            flat(Tokens.priSoft, ctx.dp(6f).toFloat())
            layoutParams = LinearLayout.LayoutParams(ctx.dp(76f), ctx.dp(76f))
        }
        th.addView(Covers.view(ctx, ctx.dp(76f), meta.third.ifBlank { null }, co.coverClass, Courses.glyph(app.store, co), 6f),
            FrameLayout.LayoutParams(ctx.dp(76f), ctx.dp(76f)))
        el.addView(th)
        val mn = vcol(ctx)
        val k = hrow(ctx)
        k.addView(label(ctx, "${prog.mods} 模块 · ${prog.chs} 章", 12f, Tokens.sub, 400).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val sb = iconBtn(ctx, if (starred) "starFill" else "star", "星标", 24f, if (starred) Tokens.pri else Tokens.sub) {
            val s = app.store.star
            if ((s[co.id] ?: 0) > 0) s.remove(co.id) else s[co.id] = 1
            app.store.save(); app.rerender?.invoke()
        }
        k.addView(sb, LinearLayout.LayoutParams(ctx.dp(24f), ctx.dp(24f)))
        mn.addView(k)
        mn.add(label(ctx, meta.first, 16.5f, Tokens.ink, Tokens.weightH(), 2), t = 2)
        if (meta.second.isNotEmpty()) {
            val q = hrow(ctx)
            q.addView(iconView(ctx, "quote", Tokens.sub, 15f))
            q.addView(label(ctx, " " + meta.second, 13f, Tokens.sub, 400, 1), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(6f) })
            mn.addView(q, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        }
        if (started) {
            val mt = hrow(ctx)
            mt.addView(iconView(ctx, "check", Tokens.pri, 13f))
            mt.addView(label(ctx, " 已过关 ${prog.done} / ${prog.total} 章", 11.5f, Tokens.sub, 400),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(5f) })
            mn.addView(mt, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        }
        el.addView(mn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(13f) })
        val act2 = hrow(ctx)
        if (started) {
            val rb = RingButton(ctx, if (prog.total == 0) 0f else prog.done.toFloat() / prog.total, "play")
            rb.setOnClickListener { onClick() }
            act2.addView(rb, LinearLayout.LayoutParams(ctx.dp(40f), ctx.dp(40f)))
        } else {
            val pb = FrameLayout(ctx).apply {
                flat(Tokens.priSoft, ctx.dp(19f).toFloat())
                layoutParams = LinearLayout.LayoutParams(ctx.dp(38f), ctx.dp(38f))
                isClickable = true
                setOnClickListener { onClick() }
            }
            pb.addView(iconView(ctx, "play", Tokens.pri, 18f), FrameLayout.LayoutParams(ctx.dp(18f), ctx.dp(18f)).also { it.gravity = Gravity.CENTER })
            act2.addView(pb)
        }
        el.addView(act2)
        return el
    }

    /** 极简版课程卡 */
    fun courseCard(act: Activity, app: App, co: Courses.Entry, prog: Courses.Prog, onClick: () -> Unit): LinearLayout {
        val ctx = act
        val meta = Courses.meta(app.store, co)
        val starred = (app.store.star[co.id] ?: 0) > 0
        val el = hrow(ctx).apply {
            setPadding(ctx.dp(14f), ctx.dp(14f), ctx.dp(14f), ctx.dp(14f))
            surface(Tokens.l2, 18f, 2f, false)
            isClickable = true
            setOnClickListener { onClick() }
        }
        val (_, _, cover) = meta
        el.addView(Covers.view(ctx, ctx.dp(76f), cover.ifBlank { null }, co.coverClass, Courses.glyph(app.store, co)),
            LinearLayout.LayoutParams(ctx.dp(76f), ctx.dp(76f)))
        val ci = vcol(ctx)
        ci.addView(label(ctx, meta.first, 16f, Tokens.ink, Tokens.weightH()))
        ci.add(muted(ctx, meta.second, 12.5f, 2), t = 4)
        if (prog.started) {
            val bar = com.qwen.opsmentor.ui.Bars.bar(ctx, prog.pct() / 100f)
            ci.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(6f)).also { it.topMargin = ctx.dp(8f) })
        }
        ci.add(label(ctx, (if (prog.started) "已过关 ${prog.done} / ${prog.total} 章 · " else "") + "${prog.mods} 模块 · ${prog.chs} 章", 11.5f, Tokens.sub), t = 6)
        el.addView(ci, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(13f) })
        el.addView(iconBtn(ctx, if (starred) "starFill" else "star", "星标", 28f, if (starred) Tokens.pri else Tokens.sub) {
            val s = app.store.star
            if ((s[co.id] ?: 0) > 0) s.remove(co.id) else s[co.id] = 1
            app.store.save(); app.rerender?.invoke()
        })
        return el
    }

    /** 通用列表行：图标 + 标题 + 说明 + 尾值 + 箭头（我的页 / 数据页 / 设置页） */
    fun list(act: Activity, icon: String, title: String, desc: String = "", tail: String = "", onClick: () -> Unit): View {
        val ctx = act
        if (Tokens.planet) {
            val el = hrow(ctx).apply {
                setPadding(0, ctx.dp(12f), 0, ctx.dp(12f))
                isClickable = true
                setOnClickListener { onClick() }
            }
            val th = FrameLayout(ctx).apply {
                flat(Tokens.priSoft, ctx.dp(6f).toFloat())
                layoutParams = LinearLayout.LayoutParams(ctx.dp(52f), ctx.dp(52f))
            }
            th.addView(iconView(ctx, icon, Tokens.pri, 22f), FrameLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f)).also { it.gravity = Gravity.CENTER })
            el.addView(th)
            val mn = vcol(ctx)
            mn.addView(label(ctx, title, 15.5f, Tokens.ink, Tokens.weightM()))
            if (desc.isNotEmpty()) mn.add(muted(ctx, desc, 12.5f, 2), t = 3)
            el.addView(mn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(13f) })
            if (tail.isNotEmpty()) el.addView(label(ctx, tail, 12.5f, Tokens.sub))
            el.addView(iconView(ctx, "chev", Tokens.sub, 18f), LinearLayout.LayoutParams(ctx.dp(18f), ctx.dp(18f)).also { it.marginStart = ctx.dp(6f) })
            return el
        }
        val el = hrow(ctx).apply {
            setPadding(ctx.dp(12f), ctx.dp(12f), ctx.dp(12f), ctx.dp(12f))
            surface(Tokens.l2, Tokens.rInner(), 1f, true)
            isClickable = true
            setOnClickListener { onClick() }
        }
        val cid = FrameLayout(ctx).apply {
            flat(Tokens.priSoft, ctx.dp(8f).toFloat())
            layoutParams = LinearLayout.LayoutParams(ctx.dp(34f), ctx.dp(34f))
        }
        cid.addView(iconView(ctx, icon, Tokens.pri, 18f), FrameLayout.LayoutParams(ctx.dp(18f), ctx.dp(18f)).also { it.gravity = Gravity.CENTER })
        el.addView(cid)
        val ci = vcol(ctx)
        ci.addView(label(ctx, title, 15f, Tokens.ink, Tokens.weightM()))
        if (desc.isNotEmpty()) ci.add(muted(ctx, desc, 12.5f, 2), t = 2)
        el.addView(ci, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(10f) })
        if (tail.isNotEmpty()) el.addView(label(ctx, tail, 12.5f, Tokens.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = ctx.dp(6f) })
        el.addView(iconView(ctx, "chev", Tokens.sub, 18f))
        return el
    }
}

/** 进度条与统计格 */
object Bars {
    /** 与 Web .bar 同形：6dp 高、主色填充、次色底 */
    fun bar(ctx: Context, pct: Float, hDp: Float = 6f): View = Line(ctx, pct, hDp)

    private class Line(ctx: Context, private var pct: Float, private val hDp: Float) : View(ctx) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(hDp))
        }

        fun setPct(v: Float) { pct = v.coerceIn(0f, 1f); invalidate() }

        override fun onDraw(c: Canvas) {
            val r = height.toFloat().coerceAtLeast(1f)
            p.color = Tokens.withAlpha(Tokens.sub, 0.16f)
            c.drawRoundRect(RectF(0f, 0f, width.toFloat(), r), r / 2f, r / 2f, p)
            p.color = Tokens.pri
            val w = width * pct.coerceIn(0f, 1f)
            if (w > 0) c.drawRoundRect(RectF(0f, 0f, w, r), r / 2f, r / 2f, p)
        }
    }

        fun statGrid(ctx: Context, items: List<Pair<String, String>>, cols: Int = 3): LinearLayout {
        val grid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        items.chunked(cols).forEach { row ->
            val r = hrow(ctx)
            row.forEach { (v, k) ->
                val cell = vcol(ctx)
                cell.flat(if (Tokens.planet) Tokens.l2 else Tokens.withAlpha(Tokens.ink, 0.03f), ctx.dp(Tokens.rInner()))
                val p = ctx.dp(10f)
                cell.setPadding(p, p, p, p)
                cell.addView(label(ctx, v, 19f, Tokens.ink, Tokens.weightH()))
                cell.add(muted(ctx, k, 11.5f, 2), t = 2)
                r.addView(cell, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    if (r.childCount > 0) it.marginStart = ctx.dp(8f)
                })
                val num = cell.getChildAt(0) as android.widget.TextView
                val n = Regex("^(\\d+)").find(v)?.groupValues?.get(1)?.toIntOrNull()
                if (n != null) num.countUp(n, v.substring(Regex("^(\\d+)").match(v)!!.value.length))
            }
            grid.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = ctx.dp(8f) })
        }
        return grid
    }
}
