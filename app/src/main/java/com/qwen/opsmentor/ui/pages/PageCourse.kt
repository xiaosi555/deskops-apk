package com.qwen.opsmentor.ui.pages
import com.qwen.opsmentor.core.animatePop

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.ui.Bars
import com.qwen.opsmentor.core.Covers
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.Opt
import com.qwen.opsmentor.core.RingButton
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.add
import com.qwen.opsmentor.core.chip
import com.qwen.opsmentor.core.countUp
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.groupTitle
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.iconBtn
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.mono
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.noteBox
import com.qwen.opsmentor.core.pill
import com.qwen.opsmentor.core.secTitle
import com.qwen.opsmentor.core.surface
import com.qwen.opsmentor.core.typeset
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.ReviewEngine
import com.qwen.opsmentor.ui.MainActivity
import com.qwen.opsmentor.ui.Page
import com.qwen.opsmentor.ui.Rows

/**
 * 课程详情（对应 Web renderCourseMin / renderCoursePlanet）：
 * 页头 + 统计与动作 + 分段标签（章节 / 课程说明 / 工具箱）。
 */
class PageCourse(act: Activity, app: App) : Page(act, app) {

    override val key = "course"

    override fun title(): String = Courses.meta(app.store, Courses.find(app.store, app.store.cur)).first
    override val learning = true

    companion object {
        var seg = "chs"
        var filter = "all"
        var descOrder = false
    }

    override fun body(host: LinearLayout) {
        val ctx = act
        val co = Courses.find(app.store, app.store.cur)
        val meta = Courses.meta(app.store, co)
        val pr = Courses.prog(app.store, pack, co.id)
        val due = ReviewEngine.due(app.store, pack).size
        val started = app.store.bag().learn.isNotEmpty()
        if (Tokens.planet) planet(host, co, meta.first, meta.second, pr, due, started) else minimal(host, co, meta.first, meta.second, pr, due, started)
    }

    // ---------------- 星球版 ----------------

    private fun planet(host: LinearLayout, co: Courses.Entry, title: String, sub: String, pr: Courses.Prog, due: Int, started: Boolean) {
        val ctx = act
        // 页头
        val head = hrow(ctx)
        head.setPadding(0, ctx.dp(4f), 0, 0)
        val left = vcol(ctx)
        left.addView(label(ctx, title, 23f, Tokens.ink, Tokens.weightH(), 3))
        if (sub.isNotEmpty()) left.add(muted(ctx, sub, 13f, 3), t = 8)
        val by = hrow(ctx)
        by.setPadding(0, ctx.dp(10f), 0, 0)
        val av = FrameLayout(ctx).apply { flat(0, ctx.dp(14f).toFloat()) }
        av.circle(Tokens.pri)
        av.addView(Icons.iconView(ctx, "book", Tokens.onPri, 15f), FrameLayout.LayoutParams(ctx.dp(15f), ctx.dp(15f)).also { it.gravity = Gravity.CENTER })
        by.addView(av)
        by.addView(label(ctx, "${pack.mods.size} 模块 · ${pack.ch.size} 章 · ${pack.routes.size} 条学习路线", 12f, Tokens.sub),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(9f) })
        left.addView(by)
        head.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val (_, _, cover) = Courses.meta(app.store, co)
        head.addView(Covers.view(ctx, ctx.dp(96f), cover.ifBlank { null }, co.coverClass, Courses.glyph(app.store, co), 6f),
            LinearLayout.LayoutParams(ctx.dp(96f), ctx.dp(96f)).also { it.topMargin = ctx.dp(14f) })
        host.addView(head)

