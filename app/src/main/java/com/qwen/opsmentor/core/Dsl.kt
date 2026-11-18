package com.qwen.opsmentor.core

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.PathInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.HorizontalScrollView
import android.widget.TextView
import android.text.TextUtils
import androidx.core.widget.NestedScrollView

/** 尺寸与动效基础量（与 Web 令牌同源） */
fun Context.dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
fun Context.dp(v: Int): Int = dp(v.toFloat())
fun Context.sp(v: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

/** 吸附曲线 130ms：导航、按压、状态切换 */
val SNAP = PathInterpolator(0.22f, 0.74f, 0.18f, 1f)
const val SNAP_D = 130L
/** 弹出曲线 360ms：复制反馈、播放环、chip 按压 */
val POP = PathInterpolator(0.32f, 1.34f, 0.5f, 1f)
const val POP_D = 360L

fun View.animatePop() {
    if (Tokens.reducedMotion) return
    animate().cancel()
    scaleX = 1f; scaleY = 1f
    animate().scaleX(0.94f).scaleY(0.94f).setDuration(SNAP_D / 2).setInterpolator(SNAP).withEndAction {
        animate().scaleX(1f).scaleY(1f).setDuration(SNAP_D / 2).setInterpolator(SNAP).start()
    }.start()
}

/** 条目入场：itemIn（淡入 + 上移 8dp），支持错峰 delay */
fun View.itemIn(delay: Long = 0) {
    if (Tokens.reducedMotion) {
        alpha = 1f; translationY = 0f; scaleX = 1f; scaleY = 1f
        return
    }
    // HTML @keyframes itemIn：460ms 淡入、上移 14px，并轻微回正缩放。
    animate().cancel()
    alpha = 0f
    translationY = context.dp(14f).toFloat()
    scaleX = 0.985f
    scaleY = 0.985f
    animate()
        .alpha(1f)
        .translationY(0f)
        .scaleX(1f)
        .scaleY(1f)
        .setStartDelay(delay)
        .setDuration(460L)
        .setInterpolator(SNAP)
        .start()
}

/** 数字滚动：850ms 三次缓出 */
fun TextView.countUp(to: Int, suffix: String = "") {
    if (Tokens.reducedMotion || to == 0) {
        text = "$to$suffix"; return
    }
    val va = ValueAnimator.ofInt(0, to)
    va.duration = 850
    va.interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
    va.addUpdateListener { text = "${it.animatedValue}$suffix" }
    va.start()
}

/**
 * 打字机标题：逐字 30ms 间隔浮现。
 * 保持单 TextView（用 alpha 渐变的前景动画），避免逐字建 View 的开销。
 */
fun TextView.typeset(maxChars: Int = 72) {
    val raw = text?.toString() ?: ""
    if (raw.isEmpty()) return
    if (Tokens.reducedMotion || raw.length > maxChars) {
        alpha = 0f
        animate().alpha(1f).setDuration(240).setInterpolator(SNAP).start()
        return
    }
    val tv = this
    tv.text = ""
    tv.contentDescription = raw
    val chars = raw.toList()
    val total = chars.size
    var shown = 0
    val step = Runnable {
        if (shown < total) {
            shown++
            tv.text = String(chars.subList(0, shown).toCharArray())
        }
    }
    repeat(total) { postDelayed(step, (it * 30).toLong()) }
}

// ---------------- 容器 ----------------

fun vcol(ctx: Context, padH: Int = 0, padV: Int = 0): LinearLayout = LinearLayout(ctx).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(ctx.dp(padH), ctx.dp(padV), ctx.dp(padH), ctx.dp(padV))
}

