package com.qwen.opsmentor.ui.pages

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.core.Covers
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.MarkEditor
import com.qwen.opsmentor.core.Opt
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.btn
import com.qwen.opsmentor.core.btnRow
import com.qwen.opsmentor.core.chip
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.iconView
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.pill
import com.qwen.opsmentor.core.surface
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.io.Exporter
import com.qwen.opsmentor.io.Saver
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.LineType
import com.qwen.opsmentor.state.Mark
import com.qwen.opsmentor.state.MarkColor
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.ui.MainActivity
import com.qwen.opsmentor.ui.Page
import com.qwen.opsmentor.ui.Rows

/**
 * 我的划线（两层）：外层按课程分组，内层该课程的划线条目列表。
 * 对应 Web renderMyMarks + v12「划线两层、返回」补丁。
 */
class PageMarks(act: Activity, app: App) : Page(act, app) {

    override val key = "my-marks"
    override fun title() = if (selected.isEmpty()) "我的划线" else "课程划线"

    private var selected = ""
    private val checked = HashSet<String>()

    override val learning: Boolean
        get() = (act as? MainActivity)?.isDirectMarksPage() == true

    fun inCourse() = selected.isNotEmpty()
    fun openCourse(courseId: String) { selected = courseId; checked.clear() }
    fun popCourse() { selected = ""; checked.clear() }

