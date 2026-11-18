package com.qwen.opsmentor.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 课程内容包 pack —— 与 Web 版 §7.2 逐字段对齐。
 * 任何字段缺失都走容错默认，绝不让页面崩溃。
 */
class Pack(
    val mods: List<Module>,
    val ch: List<Chapter>,
    val deep: Map<String, Deep>,
    val long: Map<String, LongLecture>,
    val layers: List<Pair<String, String>>,
    val cmds: List<List<String>>,
    val events: List<List<String>>,
    val scen: List<Scenario>,
    val lab: List<Pair<String, String>>,
    val routes: LinkedHashMap<String, List<String>>,
    val routeNames: LinkedHashMap<String, String>,
    val intervals: List<Int>,
    val depText: String,
    val depNotes: List<Pair<String, String>>,
) {
    val byId: Map<String, Chapter> = ch.associateBy { it.id }
    val moduleOf: Map<String, Module> = mods.associateBy { it.k }
    val knowTotal: Int = deep.values.sumOf { it.deep.size }
    val outTotal: Int = deep.values.sumOf { it.out.size }

    fun chById(id: String?): Chapter? = id?.let { byId[it] }
    fun route(key: String?): List<String> =
        routes[key] ?: routes.values.firstOrNull() ?: ch.map { it.id }
    fun routeName(key: String?): String = routeNames[key] ?: "全章节顺序"

    companion object {
        fun empty() = Pack(
            emptyList(), emptyList(), emptyMap(), emptyMap(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), linkedMapOf("all" to emptyList()),
            linkedMapOf("all" to "全章节顺序"), listOf(2, 4, 7, 15, 30), "", emptyList()
        )

        /** 容错解析：接受 chapters|ch、title|name|t、summary|s、modules|mods 等别名 */
        fun from(o: JSONObject): Pack {
            val arr = { k: String -> o.optJSONArray(k) ?: JSONArray() }
            val mods = mutableListOf<Module>()
            val modsSrc = if (arr("mods").length() > 0) arr("mods") else arr("modules")
            for (i in 0 until modsSrc.length()) {
                val m = modsSrc.optJSONObject(i) ?: continue
                val k = m.optString("k", (i + 1).toString())
                mods += Module(k, m.optString("n", "模块 $k"), m.optString("tag"))
            }
            val chSrc = if (arr("ch").length() > 0) arr("ch") else arr("chapters")
            val ch = mutableListOf<Chapter>()
            for (i in 0 until chSrc.length()) {
                val c = chSrc.optJSONObject(i) ?: continue
                val id = if (c.has("id")) c.optString("id") else (i + 1).toString()
                val plain = mutableListOf<Pair<String, String>>()
                c.optJSONArray("plain").orEmpty().let { a ->
                    for (j in 0 until a.length()) {
                        val p = a.optJSONArray(j) ?: continue
                        plain += (p.optString(0) to p.optString(1))
                    }
                }
                val cmds = mutableListOf<Cmd>()
                c.optJSONArray("cmds").orEmpty().let { a ->
                    for (j in 0 until a.length()) {
                        val x = a.optJSONObject(j) ?: continue
                        cmds += Cmd(x.optString("c"), x.optString("n"), x.optString("lv", "r"))
                    }
                }
                val pits = mutableListOf<Pit>()
                c.optJSONArray("pits").orEmpty().let { a ->
                    for (j in 0 until a.length()) {
                        val x = a.optJSONObject(j) ?: continue
                        pits += Pit(x.optString("w"), x.optString("c"), x.optString("r"))
                    }
                }
                val q = c.optJSONObject("quiz") ?: JSONObject()
                ch += Chapter(
                    id = id,
                    m = c.optString("m", c.optString("module", mods.firstOrNull()?.k ?: "A")),
                    t = if (c.has("t")) c.optString("t") else c.optString("title", c.optString("name", "第 ${i + 1} 章")),
                    s = if (c.has("s")) c.optString("s") else c.optString("summary", ""),
                    terms = c.optString("terms", ""),
                    plain = plain,
                    cmds = cmds,
                    pits = pits,
                    gate = if (c.has("gate")) c.optString("gate") else "能用自己的话讲清本章内容",
                    quiz = Quiz(q.optString("q", "用自己的话复述本章最重要的三点。"),
                        q.optString("a", "对照章节内容自查：能讲出来才算过关。"),
                        q.optString("hint", "")),
                    content = c.optString("content", ""),
                    media = c.optJSONObject("media")?.let { m ->
                        MediaRef(m.optString("kind"), m.optString("fname"), m.optString("rel"), m.optLong("size"), m.optLong("dur"))
                    },
                )
            }
            val have = mods.mapTo(HashSet()) { it.k }
            ch.forEach { c ->
                if (c.m !in have) {
                    mods += Module(c.m, "模块 ${c.m}", ""); have += c.m
                }
            }
            val deep = linkedMapOf<String, Deep>()
            (o.optJSONObject("deep") ?: JSONObject()).let { d ->
                d.keys().forEach { k ->
                    val v = d.optJSONObject(k) ?: return@forEach
                    val deeps = v.optJSONArray("deep").orEmpty().pairs()
                    val outs = v.optJSONArray("out").orEmpty().pairs()
                    val flows = v.optJSONArray("flow").orEmpty().pairs()
                    deep[k] = Deep(deeps, outs, flows)
                }
            }
            val long = LinkedHashMap<String, LongLecture>()
            (o.optJSONObject("long") ?: JSONObject()).let { l ->
                l.keys().forEach { k ->
                    val v = l.optJSONObject(k) ?: return@forEach
                    long[k] = LongLecture(v)
                }
            }
            // 章节自带 content 而无 long 时，段落切分成长讲义
            ch.forEach { c ->
                if (c.content.isNotBlank() && !long.containsKey(c.id)) {
                    long[c.id] = LongLecture.ofContent(c)
                }
            }
            val pairs = { k: String ->
                val a = arr(k)
                val out = mutableListOf<List<String>>()
                for (i in 0 until a.length()) {
                    val r = a.optJSONArray(i) ?: continue
                    val row = mutableListOf<String>()
                    for (j in 0 until r.length()) row += r.optString(j)
                    out += row
                }
                out
            }
            val layers = pairs("layers").map { it[0] to it.getOrElse(1) { "" } }
            val lab = pairs("lab").map { it[0] to it.getOrElse(1) { "" } }
            val scen = mutableListOf<Scenario>()
            arr("scen").let { a ->
                for (i in 0 until a.length()) {
                    val s = a.optJSONObject(i) ?: continue
                    val steps = s.optJSONArray("steps").orEmpty()
                    scSteps(steps).let { st -> scen += Scenario(s.optString("t"), s.optString("p"), st, s.optString("c"), s.optString("f")) }
                }
            }
            val routes = LinkedHashMap<String, List<String>>()
            val ids = ch.mapTo(HashSet()) { it.id }
            (o.optJSONObject("routes") ?: JSONObject()).let { r ->
                r.keys().forEach { k ->
                    val a = r.optJSONArray(k) ?: return@forEach
                    val list = mutableListOf<String>()
                    for (i in 0 until a.length()) {
                        val id = a.optString(i)
                        if (id in ids) list += id
                    }
                    routes[k] = list
                }
            }
            if (routes.isEmpty()) routes["all"] = ch.map { it.id }
            val rn = LinkedHashMap<String, String>()
            (o.optJSONObject("routeNames") ?: JSONObject()).let { r ->
                r.keys().forEach { k -> rn[k] = r.optString(k) }
            }
            routes.keys.forEach { k ->
                if (!rn.containsKey(k)) rn[k] = if (k == "all") "全章节顺序" else "路线 $k"
            }
            val iv = mutableListOf<Int>()
            arr("intervals").let { a -> for (i in 0 until a.length()) iv += a.optInt(i) }
            val dep = mutableListOf<Pair<String, String>>()
            pairs("depNotes").forEach { dep += it[0] to it.getOrElse(1) { "" } }
            return Pack(
                mods = mods, ch = ch, deep = deep, long = long,
                layers = layers, cmds = pairs("cmds"), events = pairs("events"), scen = scen, lab = lab,
                routes = routes, routeNames = rn,
                intervals = if (iv.isEmpty()) listOf(2, 4, 7, 15, 30) else iv,
                depText = o.optString("depText", ""), depNotes = dep,
            )
        }

        private fun scSteps(a: JSONArray): List<String> {
            val out = mutableListOf<String>()
            for (i in 0 until a.length()) out += a.optString(i)
            return out
        }

        private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

        private fun JSONArray.pairs(): List<Pair<String, String>> {
            val out = mutableListOf<Pair<String, String>>()
            for (i in 0 until length()) {
                val r = optJSONArray(i) ?: continue
                out += (r.optString(0) to r.optString(1))
            }
            return out
        }
    }
}

