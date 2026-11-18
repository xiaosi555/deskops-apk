package com.qwen.opsmentor.core

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import com.qwen.opsmentor.state.Mark
import java.io.File

/**
 * 极简 Markdown 渲染（对应 Web 的 mdBlocks / mdToHtml）。
 * 支持：# 标题、- 列表、| 表格、``` 代码、> 引用、**粗体**、`代码`、~~删除~~。
 * 每个可划线段落产出 AnnotatedText（块 id 与 Web 的 M{行号}_ 对齐）。
 */
class MarkdownView(
    private val ctx: Context,
    private val md: String,
    private val chId: String = "",
    private val blkPrefix: String = "MD",
    private val marksProvider: () -> List<Mark> = { emptyList() },
    private val onPick: (Mark) -> Unit = {},
    private val onSelect: (AnnotatedText, Int, Int) -> Unit = { _, _, _ -> },
) : LinearLayout(ctx) {

    init {
        orientation = VERTICAL
        render()
    }

    val blocks = mutableListOf<AnnotatedText>()

    private fun render() {
        val lines = md.split("\n")
        var i = 0
        val parts = mutableListOf<View>()
        while (i < lines.size) {
            val raw = lines[i]
            val l = raw.trimEnd()
            when {
                l.startsWith("```") -> {
                    val sb = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trimEnd().startsWith("```")) {
                        sb.append(lines[i]).append("\n"); i++
                    }
                    i++
                    parts.add(mono(ctx, sb.toString().trimEnd('\n')))
                    continue
                }
                l.startsWith("|") && l.contains("|") -> {
                    val rows = mutableListOf<List<String>>()
                    while (i < lines.size && lines[i].trimEnd().startsWith("|")) {
                        val cells = lines[i].trim().trim('|').split("|").map { it.trim() }
                        if (cells.joinToString("").replace("-", "").replace(":", "").replace(" ", "").isNotEmpty()) rows.add(cells)
                        i++
                    }
                    parts.add(Table(ctx).build(rows.map { it.toMutableList() }))
                    continue
                }
                l.startsWith("#") -> {
                    val lvl = l.takeWhile { it == '#' }.count()
                    val txt = l.drop(lvl).trim()
                    parts.add(label(ctx, inline(ctx, txt), 18f - lvl, Tokens.ink, Tokens.weightH()).also {
                        (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, ctx.dp(if (lvl <= 2) 14 else 8), 0, ctx.dp(4))
                    })
                }
                l.startsWith("- ") || l.startsWith("* ") -> {
                    parts.add(bullet(inline(ctx, l.substring(2).trim())))
                }
                Regex("^\\d+\\.\\s").containsMatchIn(l) -> {
                    val n = l.substringBefore('.').trim()
                    parts.add(bullet(inline(ctx, l.substringAfter('.').trim()), n + "."))
                }
                l.startsWith(">") -> {
                    parts.add(noteBox(ctx, "", l.trimStart('>', ' ')))
                }
                l.isBlank() -> { /* 空行跳过 */ }
                else -> parts.add(paragraph(l, blkPrefix + i))
            }
            i++
        }
        parts.forEach {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { lp ->
                lp.topMargin = ctx.dp(4f)
            })
        }
    }

    private fun bullet(sp: CharSequence, marker: String = "•"): LinearLayout {
        val row = hrow(ctx)
        val m = label(ctx, marker, 14.5f, Tokens.pri, Tokens.weightM())
        row.addView(m, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.marginStart = ctx.dp(4f); it.marginEnd = ctx.dp(8f)
        })
        val t = AnnotatedText(ctx, BlockSpec(chId, blkPrefix + "-b" + blocks.size, plainOf(sp)), marksProvider, onPick, onSelect)
        t.size(14.5f)
        t.weight(Tokens.weightB())
        blocks.add(t)
        row.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    private fun paragraph(l: String, blkId: String): AnnotatedText {
        val t = AnnotatedText(ctx, BlockSpec(chId, blkId, plainOf(inline(ctx, l))), marksProvider, onPick, onSelect)
        t.text = inline(ctx, l)
        t.size(15f)
        t.weight(Tokens.weightB())
        t.setLineSpacing(0f, 1.28f)
        blocks.add(t)
        return t
    }

    private fun plainOf(cs: CharSequence): String = cs.toString()

    companion object {
        /** 行内格式 → Spannable（不 innerHTML，天然安全） */
        fun inline(ctx: Context, s: String): CharSequence {
            val sb = SpannableStringBuilder()
            var i = 0
            val plain = s
            while (i < plain.length) {
                val c = plain[i]
                if (c == '*' && i + 1 < plain.length && plain[i + 1] == '*') {
                    val end = plain.indexOf("**", i + 2)
                    if (end > 0) {
                        val seg = plain.substring(i + 2, end)
                        val st = sb.length
                        sb.append(seg)
                        sb.setSpan(StyleSpan(Typeface.BOLD), st, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.setSpan(ForegroundColorSpan(Tokens.ink), st, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = end + 2
                        continue
                    }
                }
                if (c == '`') {
                    val end = plain.indexOf('`', i + 1)
                    if (end > 0) {
                        val seg = plain.substring(i + 1, end)
                        val st = sb.length
                        sb.append(seg)
                        sb.setSpan(object : android.text.style.MetricAffectingSpan() {
                            override fun updateMeasureState(p: android.text.TextPaint) {
                                p.typeface = Typeface.MONOSPACE
                            }

                            override fun updateDrawState(p: android.text.TextPaint) {
                                p.typeface = Typeface.MONOSPACE
                                p.color = if (Tokens.dark) 0xFFCFE8D8.toInt() else 0xFF1B4D36.toInt()
                                p.bgColor = Tokens.withAlpha(Tokens.pri, 0.10f)
                            }
                        }, st, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = end + 1
                        continue
                    }
                }
                if (c == '*' && !(i + 1 < plain.length && plain[i + 1] == '*')) {
                    val end = plain.indexOf('*', i + 1)
                    if (end > i + 1) {
                        val seg = plain.substring(i + 1, end)
                        val st = sb.length
                        sb.append(seg)
                        sb.setSpan(StyleSpan(Typeface.ITALIC), st, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = end + 1
                        continue
                    }
                }
                if (c == '~' && i + 1 < plain.length && plain[i + 1] == '~') {
                    val end = plain.indexOf("~~", i + 2)
                    if (end > 0) {
                        val seg = plain.substring(i + 2, end)
                        val st = sb.length
                        sb.append(seg)
                        sb.setSpan(android.text.style.StrikethroughSpan(), st, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = end + 2
                        continue
                    }
                }
                val st = sb.length
                sb.append(c)
                sb.setSpan(ForegroundColorSpan(Tokens.ink), st, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                i++
            }
            return sb
        }
    }
}

/**
 * 双轨对照 / 细节表格：列宽按内容长度分配（限最宽最窄），横向可滚。
 */
class Table(private val ctx: Context) {

    /** 列宽按内容长度分配（限最宽 / 最窄），与 Web 表格同规则 */
    fun build(rows: List<List<String>>): LinearLayout {
        val grid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        if (rows.isEmpty()) return grid
        val cols = rows.maxOf { it.size }
        val weights = DoubleArray(cols) { c ->
            var mx = 0.0
            rows.forEach { r -> if (c < r.size) mx = maxOf(mx, r[c].length.toDouble()) }
            mx / (if (c == 0) 1.0 else 1.15)
        }
        val sum = weights.sum().coerceAtLeast(1.0)
        val avail = ctx.resources.displayMetrics.widthPixels - ctx.dp(64f)
        val widths = IntArray(cols) { c ->
            val min = ctx.dp(if (cols > 2) 56f else 72f)
            maxOf(min, (avail * (weights[c] / sum)).toInt())
        }
        fun row(cells: List<String>, isHead: Boolean): LinearLayout {
            val r = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0 until cols) {
                val tv = label(ctx, cells.getOrElse(c) { "" }, if (isHead) 12.5f else 13.5f,
                    if (isHead) Tokens.sub else Tokens.ink, if (isHead) Tokens.weightS() else Tokens.weightB())
                tv.setPadding(ctx.dp(9f), ctx.dp(8f), ctx.dp(9f), ctx.dp(8f))
                r.addView(tv, LinearLayout.LayoutParams(widths[c], ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            return r
        }
        val headRow = row(rows.first(), true)
        headRow.flat(Tokens.withAlpha(Tokens.pri, if (Tokens.planet) 0.10f else 0.06f), ctx.dp(Tokens.rInner()).toFloat())
        grid.addView(headRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        rows.drop(1).forEachIndexed { idx, cells ->
            val r = row(cells, false)
            if (idx % 2 == 1) r.flat(Tokens.l3, ctx.dp(Tokens.rInner()).toFloat())
            grid.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            grid.addView(View(ctx).apply { flat(Tokens.hairc, 1f) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(1f)))
        }
        return grid
    }
}

/** 封面：课程图标 / 用户上传图片；星球版圆角 6、极简版 17 */
object Covers {
    fun view(ctx: Context, sizePx: Int, coverFile: String?, coverClass: String, glyph: String, round: Float = Tokens.rCover()): View {
        val v = android.widget.FrameLayout(ctx)
        v.layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
        val r = ctx.dp(round).toFloat()
        val img = ImageView(ctx)
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        val file = coverFile?.let { File(it) }
        if (file != null && file.exists()) {
            val bmp = android.graphics.BitmapFactory.decodeFile(file.path)
            if (bmp != null) img.setImageBitmap(bmp)
            v.flat(Tokens.priSoft, r)
        } else {
            // HTML v6 将 cv-a / cv-b / cv-c 统一为同一组绿色课程封面渐变。
            val colors = if (Tokens.dark) {
                intArrayOf(0xFF2A9E5B.toInt(), 0xFF1F7D46.toInt())
            } else {
                intArrayOf(0xFF34C374.toInt(), 0xFF27AE60.toInt())
            }
            val g = android.graphics.drawable.GradientDrawable()
            g.orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
            g.colors = colors
            g.cornerRadius = r
            v.background = g
            img.setImageDrawable(Icons.get(ctx, glyph, 0xFFFFFFFF.toInt()))
            img.scaleType = ImageView.ScaleType.FIT_CENTER
            val pd = (sizePx * 0.30f).toInt()
            img.setPadding(pd, pd, pd, pd)
        }
        v.addView(img, android.widget.FrameLayout.LayoutParams(sizePx, sizePx))
        return v
    }
}

/** 播放环：40dp 圆环进度 + 中心图标（Web .rbtn） */
class RingButton(ctx: Context, private var progress: Float, private val icon: String = "play", private val sizeDp: Float = 40f) : LinearLayout(ctx) {
    private val ring = RingView(context, progress)
    private val ic: ImageView

    init {
        val s = ctx.dp(sizeDp)
        layoutParams = LinearLayout.LayoutParams(s, s)
        addView(ring, LayoutParams(s, s))
        ic = ImageView(ctx).apply {
            setImageDrawable(Icons.get(ctx, icon, Tokens.ink))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val pd = ctx.dp(sizeDp * 0.31f)
        ic.setPadding(pd, pd, pd, pd)
        addView(ic, LayoutParams(ctx.dp(sizeDp * 0.56f), ctx.dp(sizeDp * 0.56f)).also { it.gravity = Gravity.CENTER })
        isClickable = true
    }

    fun setProgress(p: Float) {
        progress = p.coerceIn(0f, 1f); ring.p = progress; ring.invalidate()
    }

    fun setIcon(name: String, tint: Int = Tokens.ink) {
        ic.setImageDrawable(Icons.get(context, name, tint))
    }
}

class RingView(ctx: Context, var p: Float) : android.view.View(ctx) {
    private val bg = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 3f
        color = Tokens.withAlpha(Tokens.sub, 0.2f)
    }
    private val fg = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 3f
        strokeCap = android.graphics.Paint.Cap.ROUND
        color = Tokens.pri
    }

    override fun onDraw(c: android.graphics.Canvas) {
        val d = (width.coerceAtMost(height) - fg.strokeWidth * 2).coerceAtLeast(1f)
        val cx = width / 2f; val cy = height / 2f
        val rect = android.graphics.RectF(cx - d / 2, cy - d / 2, cx + d / 2, cy + d / 2)
        c.drawArc(rect, 0f, 360f, false, bg)
        if (p > 0f) c.drawArc(rect, -90f, 360f * p, false, fg)
    }
}

/** 顶部装饰（12 种）：自绘 View，星球版缺省为竖线 */
class DecoView(ctx: Context, private var kind: String, private val heightPx: Int) : android.view.View(ctx) {

    /** 切换装饰类型（设置页选择后立即生效） */
    fun update(k: String, h: Int = heightPx) {
        if (kind == k) return
        kind = k
        invalidate()
    }

    private fun isNone() = kind == "none"
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private val runnable = object : Runnable {
        override fun run() {
            t += 0.016f
            invalidate()
            if (dynamic() && !isNone() && isAttachedToWindow && !Tokens.reducedMotion) postDelayed(this, 40)
        }
    }

    private fun dynamic() = kind in setOf("aurora", "stars", "ripple", "scan", "beam")

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (dynamic() && !isNone()) post(runnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow(); removeCallbacks(runnable)
    }

    override fun onDraw(c: android.graphics.Canvas) {
        if (kind == "none") return
        val w = width.toFloat(); val h = height.toFloat()
        val pri = Tokens.pri
        when (kind) {
            "lines" -> {
                val density = resources.displayMetrics.density
                paint.strokeWidth = density
                if (Tokens.planet) {
                    // HTML 星球版顶部纹理：两层 3px / 7px 细竖纹，向下柔和淡出。
                    val lineTint = if (Tokens.dark) Tokens.mix(0xFFCDD5D1.toInt(), pri, 0.10f)
                                   else Tokens.mix(0xFF7F8784.toInt(), pri, 0.12f)
                    val colorsA = intArrayOf(
                        Tokens.withAlpha(lineTint, 0.09f),
                        Tokens.withAlpha(lineTint, 0.0765f),
                        Tokens.withAlpha(lineTint, 0.036f),
                        android.graphics.Color.TRANSPARENT,
                    )
                    val colorsB = intArrayOf(
                        Tokens.withAlpha(lineTint, 0.06f),
                        Tokens.withAlpha(lineTint, 0.051f),
                        Tokens.withAlpha(lineTint, 0.024f),
                        android.graphics.Color.TRANSPARENT,
                    )
                    val stops = floatArrayOf(0f, 0.40f, 0.76f, 1f)
                    var x = 0f
                    paint.shader = android.graphics.LinearGradient(0f, 0f, 0f, h, colorsA, stops, android.graphics.Shader.TileMode.CLAMP)
                    while (x < w) { c.drawLine(x, 0f, x, h, paint); x += density * 3f }
                    x = 0f
                    paint.shader = android.graphics.LinearGradient(0f, 0f, 0f, h, colorsB, stops, android.graphics.Shader.TileMode.CLAMP)
                    while (x < w) { c.drawLine(x, 0f, x, h, paint); x += density * 7f }
                    paint.shader = null
                } else {
                    paint.color = Tokens.withAlpha(pri, if (Tokens.dark) 0.10f else 0.07f)
                    var x = w * 0.06f
                    while (x < w) {
                        c.drawLine(x, 0f, x, h * (0.45f + 0.5f * ((x % 7f) / 7f)), paint)
                        x += density * 26f
                    }
                }
            }
            "hair" -> {
                val g = android.graphics.LinearGradient(0f, 0f, w, 0f, intArrayOf(0, Tokens.withAlpha(pri, 0.25f), 0), null, android.graphics.Shader.TileMode.CLAMP)
                paint.shader = g
                c.drawRect(0f, h * 0.6f, w, h * 0.6f + resources.displayMetrics.density, paint)
                paint.shader = null
            }
            "grid" -> {
                paint.color = Tokens.withAlpha(pri, 0.06f)
                paint.strokeWidth = resources.displayMetrics.density
                val step = resources.displayMetrics.density * 22f
                var x = 0f
                while (x < w) { c.drawLine(x, 0f, x, h, paint); x += step }
                var y = 0f
                while (y < h) { c.drawLine(0f, y, w, y, paint); y += step }
            }
            "dots" -> {
                paint.color = Tokens.withAlpha(pri, 0.12f)
                val step = resources.displayMetrics.density * 18f
                var y = step / 2
                while (y < h) {
                    var x = step / 2
                    while (x < w) { c.drawCircle(x, y, resources.displayMetrics.density * 1.2f, paint); x += step }
                    y += step
                }
            }
            "contour" -> {
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = resources.displayMetrics.density * 1.2f
                for (i in 0..5) {
                    paint.color = Tokens.withAlpha(pri, 0.05f + i * 0.01f)
                    val path = android.graphics.Path()
                    path.moveTo(0f, h * (0.2f + i * 0.13f))
                    path.cubicTo(w * 0.3f, h * (0.05f + i * 0.13f), w * 0.6f, h * (0.35f + i * 0.13f), w, h * (0.18f + i * 0.13f))
                    c.drawPath(path, paint)
                }
            }
            "ink" -> {
                val rg = android.graphics.RadialGradient(w * 0.7f, h * 0.3f, h * 0.9f, intArrayOf(Tokens.withAlpha(pri, 0.16f), 0), null, android.graphics.Shader.TileMode.CLAMP)
                paint.shader = rg
                c.drawRect(0f, 0f, w, h, paint)
                paint.shader = null
            }
            "aurora" -> {
                val shift = (Math.sin(t.toDouble()) * 0.1 + 0.2).toFloat()
                val rg = android.graphics.LinearGradient(0f, 0f, w, h, intArrayOf(Tokens.withAlpha(pri, 0.02f), Tokens.withAlpha(pri, 0.22f), Tokens.withAlpha(pri, 0.02f)), floatArrayOf(0f, shift, 1f), android.graphics.Shader.TileMode.CLAMP)
                paint.shader = rg
                c.drawRect(0f, 0f, w, h, paint)
                paint.shader = null
            }
            "stars" -> {
                paint.color = Tokens.withAlpha(pri, 0.5f)
                var i = 0
                while (i < 46) {
                    val x = ((i * 137.5f) % w)
                    val y = ((i * 71.3f) % h)
                    val r = resources.displayMetrics.density * (0.8f + (i % 3) * 0.5f)
                    val a = 0.25f + 0.6f * kotlin.math.abs(kotlin.math.sin((t + i).toDouble())).toFloat()
                    paint.alpha = (a * 255).toInt()
                    c.drawCircle(x, y, r, paint)
                    i++
                }
            }
            "ripple" -> {
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = resources.displayMetrics.density
                val cx = w / 2f; val cy = h * 0.9f
                for (i in 0..3) {
                    val rr = (h * 0.2f * i + (t * 30f) % (h * 0.2f))
                    paint.color = Tokens.withAlpha(pri, (0.16f - i * 0.03f).coerceAtLeast(0f))
                    c.drawCircle(cx, cy, rr, paint)
                }
            }
            "scan" -> {
                val y = (t * 60f) % h
                val g = android.graphics.LinearGradient(0f, y - h * 0.2f, 0f, y, intArrayOf(0, Tokens.withAlpha(pri, 0.18f)), null, android.graphics.Shader.TileMode.CLAMP)
                paint.shader = g
                c.drawRect(0f, (y - h * 0.2f).coerceAtLeast(0f), w, y, paint)
                paint.shader = null
            }
            "beam" -> {
                val x = w * (0.5f + 0.35f * Math.sin(t * 0.6).toFloat())
                val g = android.graphics.LinearGradient(x - w * 0.2f, 0f, x + w * 0.2f, 0f, intArrayOf(0, Tokens.withAlpha(pri, 0.14f), 0), null, android.graphics.Shader.TileMode.CLAMP)
                paint.shader = g
                c.drawRect(0f, 0f, w, h, paint)
                paint.shader = null
            }
        }
    }
}
