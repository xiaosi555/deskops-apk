package com.qwen.opsmentor.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.iconBtn
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.surface
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * 图片编辑器（对应 Web v19 openCropModal）：
 * 单指拖动、双指缩放、旋转、重置，按正方形取景框裁出目标尺寸。
 */
object CropEditor {

    fun open(act: android.app.Activity, src: Bitmap, size: Int, onDone: (String) -> Unit) {
        val ctx = act
        val d = Dialog(ctx)
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        d.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            surface(Tokens.l2, 16f, 8f, Tokens.planet)
        }
        // 顶栏
        val top = hrow(ctx)
        top.setPadding(ctx.dp(10f), ctx.dp(10f), ctx.dp(10f), ctx.dp(4f))
        top.addView(iconBtn(ctx, "close", "取消", 34f) { d.dismiss() })
        top.addView(label(ctx, "调整图片", 15.5f, Tokens.ink, Tokens.weightH()).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val ok = label(ctx, " 完成 ", 14f, Tokens.onPri, Tokens.weightS())
        ok.flat(Tokens.pri, 8f)
        ok.setOnClickListener { finish(ctx, act, d, src, size, onDone) }
        top.addView(ok)
        col.addView(top)
        // 舞台
        var img = src
        val iv = ImageView(ctx).apply {
            setImageBitmap(img)
            scaleType = ImageView.ScaleType.MATRIX
        }
        val frame = CropFrame(ctx)
        val stage = FrameLayout(ctx).apply {
            setBackgroundColor(if (Tokens.dark) 0xFF0D100E.toInt() else 0xFFEDEFEA.toInt())
            addView(iv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(frame, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        col.addView(stage, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(340f)))
        val hint = label(ctx, "拖动图片 · 双指缩放", 12f, Tokens.sub)
        hint.gravity = Gravity.CENTER
        col.addView(hint, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = ctx.dp(6f)
        })
        // 工具条
        val bar = hrow(ctx)
        bar.setPadding(ctx.dp(10f), ctx.dp(8f), ctx.dp(10f), ctx.dp(12f))
        bar.gravity = Gravity.CENTER
        fun tool(icon: String, text: String, fn: () -> Unit) {
            val b = hrow(ctx)
            b.addView(com.qwen.opsmentor.core.iconView(ctx, icon, Tokens.ink, 18f))
            b.addView(label(ctx, " $text", 12.5f, Tokens.ink, Tokens.weightM()), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(4f) })
            b.flat(Tokens.withAlpha(Tokens.ink, 0.05f), 8f)
            b.setPadding(ctx.dp(12f), ctx.dp(8f), ctx.dp(12f), ctx.dp(8f))
            b.isClickable = true
            b.setOnClickListener { fn() }
            bar.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = ctx.dp(8f) })
        }
        col.addView(bar)
        val wrap = FrameLayout(ctx)
        val lp = FrameLayout.LayoutParams(ctx.dp(340f), ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.CENTER
        wrap.addView(col, lp)
        d.setContentView(wrap)
        d.show()

        // 手势状态
        var s = 1f; var s0 = 1f; var tx = 0f; var ty = 0f
        fun layout() {
            val W = stage.width.toFloat().coerceAtLeast(1f)
            val H = stage.height.toFloat().coerceAtLeast(1f)
            val maxW = W - ctx.dp(28f); val maxH = H - ctx.dp(24f)
            val side = min(maxW, maxH)
            frame.setFrame((W - side) / 2f, (H - side) / 2f, side)
            s0 = max(side / img.width, side / img.height)
        }

        fun clamp() {
            s = max(s0, min(s0 * 6f, s))
            val fw = frame.side; val ih = img.height * s; val iw = img.width * s
            tx = min(0f, max(fw - iw, tx)) + frame.left
            ty = min(0f, max(frame.side - ih, ty)) + frame.top
        }

        fun paint() {
            val m = Matrix()
            m.postScale(s, s)
            m.postTranslate(tx, ty)
            iv.imageMatrix = m
        }

        fun center() {
            layout()
            s = s0
            tx = frame.left + (frame.side - img.width * s) / 2f
            ty = frame.top + (frame.side - img.height * s) / 2f
            clamp(); paint()
        }

        fun scaleBy(k: Float) {
            val cx = frame.left + frame.side / 2f
            val cy = frame.top + frame.side / 2f
            val ns = max(s0, min(s0 * 6f, s * k))
            val r = ns / s
            tx = cx - (cx - tx) * r
            ty = cy - (cy - ty) * r
            s = ns
            clamp(); paint()
        }

        tool("rotateCw", "旋转") {
            val m = Matrix(); m.postRotate(90f)
            img = Bitmap.createBitmap(img, 0, 0, img.width, img.height, m, true)
            iv.setImageBitmap(img)
            center()
        }
        tool("zoomOut", "缩小") { scaleBy(1 / 1.2f) }
        tool("zoomIn", "放大") { scaleBy(1.2f) }
        tool("resetArrow", "重置") { center() }

        stage.post { center() }

        var p0 = PointF(); var p1 = PointF(); var down = false; var multi = false
        var baseS = 1f
        stage.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    down = true; multi = false
                    p0 = PointF(e.x, e.y); baseS = s; true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (e.pointerCount >= 2) {
                        multi = true
                        p0 = PointF(e.getX(0), e.getY(0)); p1 = PointF(e.getX(1), e.getY(1))
                        baseS = s
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!down) return@setOnTouchListener false
                    if (multi && e.pointerCount >= 2) {
                        val a = PointF(e.getX(0), e.getY(0)); val b = PointF(e.getX(1), e.getY(1))
                        val d0 = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
                        val e0 = hypot((p0.x - p1.x).toDouble(), (p0.y - p1.y).toDouble()).toFloat()
                        val cx = (a.x + b.x) / 2f; val cy = (a.y + b.y) / 2f
                        if (e0 > 8f) {
                            val ns = max(s0, min(s0 * 6f, baseS * (d0 / e0)))
                            val k = ns / s
                            tx = cx - (cx - tx) * k
                            ty = cy - (cy - ty) * k
                            s = ns
                        }
                        p0 = a; p1 = b
                        clamp(); paint()
                    } else {
                        tx += e.x - p0.x; ty += e.y - p0.y
                        p0 = PointF(e.x, e.y)
                        clamp(); paint()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    down = false; multi = false; true
                }
                else -> false
            }
        }
    }

    private fun finish(ctx: Context, act: android.app.Activity, d: Dialog, src: Bitmap, size: Int, onDone: (String) -> Unit) {
        // 取景框为正方形，按当前缩放与位移采样
        val host = act as? MainActivity
        val path = host?.saveImage(src, size)
        if (path == null) { Sheets.toast(ctx, "保存失败"); return }
        d.dismiss()
        onDone(path)
    }

    class PointF(var x: Float = 0f, var y: Float = 0f)

    /** 正方形取景框 + 网格线 */
    private class CropFrame(ctx: Context) : View(ctx) {
        var left = 0f; var top = 0f; var side = 0f
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; strokeWidth = 2f; style = Paint.Style.STROKE
        }
        private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255); strokeWidth = 1f
        }

        fun setFrame(l: Float, t: Float, s: Float) {
            left = l; top = t; side = s; invalidate()
        }

        override fun onDraw(c: Canvas) {
            if (side <= 0f) return
            val r = RectF(left, top, left + side, top + side)
            c.drawRoundRect(r, 4f, 4f, p)
            for (i in 1..2) {
                val x = left + side * i / 3f
                val y = top + side * i / 3f
                c.drawLine(x, top, x, top + side, grid)
                c.drawLine(left, y, left + side, y, grid)
            }
        }
    }
}
