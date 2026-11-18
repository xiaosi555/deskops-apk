package com.qwen.opsmentor.core

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.qwen.opsmentor.App
import com.qwen.opsmentor.state.LineType
import com.qwen.opsmentor.state.Mark
import com.qwen.opsmentor.state.MarkColor
import com.qwen.opsmentor.state.Seg
import com.qwen.opsmentor.state.TAG_PRESET
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** 线型预览：横线 / 波浪线 / 虚线 */
private class TyPreview(private val ty: String, private val color: Int) : android.graphics.drawable.Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
        this.color = this@TyPreview.color
    }

    override fun draw(c: Canvas) {
        val b = bounds
        val y = b.exactCenterY()
        when (ty) {
            "dash" -> {
                p.pathEffect = DashPathEffect(floatArrayOf(9f, 6f), 0f)
                c.drawLine(b.left.toFloat(), y, b.right.toFloat(), y, p)
                p.pathEffect = null
            }
            "wavy" -> {
                val path = Path()
                path.moveTo(b.left.toFloat(), y)
                var x = b.left.toFloat()
                var up = true
                val wl = 9f
                while (x < b.right) {
                    val nx = min(b.right.toFloat(), x + wl)
                    path.quadTo((x + nx) / 2, y + (if (up) -4f else 4f), nx, y)
                    x = nx; up = !up
                }
                c.drawPath(path, p)
            }
            else -> c.drawLine(b.left.toFloat(), y, b.right.toFloat(), y, p)
        }
    }

    override fun setAlpha(alpha: Int) { p.alpha = alpha }
    override fun setColorFilter(cf: android.graphics.ColorFilter?) { p.colorFilter = cf }
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * 划线浮层（对应 Web selBar）：
 * 选中文字后浮出：拖动手柄 + 起 −/起 +/终 −/终 + + 整段 + 线型 3 + 颜色 4 + 划线 + 取消。
 * 可按住左侧手柄拖到任意位置，位置持久化（ui.bar.x/y），下次打开还在原处。
 */
class SelectionBar(private val act: Activity, private val app: App) {

    private var bar: LinearLayout? = null
    var target: AnnotatedText? = null
        private set
    private var start = 0
    private var end = 0
    private var curTy = LineType.SOLID.id
    private var curCo = MarkColor.BLUE.id
    private val tyViews = ArrayList<Pair<String, View>>();
    private val coViews = ArrayList<Pair<String, View>>()

    fun isOpen(): Boolean = bar != null
    fun currentType(): String = curTy
    fun currentColor(): String = curCo

    fun open(view: AnnotatedText, a: Int, b: Int) {
        close()
        target = view
        start = a; end = max(a + 1, b)
        val ctx = act
        val root = act.findViewById<FrameLayout>(android.R.id.content) ?: return
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            surface(Tokens.l2, 12f, 6f, Tokens.planet)
            setPadding(ctx.dp(6f), ctx.dp(4f), ctx.dp(6f), ctx.dp(4f))
        }
        this.bar = bar
        tyViews.clear(); coViews.clear()