    override fun body(host: LinearLayout) {
        val ctx = act
        val marks = store.allMarks()
        val groups = LinkedHashMap<String, MutableList<Mark>>()
        marks.forEach { m -> groups.getOrPut(m.courseId.ifBlank { store.cur }) { ArrayList() }.add(m) }
        if (marks.isEmpty()) {
            host.addView(com.qwen.opsmentor.core.emptyState(ctx, "highlighter", "还没有划线", "阅读时选中文字即可"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return
        }
        if (selected.isEmpty()) {
            host.addView(muted(ctx, "按课程整理你保存的重点。选择课程进入独立的划线详情。"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
            groups.forEach { (cid, list) ->
                val co = Courses.find(app.store, cid)
                val meta = Courses.meta(app.store, co)
                val panel = card(host, 14f, 10)
                val r = hrow(ctx)
                val (_, _, cover) = meta
                r.addView(Covers.view(ctx, ctx.dp(52f), cover.ifBlank { null }, co.coverClass, Courses.glyph(app.store, co)),
                    LinearLayout.LayoutParams(ctx.dp(52f), ctx.dp(52f)))
                val ci = vcol(ctx)
                ci.addView(label(ctx, meta.first, 15f, Tokens.ink, Tokens.weightM()))
                val last = list.maxOfOrNull { it.at } ?: 0L
                ci.addView(muted(ctx, "${list.size} 处划线 · 最近 ${Sdf.d(last)}"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(3f) })
                r.addView(ci, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(12f) })
                r.addView(iconView(ctx, "chev", Tokens.sub, 18f))
                panel.addView(r)
                panel.isClickable = true
                panel.setOnClickListener { selected = cid; checked.clear(); rerender(false) }
            }
            return
        }
        // 内层：条目列表 + 批量
        val list = (groups[selected] ?: emptyList()).sortedByDescending { it.at }
        val co = Courses.find(app.store, selected)
        val meta = Courses.meta(app.store, co)
        val back = btn(ctx, "‹ 全部课程", "ghost", small = true)
        back.setOnClickListener { popCourse(); rerender(false) }
        pageHead(host, title(), back, big = false)
        val intro = card(host, 14f, 8)
        intro.addView(label(ctx, meta.first, 16.5f, Tokens.ink, Tokens.weightH()))
        intro.addView(muted(ctx, meta.second.ifBlank { "课程划线与阅读备注" }), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
        intro.addView(pill(ctx, "${list.size} 处重点", true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        val bar = btnRow(ctx)
        bar.setPadding(0, ctx.dp(10f), 0, 0)
        bar.addView(btn(ctx, if (checked.isEmpty()) "全选" else "取消全选", "n", small = true).also {
            it.setOnClickListener {
                if (checked.isEmpty()) list.forEach { m -> checked.add(m.id) } else checked.clear()
                rerender(false)
            }
        })
        bar.addView(btn(ctx, "导出勾选 ${checked.size}", "n", small = true).also {
            it.setOnClickListener {
                if (checked.isEmpty()) { Sheets.toast(ctx, "先勾选要导出的划线"); return@setOnClickListener }
                val pack = app.packs.of(app.store, selected)
                Saver.export(ctx, "划线与备注-${Store.today()}.txt", "txt", Exporter.marksText(store, pack, list.filter { checked.contains(it.id) }))
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        bar.addView(btn(ctx, "删除勾选", "danger", small = true).also {
            it.setOnClickListener {
                if (checked.isEmpty()) { Sheets.toast(ctx, "先勾选要删除的划线"); return@setOnClickListener }
                Sheets.confirm(ctx, "删除勾选的 ${checked.size} 条划线？", "删除划线", "删除", true) {
                    store.bags[selected]?.marks?.removeAll { m -> checked.contains(m.id) }
                    checked.clear(); store.save(); app.invalidateMarks(); rerender(false)
                }
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        host.addView(bar)
        list.forEach { m -> item(m) }
    }

    private fun item(m: Mark): View {
        val ctx = act
        val card = card(content ?: LinearLayout(ctx), 14f, 8)
        val hd = hrow(ctx)
        val chk = check(ctx, checked.contains(m.id)) { on ->
            if (on) checked.add(m.id) else checked.remove(m.id)
            rerender(false)
        }
        hd.addView(chk)
        val chName = m.ch
        hd.addView(pill(ctx, chName.ifBlank { "课程内容" }, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        hd.addView(label(ctx, Sdf.d(m.at), 12f, Tokens.sub).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
        card.addView(hd)
        // 原文（带线型渲染）
        val q = label(ctx, "“" + m.joinedText() + "”", 14.5f, Tokens.ink, Tokens.weightB())
        q.flat(Tokens.l3, ctx.dp(Tokens.rInner()))
        val p = ctx.dp(11f)
        q.setPadding(p, p, p, p)
        val wrap = hrow(ctx)
        wrap.gravity = Gravity.TOP
        wrap.addView(View(ctx).apply { flat(Color.parseColor(MarkColor.of(m.co).hex), 2f) }, LinearLayout.LayoutParams(ctx.dp(3f), 0))
        wrap.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) }
        wrap.addView(q, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
        card.addView(wrap)
        if (m.tags.isNotEmpty()) {
            val tags = hrow(ctx)
            m.tags.forEach { t ->
                tags.addView(pill(ctx, t, false), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    if (tags.childCount > 0) it.marginEnd = ctx.dp(6f)
                })
            }
            card.addView(tags, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        }
        if (m.n.isNotEmpty()) card.addView(muted(ctx, "备注：" + m.n), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        val row = btnRow(ctx)
        row.setPadding(0, ctx.dp(10f), 0, 0)
        row.addView(btn(ctx, "编辑", "n", small = true).also {
            it.setOnClickListener { MarkEditor.open(act, app, m, { v -> v }, { mm -> (act as? MainActivity)?.locateCurrent(mm) ?: false }) }
        })
        row.addView(btn(ctx, "回到原文", "ghost", small = true).also {
            it.setOnClickListener {
                val ch = pack.chById(m.ch)
                if (ch != null) (act as? MainActivity)?.openChapter(m.ch) else Sheets.toast(ctx, "该课程内容尚未关联可跳转章节")
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
        row.addView(btn(ctx, "删除", "danger", small = true).also {
            it.setOnClickListener {
                Sheets.confirm(ctx, "删除这条划线？", "删除划线", "删除", true) {
                    store.bags[selected]?.marks?.removeAll { x -> x.id == m.id }
                    store.save(); app.invalidateMarks(); rerender(false)
                }
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
        card.addView(row)
        return card
    }

    private object Sdf {
        private val f = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.CHINA)
        fun d(ms: Long): String = f.format(java.util.Date(ms))
    }
}

/** 勾选框（主题化，非系统 CheckBox） */
private fun check(ctx: Activity, on: Boolean, onChange: (Boolean) -> Unit): FrameLayout {
    val box = FrameLayout(ctx)
    box.flat(if (on) Tokens.pri else Tokens.withAlpha(Tokens.sub, 0.2f), ctx.dp(6f).toFloat())
    box.layoutParams = LinearLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f))
    if (on) box.addView(iconView(ctx, "checkTick", Tokens.onPri, 14f), FrameLayout.LayoutParams(ctx.dp(14f), ctx.dp(14f)).also { it.gravity = Gravity.CENTER })
    box.isClickable = true
    box.setOnClickListener { onChange(!on) }
    return box
}

/**
 * 本课程划线（工具箱入口，与「我的划线」内层同版式，附线型 / 颜色工具条与筛选）。
 */
class PageMarksCourse(act: Activity, app: App) : Page(act, app) {

    override val key = "anno"
    // HTML STUDY_VIEWS 包含 anno：在本课程划线页阅读和整理也计入学习时间。
    override val learning = true
    override fun title() = "我的划线"

    private var fTy = ""
    private var fCo = ""
    private var fTag = ""
    private val checked = HashSet<String>()

    override fun body(host: LinearLayout) {
        val ctx = act
        val c = card(host, 16f, 10)
        val hd = hrow(ctx)
        hd.addView(label(ctx, "我的划线", 16.5f, Tokens.ink, Tokens.weightH()).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        hd.addView(pill(ctx, "${store.marks.size} 处", true))
        c.addView(hd)
        c.addView(muted(ctx, "在讲义正文或知识详解里长按选中一段话，会浮出划线面板：横线 / 波浪线 / 虚线 × 四种颜色。同一处正文允许多条不同颜色划线并存；跨段落的选择会合并成一条记录。点开后用「起 −／起 +／终 −／终 +」按字符微调范围，或用「整段」一次划满。"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        // 线型 / 颜色工具条
        val tools = vcol(ctx)
        LineType.values().forEach { t ->
            val r = hrow(ctx)
            r.addView(pill(ctx, t.label, fTy == t.id), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            r.addView(muted(ctx, "${store.marks.count { it.ty == t.id }} 条", 12f), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
            r.isClickable = true
            r.setOnClickListener { fTy = if (fTy == t.id) "" else t.id; rerender(false) }
            tools.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        }
        c.addView(tools, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        val colors = hrow(ctx)
        MarkColor.values().forEach { mc ->
            val sw = btn(ctx, mc.label, "n", small = true)
            sw.setTextColor(Color.parseColor(mc.hex))
            sw.flat(if (fCo == mc.id) Tokens.withAlpha(Color.parseColor(mc.hex), 0.20f) else Tokens.withAlpha(Tokens.ink, 0.05f), ctx.dp(Tokens.rBtn()))
            sw.setOnClickListener { fCo = if (fCo == mc.id) "" else mc.id; rerender(false) }
            colors.addView(sw, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                if (colors.childCount > 0) it.marginStart = ctx.dp(6f)
            })
        }
        c.addView(colors, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        val tagRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        com.qwen.opsmentor.state.TAG_PRESET.forEach { t ->
            tagRow.addView(chip(ctx, t, fTag == t) { fTag = if (fTag == t) "" else t; rerender(false) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    if (tagRow.childCount > 0) it.marginEnd = ctx.dp(6f)
                })
        }
        c.addView(tagRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })

        val list = store.marks.filter { m ->
            (fTy.isEmpty() || m.ty == fTy) && (fCo.isEmpty() || m.co == fCo) && (fTag.isEmpty() || m.tags.contains(fTag))
        }.sortedByDescending { it.at }
        val bar = btnRow(ctx)
        bar.setPadding(0, ctx.dp(12f), 0, 0)
        bar.addView(btn(ctx, if (checked.isEmpty()) "全选" else "取消全选", "n", small = true).also {
            it.setOnClickListener {
                if (checked.isEmpty()) list.forEach { m -> checked.add(m.id) } else checked.clear()
                rerender(false)
            }
        })
        bar.addView(btn(ctx, "导出勾选", "n", small = true).also {
            it.setOnClickListener {
                if (checked.isEmpty()) { Sheets.toast(ctx, "先勾选要导出的划线"); return@setOnClickListener }
                Saver.export(ctx, "划线与备注-${Store.today()}.txt", "txt", Exporter.marksText(store, pack, list.filter { m -> checked.contains(m.id) }))
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        bar.addView(btn(ctx, "删除勾选", "danger", small = true).also {
            it.setOnClickListener {
                if (checked.isEmpty()) { Sheets.toast(ctx, "先勾选要删除的划线"); return@setOnClickListener }
                Sheets.confirm(ctx, "删除勾选的 ${checked.size} 条划线？", "删除划线", "删除", true) {
                    store.marks.removeAll { m -> checked.contains(m.id) }
                    checked.clear(); store.save(); app.invalidateMarks(); rerender(false)
                }
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        host.addView(bar)
        if (list.isEmpty()) {
            host.addView(com.qwen.opsmentor.core.emptyState(ctx, "highlighter", "没有符合条件的划线", "换个筛选，或去讲义里划一条"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return
        }
        list.forEach { m -> row(m) }
    }

    private fun row(m: Mark): View {
        val ctx = act
        val card = card(content ?: LinearLayout(ctx), 14f, 8)
        val hd = hrow(ctx)
        hd.addView(check(ctx, checked.contains(m.id)) { on ->
            if (on) checked.add(m.id) else checked.remove(m.id)
            rerender(false)
        })
        hd.addView(pill(ctx, m.ch + " · " + (pack.chById(m.ch)?.t ?: ""), true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        hd.addView(pill(ctx, LineType.of(m.ty).label + " · " + MarkColor.of(m.co).label, false), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
        hd.addView(label(ctx, Sdf.d(m.at), 11.5f, Tokens.sub).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(6f) })
        card.addView(hd)
        card.addView(label(ctx, "“" + m.joinedText() + "”", 14.5f, Tokens.ink, Tokens.weightB()),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        if (m.n.isNotEmpty()) card.addView(muted(ctx, "备注：" + m.n), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        val row = btnRow(ctx)
        row.setPadding(0, ctx.dp(8f), 0, 0)
        row.addView(btn(ctx, "编辑", "n", small = true).also {
            it.setOnClickListener { MarkEditor.open(act, app, m, { v -> v }, { mm -> (act as? MainActivity)?.locateCurrent(mm) ?: false }) }
        })
        row.addView(btn(ctx, "定位到原文", "ghost", small = true).also {
            it.setOnClickListener {
                if (pack.chById(m.ch) != null) (act as? MainActivity)?.openChapter(m.ch) else Sheets.toast(ctx, "原文在别的页面，去知识详解里找这一条")
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
        card.addView(row)
        return card
    }

    private object Sdf {
        private val f = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.CHINA)
        fun d(ms: Long): String = f.format(java.util.Date(ms))
    }
}