fun hrow(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

fun ctx_wrap(ctx: Context): FrameLayout = FrameLayout(ctx)

/**
 * 便捷添加：w/h 支持 MATCH_PARENT / WRAP_CONTENT，l t r b 与 gap 一律按 dp 传入并自动换算成像素。
 * 对应 Web 里 h(tag, cls, txt) + appendChild + margin 的组合。
 */
fun ViewGroup.add(v: View, w: Int = ViewGroup.LayoutParams.MATCH_PARENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
                  l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0, weight: Float = 0f): View {
    val d = context.resources.displayMetrics.density
    val lp = if (this is LinearLayout) LinearLayout.LayoutParams(w, h) else ViewGroup.LayoutParams(w, h)
    if (lp is LinearLayout.LayoutParams) lp.weight = weight
    if (lp is ViewGroup.MarginLayoutParams) if (lp is ViewGroup.MarginLayoutParams) if (lp is ViewGroup.MarginLayoutParams) if (lp is ViewGroup.MarginLayoutParams) if (lp is ViewGroup.MarginLayoutParams) if (lp is ViewGroup.MarginLayoutParams) if (lp is ViewGroup.MarginLayoutParams) if (lp is ViewGroup.MarginLayoutParams) if (lp is ViewGroup.MarginLayoutParams) if (lp is ViewGroup.MarginLayoutParams) lp.setMargins((l * d).toInt(), (t * d).toInt(), (r * d).toInt(), (b * d).toInt())
    addView(v, lp)
    return v
}

fun LinearLayout.gap(px: Int) {
    addView(View(context), LinearLayout.LayoutParams(0, context.dp(px)))
}

fun nested(ctx: Context, content: View): NestedScrollView = NestedScrollView(ctx).apply {
    isFillViewport = true
    addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    overScrollMode = View.OVER_SCROLL_NEVER
    clipToPadding = false
}

fun hscroll(ctx: Context, content: View): HorizontalScrollView = HorizontalScrollView(ctx).apply {
    addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    isHorizontalScrollBarEnabled = false
}

// ---------------- 背景与描边 ----------------

/** 统一背景：4 层表面 + 极柔阴影（极简版）/ 1px 细描边（星球版） */
fun View.surface(color: Int, radius: Float, elevationDp: Float = 0f, stroke: Boolean = false) {
    val g = GradientDrawable()
    g.shape = GradientDrawable.RECTANGLE
    g.cornerRadius = context.dp(radius).toFloat()
    g.setColor(color)
    if (Tokens.planet && stroke) {
        g.setStroke(context.dp(1f), Tokens.hairc)
    }
    background = g
    if (!Tokens.planet && elevationDp > 0f) {
        elevation = context.dp(elevationDp).toFloat()
    }
}

fun View.flat(color: Int, radius: Float) {
    val g = GradientDrawable()
    g.shape = GradientDrawable.RECTANGLE
    g.cornerRadius = context.dp(radius).toFloat()
    g.setColor(color)
    background = g
}

fun View.circle(color: Int) {
    val g = GradientDrawable()
    g.shape = GradientDrawable.OVAL
    g.setColor(color)
    background = g
}

// ---------------- 文本 ----------------

fun label(ctx: Context, s: CharSequence, sizeSp: Float, color: Int, weight: Int = 400,
          maxLines: Int = 0, align: Int = Gravity.NO_GRAVITY, spacingExtra: Float = 0f): TextView =
    TextView(ctx).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        typeface = typefaceFor(weight)
        if (maxLines > 0) {
            this.maxLines = maxLines
            ellipsize = TextUtils.TruncateAt.END
        }
        gravity = align
        if (spacingExtra > 0f) letterSpacing = spacingExtra
        includeFontPadding = true
        // 默认给 LinearLayout 参数，便于调用方直接设置 topMargin
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

fun typefaceFor(weight: Int): Typeface {
    val bold = weight >= 600
    return if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
}

fun TextView.size(sizeSp: Float): TextView { setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp); return this }
fun TextView.weight(w: Int): TextView { typeface = typefaceFor(w); return this }

// ---------------- 按钮 / pill / chip ----------------

