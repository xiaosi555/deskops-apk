package com.qwen.opsmentor.io
import com.qwen.opsmentor.data.toJson

import com.qwen.opsmentor.data.Chapter
import com.qwen.opsmentor.data.Cmd
import com.qwen.opsmentor.data.ConceptBlock
import com.qwen.opsmentor.data.Deep
import com.qwen.opsmentor.data.GuideBlock
import com.qwen.opsmentor.data.LongLecture
import com.qwen.opsmentor.data.Module
import com.qwen.opsmentor.data.Pack
import com.qwen.opsmentor.data.Pit
import com.qwen.opsmentor.core.MarkEngine
import com.qwen.opsmentor.data.PackRegistry
import com.qwen.opsmentor.data.Quiz
import com.qwen.opsmentor.data.StepBlock
import com.qwen.opsmentor.state.Bag
import com.qwen.opsmentor.state.CaseRec
import com.qwen.opsmentor.state.CoMeta
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.GlossRec
import com.qwen.opsmentor.state.LearnRec
import com.qwen.opsmentor.state.Mark
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.state.UserCourse
import org.json.JSONArray
import org.json.JSONObject

/** 解析结果：摘要行 + 是否完整课程档案 */
data class Parsed(
    val obj: JSONObject,
    val err: String = "",
    val scope: String = "all",
    val summary: List<Pair<String, String>> = emptyList(),
    val full: Boolean = false,
    val fileName: String = "",
)

object Importer {

    /** 识别 JSON / Markdown / CSV 三种课程或档案文本 */
    fun parse(raw: String, fileName: String = ""): Parsed {
        val t = raw.trim()
        if (t.isEmpty()) return Parsed(JSONObject(), "内容为空", fileName = fileName)
        if (t.startsWith("{") || t.startsWith("[")) {
            val o = try {
                if (t.startsWith("[")) {
                    // 支持直接导入课程对象数组，不要把 JSON 数组误交给 JSONObject 解析。
                    JSONObject().put("courses", JSONArray(t))
                } else {
                    val x = JSONObject(t)
                    if (x.has("courses") || x.has("app")) x
                    else if (x.length() == 1 && x.optJSONArray("array") != null) JSONObject().put("courses", x.getJSONArray("array"))
                    else JSONObject().put("courses", JSONArray().put(x))
                }
            } catch (e: Exception) {
                return Parsed(JSONObject(), "JSON 格式不正确：${e.message?.take(40) ?: "无法解析"}", fileName = fileName)
            }
            return fromArchive(o, fileName)
        }
        // Markdown / CSV 视作课程资料
        val o = if (t.contains("\n|") || t.lines().size > 1 && t.lines().first().split(",").size > 2)
            CsvKit.parseCourse(t) else MdKit.parseCourse(t)
        return fromArchive(o, fileName)
    }

    private fun fromArchive(o: JSONObject, fileName: String): Parsed {
        val scope = o.optString("scope", if (o.has("courses")) "all" else "course")
        val courses = o.optJSONArray("courses") ?: return Parsed(o, "没有可导入的课程（需要 title）", fileName = fileName)
        if (courses.length() == 0) return Parsed(o, "没有可导入的课程（需要 title）", fileName = fileName)
        val sum = ArrayList<Pair<String, String>>()
        sum.add("来源 app" to o.optString("app", "未知"))
        sum.add("档案版本" to "v" + o.optInt("ver", 1))
        sum.add("导出时间" to o.optString("exportedAt", "").take(19).replace("T", " "))
        sum.add("课程数" to courses.length().toString())
        val counts = o.optJSONObject("counts")
        if (counts != null) {
            sum.add("划线" to counts.optInt("marks").toString())
            sum.add("案例" to counts.optInt("cases").toString())
            sum.add("已学章节" to counts.optInt("learned").toString())
        }
        val c0 = courses.optJSONObject(0)
        if (c0 != null) {
            sum.add("课程名称" to c0.optString("title"))
            val st = c0.optJSONObject("state")
            if (st != null) {
                sum.add("划线" to (st.optJSONArray("marks")?.length() ?: 0).toString())
                sum.add("过关章节" to (st.optJSONObject("status")?.length() ?: 0).toString())
            }
            sum.add("章节目录" to (c0.optJSONArray("chapters")?.length() ?: 0).toString() + " 章")
        }
        return Parsed(o, "", scope, sum, isFullScope(o), fileName)
    }

    fun isFullScope(o: JSONObject): Boolean {
        val scope = o.optString("scope", "all")
        return scope == "all" || scope == "course"
    }

