package com.qwen.opsmentor.io
import com.qwen.opsmentor.data.toJson

import com.qwen.opsmentor.data.Lecture
import com.qwen.opsmentor.data.Pack
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.GlossRec
import com.qwen.opsmentor.state.LineType
import com.qwen.opsmentor.state.Mark
import com.qwen.opsmentor.state.MarkColor
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.state.StyleMode
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/** 档案条目（与 Web IO_ENTRIES 对齐） */
data class IoEntry(val id: String, val title: String, val desc: String, val icon: String, val name: String)

object Exporter {

    val ENTRIES = listOf(
        IoEntry("all", "整门课程 · 完整档案", "课程资料、学习手册、划线、进度、案例、术语与个人设置，一次带走", "database", "完整档案"),
        IoEntry("course", "课程资料", "课程名称、简介、封面与章节目录（含自建、导入的课程）", "book", "课程资料"),
        IoEntry("handbook", "学习手册", "全部章节的概念、实操、指南与互动题", "bookOpen", "学习手册"),
        IoEntry("marks", "划线与备注", "划线原文、线型颜色、标签、备注与时间", "highlighter", "划线与备注"),
        IoEntry("progress", "学习进度", "章节过关状态、学习日期、当前路线与学习时长", "chart", "学习进度"),
        IoEntry("cases", "案例与术语", "故障案例库（五栏）与自填术语手册", "folder", "案例与术语"),
    )

    val FORMATS = listOf(Triple("json", "JSON", "完整结构"), Triple("md", "Markdown", "可读文档"), Triple("csv", "CSV", "一行一条"))
    val EXT = mapOf("json" to ("json" to "JSON 存档"), "md" to ("md" to "Markdown 文档"), "csv" to ("csv" to "CSV 表格"))

    fun entry(id: String?) = ENTRIES.firstOrNull { it.id == id }

    // ---------------- 学习手册 Markdown ----------------

    fun manualMd(store: Store, pack: Pack, title: String): String {
        val out = ArrayList<String>()
        out.add("# $title · 学习手册　${pack.ch.size} 章 · 概念 / 实操 / 指南 / 互动题")
        out.add("")
        out.add("> 导出时间：${nowText()}　来源：$title")
        out.add("")
        out.add("## 目录")
        pack.ch.forEach { out.add("- **${it.id}** ${it.t}　—　${it.s}") }
        out.add("")
        out.add("---")
        pack.mods.forEach { m ->
            out.add("")
            out.add("## ${m.n}　（${m.tag}）")
            pack.ch.filter { it.m == m.k }.forEach { c ->
                val d = Lecture.of(pack, c)
                out.add("")
                out.add("### ${c.id}　${c.t}")
                out.add("")
                out.add("> 本节解决：${c.s}")
                if (c.plain.isNotEmpty()) {
                    out.add("")
                    out.add("**双轨对照**")
                    out.add("| 专业术语 | 大白话 |")
                    out.add("| --- | --- |")
                    c.plain.forEach { out.add("| ${it.first} | ${it.second} |") }
                }
                out.add("")
                out.add("#### 一、概念")
                if (d.title.isNotEmpty()) {
                    out.add("")
                    out.add("**${d.title}**")
                }
                d.concept.forEach { b ->
                    if (b.p.isNotEmpty()) { out.add(""); out.add(b.p) }
                    if (b.ul.isNotEmpty()) b.ul.forEach { out.add("- $it") }
                    if (b.isTable) {
                        out.add("")
                        out.add("| " + b.head.joinToString(" | ") + " |")
                        out.add("| " + b.head.joinToString(" | ") { "---" } + " |")
                        b.rows.forEach { out.add("| " + it.joinToString(" | ") + " |") }
                    }
                    if (b.md.isNotEmpty()) { out.add(""); out.add(b.md) }
                    if (b.parts.isNotEmpty()) { out.add(""); out.add(MarkdownKit.plain(b.parts)) }
                }
                out.add("")
                out.add("#### 二、实操")
                d.practice.forEachIndexed { i, st ->
                    out.add("")
                    out.add("**${i + 1}. ${st.t}**")
                    if (st.d.isNotEmpty()) { out.add(""); out.add(st.d) }
                    if (st.md.isNotEmpty()) { out.add(""); out.add(st.md) }
                    if (st.cmd.isNotEmpty()) {
                        out.add("")
                        out.add("```")
                        out.add("  " + st.cmd)
                        out.add("```")
                    }
                    if (st.out.isNotEmpty()) {
                        out.add("")
                        out.add("预期输出")
                        out.add("")
                        out.add("```")
                        out.add("  " + st.out.replace("\n", "\n  "))
                        out.add("```")
                    }
                    if (st.read.isNotEmpty()) { out.add(""); out.add("怎么读：" + st.read) }
                }
                out.add("")
                out.add("#### 三、指南")
                d.guide.forEachIndexed { i, g ->
                    if (g.w.isNotEmpty() || g.c.isNotEmpty() || g.r.isNotEmpty()) {
                        out.add("")
                        out.add("**${i + 1}. 错误做法：${g.w}**")
                        out.add("- 后果：${g.c}")
                        out.add("- 正确：${g.r}")
                    } else {
                        out.add("")
                        out.add("**${i + 1}. ${g.t}**")
                        if (g.d.isNotEmpty()) out.add(g.d)
                    }
                }
                out.add("")
                out.add("#### 四、互动题")
                out.add("")
                out.add(d.quiz.q)
                out.add("")
                if (d.quiz.hint.isNotEmpty()) {
                    out.add("> 提示：${d.quiz.hint}")
                    out.add("")
                }
                out.add("参考答案：${d.quiz.a}")
                out.add("")
                out.add("**过关标准**：${c.gate}")
            }
        }
        return out.joinToString("\n")
    }