/** 星球版按钮：主色弱底 + 主色字；.pri 为实色主色底；ghost 为透明底次级字 */
fun btn(ctx: Context, text: String, style: String = "n", small: Boolean = false, filled: Boolean = false): TextView {
    val h = ctx.dp(if (small) 34f else 44f)
    return TextView(ctx).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (small) 13f else 14.5f)
        typeface = typefaceFor(Tokens.weightM())
        val pad = ctx.dp(if (small) 12f else 18f)
        setPadding(pad, 0, pad, 0)
        minWidth = ctx.dp(if (small) 62f else 76f)
        when {
            filled || style == "pri" -> {
                flat(Tokens.pri, Tokens.rBtn()); setTextColor(Tokens.onPri)
            }
            style == "ghost" -> {
                flat(0, Tokens.rBtn()); setTextColor(Tokens.sub)
            }
            style == "danger" -> {
                flat(Tokens.withAlpha(Tokens.bad, 0.14f), Tokens.rBtn()); setTextColor(Tokens.bad)
            }
            Tokens.planet -> {
                flat(Tokens.priSoft, Tokens.rBtn()); setTextColor(Tokens.pri)
            }
            else -> {
                surface(Tokens.l2, Tokens.rBtn(), 1f, true); setTextColor(Tokens.ink)
            }
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, h)
        isClickable = true
        val d = if (style == "pri" || filled) POP else SNAP
        setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(SNAP_D).setInterpolator(d).start()
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(SNAP_D).setInterpolator(d).start()
            }
            false
        }
    }
}

/** 按钮行：可换行的横向按钮组 */
fun btnRow(ctx: Context): LinearLayout = hrow(ctx).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}

fun pill(ctx: Context, s: String, strong: Boolean = false): TextView = TextView(ctx).apply {
    text = s
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
    typeface = typefaceFor(Tokens.weightM())
    setPadding(ctx.dp(9f), ctx.dp(3f), ctx.dp(9f), ctx.dp(3f))
    if (strong) {
        flat(Tokens.priSoft, Tokens.rPill()); setTextColor(Tokens.pri)
    } else {
        flat(if (Tokens.planet) Tokens.l3 else Tokens.withAlpha(Tokens.ink, 0.06f), Tokens.rPill())
        setTextColor(if (Tokens.planet) Tokens.pri else Tokens.sub)
    }
}

fun chip(ctx: Context, s: String, on: Boolean, icoName: String? = null, badge: Int = 0, onClick: () -> Unit): LinearLayout =
    hrow(ctx).apply {
        setPadding(ctx.dp(13f), ctx.dp(8f), ctx.dp(13f), ctx.dp(8f))
        if (on) flat(if (Tokens.planet) Tokens.priSoft else Tokens.pri, ctx.dp(if (Tokens.planet) 12f else 12f).toFloat())
        else surface(if (Tokens.planet) Tokens.l3 else Tokens.l2, 12f, 1f, true)
        val tc = when {
            on && Tokens.planet -> Tokens.pri
            on -> Tokens.onPri
            else -> Tokens.ink
        }
        if (icoName != null) {
            add(ImageView(context).apply {
                setImageDrawable(Icons.get(context, icoName, tc))
                layoutParams = LinearLayout.LayoutParams(context.dp(16f), context.dp(16f))
            })
        }
        add(label(context, s, 13f, tc, Tokens.weightM()), l = if (icoName != null) context.dp(6f) else 0)
        if (badge > 0) add(badgeDot(context, badge), l = context.dp(6f))
        isClickable = true
        setOnClickListener { animatePop(); onClick() }
    }

fun badgeDot(ctx: Context, n: Int): TextView = TextView(ctx).apply {
    text = if (n > 99) "99+" else n.toString()
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
    typeface = typefaceFor(700)
    setTextColor(0xFFFFFFFF.toInt())
    gravity = Gravity.CENTER
    minWidth = ctx.dp(17f)
    val p = ctx.dp(4f)
    setPadding(p, 0, p, 0)
    flat(if (Tokens.planet) Tokens.pri else Tokens.bad, 9f)
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ctx.dp(16f))
}

/** 图标按钮：星球版主色圆形，极简版 11dp 圆角墨色 */
fun iconBtn(ctx: Context, name: String, desc: String, sizeDp: Float = 36f, tint: Int? = null, onClick: () -> Unit): ImageView =
    ImageView(ctx).apply {
        setImageDrawable(Icons.get(ctx, name, tint ?: (if (Tokens.planet) Tokens.pri else Tokens.ink)))
        scaleType = ImageView.ScaleType.FIT_CENTER
        val s = ctx.dp(sizeDp)
        layoutParams = LinearLayout.LayoutParams(s, s)
        val pd = ctx.dp(sizeDp / 4f)
        setPadding(pd, pd, pd, pd)
        contentDescription = desc
        if (Tokens.planet) circle(Tokens.withAlpha(Tokens.pri, if (Tokens.dark) 0.10f else 0.07f)) else flat(Tokens.withAlpha(Tokens.ink, 0f), 11f)
        isClickable = true
        setOnClickListener { animatePop(); onClick() }
    }