    /** 应用档案：merge 合并 / replace 覆盖 */
    fun apply(store: Store, packs: PackRegistry, o: JSONObject, mode: String): ApplyResult {
        val out = ApplyResult()
        val courses = o.optJSONArray("courses") ?: return out
        val scope = o.optString("scope", "all")
        for (i in 0 until courses.length()) {
            val c = courses.optJSONObject(i) ?: continue
            val title = c.optString("title").trim()
            if (title.isEmpty()) continue
            val id0 = c.optString("id").trim()
            val isBuiltin = Courses.builtin.any { it.id == id0 } && id0 != ""
            val id = when {
                isBuiltin && (scope == "all" || scope == "course") -> id0
                else -> if (mode == "replace" && id0.isNotEmpty()) id0 else pickId(store, id0)
            }
            // 课程资料
            if ((scope == "all" || scope == "course") && (c.has("chapters") || c.has("ch") || c.has("handbook"))) {
                val pack = packOfJson(c)
                if (isBuiltin && id0 == Courses.OPS) {
                    // 内置课程：不覆盖内置数据，仅登记元信息与状态
                    packs.evict(id0)
                } else {
                    packs.put(id, pack)
                    val uc = store.userCourses.firstOrNull { it.id == id } ?: UserCourse(id, title,
                        c.optString("subtitle"), c.optString("description"), c.optString("glyph", "book"), c.optString("media")).also { store.userCourses.add(it) }
                    uc.title = title
                    uc.sub = c.optString("subtitle")
                    uc.desc = c.optString("description")
                    uc.glyph = c.optString("glyph", uc.glyph)
                    uc.media = c.optString("media", uc.media)
                    uc.packJson = pack.toJson().toString()
                }
                out.created += id
            }
            // 元信息覆盖
            val meta = CoMeta.from(c)
            if (meta.title.isNotEmpty() || meta.sub.isNotEmpty() || meta.cover.isNotEmpty()) {
                store.co[id] = meta
            }
            // 学习状态
            c.optJSONObject("state")?.let { st -> applyState(store, packs, id, st, mode, out) }
        }
        // 个人设置
        o.optString("theme").takeIf { it.isNotEmpty() }?.let { store.theme = it }
        o.optJSONObject("personal")?.optJSONObject("ui")?.let { applyUi(store, it) }
        o.optJSONObject("ui")?.let { applyUi(store, it) }
        store.save()
        return out
    }

    private fun pickId(store: Store, base: String): String {
        var id = base.ifBlank { "import_" + System.currentTimeMillis() }
        var n = 1
        while (Courses.all(store).any { it.id == id } || store.userCourses.any { it.id == id }) {
            id = base.ifBlank { "import_" + System.currentTimeMillis() } + "_" + n
            n++
        }
        return id
    }

    private fun applyState(store: Store, packs: PackRegistry, courseId: String, st: JSONObject, mode: String, out: ApplyResult) {
        val b = if (mode == "replace") Bag.newBag().also { store.bags[courseId] = it } else store.bags.getOrPut(courseId) { Bag.newBag() }
        st.optString("route").takeIf { it.isNotEmpty() }?.let { b.route = it }
        if (st.has("idx")) b.idx = st.optInt("idx")
        if (st.has("day")) b.day = st.optInt("day", 1)
        st.optString("mode").takeIf { it == "long" || it == "lite" }?.let { b.mode = it }
        b.pin = st.optString("pin", b.pin)
        st.optJSONObject("status")?.let { s ->
            s.keys().forEach { k ->
                val v = s.optString(k)
                if (v.isNotEmpty()) { b.status[k] = v; out.done += 1 }
            }
        }
        st.optJSONObject("learn")?.let { l ->
            l.keys().forEach { k ->
                l.optJSONObject(k)?.let { rec ->
                    val old = b.learn[k]
                    val merged = if (old != null && mode == "merge") {
                        val done = ArrayList(old.done); rec.optJSONArray("done")?.let { a -> for (j in 0 until a.length()) if (!done.contains(a.optInt(j))) done.add(a.optInt(j)) }
                        LearnRec(old.at.ifBlank { Store.today() }, done)
                    } else LearnRec.from(rec)
                    b.learn[k] = merged
                }
            }
        }
        st.optJSONObject("studyLog")?.let { log -> log.keys().forEach { k -> store.studyLog[k] = log.optInt(k) } }
        st.optJSONArray("marks")?.let { arr ->
            val incoming = ArrayList<Mark>()
            for (i in 0 until arr.length()) {
                val m = Mark.from(arr.getJSONObject(i))
                m.courseId = courseId
                incoming.add(m)
            }
            if (mode == "replace") {
                b.marks = incoming
                out.marks += incoming.size
            } else {
                incoming.forEach { m ->
                    val dup = b.marks.any { old -> MarkEngine.fingerprint(old) == MarkEngine.fingerprint(m) }
                    if (!dup) { b.marks.add(m); out.marks += 1 }
                }
            }
        }
        st.optJSONArray("cases")?.let { a ->
            val kept = if (mode == "replace") ArrayList() else ArrayList(b.cases)
            for (i in 0 until a.length()) {
                val c = CaseRec.from(a.getJSONObject(i))
                if (kept.none { it.no == c.no }) kept.add(c) else kept.indexOfFirst { it.no == c.no }.let { kept[it] = c }
            }
            b.cases = kept
        }
        st.optJSONArray("gloss")?.let { a ->
            val kept = if (mode == "replace") ArrayList() else ArrayList(b.gloss)
            for (i in 0 until a.length()) {
                val g = GlossRec.from(a.getJSONObject(i))
                if (kept.none { it.term == g.term && it.full == g.full }) kept.add(g)
            }
            b.gloss = kept
        }
        out.learned += b.learn.size
    }

