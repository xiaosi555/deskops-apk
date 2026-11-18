package com.qwen.opsmentor.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.core.Covers
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.Opt
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.add
import com.qwen.opsmentor.core.btn
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.data.Pack
import com.qwen.opsmentor.state.CoMeta
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.state.UserCourse
import java.io.File

/**
 * 课程 / 个人信息编辑器 + 图片选择裁剪。
 * 对应 Web 的 openCourseEditor / openProfileEditor / pickImage / openCropModal。
 */
object CourseEditor {

    fun open(act: Activity, app: App, co: Courses.Entry) {
        val ctx = act
        val meta = Courses.meta(app.store, co)
        var cover = meta.third
        var glyph = Courses.glyph(app.store, co)
        val body = vcol(ctx, padH = 16)
        // 封面预览 + 上传
        val row = hrow(ctx)
        val prev = FrameLayout(ctx)
        row.addView(prev, LinearLayout.LayoutParams(ctx.dp(76f), ctx.dp(76f)))
        fun paint() {
            prev.removeAllViews()
            prev.addView(Covers.view(ctx, ctx.dp(76f), cover.ifBlank { null }, co.coverClass, glyph),
                FrameLayout.LayoutParams(ctx.dp(76f), ctx.dp(76f)))
        }
        paint()
        val cb = vcol(ctx)
        val up = btn(ctx, "上传封面", "n", small = true)
        up.setOnClickListener { ImagePick.pick(ctx, 480) { path -> cover = path; paint() } }
        cb.addView(up)
        val rm = btn(ctx, "恢复默认封面", "ghost", small = true)
        rm.setOnClickListener { cover = ""; paint() }
        cb.addView(rm, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        row.addView(cb, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(12f) })
        body.addView(row)
        body.addView(Sheets.field(ctx, "课程名称", Sheets.input(ctx, meta.first, "课程名称", 40)), t = 10)
        body.addView(Sheets.field(ctx, "课程简介", Sheets.input(ctx, meta.second, "一句话介绍这门课程", 200, true)))
        val glyphBox = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        Icons.COURSE_GLYPHS.forEach { g ->
            val b = FrameLayout(ctx).apply {
                flat(if (g == glyph) Tokens.priSoft else Tokens.l3, ctx.dp(8f).toFloat())
                layoutParams = LinearLayout.LayoutParams(ctx.dp(44f), ctx.dp(44f)).also { it.marginEnd = ctx.dp(6f) }
                isClickable = true
            }
            b.addView(ImageView(ctx).apply { setImageDrawable(Icons.get(ctx, g, if (g == glyph) Tokens.pri else Tokens.sub)) },
                FrameLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f)).also { it.gravity = Gravity.CENTER })
            b.setOnClickListener {
                glyph = g
                paint()
                val box = b.parent as LinearLayout
                for (i in 0 until box.childCount) {
                    val c = box.getChildAt(i) as FrameLayout
                    val gg = Icons.COURSE_GLYPHS[i]
                    c.flat(if (gg == glyph) Tokens.priSoft else Tokens.l3, ctx.dp(8f).toFloat())
                    (c.getChildAt(0) as ImageView).setImageDrawable(Icons.get(ctx, gg, if (gg == glyph) Tokens.pri else Tokens.sub))
                }
            }
            glyphBox.addView(b)
        }
        body.addView(Sheets.field(ctx, "课程图标", HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(glyphBox, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }))
        val ti = body.getChildAt(1).getChildAt(1) as EditText
        val su = body.getChildAt(2).getChildAt(1) as EditText
        val builtin = !co.custom
        Sheets.sheet(ctx, "编辑课程", body, "保存",
            cancelText = if (builtin) "删除" else "取消", danger = builtin,
            onCancel = if (builtin) { { confirmDelete(act, app, co) } } else null,
            onSave = {
                val t = ti.text.toString().trim()
                if (t.isEmpty()) { Sheets.toast(ctx, "课程名称不能为空"); return@sheet false }
                val m = app.store.co.getOrPut(co.id) { CoMeta() }
                m.title = if (t == co.title) "" else t
                val s = su.text.toString().trim()
                m.sub = if (s == co.sub) "" else s
                m.cover = cover
                m.glyph = glyph
                m.desc = if (s.isNotEmpty() && s != co.sub) s else ""
                app.store.save()
                app.rerender?.invoke()
                Sheets.toast(ctx, "课程信息已保存")
                true
            })
    }

    fun confirmDelete(act: Activity, app: App, co: Courses.Entry) {
        val list = Courses.all(app.store)
        if (list.size <= 1) { Sheets.toast(act, "至少需要保留一门课程"); return }
        Sheets.confirm(act, "删除「" + Courses.meta(app.store, co).first + "」及它的进度、划线与案例？无法恢复。", "删除课程", "删除", true) {
            Sheets.closeAll()
            if (Courses.delete(app.store, co.id, app.packs)) {
                Sheets.toast(act, "课程已删除")
                app.rerender?.invoke()
            }
        }
    }

    fun openNew(act: Activity, app: App) {
        val ctx = act
        var cover = ""
        var glyph = "book"
        val body = vcol(ctx, padH = 16)
        val up = btn(ctx, "上传封面", "n")
        up.setOnClickListener { ImagePick.pick(ctx, 480) { cover = it; up.text = "封面已选择 ✓" } }
        body.addView(up)
        body.addView(Sheets.field(ctx, "课程名称", Sheets.input(ctx, "", "课程名称", 40)), t = 8)
        body.addView(Sheets.field(ctx, "课程简介", Sheets.input(ctx, "", "课程简介", 200, true)))
        val ti = body.getChildAt(1).getChildAt(1) as EditText
        val su = body.getChildAt(2).getChildAt(1) as EditText
        Sheets.sheet(ctx, "新增课程", body, "保存", onSave = {
            val title = ti.text.toString().trim()
            if (title.isEmpty()) { Sheets.toast(ctx, "请填写课程名称"); return@sheet false }
            val id = "custom_" + System.currentTimeMillis()
            val uc = UserCourse(id, title, su.text.toString().trim(), su.text.toString().trim(), glyph)
            app.store.userCourses.add(uc)
            if (cover.isNotEmpty()) app.store.co[id] = CoMeta(cover = cover)
            app.store.bags[id] = com.qwen.opsmentor.state.Bag.newBag()
            app.store.save()
            app.rerender?.invoke()
            Sheets.toast(ctx, "课程已新增")
            true
        })
    }
}