    private fun nowText(): String = com.qwen.opsmentor.core.Sheets.date(System.currentTimeMillis())

    // ---------------- 划线 TXT ----------------

    fun marksText(store: Store, pack: Pack, list: List<Mark>): String {
        val out = ArrayList<String>()
        out.add("桌面运维学习 · 我的划线与备注　" + com.qwen.opsmentor.core.Sheets.date(System.currentTimeMillis()))
        out.add("共 ${list.size} 处")
        out.add("")
        out.add("════════════════")
        val byCh = LinkedHashMap<String, ArrayList<Mark>>()
        list.forEach { m -> byCh.getOrPut(m.ch) { ArrayList() }.add(m) }
        byCh.forEach { (chk, items) ->
            val c0 = pack.chById(chk)
            out.add("")
            out.add("【$chk ${c0?.t ?: ""}】")
            items.forEachIndexed { i, m ->
                val extra = if (m.segs.size > 1) "　跨${m.segs.size}段" else ""
                out.add("${i + 1}. 线型：${LineType.of(m.ty).label}　颜色：${MarkColor.of(m.co).label}" +
                        (if (m.tags.isNotEmpty()) "　标签：${m.tags.joinToString("/")}" else "") + extra)
                out.add("   原文：" + m.segs.joinToString(" ／ ") { it.txt }.ifBlank { m.t })
                out.add("   备注：" + (m.n.ifBlank { "（无）" }))
                out.add("   时间：" + com.qwen.opsmentor.core.Sheets.date(m.at))
                out.add("")
            }
        }
        return out.joinToString("\n")
    }

    // ---------------- 案例 / 术语 ----------------

    fun casesText(cases: List<com.qwen.opsmentor.state.CaseRec>): String {
        val out = ArrayList<String>()
        out.add("故障案例库（五栏）　导出 " + com.qwen.opsmentor.core.Sheets.date(System.currentTimeMillis()))
        out.add("")
        cases.forEach {
            out.add("编号：" + it.no)
            out.add("现象：" + it.sym)
            out.add("排查路径：" + it.path)
            out.add("根因：" + it.cause)
            out.add("结论（下次第一步查什么）：" + it.lesson)
            out.add("关联章节：" + it.ch)
            out.add("")
            out.add("────────")
            out.add("")
        }
        return out.joinToString("\n")
    }

