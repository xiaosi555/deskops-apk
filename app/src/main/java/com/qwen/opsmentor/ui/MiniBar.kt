package com.qwen.opsmentor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.core.Covers
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.RingButton
import com.qwen.opsmentor.core.SNAP
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.circle
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.surface
import com.qwen.opsmentor.state.Courses

import com.qwen.opsmentor.core.add
/**
 * 迷你「继续学习」条（对应 Web renderMini / bindMini）：
 * - 章节页、（极简版）课程详情页不显示；
 * - 左右滑动超阈值丢弃，未达阈值回弹；
 * - 重新回到前台再次出现；
 * - 星球版带进度环 + 章节队列按钮。
 */
@SuppressLint("ViewConstructor")
class MiniBar(ctx: Context, private val app: App, private val go: (String) -> Unit) : FrameLayout(ctx) {

    private val inner = LinearLayout(ctx)
    private var gone = false
    private var shownOnce = false
    private var dragging = false
    private var movedFlag = false

    init {
        isClickable = false
        val h = ctx.dp(60f)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
        inner.orientation = LinearLayout.HORIZONTAL
        inner.gravity = Gravity.CENTER_VERTICAL
        val p = ctx.dp(10f)
        inner.setPadding(p, ctx.dp(8f), p, ctx.dp(8f))
        if (Tokens.planet) {
            inner.flat(Tokens.withAlpha(Tokens.l2, 0.97f), 0f)
        } else {
            inner.surface(Tokens.l3, 16f, 6f, false)
        }
        addView(inner, LayoutParams(
            if (Tokens.planet) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).also {
            it.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            if (Tokens.planet) {
                it.leftMargin = ctx.dp(16f); it.rightMargin = ctx.dp(16f)
            } else {
                it.leftMargin = ctx.dp(12f); it.rightMargin = ctx.dp(12f)
            }
        })
        bindDrag()
    }

    fun reappearOnResume() {
        if (gone) {
            gone = false; shownOnce = false
            invalidate()
        }
    }

    fun hide() {
        visibility = View.GONE
    }