/** 个人信息：头像 / 昵称 / 签名 */
object ProfileEditor {
    fun open(act: Activity, app: App) {
        val ctx = act
        val s = app.store
        var avatar = s.profileAvatar
        val body = vcol(ctx, padH = 16)
        val row = hrow(ctx)
        val prev = FrameLayout(ctx)
        row.addView(prev, LinearLayout.LayoutParams(ctx.dp(66f), ctx.dp(66f)))
        val nameInp = Sheets.input(ctx, s.profileName, "你的名字", 16)
        fun paint() {
            prev.removeAllViews()
            val v = View(ctx)
            val p = ctx.dp(66f)
            v.layoutParams = FrameLayout.LayoutParams(p, p)
            if (avatar.isNotEmpty() && File(avatar).exists()) {
                val bmp = BitmapFactory.decodeFile(avatar)
                val iv = ImageView(ctx).apply { setImageBitmap(bmp); scaleType = ImageView.ScaleType.CENTER_CROP }
                prev.addView(iv, FrameLayout.LayoutParams(p, p))
                iv.clipToOutline = true
                iv.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
                iv.clipToOutline = true
            } else {
                v.circle(Tokens.priSoft)
                v.addView(label(ctx, (nameInp.text.toString().trim().ifBlank { s.profileName.ifBlank { "桌面运维学习者" } }).take(1),
                    26f, Tokens.pri, Tokens.weightH()), FrameLayout.LayoutParams(p, p).also { it.gravity = Gravity.CENTER })
                prev.addView(v, FrameLayout.LayoutParams(p, p))
            }
        }
        paint()
        val cb = vcol(ctx)
        val up = btn(ctx, "更换头像", "n", small = true)
        up.setOnClickListener { ImagePick.pick(ctx, 256) { avatar = it; paint() } }
        cb.addView(up)
        val rm = btn(ctx, "移除头像", "ghost", small = true)
        rm.setOnClickListener { avatar = ""; paint() }
        cb.addView(rm, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        row.addView(cb, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(12f) })
        body.addView(row)
        nameInp.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { if (avatar.isEmpty()) paint() }
        })
        body.addView(Sheets.field(ctx, "昵称", nameInp), t = 8)
        val bio = Sheets.input(ctx, s.profileBio, "个性签名 / 学习目标（选填）", 40)
        body.addView(Sheets.field(ctx, "个性签名", bio))
        Sheets.sheet(ctx, "编辑个人信息", body, "保存", onSave = {
            val nm = nameInp.text.toString().trim()
            if (nm.isEmpty()) { Sheets.toast(ctx, "昵称不能为空"); return@sheet false }
            s.profileName = nm
            s.profileBio = bio.text.toString().trim()
            s.profileAvatar = avatar
            s.save()
            app.rerender?.invoke()
            Sheets.toast(ctx, "个人信息已保存")
            true
        })
    }

    private fun View.circle(c: Int) {
        val g = android.graphics.drawable.GradientDrawable()
        g.shape = android.graphics.drawable.GradientDrawable.OVAL
        g.setColor(c)
        background = g
    }
}

/** 图片选择入口：交给 MainActivity 的 ActivityResultLauncher，回调返回本机文件路径 */
object ImagePick {
    fun pick(act: Activity, size: Int, cb: (path: String) -> Unit) {
        val host = act as? MainActivity
        if (host == null) {
            Sheets.toast(act, "当前页面不支持选图")
            return
        }
        host.pickImage(size, cb)
    }
}
