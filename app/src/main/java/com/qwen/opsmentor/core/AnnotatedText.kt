package com.qwen.opsmentor.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import com.qwen.opsmentor.state.Mark
import com.qwen.opsmentor.state.MarkColor

/** 一个可划线文本块：块 id（同 Web data-blk）+ 原文 */
data class BlockSpec(val chId: String, val blkId: String, val text: String)

/** 划线绘制结果：区间 + 线型颜色 + 分层 */
private data class Draw(val start: Int, val end: Int, val ty: String, val co: Int, val layer: Int, val mark: Mark)

/**
 * 可划线文本块。
 *
 * 交互对齐 Web：
 * - 长按（触屏）= 选中该块并唤起划线浮层；
 * - 点已有划线 = 打开划线编辑面板；
 * - 多条划线在同一区间并存时按下划线偏移分层，互不遮挡。
 *
 * 与 Web 的差异只在"选字粒度"：Android 用长按整块 + 浮层内按字符微调（起 −/起 +/终 −/终 +/整段）达成同样精度。
 */
@SuppressLint("ViewConstructor")
class AnnotatedText(
    ctx: Context,
    var spec: BlockSpec,
    var marksProvider: () -> List<Mark>,
    var onPick: (Mark) -> Unit = {},
    var onSelect: (AnnotatedText, Int, Int) -> Unit = { _, _, _ -> },
) : AppCompatTextView(ctx) {

    private val sig: String = MarkEngine.sigOf(spec.text)
    private var hits: List<Draw> = emptyList()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dpF(1.6f)
    }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    /** 当前待确认选区（浮层打开时高亮） */
    var selStart = -1
    var selEnd = -1

    private val density = context.resources.displayMetrics.density
    private fun dpF(v: Float) = v * density
    private fun px(v: Int) = (v * density).toInt()

    /** 字号决定的下划线基准偏移（Web 用 em） */
    private val emPx: Float get() = textSize

    init {
        text = spec.text
        includeFontPadding = true
        setTextColor(Tokens.ink)
        isLongClickable = true
        setTextIsSelectable(false)
    }

    val sigId: String get() = sig

    /** 重算命中并重绘：外部在划线数据变化后调用 */
    fun refresh() {
        val resolved = MarkEngine.resolve(spec.text, sig, marksProvider(), spec.chId)
        val layers = MarkEngine.layersFor(resolved)
        hits = resolved.map { (m, range) ->
            Draw(range.first, range.last + 1, m.ty, android.graphics.Color.parseColor(MarkColor.of(m.co).hex), layers[m] ?: 0, m)
        }
        invalidate()
    }

    fun marksHere(): List<Mark> = hits.map { it.mark }.distinctBy { it.id }

    fun setSelection(a: Int, b: Int) {
        selStart = a.coerceIn(0, spec.text.length)
        selEnd = b.coerceIn(0, spec.text.length)
        if (selEnd < selStart) {
            val t = selStart; selStart = selEnd; selEnd = t
        }
        invalidate()
    }

    fun clearSelection() {
        selStart = -1; selEnd = -1; invalidate()
    }

    private var gesture: GestureDetector? = null

    private fun ensureGesture(): GestureDetector {
        gesture?.let { return it }
        val gd = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val off = offsetAt(e.x, e.y)
                val hit = hits.lastOrNull { off in it.start until it.end }
                if (hit != null) {
                    onPick(hit.mark)
                    return true
                }
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                val off = offsetAt(e.x, e.y)
                if (off < 0) return
                val range = sentenceRange(off)
                onSelect(this@AnnotatedText, range.first, range.last)
            }
        })
        gesture = gd
        return gd
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        ensureGesture().onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    /** 触点 → 字符偏移 */
    fun offsetAt(x: Float, y: Float): Int {
        val layout = layout ?: return -1
        val l = layout.getLineForVertical((y - paddingTop).toInt())
        if (l < 0 || l >= layout.lineCount) return -1
        return layout.getOffsetForHorizontal(l, x - paddingLeft)
    }

    /** 按句读切分，长按时先命中整句（再用浮层按字符微调） */
    fun sentenceRange(off: Int): IntRange {
        val t = spec.text
        if (t.isEmpty()) return 0 until 0
        val seps = charArrayOf('。', '！', '？', '；', '，', '\n', '.', '!', '?', ';', ',')
        var s = off
        while (s > 0 && !seps.contains(t[s - 1])) s--
        var e = off
        while (e < t.length && !seps.contains(t[e])) e++
        if (e < t.length) e++  // 含标点
        if (e - s < 2) return off until minOf(t.length, off + 1)
        return s until e
    }

    /** 整段区间（浮层「整段」按钮用） */
    fun blockRange(): IntRange = 0 until spec.text.length

    fun textOf(): String = spec.text

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = layout ?: return
        // 1. 待确认选区高亮
        if (selStart in 0 until selEnd) {
            selPaint.color = Tokens.withAlpha(Tokens.pri, 0.18f)
            drawRanges(canvas, layout, selStart, selEnd) { l, lo, hi, base ->
                canvas.drawRect(padLeft() + lo, base - emPx * 0.92f, padLeft() + hi, base + dpF(4f), selPaint)
            }
        }
        // 2. 划线（分层下划线）
        hits.forEach { h ->
            linePaint.color = h.co
            linePaint.pathEffect = when (h.ty) {
                "dash" -> DashPathEffect(floatArrayOf(dpF(4.5f), dpF(3.2f)), 0f)
                "wavy" -> null
                else -> null
            }
            val off = emPx * (0.14f + 0.24f * h.layer)
            drawRanges(canvas, layout, h.start, h.end) { l, lo, hi, base ->
                val y = base + dpF(2.5f) + off
                if (h.ty == "wavy") {
                    val path = Path()
                    val wl = dpF(7.5f)
                    val amp = dpF(1.7f)
                    var x = lo
                    path.moveTo(padLeft() + x, y)
                    var up = true
                    while (x < hi) {
                        val nx = minOf(hi, x + wl / 2f)
                        path.quadTo(padLeft() + (x + nx) / 2f, y + (if (up) -amp else amp), padLeft() + nx, y)
                        x = nx
                        up = !up
                    }
                    canvas.drawPath(path, linePaint)
                } else {
                    canvas.drawLine(padLeft() + lo, y, padLeft() + hi, y, linePaint)
                }
            }
        }
    }

    private inline fun drawRanges(canvas: Canvas, layout: android.text.Layout, start: Int, end: Int, body: (Int, Float, Float, Float) -> Unit) {
        if (end <= start) return
        val first = layout.getLineForOffset(start)
        val last = layout.getLineForOffset(maxOf(start, end - 1))
        for (l in first..last) {
            val lineStart = layout.getLineStart(l)
            val lineEnd = layout.getLineEnd(l)
            val a = maxOf(start, lineStart)
            val b = minOf(end, lineEnd)
            if (b <= a) continue
            val lo = if (a == lineStart) layout.getLineLeft(l) else layout.getPrimaryHorizontal(a)
            val hi = if (b == lineEnd) layout.getLineRight(l) else layout.getPrimaryHorizontal(b)
            if (hi - lo < dpF(1f)) continue
            body(l, lo, hi, layout.getLineBaseline(l) + paddingTop.toFloat())
        }
    }

    private fun padLeft() = paddingLeft
    private fun minOf(a: Int, b: Int) = if (a < b) a else b
    private fun maxOf(a: Int, b: Int) = if (a > b) a else b
}