    fun render(view: String) {
        val ctx = context
        val planet = Tokens.planet
        val b = app.store.bag()
        val pinned = if (b.pin.isNotEmpty()) pack().chById(b.pin) else null
        val ch = pinned ?: curCh()
        val hide = view == "today" || (!planet && view == "course") || gone || ch == null
        if (hide || ch == null) {
            visibility = View.GONE
            shownOnce = false
            return
        }
        val p = pack()
        val q = p.route(b.route)
        val i = b.idx.coerceAtMost((q.size - 1).coerceAtLeast(0))
        val pr = Courses.prog(app.store, p, app.store.cur)
        visibility = View.VISIBLE
        inner.removeAllViews()
        val co = Courses.find(app.store, app.store.cur)
        val meta = Courses.meta(app.store, co)
        inner.addView(Covers.view(ctx, ctx.dp(52f), meta.third.ifBlank { null }, co.coverClass, Courses.glyph(app.store, co)),
            LinearLayout.LayoutParams(ctx.dp(52f), ctx.dp(52f)))
        val mi = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mi.addView(label(ctx, ch.id + " " + ch.t, 13f, Tokens.ink, Tokens.weightM(), 1))
        mi.add(label(ctx, if (planet) "第 ${i + 1} / ${q.size} 站 · 已过关 ${pr.done}/${pr.total}"
        else "第 ${i + 1} / ${q.size} 站 · ${p.routeName(b.route)}", 11f, Tokens.sub, 400, 1), t = 2)
        inner.addView(mi, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
            it.marginStart = ctx.dp(11f); it.marginEnd = ctx.dp(8f)
        })
        if (planet) {
            val ring = RingButton(ctx, if (pr.total == 0) 0f else pr.done.toFloat() / pr.total, "play")
            ring.setOnClickListener { go("today") }
            inner.addView(ring, LinearLayout.LayoutParams(ctx.dp(40f), ctx.dp(40f)))
            val qbtn = ImageView(ctx).apply {
                setImageDrawable(Icons.get(ctx, "queue", Tokens.ink))
                contentDescription = "课程章节"
                val pd = ctx.dp(9f)
                setPadding(pd, pd, pd, pd)
                setOnClickListener { go("course") }
            }
            inner.addView(qbtn, LinearLayout.LayoutParams(ctx.dp(38f), ctx.dp(38f)).also { it.marginStart = ctx.dp(4f) })
        } else {
            val pb = ImageView(ctx).apply {
                setImageDrawable(Icons.get(ctx, "play", Tokens.onPri))
                circle(Tokens.pri)
                val pd = ctx.dp(10f)
                setPadding(pd, pd, pd, pd)
                contentDescription = "继续学习"
                setOnClickListener { go("today") }
            }
            inner.addView(pb, LinearLayout.LayoutParams(ctx.dp(38f), ctx.dp(38f)))
        }
        if (!shownOnce) {
            shownOnce = true
            inner.alpha = 0f
            inner.translationY = ctx.dp(10f).toFloat()
            inner.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(SNAP).start()
        }
    }

    private fun pack() = app.currentPack()

    private fun curCh() = run {
        val p = pack()
        val q = p.route(app.store.bag().route)
        if (q.isEmpty()) null else p.chById(q[app.store.bag().idx.coerceIn(0, q.size - 1)])
    }

    private fun View.circle(c: Int) {
        val g = android.graphics.drawable.GradientDrawable()
        g.shape = android.graphics.drawable.GradientDrawable.OVAL
        g.setColor(c)
        background = g
    }

    // ---------------- 横滑丢弃手势 ----------------

    private var sx = 0f; private var sy = 0f; private var dx = 0f; private var t0 = 0L

    private fun bindDrag() {
        var locked = false
        inner.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragging = true; locked = false; movedFlag = false
                    sx = e.rawX; sy = e.rawY; dx = 0f; t0 = System.currentTimeMillis()
                    v.animate().cancel()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging) return@setOnTouchListener false
                    val mx = e.rawX - sx
                    val my = e.rawY - sy
                    if (!locked) {
                        if (Math.abs(mx) > 8 && Math.abs(mx) > Math.abs(my) * 1.2) locked = true
                        else if (Math.abs(my) > 12) { dragging = false; return@setOnTouchListener false }
                        else return@setOnTouchListener true
                    }
                    dx = mx
                    movedFlag = true
                    v.translationX = dx
                    v.alpha = (1f - Math.abs(dx) / (v.width.coerceAtLeast(1) * 0.9f)).coerceIn(0.12f, 1f)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!dragging) return@setOnTouchListener false
                    dragging = false
                    if (!locked) {
                        v.performClick()
                        return@setOnTouchListener false
                    }
                    val vel = Math.abs(dx) / Math.max(1L, System.currentTimeMillis() - t0).toFloat()
                    val wide = width.coerceAtLeast(1)
                    if (Math.abs(dx) > v.width * 0.3f || (vel > 0.55f && Math.abs(dx) > 28f)) {
                        val dir = if (dx > 0) 1f else -1f
                        v.animate().translationX(dir * wide).alpha(0f).setDuration(260)
                            .setInterpolator(android.view.animation.DecelerateInterpolator()).withEndAction {
                                gone = true; shownOnce = false
                                visibility = View.GONE
                                v.translationX = 0f; v.alpha = 1f
                            }.start()
                    } else {
                        v.animate().translationX(0f).alpha(1f).setDuration(320).setInterpolator(SNAP).start()
                    }
                    postDelayed({ movedFlag = false }, 60)
                    true
                }
                else -> false
            }
        }
        inner.setOnClickListener {
            if (movedFlag) { movedFlag = false; return@setOnClickListener }
            go("today")
        }
    }
}