    fun csvOf(rows: List<List<String>>): String =
        rows.joinToString("\r\n") { r -> r.joinToString(",") { "\"" + (it ?: "").replace("\"", "\"\"") + "\"" } }

    fun casesCsv(cases: List<com.qwen.opsmentor.state.CaseRec>): String {
        val rows = ArrayList<List<String>>()
        rows.add(listOf("编号", "现象", "排查路径", "根因", "结论（下次第一步）", "关联章节"))
        cases.forEach { rows.add(listOf(it.no, it.sym, it.path.replace("\n", " / "), it.cause, it.lesson, it.ch)) }
        return csvOf(rows)
    }

    fun glossCsv(store: Store, pack: Pack): String {
        val rows = ArrayList<List<String>>()
        rows.add(listOf("术语", "全称", "大白话", "来源"))
        store.gloss.forEach { rows.add(listOf(it.term, it.full, it.say, "自填")) }
        baseGloss(pack).forEach { rows.add(listOf(it.term, it.full, it.say, "章节自动")) }
        return csvOf(rows)
    }

    /** 章节双轨对照自动导入的术语底表 */
    fun baseGloss(pack: Pack): List<GlossRec> {
        val out = ArrayList<GlossRec>()
        pack.ch.forEach { c ->
            c.plain.forEach { out.add(GlossRec(it.first, c.id + " " + c.t, it.second, "auto")) }
        }
        return out
    }

    // ---------------- 存档 JSON ----------------

    fun backupJson(store: Store, packs: com.qwen.opsmentor.data.PackRegistry): String = makeArchive(store, packs, "all").put("kind", "backup").toString(1)

    /** 与 Web archiveState 同结构 */
    fun stateJson(store: Store, scope: String, courseId: String = store.cur): JSONObject {
        val b = store.bag(courseId)
        val o = JSONObject()
        fun full() {
            o.put("route", b.route).put("idx", b.idx).put("day", b.day).put("mode", b.mode)
                .put("pin", b.pin).put("status", JSONObject(b.status as Map<*, *>))
                .put("learn", JSONObject().apply { b.learn.forEach { (k, v) -> put(k, v.toJson()) } })
                .put("studyLog", JSONObject(store.studyLog as Map<*, *>))
        }
        when (scope) {
            "marks" -> o.put("marks", JSONArray(store.marks.map { it.toJson() }))
            "progress" -> full()
            "cases" -> {
                o.put("cases", JSONArray(b.cases.map { it.toJson() }))
                o.put("gloss", JSONArray(b.gloss.map { it.toJson() }))
            }
            else -> {
                full()
                o.put("cases", JSONArray(b.cases.map { it.toJson() }))
                o.put("gloss", JSONArray(b.gloss.map { it.toJson() }))
                o.put("marks", JSONArray(b.marks.map { it.toJson() }))
            }
        }
        return o
    }