        bar.addView(dragHandle(ctx, bar))
        bar.addView(tiny(ctx, "起−") { shift("s", -1) })
        bar.addView(tiny(ctx, "起+") { shift("s", 1) })
        bar.addView(tiny(ctx, "终−") { shift("e", -1) })
        bar.addView(tiny(ctx, "终+") { shift("e", 1) })
        bar.addView(tiny(ctx, "整段") {
            val v = target ?: return@tiny
            val r = v.blockRange()
            if (r.last > r.first) { start = r.first; end = r.last; v.setSelection(start, end) }
        })
        bar.addView(divider(ctx))
        LineType.values().forEach { t ->
            val box = FrameLayout(ctx).apply {
                flat(Tokens.withAlpha(Tokens.ink, 0.05f), 7f)
                layoutParams = LinearLayout.LayoutParams(ctx.dp(30f), ctx.dp(32f)).also {
                    it.marginStart = ctx.dp(2f); it.marginEnd = ctx.dp(2f)
                }
                contentDescription = t.label
                isClickable = true
            }
            box.addView(View(ctx).apply {
                background = TyPreview(t.id, Tokens.ink)
            }, FrameLayout.LayoutParams(ctx.dp(22f), ctx.dp(14f)).also { it.gravity = Gravity.CENTER })
            box.setOnClickListener { curTy = t.id; paintSwatches(); box.animatePop() }
            bar.addView(box)
            tyViews.add(t.id to box)
        }
        bar.addView(divider(ctx))
        MarkColor.values().forEach { c ->
            val box = FrameLayout(ctx).apply {
                flat(Tokens.withAlpha(Tokens.ink, 0.05f), 7f)
                layoutParams = LinearLayout.LayoutParams(ctx.dp(30f), ctx.dp(32f)).also {
                    it.marginStart = ctx.dp(2f); it.marginEnd = ctx.dp(2f)
                }
                contentDescription = c.label
                isClickable = true
            }
            box.addView(View(ctx).apply { circle(Color.parseColor(c.hex)) },
                FrameLayout.LayoutParams(ctx.dp(17f), ctx.dp(17f)).also { it.gravity = Gravity.CENTER })
            box.setOnClickListener { curCo = c.id; paintSwatches(); box.animatePop() }
            bar.addView(box)
            coViews.add(c.id to box)
        }
        bar.addView(divider(ctx))
        bar.addView(tiny(ctx, "划线", strong = true) { commit() })
        bar.addView(tiny(ctx, "取消") { close() })

        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ctx.dp(44f))
        lp.gravity = Gravity.TOP or Gravity.START
        root.addView(bar, lp)
        bar.post { place() }
        bar.alpha = 0f
        bar.animate().alpha(1f).setDuration(160).setInterpolator(SNAP).start()
        paintSwatches()
    }

    private fun paintSwatches() {
        tyViews.forEach { (id, v) ->
            v.flat(if (id == curTy) Tokens.priSoft else Tokens.withAlpha(Tokens.ink, 0.05f), 7f)
            v.isSelected = id == curTy
        }
        coViews.forEach { (id, v) ->
            v.flat(if (id == curCo) Tokens.priSoft else Tokens.withAlpha(Tokens.ink, 0.05f), 7f)
            v.isSelected = id == curCo
        }
        target?.setSelection(start, end)
    }

    private fun place() {
        val b = bar ?: return
        val v = target ?: return
        val root = act.findViewById<FrameLayout>(android.R.id.content) ?: return
        val loc = IntArray(2); v.getLocationInWindow(loc)
        val scr = IntArray(2); root.getLocationInWindow(scr)
        val w = if (b.width > 0) b.width.toFloat() else app.dp(300f).toFloat()
        val h = if (b.height > 0) b.height.toFloat() else app.dp(44f).toFloat()
        var x = if (app.store.barX >= 0) app.store.barX else (loc[0] - scr[0]).toFloat()
        var y = if (app.store.barY >= 0) app.store.barY else (loc[1] - scr[1] - h - app.dp(10f))
        x = x.coerceIn(app.dp(6f).toFloat(), max(app.dp(6f).toFloat(), root.width.toFloat() - w - app.dp(6f).toFloat()))
        y = y.coerceIn(app.dp(48f).toFloat(), max(app.dp(48f).toFloat(), root.height.toFloat() - h - app.dp(84f).toFloat()))
        b.x = x; b.y = y
        app.store.barX = x; app.store.barY = y
        target?.setSelection(start, end)
    }

    fun close() {
        bar?.let { (it.parent as? ViewGroup)?.removeView(it) }
        bar = null
        val v = target
        target = null
        v?.clearSelection()
    }

    /** 丢弃待确认选区但保留浮层（用于反复唤起） */
    fun detach() {
        target?.clearSelection()
        target = null
    }

    private fun shift(edge: String, delta: Int) {
        val v = target ?: return
        val t = v.textOf()
        if (edge == "s") start = (start + delta).coerceIn(0, max(0, end - 1))
        else end = (end + delta).coerceIn(start + 1, t.length)
        v.setSelection(start, end)
    }

    /** 确认划线：落库 + 立刻可编辑备注 */
    fun commit() {
        val v = target ?: return
        val t = v.textOf()
        val a = start.coerceIn(0, t.length)
        val b = end.coerceIn(a + 1, t.length)
        val txt = t.substring(a, b)
        close()
        if (txt.replace(Regex("\\s"), "").isEmpty()) return
        val sg = Seg(
            sig = v.sigId, s = a, e = b, txt = txt,
            pre = t.substring(max(0, a - 14), a),
            post = t.substring(b, min(t.length, b + 14)),
        )
        val m = MarkEngine.create(v.spec.chId, app.store.cur, curTy, curCo, listOf(sg))
        app.store.marks.add(m)
        app.store.save()
        app.invalidateMarks()
        Sheets.popCheck(act, "已划线")
        MarkEditor.open(act, app, m, { if (it.spec.chId == m.ch) it else null }) { mm -> Pages.locate(mm) }
    }

    // ---------------- 子控件 ----------------

    private fun dragHandle(ctx: Context, bar: LinearLayout): View {
        val root = act.findViewById<FrameLayout>(android.R.id.content)
        val h = ImageView(ctx).apply {
            setImageDrawable(Icons.get(ctx, "more", Tokens.sub))
            contentDescription = "拖动位置"
            layoutParams = LinearLayout.LayoutParams(ctx.dp(24f), ctx.dp(32f)).also { it.marginEnd = ctx.dp(2f) }
        }
        var dx = 0f; var dy = 0f; var down = false
        h.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    down = true; dx = e.rawX - bar.x; dy = e.rawY - bar.y; true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!down || root == null) return@setOnTouchListener false
                    bar.x = (e.rawX - dx).coerceIn(app.dp(6f).toFloat(), root.width.toFloat() - bar.width.toFloat() - app.dp(6f).toFloat())
                    bar.y = (e.rawY - dy).coerceIn(app.dp(48f).toFloat(), root.height.toFloat() - bar.height.toFloat() - app.dp(84f).toFloat())
                    app.store.barX = bar.x; app.store.barY = bar.y
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    down = false
                    app.store.save(); true
                }
                else -> false
            }
        }
        return h
    }

    private fun tiny(ctx: Context, s: String, strong: Boolean = false, fn: () -> Unit): TextView =
        TextView(ctx).apply {
            text = s
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (strong) 13f else 11.5f)
            typeface = typefaceFor(if (strong) Tokens.weightS() else Tokens.weightM())
            setTextColor(if (strong) Tokens.onPri else Tokens.ink)
            flat(if (strong) Tokens.pri else Tokens.withAlpha(Tokens.ink, 0.05f), 7f)
            val w = ctx.dp(if (strong) 52f else 30f)
            layoutParams = LinearLayout.LayoutParams(w, ctx.dp(32f)).also {
                it.marginStart = ctx.dp(2f); it.marginEnd = ctx.dp(2f)
            }
            isClickable = true
            setOnClickListener { animatePop(); fn() }
        }

    private fun divider(ctx: Context): View = View(ctx).apply {
        flat(Tokens.hairc, 1f)
        layoutParams = LinearLayout.LayoutParams(ctx.dp(1f), ctx.dp(22f)).also {
            it.marginStart = ctx.dp(5f); it.marginEnd = ctx.dp(5f)
        }
    }
}