data class Module(val k: String, val n: String, val tag: String)
data class Cmd(val c: String, val n: String, val lv: String)
data class Pit(val w: String, val c: String, val r: String)
data class Quiz(val q: String, val a: String, val hint: String = "")

data class Chapter(
    val id: String, val m: String, val t: String, val s: String,
    val terms: String, val plain: List<Pair<String, String>>,
    val cmds: List<Cmd>, val pits: List<Pit>, val gate: String,
    val quiz: Quiz, val content: String = "", val media: MediaRef? = null,
)

/** 知识详解：标题+细节 / 命令+回显 / 名称+链路 */
data class Deep(val deep: List<Pair<String, String>>, val out: List<Pair<String, String>>, val flow: List<Pair<String, String>>)

data class Scenario(val t: String, val p: String, val steps: List<String>, val c: String, val f: String)

/** 媒体课程章节的文件引用（对应 Web 的 chapter.media） */
data class MediaRef(val kind: String, val fname: String, val uri: String, val size: Long, val dur: Long = 0)

/**
 * 长讲义区块类型：
 * 概念 = P(段落) | UL(列表) | TBL(表格) | MD(行内 Markdown) | PARTS(富文本分段)
 * 实操 = 步骤：标题 / 说明 / 命令 / 预期输出 / 怎么读
 * 指南 = 错误做法 / 后果 / 正确
 */