    /** 完整档案：app / kind / ver / scope / exportedAt / counts / courses[] */
    private fun makeCourseObject(store: Store, packs: com.qwen.opsmentor.data.PackRegistry, co: Courses.Entry, scope: String): JSONObject {
        val pack = packs.of(store, co.id)
        val (title, sub, cover) = Courses.meta(store, co)
        val c = JSONObject()
        c.put("id", co.id).put("title", title).put("subtitle", sub).put("description", co.desc).put("cover", cover)
            .put("glyph", Courses.glyph(store, co)).put("media", co.media)
        if (scope == "all" || scope == "course") {
            c.put("modules", JSONArray(pack.mods.map { JSONObject().put("k", it.k).put("n", it.n).put("tag", it.tag) }))
            c.put("chapters", JSONArray(pack.ch.map { ch ->
                JSONObject().apply {
                    put("id", ch.id); put("m", ch.m); put("t", ch.t); put("s", ch.s); put("terms", ch.terms); put("gate", ch.gate)
                    put("plain", JSONArray(ch.plain.map { JSONArray(listOf(it.first, it.second)) }))
                    put("cmds", JSONArray(ch.cmds.map { JSONObject().put("c", it.c).put("n", it.n).put("lv", it.lv) }))
                    put("pits", JSONArray(ch.pits.map { JSONObject().put("w", it.w).put("c", it.c).put("r", it.r) }))
                    put("quiz", JSONObject().put("q", ch.quiz.q).put("a", ch.quiz.a).put("hint", ch.quiz.hint))
                }
            }))
            c.put("deep", JSONObject().apply {
                pack.deep.forEach { (k, v) ->
                    put(k, JSONObject().apply {
                        put("deep", JSONArray(v.deep.map { JSONArray(listOf(it.first, it.second)) }))
                        put("out", JSONArray(v.out.map { JSONArray(listOf(it.first, it.second)) }))
                        put("flow", JSONArray(v.flow.map { JSONArray(listOf(it.first, it.second)) }))
                    })
                }
            })
            c.put("long", JSONObject().apply { pack.long.forEach { (k, v) -> put(k, v.toJson()) } })
            c.put("routes", JSONObject().apply { pack.routes.forEach { (k, v) -> put(k, JSONArray(v)) } })
            c.put("routeNames", JSONObject().apply { pack.routeNames.forEach { (k, v) -> put(k, v) } })
            c.put("depText", pack.depText)
            c.put("depNotes", JSONArray(pack.depNotes.map { JSONArray(listOf(it.first, it.second)) }))
            c.put("layers", JSONArray(pack.layers.map { JSONArray(listOf(it.first, it.second)) }))
            c.put("cmds", JSONArray(pack.cmds.map { JSONArray(it) }))
            c.put("events", JSONArray(pack.events.map { JSONArray(it) }))
            c.put("scen", JSONArray(pack.scen.map { JSONObject().put("t", it.t).put("p", it.p).put("steps", JSONArray(it.steps)).put("c", it.c).put("f", it.f) }))
            c.put("lab", JSONArray(pack.lab.map { JSONArray(listOf(it.first, it.second)) }))
        }
        if (scope == "all" || scope == "course" || scope == "handbook") c.put("handbook", manualMd(store, pack, title))
        val st = stateJson(store, scope, co.id)
        if (st.length() > 0) c.put("state", st)
        return c
    }

    /** 完整档案；all 会包含每一门课程及其独立学习数据。 */
    fun makeArchive(store: Store, packs: com.qwen.opsmentor.data.PackRegistry, scope: String): JSONObject {
        val selected = if (scope == "all") Courses.all(store) else listOf(Courses.find(store, store.cur))
        val courses = JSONArray()
        selected.forEach { co -> courses.put(makeCourseObject(store, packs, co, scope)) }
        val marks = selected.sumOf { store.bag(it.id).marks.size }
        val cases = selected.sumOf { store.bag(it.id).cases.size }
        val gloss = selected.sumOf { store.bag(it.id).gloss.size }
        val learned = selected.sumOf { store.bag(it.id).learn.size }
        return JSONObject().put("app", Store.AR_APP).put("kind", "portable-archive")
            .put("ver", Store.AR_VER).put("scope", scope).put("exportedAt", ISO.format(Date()))
            .put("counts", JSONObject().put("marks", marks).put("cases", cases).put("gloss", gloss).put("learned", learned))
            .put("courses", courses).put("personal", if (scope == "all") personalObj(store) else JSONObject())
    }

    /** 个人数据：整套状态导出（不含媒体文件本体） */
    fun personalJson(store: Store, packs: com.qwen.opsmentor.data.PackRegistry): String = makeArchive(store, packs, "all").put("kind", "personal-backup").toString(1)