        // 统计 + 动作
        val stat = hrow(ctx)
        stat.setPadding(0, ctx.dp(20f), 0, 0)
        val big = hrow(ctx)
        val num = label(ctx, "0", 30f, Tokens.ink, Tokens.weightH())
        num.countUp(pr.done)
        big.addView(num)
        big.addView(label(ctx, "章已过关 · 共 ${pr.total} 章", 12f, Tokens.sub),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        stat.addView(big, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val acts = hrow(ctx)
        val routeBtn = iconBtn(ctx, "route", "学习路线 · " + pack.routeName(app.store.bag().route), 40f) { routeSheet() }
        routeBtn.flat(Tokens.priSoft, ctx.dp(Tokens.rBtn()))
        acts.addView(routeBtn, LinearLayout.LayoutParams(ctx.dp(40f), ctx.dp(40f)))
        val go = label(ctx, if (started) "继续学习" else "开始学习", 13f, Tokens.onPri, Tokens.weightM())
        go.gravity = Gravity.CENTER
        go.flat(Tokens.pri, ctx.dp(Tokens.rBtn()))
        go.setPadding(ctx.dp(16f), 0, ctx.dp(16f), 0)
        go.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ctx.dp(40f)).also { it.marginStart = ctx.dp(11f) }
        go.setOnClickListener { nav("today") }
        acts.addView(go)
        stat.addView(acts)
        host.addView(stat)

        // 分段标签
        host.addView(segs(due))

        val body = vcol(ctx)
        host.addView(body)
        when (seg) {
            "chs" -> chapters(body)
            "about" -> about(body)
            else -> tools(body, due)
        }
    }