class LongLecture(
    val title: String,
    val concept: List<ConceptBlock>,
    val practice: List<StepBlock>,
    val guide: List<GuideBlock>,
    val quiz: Quiz,
) {
    companion object {
        fun ofContent(c: Chapter): LongLecture {
            val ps = c.content.split(Regex("\n{2,}")).map { it.trim() }.filter { it.isNotEmpty() }
            return LongLecture(
                title = c.t,
                concept = ps.map { ConceptBlock.p(it) },
                practice = listOf(StepBlock("本章无命令行操作", "参考正文中的步骤", "按正文逐项完成并留痕。", "", "", "")),
                guide = c.pits.map { GuideBlock(it.w, it.c, it.r, "", "") },
                quiz = c.quiz,
            )
        }

        operator fun invoke(o: JSONObject): LongLecture {
            val concept = mutableListOf<ConceptBlock>()
            o.optJSONArray("concept").orEmpty().let { a ->
                for (i in 0 until a.length()) {
                    val x = a.optJSONObject(i) ?: continue
                    concept += ConceptBlock.from(x)
                }
            }
            val practice = mutableListOf<StepBlock>()
            o.optJSONArray("practice").orEmpty().let { a ->
                for (i in 0 until a.length()) {
                    val x = a.optJSONObject(i) ?: continue
                    practice += StepBlock(
                        x.optString("t"), x.optString("d"), x.optString("cmd"),
                        x.optString("out"), x.optString("read"), x.optString("md"),
                        Part.from(x.optJSONArray("parts")),
                    )
                }
            }
            val guide = mutableListOf<GuideBlock>()
            o.optJSONArray("guide").orEmpty().let { a ->
                for (i in 0 until a.length()) {
                    val x = a.optJSONObject(i) ?: continue
                    guide += GuideBlock(x.optString("w"), x.optString("c"), x.optString("r"), x.optString("t"), x.optString("d"))
                }
            }
            val q = o.optJSONObject("quiz") ?: JSONObject()
            return LongLecture(o.optString("title"), concept, practice, guide,
                Quiz(q.optString("q", ""), q.optString("a", ""), q.optString("hint", "")))
        }

        private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()
    }
}

