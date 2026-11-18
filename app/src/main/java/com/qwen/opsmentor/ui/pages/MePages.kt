package com.qwen.opsmentor.ui.pages

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.core.AnnotatedText
import com.qwen.opsmentor.ui.Bars
import com.qwen.opsmentor.core.Covers
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.Opt
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
import com.qwen.opsmentor.core.iconBtn
import com.qwen.opsmentor.core.iconView
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.mono
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.noteBox
import com.qwen.opsmentor.core.pill
import com.qwen.opsmentor.core.secTitle
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.io.Exporter
import com.qwen.opsmentor.io.Importer
import com.qwen.opsmentor.io.Saver
import com.qwen.opsmentor.media.MediaCourses
import com.qwen.opsmentor.state.Appearance
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.LineType
import com.qwen.opsmentor.state.MarkColor
import com.qwen.opsmentor.state.ReviewEngine
import com.qwen.opsmentor.state.Shortcut
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.state.StyleMode
import com.qwen.opsmentor.ui.MainActivity
import com.qwen.opsmentor.ui.Page
import com.qwen.opsmentor.ui.Rows
import com.qwen.opsmentor.ui.ShortcutsEdit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 数据页：六类档案 × 三种格式的导出预览（先预览后落盘），以及档案导入（合并 / 覆盖）。
 * 对应 Web renderIo + 导出预览层。
 */
class PageIo(act: Activity, app: App) : Page(act, app) {

    override val key = "io"
    override fun title() = when (detail) {
        null -> "数据"
        "import" -> "导入档案"
        else -> Exporter.entry(detail)?.title ?: "数据"
    }

    private var detail: String? = null
    private var format = "json"
    private var impText = ""
    private var impMode = "merge"
    private var parsed: com.qwen.opsmentor.io.Parsed? = null
    private var msg = ""

    fun inDetail() = detail != null
    fun popDetail() { detail = null }

    override fun body(host: LinearLayout) {
        when (detail) {
            null -> overview(host)
            "import" -> importPage(host)
            else -> detailPage(host, detail!!)
        }
    }

