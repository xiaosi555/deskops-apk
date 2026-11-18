package com.qwen.opsmentor.ui.pages

import android.app.Activity
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.core.AnnotatedText
import com.qwen.opsmentor.core.BlockSpec
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.MarkdownView
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Table
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.add
import com.qwen.opsmentor.core.btn
import com.qwen.opsmentor.core.btnRow
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.mono
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.noteBox
import com.qwen.opsmentor.core.pill
import com.qwen.opsmentor.core.typeset
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.data.Chapter
import com.qwen.opsmentor.data.ConceptBlock
import com.qwen.opsmentor.data.GuideBlock
import com.qwen.opsmentor.data.Lecture
import com.qwen.opsmentor.data.LongLecture
import com.qwen.opsmentor.data.StepBlock
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.LineType
import com.qwen.opsmentor.state.Mark
import com.qwen.opsmentor.state.MarkColor
import com.qwen.opsmentor.state.ReviewEngine
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.ui.MainActivity
import com.qwen.opsmentor.ui.Page
import com.qwen.opsmentor.ui.Rows

/**
 * 章节学习（今日卡片，对应 Web renderToday + renderLong）：
 * 完整讲义（概念 / 实操 / 指南 / 互动题）+ 双轨对照 + 专业清单 + 过关标准 + 速览切换 + 划线。
 */
class PageToday(act: Activity, app: App) : Page(act, app) {

    override val key = "today"
    override val learning = true

    private val segs = ArrayList<String>()

    override fun posKey(): String {
        val b = app.store.bag()
        val c = current()
        return if (b.pin.isNotEmpty() && pack.chById(b.pin) != null) b.pin else (c?.id ?: key)
    }

    override fun title(): String {
        val c = current() ?: return ""
        return c.id + " " + c.t
    }

    private fun current(): Chapter? {
        val b = app.store.bag()
        if (b.pin.isNotEmpty()) pack.chById(b.pin)?.let { return it }
        val q = pack.route(b.route)
        if (q.isEmpty()) return pack.ch.firstOrNull()
        return pack.chById(q[b.idx.coerceIn(0, q.size - 1)])
    }

    override fun readingSegs(): List<String> = segs