/** 概念区块：段落 / 列表 / 表格 / md / parts */
data class ConceptBlock(
    val p: String = "", val ul: List<String> = emptyList(),
    val head: List<String> = emptyList(), val rows: List<List<String>> = emptyList(),
    val md: String = "", val title: String = "", val parts: List<Part> = emptyList(),
) {
    val isTable get() = head.isNotEmpty()
    val isMd get() = md.isNotEmpty()

    companion object {
        fun p(s: String) = ConceptBlock(p = s)
        fun from(x: JSONObject): ConceptBlock {
            val ul = mutableListOf<String>()
            x.optJSONArray("ul").orEmpty().let { a -> for (i in 0 until a.length()) ul += a.optString(i) }
            val head = mutableListOf<String>()
            val rows = mutableListOf<List<String>>()
            x.optJSONObject("tbl")?.let { t ->
                t.optJSONArray("head").orEmpty().let { a -> for (i in 0 until a.length()) head += a.optString(i) }
                t.optJSONArray("rows").orEmpty().let { a ->
                    for (i in 0 until a.length()) {
                        val r = a.optJSONArray(i) ?: continue
                        val row = mutableListOf<String>()
                        for (j in 0 until r.length()) row += r.optString(j)
                        rows += row
                    }
                }
            }
            return ConceptBlock(x.optString("p"), ul, head, rows, x.optString("md"), x.optString("t"), Part.from(x.optJSONArray("parts")))
        }

        private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()
    }
}

/** 行内富文本分段（对应 Web 的 parts：带 **粗体** / `代码` 标记的连续片段） */
data class Part(val text: String, val b: Boolean = false, val code: Boolean = false) {
    companion object {
        fun from(a: JSONArray?): List<Part> {
            if (a == null) return emptyList()
            val out = mutableListOf<Part>()
            for (i in 0 until a.length()) {
                val x = a.optJSONObject(i) ?: continue
                out += Part(x.optString("t"), x.optBoolean("b"), x.optBoolean("code") || x.optBoolean("c"))
            }
            return out
        }
    }
}

data class StepBlock(
    val t: String, val d: String, val cmd: String,
    val out: String, val read: String, val md: String = "",
    val parts: List<Part> = emptyList(),
) {
    val hasCmd get() = cmd.isNotBlank()
}

data class GuideBlock(val w: String, val c: String, val r: String, val t: String, val d: String)

/** 指南行：每个段落一行，保留"错误做法：/ 后果：/ 正确："前缀，可分别划线 */
fun GuideBlock.lines(): List<String> {
    if (w.isNotEmpty() || c.isNotEmpty() || r.isNotEmpty()) {
        val out = ArrayList<String>()
        if (w.isNotEmpty()) out.add("错误做法：$w")
        if (c.isNotEmpty()) out.add("后果：$c")
        if (r.isNotEmpty()) out.add("正确：$r")
        return out
    }
    val out = ArrayList<String>()
    if (t.isNotEmpty()) out.add(t)
    if (d.isNotEmpty()) {
        val marks = ArrayList<Pair<Int, Int>>()
        var i = 0
        while (i < d.length) {
            when {
                d.startsWith("后果：", i) -> { marks.add(i to 1); i += 3 }
                d.startsWith("正确：", i) -> { marks.add(i to 2); i += 3 }
                d.startsWith("错误做法：", i) -> { marks.add(i to 0); i += 5 }
                else -> i++
            }
        }
        if (marks.isEmpty()) {
            if (d.isNotBlank()) out.add(d.trim())
        } else {
            if (marks.first().first > 0) {
                val head = d.substring(0, marks.first().first).trim()
                if (head.isNotEmpty()) out.add(head)
            }
            for (j in marks.indices) {
                val end = if (j + 1 < marks.size) marks[j + 1].first else d.length
                val seg = d.substring(marks[j].first, end).trim()
                if (seg.isNotEmpty()) out.add(seg)
            }
        }
    }
    return out
}