    private fun applyUi(store: Store, u: JSONObject) {
        u.optString("style").takeIf { it.isNotEmpty() }?.let { store.style = if (it == "minimal") com.qwen.opsmentor.state.StyleMode.MINIMAL else com.qwen.opsmentor.state.StyleMode.PLANET }
        if (u.has("accent")) store.accent = u.optInt("accent").coerceIn(0, 8)
        if (u.has("accentCustom")) store.accentCustom = u.optString("accentCustom")
        if (u.has("accentPaletteVersion")) store.accentPaletteVersion = u.optInt("accentPaletteVersion", 1)
        when (u.optString("appearance", "")) {
            "light" -> store.appearance = com.qwen.opsmentor.state.Appearance.LIGHT
            "dark" -> store.appearance = com.qwen.opsmentor.state.Appearance.DARK
            "system" -> store.appearance = com.qwen.opsmentor.state.Appearance.SYSTEM
        }
        u.optString("deco").takeIf { it.isNotEmpty() }?.let { store.deco = it }
        u.optString("cmTab").takeIf { it.isNotEmpty() }?.let { store.cmTab = it }
        if (u.has("ttsRate")) store.ttsRate = u.optDouble("ttsRate", 1.0).toFloat()
        u.optJSONObject("profile")?.let { p ->
            store.profileName = p.optString("name", store.profileName)
            store.profileBio = p.optString("bio", store.profileBio)
        }
        u.optJSONObject("star")?.let { s -> s.keys().forEach { k -> store.star[k] = s.optInt(k, 1) } }
        u.optJSONObject("co")?.let { c -> c.keys().forEach { k -> c.optJSONObject(k)?.let { store.co[k] = CoMeta.from(it) } } }
        u.optJSONArray("deletedCourses")?.let { a -> for (i in 0 until a.length()) if (!store.deletedCourses.contains(a.optString(i))) store.deletedCourses.add(a.optString(i)) }
        u.optJSONObject("readPos")?.let { a -> a.keys().forEach { k -> store.readPos[k] = a.optInt(k) } }
        u.optJSONObject("mediaPos")?.let { a -> a.keys().forEach { k -> store.mediaPos[k] = a.optLong(k) } }
        if (u.has("mediaKeep")) store.mediaKeep = u.optBoolean("mediaKeep", true)
        if (u.has("topInset")) store.topInset = u.optBoolean("topInset", true)
        u.optJSONObject("bar")?.let { bar -> store.barX = bar.optDouble("x", -1.0).toFloat(); store.barY = bar.optDouble("y", -1.0).toFloat() }
        u.optString("ttsVoice").takeIf { it.isNotEmpty() }?.let { store.ttsVoice = it }
        u.optJSONArray("shortcuts")?.let { a ->
            val list = ArrayList<com.qwen.opsmentor.state.Shortcut>()
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { list.add(com.qwen.opsmentor.state.Shortcut.from(it)) }
            if (list.isNotEmpty()) store.shortcuts = list
        }
        u.optJSONArray("userCourses")?.let { arr ->
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { j ->
                val uc = UserCourse.from(j)
                if (store.userCourses.none { it.id == uc.id }) store.userCourses.add(uc)
            }
        }
        u.optJSONObject("studyLog")?.let { log -> log.keys().forEach { k -> store.studyLog[k] = log.optInt(k) } }
    }

    private fun packOfJson(c: JSONObject): Pack = Pack.from(c)

    class ApplyResult {
        val created = ArrayList<String>()
        var marks = 0
        var done = 0
        var learned = 0
        fun summaryText(): String = buildString {
            if (created.isNotEmpty()) append("新增 ${created.size} 门课程；")
            append("划线 +$marks；已过关 $done 章；累计已学 $learned 章")
        }
    }