    private fun overview(host: LinearLayout) {
        val ctx = act
        val c = card(host, 16f, 12)
        val hd = hrow(ctx)
        hd.addView(label(ctx, "数据", 17f, Tokens.ink, Tokens.weightH()).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        hd.addView(pill(ctx, "本机数据 · 可迁移", true))
        c.addView(hd)
        c.addView(muted(ctx, "进度、划线与备注、案例库、术语手册都存在这台设备上。换设备、卸载或清数据之前，请先导出一份完整档案。所有导出都在本机生成、不上传；点按钮只打开预览，确认后由你手动保存，不会突然弹出下载框。"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        val stats = listOf(
            store.allMarks().size.toString() to "划线",
            store.allMarks().count { it.n.isNotEmpty() }.toString() to "带备注",
            store.cases.size.toString() to "案例",
            store.gloss.size.toString() to "自填术语",
            store.status.values.count { it == "green" }.toString() to "已过关",
            pack.knowTotal.toString() to "内置知识点",
        )
        c.addView(Bars.statGrid(ctx, stats, 3), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = ctx.dp(12f)
        })
        Exporter.ENTRIES.forEach { e ->
            host.addView(Rows.list(act, e.icon, e.title, e.desc, "") {
                detail = e.id; format = if (e.id == "handbook" || e.id == "marks") "md" else "json"; rerender(true)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = ctx.dp(8f)
            })
        }
        host.addView(Rows.list(act, "upload", "导入档案", "选择或粘贴一份 JSON 存档，可合并或覆盖", "") {
            detail = "import"; rerender(true)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        host.addView(Rows.list(act, "settings", "个人数据", "导出全部个人设置与学习数据（含自建课程）", "") {
            (act as? MainActivity)?.nav("personal-data")
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        if (msg.isNotEmpty()) host.addView(muted(ctx, msg), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
    }

    private fun detailPage(host: LinearLayout, scope: String) {
        val ctx = act
        val meta = Exporter.entry(scope) ?: return overview(host)
        val head = card(host, 16f, 12)
        val hd = hrow(ctx)
        val box = FrameLayout(ctx).apply {
            flat(Tokens.priSoft, ctx.dp(10f).toFloat())
            layoutParams = LinearLayout.LayoutParams(ctx.dp(44f), ctx.dp(44f))
        }
        box.addView(iconView(ctx, meta.icon, Tokens.pri, 22f), FrameLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f)).also { it.gravity = Gravity.CENTER })
        hd.addView(box)
        val tx = vcol(ctx)
        tx.addView(label(ctx, meta.title, 15.5f, Tokens.ink, Tokens.weightH()))
        tx.addView(muted(ctx, Courses.meta(app.store, Courses.find(app.store, store.cur)).first), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(3f) })
        hd.addView(tx, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(12f) })
        head.addView(hd)

        val fmtCard = card(host, 16f, 10)
        fmtCard.addView(label(ctx, "格式", 13f, Tokens.sub, Tokens.weightM()))
        val row = hrow(ctx)
        listOf(Triple("json", "database", "JSON"), Triple("md", "book", "MD"), Triple("csv", "chart", "CSV")).forEach { (f, ico, name) ->
            val b = btn(ctx, "$name  .${f}", if (format == f) "pri" else "n", small = true)
            b.setOnClickListener { format = f; rerender(false) }
            row.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                if (row.childCount > 0) it.marginStart = ctx.dp(6f)
            })
        }
        fmtCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        val go = btn(ctx, "预览并导出 ${format.uppercase()}", "pri")
        go.setOnClickListener {
            val (name, text) = Exporter.archiveText(store, app.packs, scope, format)
            Saver.export(ctx, meta.title + " · " + format.uppercase(), name, format, text)
        }
        fmtCard.addView(go, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(44f)).also { it.topMargin = ctx.dp(12f) })
        val sum = card(host, 16f, 10)
        sum.addView(label(ctx, "包含", 13f, Tokens.sub, Tokens.weightM()))
        sum.addView(Bars.statGrid(ctx, summary(scope), 3), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        sum.addView(noteBox(ctx, "格式说明", hint(scope, format)))
    }

    private fun summary(scope: String): List<Pair<String, String>> {
        val b = store.bag()
        return listOf(
            store.marks.size.toString() to "划线",
            b.cases.size.toString() to "案例",
            b.gloss.size.toString() to "自填术语",
            b.learn.size.toString() to "已学章节",
            b.status.values.count { it == "green" }.toString() to "已过关",
            pack.ch.size.toString() to "章节目录",
        )
    }

    private fun hint(scope: String, fmt: String): String = when (fmt) {
        "json" -> "UTF-8 编码的 ${if (scope == "all") "完整档案" else Exporter.entry(scope)?.name}，字段结构与 Web 版一致，可在另一端「导入档案」原样恢复。"
        "md" -> "可读文档：标题层级与表格排版与学习手册一致，可粘进笔记软件或转 PDF。"
        else -> "CSV 带 UTF-8 BOM，Excel / WPS 直接打开不乱码；一行一条。"
    }

    private fun importPage(host: LinearLayout) {
        val ctx = act
        val c = card(host, 16f, 12)
        val hd = hrow(ctx)
        hd.addView(label(ctx, "导入档案", 17f, Tokens.ink, Tokens.weightH()).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        hd.addView(pill(ctx, "合并 / 覆盖", true))
        c.addView(hd)
        c.addView(muted(ctx, "支持本应用导出的完整档案 / 课程资料 / 局部档案（划线、进度、案例）。粘贴 JSON 文本，或从本机选择 .json 文件。"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        val ta = Sheets.input(ctx, impText, "粘贴存档 JSON，或点下方「选择文件」", 0, true)
        ta.minLines = 6
        ta.addTextChangedListener(watcher { impText = it })
        c.addView(ta, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(150f)).also { it.topMargin = ctx.dp(10f) })
        val row = btnRow(ctx)
        row.setPadding(0, ctx.dp(10f), 0, 0)
        row.addView(btn(ctx, "选择文件", "n").also {
            it.setOnClickListener { pickJson() }
        })
        row.addView(btn(ctx, "解析预览", "pri").also {
            it.setOnClickListener {
                val p = Importer.parse(impText)
                parsed = if (p.err.isEmpty()) p else null
                msg = if (p.err.isEmpty()) "解析成功：${p.summary.firstOrNull()?.second ?: ""}" else p.err
                rerender(false)
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(row)
        if (impText.isBlank()) {
            c.addView(noteBox(ctx, "导入前先备份", "导入会改动本机数据；覆盖模式会重建当前课程的进度与划线。建议先导出完整档案再操作。"))
            return
        }
        val p = parsed
        if (p == null) {
            c.addView(muted(ctx, if (msg.isNotEmpty()) msg else "还没有解析结果。", 13f))
            return
        }
        val sum = vcol(ctx)
        p.summary.forEach { (k, v) ->
            val r = hrow(ctx)
            r.addView(label(ctx, k, 13f, Tokens.sub), LinearLayout.LayoutParams(ctx.dp(96f), ViewGroup.LayoutParams.WRAP_CONTENT))
            r.addView(label(ctx, v, 13.5f, Tokens.ink, Tokens.weightM()), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            sum.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
        }
        c.addView(sum, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
        val modes = hrow(ctx)
        modes.setPadding(0, ctx.dp(12f), 0, 0)
        modes.addView(chip(ctx, "合并（保留现有数据）", impMode == "merge") { impMode = "merge"; rerender(false) })
        modes.addView(chip(ctx, "覆盖（用存档替换）", impMode == "replace") { impMode = "replace"; rerender(false) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(modes)
        if (!p.full) {
            c.addView(muted(ctx, "这是局部档案（划线 / 进度 / 案例），将按勾选范围合并进当前课程。"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        }
        val go = btn(ctx, "应用存档", "pri")
        go.setOnClickListener {
            try {
                val r = Importer.apply(store, app.packs, p.obj, impMode)
                app.retheme()
                msg = r.summaryText()
                parsed = null; impText = ""
                rerender(false)
                Sheets.toast(ctx, "导入完成：" + msg)
            } catch (e: Exception) {
                Sheets.toast(ctx, "导入失败：${e.message?.take(48) ?: "存档结构不符"}")
            }
        }
        c.addView(go, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(44f)).also { it.topMargin = ctx.dp(12f) })
    }

    private fun pickJson() {
        val a = act as? MainActivity ?: return
        a.pickText { name, text ->
            impText = text
            parsed = Importer.parse(text, name).takeIf { it.err.isEmpty() }
            rerender(false)
        }
    }
}

/**
 * 个人数据：整套状态导出 / 导入（对应 Web personal-data）。
 */
class PagePersonalData(act: Activity, app: App) : Page(act, app) {

    override val key = "personal-data"
    override fun title() = "个人数据"

    override fun body(host: LinearLayout) {
        val ctx = act
        val c = card(host, 16f, 12)
        c.addView(label(ctx, "个人数据", 17f, Tokens.ink, Tokens.weightH()))
        c.addView(muted(ctx, "学习进度、划线与备注、案例与术语、自建与导入的课程、头像与昵称、主题与主题色、快捷入口、朗读设置等。视频 / 音频文件本身不在其中，导入后需要重新连接。"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        val rows = listOf(
            Courses.all(store).size.toString() to "课程",
            store.allMarks().size.toString() to "划线",
            store.status.values.count { it == "green" }.toString() to "已过关章节",
            (store.profileName.ifBlank { "未设置" }) to "昵称",
            (if (store.style == StyleMode.PLANET) "星球版" else "极简版") to "主题",
            store.shortcuts.size.toString() to "快捷入口",
        )
        c.addView(Bars.statGrid(ctx, rows, 3), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = ctx.dp(12f)
        })
        val row = btnRow(ctx)
        row.setPadding(0, ctx.dp(12f), 0, 0)
        row.addView(btn(ctx, "预览个人数据 JSON", "pri").also {
            it.setOnClickListener {
                val text = Exporter.personalJson(store)
                Saver.export(ctx, "个人数据 · JSON 存档", "个人数据-${Store.today()}.json", "json", text)
            }
        })
        row.addView(btn(ctx, "导出全量备份", "n").also {
            it.setOnClickListener { Saver.export(ctx, "全量备份 · JSON", "桌面运维-学习存档-${Store.today()}.json", "json", Exporter.backupJson(store)) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(row)
        c.addView(noteBox(ctx, "提示", "个人数据不含媒体文件本体；导入到另一台设备后，视频 / 语音课程需要重新连接文件。"))
    }
}

/**
 * 学习日历：日 / 周 / 月 / 年四视图 + 时长汇总 + 明细 + 校正时长。
 */
class PageCalendar(act: Activity, app: App) : Page(act, app) {

    override val key = "calendar"
    override fun title() = "学习日历"

    companion object {
        private var mode = "week"
        private var off = 0
    }

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    override fun body(host: LinearLayout) {
        val ctx = act
        val now = Calendar.getInstance()
        val (from, to, label2) = range(now)
        val days = daysBetween(from, to)
        val tabs = hrow(ctx)
        listOf("day" to "日", "week" to "周", "month" to "月", "year" to "年").forEach { (k, n) ->
            tabs.addView(chip(ctx, n, mode == k) { mode = k; off = 0; rerender(true) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    if (tabs.childCount > 0) it.marginStart = ctx.dp(6f)
                })
        }
        host.addView(tabs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
        // 区间导航
        val nav = hrow(ctx)
        nav.setPadding(0, ctx.dp(12f), 0, 0)
        nav.addView(btn(ctx, "‹", "n", small = true).also { it.setOnClickListener { off--; rerender(false) } })
        nav.addView(label(ctx, label2, 15f, Tokens.ink, Tokens.weightH()).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
        nav.addView(btn(ctx, "›", "n", small = true).also {
            it.isEnabled = off < 0
            it.alpha = if (off < 0) 1f else 0.35f
            it.setOnClickListener { if (off < 0) { off++; rerender(false) } }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        host.addView(nav)
        // 汇总
        val mins = LinkedHashMap<String, Int>()
        var idx = 0
        while (idx <= days) {
            val d = fmt.format(dayAt(from, idx))
            mins[d] = minutesOf(d)
            idx++
        }
        val total = mins.values.sum()
        val active = mins.values.count { it > 0 }
        val learned = learnedIn(from, to)
        val sum = card(host, 16f, 12)
        val big = label(ctx, fmtDur(total), 30f, Tokens.ink, Tokens.weightH())
        sum.addView(big)
        sum.addView(muted(ctx, if (mode == "day") "学习章节 ${learned.size} 个" else "$active 天有学习 · 日均 ${fmtDur(if (active > 0) total / active else 0)} · 章节 ${learned.size} 个"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
        // 图表
        if (mode == "week" || mode == "year") {
            val items = ArrayList<Pair<String, Int>>()
            if (mode == "week") {
                for (i in 0..6) {
                    val d = dayAt(from, i)
                    items.add("一二三四五六日"[i].toString() + (if (fmt.format(d) == fmt.format(Date())) "·今" else ""), minutesOf(fmt.format(d)))
                }
            } else {
                val y = Calendar.getInstance().apply { add(Calendar.YEAR, off) }.get(Calendar.YEAR)
                for (mth in 0..11) {
                    var v = 0
                    val c2 = Calendar.getInstance().apply { set(y, mth, 1) }
                    while (c2.get(Calendar.MONTH) == mth) {
                        v += minutesOf(fmt.format(c2.time)); c2.add(Calendar.DAY_OF_MONTH, 1)
                    }
                    items.add("${mth + 1}", v)
                }
            }
            val mx = items.maxOf { it.second }.coerceAtLeast(1)
            val bars = hrow(ctx)
            bars.setPadding(0, ctx.dp(14f), 0, 0)
            items.forEach { (l, v) ->
                val col = vcol(ctx)
                col.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                col.addView(label(ctx, if (v > 0) shortDur(v) else "", 10.5f, Tokens.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                val fill = View(ctx)
                val h = ctx.dp(6f) + (if (v > 0) (v.toFloat() / mx * 96f) else 0f)
                fill.flat(if (v > 0) Tokens.pri else Tokens.withAlpha(Tokens.sub, 0.15f), 3f)
                col.addView(fill, LinearLayout.LayoutParams(ctx.dp(14f), h.toInt()))
                col.addView(label(ctx, l, 11f, Tokens.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(4f) })
                bars.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            }
            sum.addView(bars, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(150f)).also { it.topMargin = ctx.dp(10f) })
        }
        if (mode == "month") {
            sum.addView(monthGrid(ctx, from), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
        }
        // 明细
        val log = card(host, 16f, 12)
        if (mode == "day") {
            val key = fmt.format(from)
            log.addView(label(ctx, "当日学习", 15.5f, Tokens.ink, Tokens.weightH()))
            val ids = learnedOn(key)
            if (ids.isEmpty()) log.addView(muted(ctx, "当天没有完成章节学习的记录。"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
            ids.forEach { id ->
                val ch = pack.chById(id)
                log.addView(label(ctx, "$id ${ch?.t ?: ""}", 14f, Tokens.ink, Tokens.weightM()), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
            }
            log.addView(btn(ctx, "校正时长", "n", small = true).also {
                it.setOnClickListener {
                    val inp = Sheets.input(ctx, (store.studyLog[key] ?: 0).toString(), "例如 35（分钟）", 6)
                    Sheets.sheet(ctx, "学习时长 · $key", vcol(ctx, padH = 16).apply { addView(Sheets.field(ctx, "分钟", inp)) }, "保存", onSave = {
                        val v = inp.text.toString().toIntOrNull()
                        if (v == null || v < 0) { Sheets.toast(ctx, "请输入有效分钟数"); return@sheet false }
                        store.studyLog[key] = v; store.save(); rerender(false); Sheets.toast(ctx, "时长已保存"); true
                    })
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
        } else {
            log.addView(label(ctx, "这段时间的学习记录", 15.5f, Tokens.ink, Tokens.weightH()))
            val keys = ArrayList<String>()
            var i = 0
            while (i <= days) {
                val k = fmt.format(dayAt(from, i))
                if (minutesOf(k) > 0 || learnedOn(k).isNotEmpty()) keys.add(k)
                i++
            }
            if (keys.isEmpty()) log.addView(muted(ctx, "还没有记录。读章节时会自动计时，完成章节学习后也会出现在这里。"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
            keys.reversed().take(12).forEach { k ->
                val r = hrow(ctx)
                r.addView(label(ctx, k.substring(5).replace("-", "月") + "日", 14f, Tokens.ink, Tokens.weightM()).also {
                    it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                r.addView(muted(ctx, fmtDur(minutesOf(k)) + (if (learnedOn(k).isNotEmpty()) " · ${learnedOn(k).size} 章" else "")))
                r.isClickable = true
                r.setOnClickListener {
                    val target = fmt.parse(k) ?: return@setOnClickListener
                    off = daysBetween(target, Date()).let { if (it > 0) -1 else 0 }
                    mode = "day"
                    calOverride = target
                    rerender(false)
                }
                log.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
            }
        }
        log.addView(muted(ctx, "时长在阅读章节、复习、划线等学习页面自动记录；离开页面或 2 分钟无操作时暂停。"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
    }

    private var calOverride: Date? = null

    /** 区间：返回起止与标题 */
    private fun range(now: Calendar): Triple<Date, Date, String> {
        calOverride?.let { d ->
            calOverride = null
            return Triple(d, d, SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(d))
        }
        val c = now.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, off)
        return when (mode) {
            "day" -> Triple(c.time, c.time, SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(c.time))
            "month" -> {
                val s = c.clone() as Calendar
                s.set(Calendar.DAY_OF_MONTH, 1)
                val e = c.clone() as Calendar
                e.set(Calendar.DAY_OF_MONTH, e.getActualMaximum(Calendar.DAY_OF_MONTH))
                Triple(s.time, e.time, SimpleDateFormat("yyyy年M月", Locale.CHINA).format(c.time))
            }
            "year" -> {
                val s = c.clone() as Calendar
                s.set(Calendar.MONTH, 0); s.set(Calendar.DAY_OF_MONTH, 1)
                val e = c.clone() as Calendar
                e.set(Calendar.MONTH, 11); e.set(Calendar.DAY_OF_MONTH, 31)
                Triple(s.time, e.time, "${c.get(Calendar.YEAR)} 年")
            }
            else -> {
                val s = c.clone() as Calendar
                s.firstDayOfWeek = Calendar.MONDAY
                s.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val e = s.clone() as Calendar
                e.add(Calendar.DAY_OF_YEAR, 6)
                Triple(s.time, e.time, "${SimpleDateFormat("M月d日", Locale.CHINA).format(s.time)} – ${SimpleDateFormat("M月d日", Locale.CHINA).format(e.time)}")
            }
        }
    }

    private fun dayAt(from: Date, i: Int): Date {
        val c = Calendar.getInstance().apply { time = from }
        c.add(Calendar.DAY_OF_YEAR, i)
        return c.time
    }

    private fun daysBetween(a: Date, b: Date): Int =
        Math.round((b.time - a.time) / 86400000.0).toInt().coerceAtLeast(0)

    private fun minutesOf(day: String): Int = (store.studyLog[day] ?: 0) + autoMinutes(day)

    /** 自动累计的分钟数（与人工校正值合并取较大者） */
    private fun autoMinutes(day: String): Int = store.bags.values.sumOf { bag ->
        bag.learn.values.count { it.at == day } * 12
    }

    private fun learnedOn(day: String): List<String> =
        store.bags[store.cur]?.learn?.filterValues { it.at == day }?.keys?.toList() ?: emptyList()

    private fun learnedIn(from: Date, to: Date): List<String> {
        val out = ArrayList<String>()
        var i = 0
        val n = daysBetween(from, to)
        while (i <= n) {
            val k = fmt.format(dayAt(from, i))
            out += learnedOn(k)
            i++
        }
        return out.distinct()
    }

    private fun fmtDur(min: Int): String = when {
        min >= 60 -> "${min / 60} 小时 ${min % 60} 分"
        else -> "$min 分钟"
    }

    private fun shortDur(min: Int): String = if (min >= 60) "${min / 60}h" else if (min > 0) "${min}m" else ""

    private fun monthGrid(ctx: Activity, from: Date): View {
        val c = Calendar.getInstance().apply { time = from }
        val first = (c.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val lastDay = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val grid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val dow = hrow(ctx)
        "日一二三四五六".forEach { ch ->
            dow.addView(label(ctx, ch.toString(), 11.5f, Tokens.sub).also { it.gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        grid.addView(dow)
        val cells = ArrayList<View>()
        for (i in 0 until (first.get(Calendar.DAY_OF_WEEK) - 1)) cells.add(View(ctx))
        for (d in 1..lastDay) {
            val day = dayAt(first.time, d - 1)
            val key = fmt.format(day)
            val v = minutesOf(key)
            val cell = vcol(ctx)
            cell.gravity = Gravity.CENTER
            cell.flat(if (key == fmt.format(Date())) Tokens.priSoft else if (v > 0) Tokens.withAlpha(Tokens.pri, 0.07f) else Tokens.withAlpha(Tokens.ink, 0.03f), ctx.dp(8f).toFloat())
            cell.setPadding(0, ctx.dp(6f), 0, ctx.dp(6f))
            cell.addView(label(ctx, "$d", 13f, if (v > 0) Tokens.pri else Tokens.ink, if (v > 0) Tokens.weightM() else Tokens.weightB()))
            if (v > 0) cell.addView(label(ctx, shortDur(v), 9.5f, Tokens.sub))
            val mine = cell
            cell.isClickable = true
            cell.setOnClickListener { mode = "day"; calOverride = day; rerender(false) }
            cells.add(mine)
        }
        while (cells.size % 7 != 0) cells.add(View(ctx))
        cells.chunked(7).forEach { row ->
            val r = hrow(ctx)
            row.forEach { c2 ->
                r.addView(c2, LinearLayout.LayoutParams(0, ctx.dp(42f), 1f).also { it.marginEnd = ctx.dp(3f); it.topMargin = ctx.dp(3f) })
            }
            grid.addView(r)
        }
        return grid
    }
}

/**
 * 设置：主题色 / 外观 / 主题风格 / 顶部装饰 / 状态栏留白。
 */
class PageSettings(act: Activity, app: App) : Page(act, app) {

    override val key = "settings"
    override fun title() = "设置"

    private val DECOS = listOf(
        "lines" to "竖线", "hair" to "细光", "grid" to "网格", "dots" to "点阵", "contour" to "等高线",
        "ink" to "水墨", "aurora" to "极光", "stars" to "星空", "ripple" to "涟漪", "scan" to "扫描",
        "beam" to "光束", "none" to "无",
    )

    override fun body(host: LinearLayout) {
        val ctx = act
        // 主题色
        val c1 = card(host, 16f, 12)
        c1.addView(label(ctx, "主题色", 15.5f, Tokens.ink, Tokens.weightH()))
        val grid = vcol(ctx)
        var row = hrow(ctx)
        c1.addView(grid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
        Tokens.PALETTE.forEachIndexed { i, hex ->
            val b = FrameLayout(ctx).apply {
                flat(if (i == store.accent) Tokens.withAlpha(hex, 0.14f) else Tokens.withAlpha(Tokens.ink, 0.03f), ctx.dp(10f).toFloat())
                isClickable = true
            }
            val dot = View(ctx).apply { flat(hex, 7f) }
            b.addView(dot, FrameLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f)).also { it.gravity = Gravity.CENTER })
            val name = label(ctx, Tokens.PALETTE_NAMES[i], 10.5f, if (i == store.accent) Tokens.pri else Tokens.sub, if (i == store.accent) Tokens.weightM() else 400)
            name.gravity = Gravity.CENTER
            val col = vcol(ctx)
            col.gravity = Gravity.CENTER
            col.addView(b, LinearLayout.LayoutParams(ctx.dp(52f), ctx.dp(44f)))
            col.addView(name)
            col.isClickable = true
            col.setOnClickListener {
                store.accent = i
                store.save(); app.retheme(); rerender(false)
            }
            if (i % 4 == 0) {
                row = hrow(ctx)
                grid.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    it.topMargin = ctx.dp(6f)
                })
            }
            row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                if (row.childCount > 0) it.marginStart = ctx.dp(4f)
            })
            if (i == Tokens.PALETTE.size - 1) repeat(3 - (row.childCount - 1)) {
                row.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f).also { it.marginStart = ctx.dp(4f) })
            }
        }
        // 外观
        val c2 = card(host, 16f, 10)
        c2.addView(label(ctx, "外观", 15.5f, Tokens.ink, Tokens.weightH()))
        c2.addView(trio(listOf(
            Triple("system", "跟随系统", "auto"), Triple("light", "亮色", "sun"), Triple("dark", "暗色", "moon"),
        ), when (store.appearance) {
            Appearance.SYSTEM -> "system"; Appearance.LIGHT -> "light"; Appearance.DARK -> "dark"
        }) { v ->
            store.appearance = when (v) {
                "light" -> Appearance.LIGHT; "dark" -> Appearance.DARK; else -> Appearance.SYSTEM
            }
            store.theme = if (v == "dark") "dark" else "light"
            store.save(); app.retheme(); rerender(false)
        })
        // 主题风格
        val c3 = card(host, 16f, 10)
        c3.addView(label(ctx, "主题", 15.5f, Tokens.ink, Tokens.weightH()))
        c3.addView(trio(listOf(
            Triple("minimal", "极简版", "克制、留白、轻量层级"),
            Triple("planet", "星球版", "播客式大标题与封面列表"),
        ), if (store.style == StyleMode.PLANET) "planet" else "minimal") { v ->
            store.style = if (v == "minimal") StyleMode.MINIMAL else StyleMode.PLANET
            store.save(); app.retheme(); rerender(false)
            Sheets.toast(ctx, if (v == "planet") "已切换星球版" else "已切换极简版")
        })
        // 顶部装饰
        val c4 = card(host, 16f, 10)
        c4.addView(label(ctx, "顶部装饰", 15.5f, Tokens.ink, Tokens.weightH()))
        val dg = vcol(ctx)
        DECOS.chunked(4).forEach { row ->
            val r = hrow(ctx)
            row.forEach { (k, name) ->
                val on = (store.deco.ifBlank { if (Tokens.planet) "lines" else "none" }) == k
                val b = btn(ctx, name, if (on) "pri" else "n", small = true)
                b.setOnClickListener {
                    store.deco = k; store.save(); app.retheme(); rerender(false)
                }
                r.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    if (r.childCount > 0) it.marginStart = ctx.dp(6f)
                })
            }
            repeat(4 - row.size) { r.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f).also { it.marginStart = ctx.dp(6f) }) }
            dg.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(6f) })
        }
        c4.addView(dg)
        // 状态栏留白
        val c5 = card(host, 16f, 10)
        val sw = hrow(ctx)
        val swTitle = label(ctx, "顶部状态栏留白", 15f, Tokens.ink, Tokens.weightM())
        swTitle.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        sw.addView(swTitle)
        val pill = FrameLayout(ctx).apply {
            val w = ctx.dp(46f); val h = ctx.dp(26f)
            layoutParams = LinearLayout.LayoutParams(w, h)
        }
        fun paintSwitch() {
            pill.flat(if (store.topInset) Tokens.pri else Tokens.withAlpha(Tokens.sub, 0.25f), 13f)
            val knob = View(ctx).apply { flat(0xFFFFFFFF.toInt(), 10f) }
            pill.removeAllViews()
            pill.addView(knob, FrameLayout.LayoutParams(ctx.dp(20f), ctx.dp(20f)).also {
                it.gravity = if (store.topInset) Gravity.END else Gravity.START
                it.marginStart = ctx.dp(3f); it.marginEnd = ctx.dp(3f)
            })
        }
        paintSwitch()
        pill.isClickable = true
        pill.setOnClickListener { store.topInset = !store.topInset; store.save(); paintSwitch(); (act as? MainActivity)?.refresh() }
        sw.addView(pill)
        c5.addView(sw)
        c5.addView(muted(ctx, "关闭后内容顶到状态栏下，适合全面屏沉浸式阅读；系统状态栏图标对比度会自动调整。"),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(8f) })
    }

    private fun trio(items: List<Triple<String, String, String>>, value: String, onPick: (String) -> Unit): View {
        val ctx = act
        val row = hrow(ctx)
        items.forEach { (v, name, sub) ->
            val b = vcol(ctx)
            b.gravity = Gravity.CENTER
            b.flat(if (v == value) Tokens.priSoft else Tokens.withAlpha(Tokens.ink, 0.04f), ctx.dp(10f).toFloat())
            b.setPadding(0, ctx.dp(12f), 0, ctx.dp(12f))
            val t = label(ctx, name, 14f, if (v == value) Tokens.pri else Tokens.ink, Tokens.weightM())
            t.gravity = Gravity.CENTER
            b.addView(t)
            val s = label(ctx, if (sub.length > 12) sub.take(12) + "…" else sub, 10.5f, Tokens.sub)
            s.gravity = Gravity.CENTER
            b.addView(s, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(2f) })
            b.isClickable = true
            b.setOnClickListener { onPick(v) }
            row.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                if (row.childCount > 0) it.marginStart = ctx.dp(8f)
            })
        }
        return row
    }
}

/**
 * 课程管理：文字 / 视频 / 语音三分类 + 课程行编辑 + 导入入口。
 */
class PageCourseManage(act: Activity, app: App) : Page(act, app) {

    override val key = "course-manage"
    override fun title() = "课程管理"

    override fun appbarActions(host: LinearLayout) {
        val ctx = act
        host.addView(iconBtn(ctx, "plus", "新增课程", 34f) { com.qwen.opsmentor.ui.CourseEditor.openNew(act, app) })
        host.addView(iconBtn(ctx, "importCourse", "导入课程", 34f) { (act as? MainActivity)?.nav("course-import") })
    }

    override fun body(host: LinearLayout) {
        val ctx = act
        val cnt = mutableMapOf("text" to 0, "video" to 0, "audio" to 0)
        Courses.all(app.store).forEach { cnt[Cases_type(it)] = (cnt[Cases_type(it)] ?: 0) + 1 }
        var tab = store.cmTab
        if ((cnt[tab] ?: 0) == 0) tab = listOf("text", "video", "audio").firstOrNull { (cnt[it] ?: 0) > 0 } ?: "text"
        val tabs = hrow(ctx)
        listOf("text" to "文字", "video" to "视频", "audio" to "语音").forEach { (k, name) ->
            tabs.addView(chip(ctx, "$name ${cnt[k] ?: 0}", tab == k) {
                tab = k; store.cmTab = k; store.save(); rerender(false)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                if (tabs.childCount > 0) it.marginStart = ctx.dp(6f)
            })
        }
        host.addView(tabs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
        val list = Courses.all(app.store).filter { Cases_type(it) == tab }
        if (list.isEmpty()) {
            val empty = mapOf(
                "text" to "还没有文字课程。点右上角的导入图标，粘贴或选择课程文件即可导入。",
                "video" to "还没有视频课程。点右上角的导入图标，切到「视频」读取手机里的视频文件夹。",
                "audio" to "还没有语音课程。点右上角的导入图标，切到「语音」读取手机里的音频文件夹。",
            )
            host.addView(muted(ctx, empty[tab] ?: ""), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(14f) })
        }
        val card = card(host, 14f, 10)
        list.forEach { co ->
            val meta = Courses.meta(app.store, co)
            val p = app.packs.of(app.store, co.id)
            val pr = Courses.prog(app.store, p, co.id)
            val r = hrow(ctx)
            r.setPadding(0, ctx.dp(10f), 0, ctx.dp(10f))
            r.addView(Covers.view(ctx, ctx.dp(52f), meta.third.ifBlank { null }, co.coverClass, Courses.glyph(app.store, co)),
                LinearLayout.LayoutParams(ctx.dp(52f), ctx.dp(52f)))
            val ci = vcol(ctx)
            ci.addView(label(ctx, meta.first, 15f, Tokens.ink, Tokens.weightM(), 1))
            ci.addView(muted(ctx, meta.second.ifBlank { "课程简介尚未填写" }, 12.5f, 2), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(2f) })
            ci.addView(muted(ctx, "${pr.chs} 章 · ${pr.done}/${pr.total} 已过关", 11.5f), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(2f) })
            r.addView(ci, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(12f) })
            val edit = iconBtn(ctx, "pen", "编辑课程", 34f) { com.qwen.opsmentor.ui.CourseEditor.open(act, app, co) }
            r.addView(edit)
            r.isClickable = true
            r.setOnClickListener {
                store.setCur(co.id); store.save()
                PageCourse.seg = "chs"
                (act as? MainActivity)?.nav("course")
            }
            card.addView(r)
            if (co !== list.last()) card.addView(View(ctx).apply { flat(Tokens.hairc, 1f) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(1f)))
        }
        val row = btnRow(ctx)
        row.setPadding(0, ctx.dp(12f), 0, 0)
        row.addView(btn(ctx, "＋ 新增课程", "pri").also { it.setOnClickListener { com.qwen.opsmentor.ui.CourseEditor.openNew(act, app) } })
        row.addView(btn(ctx, "导入课程数据", "n").also { it.setOnClickListener { (act as? MainActivity)?.nav("course-import") } },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        host.addView(row)
    }

    private fun Cases_type(co: Courses.Entry): String = if (co.media == "video") "video" else if (co.media == "audio") "audio" else "text"
}

/**
 * 导入课程：文字（粘贴 / 选文件 / 图标 / 导入）与媒体（选文件夹 / 排序 / 建课）。
 */
class PageCourseImport(act: Activity, app: App) : Page(act, app) {

    override val key = "course-import"
    override fun title() = "导入课程"

    private var tab = "text"
    private var text = ""
    private var glyph = "book"
    private var mediaTitle = ""
    private var sortKey = "name"
    private var sortAsc = true

    override fun body(host: LinearLayout) {
        val ctx = act
        val tabs = hrow(ctx)
        listOf("text" to "文字", "video" to "视频", "audio" to "语音").forEach { (k, n) ->
            tabs.addView(chip(ctx, n, tab == k) { tab = k; rerender(true) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    if (tabs.childCount > 0) it.marginStart = ctx.dp(6f)
                })
        }
        host.addView(tabs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
        if (tab == "text") textPane(host) else mediaPane(host)
    }

    private fun textPane(host: LinearLayout) {
        val ctx = act
        val c = card(host, 16f, 12)
        c.addView(muted(ctx, "支持本应用导出的课程档案：JSON（完整结构）、Markdown（只用标题层级）、CSV（一行一条）。原始数据保存在本机课程资料中，不会覆盖现有学习进度。"))
        val ta = Sheets.input(ctx, text, "粘贴课程档案（JSON / Markdown / CSV），或选择文件", 0, true)
        ta.minLines = 6
        ta.addTextChangedListener(watcher { text = it })
        c.addView(ta, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(160f)).also { it.topMargin = ctx.dp(10f) })
        val row = btnRow(ctx)
        row.setPadding(0, ctx.dp(10f), 0, 0)
        row.addView(btn(ctx, "选择文件", "n").also {
            it.setOnClickListener {
                (act as MainActivity).pickText { name, body ->
                    text = body
                    rerender(false)
                }
            }
        })
        row.addView(btn(ctx, "导入课程", "pri").also {
            it.setOnClickListener {
                val p = Importer.parse(text)
                if (p.err.isNotEmpty()) { Sheets.toast(ctx, if (p.err.length > 26) p.err.take(26) + "…" else p.err); return@setOnClickListener }
                if (!p.full) { Sheets.toast(ctx, "这是局部档案（划线 / 进度 / 案例），请到「数据 → 导入档案」导入"); return@setOnClickListener }
                val r = Importer.apply(store, app.packs, p.obj, "merge")
                if (r.created.isEmpty()) { Sheets.toast(ctx, "没有可导入的课程（需要 title）"); return@setOnClickListener }
                r.created.forEach { id ->
                    store.co[id] = (store.co[id] ?: com.qwen.opsmentor.state.CoMeta()).apply { glyph = this@PageCourseImport.glyph }
                }
                store.cmTab = "text"; store.save()
                Sheets.popCheck(ctx, "已导入 ${r.created.size} 门课程")
                (act as MainActivity).goBack()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(row)
        c.addView(iconGrid(ctx), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
    }

    private fun iconGrid(ctx: Activity): View {
        val box = vcol(ctx)
        box.addView(label(ctx, "课程图标", 12.5f, Tokens.sub, Tokens.weightM()))
        Icons.COURSE_GLYPHS.chunked(8).forEach { rowItems ->
            val r = hrow(ctx)
            rowItems.forEach { g ->
                val b = FrameLayout(ctx).apply {
                    flat(if (g == glyph) Tokens.priSoft else Tokens.withAlpha(Tokens.ink, 0.04f), ctx.dp(8f).toFloat())
                    isClickable = true
                }
                b.addView(iconView(ctx, g, if (g == glyph) Tokens.pri else Tokens.sub, 20f),
                    FrameLayout.LayoutParams(ctx.dp(20f), ctx.dp(20f)).also { it.gravity = Gravity.CENTER })
                b.setOnClickListener { glyph = g; rerender(false) }
                r.addView(b, LinearLayout.LayoutParams(0, ctx.dp(40f), 1f).also { it.marginEnd = ctx.dp(4f); it.topMargin = ctx.dp(4f) })
            }
            box.addView(r)
        }
        return box
    }

    private fun mediaPane(host: LinearLayout) {
        val ctx = act
        val kind = if (tab == "video") "video" else "audio"
        val c = card(host, 16f, 12)
        c.addView(muted(ctx, "读取手机里的视频 / 音频文件夹建课：文件名即章节名，按名称自然排序（1、2、10 排在正确位置）。文件只在本机读取，不会上传。"))
        val row = btnRow(ctx)
        row.setPadding(0, ctx.dp(10f), 0, 0)
        row.addView(btn(ctx, "选择文件夹", "pri").also {
            it.setOnClickListener {
                (act as MainActivity).pickTree { uri ->
                    if (uri == null) return@pickTree
                    val items = MediaCourses.scan(ctx, uri, kind)
                    if (items.isEmpty()) { Sheets.toast(ctx, "没有在这个文件夹里找到${if (kind == "video") "视频" else "音频"}文件；可点下方「改选单个文件」"); return@pickTree }
                    if (mediaTitle.isBlank()) mediaTitle = MediaCourses.displayName(ctx, uri)
                    mediaTitle.let { t ->
                        val id = MediaCourses.createCourse(app, kind, t.ifBlank { if (kind == "video") "视频课程" else "语音课程" }, items, glyph)
                        if (id != null) {
                            store.cmTab = kind; store.save()
                            Sheets.popCheck(ctx, "已导入${if (kind == "video") "视频" else "语音"}课程")
                            (act as MainActivity).goBack()
                        } else Sheets.toast(ctx, "建课失败")
                    }
                }
            }
        })
        row.addView(btn(ctx, "改选单个文件", "n").also {
            it.setOnClickListener {
                (act as MainActivity).pickMedia(kind) { uris ->
                    if (uris.isNullOrEmpty()) { Sheets.toast(ctx, "没有选到可用文件"); return@pickMedia }
                    val items = MediaCourses.fromUris(ctx, uris, kind)
                    val id = MediaCourses.createCourse(app, kind, mediaTitle.ifBlank { if (kind == "video") "视频课程" else "语音课程" }, items, glyph)
                    if (id != null) {
                        store.cmTab = kind; store.save()
                        Sheets.popCheck(ctx, "已导入${items.size} 个文件")
                        (act as MainActivity).goBack()
                    }
                }
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(8f) })
        c.addView(row)
        val name = Sheets.input(ctx, mediaTitle, "课程名称", 40)
        name.addTextChangedListener(watcher { mediaTitle = it })
        c.addView(Sheets.field(ctx, "课程名称", name), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(12f) })
        val sorts = hrow(ctx)
        listOf("name" to "按名称", "time" to "按时间").forEach { (k, n) ->
            sorts.addView(chip(ctx, n, sortKey == k) { sortKey = k; rerender(false) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                if (sorts.childCount > 0) it.marginStart = ctx.dp(6f)
            })
        }
        sorts.addView(chip(ctx, if (sortAsc) "升序" else "降序", true) { sortAsc = !sortAsc; rerender(false) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = ctx.dp(12f) })
        c.addView(sorts, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        c.addView(iconGrid(ctx), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = ctx.dp(10f) })
        c.addView(noteBox(ctx, "保存到应用内", "默认会把文件复制一份到应用目录，下次打开免重选；关闭则每次打开需要重新连接文件夹。当前：${if (store.mediaKeep) "已开启" else "已关闭"}。"))
        c.getChildAt(c.childCount - 1).isClickable = true
        c.getChildAt(c.childCount - 1).setOnClickListener {
            store.mediaKeep = !store.mediaKeep; store.save(); rerender(false)
        }
    }
}

/**
 * 快捷入口：预览 + 列表（可拖顺序由编辑完成）+ 添加 / 编辑 / 删除。
 */
class PageShortcuts(act: Activity, app: App) : Page(act, app) {

    override val key = "shortcuts"
    override fun title() = "快捷入口"

    override fun body(host: LinearLayout) {
        ShortcutsEdit.page(act, app, host) { rerender(false) }
    }
}
