package com.qwen.opsmentor.ui.pages

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.MarkEngine
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.add
import com.qwen.opsmentor.core.btn
import com.qwen.opsmentor.core.chip
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.hscroll
import com.qwen.opsmentor.core.iconBtn
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.pill
import com.qwen.opsmentor.core.secTitle
import com.qwen.opsmentor.core.surface
import com.qwen.opsmentor.core.typeset
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.data.Chapter
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.Shortcut
import com.qwen.opsmentor.ui.MainActivity
import com.qwen.opsmentor.ui.Page
import com.qwen.opsmentor.ui.Rows
import com.qwen.opsmentor.ui.ShortcutsEdit

/**
 * 课程页（底部导航第一项，对应 Web renderMine）：
 * 大标题 + 搜索 + 快捷入口行 +（星球版）为你推荐 / 我的课程，（极简版）课程卡列表。
 */
class PageMine(act: Activity, app: App) : Page(act, app) {

    override val key = "mine"
    override val isTab = true
    override val learning = false

    private var kw = ""
    private var recOff = 0

    override fun body(host: LinearLayout) {
        val ctx = act
        val orbit = iconBtn(ctx, "orbit", "课程管理", 40f) { nav("course-manage") }
        orbit.visibility = if (Tokens.planet) View.VISIBLE else View.GONE
        pageHead(host, "课程", orbit)
        host.addView(searchBox())
        val short = shortcutsRow()
        if (short != null) host.addView(short)
        if (Tokens.planet) planetBody(host) else minimalBody(host)
    }

    private fun nav(v: String) = (act as? MainActivity)?.nav(v)