fun iconView(ctx: Context, name: String?, tint: Int, sizeDp: Float = 20f): ImageView = ImageView(ctx).apply {
    setImageDrawable(Icons.get(ctx, name, tint))
    scaleType = ImageView.ScaleType.FIT_CENTER
    layoutParams = LinearLayout.LayoutParams(ctx.dp(sizeDp), ctx.dp(sizeDp))
}

/** 分区标题：星球版主色渐变大字（Android 降级为主色 + 尾部淡色双色渐变） */
fun secTitle(ctx: Context, s: String, count: String = ""): TextView = TextView(ctx).apply {
    text = if (count.isEmpty()) s else "$s  $count"
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
    typeface = typefaceFor(Tokens.weightH())
    setTextColor(Tokens.pri)
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        setMargins(0, ctx.dp(22f), 0, ctx.dp(6f))
    }
}

/** 分组容器：小标题 + 组内容（对应 Web .my-group / .my-group-title / .my-list） */
fun groupTitle(ctx: Context, s: String): LinearLayout = LinearLayout(ctx).apply {
    orientation = LinearLayout.VERTICAL
    addView(label(ctx, s, 13f, Tokens.sub, Tokens.weightM()), LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
        it.topMargin = ctx.dp(20f); it.bottomMargin = ctx.dp(6f)
        it.marginStart = ctx.dp(2f)
    })
}

fun newGroup(host: LinearLayout, ctx: Context, s: String): LinearLayout = groupTitle(ctx, s).also {
    host.addView(it)
}


fun muted(ctx: Context, text: CharSequence, sizeSp: Float = 12.5f, maxLines: Int = 0): TextView =
    label(ctx, text, sizeSp, Tokens.sub, Tokens.weightB(), maxLines).apply { setLineSpacing(0f, 1.18f) }

fun mono(ctx: Context, text: String, output: Boolean = false): TextView = TextView(ctx).apply {
    this.text = text
    setTextColor(if (output) Tokens.sub else if (Tokens.dark) 0xFFCFE8D8.toInt() else 0xFF24533B.toInt())
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
    typeface = Typeface.MONOSPACE
    setPadding(ctx.dp(12f), ctx.dp(10f), ctx.dp(12f), ctx.dp(10f))
    setLineSpacing(0f, 1.2f)
    flat(if (output) Tokens.l3 else Tokens.withAlpha(Tokens.pri, 0.08f), ctx.dp(10f).toFloat())
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}

fun noteBox(ctx: Context, title: String, body: String): LinearLayout = vcol(ctx).apply {
    setPadding(ctx.dp(12f), ctx.dp(10f), ctx.dp(12f), ctx.dp(10f))
    flat(Tokens.priSoft, ctx.dp(10f).toFloat())
    if (title.isNotBlank()) addView(label(ctx, title, 13f, Tokens.pri, Tokens.weightM()))
    addView(muted(ctx, body, 12.5f))
}

fun emptyState(ctx: Context, icon: String, title: String, subtitle: String): LinearLayout = vcol(ctx).apply {
    gravity = Gravity.CENTER
    setPadding(ctx.dp(24f), ctx.dp(34f), ctx.dp(24f), ctx.dp(34f))
    addView(iconView(ctx, icon, Tokens.sub, 30f))
    addView(label(ctx, title, 15f, Tokens.ink, Tokens.weightM()),
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(12f) })
    addView(muted(ctx, subtitle, 12.5f),
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(5f) })
}

fun View.flat(color: Int, radiusPx: Int) = flat(color, radiusPx.toFloat())
fun View.surface(color: Int, radiusPx: Int, elevationDp: Float = 0f, stroke: Boolean = false) =
    surface(color, radiusPx.toFloat(), elevationDp, stroke)