    private fun personalObj(store: Store): JSONObject {
        val u = JSONObject()
        u.put("style", if (store.style == StyleMode.MINIMAL) "minimal" else "planet")
        u.put("accent", store.accent)
        u.put("accentCustom", store.accentCustom)
        u.put("accentPaletteVersion", store.accentPaletteVersion)
        u.put("appearance", store.appearance.name.lowercase())
        u.put("deco", store.deco)
        u.put("profile", JSONObject().put("name", store.profileName).put("bio", store.profileBio).put("avatar", ""))
        u.put("shortcuts", JSONArray(store.shortcuts.map { it.toJson() }))
        u.put("star", JSONObject(store.star as Map<*, *>))
        u.put("cmTab", store.cmTab)
        u.put("ttsRate", store.ttsRate.toDouble())
        u.put("ttsVoice", store.ttsVoice)
        u.put("userCourses", JSONArray(store.userCourses.map { it.toJson() }))
        u.put("deletedCourses", JSONArray(store.deletedCourses))
        u.put("co", JSONObject().apply { store.co.forEach { (k, v) -> put(k, v.toJson()) } })
        u.put("readPos", JSONObject(store.readPos as Map<*, *>))
        u.put("studyLog", JSONObject(store.studyLog as Map<*, *>))
        u.put("mediaPos", JSONObject(store.mediaPos as Map<*, *>))
        u.put("mediaKeep", store.mediaKeep)
        u.put("topInset", store.topInset)
        u.put("bar", JSONObject().put("x", store.barX.toDouble()).put("y", store.barY.toDouble()))
        return JSONObject().put("ui", u).put("theme", store.theme)
    }

    fun archiveText(store: Store, packs: com.qwen.opsmentor.data.PackRegistry, scope: String, format: String): Pair<String, String> {
        val e = entry(scope) ?: ENTRIES.first()
        val o = makeArchive(store, packs, scope)
        val c = o.getJSONArray("courses").getJSONObject(0)
        val title = c.optString("title")
        val name = "$title-${e.name}-${Store.today()}." + EXT[format]!!.first
        val text = when (format) {
            "json" -> o.toString(1)
            "md" -> archiveMd(o, scope)
            else -> archiveCsv(o, scope)
        }
        return Pair(name, text)
    }

    /** 档案 → Markdown（一行一条可读） */
    fun archiveMd(o: JSONObject, scope: String): String {
        val c = o.optJSONArray("courses")?.optJSONObject(0) ?: return ""
        val out = ArrayList<String>()
        out.add("# " + c.optString("title") + " · " + (entry(scope)?.name ?: "档案"))
        out.add("")
        out.add("> 导出时间：" + com.qwen.opsmentor.core.Sheets.date(System.currentTimeMillis()) + "　app=" + o.optString("app") + "　ver=" + o.optInt("ver"))
        if (c.optString("subtitle").isNotEmpty()) {
            out.add("")
            out.add(c.optString("subtitle"))
        }
        if (scope == "handbook" || scope == "all") {
            val hb = c.optString("handbook")
            if (hb.isNotEmpty()) { out.add(""); out.add(hb); return out.joinToString("\n") }
        }
        c.optJSONArray("chapters")?.let { a ->
            out.add(""); out.add("## 章节目录")
            for (i in 0 until a.length()) {
                val ch = a.getJSONObject(i)
                out.add("- **${ch.optString("id")}** ${ch.optString("t")}　—　${ch.optString("s")}")
            }
        }
        c.optJSONObject("state")?.let { st ->
            out.add(""); out.add("## 学习数据")
            st.optJSONObject("status")?.let { s ->
                out.add(""); out.add("### 过关状态")
                s.keys().forEach { k -> out.add("- $k = ${s.optString(k)}") }
            }
            st.optJSONArray("marks")?.let { a ->
                out.add(""); out.add("### 划线 ${a.length()} 处")
                for (i in 0 until a.length()) {
                    val m = a.getJSONObject(i)
                    out.add("- [${m.optString("ch")}] ${JSONArrayText.segs(m)}｜${m.optString("n")}")
                }
            }
            st.optJSONArray("cases")?.let { a ->
                out.add(""); out.add("### 案例 ${a.length()} 条")
                for (i in 0 until a.length()) {
                    val x = a.getJSONObject(i)
                    out.add("- ${x.optString("no")} ${x.optString("sym")}｜根因：${x.optString("cause")}")
                }
            }
            st.optJSONArray("gloss")?.let { a ->
                out.add(""); out.add("### 自填术语 ${a.length()} 条")
                for (i in 0 until a.length()) {
                    val g = a.getJSONObject(i)
                    out.add("- ${g.optString("term")} = ${g.optString("say")}")
                }
            }
        }
        return out.joinToString("\n")
    }

