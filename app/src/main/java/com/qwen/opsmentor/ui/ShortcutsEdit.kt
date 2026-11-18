package com.qwen.opsmentor.ui

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
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
import com.qwen.opsmentor.core.groupTitle
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.iconView
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.pill
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.state.Courses
import java.io.File

/**
 * 快捷入口（课程页搜索栏下方的功能块）：渲染、添加、编辑、预览。
 * 与 Web 的 getSC / scDefaults / scTile / openScEditor / renderShortcuts 对应。
 */
object ShortcutsEdit {

    val TARGETS = listOf(
        "course-manage" to "课程管理", "my-marks" to "我的划线", "calendar" to "学习日历",
        "today" to "今日学习", "review" to "复习队列", "know" to "知识详解", "gloss" to "术语手册",
        "case" to "案例库", "play" to "排障手册", "prog" to "学习进度", "io" to "数据", "settings" to "设置",
    )
    val ICONS = mapOf(
        "course-manage" to "book", "my-marks" to "highlighter", "calendar" to "calendar", "today" to "play",
        "review" to "refresh", "know" to "book", "gloss" to "aA", "case" to "folder", "play" to "wrench",
        "prog" to "chart", "io" to "share", "settings" to "settings",
    )
    val COLORS = listOf("#2f7d5f", "#3b6ea5", "#7a5ca8", "#c4673d", "#d9a521", "#e8e2d4", "#f3c6c0", "#bfe3d2", "#22302b", "#ffffff")

    fun go(act: Activity, app: App, target: String) {
        val ma = act as? MainActivity ?: return
        if (target.startsWith("course:")) {
            val id = target.removePrefix("course:")
            if (Courses.all(app.store).any { it.id == id }) {
                app.store.setCur(id); app.store.save()
                com.qwen.opsmentor.ui.pages.PageCourse.seg = "chs"
                ma.onCourseOpened(id)
            } else Sheets.toast(act, "这门课程已不存在")
            return
        }
        ma.nav(target)
    }

