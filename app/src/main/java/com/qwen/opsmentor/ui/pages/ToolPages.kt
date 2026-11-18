package com.qwen.opsmentor.ui.pages
import com.qwen.opsmentor.core.weight
import com.qwen.opsmentor.core.size

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.ui.Bars
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Table
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.btn
import com.qwen.opsmentor.core.btnRow
import com.qwen.opsmentor.core.chip
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.hscroll
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.mono
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.noteBox
import com.qwen.opsmentor.core.pill
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.data.Chapter
import com.qwen.opsmentor.data.Lecture
import com.qwen.opsmentor.io.Exporter
import com.qwen.opsmentor.io.Saver
import com.qwen.opsmentor.state.CaseRec
import com.qwen.opsmentor.state.GlossRec
import com.qwen.opsmentor.state.LearnRec
import com.qwen.opsmentor.state.ReviewEngine
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.ui.MainActivity
import com.qwen.opsmentor.ui.Page

/**
 * 知识详解库：关键词搜索 + 模块筛选 + 分组条目（可划线 / 可复制 / 可跳章）。
 */
class PageKnow(act: Activity, app: App) : Page(act, app) {

    override val key = "know"
    override val learning = true
    override fun title() = "知识详解"

    private var kw = ""
    private var mod = ""

    override fun body(host: LinearLayout) {
        val c = toolHeader(host, "book", "知识详解",
            "30 章的全部细节集中在这里：具体路径、默认数值、端口、报错码、命令预期输出、操作顺序。不想按路线走的时候，直接按关键词搜你要处理的那件事。")
        c.addView(pill(act, "${hits().size} 条", true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        searchField(c, "搜索关键词，例如 42 天 / SRV / 9100 / gpresult / 租约 / 恢复密钥", kw) { kw = it; rerender(false) }
        val chips = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        pack.mods.forEach { m ->
            chips.addView(chip(act, m.n.replace("模块 ", "").replace(" · ", " "), mod == m.k) {
                mod = if (mod == m.k) "" else m.k
                rerender(false)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                if (chips.childCount > 0) it.marginEnd = act.dp(8f)
            })
        }
        c.addView(hscroll(act, chips), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = act.dp(10f)
        })
        val grouped = LinkedHashMap<String, MutableList<Triple<Chapter, Int, Pair<String, String>>>>()
        hits().forEach { h -> grouped.getOrPut(h.first.m) { ArrayList() }.add(h) }
        if (grouped.isEmpty()) {
            host.addView(muted(act, "没有匹配的知识点。换个关键词试试，比如只搜「DNS」「端口」「默认」「报错」。"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = act.dp(14f) })
            return
        }
        pack.mods.forEach { m ->
            val items = grouped[m.k] ?: return@forEach
            val card = card(host, 16f, 12)
            val hd = hrow(act)
            hd.addView(label(act, m.n, 16.5f, Tokens.ink, Tokens.weightH()).also {
                it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            hd.addView(pill(act, "${items.size} 条", false))
            card.addView(hd)
            items.forEach { (ch, _, row) ->
                card.addView(knowItem(ch, row), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    it.topMargin = act.dp(14f)
                })
            }
        }
    }

    private fun hits(): List<Triple<Chapter, Int, Pair<String, String>>> {
        val out = ArrayList<Triple<Chapter, Int, Pair<String, String>>>()
        pack.mods.forEach { m ->
            if (mod.isNotEmpty() && mod != m.k) return@forEach
            pack.ch.filter { it.m == m.k }.forEach { c ->
                pack.deep[c.id]?.deep?.forEachIndexed { i, row ->
                    val hay = (c.id + " " + c.t + " " + row.first + " " + row.second).lowercase()
                    if (kw.isBlank() || hay.contains(kw.lowercase())) out.add(Triple(c, i, row))
                }
            }
        }
        return out
    }

    private fun knowItem(c: Chapter, row: Pair<String, String>): View {
        val ctx = act
        val w = vcol(ctx)
        w.flat(if (Tokens.planet) Tokens.l3 else Tokens.withAlpha(Tokens.ink, 0.03f), ctx.dp(Tokens.rInner()))
        val p = ctx.dp(12f)
        w.setPadding(p, p, p, p)
        w.addView(pill(ctx, c.id + " " + c.t, true))
        w.addView(label(ctx, row.first, 15f, Tokens.ink, Tokens.weightS()),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        w.addView(annotated(row.second, c.id, "DK" + row.first.hashCode()),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        val row2 = btnRow(ctx)
        row2.setPadding(0, ctx.dp(10f), 0, 0)
        row2.addView(btn(ctx, "复制这条", "n", small = true).also {
            it.setOnClickListener { Sheets.copy(ctx, "[${c.id} ${c.t}] ${row.first}\n\n${row.second}", "知识点已复制") }
        })
        row2.addView(btn(ctx, "学到这章", "ghost", small = true).also {
            it.setOnClickListener { (act as? MainActivity)?.openChapter(c.id) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
        w.addView(row2)
        return w
    }

    /** 可划线段落 */
    private fun annotated(text: String, chId: String, blkId: String): View {
        val t = com.qwen.opsmentor.core.AnnotatedText(act, com.qwen.opsmentor.core.BlockSpec(chId, blkId, text), { store.marks })
        t.size(14f)
        t.weight(Tokens.weightB())
        t.setLineSpacing(0f, 1.28f)
        return block(t)
    }
}

/**
 * 复习队列：间隔提取抽问（1/2/4/7/15/30 天口径）。
 */
class PageReview(act: Activity, app: App) : Page(act, app) {

    override val key = "review"
    override val learning = true
    override fun title() = "复习队列"
    private var all = false

    override fun body(host: LinearLayout) {
        val ctx = act
        val c = toolHeader(host, "refresh", "复习队列",
            "核心原则：逼自己回忆，而不是重读笔记。重读会产生“我认识它”的错觉，只有提取才建立长期通路。答错的条目会退回第 2 天节奏重新走。")
        c.addView(pill(ctx, pack.intervals.joinToString(" / ") + " 天", true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val row = btnRow(ctx)
        row.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) }
        row.addView(btn(ctx, "现在开始抽问", "pri").also { it.setOnClickListener { (act as? MainActivity)?.nav("today") } })
        row.addView(btn(ctx, if (all) "只看今日到期" else "查看全部条目", "n").also {
            it.setOnClickListener { all = !all; rerender(false) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(row)

        val list = if (all) ReviewEngine.allLearned(store, pack) else ReviewEngine.due(store, pack)
        if (list.isEmpty()) {
            host.addView(muted(ctx, "当前没有内容。先在“今日卡片”推进几章，复习队列会自动建立。"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(14f) })
            return
        }
        list.forEach { d ->
            val ch = d.ch ?: return@forEach
            val card = card(host, 16f, 10)
            val hd = hrow(ctx)
            hd.addView(label(ctx, ch.id + " " + ch.t, 15.5f, Tokens.ink, Tokens.weightH()).also {
                it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            hd.addView(pill(ctx, if (d.iv > 0) "第 ${d.iv} 天 · 已过 ${d.diff} 天" else "已学 ${d.diff} 天", false))
            card.addView(hd)
            card.addView(label(ctx, ch.quiz.q, 14.5f, Tokens.ink, Tokens.weightB()),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
            val kp = pack.deep[ch.id]?.deep ?: emptyList()
            if (kp.isNotEmpty()) {
                val w = vcol(ctx)
                w.flat(if (Tokens.planet) Tokens.l3 else Tokens.withAlpha(Tokens.ink, 0.03f), ctx.dp(Tokens.rInner()))
                val p = ctx.dp(10f)
                w.setPadding(p, p, p, p)
                w.addView(label(ctx, "细节回忆：只准说出这三条里的一条，说得出具体数值/路径/端口才算过", 13f, Tokens.ink, Tokens.weightM()))
                w.addView(muted(ctx, kp.take(3).joinToString("　·　") { it.first }),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
                card.addView(w, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
            }
            val ans = label(ctx, "参考答案：" + ch.quiz.a + "\n\n细节补一条：" + (kp.firstOrNull()?.let { it.first + " —— " + it.second } ?: "").take(400), 14f, Tokens.ink)
            ans.flat(Tokens.l3, ctx.dp(Tokens.rInner()))
            val p2 = ctx.dp(12f)
            ans.setPadding(p2, p2, p2, p2)
            ans.visibility = View.GONE
            val row2 = btnRow(ctx)
            row2.setPadding(0, ctx.dp(10f), 0, 0)
            row2.addView(btn(ctx, "看答案与细节", "n", small = true).also { b ->
                b.setOnClickListener {
                    val show = b.visibility == View.GONE
                    ans.visibility = if (show) View.VISIBLE else View.GONE
                    b.text = if (show) "收起答案" else "看答案与细节"
                }
            })
            row2.addView(btn(ctx, "复制本章要点", "n", small = true).also {
                it.setOnClickListener { Sheets.copy(ctx, Lecture.digest(pack, ch), "本章要点已复制") }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
            if (d.iv > 0) {
                row2.addView(btn(ctx, "还记得", "pri", small = true).also {
                    it.setOnClickListener {
                        ReviewEngine.markReviewed(store, pack, ch.id, d.iv)
                        Sheets.toast(ctx, "记录完成")
                        rerender(false)
                    }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
            }
            card.addView(row2)
            card.addView(ans, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        }
    }
}

/**
 * 故障案例库：五栏固定，页内展开表单（与 Web 一致，不用系统弹窗）。
 */
class PageCases(act: Activity, app: App) : Page(act, app) {

    override val key = "case"
    override val learning = false
    override fun title() = "案例库"

    private var kw = ""
    private var draft: CaseRec? = null

    override fun body(host: LinearLayout) {
        val ctx = act
        val c = toolHeader(host, "folder", "案例库",
            "比学习笔记更重要——面试和实际排障都直接调它。五栏固定，写到 30 条以上就能直接转成面试 STAR 素材。数据存在本机。")
        c.addView(pill(ctx, "${store.cases.size} 条", true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val row = btnRow(ctx)
        row.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) }
        row.addView(btn(ctx, "＋ 新建案例", "pri").also {
            it.setOnClickListener {
                draft = CaseRec(CaseRec.nextCode(store.cases), "", "", "", "", currentChapterId())
                rerender(false)
            }
        })
        row.addView(btn(ctx, "导出全部", "n").also {
            it.setOnClickListener {
                if (store.cases.isEmpty()) { Sheets.toast(ctx, "案例库还是空的"); return@setOnClickListener }
                Saver.export(ctx, "故障案例库-" + Store.today() + ".txt", "txt", Exporter.casesText(store.cases))
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(row)
        searchField(c, "输入关键词，如 DNS / 打印机 / 4625", kw) { kw = it; rerender(false) }

        draft?.let { form(it) }

        val list = store.cases.filter {
            kw.isBlank() || listOf(it.no, it.sym, it.path, it.cause, it.lesson, it.ch).joinToString(" ").lowercase().contains(kw.lowercase())
        }
        if (store.cases.isEmpty() && draft == null) {
            host.addView(muted(ctx, "还没有案例。学完一个故障场景就记一条——五栏由页面给你脚手架，别从空白开始写。"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(14f) })
        } else if (list.isEmpty() && draft == null) {
            host.addView(muted(ctx, "没有匹配 \"$kw\" 的案例。"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(14f) })
        }
        list.forEach { item -> caseItem(item) }
    }

    private fun fieldText(label2: String, value: String, multi: Boolean, onChange: (String) -> Unit): View {
        val ctx = act
        val box = vcol(ctx)
        box.addView(label(ctx, label2, 12.5f, Tokens.sub, Tokens.weightM()))
        val inp = Sheets.input(ctx, value, "", 0, multi)
        inp.addTextChangedListener(watcher { onChange(it) })
        box.addView(inp, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
        box.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) }
        return box
    }

    private fun form(item: CaseRec): View {
        val ctx = act
        val c = card(ctx.let { content ?: LinearLayout(ctx) }, 16f, 10)
        val hd = hrow(ctx)
        hd.addView(label(ctx, "新建故障案例", 16.5f, Tokens.ink, Tokens.weightH()).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        hd.addView(pill(ctx, "五栏固定", true))
        c.addView(hd)
        c.addView(fieldText("编号（自动生成，可改）", item.no, false) { item.no = it })
        c.addView(fieldText("现象：用户原话 + 具体报错文字/代码", item.sym, false) { item.sym = it })
        c.addView(fieldText("排查路径：按时间顺序记每一步查了什么、看到什么", item.path, true) { item.path = it })
        c.addView(fieldText("根因（一句话）", item.cause, false) { item.cause = it })
        c.addView(fieldText("结论：下次遇到同类问题第一步查什么", item.lesson, true) { item.lesson = it })
        c.addView(fieldText("关联章节", item.ch, false) { item.ch = it })
        val row = btnRow(ctx)
        row.setPadding(0, ctx.dp(12f), 0, 0)
        row.addView(btn(ctx, "保存案例", "pri").also {
            it.setOnClickListener {
                if (item.sym.isBlank()) { Sheets.toast(ctx, "至少把“现象”写清楚"); return@setOnClickListener }
                if (!store.cases.contains(item)) store.cases.add(item)
                store.save(); draft = null; rerender(false)
                Sheets.toast(ctx, "已保存。案例库是你最值钱的产出物")
            }
        })
        row.addView(btn(ctx, "取消", "ghost").also {
            it.setOnClickListener { draft = null; rerender(false) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(row)
        return c
    }

    private fun caseItem(item: CaseRec): View {
        val ctx = act
        val c = card(content ?: LinearLayout(ctx), 16f, 10)
        val hd = hrow(ctx)
        hd.addView(label(ctx, item.no, 15.5f, Tokens.ink, Tokens.weightH()).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        hd.addView(pill(ctx, item.ch.ifBlank { "未关联章节" }, false))
        hd.addView(btn(ctx, "删除", "ghost", small = true).also {
            it.setOnClickListener {
                Sheets.confirm(ctx, "删除案例 ${item.no}？", "删除案例", "删除", true) {
                    store.cases.remove(item); store.save(); rerender(false)
                }
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(hd)
        listOf("现象" to item.sym, "排查路径" to item.path, "根因" to item.cause, "下次第一步查什么" to item.lesson).forEach { (k, v) ->
            val r = hrow(ctx)
            r.gravity = Gravity.TOP
            r.addView(label(ctx, "$k：", 13f, Tokens.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            r.addView(label(ctx, v.ifBlank { "—" }, 13.5f, Tokens.ink, Tokens.weightB()), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = ctx.dp(4f)
            })
            c.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        }
        val row = btnRow(ctx)
        row.setPadding(0, ctx.dp(10f), 0, 0)
        row.addView(btn(ctx, "编辑", "n", small = true).also {
            it.setOnClickListener { draft = item; rerender(false) }
        })
        row.addView(btn(ctx, "复制这条", "ghost", small = true).also {
            it.setOnClickListener {
                Sheets.copy(ctx, listOf("编号：${item.no}", "现象：${item.sym}", "排查路径：${item.path}", "根因：${item.cause}",
                    "结论：${item.lesson}", "关联章节：${item.ch}").joinToString("\n"), "案例已复制")
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
        c.addView(row)
        return c
    }
}

/**
 * 术语手册：章节双轨自动导入 + 自填术语，四列表格。
 */
class PageGloss(act: Activity, app: App) : Page(act, app) {

    override val key = "gloss"
    override val learning = false
    override fun title() = "术语手册"
    private var kw = ""

    override fun body(host: LinearLayout) {
        val ctx = act
        val auto = Exporter.baseGloss(pack)
        val c = toolHeader(host, "aA", "术语手册", "每学完一章，把这一章的核心术语加进来。这张表一路长大，就是你自己的运维词典，也是面试前的速查页。")
        c.addView(pill(ctx, "自填 ${store.gloss.size} 条 ｜ 自动生成 ${auto.size} 条", true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val row = btnRow(ctx)
        row.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) }
        row.addView(btn(ctx, "＋ 补充术语", "pri").also { it.setOnClickListener { addTerm() } })
        row.addView(btn(ctx, "从已学章节自动导入", "n").also {
            it.setOnClickListener { rerender(false); Sheets.toast(ctx, "已学章节的双轨对照已全部展示在表中") }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        row.addView(btn(ctx, "导出 CSV", "ghost").also {
            it.setOnClickListener { Saver.export(ctx, "术语手册-${Store.today()}.csv", "csv", Exporter.glossCsv(store, pack)) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(row)
        searchField(c, "检索术语或大白话", kw) { kw = it; rerender(false) }

        val all = store.gloss + auto
        val rows = all.filter {
            kw.isBlank() || (it.term + " " + it.full + " " + it.say).lowercase().contains(kw.lowercase())
        }
        val card = card(host, 12f, 12)
        val table = ArrayList<List<String>>()
        table.add(listOf("术语", "全称", "大白话", "来源", ""))
        rows.take(120).forEach { g ->
            table.add(listOf(g.term, g.full, g.say, if (g.src == "auto") "章节导入" else "我填的", ""))
        }
        val tv = Table(ctx).build(table)
        card.addView(tv)
        // 自填术语的删除按钮单独列出（表格构建器不支持交互单元格）
        val mine = rows.filter { it.src != "auto" }.take(40)
        if (mine.isNotEmpty()) {
            card.addView(label(ctx, "我填的术语（可删除）", 13f, Tokens.sub, Tokens.weightM()),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
            mine.forEach { g ->
                val r = hrow(ctx)
                r.addView(label(ctx, g.term, 14f, Tokens.ink, Tokens.weightM()).also {
                    it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                r.addView(btn(ctx, "删", "ghost", small = true).also {
                    it.setOnClickListener {
                        store.gloss.remove(g); store.save(); rerender(false)
                    }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
                card.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
            }
        }
        if (rows.size > 120) card.addView(muted(ctx, "仅显示前 120 条，请用检索过滤"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
    }

    private fun addTerm() {
        val ctx = act
        val term = Sheets.input(ctx, "", "术语名（如 SRV 记录）", 40)
        val full = Sheets.input(ctx, "", "全称或说明（如 Service Resource Record）", 80)
        val say = Sheets.input(ctx, "", "一句话大白话（如：写着“物业办公室往哪走”的专用指路牌）", 120)
        val box = vcol(ctx).apply { setPadding(ctx.dp(4f), 0, ctx.dp(4f), 0) }
        box.addView(Sheets.field(ctx, "术语名", term))
        box.addView(Sheets.field(ctx, "全称 / 说明", full))
        box.addView(Sheets.field(ctx, "大白话", say))
        Sheets.sheet(ctx, "补充术语", box, "加入", onSave = {
            val t = term.text.toString().trim()
            if (t.isEmpty()) { Sheets.toast(ctx, "术语名不能为空"); return@sheet false }
            store.gloss.add(0, GlossRec(t, full.text.toString().trim(), say.text.toString().trim()))
            store.save()
            rerender(false)
            Sheets.toast(ctx, "已加入你的术语手册")
            true
        })
    }
}

/**
 * 排障手册：分层排障法 / 命令工具箱 / 高频事件 ID / 经典故障剧本。
 */
class PagePlaybook(act: Activity, app: App) : Page(act, app) {

    override val key = "play"
    override val learning = true
    override fun title() = "排障手册"

    private var cmdKw = ""
    private var evKw = ""
    private var openScen = -1

    override fun body(host: LinearLayout) {
        val ctx = act
        // 分层排障法
        val c1 = toolHeader(host, "wrench", "排障手册", "从下往上逐层确认，任何一层没通就不必怀疑上层。先量体温再拍片，别一上来就开刀。")
        c1.addView(pill(ctx, "默认主框架", true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        pack.layers.forEachIndexed { i, (k, v) ->
            val r = hrow(ctx)
            r.gravity = Gravity.TOP
            r.addView(label(ctx, "${i + 1}", 11.5f, Tokens.onPri, Tokens.weightS()).also {
                it.gravity = Gravity.CENTER
                it.flat(Tokens.pri, ctx.dp(10f).toFloat())
            }, LinearLayout.LayoutParams(ctx.dp(20f), ctx.dp(20f)))
            val box = vcol(ctx)
            box.addView(label(ctx, k, 14.5f, Tokens.ink, Tokens.weightM()))
            if (v.isNotEmpty()) box.addView(muted(ctx, v), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(2f) })
            r.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(10f) })
            c1.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        }
        c1.addView(noteBox(ctx, "动手之前必问用户三件事：", "① 什么时候开始坏的、上次能用是什么时候 ② 那之后动过什么（更新、换网线、改策略）③ 只有个别人这样，还是所有人都这样。"))
        // 命令工具箱
        val c2 = toolHeader(host, "g_terminal", "命令工具箱", "")
        c2.addView(pill(ctx, "均为只读查询", false), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        searchField(c2, "检索命令或用途，如 nslookup / 时间 / 策略", cmdKw) { cmdKw = it; rerender(false) }
        val t2 = ArrayList<List<String>>()
        t2.add(listOf("命令", "用途", "档位"))
        pack.cmds.filter { kw -> cmdKw.isBlank() || kw.joinToString(" ").lowercase().contains(cmdKw.lowercase()) }.forEach { t2.add(it) }
        c2.addView(Table(ctx).build(t2), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        // 高频事件 ID
        val c3 = toolHeader(host, "info", "高频事件 ID", "")
        c3.addView(pill(ctx, "病历本编号", false), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        searchField(c3, "输入 ID 或关键词，如 1054 / Kerberos", evKw) { evKw = it; rerender(false) }
        val t3 = ArrayList<List<String>>()
        t3.add(listOf("ID", "来源", "含义", "第一反应"))
        pack.events.filter { evKw.isBlank() || it.joinToString(" ").lowercase().contains(evKw.lowercase()) }.forEach { t3.add(it) }
        c3.addView(Table(ctx).build(t3), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        // 经典故障剧本
        val c4 = toolHeader(host, "bookOpen", "经典故障剧本", "训练方式：先只读现象，自己说第一步查什么，再展开对照标准链路。")
        c4.addView(pill(ctx, "${pack.scen.size} 个", false), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        pack.scen.forEachIndexed { i, s ->
            val acc = vcol(ctx)
            acc.flat(if (Tokens.planet) Tokens.l3 else Tokens.withAlpha(Tokens.ink, 0.03f), ctx.dp(Tokens.rInner()))
            val hd = hrow(ctx)
            hd.setPadding(ctx.dp(12f), ctx.dp(12f), ctx.dp(12f), ctx.dp(12f))
            hd.addView(label(ctx, s.t, 14.5f, Tokens.ink, Tokens.weightM()).also {
                it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            hd.addView(label(ctx, if (openScen == i) "▼" else "▶", 12f, Tokens.sub))
            hd.isClickable = true
            hd.setOnClickListener { openScen = if (openScen == i) -1 else i; rerender(false) }
            acc.addView(hd)
            if (openScen == i) {
                val inner = vcol(ctx)
                inner.setPadding(ctx.dp(12f), 0, ctx.dp(12f), ctx.dp(12f))
                inner.addView(muted(ctx, "现象：" + s.p), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                inner.addView(label(ctx, "标准排障链路", 14f, Tokens.ink, Tokens.weightH()), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
                s.steps.forEachIndexed { j, st ->
                    val r = hrow(ctx)
                    r.gravity = Gravity.TOP
                    r.addView(label(ctx, "${j + 1}", 11.5f, Tokens.pri, Tokens.weightS()))
                    r.addView(label(ctx, st, 14f, Tokens.ink), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
                    inner.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
                }
                inner.addView(label(ctx, "高频根因：" + s.c, 14f, Tokens.ink, Tokens.weightM()), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
                inner.addView(noteBox(ctx, "处理：", s.f))
                inner.addView(btn(ctx, "复制这条排障链路", "n", small = true).also {
                    it.setOnClickListener {
                        Sheets.copy(ctx, s.t + "\n现象：" + s.p + "\n\n" + s.steps.mapIndexed { k, x -> "${k + 1}. $x" }.joinToString("\n") +
                                "\n\n高频根因：" + s.c + "\n处理：" + s.f, "链路已复制")
                    }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
                acc.addView(inner)
            }
            c4.addView(acc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        }
    }
}

/**
 * 学习进度：统计 + 章节过关门 + 实验环境 + 本机数据。
 */
class PageProgress(act: Activity, app: App) : Page(act, app) {

    override val key = "prog"
    override val learning = true
    override fun title() = "学习进度"

    override fun body(host: LinearLayout) {
        val ctx = act
        val q = pack.route(store.bag().route)
        val done = q.count { store.status[it] == "green" }
        val stats = listOf(
            "$done / ${q.size}" to "已过关章节",
            "${store.bag().day}" to "累计学习 Day",
            "${pack.knowTotal}" to "知识点详解",
            "${pack.outTotal}" to "命令输出样例",
            "${store.cases.size} 条" to "案例库",
            "${store.gloss.size} 条" to "自填术语",
            "${ReviewEngine.due(store, pack).size}" to "待复习",
        )
        val c0 = toolHeader(host, "chart", "学习进度", "")
        c0.addView(Bars.statGrid(ctx, stats.map { it.second to it.first }, 3), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = ctx.dp(12f)
        })
        // 过关门
        val c1 = toolHeader(host, "check", "章节过关门", "🔴 概念说不出来 ｜ 🟡 能说出但不清楚在什么故障场景用 ｜ 🟢 能结合案例讲清。未过关不进下一章，补测不超过 15 分钟。")
        c1.addView(pill(ctx, "三问齐了才算过", true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        q.forEach { id ->
            val ch = pack.chById(id) ?: return@forEach
            val st = store.status[id]
            val r = hrow(ctx)
            r.gravity = Gravity.TOP
            val box = vcol(ctx)
            box.addView(label(ctx, ch.id + " " + ch.t, 14.5f, Tokens.ink, Tokens.weightM()))
            box.addView(muted(ctx, ch.gate, 12.5f), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(2f) })
            r.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            r.addView(label(ctx, Tokens.statusLabel(st), 12.5f, Tokens.statusColor(st), Tokens.weightM()),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = ctx.dp(8f) })
            val sel = btn(ctx, "切换", "ghost", small = true)
            sel.setOnClickListener {
                Sheets.picker(ctx, ch.id + " " + ch.t, listOf(
                    com.qwen.opsmentor.core.Opt("green", "🟢 过关", icon = "check"),
                    com.qwen.opsmentor.core.Opt("yellow", "🟡 半过", icon = "refresh"),
                    com.qwen.opsmentor.core.Opt("red", "🔴 未过关", icon = "close"),
                    com.qwen.opsmentor.core.Opt("", "清除状态", icon = "trash", danger = true),
                ), st) { v, _ ->
                    if (v.isEmpty()) store.status.remove(ch.id) else {
                        store.status[ch.id] = v
                        if (!store.learn.containsKey(ch.id)) store.learn[ch.id] = LearnRec(Store.today(), ArrayList())
                    }
                    store.save(); rerender(false)
                }
            }
            r.addView(sel)
            c1.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        }
        // 实验环境
        val c2 = toolHeader(host, "g_server", "实验环境", "")
        c2.addView(pill(ctx, "动手的前提", true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        c2.addView(mono(ctx, "你的实体机（Win10/11 专业版，建议 16G 内存）\n└── Hyper-V 或 VMware Workstation Player（免费）\n    ├── VM1 Windows Server 2022 评估版 → AD DS + DNS，域名 lab.contoso.com\n    ├── VM2 Windows 11 评估版 → 加域客户端\n    └── VM3（可选）第二台域控 → 学复制与备份时才加"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        pack.lab.forEach { (k, v) ->
            val r = vcol(ctx)
            r.addView(label(ctx, k, 14f, Tokens.ink, Tokens.weightM()))
            r.addView(muted(ctx, v, 12.5f), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(2f) })
            c2.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        }
        c2.addView(noteBox(ctx, "红线：", "服务器侧操作只在实验虚拟机做。生产环境一律走公司变更审批；任何需要管理员权限的操作都不要在自己办公机上随手跑。"))
        // 数据
        val c3 = toolHeader(host, "database", "数据", "本应用是纯本地应用：进度、案例库、术语手册全部保存在本机，不联网、不上传、无账号体系。换设备或清除数据前，请先导出完整档案。")
        c3.addView(btn(ctx, "清空全部本地数据", "danger").also {
            it.setOnClickListener {
                Sheets.confirm(ctx, "确认清空本机保存的全部进度、案例库与自填术语？此操作不可撤销。", "清空数据", "清空", true) {
                    store.wipe()
                    store.load()
                    app.retheme()
                    (act as? MainActivity)?.nav("mine")
                    Sheets.toast(ctx, "已清空")
                }
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
    }
}