/**
 * 划线编辑面板（对应 Web mark editor panel）。
 * @param resolveBlock 当前页面上可微调该划线的块（找不到则提示先定位到原文）
 * @param locate 定位到原文
 */
object MarkEditor {

    fun open(
        act: Activity, app: App, m: Mark,
        resolveBlock: (AnnotatedText) -> AnnotatedText?,
        locate: (Mark) -> Boolean,
    ) {
        val ctx: Context = act
        val body = vcol(ctx, padH = 16)
        val quote = label(ctx, "“" + m.joinedText() + "”", 14.5f, Tokens.ink, Tokens.weightM())
        quote.flat(Tokens.l3, Tokens.rInner())
        quote.setPadding(ctx.dp(12f), ctx.dp(12f), ctx.dp(12f), ctx.dp(12f))
        body.addView(quote)

        body.add(label(ctx, "线型", 12.5f, Tokens.sub, Tokens.weightM()), t = ctx.dp(14f))
        val tyRow = hrow(ctx)
        LineType.values().forEach { t ->
            val c = Chip(ctx, t.label, m.ty == t.id) { on ->
                if (on) m.ty = t.id
            }
            tyRow.addView(c, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                if (tyRow.childCount > 0) it.marginStart = ctx.dp(6f)
            })
        }
        body.addView(tyRow)