    private fun searchBox(): View {
        val ctx = act
        val box = hrow(ctx)
        box.surface(Tokens.l2, 14f, if (Tokens.planet) 0f else 1f, true)
        box.setPadding(ctx.dp(13f), ctx.dp(11f), ctx.dp(9f), ctx.dp(11f))
        (box.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = ctx.dp(10f)
        })
        box.addView(Icons.iconView(ctx, "search", Tokens.sub, 19f))
        val inp = EditText(ctx).apply {
            hint = "搜索课程、章节、关键词"
            setHintTextColor(Tokens.withAlpha(Tokens.sub, 0.8f))
            setTextColor(Tokens.ink)
            setText(kw)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            background = null
            isSingleLine = true
            setPadding(ctx.dp(9f), 0, 0, 0)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    kw = s?.toString() ?: ""
                    rerender(false)
                }
            })
        }
        box.addView(inp, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val clr = iconBtn(ctx, "close", "清除", 28f) {
            kw = ""; rerender(false)
        }
        clr.visibility = if (kw.isEmpty()) View.GONE else View.VISIBLE
        box.addView(clr)
        return box
    }

    private fun shortcutsRow(): View? {
        val ctx = act
        val list = if (app.store.shortcuts.isNotEmpty()) app.store.shortcuts else Shortcut.defaults()
        if (kw.isNotBlank()) return null
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        list.forEach { sc ->
            row.addView(ShortcutsEdit.tile(ctx, app, sc), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                if (row.childCount > 0) it.marginStart = ctx.dp(8f)
            })
        }
        val add = btn(ctx, "＋添加入口", "n", small = true)
        add.setOnClickListener { ShortcutsEdit.open(act, app, -1) }
        row.addView(add, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        return hscroll(ctx, row).also {
            it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { lp ->
                lp.topMargin = ctx.dp(12f)
            }
        }
    }

    /** 星球版：为你推荐（未过关池 + 换一换）→ 我的课程 */
    private fun planetBody(host: LinearLayout) {
        val ctx = act
        if (kw.isBlank()) {
            val pool = routePool()
            host.addView(secTitle(ctx, "为你推荐"))
            val rec = vcol(ctx)
            if (pool.isEmpty()) {
                rec.addView(muted(ctx, "全部章节都已过关，去「复习队列」巩固一下吧。"))
            } else {
                val n = minOf(3, pool.size)
                for (i in 0 until n) {
                    val c = pool[(recOff + i) % pool.size]
                    rec.addView(Rows.chapter(act, app, pack, c, currentId(), { openChapter(c.id) }, { chapterMenu(c) }))
                }
                if (pool.size > 3) {
                    val more = hrow(ctx)
                    more.gravity = Gravity.CENTER_HORIZONTAL
                    val b = btn(ctx, "换一换", "n", small = true)
                    b.setOnClickListener {
                        recOff = (recOff + 3) % pool.size
                        rerender(false)
                    }
                    more.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                        it.topMargin = ctx.dp(10f); it.bottomMargin = ctx.dp(4f)
                    })
                    host.addView(more)
                }
            }
            host.addView(rec)
            host.addView(secTitle(ctx, "我的课程"))
            val list = vcol(ctx)
            Courses.sorted(app.store).forEach { co ->
                val p = Courses.prog(app.store, app.packs.of(app.store, co.id), co.id)
                list.addView(Rows.course(act, app, app.packs.of(app.store, co.id), co, p.started, p) { openCourse(co) })
            }
            host.addView(list)
            return
        }
        // 搜索态：课程 / 章节 / 知识点三段
        val r = searchAll(kw.trim().lowercase())
        val n = r.first.size + r.second.size + r.third.size
        if (n == 0) {
            host.addView(muted(ctx, "没有找到「${kw.trim()}」，试试 DNS、GPO、打印机、4625").also {
                (it.layoutParams as LinearLayout.LayoutParams).topMargin = ctx.dp(18f)
            })
            return
        }
        if (r.first.isNotEmpty()) {
            host.addView(secTitle(ctx, "课程", r.first.size.toString()))
            r.first.forEach { co -> host.addView(Rows.course(act, app, app.packs.of(app.store, co.id), co, Courses.prog(app.store, app.packs.of(app.store, co.id), co.id).started, Courses.prog(app.store, app.packs.of(app.store, co.id), co.id)) { openCourse(co) }) }
        }
        if (r.second.isNotEmpty()) {
            host.addView(secTitle(ctx, "章节", r.second.size.toString()))
            r.second.take(30).forEach { c -> host.addView(Rows.chapter(act, app, pack, c, currentId(), { openChapter(c.id) })) }
        }
        if (r.third.isNotEmpty()) {
            host.addView(secTitle(ctx, "知识点", (if (r.third.size >= 30) "30+" else r.third.size.toString())))
            r.third.forEach { (c, row) ->
                val el = hrow(ctx).apply {
                    setPadding(0, ctx.dp(12f), 0, ctx.dp(12f))
                    isClickable = true
                    setOnClickListener { openChapter(c.id) }
                }
                val th = FrameLayout(ctx).apply {
                    flat(Tokens.priSoft, ctx.dp(6f).toFloat())
                    layoutParams = LinearLayout.LayoutParams(ctx.dp(52f), ctx.dp(52f))
                }
                th.addView(Icons.iconView(ctx, "book", Tokens.pri, 22f), FrameLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f)).also { g -> g.gravity = Gravity.CENTER })
                el.addView(th)
                val mn = vcol(ctx)
                mn.addView(label(ctx, c.id + " " + c.t, 12f, Tokens.sub, 400))
                mn.addView(label(ctx, row.first, 15.5f, Tokens.ink, Tokens.weightM()), t = 2)
                mn.addView(muted(ctx, row.second, 12.5f, 2), t = 2)
                el.addView(mn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(13f) })
                host.addView(el)
            }
        }
    }

    private fun minimalBody(host: LinearLayout) {
        val ctx = act
        if (kw.isBlank()) {
            Courses.sorted(app.store).forEach { co ->
                val p = Courses.prog(app.store, app.packs.of(app.store, co.id), co.id)
                host.addView(Rows.courseCard(act, app, co, p) { openCourse(co) })
            }
            val list = vcol(ctx)
            host.addView(list)
            return
        }
        val r = searchAll(kw.trim().lowercase())
        val n = r.first.size + r.second.size + r.third.size
        if (n == 0) {
            host.addView(muted(ctx, "没有找到「${kw.trim()}」，试试 DNS、GPO、打印机、4625"))
            return
        }
        if (r.first.isNotEmpty()) {
            host.addView(label(ctx, "课程 · ${r.first.size}", 13f, Tokens.sub, Tokens.weightM()), t = 14)
            r.first.forEach { co -> host.addView(Rows.courseCard(act, app, co, Courses.prog(app.store, app.packs.of(app.store, co.id), co.id)) { openCourse(co) }) }
        }
        if (r.second.isNotEmpty()) {
            host.addView(label(ctx, "章节 · ${r.second.size}", 13f, Tokens.sub, Tokens.weightM()), t = 14)
            r.second.take(30).forEach { c -> host.addView(Rows.chapter(act, app, pack, c, currentId(), { openChapter(c.id) })) }
        }
        if (r.third.isNotEmpty()) {
            host.addView(label(ctx, "知识点 · ${minOf(r.third.size, 30)}${if (r.third.size >= 30) "+" else ""}", 13f, Tokens.sub, Tokens.weightM()), t = 14)
            r.third.forEach { (c, row) ->
                val el = vcol(ctx)
                el.surface(Tokens.l2, Tokens.rInner(), 1f, true)
                el.setPadding(ctx.dp(12f), ctx.dp(12f), ctx.dp(12f), ctx.dp(12f))
                el.addView(label(ctx, c.id + " " + c.t, 12f, Tokens.pri, Tokens.weightM()))
                el.addView(label(ctx, row.first, 15f, Tokens.ink, Tokens.weightM()), t = 4)
                el.addView(muted(ctx, row.second, 12.5f, 3), t = 3)
                el.isClickable = true
                el.setOnClickListener { openChapter(c.id) }
                host.addView(el, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
            }
        }
    }

    // ---------------- 数据辅助 ----------------

    private fun routePool(): List<Chapter> {
        val q = pack.route(bag().route)
        return q.mapNotNull { pack.chById(it) }.filter { bag().status[it.id] != "green" }
    }

    private fun currentId(): String {
        val p = bag().pin
        if (p.isNotEmpty() && pack.chById(p) != null) return p
        val q = pack.route(bag().route)
        if (q.isEmpty()) return ""
        return q[bag().idx.coerceIn(0, q.size - 1)]
    }

    private fun bag() = app.store.bag()

    private fun searchAll(k: String): Triple<List<Courses.Entry>, List<Chapter>, List<Pair<Chapter, Pair<String, String>>>> {
        val cs = Courses.all(app.store).filter { co ->
            val m = Courses.meta(app.store, co)
            (m.first + " " + m.second + " " + co.title + " " + co.desc).lowercase().contains(k)
        }
        val chs = pack.ch.filter { c ->
            (c.id + " " + c.t + " " + c.s + " " + c.terms).lowercase().contains(k)
        }
        val know = ArrayList<Pair<Chapter, Pair<String, String>>>()
        pack.ch.forEach { c ->
            val d = pack.deep[c.id] ?: return@forEach
            d.deep.forEach { row ->
                if (know.size >= 30) return@forEach
                if ((row.first + " " + row.second).lowercase().contains(k)) know.add(c to row)
            }
        }
        return Triple(cs, chs, know)
    }

    private fun openCourse(co: Courses.Entry) {
        (act as? MainActivity)?.onCourseOpened(co.id)
    }

    private fun openChapter(id: String) {
        (act as? MainActivity)?.openChapter(id)
    }

    /** 章节三点菜单：开始学习 / 标记 🟢🟡🔴 / 清除状态 */
    private fun chapterMenu(c: Chapter) {
        Sheets.picker(act, c.id + " " + c.t, listOf(
            com.qwen.opsmentor.core.Opt("play", "开始学习本章", icon = "play"),
            com.qwen.opsmentor.core.Opt("green", "标记为已过关", icon = "check"),
            com.qwen.opsmentor.core.Opt("yellow", "标记为半过关", icon = "refresh"),
            com.qwen.opsmentor.core.Opt("red", "标记为未过关", icon = "close"),
            com.qwen.opsmentor.core.Opt("clear", "清除状态", icon = "trash", danger = true),
        ), null) { v, label2 ->
            val b = bag()
            when (v) {
                "play" -> openChapter(c.id)
                "clear" -> { b.status.remove(c.id); Sheets.toast(act, "已清除状态") }
                else -> {
                    b.status[c.id] = v
                    if (!b.learn.containsKey(c.id)) b.learn[c.id] = com.qwen.opsmentor.state.LearnRec(com.qwen.opsmentor.state.Store.today(), ArrayList())
                    Sheets.toast(act, "${c.id} $label2")
                }
            }
            app.store.save()
            rerender(false)
        }
    }
}