    /** 档案 → CSV */
    fun archiveCsv(o: JSONObject, scope: String): String {
        val c = o.optJSONArray("courses")?.optJSONObject(0) ?: return ""
        val rows = ArrayList<List<String>>()
        val st = c.optJSONObject("state")
        if (st?.optJSONArray("marks") != null) {
            rows.add(listOf("章节", "线型", "颜色", "标签", "备注", "原文", "时间"))
            val a = st.getJSONArray("marks")
            for (i in 0 until a.length()) {
                val m = a.getJSONObject(i)
                rows.add(listOf(
                    chTitle(c, m.optString("ch")), m.optString("ty"), m.optString("co"),
                    m.optJSONArray("tags")?.joinToString("/") ?: "", m.optString("n"),
                    JSONArrayText.segs(m), m.optLong("at").let { com.qwen.opsmentor.core.Sheets.date(it) },
                ))
            }
        } else if (st?.optJSONArray("cases") != null) {
            rows.add(listOf("编号", "现象", "排查路径", "根因", "结论（下次第一步）", "关联章节"))
            val a = st.getJSONArray("cases")
            for (i in 0 until a.length()) {
                val x = a.getJSONObject(i)
                rows.add(listOf(x.optString("no"), x.optString("sym"), x.optString("path").replace("\n", " / "),
                    x.optString("cause"), x.optString("lesson"), x.optString("ch")))
            }
        } else if (st?.optJSONObject("status") != null) {
            rows.add(listOf("章节", "状态", "首学日期", "已完成复习档"))
            val s = st.getJSONObject("status"); val learn = st.optJSONObject("learn") ?: JSONObject()
            val a = c.optJSONArray("chapters") ?: JSONArray()
            for (i in 0 until a.length()) {
                val ch = a.getJSONObject(i); val id = ch.optString("id")
                val rec = learn.optJSONObject(id)
                rows.add(listOf(chTitle(c, id), s.optString(id), rec?.optString("at") ?: "",
                    rec?.optJSONArray("done")?.joinToString("/") ?: ""))
            }
        } else {
            rows.add(listOf("章节", "模块", "标题", "本节解决", "过关标准"))
            val a = c.optJSONArray("chapters") ?: JSONArray()
            for (i in 0 until a.length()) {
                val ch = a.getJSONObject(i)
                rows.add(listOf(ch.optString("id"), ch.optString("m"), ch.optString("t"), ch.optString("s"), ch.optString("gate")))
            }
        }
        return csvOf(rows)
    }

    private fun chTitle(c: JSONObject, id: String): String {
        val a = c.optJSONArray("chapters") ?: return id
        for (i in 0 until a.length()) {
            val ch = a.getJSONObject(i)
            if (ch.optString("id") == id) return "$id ${ch.optString("t")}"
        }
        return id
    }

    private object JSONArrayText {
        fun segs(m: JSONObject): String {
            val a = m.optJSONArray("segs")
            if (a == null) return m.optString("t")
            val out = ArrayList<String>()
            for (i in 0 until a.length()) out.add(a.getJSONObject(i).optString("txt"))
            return out.joinToString(" ／ ").ifBlank { m.optString("t") }
        }
    }

    private val ISO = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }

    fun fmtSize(n: Long): String = com.qwen.opsmentor.core.Sheets.fmtSizeLong(n)
}


private object MarkdownKit {
    fun plain(parts: List<com.qwen.opsmentor.data.Part>): String = parts.joinToString("") { it.text }
}
private fun org.json.JSONArray.joinToString(separator: String): String =
    (0 until length()).joinToString(separator) { optString(it) }