        body.add(label(ctx, "颜色", 12.5f, Tokens.sub, Tokens.weightM()), t = ctx.dp(12f))
        val coRow = hrow(ctx)
        val coChips = ArrayList<Pair<String, Chip>>()
        MarkColor.values().forEach { c ->
            val chip = Chip(ctx, c.label, m.co == c.id) { on -> if (on) m.co = c.id }
            chip.setTextColor(Color.parseColor(c.hex))
            coRow.addView(chip, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                if (coRow.childCount > 0) it.marginStart = ctx.dp(6f)
            })
            coChips.add(c.id to chip)
        }
        body.addView(coRow)
        fun repaintCo() {
            coChips.forEach { (id, chip) ->
                chip.setActive(id == m.co)
                chip.setBackgroundResource(0)
                chip.flat(Tokens.withAlpha(Color.parseColor(MarkColor.of(id).hex), if (id == m.co) 0.20f else 0.07f), 8f)
            }
        }
        coChips.forEach { (id, chip) ->
            chip.onClickExtra = { m.co = id; repaintCo() }
        }
        repaintCo()

        body.add(label(ctx, "标签", 12.5f, Tokens.sub, Tokens.weightM()), t = ctx.dp(12f))
        val tagBox = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val tags = ArrayList(TAG_PRESET)
        m.tags.forEach { if (!tags.contains(it)) tags.add(it) }
        fun buildTags() {
            tagBox.removeAllViews()
            tags.forEach { t ->
                val c = Chip(ctx, t, m.tags.contains(t)) { on ->
                    if (on && !m.tags.contains(t)) m.tags.add(t)
                    if (!on) m.tags.remove(t)
                }
                tagBox.addView(c, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    it.marginEnd = ctx.dp(6f)
                })
            }
            val add = btn(ctx, "＋自定义", "n", small = true)
            add.setOnClickListener {
                Sheets.prompt(ctx, "自定义标签", "", 12, "如：客户投诉高发") { v ->
                    if (!tags.contains(v)) { tags.add(v); m.tags.add(v); buildTags() }
                }
            }
            tagBox.addView(add, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        buildTags()
        body.addView(HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(tagBox, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })

        body.add(label(ctx, "备注（多行）", 12.5f, Tokens.sub, Tokens.weightM()), t = ctx.dp(12f))
        val edit = Sheets.input(ctx, m.n, "写下为什么划它、面试怎么讲", 0, true)
        edit.minLines = 3
        body.addView(edit)

        val adjRow = hrow(ctx)
        adjRow.setPadding(0, ctx.dp(12f), 0, 0)
        fun doAdjust(edge: String, delta: Int) {
            val host = Pages.currentBlocks().firstOrNull { it.spec.chId == m.ch && MarkEngine.resolve(it.textOf(), it.sigId, listOf(m), m.ch).isNotEmpty() }
            if (host == null) {
                Sheets.toast(ctx, "这条划线不在当前页面，先点「定位到原文」再微调")
                return
            }
            if (MarkEngine.adjust(m, host.textOf(), host.sigId, edge, delta)) {
                app.invalidateMarks()
                quote.text = "“" + m.joinedText() + "”"
                val sg = m.segs[if (edge == "s") 0 else m.segs.size - 1]
                host.setSelection(sg.s, sg.e)
            }
        }
        listOf("起 −" to ("s" to -1), "起 +" to ("s" to 1), "终 −" to ("e" to -1), "终 +" to ("e" to 1)).forEach { (name, spec) ->
            val b = btn(ctx, name, "n", small = true)
            b.setOnClickListener { doAdjust(spec.first, spec.second) }
            adjRow.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.marginEnd = ctx.dp(6f)
            })
        }
        val whole = btn(ctx, "整段", "n", small = true)
        whole.setOnClickListener {
            val host = Pages.currentBlocks().firstOrNull { it.spec.chId == m.ch }
            if (host == null) { Sheets.toast(ctx, "这条划线不在当前页面，先点「定位到原文」再微调"); return@setOnClickListener }
            MarkEngine.expandToBlock(m, host.textOf(), host.sigId)
            app.invalidateMarks()
            quote.text = "“" + m.joinedText() + "”"
        }
        adjRow.addView(whole)
        body.addView(adjRow)

        val ops = hrow(ctx)
        ops.setPadding(0, ctx.dp(8f), 0, 0)
        val go = btn(ctx, "定位到原文", "n", small = true)
        go.setOnClickListener { if (!locate(m)) Sheets.toast(ctx, "原文在别的页面，去课程或知识详解里找这一条") }
        ops.addView(go, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = ctx.dp(6f) })
        val del = btn(ctx, "删除这条划线", "danger", small = true)
        del.setOnClickListener {
            Sheets.confirm(ctx, "删除这条划线？同一处的其它划线不受影响。", "删除划线", "删除", true) {
                app.store.marks.removeAll { x -> x.id == m.id }
                app.store.save(); app.invalidateMarks(); Sheets.toast(ctx, "已删除")
            }
        }
        ops.addView(del)
        body.addView(ops)

        Sheets.sheet(ctx, "编辑划线", body, "保存", onSave = {
            m.n = edit.text.toString().trim()
            m.up = System.currentTimeMillis()
            Pages.currentBlocks().forEach { it.clearSelection() }
            app.store.save()
            app.invalidateMarks()
            Sheets.popCheck(ctx, "划线已保存")
            true
        })
    }
}

/** 通用 chip：选中态主色弱底 + 主色字 */
class Chip(ctx: Context, text: String, var on: Boolean, val onChange: (Boolean) -> Unit = {}) : LinearLayout(ctx) {
    private val tv: TextView
    var onClickExtra: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        tv = label(ctx, text, 13f, if (on) Tokens.pri else Tokens.ink, Tokens.weightM())
        addView(tv, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        paint()
        isClickable = true
        setOnClickListener {
            on = !on
            paint()
            animatePop()
            onChange(on)
            onClickExtra?.invoke()
        }
    }

    fun setTextColor(c: Int) { tv.setTextColor(c) }
    fun setActive(v: Boolean) { on = v; paint() }

    private fun paint() {
        flat(if (on) Tokens.priSoft else if (Tokens.planet) Tokens.l3 else Tokens.withAlpha(Tokens.ink, 0.05f), 8f)
        if (!on) tv.setTextColor(if (Tokens.dark) 0xFFDFE5EE.toInt() else Tokens.ink) else tv.setTextColor(Tokens.pri)
        val p = context.dp(13f)
        setPadding(p, context.dp(7f), p, context.dp(7f))
    }

    fun setText(s: String) { tv.text = s }
}