    /** Markdown 简易课程：只用标题层级 */
    object MdKit {
        fun parseCourse(md: String): JSONObject {
            val lines = md.split("\n")
            val courses = ArrayList<MutableMap<String, Any>>()
            var cur: MutableMap<String, Any>? = null
            var ch: MutableMap<String, Any>? = null
            var buf = ArrayList<String>()
            fun flushCh() {
                if (ch != null && cur != null) {
                    val body = buf.joinToString("\n").trim()
                    val c = ch!!
                    if (c["content"] == null || (c["content"] as String).isBlank()) c["content"] = body
                    (cur!!["chapters"] as ArrayList<MutableMap<String, Any>>).add(c)
                }
                buf = ArrayList()
            }
            fun flushCo() {
                flushCh()
                ch = null
                if (cur != null) courses.add(cur!!)
            }
            lines.forEach { raw ->
                val l = raw.trimEnd()
                when {
                    l.startsWith("# ") -> {
                        flushCo()
                        cur = linkedMapOf("title" to l.substring(2).trim(), "chapters" to ArrayList<MutableMap<String, Any>>())
                    }
                    l.startsWith("## ") -> {
                        flushCh()
                        if (cur == null) cur = linkedMapOf("title" to "导入课程", "chapters" to ArrayList<MutableMap<String, Any>>())
                        ch = linkedMapOf("t" to l.substring(3).trim(), "chapters" to ArrayList<MutableMap<String, Any>>())
                    }
                    l.isNotEmpty() -> buf.add(l)
                }
            }
            flushCo()
            val arr = JSONArray()
            courses.forEach { item ->
                val o = JSONObject()
                o.put("title", item["title"])
                o.put("subtitle", "")
                val arr2 = JSONArray()
                (item["chapters"] as List<*>).forEach { h -> arr2.put(JSONObject(h as Map<*, *>)) }
                o.put("chapters", arr2)
                arr.put(o)
            }
            return JSONObject().put("app", Store.AR_APP).put("kind", "portable-archive").put("ver", Store.AR_VER).put("scope", "course").put("courses", arr)
        }
    }

    /** CSV：完整写法有 json 列；简易写法 id,title,content */
    object CsvKit {
        fun parseCourse(text: String): JSONObject {
            val rows = splitCsv(text)
            if (rows.isEmpty()) return JSONObject().put("courses", JSONArray())
            val head = rows.first().map { it.trim() }
            val arr = JSONArray()
            val chapters = JSONArray()
            rows.drop(1).forEach { r ->
                val o = JSONObject()
                head.forEachIndexed { i, h -> if (i < r.size) o.put(h, r[i]) }
                if (o.has("json")) {
                    val raw = o.optString("json")
                    if (raw.trim().startsWith("{")) {
                        val parsed = try { JSONObject(raw) } catch (e: Exception) { null }
                        if (parsed != null) {
                            if (parsed.has("title")) { arr.put(parsed); return@forEach }
                            chapters.put(parsed); return@forEach
                        }
                    }
                }
                val co = JSONObject()
                co.put("id", o.optString("id"))
                co.put("t", o.optString("title", o.optString("t")))
                co.put("s", o.optString("summary", o.optString("s")))
                co.put("content", o.optString("content", ""))
                co.put("gate", o.optString("gate", "能用自己的话讲清本章内容"))
                chapters.put(co)
            }
            val c = JSONObject().put("title", "导入课程").put("chapters", chapters)
            arr.put(if (arr.length() == 0) c else arr.getJSONObject(0))
            return JSONObject().put("app", Store.AR_APP).put("kind", "portable-archive").put("ver", Store.AR_VER).put("scope", "course").put("courses", arr)
        }

        fun splitCsv(text: String): List<List<String>> {
            val rows = ArrayList<List<String>>()
            var field = StringBuilder(); var row = ArrayList<String>()
            var quoted = false; var i = 0
            while (i < text.length) {
                val c = text[i]
                when {
                    quoted -> {
                        if (c == '"') {
                            if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ } else quoted = false
                        } else field.append(c)
                    }
                    c == '"' -> quoted = true
                    c == ',' -> { row.add(field.toString()); field = StringBuilder() }
                    c == '\n' || c == '\r' -> {
                        if (field.isNotEmpty() || row.isNotEmpty()) {
                            row.add(field.toString()); field = StringBuilder()
                            if (row.any { it.isNotBlank() }) rows.add(row)
                            row = ArrayList()
                        }
                        if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    }
                    else -> field.append(c)
                }
                i++
            }
            if (field.isNotEmpty() || row.isNotEmpty()) {
                row.add(field.toString())
                if (row.any { it.isNotBlank() }) rows.add(row)
            }
            return rows
        }
    }
}

private fun org.json.JSONArray.joinToString(separator: String): String =
    (0 until length()).joinToString(separator) { optString(it) }