    /** 单个入口磁贴 */
    fun tile(ctx: Activity, app: App, sc: com.qwen.opsmentor.state.Shortcut, preview: Boolean = false): View {
        val b = vcol(ctx)
        b.gravity = Gravity.CENTER_VERTICAL
        val img = sc.img.isNotEmpty() && File(sc.img).exists()
        val bg = if (img) 0 else runCatching { Color.parseColor(sc.color) }.getOrElse { if (Tokens.planet) Tokens.l2 else Tokens.withAlpha(Tokens.ink, 0.05f) }
        val tc = when {
            img -> (sc.tc.ifBlank { "#ffffff" }).let { runCatching { Color.parseColor(it) }.getOrElse { 0xFFFFFFFF.toInt() } }
            sc.color.isNotEmpty() -> Tokens.autoTextColor(bg)
            else -> Tokens.ink
        }
        b.flat(if (img) Tokens.l2 else bg, ctx.dp(10f).toFloat())
        val w = if (Tokens.planet) ctx.dp(64f) else ctx.dp(58f)
        b.setPadding(0, ctx.dp(9f), 0, ctx.dp(9f))
        if (!img) {
            val ic = iconView(ctx, ICONS[sc.target] ?: "layers", if (sc.color.isEmpty() && !preview) Tokens.pri else tc, 18f)
            ic.gravity = Gravity.CENTER
            b.addView(ic, LinearLayout.LayoutParams(ctx.dp(18f), ctx.dp(18f)).also { it.gravity = Gravity.CENTER_HORIZONTAL })
        }
        val lab = label(ctx, sc.name.ifBlank { "未命名" }, if (Tokens.planet) 12f else 11.5f, tc, Tokens.weightM())
        lab.gravity = Gravity.CENTER
        lab.maxLines = 2
        lab.ellipsize = android.text.TextUtils.TruncateAt.END
        b.addView(lab, LinearLayout.LayoutParams(ctx.dp(60f), ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
        b.layoutParams = LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT)
        if (!preview) {
            b.isClickable = true
            b.setOnLongClickListener { open(ctx, app, app.store.shortcuts.indexOfFirst { it.id == sc.id }); true }
            b.setOnClickListener { animate(b); go(ctx, app, sc.target) }
        } else {
            b.isClickable = false
        }
        return b
    }

    private fun animate(v: View) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(com.qwen.opsmentor.core.POP).start()
        }.start()
    }

    /** 编辑 / 添加弹层 */
    fun open(act: Activity, app: App, idx: Int) {
        val ctx = act
        val isNew = idx < 0
        val list = app.store.shortcuts
        val it = if (isNew) com.qwen.opsmentor.state.Shortcut("sc_" + System.currentTimeMillis(), "", "course-manage")
        else list[idx]
        var name = it.name
        var target = it.target
        var img = it.img
        var color = it.color
        var tc = it.tc
        val body = vcol(ctx, padH = 16)
        val pv = hrow(ctx).apply { gravity = Gravity.CENTER }
        fun paintPv() {
            pv.removeAllViews()
            val tmp = com.qwen.opsmentor.state.Shortcut("p", name, target, img, color, tc)
            pv.addView(tile(ctx, app, tmp, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        paintPv()
        body.addView(pv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = ctx.dp(10f) })
        val nameInp = Sheets.input(ctx, name, "例如：复习", 8)
        nameInp.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { name = s?.toString()?.trim() ?: ""; paintPv() }
        })
        body.addView(Sheets.field(ctx, "名称", nameInp))
        var targetBtn = btn(ctx, TARGETS.firstOrNull { it.first == target }?.second ?: "选择目标", "n")
        targetBtn.setOnClickListener {
            val items = TARGETS.map { Opt(it.first, it.second, icon = ICONS[it.first] ?: "layers") } +
                    Courses.all(app.store).map { Opt("course:${it.id}", "打开课程：" + Courses.meta(app.store, it).first, icon = "book") }
            Sheets.picker(ctx, "点击后前往", items, target) { v, label2 -> target = v; targetBtn.text = label2; paintPv() }
        }
        body.addView(Sheets.field(ctx, "点击后前往", targetBtn))
        val imgRow = btnRow(ctx)
        val pick = btn(ctx, "选择图片", "n", small = true)
        pick.setOnClickListener { ImagePick.pick(ctx, 360) { path ->
            img = path
            tc = if (Tokens.lum(Colors_of(path)) > 0.62f) "#161a18" else "#ffffff"
            paintPv()
        } }
        imgRow.addView(pick)
        val clr = btn(ctx, "移除图片", "ghost", small = true)
        clr.visibility = if (img.isEmpty()) View.GONE else View.VISIBLE
        clr.setOnClickListener { img = ""; tc = ""; paintPv(); clr.visibility = View.GONE }
        imgRow.addView(clr, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        body.addView(Sheets.field(ctx, "背景图片", imgRow))
        body.addView(muted(ctx, "可选。设置图片后，图片会铺满整个功能块，文字颜色按图片底部明暗自动选择。"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val colRow = hrow(ctx)
        fun paintCols() {
            colRow.removeAllViews()
            val none = btn(ctx, "默认", if (color.isEmpty()) "pri" else "n", small = true)
            none.setOnClickListener { color = ""; paintPv(); paintCols() }
            colRow.addView(none)
            COLORS.forEach { c ->
                val b = View(ctx).apply {
                    flat(Color.parseColor(c), ctx.dp(7f).toFloat())
                    layoutParams = LinearLayout.LayoutParams(ctx.dp(30f), ctx.dp(30f)).also {
                        it.marginStart = ctx.dp(6f)
                    }
                    isClickable = true
                }
                b.setOnClickListener { color = if (color == c) "" else c; paintPv(); paintCols() }
                colRow.addView(b)
                if (colRow.childCount >= 6) return@forEach
            }
        }
        paintCols()
        body.addView(Sheets.field(ctx, "底纹颜色", colRow))
        if (!isNew) {
            val del = btn(ctx, "删除这个入口", "danger", small = true)
            del.setOnClickListener {
                Sheets.confirm(ctx, "删除「${it.name.ifBlank { "未命名" }}」？", "删除入口", "删除", true) {
                    list.removeAt(idx); app.store.save(); app.rerender?.invoke()
                    Sheets.closeAll()
                }
            }
            body.addView(del, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        }
        Sheets.sheet(ctx, if (isNew) "添加入口" else "编辑入口", body, "保存", onSave = {
            if (name.isEmpty()) { Sheets.toast(ctx, "请先填写名称"); return@sheet false }
            it.name = name; it.target = target; it.img = img; it.color = color; it.tc = tc
            if (isNew) list.add(it) else list[idx] = it
            app.store.save()
            app.rerender?.invoke()
            true
        })
    }

    private fun Colors_of(path: String): Int = runCatching {
        val bmp = android.graphics.BitmapFactory.decodeFile(path)
        val px = bmp.getPixel(bmp.width / 2, (bmp.height * 0.9f).toInt())
        bmp.recycle()
        px
    }.getOrElse { Tokens.l2 }

    /** 快捷入口编辑页 */
    fun page(act: Activity, app: App, host: LinearLayout, refresh: () -> Unit) {
        val ctx = act
        val list = app.store.shortcuts
        val pv = ctx.let { vcol(it, padH = 16) }
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            flat(Tokens.l2, ctx.dp(Tokens.rCard()))
            setPadding(ctx.dp(14f), ctx.dp(14f), ctx.dp(14f), ctx.dp(14f))
        }
        card.addView(label(ctx, "预览", 15.5f, Tokens.ink, Tokens.weightH()))
        val row = hrow(ctx)
        list.forEach { s ->
            row.addView(tile(ctx, app, s, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                if (row.childCount > 0) it.marginStart = ctx.dp(8f)
            })
        }
        card.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        card.addView(muted(ctx, if (list.size > 3) "入口超过 3 个时，在课程页可左右滑动。" else "显示在「课程」页搜索栏下方，超过 3 个时可左右滑动。"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        host.addView(card)
        val list2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            flat(Tokens.l2, ctx.dp(Tokens.rCard()))
            setPadding(ctx.dp(14f), ctx.dp(6f), ctx.dp(14f), ctx.dp(6f))
        }
        (list2.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)).topMargin = ctx.dp(10f)
        list.forEachIndexed { i, s ->
            val r = hrow(ctx)
            r.setPadding(0, ctx.dp(12f), 0, ctx.dp(12f))
            r.addView(iconView(ctx, ICONS[s.target] ?: "layers", Tokens.pri, 20f))
            val box = vcol(ctx)
            box.addView(label(ctx, s.name.ifBlank { "未命名" }, 15f, Tokens.ink, Tokens.weightM()))
            box.addView(muted(ctx, TARGETS.firstOrNull { it.first == s.target }?.second ?: ("打开课程：" + s.target.removePrefix("course:")), 12f),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(2f) })
            r.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(10f) })
            val up = btn(ctx, "↑", "n", small = true)
            up.isEnabled = i > 0
            up.setOnClickListener { if (i > 0) { val t = list[i - 1]; list[i - 1] = s; list[i] = t; app.store.save(); refresh() } }
            r.addView(up)
            val mv = btn(ctx, "编辑", "ghost", small = true)
            mv.setOnClickListener { open(act, app, i) }
            r.addView(mv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
            list2.addView(r)
            if (i < list.size - 1) list2.addView(View(ctx).apply { flat(Tokens.hairc, 1f) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(1f)))
        }
        host.addView(list2)
        val row2 = btnRow(ctx)
        row2.setPadding(0, ctx.dp(12f), 0, 0)
        row2.addView(btn(ctx, "＋ 添加入口", "pri").also { it.setOnClickListener { open(act, app, -1) } })
        row2.addView(btn(ctx, "恢复默认", "n").also {
            it.setOnClickListener {
                Sheets.confirm(ctx, "用默认的三个入口替换当前配置？", "恢复默认", "恢复") {
                    app.store.shortcuts = com.qwen.opsmentor.state.Shortcut.defaults()
                    app.store.save(); refresh()
                }
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        host.addView(row2)
    }
}