    private fun segs(due: Int): View {
        val ctx = act
        val tabs = hrow(ctx)
        tabs.setPadding(0, ctx.dp(18f), 0, 0)
        listOf("chs" to "章节", "about" to "课程说明", "tools" to "工具箱").forEachIndexed { i, (k, name) ->
            val selected = seg == k
            val item = vcol(ctx).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                isClickable = true
                setOnClickListener { seg = k; rerender(false) }
            }
            val title = label(ctx, name + if (k == "tools" && due > 0) "  $due" else "", 14.5f,
                if (selected) Tokens.pri else Tokens.ink, if (selected) Tokens.weightH() else Tokens.weightM())
            title.setPadding(0, ctx.dp(10f), 0, ctx.dp(8f))
            item.addView(title)
            val indicator = View(ctx).apply {
                flat(if (selected) Tokens.pri else android.graphics.Color.TRANSPARENT, 2f)
            }
            item.addView(indicator, LinearLayout.LayoutParams(ctx.dp(24f), ctx.dp(3f)))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (i > 0) lp.marginStart = ctx.dp(28f)
            tabs.addView(item, lp)
        }
        val line = View(ctx).apply { flat(Tokens.hairc, 1f) }
        val wrap = vcol(ctx)
        wrap.addView(tabs)
        wrap.addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(1f)))
        return wrap
    }

    private fun routeSheet() {
        val items = pack.routes.keys.map { k ->
            Opt(k, pack.routeName(k), pack.route(k).size.toString() + " 章", if (k == app.store.bag().route) "check" else "route")
        }
        Sheets.picker(act, "学习路线", items, app.store.bag().route) { v, name ->
            val b = app.store.bag()
            b.route = v; b.idx = 0
            app.store.save()
            rerender(true)
            Sheets.toast(act, "已切换到$name")
        }
    }

    private fun chapters(host: LinearLayout) {
        val ctx = act
        val done = pack.ch.count { app.store.bag().status[it.id] == "green" }
        var list = pack.ch.filter {
            when (filter) {
                "done" -> app.store.bag().status[it.id] == "green"
                "todo" -> app.store.bag().status[it.id] != "green"
                else -> true
            }
        }
        if (descOrder) list = list.reversed()
        val cnt = hrow(ctx)
        cnt.setPadding(0, ctx.dp(14f), 0, 0)
        cnt.addView(label(ctx, "${list.size} 章", 13f, Tokens.sub))
        val tools = hrow(ctx)
        val sortBtn = iconBtn(ctx, "sort", "切换排序", 34f) { descOrder = !descOrder; rerender(false) }
        if (descOrder) sortBtn.scaleY = -1f
        tools.addView(sortBtn, LinearLayout.LayoutParams(ctx.dp(34f), ctx.dp(34f)))
        cnt.addView(tools, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        host.addView(cnt)
        val chips = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("all" to "全部", "todo" to "未过关 ${pack.ch.size - done}", "done" to "已过关 $done").forEach { (k, name) ->
            chips.addView(chip(ctx, name, filter == k) { filter = k; rerender(false) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    if (chips.childCount > 0) it.marginEnd = ctx.dp(8f)
                })
        }
        host.addView(chips, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = ctx.dp(6f); it.bottomMargin = ctx.dp(4f)
        })
        if (list.isEmpty()) {
            host.addView(muted(ctx, "这个分类下还没有章节。"))
            return
        }
        val curId = currentChapterId()
        list.forEach { c ->
            host.addView(Rows.chapter(act, app, pack, c, curId, { openChapter(c.id) }, { chapterMenu(c) }))
        }
    }

    private fun about(host: LinearLayout) {
        val ctx = act
        val co = Courses.find(app.store, app.store.cur)
        val card = card(host, 16f, 14)
        card.addView(label(ctx, co.desc.ifBlank { co.sub }, 15f, Tokens.ink, Tokens.weightM()))
        if (pack.depText.isNotEmpty()) {
            card.addView(muted(ctx, "模块依赖关系：什么能跳，什么绝对不能").also {
                (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, ctx.dp(16f), 0, ctx.dp(8f))
            })
            card.addView(mono(ctx, pack.depText))
        }
        if (pack.depNotes.isNotEmpty()) {
            val g = vcol(ctx)
            pack.depNotes.forEach { g.addView(noteBox(ctx, it.first, it.second), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = ctx.dp(10f)
            }) }
            g.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(14f) }
            card.addView(g)
        }
    }

    private val TOOL_GROUPS = listOf(
        "学习节奏" to listOf(Triple("today", "今日学习", "play"), Triple("review", "复习队列", "refresh"), Triple("anno", "我的划线", "highlighter")),
        "知识资料库" to listOf(Triple("know", "知识详解", "book"), Triple("gloss", "术语手册", "aA"), Triple("case", "案例库", "folder")),
        "方法与进度" to listOf(Triple("play", "排障手册", "wrench"), Triple("prog", "学习进度", "chart"), Triple("io", "数据", "share")),
    )

    private fun tools(host: LinearLayout, due: Int) {
        val ctx = act
        TOOL_GROUPS.forEach { (g, items) ->
            host.addView(groupTitle(ctx, g))
            val grid = vcol(ctx)
            items.chunked(3).forEach { rowItems ->
                val r = hrow(ctx)
                rowItems.forEach { (v, name, icon) ->
                    val badge = if (v == "review") due else 0
                    r.addView(tile(ctx, name, icon, badge) { nav(v) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                        if (r.childCount > 0) it.marginStart = ctx.dp(8f)
                    })
                }
                repeat(3 - rowItems.size) { r.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f).also { it.marginStart = ctx.dp(8f) }) }
                grid.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = ctx.dp(8f) })
            }
            host.addView(grid)
        }
    }

    private fun tile(ctx: Activity, label2: String, icon: String, badge: Int, onClick: () -> Unit): View {
        val t = vcol(ctx)
        t.gravity = Gravity.CENTER
        t.minimumHeight = ctx.dp(88f)
        if (Tokens.planet) {
            t.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        } else {
            t.flat(Tokens.withAlpha(Tokens.ink, 0.03f), ctx.dp(Tokens.rInner()))
        }
        t.setPadding(0, ctx.dp(12f), 0, ctx.dp(12f))
        val iconSize = if (Tokens.planet) 27f else 20f
        val box = FrameLayout(ctx)
        box.addView(Icons.iconView(ctx, icon, Tokens.pri, iconSize), FrameLayout.LayoutParams(ctx.dp(iconSize), ctx.dp(iconSize)).also { it.gravity = Gravity.CENTER })
        if (badge > 0) box.addView(com.qwen.opsmentor.core.badgeDot(ctx, badge), FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ctx.dp(16f)).also {
            it.gravity = Gravity.TOP or Gravity.END
            it.topMargin = -ctx.dp(4f); it.marginEnd = -ctx.dp(6f)
        })
        t.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(if (Tokens.planet) 30f else 24f)))
        t.addView(label(ctx, label2, if (Tokens.planet) 14.5f else 12.5f, Tokens.ink, Tokens.weightM()),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(if (Tokens.planet) 11f else 6f) })
        t.isClickable = true
        t.setOnClickListener { t.animatePop(); onClick() }
        return t
    }

    // ---------------- 极简版 ----------------

    private fun minimal(host: LinearLayout, co: Courses.Entry, title: String, sub: String, pr: Courses.Prog, due: Int, started: Boolean) {
        val ctx = act
        val hero = hrow(ctx)
        hero.flat(Tokens.l2, ctx.dp(18f))
        hero.setPadding(ctx.dp(14f), ctx.dp(14f), ctx.dp(14f), ctx.dp(14f))
        val (_, _, cover) = Courses.meta(app.store, co)
        hero.addView(Covers.view(ctx, ctx.dp(86f), cover.ifBlank { null }, co.coverClass, Courses.glyph(app.store, co)))
        val ci = vcol(ctx)
        ci.addView(label(ctx, title, 20f, Tokens.ink, Tokens.weightH(), 2))
        ci.add(muted(ctx, sub, 13f, 3), t = 6)
        ci.add(label(ctx, "${pack.mods.size} 模块 · ${pack.ch.size} 章 · ${pack.routes.size} 条学习路线", 11.5f, Tokens.sub), t = 8)
        hero.addView(ci, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(13f) })
        host.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })

        val st = hrow(ctx)
        st.addView(label(ctx, "${pr.done}", 26f, Tokens.ink, Tokens.weightH()))
        st.addView(muted(ctx, "章已过关 / 共 ${pr.total} 章 · 累计学习 Day ${app.store.bag().day}"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
        host.addView(st, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(14f) })

        val acts = hrow(ctx)
        acts.addView(com.qwen.opsmentor.core.btn(ctx, if (started) "继续学习" else "开始学习", "pri").also {
            it.setOnClickListener { nav("today") }
        })
        acts.addView(com.qwen.opsmentor.core.btn(ctx, pack.routeName(app.store.bag().route), "n").also {
            it.setOnClickListener { routeSheet() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        host.addView(acts, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })

        host.addView(segs(due))
        when (seg) {
            "chs" -> {
                pack.mods.forEach { m ->
                    val mh = hrow(ctx)
                    mh.setPadding(0, ctx.dp(16f), 0, ctx.dp(6f))
                    mh.addView(label(ctx, m.k, 12f, Tokens.pri, Tokens.weightS()).also {
                        it.flat(Tokens.priSoft, ctx.dp(6f).toFloat())
                        val p = ctx.dp(6f); it.setPadding(p, 0, p, 0)
                    })
                    mh.addView(label(ctx, m.n.replace("模块 ${m.k} · ", ""), 15f, Tokens.ink, Tokens.weightH()),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
                    mh.addView(muted(ctx, m.tag, 11.5f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
                    host.addView(mh)
                    pack.ch.filter { it.m == m.k }.forEach { c ->
                        host.addView(Rows.chapter(act, app, pack, c, currentChapterId(), { openChapter(c.id) }, { chapterMenu(c) }),
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = ctx.dp(6f) })
                    }
                }
            }
            "about" -> about(host)
            else -> tools(host, due)
        }
    }

    // ---------------- 共享 ----------------


    private fun openChapter(id: String) = (act as? MainActivity)?.openChapter(id) ?: run { nav("today") }

    private fun chapterMenu(c: com.qwen.opsmentor.data.Chapter) {
        Sheets.picker(act, c.id + " " + c.t, listOf(
            Opt("play", "开始学习本章", icon = "play"),
            Opt("green", "标记为已过关", icon = "check"),
            Opt("yellow", "标记为半过关", icon = "refresh"),
            Opt("red", "标记为未过关", icon = "close"),
            Opt("clear", "清除状态", icon = "trash", danger = true),
        ), null) { v, name ->
            val b = app.store.bag()
            when (v) {
                "play" -> openChapter(c.id)
                "clear" -> { b.status.remove(c.id); Sheets.toast(act, "已清除状态") }
                else -> {
                    b.status[c.id] = v
                    if (!b.learn.containsKey(c.id)) b.learn[c.id] = com.qwen.opsmentor.state.LearnRec(com.qwen.opsmentor.state.Store.today(), ArrayList())
                    Sheets.toast(act, "${c.id} ${name.replace("标记为", "")}")
                }
            }
            app.store.save(); rerender(false)
        }
    }

    private fun View.circle(c: Int) {
        val g = android.graphics.drawable.GradientDrawable()
        g.shape = android.graphics.drawable.GradientDrawable.OVAL
        g.setColor(c)
        background = g
    }

    private fun nav(v: String) = (act as? MainActivity)?.nav(v)
}