    override fun body(host: LinearLayout) {
        val ctx = act
        segs.clear()
        val c = current()
        if (c == null) {
            host.addView(com.qwen.opsmentor.core.emptyState(ctx, "book", "这门课程还没有章节内容", "可通过「课程 → 导入」补充"))
            return
        }
        val b = app.store.bag()
        val q = pack.route(b.route)
        val card = card(host, 16f, 12)
        // 卡头
        val hd = vcol(ctx)
        val pillRow = hrow(ctx)
        pillRow.addView(pill(ctx, c.id + " ｜ 模块 " + c.m, true))
        hd.addView(pillRow)
        val th = label(ctx, c.t, 20f, Tokens.ink, Tokens.weightH())
        th.typeset(26)
        hd.addView(th, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        card.addView(hd)
        card.addView(muted(ctx, "Day ${b.day} · 第 ${(b.idx.coerceIn(0, (q.size - 1).coerceAtLeast(0))) + 1} / ${q.size} 站 ｜ 本节要解决的问题：${c.s}"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        if (b.pin.isNotEmpty()) {
            card.addView(noteBox(ctx, "", "当前是从课程地图直接展开的详解视图（${b.pin}），不改变你的路线进度。看完可以按下面的「回到路线进度」继续原来的学习顺序。"))
        }

        renderLecture(card, c)

        // 双轨对照
        if (c.plain.isNotEmpty()) {
            val sec = vcol(ctx)
            sec.addView(label(ctx, "核心概念 · 双轨表达", 16f, Tokens.ink, Tokens.weightH()))
            sec.addView(muted(ctx, "下面这张表左边是大白话，右边是面试和文档里要说的术语。角色一一对应，不要只记一边。"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
            sec.addView(Table(ctx).build(listOf(listOf("大白话", "专业术语")) + c.plain.map { listOf(it.first, it.second) }),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
            card.addView(sec, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(18f) })
        }
        // 专业清单
        if (c.terms.isNotEmpty()) {
            val sec = vcol(ctx)
            sec.addView(label(ctx, "本章专业清单", 16f, Tokens.ink, Tokens.weightH()))
            sec.addView(paragraph(c.terms.split("｜").joinToString(" ／ "), "TERMS"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
            card.addView(sec, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(16f) })
        }
        // 过关标准
        val gs = vcol(ctx)
        gs.addView(label(ctx, "过关标准", 16f, Tokens.ink, Tokens.weightH()))
        gs.addView(paragraph(c.gate, "GATE"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        val st = b.status[c.id]
        gs.addView(label(ctx, Tokens.statusLabel(st), 13f, Tokens.statusColor(st), Tokens.weightM()),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        card.addView(gs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(16f) })

        // 导航按钮
        val nav = btnRow(ctx)
        nav.setPadding(0, ctx.dp(14f), 0, 0)
        val half = btn(ctx, "🟡 能说出但不清楚用在哪", "n", small = true)
        half.setOnClickListener {
            b.status[c.id] = "yellow"
            if (!b.learn.containsKey(c.id)) b.learn[c.id] = com.qwen.opsmentor.state.LearnRec(Store.today(), ArrayList())
            app.store.save()
            Sheets.toast(ctx, "已标记 🟡，建议补一次场景提问")
            rerender(false)
        }
        nav.addView(half)
        val jump = btn(ctx, "跳到指定章节", "ghost", small = true)
        jump.setOnClickListener { nav2("course") }
        nav.addView(jump, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        val next = btn(ctx, "下一站 →", "pri", small = true)
        next.setOnClickListener {
            if (!b.learn.containsKey(c.id)) b.learn[c.id] = com.qwen.opsmentor.state.LearnRec(Store.today(), ArrayList())
            b.pin = ""
            b.idx = (b.idx + 1).coerceAtMost((q.size - 1).coerceAtLeast(0))
            b.day += 1
            app.store.save()
            (act as? MainActivity)?.toTop = true
            Sheets.toast(ctx, "已进入 " + (q.getOrElse(b.idx) { "" }))
            rerender(true)
        }
        nav.addView(next, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        card.addView(nav)
        card.addView(noteBox(ctx, "今天只需带走的一句话", c.s + "。这一章的过关标准就是“能不能不看资料把这个类比和术语对上”。"))
    }

    private fun nav2(v: String) = (act as? MainActivity)?.nav(v)

    /** 一个可划线段落 */
    private fun paragraph(text: String, blkId: String, sizeSp: Float = 15f, weight: Int = Tokens.weightB()): View {
        val ctx = act
        val t = AnnotatedText(ctx, BlockSpec(currentId(), blkId, text), { app.store.marks })
        t.size(sizeSp)
        t.weight(weight)
        t.setLineSpacing(0f, 1.30f)
        return block(t)
    }

    private fun currentId(): String = posKey().ifEmpty { current()?.id ?: "" }

    /** 讲义四段 */
    private fun renderLecture(card: LinearLayout, c: Chapter) {
        val ctx = act
        val d = Lecture.of(pack, c)
        val lite = app.store.bag().mode == "lite"
        val isMd = d.concept.isNotEmpty() && d.concept.all { it.md.isNotEmpty() }
        if (isMd) {
            d.concept.forEachIndexed { i, b ->
                val mv = MarkdownView(ctx, b.md, currentId(), "M${i}_", { app.store.marks }, { m -> MarkSheet.open(act, app, m) }, { v, a, b2 ->
                    (act as? MainActivity)?.selectionBar?.open(v, a, b2)
                })
                card.addView(mv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(14f) })
                blocks.addAll(mv.blocks)
                mv.blocks.forEach { it.refresh() }
                segs.addAll(mv.blocks.map { it.textOf() })
            }
        } else {
            if (d.concept.isNotEmpty()) section(card, "概", "概念", "讲透它到底解决什么问题") { box ->
                if (d.title.isNotEmpty()) {
                    box.addView(paragraph(d.title, "T", 15.5f, Tokens.weightH()), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
                    segs.add(d.title)
                }
                d.concept.forEachIndexed { i, b ->
                    when {
                        b.p.isNotEmpty() -> {
                            box.addView(paragraph(b.p, "C${i}"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
                            segs.add(b.p)
                        }
                        b.parts.isNotEmpty() -> {
                            val txt = b.parts.joinToString("") { it.text }
                            box.addView(paragraph(txt, "C${i}p"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
                            segs.add(txt)
                        }
                        b.md.isNotEmpty() -> {
                            val mv = MarkdownView(ctx, b.md, currentId(), "C${i}m", { app.store.marks }, { m -> MarkSheet.open(act, app, m) }, { v, a, b2 ->
                                (act as? MainActivity)?.selectionBar?.open(v, a, b2)
                            })
                            box.addView(mv)
                            blocks.addAll(mv.blocks)
                            segs.addAll(mv.blocks.map { it.textOf() })
                        }
                    }
                    if (b.ul.isNotEmpty()) {
                        b.ul.forEachIndexed { j, u ->
                            val row = hrow(ctx)
                            row.addView(label(ctx, "•", 14f, Tokens.pri, Tokens.weightM()))
                            row.addView(paragraph(u, "C${i}u$j", 14.5f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
                            box.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
                            segs.add(u)
                        }
                    }
                    if (b.isTable) {
                        box.addView(Table(ctx).build(listOf(b.head) + b.rows), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
                    }
                }
            }
        }
        if (d.practice.isNotEmpty()) section(card, "操", "实操", "命令、图形界面路径与预期输出") { box ->
            d.practice.forEachIndexed { i, st ->
                box.addView(step(st, i, lite), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
            }
        }
        if (d.guide.isNotEmpty() && !lite) section(card, "指", "指南", "避坑与运维习惯") { box ->
            d.guide.forEachIndexed { i, g ->
                val row = hrow(ctx)
                row.addView(label(ctx, "${i + 1}", 12.5f, Tokens.pri, Tokens.weightS()).also {
                    it.gravity = Gravity.CENTER
                    it.flat(Tokens.priSoft, ctx.dp(6f).toFloat())
                }, LinearLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f)))
                val bx = vcol(ctx)
                g.lines().forEachIndexed { j, line ->
                    bx.addView(paragraph(line, "G${i}$j", 14.5f),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(if (j == 0) 0 else 3f) })
                    segs.add(line)
                }
                bx.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(10f) }
                row.addView(bx)
                box.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
            }
        }
        if (d.quiz.q.isNotEmpty()) section(card, "题", "互动题", "答完再看解析") { box ->
            box.addView(paragraph(d.quiz.q, "QZ", 15f), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
            segs.add(d.quiz.q)
            if (d.quiz.hint.isNotEmpty()) box.addView(muted(ctx, "提示：" + d.quiz.hint), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
            val ans = label(ctx, "参考答案：" + d.quiz.a, 14.5f, Tokens.ink, Tokens.weightB())
            ans.flat(Tokens.l3, ctx.dp(Tokens.rInner()))
            val p = ctx.dp(12f)
            ans.setPadding(p, p, p, p)
            ans.maxLines = if (lite) 3 else 0
            ans.ellipsize = TextUtils.TruncateAt.END
            ans.visibility = View.GONE
            val row = btnRow(ctx)
            row.setPadding(0, ctx.dp(10f), 0, 0)
            row.addView(btn(ctx, "看参考答案", "n", small = true).also {
                it.setOnClickListener {
                    if (ans.visibility == View.GONE) {
                        ans.visibility = View.VISIBLE; ans.maxLines = 0; it.text = "收起答案"
                    } else { ans.visibility = View.GONE; it.text = "看参考答案" }
                }
            })
            row.addView(btn(ctx, "这题我答对了 · 标记 🟢 过关", "pri", small = true).also { b ->
                b.setOnClickListener {
                    val bag = app.store.bag()
                    if (!bag.learn.containsKey(currentId())) bag.learn[currentId()] = com.qwen.opsmentor.state.LearnRec(Store.today(), ArrayList())
                    bag.status[currentId()] = "green"
                    app.store.save()
                    Sheets.toast(ctx, "已标记过关并加入复习队列")
                    rerender(false)
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
            box.addView(row)
            box.addView(ans, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        }
        // 模式切换行
        val mr = vcol(ctx)
        val r1 = hrow(ctx)
        val longB = btn(ctx, "完整讲义", if (app.store.bag().mode == "long") "pri" else "n", small = true)
        longB.setOnClickListener { app.store.bag().mode = "long"; app.store.save(); rerender(false) }
        r1.addView(longB)
        r1.addView(btn(ctx, "速览（折叠细节）", if (app.store.bag().mode == "lite") "pri" else "n", small = true).also {
            it.setOnClickListener { app.store.bag().mode = "lite"; app.store.save(); rerender(false) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
        r1.addView(btn(ctx, "我的划线 ${app.store.marks.size}", "ghost", small = true).also {
            it.setOnClickListener { nav2("anno") }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
        mr.addView(r1, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(16f) })
        mr.addView(muted(ctx, "长按选中正文即可划线（可跨段）；同一段落能叠多条不同颜色，删除只删自己那条。点划线写备注、改线型、打标签。"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        card.addView(mr)
    }

    private fun section(card: LinearLayout, no: String, title: String, sub: String, fill: (LinearLayout) -> Unit) {
        val ctx = act
        val sec = vcol(ctx)
        val lt = hrow(ctx)
        val num = label(ctx, no, 12.5f, Tokens.onPri, Tokens.weightS())
        num.gravity = Gravity.CENTER
        num.circleS(ctx, Tokens.pri, 11f)
        lt.addView(num, LinearLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f)))
        lt.addView(label(ctx, title, 16f, Tokens.ink, Tokens.weightH()), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        lt.addView(muted(ctx, sub, 11.5f), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        sec.addView(lt)
        val box = vcol(ctx)
        fill(box)
        sec.addView(box)
        card.addView(sec, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(16f) })
    }

    private fun step(st: StepBlock, i: Int, lite: Boolean): View {
        val ctx = act
        val w = vcol(ctx)
        w.flat(if (Tokens.planet) Tokens.l3 else Tokens.withAlpha(Tokens.ink, 0.03f), ctx.dp(Tokens.rInner()))
        val p = ctx.dp(12f)
        w.setPadding(p, p, p, p)
        val hd = hrow(ctx)
        val num = label(ctx, "${i + 1}", 12f, Tokens.pri, Tokens.weightS())
        num.gravity = Gravity.CENTER
        hd.addView(num, LinearLayout.LayoutParams(ctx.dp(20f), ctx.dp(20f)))
        hd.addView(label(ctx, st.t, 15f, Tokens.ink, Tokens.weightH()), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
        w.addView(hd)
        if (st.d.isNotEmpty()) {
            w.addView(paragraph(st.d, "P${i}d", 14f), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
            segs.add(st.d)
        }
        if (st.md.isNotEmpty() && !lite) {
            val mv = MarkdownView(ctx, st.md, currentId(), "P${i}m", { app.store.marks }, { m -> MarkSheet.open(act, app, m) }, { v, a, b2 ->
                (act as? MainActivity)?.selectionBar?.open(v, a, b2)
            })
            w.addView(mv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
            blocks.addAll(mv.blocks)
        }
        if (st.cmd.isNotEmpty()) {
            w.addView(mono(ctx, st.cmd), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
            if (st.out.isNotEmpty() && !lite) {
                w.addView(label(ctx, "输出样例", 11.5f, Tokens.sub, Tokens.weightM()), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
                w.addView(mono(ctx, st.out, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
            }
            if (st.read.isNotEmpty() && !lite) {
                w.addView(paragraph("怎么读：" + st.read, "P${i}r", 13.5f), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
                segs.add("怎么读：" + st.read)
            }
            val row = btnRow(ctx)
            row.setPadding(0, ctx.dp(8f), 0, 0)
            row.addView(btn(ctx, "复制这条命令", "n", small = true).also {
                it.setOnClickListener { Sheets.copy(ctx, st.cmd, "命令已复制") }
            })
            row.addView(btn(ctx, "复制本节要点", "ghost", small = true).also {
                it.setOnClickListener { Sheets.copy(ctx, Lecture.digest(pack, current() ?: return@setOnClickListener), "要点已复制") }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(6f) })
            w.addView(row)
        }
        return w
    }

    private fun View.circleS(ctx: Activity, color: Int, rDp: Float) {
        flat(if (Tokens.planet) Tokens.pri else Tokens.priSoft, ctx.dp(rDp))
        setTextColor(if (Tokens.planet) Tokens.onPri else Tokens.pri)
    }
}

/** 点划线后的快捷编辑（浮层面板走 Sheets，与 Web mark editor panel 对齐） */
object MarkSheet {
    fun open(act: Activity, app: App, m: Mark) {
        val host = act as? MainActivity
        com.qwen.opsmentor.core.MarkEditor.open(act, app, m, { v -> v }, { mm -> host?.locateCurrent(mm) ?: false })
    }
}
